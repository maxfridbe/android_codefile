package com.TechHurts.CodeFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A unified diff or patch file as numbered rows; independent of Android for JVM tests. */
final class PatchModel {
  static final int FILE = 0, HUNK = 1, ADD = 2, DEL = 3, CONTEXT = 4, META = 5;
  private static final int MAX_ROWS = 20000;
  private static final Pattern HUNK_HEADER =
      Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");

  static final class Row {
    final int kind, oldNo, newNo;
    final String text;

    Row(int kind, int oldNo, int newNo, String text) {
      this.kind = kind;
      this.oldNo = oldNo;
      this.newNo = newNo;
      this.text = text;
    }
  }

  final List<Row> rows = new ArrayList<>();
  int files, added, removed;

  PatchModel(String patch) {
    if (patch.length() > 1048576) throw new IllegalArgumentException("Patch view supports 1 MiB");
    String[] lines = SourceTools.normalize(patch).split("\n", -1);
    int oldNo = 0, newNo = 0, oldLeft = 0, newLeft = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (i == lines.length - 1 && line.isEmpty()) break;
      if (rows.size() >= MAX_ROWS) throw new IllegalArgumentException("Patch view supports 20,000 lines");
      boolean inHunk = oldLeft > 0 || newLeft > 0;
      if (inHunk && !line.startsWith("\\")) {
        char c = line.isEmpty() ? ' ' : line.charAt(0);
        if (c == '+') {
          rows.add(new Row(ADD, 0, newNo++, line.substring(1)));
          newLeft--;
          added++;
          continue;
        }
        if (c == '-') {
          rows.add(new Row(DEL, oldNo++, 0, line.substring(1)));
          oldLeft--;
          removed++;
          continue;
        }
        rows.add(new Row(CONTEXT, oldNo++, newNo++, line.isEmpty() ? "" : line.substring(1)));
        oldLeft--;
        newLeft--;
        continue;
      }
      if (line.startsWith("\\")) {
        rows.add(new Row(META, 0, 0, line));
        continue;
      }
      Matcher hunk = HUNK_HEADER.matcher(line);
      if (hunk.find()) {
        oldNo = Integer.parseInt(hunk.group(1));
        newNo = Integer.parseInt(hunk.group(3));
        oldLeft = hunk.group(2) == null ? 1 : Integer.parseInt(hunk.group(2));
        newLeft = hunk.group(4) == null ? 1 : Integer.parseInt(hunk.group(4));
        rows.add(new Row(HUNK, 0, 0, line));
        continue;
      }
      boolean fileHeader = line.startsWith("diff --git ") || line.startsWith("Index: ");
      boolean marker = line.startsWith("--- ") || line.startsWith("+++ ");
      if (line.startsWith("diff --git ") || line.startsWith("+++ ") && !seenGitHeader(i, lines)) files++;
      rows.add(new Row(fileHeader || marker ? FILE : META, 0, 0, line));
    }
  }

  /** A +++ line only starts a new file when no "diff --git" header introduced it. */
  private static boolean seenGitHeader(int index, String[] lines) {
    for (int i = index - 1; i >= 0 && index - i <= 6; i--)
      if (lines[i].startsWith("diff --git ")) return true;
    return false;
  }
}
