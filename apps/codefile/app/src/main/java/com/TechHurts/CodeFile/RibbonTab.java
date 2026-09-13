package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.*;
import android.widget.Button;

/** Rounded tab with an open bottom edge joining the command ribbon. */
final class RibbonTab extends Button {
  private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path border = new Path();
  private int foreground, accent, surface;

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
    setBackground(shape);
    outline.setColor((accent & 0xffffff) | (isSelected() ? 0xb0000000 : 0x45000000));
    outline.setStyle(Paint.Style.STROKE);
    outline.setStrokeWidth(Math.max(1, getResources().getDisplayMetrics().density));
    invalidate();
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float edge = outline.getStrokeWidth() / 2;
    float radius = 7 * getResources().getDisplayMetrics().density;
    float right = getWidth() - edge;
    border.reset();
    border.moveTo(edge, getHeight());
    border.lineTo(edge, edge + radius);
    border.quadTo(edge, edge, edge + radius, edge);
    border.lineTo(right - radius, edge);
    border.quadTo(right, edge, right, edge + radius);
    border.lineTo(right, getHeight());
    canvas.drawPath(border, outline);
  }
}
