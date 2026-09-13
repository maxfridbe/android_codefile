package com.TechHurts.CodeFile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** Scroll displacement directly controls visible height; tab actions animate to an endpoint. */
final class CollapsingRibbon extends FrameLayout {
  private float fraction = 1f;
  private int expandedHeight;
  private ValueAnimator animator;
  private final android.graphics.Paint outline =
      new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  private java.util.List<? extends View> tabs = java.util.Collections.emptyList();
  private final int[] origin = new int[2], tabOrigin = new int[2];

  void setOutline(java.util.List<? extends View> tabs, int accent) {
    this.tabs = tabs;
    outline.setColor((accent & 0xffffff) | 0xb0000000);
    outline.setStrokeWidth(Math.max(1, getResources().getDisplayMetrics().density));
    invalidate();
  }

  protected void dispatchDraw(android.graphics.Canvas canvas) {
    super.dispatchDraw(canvas);
    if (getHeight() == 0) return;
    float left = getWidth(), right = getWidth();
    getLocationInWindow(origin);
    for (View tab : tabs) {
      if (!tab.isSelected()) continue;
      tab.getLocationInWindow(tabOrigin);
      left = Math.max(0, Math.min(getWidth(), tabOrigin[0] - origin[0]));
      right = Math.max(0, Math.min(getWidth(), tabOrigin[0] - origin[0] + tab.getWidth()));
      break;
    }
    float y = outline.getStrokeWidth() / 2;
    canvas.drawLine(0, y, left, y, outline);
    canvas.drawLine(right, y, getWidth(), y, outline);
  }

  CollapsingRibbon(Context context) {
    super(context);
    setClipChildren(true);
    setClipToPadding(true);
  }

  float fraction() {
    return fraction;
  }

  void consumeScroll(int delta) {
    cancelAnimation();
    setFraction(fraction - delta / (float) Math.max(1, expandedHeight));
  }

  void animateTo(boolean collapsed) {
    cancelAnimation();
    float target = collapsed ? 0 : 1;
    if (target == fraction) return;
    animator = ValueAnimator.ofFloat(fraction, target);
    animator.setDuration(Math.round(180 * Math.abs(target - fraction)));
    animator.addUpdateListener(value -> setFraction((float) value.getAnimatedValue()));
    animator.start();
  }

  private void cancelAnimation() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
  }

  private void setFraction(float next) {
    fraction = Math.max(0, Math.min(1, next));
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) child.setVisibility(fraction == 0 ? INVISIBLE : VISIBLE);
    }
    requestLayout();
  }

  protected void onMeasure(int widthSpec, int heightSpec) {
    expandedHeight = 0;
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() == GONE) continue;
      child.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
      expandedHeight = Math.max(expandedHeight, child.getMeasuredHeight());
    }
    setMeasuredDimension(MeasureSpec.getSize(widthSpec), Math.round(expandedHeight * fraction));
  }

  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() == GONE) continue;
      child.layout(0, 0, r - l, expandedHeight);
      child.setTranslationY(getHeight() - expandedHeight);
    }
  }

  protected void onDetachedFromWindow() {
    cancelAnimation();
    super.onDetachedFromWindow();
  }
}
