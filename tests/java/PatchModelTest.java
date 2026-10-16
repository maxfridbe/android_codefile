package com.TechHurts.CodeFile;
public class PatchModelTest {
    static int checks = 0;
    static void eq(Object want, Object got, String what) { checks++; if (!String.valueOf(want).equals(String.valueOf(got))) throw new AssertionError(what + ": wanted " + want + " got " + got); }
    public static void main(String[] args) {
        String patch = "diff --git a/A.java b/A.java\nindex 1..2 100644\n--- a/A.java\n+++ b/A.java\n@@ -1,3 +1,4 @@\n keep\n-old\n+new\n+extra\n tail\n\\ No newline at end of file\ndiff --git a/B.txt b/B.txt\n--- a/B.txt\n+++ b/B.txt\n@@ -5 +5 @@\n--dash line\n+++plus line\n";
        PatchModel m = new PatchModel(patch);
        eq(2, m.files, "files");
        eq(3, m.added, "added");
        eq(2, m.removed, "removed");
        PatchModel.Row first = m.rows.get(5);
        eq(PatchModel.CONTEXT, first.kind, "context kind"); eq(1, first.oldNo, "context old"); eq(1, first.newNo, "context new");
        PatchModel.Row del = m.rows.get(6);
        eq(PatchModel.DEL, del.kind, "del kind"); eq(2, del.oldNo, "del old"); eq("old", del.text, "del text");
        PatchModel.Row add = m.rows.get(8);
        eq(PatchModel.ADD, add.kind, "add kind"); eq(3, add.newNo, "add new"); eq("extra", add.text, "add text");
        eq(PatchModel.META, m.rows.get(10).kind, "no newline marker");
        // content that looks like file markers inside a hunk is still content
        PatchModel.Row dash = m.rows.get(m.rows.size() - 2);
        eq(PatchModel.DEL, dash.kind, "dash line kind"); eq("-dash line", dash.text, "dash line text");
        PatchModel.Row plus = m.rows.get(m.rows.size() - 1);
        eq(PatchModel.ADD, plus.kind, "plus line kind"); eq("++plus line", plus.text, "plus line text");
        PatchModel plain = new PatchModel("--- a/x\n+++ b/x\n@@ -1 +1 @@\n-a\n+b\n");
        eq(1, plain.files, "plain diff files");
        eq(0, new PatchModel("").rows.size(), "empty");

        eq("{\"a\":1}", JsoncCleaner.clean("{\"a\":1}"), "unchanged");
        eq("{\n  \n  \"a\": 1\n}", JsoncCleaner.clean("{\n  // c\n  \"a\": 1,\n}"), "line comment and comma");
        eq("{\"u\":\"http://x//y /* z */\"}", JsoncCleaner.clean("{\"u\":\"http://x//y /* z */\"}"), "comment markers inside strings");
        eq("[1,2 ]", JsoncCleaner.clean("[1,2, ]"), "trailing comma in array");
        eq("{\"a\": 1   }", JsoncCleaner.clean("{\"a\": 1 /* x */ }"), "block comment");
        eq("{\"q\":\"a\\\"//b\"}", JsoncCleaner.clean("{\"q\":\"a\\\"//b\"}"), "escaped quote");
        System.out.println(checks + " patch and JSONC checks passed");
    }
}
