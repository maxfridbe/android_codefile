package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.*;
import android.widget.Button;

/** Flat tab with a visible active underline, independent of command-button styling. */
final class RibbonTab extends Button {
  private final Paint underline = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int foreground, muted, accent, surface;

  RibbonTab(Context context) {
    super(context);
    setAllCaps(false);
    setTextSize(13);
    setPadding(0, 0, 0, 0);
    setMinimumWidth(0);
    setMinWidth(0);
  }

  void setColors(int foreground, int muted, int accent, int surface) {
    this.foreground = foreground;
    this.muted = muted;
    this.accent = accent;
    this.surface = surface;
    refresh();
  }

  public void setSelected(boolean selected) {
    super.setSelected(selected);
    refresh();
  }

  private void refresh() {
    setTextColor(isSelected() ? accent : foreground);
    setBackgroundColor(isSelected() ? surface : Color.TRANSPARENT);
    underline.setColor(accent);
    invalidate();
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (isSelected()) {
      float inset = getWidth() * .22f;
      float height = 3 * getResources().getDisplayMetrics().density;
      canvas.drawRect(inset, getHeight() - height, getWidth() - inset, getHeight(), underline);
    }
  }
}
