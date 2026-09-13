package com.TechHurts.CodeFile;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.*;
import org.snakeyaml.engine.v2.api.*;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.nodes.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** Data-only formatters. Never construct Java objects from YAML tags. */
final class TextFormats {
  static String extension(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static boolean supports(String name, String language) {
    return Arrays.asList("yaml", "csv", "tsv", "jsonl", "ndjson", "xml").contains(language)
        || Arrays.asList(
                "yaml",
                "yml",
                "csv",
                "tsv",
                "xml",
                "svg",
                "jsonl",
                "ndjson",
                "txt",
                "log",
                "ini",
                "cfg",
                "properties",
                "toml",
                "md",
                "markdown")
            .contains(extension(name));
  }

  static String format(String name, String language, String text, int indent) throws Exception {
    String ext =
        Arrays.asList("csv", "tsv", "jsonl", "ndjson", "xml").contains(language)
            ? language
            : extension(name);
    if (language.equals("yaml") || ext.equals("yaml") || ext.equals("yml"))
      return yaml(text, indent);
    if (ext.equals("csv") || ext.equals("tsv"))
      return csv(delimited(text, ext.equals("tsv") ? '\t' : ','), ext.equals("tsv") ? '\t' : ',');
    if (ext.equals("xml") || ext.equals("svg")) return xml(text, indent);
    if (ext.equals("jsonl") || ext.equals("ndjson")) {
      StringBuilder out = new StringBuilder();
      for (String line : SourceTools.normalize(text).split("\n")) {
        if (line.isBlank()) {
          out.append('\n');
          continue;
        }
        JSONTokener tokener = new JSONTokener(line);
        Object value = tokener.nextValue();
        if (tokener.nextClean() != 0)
          throw new IllegalArgumentException("Extra content in JSON record");
        out.append(compactJson(line)).append('\n');
      }
      return out.toString();
    }
    // Whitespace can be data (Markdown hard breaks, INI values, log columns).
    return SourceTools.normalize(text);
  }

  private static String compactJson(String source) {
    StringBuilder out = new StringBuilder();
    boolean quote = false, escape = false;
    for (char c : source.toCharArray()) {
      if (quote) {
        out.append(c);
        if (escape) escape = false;
        else if (c == '\\') escape = true;
        else if (c == '"') quote = false;
        continue;
      }
      if (c == '"') quote = true;
      if (!Character.isWhitespace(c)) out.append(c);
    }
    return out.toString();
  }

  static String yaml(String text, int indent) {
    LoadSettings load =
        LoadSettings.builder()
            .setParseComments(true)
            .setCodePointLimit(1048576)
            .setMaxAliasesForCollections(50)
            .build();
    List<org.snakeyaml.engine.v2.nodes.Node> nodes = new ArrayList<>();
    for (org.snakeyaml.engine.v2.nodes.Node node : new Compose(load).composeAllFromString(text))
      nodes.add(node);
    if (nodes.isEmpty()) return text;
    DumpSettings settings =
        DumpSettings.builder()
            .setDumpComments(true)
            .setIndent(indent)
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setExplicitStart(nodes.size() > 1)
            .setWidth(100)
            .build();
    StringBuilder out = new StringBuilder();
    StreamDataWriter writer =
        new StreamDataWriter() {
          public void write(String s) {
            out.append(s);
          }

          public void write(String s, int off, int len) {
            out.append(s, off, off + len);
          }
        };
    for (org.snakeyaml.engine.v2.nodes.Node node : nodes) {
      block(node, Collections.newSetFromMap(new IdentityHashMap<>()));
      new Dump(settings).dumpNode(node, writer);
    }
    return out.toString();
  }

  static void block(
      org.snakeyaml.engine.v2.nodes.Node node, Set<org.snakeyaml.engine.v2.nodes.Node> visited) {
    if (!visited.add(node)) return;
    // Keep flow collections with attached comments intact: moving their closing-token
    // comments into block layout is ambiguous and unsupported by the emitter.
    if (node instanceof CollectionNode
        && ((CollectionNode<?>) node).getFlowStyle() == FlowStyle.FLOW
        && node.getInLineComments() != null
        && !node.getInLineComments().isEmpty()) return;
    if (node instanceof CollectionNode) ((CollectionNode<?>) node).setFlowStyle(FlowStyle.BLOCK);
    if (node instanceof MappingNode)
      for (NodeTuple pair : ((MappingNode) node).getValue()) {
        block(pair.getKeyNode(), visited);
        block(pair.getValueNode(), visited);
      }
    else if (node instanceof SequenceNode)
      for (org.snakeyaml.engine.v2.nodes.Node child : ((SequenceNode) node).getValue())
        block(child, visited);
  }

  /** RFC 4180-style records, preserving embedded newlines, escaped quotes and empty fields. */
  static List<List<String>> delimited(String input, char separator) {
    if (input.length() > 1048576)
      throw new IllegalArgumentException("Delimited data is limited to 1 MiB");
    List<List<String>> records = new ArrayList<>();
    if (input.isEmpty()) return records;
    List<String> row = new ArrayList<>();
    StringBuilder value = new StringBuilder();
    boolean quoted = false, closed = false, ended = false;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      ended = false;
      if (quoted) {
        if (c == '"') {
          if (i + 1 < input.length() && input.charAt(i + 1) == '"') {
            value.append('"');
            i++;
          } else {
            quoted = false;
            closed = true;
          }
        } else value.append(c);
        continue;
      }
      if (c == separator) {
        row.add(value.toString());
        value.setLength(0);
        closed = false;
        if (row.size() > 256) throw new IllegalArgumentException("Maximum 256 columns");
      } else if (c == '\r' || c == '\n') {
        if (c == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
        row.add(value.toString());
        records.add(row);
        row = new ArrayList<>();
        value.setLength(0);
        closed = false;
        ended = true;
      } else if (c == '"' && value.length() == 0 && !closed) quoted = true;
      else {
        if (closed || c == '"')
          throw new IllegalArgumentException(
              "Invalid quote in delimited record " + (records.size() + 1));
        value.append(c);
      }
    }
    if (quoted) throw new IllegalArgumentException("Unclosed quoted field");
    if (!ended) {
      row.add(value.toString());
      records.add(row);
    }
    return records;
  }

  static String csv(List<List<String>> records, char separator) {
    StringBuilder out = new StringBuilder();
    for (List<String> row : records) {
      for (int i = 0; i < row.size(); i++) {
        if (i > 0) out.append(separator);
        String field = row.get(i);
        if (field.indexOf(separator) >= 0
            || field.indexOf('"') >= 0
            || field.indexOf('\n') >= 0
            || field.indexOf('\r') >= 0)
          out.append('"').append(field.replace("\"", "\"\"")).append('"');
        else out.append(field);
      }
      out.append('\n');
    }
    return out.toString();
  }

  static String xml(String text, int indent) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    if (text.contains("<!DOCTYPE"))
      throw new IllegalArgumentException(
          "XML formatting does not accept document type declarations");
    factory.setExpandEntityReferences(false);
    javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(
        (publicId, systemId) -> {
          throw new org.xml.sax.SAXException("External entities are disabled");
        });
    org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(text)));
    // Mixed content and xml:space documents are whitespace-sensitive: retain their layout.
    if (sensitive(doc.getDocumentElement())) return text;
    removeLayout(doc.getDocumentElement());
    Transformer transform = TransformerFactory.newInstance().newTransformer();
    transform.setOutputProperty(OutputKeys.INDENT, "yes");
    transform.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
    transform.setOutputProperty(
        OutputKeys.OMIT_XML_DECLARATION, text.stripLeading().startsWith("<?xml") ? "no" : "yes");
    StringWriter out = new StringWriter();
    transform.transform(new DOMSource(doc), new StreamResult(out));
    return out.toString();
  }

  static boolean sensitive(org.w3c.dom.Node node) {
    boolean element = false, words = false;
    if (node instanceof Element && ((Element) node).hasAttribute("xml:space")) return true;
    for (org.w3c.dom.Node child = node.getFirstChild();
        child != null;
        child = child.getNextSibling()) {
      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        element = true;
        if (sensitive(child)) return true;
      } else if (child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) return true;
      else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE && !child.getNodeValue().isBlank())
        words = true;
    }
    return element && words;
  }

  static void removeLayout(org.w3c.dom.Node node) {
    boolean hasElement = false;
    for (org.w3c.dom.Node c = node.getFirstChild(); c != null; c = c.getNextSibling())
      if (c.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) hasElement = true;
    for (org.w3c.dom.Node c = node.getFirstChild(); c != null; ) {
      org.w3c.dom.Node next = c.getNextSibling();
      if (hasElement && c.getNodeType() == org.w3c.dom.Node.TEXT_NODE && c.getNodeValue().isBlank())
        node.removeChild(c);
      else removeLayout(c);
      c = next;
    }
  }
}
