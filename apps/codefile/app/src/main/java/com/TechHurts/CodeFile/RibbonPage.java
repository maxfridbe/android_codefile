package com.TechHurts.CodeFile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.text.style.AbsoluteSizeSpan;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Fixed-height Office-style command groups; a primary tile and two aligned secondary commands. */
final class RibbonPage extends HorizontalScrollView {
  private final List<CommandGroup> groups = new ArrayList<>();
  private int surface, foreground, muted, accent;
  private final LinearLayout track;

  RibbonPage(Context context, String[] captions, List<Button> commands, int[][] order) {
    super(context);
    setContentDescription("Grouped command ribbon");
    setFillViewport(true);
    setHorizontalScrollBarEnabled(false);
    setOverScrollMode(OVER_SCROLL_NEVER);
    track = new LinearLayout(context);
    track.setOrientation(LinearLayout.HORIZONTAL);
    addView(track, new LayoutParams(-2, dp(112)));
    for (int i = 0; i < captions.length; i++) {
      List<Button> group = new ArrayList<>();
      for (int index : order[i]) group.add(commands.get(index));
      CommandGroup view = new CommandGroup(context, captions[i], group);
      groups.add(view);
      track.addView(view, new LinearLayout.LayoutParams(dp(184), dp(112)));
    }
  }

  void setColors(int surface, int foreground, int muted, int accent) {
    this.surface = surface;
    this.foreground = foreground;
    this.muted = muted;
    this.accent = accent;
    GradientDrawable background = new GradientDrawable();
    background.setColor(surface);
    background.setCornerRadius(dp(6));
    setBackground(background);
    setClipToOutline(true);
    for (CommandGroup group : groups) group.tint();
    invalidate();
  }

  void prependPanel(View panel, int width) {
    track.addView(panel, 0, new LinearLayout.LayoutParams(dp(width), dp(112)));
  }

  protected void onMeasure(int ws, int hs) {
    super.onMeasure(ws, MeasureSpec.makeMeasureSpec(dp(112), MeasureSpec.EXACTLY));
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private int blend(int color, int other, float amount) {
    return Color.rgb(
        Math.round(Color.red(color) * (1 - amount) + Color.red(other) * amount),
        Math.round(Color.green(color) * (1 - amount) + Color.green(other) * amount),
        Math.round(Color.blue(color) * (1 - amount) + Color.blue(other) * amount));
  }

  private Drawable commandBackground() {
    GradientDrawable fill = new GradientDrawable();
    fill.setColors(
        new int[] {blend(surface, 0xffffffff, .045f), blend(surface, 0xff000000, .055f)});
    fill.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
    fill.setStroke(dp(1), (accent & 0xffffff) | 0x45000000);
    fill.setCornerRadius(dp(5));
    return new RippleDrawable(ColorStateList.valueOf((accent & 0xffffff) | 0x33000000), fill, null);
  }

  private String shortLabel(Button button) {
    String label = String.valueOf(button.getContentDescription());
    if (label.equals("Service info")) return "Service";
    if (label.equals("Navigation")) return "Browse";
    return label;
  }

  private CharSequence commandText(Button button, boolean primary) {
    CharSequence original = button.getText();
    int glyphLength = Character.charCount(Character.codePointAt(original, 0));
    SpannableStringBuilder text = new SpannableStringBuilder(original.subSequence(0, glyphLength));
    text.setSpan(
        new AbsoluteSizeSpan(primary ? 27 : 16, true),
        0,
        glyphLength,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.append(primary ? "\n" : "  ").append(shortLabel(button));
    return text;
  }

  private final class CommandGroup extends ViewGroup {
    private final List<Button> commands;
    private final TextView caption;
    private final Paint rule = new Paint();

    CommandGroup(Context context, String name, List<Button> commands) {
      super(context);
      setWillNotDraw(false);
      this.commands = commands;
      for (int i = 0; i < commands.size(); i++) {
        Button command = commands.get(i);
        command.setText(commandText(command, i == 0));
        command.setTextSize(11);
        command.setGravity(i == 0 ? Gravity.CENTER : Gravity.CENTER_VERTICAL | Gravity.START);
        command.setPadding(dp(4), 0, dp(2), 0);
        command.setMinimumWidth(0);
        command.setMinWidth(0);
        command.setMaxLines(2);
        command.setEllipsize(TextUtils.TruncateAt.END);
        addView(command);
      }
      caption = new TextView(context);
      caption.setText(name);
      caption.setTextSize(10);
      caption.setGravity(Gravity.CENTER);
      caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      addView(caption);
    }

    void tint() {
      caption.setTextColor(muted);
      for (Button command : commands) {
        command.setTextColor(foreground);
        command.setBackground(commandBackground());
      }
      rule.setColor((accent & 0xffffff) | 0x65000000);
      invalidate();
    }

    protected void onMeasure(int ws, int hs) {
      int w = MeasureSpec.getSize(ws);
      int primary = dp(56), secondary = Math.max(0, w - dp(65));
      for (int i = 0; i < commands.size(); i++) {
        Button button = commands.get(i);
        button.measure(
            MeasureSpec.makeMeasureSpec(i == 0 ? primary : secondary, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(dp(i == 0 ? 92 : 44), MeasureSpec.EXACTLY));
      }
      caption.measure(
          MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(dp(16), MeasureSpec.EXACTLY));
      setMeasuredDimension(w, dp(112));
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
      int w = r - l;
      commands.get(0).layout(dp(3), dp(2), dp(59), dp(94));
      for (int i = 1; i < commands.size(); i++)
        commands.get(i).layout(dp(62), dp(2 + 48 * (i - 1)), w - dp(3), dp(46 + 48 * (i - 1)));
      caption.layout(0, dp(96), w, dp(112));
    }

    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.drawLine(getWidth() - 1, dp(8), getWidth() - 1, dp(104), rule);
    }
  }
}
