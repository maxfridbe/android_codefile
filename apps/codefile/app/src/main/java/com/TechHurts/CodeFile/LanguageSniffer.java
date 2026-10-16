package com.TechHurts.CodeFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guesses a document's language from its text alone. Unambiguous markers (shebang, valid JSON, diff
 * headers, prologs) answer immediately. Everything else walks a small family tree: structural
 * traits pick a family (data, markup, C-like, indentation, shell/scripting, query, build), then
 * language signatures choose the leaf inside it. Pure Java so it can be tested on the JVM; result
 * keys match the bundled language catalog.
 */
public final class LanguageSniffer {
  /** A guess: catalog key, display label, family and 0-100 confidence. */
  public static final class Result {
    public final String language, label, family;
    public final int confidence;

    Result(String language, String label, String family, int confidence) {
      this.language = language;
      this.label = label;
      this.family = family;
      this.confidence = confidence;
    }

    public boolean known() {
      return !"plain".equals(language);
    }

    @Override
    public String toString() {
      return label + " (" + family + ", " + confidence + "%)";
    }
  }

  public static final Result UNKNOWN = new Result("plain", "Plain text", "text", 0);
  private static final int LIMIT = 16000, MIN_SCORE = 4;

  private static final class Rule {
    final Pattern pattern;
    final int weight, cap;

    Rule(Pattern pattern, int weight, int cap) {
      this.pattern = pattern;
      this.weight = weight;
      this.cap = cap;
    }
  }

  private static final class Lang {
    final String key, label, family;
    final List<Rule> rules = new ArrayList<>();

    Lang(String key, String label, String family) {
      this.key = key;
      this.label = label;
      this.family = family;
    }
  }

  private static final Map<String, Lang> LANGS = new LinkedHashMap<>();

  private static Lang lang(String key, String label, String family) {
    Lang lang = new Lang(key, label, family);
    LANGS.put(key, lang);
    return lang;
  }

  /** Each regex adds {@code weight} per match, up to {@code cap}; negatives count against it. */
  private static void rules(Lang lang, int weight, int cap, int flags, String... regexes) {
    for (String regex : regexes)
      lang.rules.add(new Rule(Pattern.compile(regex, flags | Pattern.MULTILINE), weight, cap));
  }

  private static void rules(Lang lang, int weight, int cap, String... regexes) {
    rules(lang, weight, cap, 0, regexes);
  }

