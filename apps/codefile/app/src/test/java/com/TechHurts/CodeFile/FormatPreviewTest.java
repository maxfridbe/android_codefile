package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import android.graphics.Typeface;
import android.text.Spanned;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class FormatPreviewTest {
  @Test
  public void treePreservesNumbersAndNestedTypes() throws Exception {
    JsonTree.Node root =
        JsonTree.parse(
            "{\"id\":12345678901234567890123,\"items\":[true,null,{\"name\":\"café\"}]}");
    assertEquals("12345678901234567890123", root.children.get(0).value);
    JsonTree.Node items = root.children.get(1);
    assertEquals("array", items.type);
    assertEquals("null", items.children.get(1).type);
    assertEquals("café", items.children.get(2).children.get(0).value);
    assertEquals("$[\"items\"][2][\"name\"]", items.children.get(2).children.get(0).path);
    assertEquals("42", JsonTree.parse("42").value);
  }

  @Test
  public void treeRejectsMalformedAndMultipleRoots() {
    for (String bad : new String[] {"{bad:1}", "[1,]", "{} {}", "true,false", ""}) {
      try {
        JsonTree.parse(bad);
        fail("Accepted invalid JSON: " + bad);
      } catch (Exception expected) {
      }
    }
  }

  @Test
  public void markdownRendersNativeSpansAndExtensions() throws Exception {
    io.noties.markwon.Markwon renderer =
        MarkdownPreview.renderer(
            RuntimeEnvironment.getApplication(),
            AppTheme.resolve("Carbon", false),
            Typeface.MONOSPACE);
    Spanned result =
        renderer.toMarkdown(
            "# Heading\n\n"
                + "**bold** and ~~gone~~\n\n"
                + "- [x] Done\n\n"
                + "```java\n"
                + "int x = 1;\n"
                + "```\n\n"
                + "| A | B |\n"
                + "|---|---|\n"
                + "| 1 | 2 |\n");
    assertTrue(result.toString().contains("Heading"));
    assertFalse(result.toString().contains("**bold**"));
    assertTrue(result.toString().contains("int x = 1;"));
    assertTrue(result.getSpans(0, result.length(), Object.class).length > 4);
    android.widget.TextView preview =
        MarkdownPreview.view(
            RuntimeEnvironment.getApplication(), AppTheme.resolve("Carbon", false));
    renderer.setParsedMarkdown(preview, result);
    screenshot(preview, "native-markdown.png");
  }

  @Test
  public void treeRowsExpandAndCollapse() throws Exception {
    JsonTree.Node root = JsonTree.parse("{\"items\":[1,2]}");
    JsonTreeView view =
        new JsonTreeView(
            RuntimeEnvironment.getApplication(),
            root,
            AppTheme.resolve("Carbon", false),
            (glyph, text) -> text,
            node -> {});
    assertEquals(2, view.getAdapter().getCount());
    view.performItemClick(null, 1, 1);
    assertEquals(4, view.getAdapter().getCount());
    screenshot(view, "native-json-tree.png");
    view.performItemClick(null, 1, 1);
    assertEquals(2, view.getAdapter().getCount());
  }

  private void screenshot(android.view.View view, String name) throws Exception {
    view.measure(
        android.view.View.MeasureSpec.makeMeasureSpec(480, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY));
    view.layout(0, 0, 480, 800);
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(480, 800, android.graphics.Bitmap.Config.ARGB_8888);
    view.draw(new android.graphics.Canvas(bitmap));
    try (java.io.FileOutputStream out = new java.io.FileOutputStream("build/" + name)) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
  }
}
