package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class StructuredTreeTest {
  private static JsonTree.Node kid(JsonTree.Node node, String key) {
    for (JsonTree.Node child : node.children) if (child.key.equals(key)) return child;
    fail("no child " + key + " under " + node.key);
    return null;
  }

  @Test
  public void yamlBecomesTypedTree() throws Exception {
    JsonTree.Node root =
        StructuredTree.yaml("name: ci\nretries: 3\nfast: true\nnothing: null\nsteps:\n  - run: make\n  - run: test\n");
    assertEquals("object", root.type);
    assertTrue(root.expanded);
    assertEquals("string", kid(root, "name").type);
    assertEquals("number", kid(root, "retries").type);
    assertEquals("boolean", kid(root, "fast").type);
    assertEquals("null", kid(root, "nothing").type);
    JsonTree.Node steps = kid(root, "steps");
    assertEquals("array", steps.type);
    assertEquals(2, steps.children.size());
    assertEquals("make", kid(steps.children.get(0), "run").value);
    assertEquals("$[\"steps\"][1][\"run\"]", kid(steps.children.get(1), "run").path);
  }

  @Test
  public void yamlMultipleDocumentsAndErrors() throws Exception {
    JsonTree.Node root = StructuredTree.yaml("a: 1\n---\nb: 2\n");
    assertEquals("array", root.type);
    assertEquals("document 2", root.children.get(1).key);
    try {
      StructuredTree.yaml("a: [1, 2\n");
      fail("malformed YAML must be rejected");
    } catch (IOException expected) {
      assertTrue(expected.getMessage().startsWith("not valid YAML"));
    }
    try {
      StructuredTree.yaml("   \n");
      fail("empty YAML must be rejected");
    } catch (IOException expected) {
    }
  }

  @Test
  public void yamlAliasBombIsRefused() {
    StringBuilder bomb = new StringBuilder("a: &a [x,x,x,x,x,x,x,x,x,x]\n");
    String previous = "a";
    for (char c = 'b'; c <= 'j'; c++) {
      bomb.append(c).append(": &").append(c).append(" [");
      for (int i = 0; i < 10; i++) bomb.append(i == 0 ? "" : ",").append('*').append(previous);
      bomb.append("]\n");
      previous = String.valueOf(c);
    }
    try {
      StructuredTree.yaml(bomb.toString());
      fail("exponential alias expansion must be refused");
    } catch (IOException | RuntimeException expected) {
    }
  }

  @Test
  public void tomlTablesArraysAndInlineValues() throws Exception {
    JsonTree.Node root =
        StructuredTree.toml(
            "title = \"demo\" # comment\n"
                + "[package]\nname = \"codefile\"\nversion = 26\nstable = false\nreleased = 2026-10-16\n"
                + "tags = [\n  \"a\",\n  \"b\", # trailing\n]\n"
                + "[dependencies]\nserde = { version = \"1\", features = [\"derive\"] }\n"
                + "[[bin]]\nname = \"one\"\n[[bin]]\nname = \"two\"\n"
                + "[server.http]\nport = 8080\nnote = \"\"\"\nline1\nline2\"\"\"\n"
                + "'quoted key'.sub = 'lit\\eral'\n");
    assertEquals("demo", kid(root, "title").value);
    JsonTree.Node pkg = kid(root, "package");
    assertEquals("number", kid(pkg, "version").type);
    assertEquals("boolean", kid(pkg, "stable").type);
    assertEquals("datetime", kid(pkg, "released").type);
    assertEquals(2, kid(pkg, "tags").children.size());
    JsonTree.Node serde = kid(kid(root, "dependencies"), "serde");
    assertEquals("object", serde.type);
    assertEquals("derive", kid(serde, "features").children.get(0).value);
    JsonTree.Node bins = kid(root, "bin");
    assertEquals("array", bins.type);
    assertEquals("two", kid(bins.children.get(1), "name").value);
    assertEquals("8080", kid(kid(root, "server"), "http").children.get(0).value);
    assertEquals("line1\nline2", kid(kid(kid(root, "server"), "http"), "note").value);
    assertEquals("lit\\eral", kid(kid(kid(kid(root, "server"), "http"), "quoted key"), "sub").value);
  }

  @Test
  public void tomlErrorsNameTheLine() {
    try {
      StructuredTree.toml("a = 1\nb = \"unclosed\nc = 3\n");
      fail("unclosed string");
    } catch (IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().startsWith("TOML line 2"));
    }
  }

  @Test
  public void iniAndPropertiesNestSensibly() throws Exception {
    JsonTree.Node ini = StructuredTree.ini("; c\ntop = 1\n[core]\nname = \"x\"\nmode: fast\n[paths]\nroot=/tmp\n");
    assertEquals("1", kid(ini, "top").value);
    assertEquals("x", kid(kid(ini, "core"), "name").value);
    assertEquals("fast", kid(kid(ini, "core"), "mode").value);
    assertEquals("/tmp", kid(kid(ini, "paths"), "root").value);

    JsonTree.Node props =
        StructuredTree.properties("# c\napp.name=demo\napp.version = 2\nserver.port:8080\nlong=a\\\n  b\nexport KEY=\"v\"\napp=flat\n");
    JsonTree.Node app = kid(props, "app");
    assertEquals("object", app.type);
    assertEquals("demo", kid(app, "name").value);
    assertEquals("number", kid(app, "version").type);
    assertEquals("8080", kid(kid(props, "server"), "port").value);
    assertEquals("ab", kid(props, "long").value);
    assertEquals("v", kid(props, "KEY").value);
    int flat = 0;
    for (JsonTree.Node child : props.children)
      if (child.key.equals("app") && child.type.equals("string")) flat++;
    assertEquals("a plain value that collides with a nested key is kept, not dropped", 1, flat);
  }

  @Test
  public void xmlBecomesElementTree() throws Exception {
    JsonTree.Node root =
        StructuredTree.xml("<?xml version=\"1.0\"?><!-- c --><cfg v=\"2\"><item id=\"1\">one</item><item id=\"2\"/><name>x &amp; y</name><![CDATA[raw]]></cfg>");
    assertEquals("element", root.type);
    assertEquals("cfg", root.key);
    assertEquals("2", kid(root, "@v").value);
    assertEquals("x & y", kid(root, "name").value);
    assertEquals("raw", kid(root, "#text").value);
    assertEquals("/cfg/item[1]", root.children.get(2).path);
    assertEquals("/cfg/item[2]", root.children.get(3).path);
    assertEquals("one", kid(root.children.get(2), "#text").value);
    try {
      StructuredTree.xml("<a><b></a>");
      fail("mismatched tags");
    } catch (IOException expected) {
      assertTrue(expected.getMessage().startsWith("not valid XML"));
    }
  }

  @Test
  public void jsonLinesAndJsonc() throws Exception {
    JsonTree.Node lines = StructuredTree.jsonLines("{\"a\":1}\n\n{\"a\":2}\n");
    assertEquals(2, lines.children.size());
    assertEquals("line 3", lines.children.get(1).key);
    assertEquals("2", kid(lines.children.get(1), "a").value);
    try {
      StructuredTree.jsonLines("{\"a\":1}\nnot json\n");
      fail("bad line");
    } catch (IOException expected) {
      assertEquals("line 2 is not valid JSON", expected.getMessage());
    }
    JsonTree.Node jsonc = StructuredTree.jsonc("{\n // note\n \"a\": [1, 2,], /* x */\n \"b\": \"//keep\",\n}");
    assertEquals(2, kid(jsonc, "a").children.size());
    assertEquals("//keep", kid(jsonc, "b").value);
  }
}
