package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;
import io.noties.markwon.*;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;

/** CommonMark rendered into native Android spans; no browser or remote resource loader. */
final class MarkdownPreview {
  static Markwon renderer(Context context, AppTheme theme, Typeface font) {
    return Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(theme.accent, theme.muted, theme.background))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(
            new AbstractMarkwonPlugin() {
              @Override
              public void configureTheme(MarkwonTheme.Builder builder) {
                builder
                    .linkColor(theme.accent)
                    .codeTextColor(theme.text)
                    .codeBackgroundColor(theme.surface)
                    .codeTypeface(font)
                    .blockQuoteColor(theme.accent)
                    .headingBreakColor(theme.muted);
              }
            })
        .build();
  }

  static TextView view(Context context, AppTheme theme) {
    TextView view = new TextView(context);
    view.setContentDescription("Markdown preview");
    view.setTextColor(theme.text);
    view.setLinkTextColor(theme.accent);
    view.setBackgroundColor(theme.background);
    view.setTextSize(16);
    int padding = Math.round(12 * context.getResources().getDisplayMetrics().density);
    view.setPadding(padding, padding, padding, padding);
    view.setTextIsSelectable(true);
    return view;
  }
}
