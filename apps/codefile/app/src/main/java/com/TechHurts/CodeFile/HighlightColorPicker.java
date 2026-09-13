package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.Gravity;
import android.widget.*;
import java.util.Locale;

/** Native RGB/hex picker with a live sample. Applying belongs to the caller. */
final class HighlightColorPicker extends LinearLayout {
  private final SeekBar[] channels = new SeekBar[3];
  private final EditText hex;
  private final TextView preview;
  private boolean updating;
  private int color;

  HighlightColorPicker(Context context, int initial, int foreground) {
    super(context);
    setOrientation(VERTICAL);
    setPadding(dp(16), dp(8), dp(16), dp(8));
    preview = new TextView(context);
    preview.setText("Selected text");
    preview.setGravity(Gravity.CENTER);
    preview.setTextSize(18);
    addView(preview, new LayoutParams(-1, dp(56)));
    hex = new EditText(context);
    hex.setSingleLine(true);
    hex.setTextColor(foreground);
    hex.setHint("#RRGGBB");
    hex.setContentDescription("Highlight hex color");
    addView(hex, new LayoutParams(-1, dp(48)));
    String[] names = {"Red", "Green", "Blue"};
    for (int i = 0; i < 3; i++) {
      TextView label = new TextView(context);
      label.setText(names[i]);
      label.setTextColor(foreground);
      addView(label);
      SeekBar slider = new SeekBar(context);
      slider.setMax(255);
      slider.setContentDescription(names[i]);
      channels[i] = slider;
      addView(slider, new LayoutParams(-1, dp(40)));
      slider.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar view, int progress, boolean user) {
              if (!updating)
                setColor(
                    Color.rgb(
                        channels[0].getProgress(),
                        channels[1].getProgress(),
                        channels[2].getProgress()));
            }

            public void onStartTrackingTouch(SeekBar view) {}

            public void onStopTrackingTouch(SeekBar view) {}
          });
    }
    hex.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (updating) return;
            if (!s.toString().matches("#[0-9a-fA-F]{6}")) {
              hex.setError("Use #RRGGBB");
              return;
            }
            setColor(Color.parseColor(s.toString()));
          }

          public void afterTextChanged(Editable text) {}
        });
    setColor(initial);
  }

  void setColor(int color) {
    updating = true;
    this.color = color | 0xff000000;
    channels[0].setProgress(Color.red(color));
    channels[1].setProgress(Color.green(color));
    channels[2].setProgress(Color.blue(color));
    hex.setText(String.format(Locale.ROOT, "#%06X", color & 0xffffff));
    hex.setError(null);
    preview.setBackgroundColor(this.color);
    preview.setTextColor(
        androidx.core.graphics.ColorUtils.calculateLuminance(this.color) > .4
            ? Color.BLACK
            : Color.WHITE);
    updating = false;
  }

  int color() {
    if (hex.getError() != null) throw new IllegalArgumentException("Use a six-digit hex color");
    return color;
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }
}
