package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LocalLanguageServiceTest {
  @Test
  public void javaResolvesLocalVariableInsteadOfSameNamedField() throws Exception {
    String source =
        "class Demo {\n"
            + " int value;\n"
            + " void run() {\n"
            + " int value=1;\n"
            + " System.out.println(value);\n"
            + " }\n"
            + "}\n";
    LocalLanguageService service =
        new LocalLanguageService("Demo.java", "java", source, "file:///Demo.java");
    assertTrue(service.description().contains("AST"));
    JSONArray result =
        (JSONArray)
            service.handle(
                "textDocument/definition",
                NativeLsp.object("position", NativeLsp.object("line", 4, "character", 20)));
    assertEquals(1, result.length());
    assertEquals(
        3, result.getJSONObject(0).getJSONObject("range").getJSONObject("start").getInt("line"));
  }

  @Test
  public void embeddedLspStartsWithoutNetworkConfiguration() throws Exception {
    try (NativeLsp server = new NativeLsp()) {
      server.startLocal("Demo.java", "java", "class Demo { void run() {} }");
      JSONArray symbols =
          server.documentRequest("documentSymbol", new JSONObject()).getJSONArray("result");
      assertTrue(symbols.length() >= 2);
      assertTrue(server.capabilities.getBoolean("definitionProvider"));
    }
  }

  @Test
  public void otherLanguagesAdvertiseLexicalFallback() throws Exception {
    LocalLanguageService service =
        new LocalLanguageService(
            "demo.py", "python", "def hello():\n return 1\n", "file:///demo.py");
    assertTrue(service.description().contains("lexical"));
    assertEquals(
        "hello",
        ((JSONArray) service.handle("documentSymbol", new JSONObject()))
            .getJSONObject(0)
            .getString("name"));
  }
}
