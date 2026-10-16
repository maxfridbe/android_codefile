package com.TechHurts.CodeFile;

/** Turns JSON with comments and trailing commas into strict JSON; strings are left untouched. */
final class JsoncCleaner {
  private JsoncCleaner() {}

  static String clean(String source) {
    return trailingCommas(comments(source));
  }

  private static String comments(String s) {
    StringBuilder out = new StringBuilder(s.length());
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == '"') {
        i = copyString(s, i, out);
      } else if (c == '/' && s.startsWith("//", i)) {
        while (i < s.length() && s.charAt(i) != '\n') i++;
      } else if (c == '/' && s.startsWith("/*", i)) {
        int end = s.indexOf("*/", i + 2);
        i = end < 0 ? s.length() : end + 2;
        out.append(' ');
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static int copyString(String s, int start, StringBuilder out) {
    int i = start;
    out.append(s.charAt(i++));
    while (i < s.length()) {
      char c = s.charAt(i++);
      out.append(c);
      if (c == '\\' && i < s.length()) out.append(s.charAt(i++));
      else if (c == '"') break;
    }
    return i;
  }

  private static String trailingCommas(String s) {
    StringBuilder out = new StringBuilder(s.length());
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c == '"') {
        i = copyString(s, i, out);
        continue;
      }
      if (c == ',' && closesNext(s, i + 1)) {
        i++;
        continue;
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static boolean closesNext(String s, int from) {
    int i = from;
    while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    return i < s.length() && (s.charAt(i) == '}' || s.charAt(i) == ']');
  }
}
