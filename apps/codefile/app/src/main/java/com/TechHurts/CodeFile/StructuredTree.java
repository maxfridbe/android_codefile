package com.TechHurts.CodeFile;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.xmlpull.v1.XmlPullParser;

/** Tree views of YAML, TOML, INI, properties, XML, JSON Lines and JSONC, as {@link JsonTree}. */
final class StructuredTree {
  private static final int MAX_CHARS = 2 * 1024 * 1024, MAX_NODES = 50000, MAX_DEPTH = 128;
  private int count;

  private StructuredTree() {}

  private static void limit(String source) throws IOException {
    if (source.length() > MAX_CHARS) throw new IOException("Tree view supports files up to 2 MiB");
  }

  private JsonTree.Node make(String key, String path, String type, String value, int depth)
      throws IOException {
    if (depth > MAX_DEPTH || ++count > MAX_NODES)
      throw new IOException("Tree exceeds 128 levels or 50,000 values");
    return new JsonTree.Node(key, path, type, value);
  }

  private static String sub(String path, String key) {
    return path + "[" + org.json.JSONObject.quote(key) + "]";
  }

  private static JsonTree.Node opened(JsonTree.Node root) {
    root.expanded = true;
    return root;
  }

  // ---- JSON Lines and JSONC ----------------------------------------------------------------

