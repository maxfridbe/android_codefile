package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TextFormatsTest {
  @Test
  public void yamlRetainsCommentsQuotedScalarsAndBlockContent() {
    String source =
        "# configuration\n"
            + "service: {port: 8080, name: 'true'} # keep me\n"
            + "script: |\n"
            + "  echo hello\n"
            + "  echo world\n";
    String formatted = TextFormats.yaml(source, 2);
    assertTrue(formatted.contains("# configuration"));
    assertTrue(formatted.contains("# keep me"));
    assertTrue(formatted.contains("'true'"));
    assertTrue(formatted.contains("port: 8080"));
    assertTrue(TextFormats.yaml("service:\n    port: 8080\n", 2).contains("\n  port: 8080"));
    assertTrue(formatted.contains("echo hello\n"));
    assertEquals(formatted, TextFormats.yaml(formatted, 2));
  }

  @Test
  public void yamlRetainsMultiDocumentAndAliases() {
    String source = "base: &base {x: 1}\ncopy: *base\n---\nvalue: !custom tagged\n";
    String result = TextFormats.yaml(source, 2);
    assertEquals(2, result.split("---", -1).length - 1);
    assertTrue(result.contains("!custom"));
    assertTrue(result.contains("*"));
    assertTrue(result.contains("&"));
  }

  @Test
  public void invalidYamlIsRejected() {
    assertThrows(RuntimeException.class, () -> TextFormats.yaml("a: [broken", 2));
  }

  @Test
  public void csvRoundTripPreservesAllFieldValues() {
    String source = "name,value,empty\r\n\"a,b\",\"line1\r\nline2\",\r\nquote,\"a\"\"b\",x\r\n";
    List<List<String>> records = TextFormats.delimited(source, ',');
    assertEquals(3, records.size());
    assertEquals("line1\r\nline2", records.get(1).get(1));
    assertEquals("a\"b", records.get(2).get(1));
    assertEquals(records, TextFormats.delimited(TextFormats.csv(records, ','), ','));
  }

  @Test
  public void tsvKeepsLeadingZerosAndSpaces() {
    String source = "id\tname\n001\t A \n";
    assertEquals(source, TextFormats.csv(TextFormats.delimited(source, '\t'), '\t'));
  }

  @Test
  public void invalidCsvQuoteFailsInsteadOfLosingData() {
    assertThrows(IllegalArgumentException.class, () -> TextFormats.delimited("a,\"broken", ','));
    assertThrows(IllegalArgumentException.class, () -> TextFormats.delimited("\"a\"junk,b", ','));
  }

  @Test
  public void jsonLinesPreserveLargeNumbersAndStringWhitespace() throws Exception {
    String input = "{ \"n\": 123456789012345678901234567890, \"s\": \"a b\" }\n";
    assertEquals(
        "{\"n\":123456789012345678901234567890,\"s\":\"a b\"}\n",
        TextFormats.format("data.jsonl", "jsonl", input, 2));
  }

  @Test
  public void sensitiveTextWhitespaceIsPreserved() throws Exception {
    assertEquals("text  \n", TextFormats.format("readme.md", "markdown", "text  \r\n", 2));
  }

  @Test
  public void xmlProtectsMixedContentAndRejectsDoctype() throws Exception {
    String mixed = "<p>hello <b>world</b> !</p>";
    assertEquals(mixed, TextFormats.xml(mixed, 2));
    assertThrows(
        Exception.class, () -> TextFormats.xml("<!DOCTYPE a SYSTEM 'file:///etc/passwd'><a/>", 2));
  }
}
