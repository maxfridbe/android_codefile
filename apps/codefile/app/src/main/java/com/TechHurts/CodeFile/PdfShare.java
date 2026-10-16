package com.TechHurts.CodeFile;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import java.io.*;
import java.util.*;

/** Native, paginated PDF rendering of an exact text range and its highlight annotations. */
final class PdfShare {
  /** A paper size in PDF points (1/72 inch). */
  static final class PageSize {
    static final PageSize A4 = new PageSize("A4", 595, 842, "210 × 297 mm"),
        LETTER = new PageSize("Letter", 612, 792, "8.5 × 11 in"),
        LEGAL = new PageSize("Legal", 612, 1008, "8.5 × 14 in"),
        A5 = new PageSize("A5", 420, 595, "148 × 210 mm"),
        A6 = new PageSize("A6", 298, 420, "105 × 148 mm"),
        B5 = new PageSize("B5", 499, 709, "176 × 250 mm"),
        EXECUTIVE = new PageSize("Executive", 522, 756, "7.25 × 10.5 in"),
        TABLOID = new PageSize("Tabloid", 792, 1224, "11 × 17 in");
    static final List<PageSize> ALL =
        Arrays.asList(A4, LETTER, LEGAL, A5, A6, B5, EXECUTIVE, TABLOID);

    final String name, size;
    final int width, height;

    private PageSize(String name, int width, int height, String size) {
      this.name = name;
      this.width = width;
      this.height = height;
      this.size = size;
    }

    String label() {
      return name + " · " + size;
    }

    /** The saved choice, or A4 when the name is unknown. */
    static PageSize byName(String name) {
      for (PageSize page : ALL) if (page.name.equals(name)) return page;
      return A4;
    }
  }

