package com.TechHurts.CodeFile;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.*;

/** Recycled native tree rows; expansion never creates nested view hierarchies. */
final class JsonTreeView extends ListView {
  private final List<JsonTree.Node> visible = new ArrayList<>();
  private final List<Integer> depths = new ArrayList<>();
  private final JsonTree.Node root;
  private final AppTheme theme;
  private final BiFunction<Integer, String, CharSequence> icon;
  private final Rows rows = new Rows();

  JsonTreeView(
      Context context,
      JsonTree.Node root,
      AppTheme theme,
      BiFunction<Integer, String, CharSequence> icon,
      Consumer<JsonTree.Node> inspect) {
    super(context);
    this.root = root;
    this.theme = theme;
    this.icon = icon;
    setContentDescription("JSON tree");
    setBackgroundColor(theme.background);
    setAdapter(rows);
    setOnItemClickListener(
        (parent, view, position, id) -> {
          JsonTree.Node node = visible.get(position);
          if (node.container()) {
            node.expanded = !node.expanded;
            rebuild();
          } else inspect.accept(node);
        });
    rebuild();
  }

  private void rebuild() {
    visible.clear();
    depths.clear();
    append(root, 0);
    rows.notifyDataSetChanged();
  }

  private void append(JsonTree.Node node, int depth) {
    visible.add(node);
    depths.add(depth);
    if (node.expanded) for (JsonTree.Node child : node.children) append(child, depth + 1);
  }

  private final class Rows extends BaseAdapter {
    public int getCount() {
      return visible.size();
    }

    public JsonTree.Node getItem(int position) {
      return visible.get(position);
    }

    public long getItemId(int position) {
      return position;
    }

    public View getView(int position, View recycled, ViewGroup parent) {
      TextView row =
          recycled instanceof TextView ? (TextView) recycled : new TextView(getContext());
      JsonTree.Node node = getItem(position);
      String value = node.container() ? node.children.size() + " items" : node.value;
      if (value.length() > 160) value = value.substring(0, 160) + "…";
      row.setText(
          icon.apply(
              node.container() ? (node.expanded ? 0xf0140 : 0xf0142) : 0xf0224,
              node.key + " · " + node.type + "\n" + value));
      row.setTextColor(node.container() ? theme.accent : theme.text);
      row.setTextSize(14);
      float density = getResources().getDisplayMetrics().density;
      row.setPadding(
          Math.round((8 + Math.min(depths.get(position), 12) * 14) * density),
          Math.round(10 * density),
          Math.round(8 * density),
          Math.round(10 * density));
      return row;
    }
  }
}