  static JsonTree.Node jsonLines(String source) throws IOException {
    limit(source);
    StructuredTree t = new StructuredTree();
    JsonTree.Node root = t.make("$", "$", "array", "", 0);
    String[] lines = source.split("\\R");
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].trim().isEmpty()) continue;
      String key = "line " + (i + 1);
      try {
        root.children.add(JsonTree.parse(lines[i], key, "$[" + (i + 1) + "]"));
      } catch (IOException | RuntimeException bad) {
        throw new IOException("line " + (i + 1) + " is not valid JSON");
      }
      if (root.children.size() > MAX_NODES) throw new IOException("More than 50,000 records");
    }
    if (root.children.isEmpty()) throw new IOException("No JSON records");
    return opened(root);
  }

  static JsonTree.Node jsonc(String source) throws IOException {
    return JsonTree.parse(JsoncCleaner.clean(source));
  }

  // ---- YAML --------------------------------------------------------------------------------

  static JsonTree.Node yaml(String source) throws IOException {
    limit(source);
    LoadSettings settings =
        LoadSettings.builder().setCodePointLimit(MAX_CHARS).setMaxAliasesForCollections(50).build();
    List<org.snakeyaml.engine.v2.nodes.Node> docs = new ArrayList<>();
    try {
      for (org.snakeyaml.engine.v2.nodes.Node doc :
          new Compose(settings).composeAllFromString(source)) docs.add(doc);
    } catch (RuntimeException bad) {
      throw new IOException("not valid YAML: " + String.valueOf(bad.getMessage()).split("\n")[0]);
    }
    if (docs.isEmpty()) throw new IOException("The YAML document is empty");
    StructuredTree t = new StructuredTree();
    if (docs.size() == 1) return opened(t.yamlNode(docs.get(0), "$", "$", 0));
    JsonTree.Node root = t.make("$", "$", "array", "", 0);
    for (int i = 0; i < docs.size(); i++)
      root.children.add(t.yamlNode(docs.get(i), "document " + (i + 1), "$[" + i + "]", 1));
    return opened(root);
  }

  private JsonTree.Node yamlNode(
      org.snakeyaml.engine.v2.nodes.Node node, String key, String path, int depth)
      throws IOException {
    if (node instanceof MappingNode) {
      JsonTree.Node object = make(key, path, "object", "", depth);
      for (NodeTuple tuple : ((MappingNode) node).getValue()) {
        String child =
            tuple.getKeyNode() instanceof ScalarNode
                ? ((ScalarNode) tuple.getKeyNode()).getValue()
                : "(complex key)";
        object.children.add(yamlNode(tuple.getValueNode(), child, sub(path, child), depth + 1));
      }
      return object;
    }
    if (node instanceof SequenceNode) {
      JsonTree.Node array = make(key, path, "array", "", depth);
      int index = 0;
      for (org.snakeyaml.engine.v2.nodes.Node item : ((SequenceNode) node).getValue()) {
        array.children.add(yamlNode(item, String.valueOf(index), path + "[" + index + "]", depth + 1));
        index++;
      }
      return array;
    }
    ScalarNode scalar = (ScalarNode) node;
    Tag tag = scalar.getTag();
    String type = "string";
    if (tag.equals(Tag.NULL)) type = "null";
    else if (tag.equals(Tag.BOOL)) type = "boolean";
    else if (tag.equals(Tag.INT) || tag.equals(Tag.FLOAT)) type = "number";
    return make(key, path, type, scalar.getValue(), depth);
  }

  // ---- INI and properties -------------------------------------------------------------------

  static JsonTree.Node ini(String source) throws IOException {
    limit(source);
    StructuredTree t = new StructuredTree();
    JsonTree.Node root = t.make("$", "$", "object", "", 0), section = root;
    for (String raw : source.split("\\R")) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;
      if (line.startsWith("[") && line.endsWith("]")) {
        String name = line.substring(1, line.length() - 1).trim();
        section = t.make(name, sub("$", name), "object", "", 1);
        root.children.add(section);
        continue;
      }
      int eq = firstSeparator(line, "=:");
      String key = (eq < 0 ? line : line.substring(0, eq)).trim();
      String value = eq < 0 ? "" : unquote(line.substring(eq + 1).trim());
      section.children.add(t.make(key, sub(section.path, key), scalarType(value), value, 2));
    }
    return opened(root);
  }

  /** Java .properties and .env style: dotted keys become nested objects where that is unambiguous. */
  static JsonTree.Node properties(String source) throws IOException {
    limit(source);
    StructuredTree t = new StructuredTree();
    JsonTree.Node root = t.make("$", "$", "object", "", 0);
    StringBuilder pending = new StringBuilder();
    for (String raw : source.split("\\R")) {
      String line = pending.length() > 0 ? pending + raw.trim() : raw.trim();
      pending.setLength(0);
      if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue;
      if (line.endsWith("\\") && !line.endsWith("\\\\")) {
        pending.append(line, 0, line.length() - 1);
        continue;
      }
      if (line.startsWith("export ")) line = line.substring(7).trim();
      int sep = firstSeparator(line, "=: \t");
      String key = (sep < 0 ? line : line.substring(0, sep)).trim();
      String rest = sep < 0 ? "" : line.substring(sep).trim();
      // A key may end at whitespace, with one optional = or : before the value.
      if (rest.startsWith("=") || rest.startsWith(":")) rest = rest.substring(1).trim();
      t.put(root, key, unquote(rest));
    }
    return opened(root);
  }

  private void put(JsonTree.Node root, String dotted, String value) throws IOException {
    String[] parts = dotted.split("\\.");
    JsonTree.Node at = root;
    for (int i = 0; i < parts.length - 1; i++) {
      JsonTree.Node next = child(at, parts[i]);
      if (next == null) {
        next = make(parts[i], sub(at.path, parts[i]), "object", "", i + 1);
        at.children.add(next);
      }
      if (!next.container()) {
        at.children.add(make(dotted, sub(at.path, dotted), scalarType(value), value, i + 1));
        return;
      }
      at = next;
    }
    String leaf = parts[parts.length - 1];
    if (child(at, leaf) != null) leaf = dotted;
    at.children.add(make(leaf, sub(at.path, leaf), scalarType(value), value, parts.length));
  }

  private static JsonTree.Node child(JsonTree.Node parent, String key) {
    for (JsonTree.Node child : parent.children) if (child.key.equals(key)) return child;
    return null;
  }

  private static int firstSeparator(String line, String separators) {
    for (int i = 0; i < line.length(); i++) if (separators.indexOf(line.charAt(i)) >= 0) return i;
    return -1;
  }

  private static String unquote(String value) {
    boolean quoted =
        value.length() >= 2
            && (value.charAt(0) == '"' || value.charAt(0) == '\'')
            && value.charAt(value.length() - 1) == value.charAt(0);
    return quoted ? value.substring(1, value.length() - 1) : value;
  }

  private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

  private static String scalarType(String value) {
    if (value.equals("true") || value.equals("false")) return "boolean";
    return NUMBER.matcher(value).matches() ? "number" : "string";
  }

  // ---- TOML --------------------------------------------------------------------------------

  static JsonTree.Node toml(String source) throws IOException {
    limit(source);
    return opened(new StructuredTree().new Toml(source).parse());
  }

  /** Tables, arrays of tables, dotted keys, inline tables, multi-line arrays and strings. */
  private final class Toml {
    private final String s;
    private int i;

    Toml(String s) {
      this.s = s.replace("\r\n", "\n");
    }

    JsonTree.Node parse() throws IOException {
      JsonTree.Node root = make("$", "$", "object", "", 0), table = root;
      while (true) {
        skipBlank();
        if (i >= s.length()) return root;
        if (s.charAt(i) == '[') {
          table = header(root);
        } else {
          List<String> key = keyPath();
          expect('=');
          skipSpaces();
          assign(table, key, 1);
        }
        endOfLine();
      }
    }

    private JsonTree.Node header(JsonTree.Node root) throws IOException {
      boolean array = s.startsWith("[[", i);
      i += array ? 2 : 1;
      List<String> path = keyPath();
      expect(']');
      if (array) expect(']');
      JsonTree.Node at = root;
      for (int n = 0; n < path.size(); n++) {
        boolean last = n == path.size() - 1;
        at = step(at, path.get(n), last && array, n + 1);
      }
      return at;
    }

    private JsonTree.Node step(JsonTree.Node at, String key, boolean newArrayItem, int depth)
        throws IOException {
      JsonTree.Node next = child(at, key);
      if (newArrayItem) {
        if (next == null) {
          next = make(key, sub(at.path, key), "array", "", depth);
          at.children.add(next);
        }
        JsonTree.Node item =
            make(String.valueOf(next.children.size()), next.path + "[" + next.children.size() + "]", "object", "", depth + 1);
        next.children.add(item);
        return item;
      }
      if (next == null) {
        next = make(key, sub(at.path, key), "object", "", depth);
        at.children.add(next);
      }
      boolean arrayOfTables = next.type.equals("array") && !next.children.isEmpty();
      return arrayOfTables ? next.children.get(next.children.size() - 1) : next;
    }

    private void assign(JsonTree.Node table, List<String> key, int depth) throws IOException {
      JsonTree.Node at = table;
      for (int n = 0; n < key.size() - 1; n++) at = step(at, key.get(n), false, depth + n);
      String leaf = key.get(key.size() - 1);
      at.children.add(value(at, leaf, depth + key.size()));
    }

    private JsonTree.Node value(JsonTree.Node parent, String key, int depth) throws IOException {
      String path = parent.type.equals("array") ? parent.path + "[" + key + "]" : sub(parent.path, key);
      if (i >= s.length()) throw fail("a value is missing");
      char c = s.charAt(i);
      if (c == '[') return array(key, path, depth);
      if (c == '{') return inline(key, path, depth);
      if (s.startsWith("\"\"\"", i)) return make(key, path, "string", multiline("\"\"\"", true), depth);
      if (s.startsWith("'''", i)) return make(key, path, "string", multiline("'''", false), depth);
      if (c == '"') return make(key, path, "string", quoted(), depth);
      if (c == '\'') return make(key, path, "string", literal(), depth);
      String token = bare();
      if (token.isEmpty()) throw fail("a value is missing");
      if (token.equals("true") || token.equals("false")) return make(key, path, "boolean", token, depth);
      boolean date = token.matches("\\d{4}-\\d\\d-\\d\\d.*") || token.matches("\\d\\d:\\d\\d:\\d\\d.*");
      if (date) return make(key, path, "datetime", dateTime(token), depth);
      return make(key, path, "number", token, depth);
    }

    private String dateTime(String token) {
      boolean spaced =
          token.matches("\\d{4}-\\d\\d-\\d\\d")
              && i + 1 < s.length()
              && s.charAt(i) == ' '
              && Character.isDigit(s.charAt(i + 1));
      if (!spaced) return token;
      i++;
      return token + " " + bare();
    }

    private JsonTree.Node array(String key, String path, int depth) throws IOException {
      JsonTree.Node array = make(key, path, "array", "", depth);
      i++;
      while (true) {
        skipBlank();
        if (i >= s.length()) throw fail("an array is not closed");
        if (s.charAt(i) == ']') {
          i++;
          return array;
        }
        array.children.add(value(array, String.valueOf(array.children.size()), depth + 1));
        skipBlank();
        if (i < s.length() && s.charAt(i) == ',') i++;
        else if (i >= s.length() || s.charAt(i) != ']') throw fail("expected , or ]");
      }
    }

    private JsonTree.Node inline(String key, String path, int depth) throws IOException {
      JsonTree.Node table = make(key, path, "object", "", depth);
      i++;
      skipSpaces();
      if (i < s.length() && s.charAt(i) == '}') {
        i++;
        return table;
      }
      while (true) {
        skipSpaces();
        List<String> name = keyPath();
        expect('=');
        skipSpaces();
        assign(table, name, depth + 1);
        skipSpaces();
        if (i >= s.length()) throw fail("an inline table is not closed");
        char c = s.charAt(i++);
        if (c == '}') return table;
        if (c != ',') throw fail("expected , or }");
      }
    }

    private List<String> keyPath() throws IOException {
      List<String> parts = new ArrayList<>();
      while (true) {
        skipSpaces();
        if (i >= s.length()) throw fail("a key is missing");
        char c = s.charAt(i);
        if (c == '"') parts.add(quoted());
        else if (c == '\'') parts.add(literal());
        else parts.add(bareKey());
        skipSpaces();
        if (i < s.length() && s.charAt(i) == '.') i++;
        else return parts;
      }
    }

    private String bareKey() throws IOException {
      int start = i;
      while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || "_-".indexOf(s.charAt(i)) >= 0)) i++;
      if (i == start) throw fail("a key is missing");
      return s.substring(start, i);
    }

    private String bare() {
      int start = i;
      while (i < s.length() && " \t\n,]}#".indexOf(s.charAt(i)) < 0) i++;
      return s.substring(start, i);
    }

    private String quoted() throws IOException {
      StringBuilder out = new StringBuilder();
      i++;
      while (i < s.length() && s.charAt(i) != '"') {
        if (s.charAt(i) == '\n') throw fail("a string is not closed");
        if (s.charAt(i) == '\\') escape(out);
        else out.append(s.charAt(i++));
      }
      if (i >= s.length()) throw fail("a string is not closed");
      i++;
      return out.toString();
    }

    private void escape(StringBuilder out) throws IOException {
      i++;
      if (i >= s.length()) throw fail("a string is not closed");
      char c = s.charAt(i++);
      Map<Character, String> simple = new HashMap<>();
      simple.put('n', "\n");
      simple.put('t', "\t");
      simple.put('r', "\r");
      simple.put('b', "\b");
      simple.put('f', "\f");
      simple.put('"', "\"");
      simple.put('\\', "\\");
      if (simple.containsKey(c)) out.append(simple.get(c));
      else if ((c == 'u' || c == 'U') && i + (c == 'u' ? 4 : 8) <= s.length()) {
        int len = c == 'u' ? 4 : 8;
        out.appendCodePoint(Integer.parseInt(s.substring(i, i + len), 16));
        i += len;
      } else out.append('\\').append(c);
    }

    private String literal() throws IOException {
      int end = s.indexOf('\'', i + 1);
      if (end < 0 || s.substring(i, end).contains("\n")) throw fail("a string is not closed");
      String value = s.substring(i + 1, end);
      i = end + 1;
      return value;
    }

    private String multiline(String fence, boolean escapes) throws IOException {
      i += 3;
      if (i < s.length() && s.charAt(i) == '\n') i++;
      StringBuilder out = new StringBuilder();
      while (i < s.length() && !s.startsWith(fence, i)) {
        if (escapes && s.charAt(i) == '\\') {
          if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
            i += 2;
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
          } else escape(out);
        } else out.append(s.charAt(i++));
      }
      if (i >= s.length()) throw fail("a multi-line string is not closed");
      i += 3;
      return out.toString();
    }

    private void expect(char c) throws IOException {
      skipSpaces();
      if (i >= s.length() || s.charAt(i) != c) throw fail("expected " + c);
      i++;
    }

    private void skipSpaces() {
      while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
    }

    /** Whitespace, newlines and comments. */
    private void skipBlank() {
      while (i < s.length()) {
        char c = s.charAt(i);
        if (c == '#') while (i < s.length() && s.charAt(i) != '\n') i++;
        else if (Character.isWhitespace(c)) i++;
        else return;
      }
    }

    private void endOfLine() throws IOException {
      skipSpaces();
      if (i < s.length() && s.charAt(i) == '#') while (i < s.length() && s.charAt(i) != '\n') i++;
      if (i < s.length() && s.charAt(i) != '\n') throw fail("unexpected text after a value");
    }

    private IOException fail(String why) {
      int line = 1;
      for (int n = 0; n < Math.min(i, s.length()); n++) if (s.charAt(n) == '\n') line++;
      return new IOException("TOML line " + line + ": " + why);
    }
  }

  // ---- XML ---------------------------------------------------------------------------------

  private static final class Element {
    final String name;
    final List<String[]> attributes = new ArrayList<>();
    final List<Element> children = new ArrayList<>();
    final StringBuilder text = new StringBuilder();

    Element(String name) {
      this.name = name;
    }
  }

  static JsonTree.Node xml(String source) throws IOException {
    limit(source);
    Element root = readXml(source);
    return opened(new StructuredTree().element(root, "/" + root.name, 0));
  }

  private static Element readXml(String source) throws IOException {
    try {
      XmlPullParser parser = android.util.Xml.newPullParser();
      parser.setInput(new StringReader(source));
      List<Element> stack = new ArrayList<>();
      Element root = null;
      int total = 0;
      for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
        if (type == XmlPullParser.START_TAG) {
          if (++total > MAX_NODES || stack.size() > MAX_DEPTH)
            throw new IOException("Tree exceeds 128 levels or 50,000 values");
          Element element = new Element(parser.getName());
          for (int a = 0; a < parser.getAttributeCount(); a++)
            element.attributes.add(new String[] {parser.getAttributeName(a), parser.getAttributeValue(a)});
          if (stack.isEmpty()) root = element;
          else stack.get(stack.size() - 1).children.add(element);
          stack.add(element);
        } else if (type == XmlPullParser.END_TAG) {
          stack.remove(stack.size() - 1);
        } else if (type == XmlPullParser.TEXT && !stack.isEmpty()) {
          stack.get(stack.size() - 1).text.append(parser.getText());
        }
      }
      if (root == null) throw new IOException("The XML document is empty");
      return root;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("not valid XML: " + String.valueOf(e.getMessage()).split("\n")[0]);
    }
  }

  private JsonTree.Node element(Element element, String path, int depth) throws IOException {
    String text = element.text.toString().trim();
    boolean leaf = element.children.isEmpty() && element.attributes.isEmpty();
    if (leaf) return make(element.name, path, "string", text, depth);
    JsonTree.Node node = make(element.name, path, "element", "", depth);
    for (String[] attribute : element.attributes)
      node.children.add(make("@" + attribute[0], path + "/@" + attribute[0], "string", attribute[1], depth + 1));
    if (!text.isEmpty()) node.children.add(make("#text", path + "/text()", "string", text, depth + 1));
    Map<String, Integer> totals = new HashMap<>(), seen = new HashMap<>();
    for (Element child : element.children) totals.merge(child.name, 1, Integer::sum);
    for (Element child : element.children) {
      int n = seen.merge(child.name, 1, Integer::sum);
      String suffix = totals.get(child.name) > 1 ? "[" + n + "]" : "";
      node.children.add(element(child, path + "/" + child.name + suffix, depth + 1));
    }
    return node;
  }
}
