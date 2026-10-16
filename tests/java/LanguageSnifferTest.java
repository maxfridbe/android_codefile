package com.TechHurts.CodeFile;
public class LanguageSnifferTest {
    static int checks = 0;
    static void is(String want, String code) {
        checks++;
        LanguageSniffer.Result r = LanguageSniffer.sniff(code);
        if (!want.equals(r.language)) throw new AssertionError("wanted " + want + " got " + r + " for:\n" + code);
    }
    static void family(String want, String code) {
        checks++;
        LanguageSniffer.Result r = LanguageSniffer.sniff(code);
        if (!want.equals(r.family)) throw new AssertionError("wanted family " + want + " got " + r + " for:\n" + code);
    }
    public static void main(String[] a) {
        is("java", "package demo;\n\nimport java.util.List;\n\npublic final class Inventory {\n  private final List<String> items = new java.util.ArrayList<>();\n  public static void main(String[] args) {\n    System.out.println(\"hi\");\n  }\n}\n");
        is("kotlin", "package demo\n\nimport kotlin.math.max\n\ndata class Item(val name: String, val qty: Int)\n\nfun total(items: List<Item>): Int {\n    return items.sumOf { it.qty }\n}\n\nfun main() {\n    println(total(listOf()))\n}\n");
        is("csharp", "using System;\n\nnamespace Demo\n{\n    public class Program\n    {\n        public string Name { get; set; }\n        static void Main(string[] args)\n        {\n            Console.WriteLine(\"Hello\");\n        }\n    }\n}\n");
        is("c", "#include <stdio.h>\n#include <stdlib.h>\n\nint main(int argc, char **argv) {\n    char *p = malloc(10);\n    printf(\"%s\\n\", p);\n    free(p);\n    return 0;\n}\n");
        is("cpp", "#include <iostream>\n#include <vector>\n\nusing namespace std;\n\nint main() {\n    std::vector<int> v;\n    cout << v.size() << endl;\n    return 0;\n}\n");
        is("javascript", "const fs = require('fs');\n\nfunction read(path) {\n  return fs.readFileSync(path, 'utf8');\n}\n\nconsole.log(read('a.txt'));\nmodule.exports = { read };\n");
        is("javascript", "import React from 'react';\n\nexport default function App() {\n  const [n, setN] = useState(0);\n  return n === 1 ? null : document.title;\n}\n");
        is("typescript", "interface User {\n  name: string;\n  age?: number;\n}\n\nexport type Id = string;\n\nexport function greet(u: User): string {\n  return `hi ${u.name}`;\n}\n");
        is("go", "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tx := 1\n\tif err != nil {\n\t\treturn\n\t}\n\tfmt.Println(x)\n}\n");
        is("rust", "use std::collections::HashMap;\n\n#[derive(Debug)]\nstruct Point { x: i32 }\n\nfn main() {\n    let mut m = HashMap::new();\n    println!(\"{:?}\", m);\n}\n\nimpl Point {\n    fn new() -> Self { Point { x: 0 } }\n}\n");
        is("swift", "import Foundation\n\nstruct Item: Codable {\n    let name: String\n}\n\nfunc load(path: String) -> [Item] {\n    guard let data = FileManager.default.contents(atPath: path) else { return [] }\n    return []\n}\n");
        is("python", "import os\nfrom typing import List\n\nclass Repo:\n    def __init__(self, path):\n        self.path = path\n\n    def files(self) -> List[str]:\n        return [f for f in os.listdir(self.path)]\n\nif __name__ == '__main__':\n    print(Repo('.').files())\n");
        is("python", "def add(a, b):\n    return a + b\n\nfor i in range(3):\n    print(add(i, 1))\n");
        is("ruby", "require 'json'\n\nclass Person\n  attr_accessor :name\n\n  def initialize(name)\n    @name = name\n  end\n\n  def greet\n    puts \"hi #{@name}\"\n  end\nend\n");
        is("shellscript", "#!/bin/bash\nset -e\nif [ -z \"$1\" ]; then\n  echo \"usage\"\n  exit 1\nfi\nfor f in $(ls); do\n  echo \"$f\"\ndone\n");
        is("shellscript", "if [[ -f \"$HOME/.bashrc\" ]]; then\n  source \"$HOME/.bashrc\"\nfi\nexport PATH=\"$PATH:/opt/bin\"\ncd /tmp && ls | grep foo\n");
        is("python", "#!/usr/bin/env python3\nprint('x')\n");
        is("javascript", "#!/usr/bin/env node\nconsole.log(1)\n");
        is("php", "<?php\n$a = 1;\necho $a;\n");
        is("lua", "local function add(a, b)\n  return a + b\nend\n\nlocal t = {}\nfor i, v in ipairs(t) do\n  print(v)\nend\nif x ~= nil then print(x) end\n");
        is("perl", "use strict;\nuse warnings;\n\nmy $name = shift;\nmy @items = (1, 2);\nsub greet {\n  print \"hi $name\\n\";\n}\n");
        is("r", "library(dplyr)\n\ndf <- data.frame(a = c(1, 2))\nres <- df %>% filter(a > 1)\nggplot(res)\n");
        is("powershell", "param([string]$Name)\n\nGet-ChildItem -Path . | Where-Object { $_.Length -gt 10 }\nWrite-Host \"Hello $Name\"\n");
        is("bat", "@echo off\nset NAME=world\necho Hello %NAME%\npause\n");
        is("sql", "SELECT u.id, u.name\nFROM users u\nLEFT JOIN orders o ON o.user_id = u.id\nWHERE u.active = 1\nORDER BY u.name;\n");
        is("sql", "CREATE TABLE items (\n  id INTEGER PRIMARY KEY,\n  name TEXT\n);\nINSERT INTO items VALUES (1, 'a');\n");
        is("css", "body {\n  margin: 0;\n  font-family: sans-serif;\n}\n\n.card > .title {\n  color: #333;\n  padding: 8px 12px;\n}\n\n@media (max-width: 600px) {\n  .card { display: none; }\n}\n");
        is("scss", "$primary: #333;\n\n@mixin round($r) {\n  border-radius: $r;\n}\n\n.btn {\n  color: $primary;\n  @include round(4px);\n  &:hover { color: red; }\n}\n");
        is("html", "<!DOCTYPE html>\n<html><body><p>hi</p></body></html>\n");
        is("html", "<div class=\"a\">\n  <span>hi</span>\n  <ul><li>x</li></ul>\n</div>\n");
        is("xml", "<?xml version=\"1.0\"?>\n<root><item id=\"1\"/></root>\n");
        is("xml", "<config>\n  <entry key=\"a\" value=\"b\"/>\n  <entry key=\"c\" value=\"d\"/>\n</config>\n");
        is("json", "{\"a\": [1, 2, {\"b\": null}], \"c\": \"d\"}");
        is("json", "[1, 2, 3]");
        is("jsonc", "{\n  // comment\n  \"a\": 1,\n  \"b\": [1, 2,],\n}\n");
        is("jsonl", "{\"a\":1}\n{\"a\":2}\n{\"a\":3}\n");
        is("yaml", "name: build\non:\n  push:\n    branches: [main]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - run: make\n");
        is("yaml", "---\n- name: a\n  value: 1\n- name: b\n  value: 2\n");
        is("dockercompose", "services:\n  web:\n    image: nginx\n    ports:\n      - \"80:80\"\n");
        is("ini", "[core]\nname = test\nmode = fast\n\n[paths]\nroot = /tmp\n");
        LanguageSniffer.Result toml = LanguageSniffer.sniff("[package]\nname = \"demo\"\nversion = \"0.1.0\"\n\n[dependencies]\nserde = \"1\"\n");
        checks++; if (!toml.language.equals("ini") || !toml.label.equals("TOML")) throw new AssertionError("toml: " + toml);
        is("properties", "DB_HOST=localhost\nDB_PORT=5432\nSECRET_KEY=abc\n");
        is("properties", "app.name=demo\napp.version=1\nserver.port=8080\n");
        is("markdown", "# Title\n\nSome **bold** text and a [link](https://x.y).\n\n- one\n- two\n\n```java\nint a;\n```\n");
        is("markdown", "## Notes\n\n| a | b |\n|---|---|\n| 1 | 2 |\n");
        is("diff", "diff --git a/x b/x\n--- a/x\n+++ b/x\n@@ -1,2 +1,2 @@\n-a\n+b\n");
        is("dockerfile", "FROM ubuntu:22.04\nRUN apt-get update\nCOPY . /app\nCMD [\"/app/run\"]\n");
        is("makefile", ".PHONY: all clean\n\nall: main.o\n\tcc -o app main.o\n\nclean:\n\trm -f app *.o\n");
        is("latex", "\\documentclass{article}\n\\begin{document}\nHi\n\\end{document}\n");
        is("log", "2026-10-16 10:00:01 INFO starting\n2026-10-16 10:00:02 WARN slow\n2026-10-16 10:00:03 ERROR failed\n");
        is("csv", "id,name,qty\n1,apple,3\n2,pear,4\n3,fig,5\n");
        is("tsv", "id\tname\n1\ta\n2\tb\n3\tc\n");
        is("csv", "a,b\n\"x, y\",2\n\"z\",3\n\"w\",4\n");
        is("plain", "");
        is("plain", "   \n\n");
        is("plain", "Just a sentence of ordinary prose, nothing more.\nAnother line of it.\n");
        is("plain", "hello");
        family("c-like", "package demo;\npublic class A { public static void main(String[] a) { System.out.println(1); } }\n");
        family("indent", "def f(x):\n    return x\n");
        family("script", "#!/bin/sh\necho hi\n");
        family("data", "{\"a\":1}");
        family("markup", "<!DOCTYPE html><html></html>");
        family("query", "SELECT a FROM b WHERE c = 1;");
        LanguageSniffer.Result bom = LanguageSniffer.sniff("\ufeff{\"a\":1}");
        checks++; if (!bom.language.equals("json")) throw new AssertionError("bom json: " + bom);
        checks++; if (!LanguageSniffer.fileName("clipboard", LanguageSniffer.sniff("import os\ndef f(x):\n    return x\n")).equals("clipboard.py")) throw new AssertionError("name py");
        checks++; if (!LanguageSniffer.fileName("c", LanguageSniffer.sniff("FROM a\nRUN b\n")).equals("Dockerfile")) throw new AssertionError("name docker");
        checks++; if (!LanguageSniffer.fileName("x", LanguageSniffer.sniff("[package]\nname = \"a\"\n")).equals("x.toml")) throw new AssertionError("name toml");
        checks++; if (!LanguageSniffer.fileName("x", LanguageSniffer.UNKNOWN).equals("x.txt")) throw new AssertionError("name txt");
        System.out.println(checks + " language sniffer checks passed");
    }
}
