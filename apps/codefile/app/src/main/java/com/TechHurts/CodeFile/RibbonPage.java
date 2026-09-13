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
final class RibbonPage extends ViewGroup {
  private final List<CommandGroup> groups = new ArrayList<>();
  private int surface, foreground, muted, accent;

  RibbonPage(Context context, String[] captions, List<Button> commands, int[][] order) {
    super(context);
    setContentDescription("Grouped command ribbon");
    for (int i = 0; i < captions.length; i++) {
      List<Button> group = new ArrayList<>();
      for (int index : order[i]) group.add(commands.get(index));
      CommandGroup view = new CommandGroup(context, captions[i], group);
      groups.add(view);
      addView(view);
    }
  }

  void setColors(int surface, int foreground, int muted, int accent) {
    this.surface = surface;
    this.foreground = foreground;
    this.muted = muted;
    this.accent = accent;
    setBackgroundColor(surface);
    for (CommandGroup group : groups) group.tint();
    invalidate();
  }

  protected void onMeasure(int ws, int hs) {
    int width = MeasureSpec.getSize(ws);
    int each = Math.min(width / Math.max(1, groups.size()), dp(228));
    for (CommandGroup group : groups)
      group.measure(
          MeasureSpec.makeMeasureSpec(each, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(dp(120), MeasureSpec.EXACTLY));
    setMeasuredDimension(width, dp(120));
  }

  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int left = 0;
    for (CommandGroup group : groups) {
      int width = group.getMeasuredWidth();
      group.layout(left, 0, left + width, b - t);
      left += width;
    }
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private Drawable commandBackground() {
    GradientDrawable fill = new GradientDrawable();
    fill.setColor(surface);
    fill.setCornerRadius(dp(3));
    return new RippleDrawable(ColorStateList.valueOf((accent & 0xffffff) | 0x33000000), fill, null);
  }

  private String shortLabel(Button button) {
    String label = String.valueOf(button.getContentDescription());
    if (label.equals("Enable navigation")) return "Enable";
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
    private final Button launcher;
    private final Paint rule = new Paint();
    private boolean compact;

    CommandGroup(Context context, String name, List<Button> commands) {
      super(context);
      setWillNotDraw(false);
      this.commands = commands;
      for (int i = 0; i < commands.size(); i++) {
        Button command = commands.get(i);
        command.setText(commandText(command, i == 0));
        command.setTextSize(11);
        command.setGravity(i == 0 ? Gravity.CENTER : Gravity.CENTER_VERTICAL | Gravity.START);
        command.setPadding(dp(5), 0, dp(3), 0);
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
      launcher = new Button(context);
      launcher.setAllCaps(false);
      SpannableStringBuilder launchText =
          new SpannableStringBuilder(commandText(commands.get(0), true));
      int labelStart = launchText.toString().indexOf('\n') + 1;
      launchText.replace(labelStart, launchText.length(), name + " ▾");
      launcher.setText(launchText);
      launcher.setContentDescription(name + " commands");
      launcher.setTextSize(11);
      launcher.setOnClickListener(v -> showCommands());
      addView(launcher);
    }

    private CharSequence menuLabel(Button command) {
      CharSequence text = command.getText();
      int length = Character.charCount(Character.codePointAt(text, 0));
      return new SpannableStringBuilder(text.subSequence(0, length))
          .append("  ")
          .append(command.getContentDescription());
    }

    private void showCommands() {
      PopupMenu menu = new PopupMenu(getContext(), launcher);
      for (int i = 0; i < commands.size(); i++)
        menu.getMenu().add(0, i, i, menuLabel(commands.get(i)));
      menu.setOnMenuItemClickListener(
          item -> {
            commands.get(item.getItemId()).performClick();
            return true;
          });
      menu.show();
    }

    void tint() {
      caption.setTextColor(muted);
      for (Button command : commands) {
        command.setTextColor(foreground);
        command.setBackground(commandBackground());
      }
      launcher.setTextColor(foreground);
      launcher.setBackground(commandBackground());
      rule.setColor((muted & 0xffffff) | 0x55000000);
      invalidate();
    }

    protected void onMeasure(int ws, int hs) {
      int w = MeasureSpec.getSize(ws);
      compact = w < dp(138);
      int primary = dp(58), secondary = Math.max(0, w - primary - dp(12));
      for (int i = 0; i < commands.size(); i++) {
        Button button = commands.get(i);
        button.setVisibility(compact ? GONE : VISIBLE);
        button.measure(
            MeasureSpec.makeMeasureSpec(i == 0 ? primary : secondary, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(dp(i == 0 ? 96 : 48), MeasureSpec.EXACTLY));
      }
      launcher.setVisibility(compact ? VISIBLE : GONE);
      launcher.measure(
          MeasureSpec.makeMeasureSpec(Math.max(0, w - dp(12)), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(dp(96), MeasureSpec.EXACTLY));
      caption.measure(
          MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(dp(20), MeasureSpec.EXACTLY));
      setMeasuredDimension(w, dp(120));
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
      int w = r - l;
      commands.get(0).layout(dp(6), dp(2), dp(64), dp(98));
      for (int i = 1; i < commands.size(); i++)
        commands.get(i).layout(dp(64), dp(2 + 48 * (i - 1)), w - dp(6), dp(2 + 48 * i));
      launcher.layout(dp(6), dp(2), w - dp(6), dp(98));
      caption.layout(0, dp(100), w, dp(120));
    }

    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.drawLine(getWidth() - 1, dp(8), getWidth() - 1, dp(112), rule);
    }
  }
}
