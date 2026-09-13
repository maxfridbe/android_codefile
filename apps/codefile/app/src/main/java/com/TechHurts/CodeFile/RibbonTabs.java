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
    boolean narrow = available < Math.round(400 * density);
    int identityWidth = Math.round((narrow ? 76 : 104) * density);
    int fixed = identityWidth + Math.round((narrow ? 48 : 96) * density);
    int tabWidth = Math.min(Math.round(88 * density), Math.max(0, (available - fixed) / 3));
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if ("identity".equals(child.getTag())) child.getLayoutParams().width = identityWidth;
      if ("ribbonToggle".equals(child.getTag())) child.setVisibility(narrow ? GONE : VISIBLE);
      if (!(child instanceof RibbonTab)) continue;
      LayoutParams layout = (LayoutParams) child.getLayoutParams();
      layout.width = tabWidth;
      layout.weight = 0;
    }
    super.onMeasure(widthSpec, heightSpec);
  }
}