  static {
    String jsCommon =
        "\\bfunction\\s*\\w*\\s*\\(";
    Lang java = lang("java", "Java", "c-like");
    rules(java, 3, 3, "^\\s*package\\s+[\\w.]+;");
    rules(java, 4, 8, "^\\s*import\\s+(java|javax|android|org|com)\\.[\\w.*]+;");
    rules(java, 2, 4, "\\bpublic\\s+(static\\s+)?(final\\s+)?(abstract\\s+)?(class|interface|enum|record)\\b");
    rules(java, 4, 4, "System\\.(out|err)\\.print");
    rules(java, 2, 4, "@Override\\b");
    rules(java, 4, 4, "\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String");
    rules(java, 1, 3, "\\bnew\\s+\\w+(<[\\w<>, ?]*>)?\\s*\\(");
    rules(java, 1, 3, "\\b(private|protected)\\s+(final\\s+)?\\w+(<[^>]*>)?\\s+\\w+\\s*[;=(]");
    rules(java, -3, 6, "\\bfun\\s+\\w+\\s*\\(");

    Lang kotlin = lang("kotlin", "Kotlin", "c-like");
    rules(kotlin, 3, 6, "^\\s*(override\\s+|private\\s+|internal\\s+|suspend\\s+)*fun\\s+[\\w.<>]+\\s*\\(");
    rules(kotlin, 1, 3, "\\b(val|var)\\s+\\w+\\s*(:\\s*[\\w<>?,. ]+)?\\s*=");
    rules(kotlin, 2, 2, "^\\s*package\\s+[\\w.]+\\s*$");
    rules(kotlin, 4, 4, "\\bdata\\s+class\\b", "\\bcompanion\\s+object\\b", "\\blateinit\\b");
    rules(kotlin, 4, 4, "\\boverride\\s+fun\\b", "^\\s*import\\s+(kotlin|kotlinx|androidx)\\.");
    rules(kotlin, 2, 2, "\\bwhen\\s*(\\(.*\\))?\\s*\\{", "\\?\\.\\w+|\\?:");
    rules(kotlin, 1, 2, "\\bprintln\\(");

    Lang csharp = lang("csharp", "C#", "c-like");
    rules(csharp, 5, 5, "^\\s*using\\s+System(\\.[\\w.]+)?;");
    rules(csharp, 3, 3, "^\\s*namespace\\s+[\\w.]+\\s*(\\{|;|$)");
    rules(csharp, 4, 4, "\\bConsole\\.(Write|Read)", "\\{\\s*get;\\s*(set;|init;)?\\s*\\}", "\\bnameof\\(");
    rules(csharp, 4, 4, "\\bstatic\\s+(void|async\\s+Task)\\s+Main\\s*\\(");
    rules(csharp, 1, 3, "\\b(public|private|internal)\\s+(static\\s+)?(async\\s+)?(sealed\\s+)?(class|struct|interface)\\b");
    rules(csharp, 1, 2, "\\bforeach\\s*\\(", "\\bvar\\s+\\w+\\s*=\\s*new\\b");

    Lang c = lang("c", "C", "c-like");
    rules(c, 5, 5, "^\\s*#include\\s*<(stdio|stdlib|string|stdint|stddef|math|unistd|ctype|assert|errno|time|stdbool)\\.h>");
    rules(c, 3, 3, "\\bint\\s+main\\s*\\(\\s*(void|int\\s+argc[^)]*)?\\)", "\\bprintf\\s*\\(", "\\btypedef\\s+(struct|enum)\\b");
    rules(c, 2, 4, "\\b(malloc|calloc|free|sizeof)\\s*\\(", "\\bNULL\\b", "\\bchar\\s*\\*\\s*\\w+");
    rules(c, 1, 2, "^\\s*#define\\s+\\w+", "\\bstruct\\s+\\w+\\s*\\{");
    rules(c, -3, 6, "\\bstd::|\\bnamespace\\b|\\bclass\\b|\\btemplate\\s*<");

    Lang cpp = lang("cpp", "C++", "c-like");
    rules(cpp, 6, 6, "^\\s*#include\\s*<(iostream|vector|string|map|memory|algorithm|cstdio|cstdlib|set|unordered_map|thread|functional|sstream|fstream)>");
    rules(cpp, 4, 8, "\\bstd::\\w+", "\\bcout\\s*<<|\\bcin\\s*>>", "\\busing\\s+namespace\\s+std\\b");
    rules(cpp, 3, 3, "\\btemplate\\s*<[^>]*>");
    rules(cpp, 2, 4, "\\bnamespace\\s+\\w+\\s*\\{", "\\b(nullptr|constexpr)\\b", "\\bclass\\s+\\w+(\\s*:\\s*(public|private|protected)\\s+\\w+)?\\s*\\{");
    rules(cpp, 2, 2, "^\\s*#include\\s*[<\"][\\w./]+(\\.h|\\.hpp)?[>\"]");
    rules(cpp, 1, 2, "\\bprintf\\s*\\(", "\\bint\\s+main\\s*\\(");

    Lang js = lang("javascript", "JavaScript", "c-like");
    jsRules(js, jsCommon);
    Lang ts = lang("typescript", "TypeScript", "c-like");
    jsRules(ts, jsCommon);
    rules(ts, 3, 6, ":\\s*(string|number|boolean|any|void|unknown|never)\\b");
    rules(ts, 4, 4, "^\\s*(export\\s+)?interface\\s+\\w+\\s*(<[^>]*>)?\\s*(extends\\s+[\\w, ]+\\s*)?\\{");
    rules(ts, 4, 4, "^\\s*(export\\s+)?type\\s+\\w+(<[^>]*>)?\\s*=", "^\\s*import\\s+type\\b");
    rules(ts, 2, 4, "\\b(private|public|protected|readonly)\\s+\\w+\\s*[:?]", "\\w+\\?:\\s*\\w+", "\\bas\\s+(const|\\w+)\\b");
    rules(ts, 1, 2, "\\benum\\s+\\w+\\s*\\{");

    Lang go = lang("go", "Go", "c-like");
    rules(go, 3, 3, "^\\s*package\\s+\\w+\\s*$");
    rules(go, 4, 8, "^\\s*func\\s+(\\(\\w+\\s+\\*?\\w+\\)\\s*)?\\w+\\s*\\(");
    rules(go, 3, 6, ":=");
    rules(go, 5, 5, "\\bfmt\\.(Print|Sprint|Errorf|Fprint)", "\\berr\\s*!=\\s*nil\\b", "^\\s*type\\s+\\w+\\s+(struct|interface)\\s*\\{");
    rules(go, 3, 3, "^\\s*import\\s+(\\(|\")", "\\bgo\\s+func\\b|\\bchan\\s+\\w+|\\bdefer\\s", "\\bmake\\(\\s*(\\[\\]|map|chan)");

    Lang rust = lang("rust", "Rust", "c-like");
    rules(rust, 4, 8, "\\bfn\\s+\\w+\\s*(<[^>]*>)?\\s*\\(");
    rules(rust, 2, 4, "\\blet\\s+(mut\\s+)?\\w+");
    rules(rust, 5, 5, "\\b(println|format|vec|panic|assert_eq)!\\s*[\\(\\[]", "^\\s*use\\s+(std|crate|super|self)::", "#\\[(derive|cfg|test|allow)\\b");
    rules(rust, 4, 4, "\\bimpl(\\s*<[^>]*>)?\\s+\\w+");
    rules(rust, 2, 4, "\\b(pub\\s+)?(struct|enum|trait)\\s+\\w+", "\\bmatch\\s+[\\w.&*]+\\s*\\{", "\\.unwrap\\(\\)|\\)\\?;");
    rules(rust, 1, 2, "->\\s*(Result|Option|Self|&|\\w+)", "\\bmod\\s+\\w+");

    Lang swift = lang("swift", "Swift", "c-like");
    rules(swift, 6, 6, "^\\s*import\\s+(Foundation|UIKit|SwiftUI|Combine|AppKit)\\b");
    rules(swift, 4, 8, "\\bfunc\\s+\\w+\\s*(<[^>]*>)?\\s*\\([^)]*\\)\\s*(->\\s*[\\w\\[\\]?<>:, ]+)?\\s*\\{");
    rules(swift, 5, 5, "\\bguard\\s+(let|var)\\b", "\\bif\\s+let\\b", "@(State|Published|ObservedObject|main|objc|IBOutlet)\\b");
    rules(swift, 2, 4, "\\b(let|var)\\s+\\w+\\s*:\\s*[\\w\\[\\]?<>:, ]+\\s*=", "\\bstruct\\s+\\w+\\s*:\\s*\\w+");
    rules(swift, 3, 3, "\\bextension\\s+\\w+");

    Lang php = lang("php", "PHP", "c-like");
    rules(php, 10, 10, "<\\?php");
    rules(php, 4, 4, "\\$this->");
    rules(php, 2, 6, "\\$\\w+\\s*=", "\\becho\\b", "\\barray\\(");
    rules(php, 1, 3, "->\\w+\\(");

    Lang dart = lang("dart", "Dart", "c-like");
    rules(dart, 6, 6, "^\\s*import\\s+'package:", "\\bWidget\\s+build\\(", "\\b(Stateless|Stateful)Widget\\b");
    rules(dart, 3, 3, "\\bvoid\\s+main\\s*\\(\\s*\\)");
    rules(dart, 2, 4, "\\b(late|required)\\s+\\w+", "\\bFuture<");

    Lang groovy = lang("groovy", "Groovy", "c-like");
    rules(groovy, 5, 5, "\\bpipeline\\s*\\{", "\\bimplementation\\s+['\"(]");
    rules(groovy, 4, 4, "^\\s*(task|apply plugin|dependencies|repositories|plugins)\\b");
    rules(groovy, 3, 3, "\\bdef\\s+\\w+\\s*=");

    Lang objc = lang("objective-c", "Objective-C", "c-like");
    rules(objc, 6, 6, "^\\s*#import\\s*[<\"]", "@interface\\b|@implementation\\b|@property\\b");
    rules(objc, 4, 4, "\\bNS[A-Z]\\w+\\b");

    Lang css = lang("css", "CSS", "c-like");
    rules(css, 3, 9, "^\\s*[.#]?[\\w-]+(\\s*[,>+~]\\s*[.#]?[\\w-]+)*\\s*\\{\\s*$");
    rules(css, 2, 8, "^\\s*[a-z-]+\\s*:\\s*[^;{}]+;\\s*$");
    rules(css, 4, 4, "@media\\b|@font-face|@keyframes|@import\\s+url", "\\bvar\\(--");
    rules(css, 2, 4, "\\b\\d+(px|em|rem|%|vh|vw)\\b");
    rules(css, 1, 2, "#[0-9a-fA-F]{3,8}\\b");
    rules(css, -4, 8, "\\b(function|return|const|let|class)\\b");
    Lang scss = lang("scss", "SCSS", "c-like");
    scss.rules.addAll(css.rules);
    rules(scss, 4, 4, "^\\s*\\$[\\w-]+\\s*:");
    rules(scss, 5, 5, "@mixin\\b|@include\\b|@use\\b|@extend\\b");
    rules(scss, 3, 3, "^\\s*&[:.\\-_]\\w*");
    Lang less = lang("less", "Less", "c-like");
    less.rules.addAll(css.rules);
    rules(less, 4, 4, "^\\s*@[\\w-]+\\s*:");
    rules(less, 2, 2, "^\\s*\\.[\\w-]+\\(.*\\)\\s*;");

    Lang python = lang("python", "Python", "indent");
    rules(python, 5, 10, "^\\s*def\\s+\\w+\\s*\\(.*\\)\\s*(->\\s*[^:]+)?:\\s*$");
    rules(python, 5, 5, "^\\s*class\\s+\\w+(\\(.*\\))?:\\s*$");
    rules(python, 3, 6, "^\\s*(import\\s+[\\w.]+(\\s+as\\s+\\w+)?\\s*$|from\\s+[\\w.]+\\s+import\\s+)");
    rules(python, 8, 8, "\\bif\\s+__name__\\s*==\\s*['\"]__main__['\"]");
    rules(python, 2, 4, "\\bself\\b");
    rules(python, 3, 3, "^\\s*(elif\\b.*|except\\b.*|finally|with\\s+.+\\s+as\\s+\\w+.*):\\s*$", "\\bin\\s+range\\(");
    rules(python, 2, 4, "\\b(None|True|False)\\b", "\\blambda\\b[^:\\n]*:", "\\bf\"[^\"\\n]*\\{");
    rules(python, 1, 2, "\\bprint\\(", "\"\"\"");
    rules(python, -3, 6, "^\\s*\\w+.*;\\s*$", "\\bfunction\\b|\\bconst\\b|\\bvar\\b");

    Lang shell = lang("shellscript", "Shell", "script");
    rules(shell, 2, 4, "\\$\\(.+\\)|\\$\\{\\w+");
    rules(shell, 4, 8, "^\\s*(if|elif)\\s+\\[\\[?\\s", "^\\s*(fi|done|esac)\\s*$");
    rules(shell, 2, 4, "\\bthen\\s*$", "\\[\\[");
    rules(shell, 2, 8, "^\\s*(echo|export|source|cd|mkdir|chmod|grep|sed|awk|curl|sudo|apt|set\\s+-[euxo]+)\\b");
    rules(shell, 1, 4, "\\$\\w+", "^\\s*\\w+=\\S*\\s*$", " \\| ");
    rules(shell, 3, 3, "^\\s*function\\s+\\w+\\s*(\\(\\))?\\s*\\{|^\\w+\\s*\\(\\)\\s*\\{", "\\$\\?|\\$@|\\$[1-9]");

    Lang ruby = lang("ruby", "Ruby", "script");
    rules(ruby, 3, 6, "^\\s*def\\s+\\w+[?!]?(\\(.*\\))?\\s*$");
    rules(ruby, 2, 4, "^\\s*end\\s*$");
    rules(ruby, 3, 3, "\\bputs\\b");
    rules(ruby, 4, 4, "\\brequire(_relative)?\\s+['\"]", "\\battr_(accessor|reader|writer)\\b", "\\bdo\\s*\\|\\w+(,\\s*\\w+)*\\|");
    rules(ruby, 3, 3, "^\\s*class\\s+\\w+(\\s*<\\s*\\w+(::\\w+)*)?\\s*$", ":\\w+\\s*=>");
    rules(ruby, 2, 4, "^\\s*module\\s+\\w+", "@\\w+\\s*=", "\\.each\\s+do|\\.map\\s*\\{");
    rules(ruby, 4, 4, "\\b(elsif|unless)\\b");

    Lang lua = lang("lua", "Lua", "script");
    rules(lua, 4, 8, "\\blocal\\s+(function\\s+)?\\w+");
    rules(lua, 3, 3, "~=");
    rules(lua, 2, 4, "\\b(ipairs|pairs|tostring|setmetatable)\\(");
    rules(lua, 1, 3, "^\\s*end\\s*$", "\\bthen\\s*$", "\\bnil\\b");

    Lang perl = lang("perl", "Perl", "script");
    rules(perl, 5, 5, "^\\s*use\\s+(strict|warnings|\\w+(::\\w+)*)\\s*;", "\\bmy\\s+[$@%]\\w+", "\\bsub\\s+\\w+\\s*\\{");
    rules(perl, 4, 4, "\\$\\w+\\s*=~", "\\bforeach\\s+my\\b");
    rules(perl, 2, 4, "@\\w+\\s*=", "\\$_\\b");

    Lang r = lang("r", "R", "script");
    rules(r, 4, 8, "\\w\\s*<-\\s*\\S");
    rules(r, 5, 5, "\\blibrary\\(\\w+\\)", "\\bggplot\\(");
    rules(r, 3, 3, "%>%|\\|>", "\\bdata\\.frame\\(");

    Lang ps = lang("powershell", "PowerShell", "script");
    rules(ps, 5, 10, "\\b(Get|Set|New|Remove|Write|Invoke|Import)-\\w+\\b");
    rules(ps, 4, 4, "\\bparam\\s*\\(", "\\|\\s*(Where|ForEach|Select|Sort)-Object\\b", "\\s-(eq|ne|gt|lt|like|match)\\s");
    rules(ps, 2, 4, "\\$\\w+\\s*=", "\\$_\\b");

    Lang bat = lang("bat", "Batch", "script");
    rules(bat, 3, 9, Pattern.CASE_INSENSITIVE, "^\\s*(set|goto|rem|call|if\\s+exist|pause|setlocal)\\b");
    rules(bat, 3, 6, "%\\w+%", "^\\s*:\\w+\\s*$");

    Lang vb = lang("vb", "Visual Basic", "script");
    rules(vb, 5, 10, Pattern.CASE_INSENSITIVE, "\\bDim\\s+\\w+\\s+As\\b", "^\\s*(Public|Private)\\s+(Sub|Function|Class)\\b", "\\bEnd\\s+(Sub|Function|If|Class)\\b", "^\\s*Imports\\s+System");

    Lang fsharp = lang("fsharp", "F#", "script");
    rules(fsharp, 4, 4, "^\\s*open\\s+System", "\\bmatch\\s+.*\\s+with\\b");
    rules(fsharp, 3, 3, "\\|>", "^\\s*module\\s+\\w+\\s*=");
    rules(fsharp, 2, 4, "^\\s*let\\s+(rec\\s+)?\\w+.*=\\s*$");

    Lang julia = lang("julia", "Julia", "script");
    rules(julia, 4, 4, "::\\s*(Int|Float64|String|Vector|Array)\\b");
    rules(julia, 3, 3, "^\\s*using\\s+\\w+", "^\\s*function\\s+\\w+\\(.*\\)\\s*$");
    rules(julia, 1, 2, "^\\s*end\\s*$");

    Lang sql = lang("sql", "SQL", "query");
    int ci = Pattern.CASE_INSENSITIVE;
    rules(sql, 5, 5, ci, "\\bSELECT\\b[\\s\\S]{0,400}?\\bFROM\\b", "\\bINSERT\\s+INTO\\b", "\\bCREATE\\s+(TABLE|INDEX|VIEW|DATABASE)\\b", "\\bUPDATE\\s+\\w+\\s+SET\\b", "\\bDELETE\\s+FROM\\b", "\\bALTER\\s+TABLE\\b");
    rules(sql, 2, 6, ci, "\\b(WHERE|GROUP\\s+BY|ORDER\\s+BY|LEFT\\s+JOIN|INNER\\s+JOIN|JOIN)\\b");

    Lang makefile = lang("makefile", "Makefile", "build");
    rules(makefile, 5, 5, "^\\.PHONY\\b");
    rules(makefile, 4, 4, "^(all|clean|install|test):");
    rules(makefile, 2, 6, "^\\t[^\\n]+", "^\\w+\\s*[:?+]?=\\s*");
    rules(makefile, 3, 3, "\\$\\(\\w+\\)");

    Lang yaml = lang("yaml", "YAML", "data");
    rules(yaml, 4, 4, "\\A---\\s*$");
    rules(yaml, 1, 8, "^\\s*[\\w\"'./-]+:\\s+\\S");
    rules(yaml, 1, 5, "^\\s*-\\s+\\S");
    rules(yaml, 1, 4, "^\\s*[\\w-]+:\\s*$");
    rules(yaml, -2, 6, "[{}]");
    rules(yaml, -3, 6, ";\\s*$");
    rules(yaml, -4, 8, "^\\s*(def|class|function|public|import|return)\\b");
    Lang compose = lang("dockercompose", "Docker Compose", "data");
    compose.rules.addAll(yaml.rules);
    rules(compose, 8, 8, "^services:\\s*$");
    rules(compose, 3, 3, "^\\s+(image|build|ports|volumes|depends_on|environment):");

    Lang ini = lang("ini", "INI", "data");
    rules(ini, 3, 9, "^\\[[\\w .:-]+\\]\\s*$");
    rules(ini, 1, 6, "^[\\w.-]+\\s*=\\s*\\S");
    rules(ini, -3, 6, "\\{|;\\s*$");
    Lang props = lang("properties", "Properties", "data");
    rules(props, 2, 8, "^[A-Z][A-Z0-9_]*=\\S*");
    rules(props, 1, 5, "^[\\w.-]+=[^\\s].*");
    rules(props, 2, 6, "^[a-z][\\w-]*(\\.[\\w-]+)+\\s*=");
    rules(props, -4, 8, "^\\[[\\w .:-]+\\]\\s*$");
    rules(props, -3, 6, "\\{|;\\s*$");

    Lang markdown = lang("markdown", "Markdown", "markup");
    rules(markdown, 5, 10, "^```");
    rules(markdown, 3, 9, "^#{1,6}\\s+[A-Za-z0-9`*\\[]");
    rules(markdown, 1, 4, "^\\s*[-*+]\\s+\\S");
    rules(markdown, 1, 3, "^\\s*\\d+\\.\\s+\\S");
    rules(markdown, 3, 6, "\\[[^\\]\\n]+\\]\\([^)\\n]+\\)", "!\\[[^\\]\\n]*\\]\\(");
    rules(markdown, 2, 4, "\\*\\*[^*\\n]+\\*\\*", "^>\\s");
    rules(markdown, 5, 5, "^\\|[\\s:|-]+\\|\\s*$");
    rules(markdown, 2, 2, "^(={3,}|-{3,})\\s*$");
    rules(markdown, -4, 8, ";\\s*$");

    Lang html = lang("html", "HTML", "markup");
    rules(html, 2, 10, "</?(div|span|p|a|ul|ol|li|table|tr|td|h[1-6]|body|head|html|script|style|meta|link|img|form|input|button|section|nav|header|footer)\\b[^>]*>");
    Lang xml = lang("xml", "XML", "markup");
    rules(xml, 1, 6, "<\\w+(:\\w+)?(\\s+[\\w:-]+=\"[^\"]*\")*\\s*/?>", "</\\w+(:\\w+)?>");
    rules(xml, -2, 6, "</?(div|span|p|a|body|html)\\b");

    lang("xsl", "XSLT", "markup");
    Lang tex = lang("latex", "LaTeX", "markup");
    rules(tex, 3, 9, "\\\\(documentclass|usepackage|begin|end|section|subsection|textbf|textit|item|cite|ref|label)\\b");
  }

