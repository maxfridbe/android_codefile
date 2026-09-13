package com.TechHurts.CodeFile;

import java.util.*;

/** Session annotations in UTF-16 offsets, independent of syntax and search results. */
final class TextHighlights {
  static final class Range {
    final int start, end, color;

    Range(int start, int end, int color) {
      this.start = start;
      this.end = end;
      this.color = color;
    }
  }

  private final List<Range> ranges = new ArrayList<>();

  List<Range> ranges() {
    return Collections.unmodifiableList(ranges);
  }

  void apply(int start, int end, int color) {
    if (start < 0 || end <= start) throw new IllegalArgumentException("Select text to highlight");
    List<Range> next = new ArrayList<>();
    for (Range range : ranges) {
      if (range.end <= start || range.start >= end) {
        next.add(range);
        continue;
      }
      if (range.start < start) next.add(new Range(range.start, start, range.color));
      if (range.end > end) next.add(new Range(end, range.end, range.color));
    }
    if (color != 0) next.add(new Range(start, end, color));
    next.sort(Comparator.comparingInt(range -> range.start));
    ranges.clear();
    ranges.addAll(next);
  }

  void clear() {
    ranges.clear();
  }
}
