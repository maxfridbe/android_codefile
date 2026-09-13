package com.TechHurts.CodeFile;

import android.graphics.Typeface;
import android.text.*;
import android.text.style.*;
import io.github.rosemoe.sora.lang.styling.*;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import java.util.List;

/** Immutable export inputs captured before handing PDF rendering to a worker. */
final class PdfAppearance {
  final SpannableString text;
  final int background, foreground, gutter;
  final boolean numbers;
  final String[] sourceLabels;
  final float spacing;
  final int tabWidth;

  PdfAppearance(SpannableString text) {
    this(text, 0xffffffff, 0xff000000, 0xff505050, true, null, 1.3f, 4);
  }

  private PdfAppearance(
      SpannableString text,
      int background,
      int foreground,
      int gutter,
      boolean numbers,
      String[] labels,
      float spacing,
      int tabWidth) {
    this.text = text;
    this.background = background;
    this.foreground = foreground;
    this.gutter = gutter;
    this.numbers = numbers;
    sourceLabels = labels == null ? null : labels.clone();
    this.spacing = spacing;
    this.tabWidth = tabWidth;
  }

  static PdfAppearance capture(
      CodeEditor editor,
      int start,
      int end,
      List<TextHighlights.Range> marks,
      String[] labels,
      float spacing) {
    if (end - start > 1024 * 1024)
      throw new IllegalArgumentException("Select up to 1 MiB of text for PDF sharing");
    String source = editor.getText().toString();
    SpannableString text = new SpannableString(source.substring(start, end));
    Styles styles = editor.getStyles();
    if (styles != null && styles.spans != null)
      copySyntax(editor, text, start, end, styles.spans.read());
    SpannableString marked = PdfShare.annotated(source, start, end, marks);
    TextUtils.copySpansFrom(marked, 0, marked.length(), BackgroundColorSpan.class, text, 0);
    EditorColorScheme colors = editor.getColorScheme();
    return new PdfAppearance(
        text,
        colors.getColor(EditorColorScheme.WHOLE_BACKGROUND),
        colors.getColor(EditorColorScheme.TEXT_NORMAL),
        colors.getColor(EditorColorScheme.LINE_NUMBER),
        editor.isLineNumberEnabled(),
        labels,
        spacing,
        editor.getTabWidth());
  }

  private static void copySyntax(
      CodeEditor editor, SpannableString text, int start, int end, Spans.Reader reader) {
    int first = editor.getText().getIndexer().getCharPosition(start).line;
    int last = editor.getText().getIndexer().getCharPosition(end).line;
    for (int line = first; line <= last; line++) {
      int base = editor.getText().getCharIndex(line, 0);
      copyLine(
          text,
          reader.getSpansOnLine(line),
          base,
          editor.getText().getColumnCount(line),
          start,
          end,
          editor.getColorScheme());
    }
  }

  private static void copyLine(
      SpannableString text,
      List<Span> spans,
      int base,
      int length,
      int start,
      int end,
      EditorColorScheme colors) {
    for (int i = 0; i < spans.size(); i++) {
      Span span = spans.get(i);
      int next = i + 1 < spans.size() ? spans.get(i + 1).getColumn() : length;
      int left = Math.max(start, base + span.getColumn());
      int right = Math.min(end, base + next);
      if (left < right) copyStyle(text, span, left - start, right - start, colors);
    }
  }

  private static void copyStyle(
      SpannableString text, Span span, int left, int right, EditorColorScheme colors) {
    int flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    text.setSpan(
        new ForegroundColorSpan(colors.getColor(span.getForegroundColorId())), left, right, flags);
    if (span.getBackgroundColorId() != 0)
      text.setSpan(
          new BackgroundColorSpan(colors.getColor(span.getBackgroundColorId())),
          left,
          right,
          flags);
    long bits = span.getStyle();
    int style =
        (TextStyle.isBold(bits) ? Typeface.BOLD : 0)
            | (TextStyle.isItalics(bits) ? Typeface.ITALIC : 0);
    if (style != 0) text.setSpan(new StyleSpan(style), left, right, flags);
    if (TextStyle.isStrikeThrough(bits)) text.setSpan(new StrikethroughSpan(), left, right, flags);
    if (span.getUnderlineColor() != null) text.setSpan(new UnderlineSpan(), left, right, flags);
  }
}
