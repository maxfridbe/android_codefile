package com.TechHurts.CodeFile;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.*;
import android.content.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import io.github.rosemoe.sora.widget.CodeEditor;
import java.io.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NativeActivityTest {
  private <T> T find(View view, Class<T> cls) {
    if (cls.isInstance(view)) return cls.cast(view);
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++) {
        T found = find(g.getChildAt(i), cls);
        if (found != null) return found;
      }
    }
    return null;
  }

  private Button button(View view, String name) {
    if (view instanceof Button && name.equals(String.valueOf(view.getContentDescription())))
      return (Button) view;
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++) {
        Button found = button(g.getChildAt(i), name);
        if (found != null) return found;
      }
    }
    return null;
  }

  private void noBrowser(View v) {
    assertFalse(v.getClass().getName().startsWith("android.webkit."));
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) noBrowser(g.getChildAt(i));
    }
  }

  @Test
  public void nativeActivityShowsReadOnlyEditorAndNativeDialogs() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      assertNotNull(editor);
      assertFalse(editor.isEditable());
      assertTrue(editor.getText().toString().contains("class Welcome"));
      noBrowser(root);
      button(root, "Settings").performClick();
      AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      assertTrue(dialog.isShowing());
      assertNotNull(button(dialog.getWindow().getDecorView(), "Share debug log (.gz)"));
      noBrowser(dialog.getWindow().getDecorView());
      dialog.dismiss();
    }
  }

  @Test
  public void sourceFromShareIntentIsNotExecutable() {
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "<script>alert('no')</script>");
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      shadowOf(Looper.getMainLooper()).idle();
      CodeEditor editor = find(c.get().getWindow().getDecorView(), CodeEditor.class);
      assertEquals("<script>alert('no')</script>", editor.getText().toString());
    }
  }

  @Test
  public void diagnosticsAreActualGzipAndNeverIncludeSource() {
    Application app = RuntimeEnvironment.getApplication();
    DebugLog log = new DebugLog(app);
    log.event("native.test");
    try {
      File gz = log.gzip(app);
      String text;
      try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(gz))) {
        text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      }
      assertTrue(text.contains("native.test"));
      assertTrue(text.contains("UI: native Android"));
      assertFalse(text.contains("<script>"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void bundledGrammarsLoadIntoNativeEditor() throws Exception {
    NativeSyntax syntax = new NativeSyntax();
    syntax.load(RuntimeEnvironment.getApplication());
    assertTrue(syntax.names().size() >= 66);
    CodeEditor editor = new CodeEditor(RuntimeEnvironment.getApplication());
    try {
      for (String name :
          new String[] {"a.java", "a.ts", "a.py", "a.rs", "a.kt", "a.cpp", "a.yaml"}) {
        String lang = syntax.detect(name);
        assertNotEquals("plain", lang);
        syntax.apply(editor, lang, "darcula");
        assertTrue(editor.getEditorLanguage().getClass().getName().contains("TextMate"));
      }
      for (String language : syntax.names())
        if (!java.util.Arrays.asList("plain", "csv", "tsv").contains(language))
          syntax.apply(editor, language, "darcula");
    } finally {
      editor.release();
    }
  }

  @Test
  public void workspaceReflowsAcrossPhoneTabletAndSeparatingHinge() throws Exception {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity a = c.get();
      java.lang.reflect.Field field = MainActivity.class.getDeclaredField("workspace");
      field.setAccessible(true);
      ViewGroup workspace = (ViewGroup) field.get(a);
      CodeEditor editor = find(a.getWindow().getDecorView(), CodeEditor.class);
      String source = editor.getText().toString();
      int w = a.dp(1200), h = a.dp(800);
      workspace.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
      workspace.layout(0, 0, w, h);
      assertEquals(0, workspace.getChildAt(1).getLeft());
      assertTrue(workspace.getChildAt(1).getTop() > 0);
      w = a.dp(400);
      workspace.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
      workspace.layout(0, 0, w, h);
      assertEquals(0, workspace.getChildAt(1).getLeft());
      assertTrue(workspace.getChildAt(1).getTop() > 0);
      assertSame(editor, find(workspace, CodeEditor.class));
      assertEquals(source, editor.getText().toString());
      assertTrue(editor.isScalable());
      assertTrue(editor.isWordwrap());
      java.lang.reflect.Field hinge = workspace.getClass().getDeclaredField("hinge");
      hinge.setAccessible(true);
      int[] origin = new int[2];
      workspace.getLocationInWindow(origin);
      hinge.set(
          workspace,
          new android.graphics.Rect(
              origin[0] + a.dp(490), origin[1], origin[0] + a.dp(510), origin[1] + h));
      w = a.dp(1000);
      workspace.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
      workspace.layout(0, 0, w, h);
      assertEquals(a.dp(510), workspace.getChildAt(1).getLeft());
      hinge.set(
          workspace,
          new android.graphics.Rect(
              origin[0], origin[1] + a.dp(300), origin[0] + w, origin[1] + a.dp(320)));
      workspace.requestLayout();
      workspace.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
      workspace.layout(0, 0, w, h);
      assertEquals(0, workspace.getChildAt(1).getTop());
      assertEquals(a.dp(320), workspace.getChildAt(0).getTop());
    }
  }

  @Test
  public void nativeZoomAndFitWidthDoNotChangeSource() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      View root = c.get().getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      String text = editor.getText().toString();
      float size = editor.getTextSizePx();
      button(root, "Zoom +").performClick();
      assertTrue(editor.getTextSizePx() > size);
      button(root, "Wrap").performClick();
      assertFalse(editor.isWordwrap());
      button(root, "Fit width").performClick();
      assertTrue(editor.isWordwrap());
      assertEquals(text, editor.getText().toString());
    }
  }

  private void touch(
      CodeEditor editor,
      long start,
      long time,
      int action,
      float x1,
      float y1,
      Float x2,
      Float y2) {
    int count = x2 == null ? 1 : 2;
    MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[count];
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[count];
    for (int i = 0; i < count; i++) {
      props[i] = new MotionEvent.PointerProperties();
      props[i].id = i;
      props[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
      coords[i] = new MotionEvent.PointerCoords();
      coords[i].x = i == 0 ? x1 : x2;
      coords[i].y = i == 0 ? y1 : y2;
      coords[i].pressure = 1;
      coords[i].size = 1;
    }
    MotionEvent event =
        MotionEvent.obtain(
            start,
            time,
            action,
            count,
            props,
            coords,
            0,
            0,
            1,
            1,
            0,
            0,
            android.view.InputDevice.SOURCE_TOUCHSCREEN,
            0);
    editor.dispatchTouchEvent(event);
    event.recycle();
  }

  @Test
  public void nativeTouchPinchZoomChangesScaleWithoutEditing() {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      CodeEditor editor = find(c.get().getWindow().getDecorView(), CodeEditor.class);
      editor.measure(
          View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
      editor.layout(0, 0, 800, 600);
      String text = editor.getText().toString();
      float oldSize = editor.getTextSizePx();
      long t = android.os.SystemClock.uptimeMillis();
      touch(editor, t, t, MotionEvent.ACTION_DOWN, 250, 250, null, null);
      touch(
          editor,
          t,
          t + 30,
          MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
          250,
          250,
          450f,
          250f);
      for (int i = 1; i <= 8; i++)
        touch(
            editor,
            t,
            t + 30 + i * 30,
            MotionEvent.ACTION_MOVE,
            250 - i * 12,
            250,
            450f + i * 12,
            250f);
      touch(
          editor,
          t,
          t + 300,
          MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
          154,
          250,
          546f,
          250f);
      touch(editor, t, t + 320, MotionEvent.ACTION_UP, 154, 250, null, null);
      assertTrue("Native pinch should zoom", editor.getTextSizePx() > oldSize);
      assertEquals(text, editor.getText().toString());
    }
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  @Config(sdk = 28, qualifiers = "w480dp-h1000dp")
  public void nativePortraitRendersToBitmap() throws Exception {
    try (ActivityController<MainActivity> c =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity a = c.get();
      View root = a.getWindow().getDecorView();
      java.lang.reflect.Method add =
          MainActivity.class.getDeclaredMethod(
              "addDocument", String.class, String.class, String.class, String.class);
      add.setAccessible(true);
      add.invoke(
          a,
          "Reflow.java",
          "/** A long source line must reflow without changing its logical line number: this"
              + " comment deliberately extends beyond the width of a phone screen and remains fully"
              + " readable through native text layout. */\n"
              + "class Reflow {\n"
              + "    int answer = 42;\n"
              + "}\n",
          "UTF-8",
          "");
      int width = 480, height = 1000;
      root.measure(
          View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, width, height);
      CodeEditor renderedEditor = find(root, CodeEditor.class);
      java.lang.reflect.Field ready = MainActivity.class.getDeclaredField("syntaxReady");
      ready.setAccessible(true);
      for (int attempt = 0; attempt < 100; attempt++) {
        shadowOf(Looper.getMainLooper()).idle();
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        if (ready.getBoolean(a)
            && renderedEditor.getLayout().getRowCount() > renderedEditor.getText().getLineCount())
          break;
        Thread.sleep(50);
      }
      assertTrue("Grammars initialized", ready.getBoolean(a));
      assertTrue(
          "Reflow: width="
              + renderedEditor.getWidth()
              + " height="
              + renderedEditor.getHeight()
              + " wrap="
              + renderedEditor.isWordwrap()
              + " rows="
              + renderedEditor.getLayout().getRowCount(),
          renderedEditor.getLayout().getRowCount() > renderedEditor.getText().getLineCount());
      for (int frame = 0; frame < 20; frame++) {
        Thread.sleep(50);
        shadowOf(Looper.getMainLooper()).idle();
      }
      android.graphics.Bitmap bitmap =
          android.graphics.Bitmap.createBitmap(
              width, height, android.graphics.Bitmap.Config.ARGB_8888);
      root.draw(new android.graphics.Canvas(bitmap));
      File out = new File("build/native-portrait.png");
      out.getParentFile().mkdirs();
      try (FileOutputStream stream = new FileOutputStream(out)) {
        assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream));
      }
      assertTrue(find(root, CodeEditor.class).getHeight() > 200);
    }
  }

  private void assertFlowBounds(View view) {
    if (view instanceof ActionFlow
        || view instanceof RibbonPage
        || view.getClass().getSimpleName().equals("CommandGroup")) {
      ViewGroup flow = (ViewGroup) view;
      for (int i = 0; i < flow.getChildCount(); i++) {
        View child = flow.getChildAt(i);
        if (child.getVisibility() != View.VISIBLE) continue;
        assertTrue(child.getLeft() >= 0);
        assertTrue(child.getRight() <= flow.getWidth());
        assertTrue(child.getBottom() <= flow.getHeight());
      }
    }
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View child = ((ViewGroup) view).getChildAt(i);
        if (child.getVisibility() == View.VISIBLE) assertFlowBounds(child);
      }
  }

  @Test
  public void ribbonFitsNarrowWidthsAndCollapsesFromActualEditorScrollEvents() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      for (int width : new int[] {320, 360, 411, 600, 840})
        for (String group : new String[] {"File", "View", "Code"}) {
          expandTab(root, group);
          root.measure(
              View.MeasureSpec.makeMeasureSpec(activity.dp(width), View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(activity.dp(900), View.MeasureSpec.EXACTLY));
          root.layout(0, 0, activity.dp(width), activity.dp(900));
          assertFlowBounds(root);
        }
      editor.dispatchEvent(
          new io.github.rosemoe.sora.event.ScrollEvent(
              editor,
              0,
              0,
              0,
              activity.dp(180),
              io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG));
      assertNotNull(button(root, "Expand ribbon"));
      assertFalse(button(root, "Format").isShown());
      expandTab(root, "Code");
      assertTrue(button(root, "Format").isShown());
      editor.dispatchEvent(
          new io.github.rosemoe.sora.event.ScrollEvent(
              editor, 0, 80, 0, 0, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG));
      assertNotNull(button(root, "Collapse ribbon"));
    }
  }

  @Test
  public void themeSelectionImmediatelyRepaintsWithoutReplacingEditor() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      String source = editor.getText().toString();
      button(root, "Settings").performClick();
      AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      Spinner theme = find(dialog.getWindow().getDecorView(), Spinner.class);
      theme.setSelection(5);
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals(
          "High contrast",
          activity.getSharedPreferences("native-settings", 0).getString("theme", ""));
      assertEquals(
          android.graphics.Color.BLACK,
          editor
              .getColorScheme()
              .getColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND));
      theme.setSelection(4);
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals(
          "Paper", activity.getSharedPreferences("native-settings", 0).getString("theme", ""));
      assertEquals(
          android.graphics.Color.parseColor("#FAFAF7"),
          editor
              .getColorScheme()
              .getColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND));
      assertSame(editor, find(root, CodeEditor.class));
      assertEquals(source, editor.getText().toString());
      dialog.dismiss();
    }
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  @Config(sdk = 28, qualifiers = "w480dp-h850dp")
  public void settingsUsesCollapsibleGroupsAndRendersNatively() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      View root = controller.get().getWindow().getDecorView();
      button(root, "Settings").performClick();
      AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      View settings = dialog.getWindow().getDecorView();
      assertFalse(button(settings, "Share debug log (.gz)").isShown());
      button(settings, "About & diagnostics").performClick();
      assertTrue(button(settings, "Share debug log (.gz)").isShown());
      button(settings, "About & diagnostics").performClick();
      settings.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(850, View.MeasureSpec.EXACTLY));
      settings.layout(0, 0, 480, 850);
      shadowOf(Looper.getMainLooper()).idle();
      android.graphics.Bitmap bitmap =
          android.graphics.Bitmap.createBitmap(480, 850, android.graphics.Bitmap.Config.ARGB_8888);
      settings.draw(new android.graphics.Canvas(bitmap));
      try (FileOutputStream stream = new FileOutputStream("build/native-settings.png")) {
        assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream));
      }
      dialog.dismiss();
    }
  }

  private void awaitMatches(CodeEditor editor, int expected) throws Exception {
    for (int attempt = 0; attempt < 100; attempt++) {
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));
      if (editor.getSearcher().hasQuery()
          && editor.getSearcher().getMatchedPositionCount() == expected) return;
      Thread.sleep(20);
    }
    assertEquals(expected, editor.getSearcher().getMatchedPositionCount());
  }

  @Test
  public void searchHighlightsMatchesHandlesOptionsAndRemembersColor() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      // Let the initial grammar callback finish before replacing this test document.
      java.lang.reflect.Field ready = MainActivity.class.getDeclaredField("syntaxReady");
      ready.setAccessible(true);
      for (int i = 0; i < 100 && !ready.getBoolean(activity); i++) {
        shadowOf(Looper.getMainLooper()).idle();
        Thread.sleep(20);
      }
      editor.setText("Alpha alpha ALPHA\n");
      button(root, "Search").performClick();
      AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      View controls = dialog.getWindow().getDecorView();
      EditText query = find(controls, EditText.class);
      query.setText("alpha");
      awaitMatches(editor, 3);
      button(controls, "Next").performClick();
      assertTrue(editor.getCursor().isSelected());
      button(controls, "Match case").performClick();
      awaitMatches(editor, 1);
      button(controls, "Pink highlight").performClick();
      int color = 0x70ff66bb;
      assertEquals(
          color,
          editor
              .getColorScheme()
              .getColor(
                  io.github.rosemoe.sora.widget.schemes.EditorColorScheme.MATCHED_TEXT_BACKGROUND));
      assertEquals(
          color, activity.getSharedPreferences("native-settings", 0).getInt("searchColor", 0));
      button(controls, "Regular expression").performClick();
      query.setText("[");
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));
      assertNotNull(query.getError());
      assertFalse(editor.getSearcher().hasQuery());
      button(controls, "Clear").performClick();
      button(controls, "Next").performClick();
      assertFalse(editor.getSearcher().hasQuery());
      dialog.dismiss();
      activity.onConfigurationChanged(activity.getResources().getConfiguration());
      assertEquals(
          color,
          editor
              .getColorScheme()
              .getColor(
                  io.github.rosemoe.sora.widget.schemes.EditorColorScheme.MATCHED_TEXT_BACKGROUND));
    }
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  public void officeRibbonKeepsGroupBaselinesAndTabSizingOnTablet() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      for (String tab : new String[] {"File", "View", "Code"}) {
        expandTab(root, tab);
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1200, 900);
        assertTrue(button(root, tab).isSelected());
        assertTrue(button(root, tab).getWidth() <= activity.dp(88));
        java.lang.reflect.Field pages = MainActivity.class.getDeclaredField("ribbonPages");
        pages.setAccessible(true);
        for (View page : (java.util.List<View>) pages.get(activity))
          if (page.isShown()) {
            assertEquals(activity.dp(112), page.getHeight());
            ViewGroup groups = (ViewGroup) page;
            assertEquals(groups.getChildAt(0).getTop(), groups.getChildAt(1).getTop());
            assertEquals(groups.getChildAt(0).getBottom(), groups.getChildAt(1).getBottom());
            assertFlowBounds(page);
          }
      }
      android.graphics.Bitmap bitmap =
          android.graphics.Bitmap.createBitmap(1200, 900, android.graphics.Bitmap.Config.ARGB_8888);
      root.draw(new android.graphics.Canvas(bitmap));
      try (FileOutputStream stream = new FileOutputStream("build/native-tablet-ribbon.png")) {
        assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream));
      }
    }
  }

  private void expandTab(View root, String name) {
    Button tab = button(root, name);
    tab.performClick();
    shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
    CollapsingRibbon body = find(root, CollapsingRibbon.class);
    if (body.fraction() < .99f) {
      tab.performClick();
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
    }
  }

  @Test
  public void activeTabTogglesAnimatedRibbonButFilenameStaysVisible() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CollapsingRibbon ribbon = find(root, CollapsingRibbon.class);
      expandTab(root, "File");
      assertEquals(1f, ribbon.fraction(), .001f);
      button(root, "File").performClick();
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
      assertEquals(0f, ribbon.fraction(), .001f);
      assertTrue(button(root, "Close file").isShown());
      button(root, "File").performClick();
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
      assertEquals(1f, ribbon.fraction(), .001f);
      root.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 480, 1000);
      ribbon.consumeScroll(activity.dp(30));
      assertTrue(ribbon.fraction() > 0 && ribbon.fraction() < 1);
      float partial = ribbon.fraction();
      ribbon.consumeScroll(-activity.dp(10));
      assertTrue(ribbon.fraction() > partial);
      assertFalse(find(root, CodeEditor.class).isLineNumberPinned());
    }
  }

  private void finger(
      LineSelectionEditor editor, long start, long time, int action, float x, float y) {
    MotionEvent event = MotionEvent.obtain(start, time, action, x, y, 0);
    editor.dispatchTouchEvent(event);
    event.recycle();
  }

  @Test
  public void slowGutterDragSelectsWholeLinesAndShowsSelectionActions() throws Exception {
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "one\nsecond\nthird\nlast\n");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      LineSelectionEditor editor = find(root, LineSelectionEditor.class);
      shadowOf(Looper.getMainLooper()).idle();
      editor.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
      editor.layout(0, 0, 480, 600);
      int height = editor.getRowHeight();
      long start = android.os.SystemClock.uptimeMillis();
      finger(editor, start, start, MotionEvent.ACTION_DOWN, 2, height * 2.5f);
      finger(editor, start, start + 1500, MotionEvent.ACTION_MOVE, 2, height * 1.5f);
      finger(editor, start, start + 2000, MotionEvent.ACTION_UP, 2, height * 1.5f);
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
      assertEquals(1, editor.firstSelectedLine());
      assertEquals(2, editor.lastSelectedLine());
      assertEquals(1, editor.getCursor().getLeftLine());
      assertEquals(3, editor.getCursor().getRightLine());
      assertEquals(0, editor.getCursor().getRightColumn());
      assertNotNull(button(root, "Ask LLM"));
      java.lang.reflect.Method range = MainActivity.class.getDeclaredMethod("selectedLines");
      range.setAccessible(true);
      assertArrayEquals(new int[] {2, 3}, (int[]) range.invoke(activity));
      button(root, "Ask LLM").performClick();
      assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }
  }
}
