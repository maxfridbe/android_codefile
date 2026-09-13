package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.LinearLayout;

/** Continuous accent outline, open under the selected tab into the command area. */
final class RibbonTabs extends LinearLayout {
  private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

  RibbonTabs(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
    setWillNotDraw(false);
    border.setStyle(Paint.Style.STROKE);
  }

  void setAccent(int accent) {
    border.setColor((accent & 0xffffff) | 0xa0000000);
    border.setStrokeWidth(getResources().getDisplayMetrics().density);
    invalidate();
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float left = 0, right = 0;
    for (int i = 0; i < getChildCount(); i++) {
      View tab = getChildAt(i);
      if (tab instanceof RibbonTab && tab.isSelected()) {
        left = tab.getLeft();
        right = tab.getRight();
      }
    }
    float width = getWidth() - 1,
        height = getHeight() - 1,
        radius = 6 * getResources().getDisplayMetrics().density;
    Path path = new Path();
    path.moveTo(Math.max(radius, right), height);
    path.lineTo(width - radius, height);
    path.quadTo(width, height, width, height - radius);
    path.lineTo(width, radius);
    path.quadTo(width, 1, width - radius, 1);
    path.lineTo(radius, 1);
    path.quadTo(1, 1, 1, radius);
    path.lineTo(1, height - radius);
    path.quadTo(1, height, radius, height);
    path.lineTo(Math.max(radius, left), height);
    canvas.drawPath(path, border);
  }
}