  private static void jsRules(Lang lang, String function) {
    rules(lang, 2, 4, function);
    rules(lang, 2, 4, "\\b(const|let)\\s+[\\w{}\\[\\], ]+\\s*=");
    rules(lang, 1, 3, "=>");
    rules(lang, 4, 4, "console\\.(log|error|warn)\\(", "\\brequire\\(\\s*['\"]", "module\\.exports");
    rules(lang, 3, 3, "^\\s*import\\s+.+\\s+from\\s+['\"]", "^\\s*export\\s+(default|const|function|class)\\b");
    rules(lang, 2, 4, "document\\.|window\\.", "===|!==");
    rules(lang, 1, 3, "\\bawait\\b|\\basync\\b", "\\.then\\(", "\\bnew Promise\\b");
  }

  private static final Map<String, String> NAMES = new HashMap<>();

  static {
    String[] pairs = {
      "java=.java", "kotlin=.kt", "csharp=.cs", "c=.c", "cpp=.cpp", "javascript=.js",
      "typescript=.ts", "go=.go", "rust=.rs", "swift=.swift", "php=.php", "dart=.dart",
      "groovy=.groovy", "objective-c=.m", "css=.css", "scss=.scss", "less=.less",
      "python=.py", "shellscript=.sh", "ruby=.rb", "lua=.lua", "perl=.pl", "r=.r",
      "powershell=.ps1", "bat=.bat", "vb=.vb", "fsharp=.fs", "julia=.jl", "sql=.sql",
      "yaml=.yaml", "ini=.ini", "properties=.properties", "markdown=.md", "html=.html",
      "xml=.xml", "xsl=.xsl", "latex=.tex", "bibtex=.bib", "json=.json", "jsonc=.jsonc",
      "jsonl=.jsonl", "csv=.csv", "tsv=.tsv", "diff=.diff", "log=.log"
    };
    for (String pair : pairs) NAMES.put(pair.substring(0, pair.indexOf('=')), pair.substring(pair.indexOf('=') + 1));
  }

