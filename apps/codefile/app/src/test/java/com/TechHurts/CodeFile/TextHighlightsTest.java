package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import org.junit.Test;

public class TextHighlightsTest {
  @Test
  public void overlappingColorsReplaceOnlyTheSelectedRange() {
    TextHighlights marks = new TextHighlights();
    marks.apply(2, 12, 1);
    marks.apply(5, 8, 2);
    assertEquals(3, marks.ranges().size());
    assertEquals(2, marks.ranges().get(0).start);
    assertEquals(5, marks.ranges().get(0).end);
    assertEquals(2, marks.ranges().get(1).color);
    assertEquals(8, marks.ranges().get(2).start);
    assertEquals(12, marks.ranges().get(2).end);
  }

  @Test
  public void removalSplitsAndLeavesUnselectedTextMarked() {
    TextHighlights marks = new TextHighlights();
    marks.apply(0, 20, 1);
    marks.apply(5, 15, 0);
    assertEquals(2, marks.ranges().size());
    assertEquals(5, marks.ranges().get(0).end);
    assertEquals(15, marks.ranges().get(1).start);
    marks.clear();
    assertTrue(marks.ranges().isEmpty());
  }
}