  static SpannableString annotated(
      String source, int start, int end, List<TextHighlights.Range> marks) {
    if (start < 0 || end > source.length() || start > end)
      throw new IllegalArgumentException("Invalid PDF range");
    SpannableString text = new SpannableString(source.substring(start, end));
    for (TextHighlights.Range mark : marks) {
      int left = Math.max(start, mark.start), right = Math.min(end, mark.end);
      if (left < right)
        text.setSpan(
            new BackgroundColorSpan(mark.color),
            left - start,
            right - start,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return text;
  }

  /** The same page drawing is exercised on native bitmap canvases and PDF canvases. */
  static final class Pages {
    private final StaticLayout layout;
    private final int[] numbers;
    private final List<Integer> starts = new ArrayList<>();
    private final String name;
    private final PdfAppearance appearance;
    private final PageSize page;
    private final float textLeft;

    Pages(
        String name,
        String source,
        int start,
        int end,
        List<TextHighlights.Range> marks,
        Typeface font)
        throws IOException {
      this(name, source, start, end, font, new PdfAppearance(annotated(source, start, end, marks)));
    }

    Pages(String name, String source, int start, int end, Typeface font, PdfAppearance appearance)
        throws IOException {
      this(name, source, start, end, font, appearance, PageSize.A4);
    }

    Pages(
        String name,
        String source,
        int start,
        int end,
        Typeface font,
        PdfAppearance appearance,
        PageSize page)
        throws IOException {
      if (end - start > 1024 * 1024)
        throw new IOException("Select up to 1 MiB of text for PDF sharing");
      this.name = name;
      this.page = page;
      this.appearance = appearance;
      textLeft = appearance.numbers ? (appearance.sourceLabels == null ? 76 : 116) : 36;
      SpannableString text = appearance.text;
      TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(appearance.foreground);
      paint.setTypeface(font);
      paint.setTextSize(10);
      int tab = Math.max(1, Math.round(paint.measureText(" ") * appearance.tabWidth));
      for (int x = tab; x < page.width - 71; x += tab)
        text.setSpan(
            new android.text.style.TabStopSpan.Standard(x),
            0,
            text.length(),
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
      layout =
          StaticLayout.Builder.obtain(text, 0, text.length(), paint, (int) (page.width - 36 - textLeft))
              .setIncludePad(false)
              .setLineSpacing(0, appearance.spacing)
              .build();
      numbers = new int[layout.getLineCount()];
      int sourceLine = 1;
      for (int i = 0; i < start; i++) if (source.charAt(i) == '\n') sourceLine++;
      int consumed = 0;
      for (int row = 0; row < numbers.length; row++) {
        int offset = layout.getLineStart(row);
        while (consumed < offset) {
          if (text.charAt(consumed++) == '\n') sourceLine++;
        }
        numbers[row] = row == 0 || (offset > 0 && text.charAt(offset - 1) == '\n') ? sourceLine : 0;
      }
      int row = 0;
      while (row < layout.getLineCount()) {
        if (starts.size() == 500)
          throw new IOException("PDF exceeds 500 pages; select a smaller range");
        starts.add(row);
        int first = row, top = layout.getLineTop(row);
        while (row < layout.getLineCount() && layout.getLineBottom(row) - top <= page.height - 116)
          row++;
        if (row == first) row++;
      }
    }

    int count() {
      return starts.size();
    }

    void draw(Canvas canvas, int index) {
      int first = starts.get(index),
          end = index + 1 < starts.size() ? starts.get(index + 1) : layout.getLineCount();
      int top = layout.getLineTop(first);
      canvas.drawColor(appearance.background);
      Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
      label.setColor(appearance.gutter);
      label.setTextSize(10);
      canvas.drawText(name.length() > 80 ? name.substring(0, 80) : name, 36, 32, label);
      canvas.drawText("CodeFile · " + (index + 1), 36, page.height - 28, label);
      label.setTypeface(Typeface.MONOSPACE);
      label.setTextSize(8);
      if (appearance.numbers)
        for (int line = first; line < end; line++) drawNumber(canvas, label, line, top);
      canvas.save();
      canvas.clipRect(textLeft, 56, page.width - 36, 56 + layout.getLineBottom(end - 1) - top);
      canvas.translate(textLeft, 56 - top);
      layout.draw(canvas);
      canvas.restore();
    }

    private void drawNumber(Canvas canvas, Paint label, int row, int top) {
      int number = numbers[row];
      if (number <= 0) return;
      float y = 56 + layout.getLineBaseline(row) - top;
      canvas.drawText(Integer.toString(number), textLeft - 40, y, label);
      if (appearance.sourceLabels != null && number <= appearance.sourceLabels.length)
        canvas.drawText(appearance.sourceLabels[number - 1], 36, y, label);
    }
  }

  static File write(
      File cache,
      String name,
      String source,
      int start,
      int end,
      PdfAppearance appearance,
      Typeface font)
      throws IOException {
    return write(cache, name, source, start, end, appearance, font, PageSize.A4);
  }

  static File write(
      File cache,
      String name,
      String source,
      int start,
      int end,
      PdfAppearance appearance,
      Typeface font,
      PageSize size)
      throws IOException {
    Pages pages = new Pages(name, source, start, end, font, appearance, size);
    File directory = new File(cache, "shares");
    if (!directory.isDirectory() && !directory.mkdirs())
      throw new IOException("Cannot create PDF share directory");
    String safe = name.replaceAll("[^a-zA-Z0-9._ -]", "_");
    if (safe.length() > 100) safe = safe.substring(0, 100);
    File file = new File(directory, UUID.randomUUID() + "_" + safe + ".pdf");
    boolean complete = false;
    PdfDocument pdf = new PdfDocument();
    try {
      for (int index = 0; index < pages.count(); index++) {
        PdfDocument.Page page =
            pdf.startPage(new PdfDocument.PageInfo.Builder(size.width, size.height, index + 1).create());
        try {
          pages.draw(page.getCanvas(), index);
        } finally {
          pdf.finishPage(page);
        }
      }
      try (FileOutputStream out = new FileOutputStream(file)) {
        pdf.writeTo(out);
      }
      complete = true;
      return file;
    } finally {
      pdf.close();
      if (!complete) file.delete();
    }
  }
}
