package com.TechHurts.CodeFile;

import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A link to one file on github.com, mapped to its raw-content URL. Independent of Android so the
 * policy can be tested on the JVM. Only public https content is ever requested.
 */
public final class GithubUrl {
  private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]+");
  private static final int MAX_SHARED_TEXT = 1000;

  /** https://raw.githubusercontent.com/... without query or fragment. */
  public final String raw;

  public final String owner, repo, ref, path, name;

  private GithubUrl(String raw, String owner, String repo, String ref, String path, String name) {
    this.raw = raw;
    this.owner = owner;
    this.repo = repo;
    this.ref = ref;
    this.path = path;
    this.name = name;
  }

  /** Short human label such as {@code owner/repo@main:src/App.java}. */
  public String label() {
    return owner + "/" + repo + "@" + ref + ":" + path;
  }

  /** Returns null when the value is not a github.com or raw.githubusercontent.com file link. */
  public static GithubUrl parse(String value) {
    try {
      return parseStrict(value == null ? "" : value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  /** The link in a short shared message, only when the message holds exactly one such link. */
  public static GithubUrl fromSharedText(String text) {
    if (text == null || text.length() > MAX_SHARED_TEXT) return null;
    GithubUrl found = null;
    for (String token : text.trim().split("\\s+")) {
      GithubUrl link = parse(token);
      if (link == null) continue;
      if (found != null && !found.raw.equals(link.raw)) return null;
      found = link;
    }
    return found;
  }

  private static GithubUrl parseStrict(String value) throws Exception {
    URI uri = new URI(value);
    String scheme = uri.getScheme();
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT);
    boolean web = host.equals("github.com") || host.equals("www.github.com");
    boolean raw = host.equals("raw.githubusercontent.com");
    boolean schemeOk = "https".equals(scheme) || "http".equals(scheme);
    boolean portOk = uri.getPort() == -1 || uri.getPort() == 443 || uri.getPort() == 80;
    if (!schemeOk || !(web || raw) || !portOk || uri.getUserInfo() != null) return null;
    List<String> raws = segments(uri.getRawPath());
    // github.com/{owner}/{repo}/(blob|raw)/{ref}/{path...}; raw host: /{owner}/{repo}/{ref}/{path...}
    int skip = web ? 1 : 0;
    if (web && !(raws.size() > 4 && (raws.get(2).equals("blob") || raws.get(2).equals("raw"))))
      return null;
    if (raw && raws.size() < 4) return null;
    List<String> parts = new ArrayList<>();
    for (String segment : raws) parts.add(decode(segment));
    String owner = parts.get(0), repo = parts.get(1), ref = parts.get(2 + skip);
    if (!NAME.matcher(owner).matches() || !NAME.matcher(repo).matches()) return null;
    StringBuilder rawPath = new StringBuilder();
    for (int i = 3 + skip; i < raws.size(); i++) rawPath.append('/').append(raws.get(i));
    String path = String.join("/", parts.subList(3 + skip, parts.size()));
    String name = parts.get(parts.size() - 1);
    String rawUrl =
        "https://raw.githubusercontent.com/"
            + raws.get(0)
            + "/"
            + raws.get(1)
            + "/"
            + raws.get(2 + skip)
            + rawPath;
    return new GithubUrl(rawUrl, owner, repo, ref, path, name);
  }

  private static List<String> segments(String rawPath) {
    List<String> out = new ArrayList<>();
    if (rawPath == null) return out;
    for (String piece : rawPath.split("/", -1)) if (!piece.isEmpty()) out.add(piece);
    return out;
  }

  private static String decode(String segment) throws Exception {
    String decoded = URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8");
    boolean bad =
        decoded.isEmpty()
            || decoded.equals(".")
            || decoded.equals("..")
            || decoded.indexOf('/') >= 0
            || decoded.indexOf('\\') >= 0;
    for (int i = 0; i < decoded.length() && !bad; i++)
      bad = Character.isISOControl(decoded.charAt(i));
    if (bad) throw new IllegalArgumentException("Unsafe path segment");
    return decoded;
  }
}
