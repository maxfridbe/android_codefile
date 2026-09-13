package com.TechHurts.CodeFile;

import android.content.Context;
import android.widget.LinearLayout;

/** Natural-width tab strip; each tab owns its outline, leaving branding and tools unframed. */
final class RibbonTabs extends LinearLayout {
  RibbonTabs(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
  }
}
