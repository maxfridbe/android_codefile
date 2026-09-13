package com.TechHurts.CodeFile;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.List;

/**
 * One horizontal viewport keeps every record's columns aligned. ListView recycles vertical rows.
 */
final class DelimitedTableView extends HorizontalScrollView {
  private final List<List<String>> records;
  private final int foreground, muted, even, odd;

  DelimitedTableView(
      Context context, List<List<String>> records, int foreground, int muted, int even, int odd) {
    super(context);
    this.records = records;
    this.foreground = foreground;
    this.muted = muted;
    this.even = even;
    this.odd = odd;
    ListView list = new ListView(context);
    list.setAdapter(new RecordsAdapter());
    int columns = 1;
    for (List<String> record : records) columns = Math.max(columns, record.size());
    addView(list, new ViewGroup.LayoutParams(dp(56 + 180 * columns), dp(400)));
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private TextView cell(String value, int color) {
    TextView text = new TextView(getContext());
    text.setText(value);
    text.setTextColor(color);
    text.setTextSize(13);
    text.setPadding(dp(12), dp(8), dp(12), dp(8));
    text.setTextIsSelectable(true);
    return text;
  }

  private final class RecordsAdapter extends BaseAdapter {
    public int getCount() {
      return records.size();
    }

    public Object getItem(int index) {
      return records.get(index);
    }

    public long getItemId(int index) {
      return index;
    }

    public View getView(int index, View recycled, ViewGroup parent) {
      LinearLayout row =
          recycled instanceof LinearLayout
              ? (LinearLayout) recycled
              : new LinearLayout(getContext());
      row.removeAllViews();
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setBackgroundColor(index % 2 == 0 ? even : odd);
      row.addView(
          cell(Integer.toString(index + 1), muted), new LinearLayout.LayoutParams(dp(56), -2));
      for (String value : records.get(index))
        row.addView(cell(value, foreground), new LinearLayout.LayoutParams(dp(180), -2));
      return row;
    }
  }
}