  /** A file name that suits the guess, such as {@code clipboard.py} or {@code Dockerfile}. */
  public static String fileName(String base, Result guess) {
    if (guess.label.equals("TOML")) return base + ".toml";
    if (guess.language.equals("dockerfile")) return "Dockerfile";
    if (guess.language.equals("makefile")) return "Makefile";
    if (guess.language.equals("dockercompose")) return "docker-compose.yml";
    return base + NAMES.getOrDefault(guess.language, ".txt");
  }

  private LanguageSniffer() {}

  public static Result sniff(String text) {
    if (text == null) return UNKNOWN;
    String all = text.startsWith("\ufeff") ? text.substring(1) : text;
    String sample = all.length() > LIMIT ? all.substring(0, LIMIT) : all;
    String trimmed = sample.trim();
    if (trimmed.isEmpty()) return UNKNOWN;
    Result definite = definitive(sample, trimmed);
    return definite != null ? definite : scored(sample);
  }

  // ---- stage 0: unambiguous markers -------------------------------------------------------

  private static Result hit(String key, int confidence) {
    Lang lang = LANGS.get(key);
    if (lang != null) return new Result(key, lang.label, lang.family, confidence);
    return new Result(key, key.toUpperCase(Locale.ROOT), "data", confidence);
  }

