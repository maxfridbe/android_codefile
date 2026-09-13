package com.TechHurts.CodeFile;

import java.util.*;
import java.util.regex.*;

/** Source operations are pure Java; document contents never execute. */
public final class SourceTools {
  private SourceTools() {}

  public static String normalize(String text) {
    return text.replace("\r\n", "\n").replace('\r', '\n');
  }

  public static final class Symbol {
    public final String name;
    public final int line, column;

    Symbol(String name, int line, int column) {
      this.name = name;
      this.line = line;
      this.column = column;
    }

    @Override
    public String toString() {
      return name + " · line " + (line + 1);
    }
  }

  public static List<Symbol> symbols(String text) {
    List<Symbol> result = new ArrayList<>();
    Pattern named =
        Pattern.compile(
            "\\b(?:class|interface|enum|struct|record|object|trait|fn|fun|function|def|module|namespace|type|const|let|var)\\s+([\\p{L}_$][\\p{L}\\p{N}_$]*)");
    Pattern method =
        Pattern.compile(
            "(?:^|\\s)([\\p{L}_$][\\p{L}\\p{N}_$]*)\\s*\\([^;]*\\)\\s*(?::[^=]+)?(?:\\{|=>|$)");
    int n = 0;
    boolean block = false;
    for (String line : normalize(text).split("\n", -1)) {
      String trim = line.trim();
      if (trim.startsWith("/*")) block = true;
      if (!block && !trim.startsWith("//") && !trim.startsWith("#") && !trim.startsWith("*")) {
        Matcher m = named.matcher(line);
        boolean found = false;
        while (m.find()) {
          result.add(new Symbol(m.group(1), n, m.start(1)));
          found = true;
        }
        if (!found) {
          m = method.matcher(line);
          if (m.find()
              && !Arrays.asList("if", "for", "while", "switch", "catch", "return")
                  .contains(m.group(1))) result.add(new Symbol(m.group(1), n, m.start(1)));
        }
      }
      if (line.contains("*/")) block = false;
      n++;
      if (result.size() >= 2000) break;
    }
    return result;
  }

