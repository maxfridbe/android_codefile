package com.TechHurts.CodeFile;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Tabs stay left aligned on wide windows, while shrinking together in narrow hinge panes. */
final class RibbonTabs extends LinearLayout {
  RibbonTabs(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
  }

  protected void onMeasure(int widthSpec, int heightSpec) {
    int available = MeasureSpec.getSize(widthSpec);
    float density = getResources().getDisplayMetrics().density;
    int tabWidth =
        Math.min(Math.round(88 * density), Math.max(0, (available - Math.round(60 * density)) / 3));
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (!(child instanceof RibbonTab)) continue;
      LayoutParams layout = (LayoutParams) child.getLayoutParams();
      layout.width = tabWidth;
      layout.weight = 0;
    }
    super.onMeasure(widthSpec, heightSpec);
  }
}