  private static Result definitive(String sample, String trimmed) {
    Result r = shebang(trimmed);
    if (r == null) r = prolog(trimmed);
    if (r == null) r = json(trimmed);
    if (r == null) r = markers(sample);
    if (r == null) r = table(sample);
    return r;
  }

  private static Result shebang(String trimmed) {
    if (!trimmed.startsWith("#!")) return null;
    int end = trimmed.indexOf('\n');
    String line = (end < 0 ? trimmed : trimmed.substring(0, end)).toLowerCase(Locale.ROOT);
    String[][] table = {
      {"python", "python"}, {"node", "javascript"}, {"deno", "javascript"}, {"bun", "javascript"},
      {"ruby", "ruby"}, {"perl", "perl"}, {"php", "php"}, {"lua", "lua"}, {"pwsh", "powershell"},
      {"powershell", "powershell"}, {"rscript", "r"}, {"bash", "shellscript"},
      {"zsh", "shellscript"}, {"dash", "shellscript"}, {"ksh", "shellscript"},
      {"/sh", "shellscript"}, {" sh", "shellscript"}
    };
    for (String[] row : table) if (line.contains(row[0])) return hit(row[1], 98);
    return null;
  }

  private static Result prolog(String trimmed) {
    String head = trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    String lower = head.toLowerCase(Locale.ROOT);
    if (lower.startsWith("<?php")) return hit("php", 99);
    if (lower.startsWith("<?xml"))
      return trimmed.contains("<xsl:") ? hit("xsl", 97) : hit("xml", 97);
    if (lower.startsWith("<!doctype html") || lower.startsWith("<html")) return hit("html", 97);
    if (lower.startsWith("<svg")) return hit("xml", 95);
    return null;
  }

