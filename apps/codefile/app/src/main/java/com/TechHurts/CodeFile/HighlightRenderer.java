package com.TechHurts.CodeFile;

import android.graphics.Canvas;
import androidx.collection.MutableIntList;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.MutableInt;
import io.github.rosemoe.sora.widget.EditorRenderer;
import io.github.rosemoe.sora.widget.layout.Row;
import java.util.List;

/** Uses Sora's native bidi-aware range painter before syntax glyphs are drawn. */
final class HighlightRenderer extends EditorRenderer {
  private final LineSelectionEditor owner;
  private final android.graphics.Paint sourcePaint =
      new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

  HighlightRenderer(LineSelectionEditor owner) {
    super(owner);
    this.owner = owner;
  }

  @Override
  protected void drawRows(
      Canvas canvas,
      float offset,
      LongArrayList numbers,
      List<DrawCursorTask> cursors,
      MutableIntList current,
      MutableInt first) {
    if (owner.highlights != null) drawHighlights(canvas);
    super.drawRows(canvas, offset, numbers, cursors, current, first);
  }

  @Override
  protected void drawLineNumber(
      Canvas canvas, int line, int row, float offset, float width, int color) {
    float source = owner.sourceWidth();
    if (source > 0 && line < owner.sourceLabels.length) {
      sourcePaint.setTypeface(owner.getTypefaceLineNumber());
      sourcePaint.setTextSize(10 * owner.getResources().getDisplayMetrics().density);
      sourcePaint.setColor(color);
      canvas.drawText(
          owner.sourceLabels[line],
          offset + 2 * owner.getResources().getDisplayMetrics().density,
          owner.getRowBaseline(row) - owner.getOffsetY(),
          sourcePaint);
    }
    super.drawLineNumber(canvas, line, row, offset + source, width - source, color);
  }

  private void drawHighlights(Canvas canvas) {
    int end = Math.min(owner.getLastVisibleRow(), owner.getLayout().getRowCount() - 1);
    for (int row = owner.getFirstVisibleRow(); row <= end; row++) {
      Row info = owner.getLayout().getRowAt(row);
      int base = owner.getText().getCharIndex(info.lineIndex, 0);
      for (TextHighlights.Range range : owner.highlights.ranges()) {
        if (range.end <= base + info.startColumn) continue;
        if (range.start >= base + info.endColumn) break;
        drawRowRegionBackground(
            canvas,
            row,
            info.lineIndex,
            Math.max(0, range.start - base),
            range.end - base,
            info.startColumn,
            info.endColumn,
            range.color);
      }
    }
  }
}
