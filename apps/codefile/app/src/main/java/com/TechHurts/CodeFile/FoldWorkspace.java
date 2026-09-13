package com.TechHurts.CodeFile;

import android.graphics.Rect;
import android.view.*;

final class FoldWorkspace extends android.view.ViewGroup {
  Rect hinge;
  int mode, cutStart, cutEnd, rail, toolbarHeight;

  FoldWorkspace(android.content.Context context) {
    super(context);
    setContentDescription("Adaptive phone tablet and fold workspace");
  }

  protected void onMeasure(int widthSpec, int heightSpec) {
    int w = MeasureSpec.getSize(widthSpec), h = MeasureSpec.getSize(heightSpec);
    setMeasuredDimension(w, h);
    mode = 0; // A ribbon runs across the window; separating hinges override its placement.
    int[] origin = new int[2];
    getLocationInWindow(origin);
    if (hinge != null) {
      Rect local = new Rect(hinge);
      local.offset(-origin[0], -origin[1]);
      if (local.height() > local.width() && local.left >= dp(180) && w - local.right >= dp(180)) {
        mode = 2;
        cutStart = local.left;
        cutEnd = local.right;
        rail = cutStart;
      } else if (local.width() >= local.height() && local.top > 0 && local.bottom < h) {
        mode = 3;
        cutStart = local.top;
        cutEnd = local.bottom;
      }
    }
    if (mode == 2) {
      getChildAt(0)
          .measure(
              MeasureSpec.makeMeasureSpec(rail, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
      getChildAt(1)
          .measure(
              MeasureSpec.makeMeasureSpec(w - cutEnd, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    } else if (mode == 3) {
      getChildAt(0)
          .measure(
              MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(h - cutEnd, MeasureSpec.EXACTLY));
      getChildAt(1)
          .measure(
              MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(cutStart, MeasureSpec.EXACTLY));
    } else {
      getChildAt(0)
          .measure(
              MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(Math.max(dp(100), h / 2), MeasureSpec.AT_MOST));
      toolbarHeight = getChildAt(0).getMeasuredHeight();
      getChildAt(1)
          .measure(
              MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(Math.max(0, h - toolbarHeight), MeasureSpec.EXACTLY));
    }
  }

  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int w = r - l, h = b - t;
    if (mode == 2) {
      getChildAt(0).layout(0, 0, rail, h);
      getChildAt(1).layout(cutEnd, 0, w, h);
    } else if (mode == 3) {
      getChildAt(1).layout(0, 0, w, cutStart);
      getChildAt(0).layout(0, cutEnd, w, h);
    } else {
      getChildAt(0).layout(0, 0, w, toolbarHeight);
      getChildAt(1).layout(0, toolbarHeight, w, h);
    }
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
