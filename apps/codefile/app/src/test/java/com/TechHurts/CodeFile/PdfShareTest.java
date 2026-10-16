package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PdfShareTest {
  @Test
  public void selectedPdfClipsHighlightsToExactTextRange() {
    TextHighlights marks = new TextHighlights();
    marks.apply(0, 7, 0x70ff66bb);
    marks.apply(9, 12, 0x7040cfff);
    SpannableString text = PdfShare.annotated("before selected after", 3, 11, marks.ranges());
    assertEquals("ore sele", text.toString());
    BackgroundColorSpan[] colors = text.getSpans(0, text.length(), BackgroundColorSpan.class);
    assertEquals(2, colors.length);
    assertEquals(0, text.getSpanStart(colors[0]));
    assertEquals(4, text.getSpanEnd(colors[0]));
    assertEquals(0x70ff66bb, colors[0].getBackgroundColor());
    assertEquals(8, text.getSpanEnd(colors[1]));
  }

  @Test
  public void pdfCapturesSyntaxThemeAndSourceReferencesWithExactSelection() throws Exception {
    LineSelectionEditor editor = new LineSelectionEditor(RuntimeEnvironment.getApplication());
    editor.setText("int value = 42;\n");
    io.github.rosemoe.sora.widget.schemes.EditorColorScheme colors = editor.getColorScheme();
    colors.setColor(
        io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND, 0xff101820);
    colors.setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.KEYWORD, 0xff88ddff);
    io.github.rosemoe.sora.lang.styling.MappedSpans.Builder builder =
        new io.github.rosemoe.sora.lang.styling.MappedSpans.Builder();
    builder.addIfNeeded(
        0,
        0,
        io.github.rosemoe.sora.lang.styling.TextStyle.makeStyle(
            io.github.rosemoe.sora.widget.schemes.EditorColorScheme.KEYWORD, 0, true, true, false));
    builder.addIfNeeded(0, 3, io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL);
    editor.setStyles(new io.github.rosemoe.sora.lang.styling.Styles(builder.build()));
    TextHighlights marks = new TextHighlights();
    marks.apply(4, 9, 0x70ff66bb);
    PdfAppearance appearance =
        PdfAppearance.capture(editor, 1, 9, marks.ranges(), new String[] {"~8", "9"}, 1.6f);
    assertEquals("nt value", appearance.text.toString());
    android.text.style.ForegroundColorSpan[] spans =
        appearance.text.getSpans(0, 2, android.text.style.ForegroundColorSpan.class);
    assertEquals(0xff88ddff, spans[0].getForegroundColor());
    assertEquals(0, appearance.text.getSpanStart(spans[0]));
    assertEquals(2, appearance.text.getSpanEnd(spans[0]));
    assertEquals(
        android.graphics.Typeface.BOLD_ITALIC,
        appearance.text.getSpans(0, 2, android.text.style.StyleSpan.class)[0].getStyle());
    assertEquals("~8", appearance.sourceLabels[0]);
    assertEquals(1.6f, appearance.spacing, .01f);
    assertEquals(1, appearance.text.getSpans(0, 8, BackgroundColorSpan.class).length);
    PdfShare.Pages pages =
        new PdfShare.Pages(
            "syntax.java", editor.getText().toString(), 1, 9, Typeface.MONOSPACE, appearance);
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(595, 842, android.graphics.Bitmap.Config.ARGB_8888);
    pages.draw(new android.graphics.Canvas(bitmap), 0);
    assertEquals(0xff101820, bitmap.getPixel(0, 0));
    try (FileOutputStream out = new FileOutputStream("build/native-pdf-syntax.png")) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    editor.release();
  }

  @Test
  public void nativePdfPagesPreserveHighlightsAndPaginate() throws Exception {
    String source = "    int highlighted = 42;\n".repeat(120);
    TextHighlights marks = new TextHighlights();
    marks.apply(0, source.length(), 0x70ff66bb);
    PdfShare.Pages pages =
        new PdfShare.Pages(
            "highlight.java", source, 0, source.length(), marks.ranges(), Typeface.MONOSPACE);
    assertTrue(pages.count() >= 2);
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(595, 842, android.graphics.Bitmap.Config.ARGB_8888);
    pages.draw(new android.graphics.Canvas(bitmap), 0);
    int pixel = bitmap.getPixel(78, 60);
    assertTrue(
        "Highlight pink survives page drawing",
        android.graphics.Color.red(pixel) > android.graphics.Color.green(pixel));
    assertTrue(android.graphics.Color.blue(pixel) > android.graphics.Color.green(pixel));
    try (FileOutputStream out = new FileOutputStream("build/native-pdf-page.png")) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    pages.draw(new android.graphics.Canvas(bitmap), pages.count() - 1);
    assertEquals(
        "application/pdf",
        new ShareProvider()
            .getType(Uri.parse("content://com.TechHurts.CodeFile.share/example.pdf")));
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  public void paginationFollowsThePickedPaperSize() throws Exception {
    String source = "    int highlighted = 42;\n".repeat(400);
    PdfAppearance look =
        new PdfAppearance(PdfShare.annotated(source, 0, source.length(), new java.util.ArrayList<>()));
    int a4 = new PdfShare.Pages("x.java", source, 0, source.length(), Typeface.MONOSPACE, look, PdfShare.PageSize.A4).count();
    int a5 = new PdfShare.Pages("x.java", source, 0, source.length(), Typeface.MONOSPACE,
        new PdfAppearance(PdfShare.annotated(source, 0, source.length(), new java.util.ArrayList<>())), PdfShare.PageSize.A5).count();
    int legal = new PdfShare.Pages("x.java", source, 0, source.length(), Typeface.MONOSPACE,
        new PdfAppearance(PdfShare.annotated(source, 0, source.length(), new java.util.ArrayList<>())), PdfShare.PageSize.LEGAL).count();
    assertTrue("A5 holds less per page than A4", a5 > a4);
    assertTrue("Legal is taller than A4", legal < a4);
    // Drawing fills the whole sheet at the chosen size, including the page-number footer.
    PdfShare.PageSize page = PdfShare.PageSize.byName("Letter");
    assertEquals(612, page.width);
    assertEquals(792, page.height);
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888);
    new PdfShare.Pages("x.java", source, 0, source.length(), Typeface.MONOSPACE,
        new PdfAppearance(PdfShare.annotated(source, 0, source.length(), new java.util.ArrayList<>())), page)
        .draw(new android.graphics.Canvas(bitmap), 0);
    assertEquals(0xffffffff, bitmap.getPixel(page.width - 1, page.height - 1));
    assertEquals("A4", PdfShare.PageSize.byName("nonsense").name);
    assertEquals(8, PdfShare.PageSize.ALL.size());
    assertEquals("A5 · 148 × 210 mm", PdfShare.PageSize.A5.label());
  }
}
