package com.TechHurts.CodeFile;
public class GithubUrlTest {
    static int checks = 0;
    static void eq(Object want, Object got, String what) { checks++; if (!String.valueOf(want).equals(String.valueOf(got))) throw new AssertionError(what + ": wanted " + want + " got " + got); }
    static void accept(String url, String raw, String name) { GithubUrl g = GithubUrl.parse(url); if (g == null) throw new AssertionError("Rejected: " + url); eq(raw, g.raw, url); eq(name, g.name, url + " name"); }
    static void reject(String url) { checks++; if (GithubUrl.parse(url) != null) throw new AssertionError("Accepted: " + url); }
    public static void main(String[] args) {
        accept("https://github.com/maxfridbe/android_codefile/blob/main/apps/codefile/build.gradle.kts",
            "https://raw.githubusercontent.com/maxfridbe/android_codefile/main/apps/codefile/build.gradle.kts", "build.gradle.kts");
        accept("https://github.com/o/r/blob/v1.2/src/A.java#L10-L20", "https://raw.githubusercontent.com/o/r/v1.2/src/A.java", "A.java");
        accept("https://github.com/o/r/blob/main/README.md?plain=1", "https://raw.githubusercontent.com/o/r/main/README.md", "README.md");
        accept("https://github.com/o/r/raw/abc123/dir/f.py", "https://raw.githubusercontent.com/o/r/abc123/dir/f.py", "f.py");
        accept("https://raw.githubusercontent.com/o/r/main/x/y.json", "https://raw.githubusercontent.com/o/r/main/x/y.json", "y.json");
        accept("https://www.GitHub.com/o/r/blob/main/a%20b.txt", "https://raw.githubusercontent.com/o/r/main/a%20b.txt", "a b.txt");
        accept("http://github.com/o/r/blob/main/a.c", "https://raw.githubusercontent.com/o/r/main/a.c", "a.c");
        GithubUrl g = GithubUrl.parse("  https://github.com/o/r/blob/main/src/A.java  ");
        eq("o/r@main:src/A.java", g.label(), "label");
        reject(null);
        reject("");
        reject("https://github.com/o/r");
        reject("https://github.com/o/r/blob/main");
        reject("https://github.com/o/r/tree/main/src");
        reject("https://github.com/o/r/issues/1");
        reject("https://github.com/o/r/blob/main/../../etc/passwd");
        reject("https://github.com/o/r/blob/main/a/%2e%2e/b");
        reject("https://github.com/o/r/blob/main/a%2Fb");
        reject("https://github.com/o/r/blob/main/a%00b");
        reject("https://github.com/o r/blob/main/a");
        reject("https://github.com/o/r%2Fx/blob/main/a");
        reject("https://user:pw@github.com/o/r/blob/main/a");
        reject("https://github.com:8443/o/r/blob/main/a");
        reject("https://evilgithub.com/o/r/blob/main/a");
        reject("https://github.com.evil.io/o/r/blob/main/a");
        reject("https://gist.github.com/o/abc");
        reject("https://raw.githubusercontent.com/o/r/main");
        reject("ftp://github.com/o/r/blob/main/a");
        reject("file:///o/r/blob/main/a");
        reject("content://github.com/o/r/blob/main/a");
        reject("not a url");
        eq("o/r@main:a.txt", GithubUrl.fromSharedText("https://github.com/o/r/blob/main/a.txt").label(), "shared bare");
        eq("o/r@main:a.txt", GithubUrl.fromSharedText("Look at this\nhttps://github.com/o/r/blob/main/a.txt").label(), "shared with words");
        eq(null, GithubUrl.fromSharedText("https://github.com/o/r/blob/main/a.txt https://github.com/o/r/blob/main/b.txt"), "two links");
        eq(null, GithubUrl.fromSharedText("just some text"), "no link");
        eq(null, GithubUrl.fromSharedText("x ".repeat(600) + "https://github.com/o/r/blob/main/a.txt"), "long text");
        System.out.println(checks + " GitHub URL checks passed");
    }
}
