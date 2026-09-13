package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.*;

/** Searchable native navigation results with declaration context and recycled preview rows. */
final class SymbolBrowser extends LinearLayout {
  static final class Entry {
    final String name, info, uri;
    final int line, column;

    Entry(String name, String info, String uri, int line, int column) {
      this.name = name;
      this.info = info;
      this.uri = uri;
      this.line = line;
      this.column = column;
    }
  }

  private final List<Entry> entries, visible = new ArrayList<>();
  private final String[] lines;
  private final int foreground, muted, surface, accent;
  private final Typeface font;
  private final Function<String, CharSequence> title;
  private final TextView count;
  private final Results adapter = new Results();

  SymbolBrowser(
      Context context,
      String source,
      List<Entry> entries,
      int foreground,
      int muted,
      int surface,
      int accent,
      Typeface font,
      Function<String, CharSequence> title,
      Consumer<Entry> choose) {
    super(context);
    this.entries = entries;
    lines = source.split("\n", -1);
    this.foreground = foreground;
    this.muted = muted;
    this.surface = surface;
    this.accent = accent;
    this.font = font;
    this.title = title;
    setOrientation(VERTICAL);
    EditText search = new EditText(context);
    search.setSingleLine(true);
    search.setHint("Search symbols, details or code");
    search.setContentDescription("Search symbols");
    search.setTextColor(foreground);
    search.setHintTextColor(muted);
    addView(search, new LayoutParams(-1, dp(48)));
    count = text(12, muted);
    addView(count);
    ListView results = new ListView(context);
    results.setContentDescription("Symbol results");
    results.setDividerHeight(dp(6));
    results.setAdapter(adapter);
    results.setOnItemClickListener(
        (parent, view, position, id) -> choose.accept(visible.get(position)));
    addView(results, new LayoutParams(-1, 0, 1));
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            filter(s.toString());
          }

          public void afterTextChanged(Editable value) {}
        });
    filter("");
  }

  private void filter(String query) {
    String term = query.trim().toLowerCase(Locale.ROOT);
    visible.clear();
    for (Entry entry : entries)
      if ((entry.name + " " + entry.info + " " + declaration(entry))
          .toLowerCase(Locale.ROOT)
          .contains(term)) visible.add(entry);
    count.setText(visible.size() + " / " + entries.size() + " symbols · tap to jump");
    adapter.notifyDataSetChanged();
  }

  private String declaration(Entry entry) {
    return entry.uri.isEmpty() && entry.line >= 0 && entry.line < lines.length
        ? lines[entry.line]
        : "";
  }

  private String preview(Entry entry) {
    if (!entry.uri.isEmpty()) return "External source · " + entry.uri;
    StringBuilder text = new StringBuilder();
    for (int line = Math.max(0, entry.line - 1);
        line <= Math.min(lines.length - 1, entry.line + 1);
        line++) {
      if (text.length() > 0) text.append('\n');
      text.append(line == entry.line ? "› " : "  ").append(line + 1).append("  ");
      String code = lines[line];
      text.append(code.length() > 240 ? code.substring(0, 240) + "…" : code);
    }
    return text.toString();
  }

  private TextView text(int size, int color) {
    TextView text = new TextView(getContext());
    text.setTextSize(size);
    text.setTextColor(color);
    text.setPadding(dp(8), dp(4), dp(8), dp(4));
    return text;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private final class Results extends BaseAdapter {
    public int getCount() {
      return visible.size();
    }

    public Entry getItem(int position) {
      return visible.get(position);
    }

    public long getItemId(int position) {
      return position;
    }

    public View getView(int position, View recycled, ViewGroup parent) {
      LinearLayout row = recycled instanceof LinearLayout ? (LinearLayout) recycled : createRow();
      Entry entry = getItem(position);
      ((TextView) row.getChildAt(0)).setText(title.apply(entry.name));
      ((TextView) row.getChildAt(1))
          .setText(entry.info + " · line " + (entry.line + 1) + ", column " + (entry.column + 1));
      ((TextView) row.getChildAt(2)).setText(preview(entry));
      return row;
    }

    private LinearLayout createRow() {
      LinearLayout row = new LinearLayout(getContext());
      row.setOrientation(VERTICAL);
      GradientDrawable background = new GradientDrawable();
      background.setColor(surface);
      background.setCornerRadius(dp(6));
      background.setStroke(dp(1), (accent & 0xffffff) | 0x65000000);
      row.setBackground(background);
      row.addView(text(15, accent));
      row.addView(text(12, muted));
      TextView code = text(12, foreground);
      code.setTypeface(font);
      row.addView(code);
      return row;
    }
  }
}
