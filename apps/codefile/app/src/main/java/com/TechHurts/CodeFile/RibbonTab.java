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
    if (getParent() instanceof android.view.View) ((android.view.View) getParent()).invalidate();
  }

  private void refresh() {
    setTextColor(isSelected() ? accent : foreground);
    android.graphics.drawable.GradientDrawable shape =
        new android.graphics.drawable.GradientDrawable();
    shape.setColor(isSelected() ? surface : Color.TRANSPARENT);
    float radius = 7 * getResources().getDisplayMetrics().density;
    shape.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
    shape.setStroke(
        Math.max(1, Math.round(getResources().getDisplayMetrics().density)),
        (accent & 0xffffff) | (isSelected() ? 0xb0000000 : 0x45000000));
    setBackground(shape);
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
