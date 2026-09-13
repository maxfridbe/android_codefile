package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import android.view.*;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PanGestureTest {
  @Test
  public void horizontalGutterDragPansWithoutSelectionOrRibbonChangesAtDifferentSizes() {
    FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
    LineSelectionEditor editor = new LineSelectionEditor(parent.getContext());
    parent.addView(editor);
    editor.setWordwrap(false);
    editor.setText("long source line ".repeat(100) + "\nsecond");
    editor.setSourceLabels(new String[] {"1", "2"});
    int[] ribbon = {0};
    editor.setOnRibbonDrag(delta -> ribbon[0]++);
    for (int size : new int[] {8, 14, 26, 40}) {
      editor.setTextSize(size);
      parent.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
      parent.layout(0, 0, 480, 600);
      editor.getScroller().startScroll(editor.getOffsetX(), 0, -editor.getOffsetX(), 0, 0);
      editor.getScroller().abortAnimation();
      long time = android.os.SystemClock.uptimeMillis();
      finger(editor, time, 0, MotionEvent.ACTION_DOWN, 2, 100);
      finger(editor, time, 20, MotionEvent.ACTION_MOVE, -60, 101);
      finger(editor, time, 40, MotionEvent.ACTION_MOVE, -140, 102);
      editor.getScroller().abortAnimation();
      assertFalse(editor.isSelectingLines());
      assertFalse(editor.getCursor().isSelected());
      assertTrue("Pan leaves gutter at size " + size, editor.getOffsetX() > 0);
      assertEquals(480, editor.getWidth());
      assertEquals(0, ribbon[0]);
      finger(editor, time, 60, MotionEvent.ACTION_CANCEL, -140, 102);
    }
    editor.release();
  }

  private void finger(
      LineSelectionEditor editor, long start, long elapsed, int action, float x, float y) {
    MotionEvent event = MotionEvent.obtain(start, start + elapsed, action, x, y, 0);
    editor.dispatchTouchEvent(event);
    event.recycle();
  }
}