  public static String wordAt(String text, int position) {
    int start = Math.max(0, Math.min(position, text.length())), end = start;
    while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) start--;
    while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) end++;
    return text.substring(start, end);
  }

  public static List<Symbol> references(String text, String word) {
    List<Symbol> result = new ArrayList<>();
    if (word.isEmpty()) return result;
    Pattern p =
        Pattern.compile("(?<![\\p{L}\\p{N}_$])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_$])");
    int n = 0;
    for (String line : text.split("\n", -1)) {
      Matcher m = p.matcher(line);
      while (m.find()) {
        result.add(new Symbol(word, n, m.start()));
        if (result.size() == 2000) return result;
      }
      n++;
    }
    return result;
  }

  /** Whitespace-only indentation. Preserves every line and quoted/comment contents. */
  public static String indent(String text, int width, boolean tabs) {
    StringBuilder out = new StringBuilder();
    int depth = 0;
    boolean block = false;
    char quote = 0;
    boolean escape = false;
    String[] lines = normalize(text).split("\n", -1);
    for (int n = 0; n < lines.length; n++) {
      String raw = lines[n], trim = raw.stripLeading();
      boolean protectedLine = block || quote != 0;
      int level = Math.max(0, depth - (trim.startsWith("}") || trim.startsWith("]") ? 1 : 0));
      if (!protectedLine && !trim.isEmpty())
        out.append((tabs ? "\t" : " ".repeat(width)).repeat(Math.min(level, 100))).append(trim);
      else out.append(raw);
      for (int i = 0; i < raw.length(); i++) {
        char c = raw.charAt(i), next = i + 1 < raw.length() ? raw.charAt(i + 1) : 0;
        if (block) {
          if (c == '*' && next == '/') {
            block = false;
            i++;
          }
          continue;
        }
        if (quote != 0) {
          if (escape) {
            escape = false;
            continue;
          }
          if (c == '\\') {
            escape = true;
            continue;
          }
          if (c == quote) quote = 0;
          continue;
        }
        if (c == '/' && next == '/') break;
        if (c == '/' && next == '*') {
          block = true;
          i++;
          continue;
        }
        if (c == '"' || c == '\'' || c == '`') {
          quote = c;
          continue;
        }
        if (c == '{' || c == '[') depth++;
        else if (c == '}' || c == ']') depth = Math.max(0, depth - 1);
      }
      if (n < lines.length - 1) out.append('\n');
    }
    return out.toString();
  }

  /** JSON layout changes only whitespace outside strings. */
  public static String jsonFormat(String source, int width, boolean tabs) {
    StringBuilder out = new StringBuilder();
    int level = 0;
    boolean quote = false, escape = false;
    String unit = tabs ? "\t" : " ".repeat(width);
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (quote) {
        out.append(c);
        if (escape) escape = false;
        else if (c == '\\') escape = true;
        else if (c == '"') quote = false;
        continue;
      }
      if (Character.isWhitespace(c)) continue;
      if (c == '"') {
        quote = true;
        out.append(c);
      } else if (c == '{' || c == '[') {
        out.append(c);
        level++;
        out.append('\n').append(unit.repeat(Math.min(level, 100)));
      } else if (c == '}' || c == ']') {
        level = Math.max(0, level - 1);
        trimTail(out);
        out.append('\n').append(unit.repeat(Math.min(level, 100))).append(c);
      } else if (c == ',') {
        out.append(',').append('\n').append(unit.repeat(Math.min(level, 100)));
      } else if (c == ':') out.append(": ");
      else out.append(c);
    }
    return out.append('\n').toString();
  }

  private static void trimTail(StringBuilder b) {
    while (b.length() > 0 && Character.isWhitespace(b.charAt(b.length() - 1)))
      b.setLength(b.length() - 1);
  }

  /** Match non-whitespace characters in order. ~ explicitly marks unmatched lines. */
  public static String[] sourceLines(String original, String formatted) {
    String[] rows = normalize(formatted).split("\n", -1), labels = new String[rows.length];
    if (normalize(original).equals(normalize(formatted))) {
      for (int i = 0; i < rows.length; i++) labels[i] = "" + (i + 1);
      return labels;
    }
    int offset = 0, line = 1;
    for (int i = 0; i < rows.length; i++) {
      int first = 0, last = 0;
      boolean approximate = false;
      for (char c : rows[i].toCharArray()) {
        if (Character.isWhitespace(c)) continue;
        while (offset < original.length() && Character.isWhitespace(original.charAt(offset))) {
          if (original.charAt(offset++) == '\n') line++;
        }
        if (offset < original.length() && original.charAt(offset) == c) {
          if (first == 0) first = line;
          last = line;
          offset++;
        } else approximate = true;
      }
      labels[i] =
          (approximate || first == 0 ? "~" : "")
              + (first == 0 ? line : first)
              + (last > first && first != 0 ? "–" + last : "");
    }
    return labels;
  }

  public static String snippet(
      String text,
      String name,
      int from,
      int to,
      String[] labels,
      boolean numbered,
      boolean fenced) {
    String[] lines = normalize(text).split("\n", -1);
    if (from < 1 || to < from || to > lines.length)
      throw new IllegalArgumentException("Choose lines 1–" + lines.length);
    StringBuilder body = new StringBuilder();
    int longest = 0, run = 0;
    for (int i = from - 1; i < to; i++) {
      if (i >= from) body.append('\n');
      if (numbered) body.append(labels == null ? i + 1 : labels[i]).append(" | ");
      body.append(lines[i]);
    }
    for (int i = 0; i < body.length(); i++) {
      run = body.charAt(i) == '`' ? run + 1 : 0;
      longest = Math.max(longest, run);
    }
    if (!fenced) return body.toString();
    String fence = "`".repeat(Math.max(3, longest + 1));
    return name + " · lines " + from + "–" + to + "\n" + fence + "\n" + body + "\n" + fence;
  }
}
