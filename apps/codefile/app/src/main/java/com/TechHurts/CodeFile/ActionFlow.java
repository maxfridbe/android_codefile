package com.TechHurts.CodeFile;

import android.view.*;

final class ActionFlow extends ViewGroup {
  ActionFlow(android.content.Context context) {
    super(context);
  }

  protected void onMeasure(int ws, int hs) {
    int width = MeasureSpec.getSize(ws), x = 0, y = 0, line = 0;
    for (int i = 0; i < getChildCount(); i++) {
      View v = getChildAt(i);
      v.measure(
          MeasureSpec.makeMeasureSpec(Math.max(1, width - dp(6)), MeasureSpec.AT_MOST),
          MeasureSpec.makeMeasureSpec(dp(48), MeasureSpec.EXACTLY));
      int cw = v.getMeasuredWidth() + dp(6), ch = dp(54);
      if (x + cw > width && x > 0) {
        x = 0;
        y += line;
        line = 0;
      }
      x += cw;
      line = Math.max(line, ch);
    }
    setMeasuredDimension(width, y + line);
  }

  protected void onLayout(boolean c, int l, int t, int r, int b) {
    int width = r - l, x = 0, y = 0;
    for (int i = 0; i < getChildCount(); i++) {
      View v = getChildAt(i);
      int cw = v.getMeasuredWidth() + dp(6);
      if (x + cw > width && x > 0) {
        x = 0;
        y += dp(54);
      }
      v.layout(x + dp(3), y + dp(3), x + cw - dp(3), y + dp(51));
      x += cw;
    }
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