  private static Result json(String trimmed) {
    char first = trimmed.charAt(0);
    if (first != '{' && first != '[') return null;
    if (new Json(trimmed, false).valid()) return hit("json", 99);
    if (jsonLines(trimmed)) return new Result("jsonl", "JSON Lines", "data", 96);
    if (new Json(trimmed, true).valid()) return new Result("jsonc", "JSON with comments", "data", 93);
    return null;
  }

  private static boolean jsonLines(String trimmed) {
    String[] lines = trimmed.split("\\R");
    int records = 0;
    for (String line : lines) {
      String t = line.trim();
      if (t.isEmpty()) continue;
      if (t.charAt(0) != '{' || !new Json(t, false).valid()) return false;
      records++;
    }
    return records >= 2;
  }

  private static final Pattern DIFF_HUNK = Pattern.compile("^@@ -\\d+(,\\d+)? \\+\\d+", Pattern.MULTILINE);
  private static final Pattern DOCKER_LINE =
      Pattern.compile(
          "^(FROM|RUN|COPY|CMD|ENTRYPOINT|EXPOSE|ENV|WORKDIR|ADD|ARG|LABEL|USER|VOLUME|HEALTHCHECK)\\s",
          Pattern.MULTILINE);
  private static final Pattern LOG_LINE =
      Pattern.compile(
          "^(\\[?\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}|\\w{3}\\s+\\d+\\s\\d\\d:\\d\\d:\\d\\d|\\d{2}-\\d{2}\\s\\d\\d:\\d\\d:\\d\\d)");
  private static final Pattern LOG_LEVEL =
      Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|[VDIWEF]/)");

  private static Result markers(String sample) {
    if (Pattern.compile("^diff --git |^Index: |^--- .*\\n\\+\\+\\+ ", Pattern.MULTILINE).matcher(sample).find()
        || count(DIFF_HUNK, sample) >= 2) return hit("diff", 96);
    if (count(DOCKER_LINE, sample) >= 2 && Pattern.compile("^(FROM|ARG)\\s", Pattern.MULTILINE).matcher(sample).find())
      return new Result("dockerfile", "Dockerfile", "build", 94);
    if (Pattern.compile("^@echo\\s+off", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(sample).find())
      return hit("bat", 96);
    if (sample.contains("\\documentclass") || sample.contains("\\begin{document}")) return hit("latex", 96);
    if (Pattern.compile("^@(article|book|inproceedings|misc|phdthesis|techreport)\\s*\\{", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(sample).find())
      return new Result("bibtex", "BibTeX", "markup", 94);
    return logLines(sample);
  }

  private static Result logLines(String sample) {
    String[] lines = sample.split("\\R");
    int total = 0, stamped = 0, levels = 0;
    for (String line : lines) {
      if (line.trim().isEmpty()) continue;
      total++;
      if (LOG_LINE.matcher(line).find()) stamped++;
      if (LOG_LEVEL.matcher(line).find()) levels++;
    }
    boolean log = total >= 3 && stamped * 10 >= total * 7 && levels * 10 >= total * 4;
    return log ? new Result("log", "Log", "text", 90) : null;
  }

  private static int count(Pattern pattern, String text) {
    Matcher m = pattern.matcher(text);
    int n = 0;
    while (m.find() && n < 100) n++;
    return n;
  }

  /** Delimited text: the same number of fields on every one of several lines, and no code. */
  private static Result table(String sample) {
    List<String> lines = new ArrayList<>();
    for (String line : sample.split("\\R")) {
      if (!line.trim().isEmpty()) lines.add(line);
      if (lines.size() == 12) break;
    }
    if (lines.size() < 3) return null;
    for (String line : lines) if (line.matches(".*[{}()=<>].*")) return null;
    char[] delimiters = {'\t', ',', ';', '|'};
    for (char delimiter : delimiters) {
      int fields = fieldCount(lines.get(0), delimiter);
      if (fields < 2 || !sameWidth(lines, delimiter, fields)) continue;
      if (delimiter == ';' && lines.get(0).trim().endsWith(";")) continue;
      return delimiter == '\t' ? hit("tsv", 92) : new Result("csv", "CSV", "data", 88);
    }
    return null;
  }

  private static boolean sameWidth(List<String> lines, char delimiter, int fields) {
    for (String line : lines) if (fieldCount(line, delimiter) != fields) return false;
    return true;
  }

  private static int fieldCount(String line, char delimiter) {
    String bare = line.replaceAll("\"[^\"]*\"", "\"\"");
    int n = 1;
    for (int i = 0; i < bare.length(); i++) if (bare.charAt(i) == delimiter) n++;
    return n;
  }

  // ---- stage 1 + 2: family prior, then leaf language ---------------------------------------

  private static Result scored(String sample) {
    Map<String, Integer> prior = familyPrior(sample);
    Lang best = null, second = null;
    int bestScore = 0, secondScore = 0;
    for (Lang lang : LANGS.values()) {
      int score = own(lang, sample);
      if (score <= 0) continue;
      score += prior.getOrDefault(lang.family, 0);
      if (score > bestScore) {
        second = best;
        secondScore = bestScore;
        best = lang;
        bestScore = score;
      } else if (score > secondScore) {
        second = lang;
        secondScore = score;
      }
    }
    if (best == null || bestScore < MIN_SCORE) return UNKNOWN;
    int margin = second == null ? bestScore : bestScore - secondScore;
    int confidence = Math.max(10, Math.min(95, 25 + 3 * bestScore + 4 * margin));
    return refine(best, sample, confidence);
  }

  /** TOML rides on the INI grammar; say so in the label. */
  private static Result refine(Lang best, String sample, int confidence) {
    boolean toml =
        best.key.equals("ini")
            && (Pattern.compile("^\\[\\[[\\w.-]+\\]\\]", Pattern.MULTILINE).matcher(sample).find()
                || Pattern.compile("^[\\w.-]+\\s*=\\s*(\"|\\[|true\\b|false\\b)", Pattern.MULTILINE).matcher(sample).find());
    if (toml) return new Result("ini", "TOML", "data", confidence);
    return new Result(best.key, best.label, best.family, confidence);
  }

  private static int own(Lang lang, String sample) {
    int total = 0;
    for (Rule rule : lang.rules) {
      int n = count(rule.pattern, sample);
      total += Math.max(-rule.cap, Math.min(rule.cap, n * rule.weight));
    }
    return total;
  }

  /** Structural traits that say which branch of the tree to climb, before any keyword is read. */
  private static Map<String, Integer> familyPrior(String sample) {
    Map<String, Integer> prior = new HashMap<>();
    int semis = count(Pattern.compile(";\\s*$", Pattern.MULTILINE), sample);
    int braces = count(Pattern.compile("[{}]"), sample);
    int colons = count(Pattern.compile(":\\s*$", Pattern.MULTILINE), sample);
    int keyValues = count(Pattern.compile("^\\s*[\\w\"'.-]+\\s*[:=]\\s*\\S", Pattern.MULTILINE), sample);
    int tags = count(Pattern.compile("</?\\w+[^>]*>"), sample);
    int lines = Math.max(1, sample.split("\\R").length);
    if (semis >= 3 || braces >= 4) prior.put("c-like", 3);
    if (colons >= 2 && braces == 0 && semis == 0) prior.put("indent", 4);
    if (count(Pattern.compile("\\$\\w+|^\\s*(fi|done|esac|end)\\s*$", Pattern.MULTILINE), sample) >= 3)
      prior.put("script", 3);
    if (keyValues * 10 >= lines * 6 && braces == 0 && semis == 0) prior.put("data", 3);
    if (tags >= 3) prior.put("markup", 3);
    if (count(Pattern.compile("^\\s*(SELECT|INSERT|CREATE|UPDATE|DELETE)\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE), sample) >= 1)
      prior.put("query", 2);
    return prior;
  }

  /** Small strict JSON checker; lenient mode also allows comments and trailing commas. */
  private static final class Json {
    private final String s;
    private final boolean lenient;
    private int i;

    Json(String s, boolean lenient) {
      this.s = s;
      this.lenient = lenient;
    }

    boolean valid() {
      skip();
      boolean ok = value(0);
      skip();
      return ok && i == s.length();
    }

    private void skip() {
      while (i < s.length()) {
        char c = s.charAt(i);
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
        else if (!lenient || !comment()) return;
      }
    }

    private boolean comment() {
      if (s.startsWith("//", i)) {
        int end = s.indexOf('\n', i);
        i = end < 0 ? s.length() : end + 1;
        return true;
      }
      if (!s.startsWith("/*", i)) return false;
      int end = s.indexOf("*/", i + 2);
      i = end < 0 ? s.length() : end + 2;
      return true;
    }

    private boolean value(int depth) {
      if (depth > 200 || i >= s.length()) return false;
      char c = s.charAt(i);
      if (c == '{') return container(depth, '}', true);
      if (c == '[') return container(depth, ']', false);
      if (c == '"') return string();
      if (s.startsWith("true", i)) return advance(4);
      if (s.startsWith("false", i)) return advance(5);
      if (s.startsWith("null", i)) return advance(4);
      return number();
    }

    private boolean advance(int n) {
      i += n;
      return true;
    }

    private boolean container(int depth, char close, boolean object) {
      i++;
      skip();
      if (i < s.length() && s.charAt(i) == close) return advance(1);
      while (i < s.length()) {
        if (!member(depth, object)) return false;
        skip();
        if (i >= s.length()) return false;
        char c = s.charAt(i++);
        if (c == close) return true;
        if (c != ',') return false;
        skip();
        if (lenient && i < s.length() && s.charAt(i) == close) return advance(1);
      }
      return false;
    }

    private boolean member(int depth, boolean object) {
      skip();
      if (object) {
        if (i >= s.length() || s.charAt(i) != '"' || !string()) return false;
        skip();
        if (i >= s.length() || s.charAt(i++) != ':') return false;
        skip();
      }
      return value(depth + 1);
    }

    private boolean string() {
      i++;
      while (i < s.length()) {
        char c = s.charAt(i++);
        if (c == '"') return true;
        if (c == '\n' || c < ' ') return false;
        if (c == '\\') i++;
      }
      return false;
    }

    private boolean number() {
      int start = i;
      if (i < s.length() && s.charAt(i) == '-') i++;
      int digits = 0;
      while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) {
        if (Character.isDigit(s.charAt(i))) digits++;
        i++;
      }
      return digits > 0 && i > start;
    }
  }
}
