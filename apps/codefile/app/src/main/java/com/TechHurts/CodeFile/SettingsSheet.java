package com.TechHurts.CodeFile;

import android.content.Context;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Pinned category navigation with independently scrolling settings pages. */
final class SettingsSheet extends LinearLayout {
  private final LinearLayout tabs;
  private final FrameLayout pages;
  private final List<Button> buttons = new ArrayList<>();

  SettingsSheet(Context context) {
    super(context);
    setOrientation(VERTICAL);
    HorizontalScrollView scroll = new HorizontalScrollView(context);
    scroll.setHorizontalScrollBarEnabled(false);
    tabs = new LinearLayout(context);
    scroll.addView(tabs);
    addView(scroll, new LayoutParams(-1, dp(44)));
    pages = new FrameLayout(context);
    addView(pages, new LayoutParams(-1, 0, 1));
  }

  LinearLayout addCategory(Button button) {
    int index = buttons.size();
    buttons.add(button);
    tabs.addView(button, new LayoutParams(dp(index == 0 ? 112 : 88), dp(40)));
    LinearLayout body = new LinearLayout(getContext());
    body.setOrientation(VERTICAL);
    body.setPadding(dp(12), dp(12), dp(12), dp(16));
    ScrollView scroll = new ScrollView(getContext());
    scroll.addView(body);
    pages.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    scroll.setVisibility(index == 0 ? VISIBLE : GONE);
    button.setSelected(index == 0);
    button.setOnClickListener(v -> select(index));
    return body;
  }

  private void select(int index) {
    for (int i = 0; i < buttons.size(); i++) {
      buttons.get(i).setSelected(i == index);
      pages.getChildAt(i).setVisibility(i == index ? View.VISIBLE : View.GONE);
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
