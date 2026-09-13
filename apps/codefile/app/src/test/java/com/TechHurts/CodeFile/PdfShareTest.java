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
}
