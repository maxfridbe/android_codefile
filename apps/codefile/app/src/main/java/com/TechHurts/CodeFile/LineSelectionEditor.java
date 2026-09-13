package com.TechHurts.CodeFile;

import android.content.Context;
import android.view.MotionEvent;
import io.github.rosemoe.sora.event.*;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.CodeEditor;

/** Gutter gestures select complete source lines immediately; no long-press timeout is involved. */
final class LineSelectionEditor extends CodeEditor {
  TextHighlights highlights;

  @Override
  protected io.github.rosemoe.sora.widget.EditorRenderer onCreateRenderer() {
    return new HighlightRenderer(this);
  }

  void setHighlights(TextHighlights highlights) {
    this.highlights = highlights;
    invalidate();
  }

  private boolean dragging;
  private float touchX, touchY, lastTouchY;
  private boolean ribbonGesture, multitouch;
  private java.util.function.IntConsumer ribbonDrag = delta -> {};

  void setOnRibbonDrag(java.util.function.IntConsumer callback) {
    ribbonDrag = callback;
  }

  public boolean dispatchTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      touchX = event.getRawX();
      touchY = lastTouchY = event.getRawY();
      ribbonGesture = false;
      multitouch = false;
    } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
      multitouch = true;
    } else if (action == MotionEvent.ACTION_MOVE
        && !multitouch
        && event.getPointerCount() == 1
        && !dragging
        && !getEventHandler().hasAnyHeldHandle()) {
      float dy = event.getRawY() - touchY, dx = event.getRawX() - touchX;
      if (!ribbonGesture
          && Math.abs(dy) > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()
          && Math.abs(dy) > Math.abs(dx)) ribbonGesture = true;
      if (ribbonGesture) ribbonDrag.accept(Math.round(lastTouchY - event.getRawY()));
      lastTouchY = event.getRawY();
    }
    return super.dispatchTouchEvent(event);
  }

  private int anchor, first = -1, last = -1;
  private float pointerY;
  private Runnable selectionFinished = () -> {};
  private final Runnable edgeScroll = this::scrollSelectionEdge;

  LineSelectionEditor(Context context) {
    super(context);
  }

  void setOnLineSelectionFinished(Runnable callback) {
    selectionFinished = callback;
  }

  boolean isSelectingLines() {
    return dragging;
  }

  int firstSelectedLine() {
    return first;
  }

  int lastSelectedLine() {
    return last;
  }

  void clearLineSelection() {
    first = -1;
    last = -1;
  }

  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      clearLineSelection();
      float visibleGutter = measureTextRegionOffset() - getOffsetX();
      dragging = isLineNumberEnabled() && event.getX() >= 0 && event.getX() < visibleGutter;
      if (!dragging) return super.onTouchEvent(event);
      getScroller().forceFinished(true);
      pointerY = event.getY();
      anchor = lineAt(pointerY);
      getParent().requestDisallowInterceptTouchEvent(true);
      selectThrough(anchor);
      postDelayed(edgeScroll, 16);
      return true;
    }
    if (!dragging) return super.onTouchEvent(event);
    if (action == MotionEvent.ACTION_MOVE) {
      pointerY = event.getY();
      selectThrough(lineAt(pointerY));
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      removeCallbacks(edgeScroll);
      dragging = false;
      getParent().requestDisallowInterceptTouchEvent(false);
      if (action == MotionEvent.ACTION_UP) selectionFinished.run();
      return true;
    }
    return true;
  }

  private int lineAt(float y) {
    return Math.max(
        0,
        Math.min(
            getLineCount() - 1,
            IntPair.getFirst(
                getPointPositionOnScreen(
                    measureTextRegionOffset(), Math.max(0, Math.min(getHeight() - 1, y))))));
  }

  private void selectThrough(int line) {
    first = Math.min(anchor, line);
    last = Math.max(anchor, line);
    int end = last < getLineCount() - 1 ? last + 1 : last;
    int column = last < getLineCount() - 1 ? 0 : getText().getColumnCount(last);
    setSelectionRegion(first, 0, end, column, false, SelectionChangeEvent.CAUSE_UNKNOWN);
  }

  private void scrollSelectionEdge() {
    if (!dragging) return;
    int edge = Math.round(28 * getResources().getDisplayMetrics().density);
    int step =
        pointerY < edge
            ? -getRowHeight() / 2
            : pointerY > getHeight() - edge ? getRowHeight() / 2 : 0;
    int before = getOffsetY(), after = Math.max(0, Math.min(getScrollMaxY(), before + step));
    if (after != before) {
      getScroller().startScroll(getOffsetX(), before, 0, after - before, 0);
      getScroller().abortAnimation();
      dispatchEvent(
          new ScrollEvent(
              this, getOffsetX(), before, getOffsetX(), after, ScrollEvent.CAUSE_TEXT_SELECTING));
      selectThrough(lineAt(pointerY));
      invalidate();
    }
    postDelayed(edgeScroll, 16);
  }

  protected void onDetachedFromWindow() {
    removeCallbacks(edgeScroll);
    dragging = false;
    super.onDetachedFromWindow();
  }
}
