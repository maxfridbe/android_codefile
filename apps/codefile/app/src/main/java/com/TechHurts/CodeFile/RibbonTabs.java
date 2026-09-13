package com.TechHurts.CodeFile;

import android.content.Context;
import android.widget.LinearLayout;

/** Natural-width tabs remain legible inside the horizontal tab scroller. */
final class RibbonTabs extends LinearLayout {
  RibbonTabs(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
  }
}
