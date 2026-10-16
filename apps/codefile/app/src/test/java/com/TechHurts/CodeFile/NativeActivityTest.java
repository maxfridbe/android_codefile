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
import org.robolectric.shadows.ShadowDialog;

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
  public void normalSelectionRetainsNativeCopyToolbar() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      CodeEditor editor = find(controller.get().getWindow().getDecorView(), CodeEditor.class);
      io.github.rosemoe.sora.widget.component.EditorTextActionWindow toolbar =
          editor.getComponent(io.github.rosemoe.sora.widget.component.EditorTextActionWindow.class);
      assertTrue(toolbar.isEnabled());
      editor.setSelectionRegion(0, 0, 0, 4);
      toolbar.displayWindow();
      View copy = toolbar.getView().findViewById(io.github.rosemoe.sora.R.id.panel_btn_copy);
      assertEquals(View.VISIBLE, copy.getVisibility());
      copy.performClick();
      android.content.ClipboardManager clipboard =
          (android.content.ClipboardManager)
              controller.get().getSystemService(Context.CLIPBOARD_SERVICE);
      assertEquals(
          editor.getText().getLineString(0).substring(0, 4),
          clipboard.getPrimaryClip().getItemAt(0).getText().toString());
    }
  }

  @Test
  public void incomingClipTextCanBeSavedAsUtf8() throws Exception {
    Intent incoming =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "shared.yaml");
    incoming.setClipData(ClipData.newPlainText("source", "name: café\n"));
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, incoming).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals("name: café\n", find(root, CodeEditor.class).getText().toString());
      button(root, "Save").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      org.robolectric.shadows.ShadowActivity.IntentForResult request =
          shadowOf(activity).getNextStartedActivityForResult();
      assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.intent.getAction());
      assertEquals("shared.yaml", request.intent.getStringExtra(Intent.EXTRA_TITLE));
      android.net.Uri uri = android.net.Uri.parse("content://test/output.yaml");
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      shadowOf(activity.getContentResolver()).registerOutputStream(uri, output);
      activity.onActivityResult(request.requestCode, Activity.RESULT_OK, new Intent().setData(uri));
      for (int i = 0; i < 100 && output.size() == 0; i++) {
        Thread.sleep(10);
        shadowOf(Looper.getMainLooper()).idle();
      }
      assertEquals("name: café\n", output.toString("UTF-8"));
    }
  }

  private void settle(MainActivity activity, java.util.function.BooleanSupplier done)
      throws Exception {
    for (int i = 0; i < 200 && !done.getAsBoolean(); i++) {
      Thread.sleep(10);
      shadowOf(Looper.getMainLooper()).idle();
    }
  }

  @Test
  public void remoteGithubFileOffersSaveCopyAndSharesAsAttachment() throws Exception {
    GithubUrl link = GithubUrl.parse("https://github.com/o/r/blob/main/src/Remote.java");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      assertFalse("local file has no Save copy", button(root, "Save copy").isShown());
      activity.addDocument("Remote.java", "class Remote {}\n", "UTF-8", link.raw, link);
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals("class Remote {}\n", find(root, CodeEditor.class).getText().toString());
      assertTrue("remote file offers Save copy", button(root, "Save copy").isShown());

      button(root, "Save copy").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      org.robolectric.shadows.ShadowActivity.IntentForResult request =
          shadowOf(activity).getNextStartedActivityForResult();
      assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.intent.getAction());
      assertEquals("application/octet-stream", request.intent.getType());
      assertEquals("Remote.java", request.intent.getStringExtra(Intent.EXTRA_TITLE));

      shadowOf(activity).clearNextStartedActivities();
      button(root, "Share file").performClick();
      settle(activity, () -> shadowOf(activity).peekNextStartedActivity() != null);
      Intent chooser = shadowOf(activity).getNextStartedActivity();
      Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
      assertEquals(Intent.ACTION_SEND, send.getAction());
      assertFalse("file, not pasted text", send.hasExtra(Intent.EXTRA_TEXT));
      android.net.Uri stream = send.getParcelableExtra(Intent.EXTRA_STREAM);
      assertEquals("content", stream.getScheme());
      assertEquals(
          "class Remote {}\n",
          new String(
              activity.getContentResolver().openInputStream(stream).readAllBytes(),
              java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  public void localFileSharesAsAttachmentWithoutSaveCopy() throws Exception {
    Intent incoming =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "notes.md")
            .putExtra(Intent.EXTRA_TEXT, "# Notes\n");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, incoming).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      assertFalse(button(root, "Save copy").isShown());
      button(root, "Share file").performClick();
      settle(activity, () -> shadowOf(activity).peekNextStartedActivity() != null);
      Intent send =
          shadowOf(activity)
              .getNextStartedActivity()
              .getParcelableExtra(Intent.EXTRA_INTENT);
      assertTrue(send.hasExtra(Intent.EXTRA_STREAM));
      assertFalse(send.hasExtra(Intent.EXTRA_TEXT));
    }
  }

  @Test
  public void unsupportedWebLinkIsRejectedWithoutFetching() {
    Intent view =
        new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com/a/blob/b/c"));
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, view).setup()) {
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals(
          "Only GitHub file links (github.com/…/blob/…) can be opened",
          org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }
  }

  private void putOnClipboard(MainActivity activity, String text) {
    ((android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE))
        .setPrimaryClip(ClipData.newPlainText("code", text));
  }

  private String currentTab(View root) {
    return String.valueOf(find(root, Spinner.class).getSelectedItem());
  }

  @Test
  public void clipboardCodeOpensWithSniffedLanguageAndStaysChangeable() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      putOnClipboard(
          activity,
          "import os\n\nclass Repo:\n    def files(self):\n        return os.listdir('.')\n");
      button(root, "Clipboard").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals("clipboard.py", currentTab(root));
      assertTrue(find(root, CodeEditor.class).getText().toString().contains("class Repo"));
      assertTrue(
          org.robolectric.shadows.ShadowToast.getTextOfLatestToast().startsWith("Looks like Python"));

      // The guess is only a starting point: the language picker still offers every language.
      button(root, "Language").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      android.app.AlertDialog picker = (android.app.AlertDialog) ShadowDialog.getLatestDialog();
      assertEquals("Auto-detect from content", picker.getListView().getItemAtPosition(0));
      assertTrue(picker.getListView().getCount() > 60);
    }
  }

  @Test
  public void emptyClipboardOpensNothing() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      String before = currentTab(root);
      putOnClipboard(activity, "   ");
      button(root, "Clipboard").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals(before, currentTab(root));
      assertEquals(
          "The clipboard has no text", org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }
  }

  @Test
  public void untitledSharedYamlIsNamedAfterItsSniffedLanguage() {
    Intent incoming =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "name: ci\non:\n  push:\n    branches: [main]\njobs:\n  t:\n    runs-on: x\n");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, incoming).setup()) {
      shadowOf(Looper.getMainLooper()).idle();
      assertEquals(
          "Shared snippet.yaml", currentTab(controller.get().getWindow().getDecorView()));
    }
  }

  private void open(MainActivity activity, String name, String text) throws Exception {
    java.lang.reflect.Method add =
        MainActivity.class.getDeclaredMethod(
            "addDocument", String.class, String.class, String.class, String.class);
    add.setAccessible(true);
    add.invoke(activity, name, text, "UTF-8", "");
    shadowOf(Looper.getMainLooper()).idle();
  }

  @Test
  public void dataTabAdaptsToTheOpenFormat() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      open(activity, "ci.yml", "name: ci\non: push\n");
      assertEquals(View.VISIBLE, button(root, "YAML").getVisibility());
      assertNull("the tab is named for the format, not JSON", button(root, "JSON"));
      expandTab(root, "YAML");
      assertTrue(button(root, "View tree").isShown());
      assertFalse(button(root, "View JSON tree").isShown());
      assertFalse(button(root, "View diff").isShown());
      open(activity, "Cargo.toml", "[package]\nname = \"a\"\n");
      assertEquals(View.VISIBLE, button(root, "TOML").getVisibility());
      open(activity, "pom.xml", "<project><a/></project>");
      assertEquals(View.VISIBLE, button(root, "XML").getVisibility());
      open(activity, "app.properties", "a.b=1\n");
      assertEquals(View.VISIBLE, button(root, "Props").getVisibility());
      open(activity, "fix.patch", "--- a/x\n+++ b/x\n@@ -1 +1 @@\n-a\n+b\n");
      assertEquals(View.VISIBLE, button(root, "Diff").getVisibility());
      expandTab(root, "Diff");
      assertTrue(button(root, "View diff").isShown());
      assertFalse(button(root, "View tree").isShown());
      open(activity, "events.jsonl", "{\"a\":1}\n{\"a\":2}\n");
      assertEquals(View.VISIBLE, button(root, "JSON").getVisibility());
      expandTab(root, "JSON");
      assertTrue(button(root, "View JSON tree").isShown());
      open(activity, "notes.txt", "hello");
      assertEquals(View.GONE, button(root, "JSON").getVisibility());
    }
  }

  @Test
  public void castThisOpensTheSystemCastScreen() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      button(root, "Cast this").performClick();
      assertEquals(
          android.provider.Settings.ACTION_CAST_SETTINGS,
          shadowOf(activity).getNextStartedActivity().getAction());
    }
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  public void sharePdfAsksForPaperSizeAndRemembersIt() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      shadowOf(Looper.getMainLooper()).idle();
      button(root, "Share PDF").performClick();
      shadowOf(Looper.getMainLooper()).idle();
      android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();
      Spinner size = find(dialog.getWindow().getDecorView(), Spinner.class);
      assertEquals(8, size.getCount());
      assertEquals("A4 · 210 × 297 mm", size.getSelectedItem());
      size.setSelection(3);
      button(dialog.getWindow().getDecorView(), "Share PDF").performClick();
      assertEquals(
          "A5",
          activity.getSharedPreferences("native-settings", Context.MODE_PRIVATE).getString("pdfPage", ""));
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
      assertFalse(editor.isWordwrap());
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
    if (view instanceof ActionFlow || view.getClass().getSimpleName().equals("CommandGroup")) {
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
  public void ribbonScrollsHorizontallyAndTracksFingerMotionWithoutLayoutFeedback() {
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
      CollapsingRibbon body = find(root, CollapsingRibbon.class);
      float before = body.fraction();
      editor.dispatchEvent(
          new io.github.rosemoe.sora.event.ScrollEvent(
              editor, 0, 0, 0, 180, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG));
      assertEquals("Layout-driven scrolling must not move the ribbon", before, body.fraction(), 0f);
      long time = android.os.SystemClock.uptimeMillis();
      dragEditor(editor, time, 400, 160);
      assertNotNull(button(root, "Expand ribbon"));
      assertFalse(button(root, "Format").isShown());
      expandTab(root, "Code");
      assertTrue(button(root, "Format").isShown());
      dragEditor(editor, time + 500, 160, 400);
      assertNotNull(button(root, "Collapse ribbon"));
    }
  }

  private void dragEditor(CodeEditor editor, long time, float from, float to) {
    for (int i = 0; i < 3; i++) {
      MotionEvent event =
          MotionEvent.obtain(
              time,
              time + i * 100,
              i == 0
                  ? MotionEvent.ACTION_DOWN
                  : i == 1 ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_UP,
              160,
              i == 0 ? from : to,
              0);
      editor.dispatchTouchEvent(event);
      event.recycle();
    }
  }

  @Test
  public void overflowCommandsRemainReachableAndLogsLiveInSupport() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      root.measure(
          View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 320, 900);
      java.lang.reflect.Field field = MainActivity.class.getDeclaredField("ribbonPages");
      field.setAccessible(true);
      java.util.List<View> pages = (java.util.List<View>) field.get(activity);
      RibbonPage file = (RibbonPage) pages.get(0);
      assertTrue(file.getChildAt(0).getWidth() > file.getWidth());
      file.scrollTo(file.getChildAt(0).getWidth(), 0);
      assertTrue(file.getScrollX() > 0);
      assertFalse(button(root, "Log .gz").isShown());
      expandTab(root, "Support");
      assertTrue(button(root, "Log .gz").isShown());
      assertNotNull(button(pages.get(3), "Licenses"));
      assertNull(button(pages.get(2), "Service info"));
      AlertDialog before = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      expandTab(root, "Navigation");
      assertSame(before, org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
      for (String name : new String[] {"Symbols", "Definition", "References", "Info", "Table"})
        assertTrue(button(pages.get(5), name).isShown());
      assertNotNull(button(root, "Close file"));
      assertNull(button(pages.get(1), "Line"));
      assertNull(button(pages.get(5), "Line"));
      assertNotNull(button(pages.get(4), "Go to line"));
      assertNotNull(button(pages.get(3), "Settings"));
    }
  }

  @Test
  public void appIconOpensSettingsAndSymbolSearchJumpsToHighlightedLine() throws Exception {
    String source = "class Demo {\n  void alpha() {}\n  void beta() {}\n}\n";
    Intent intent =
        new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, source);
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      ViewGroup identity = root.findViewWithTag("identity");
      identity.getChildAt(0).performClick();
      AlertDialog settings = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      assertTrue(settings.isShowing());
      settings.dismiss();
      java.lang.reflect.Method show =
          MainActivity.class.getDeclaredMethod("showSymbols", java.util.List.class);
      show.setAccessible(true);
      show.invoke(
          activity,
          java.util.Arrays.asList(
              new SourceTools.Symbol("alpha", 1, 7), new SourceTools.Symbol("beta", 2, 7)));
      AlertDialog symbols = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      View dialogRoot = symbols.getWindow().getDecorView();
      EditText search = find(dialogRoot, EditText.class);
      search.setText("BETA");
      ListView results = find(dialogRoot, ListView.class);
      assertEquals(1, results.getAdapter().getCount());
      View result = results.getAdapter().getView(0, null, results);
      assertTrue(
          ((TextView) ((ViewGroup) result).getChildAt(2))
              .getText()
              .toString()
              .contains("void beta()"));
      results.performItemClick(result, 0, 0);
      LineSelectionEditor editor = find(root, LineSelectionEditor.class);
      assertEquals(2, editor.getCursor().getLeftLine());
      assertEquals(2, editor.navigationLine);
      assertEquals(source, editor.getText().toString());
      assertFalse(symbols.isShowing());
    }
  }

  @Test
  public void fitWidthPreservesWrapAndItsRibbonState() {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      CodeEditor editor = find(root, CodeEditor.class);
      root.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 480, 1000);
      expandTab(root, "View");
      Button wrap = button(root, "Wrap");
      assertTrue(wrap.isSelected());
      button(root, "Fit width").performClick();
      assertTrue(editor.isWordwrap());
      assertTrue(wrap.isSelected());
      wrap.performClick();
      assertFalse(editor.isWordwrap());
      assertFalse(wrap.isSelected());
      button(root, "Fit width").performClick();
      assertFalse(editor.isWordwrap());
      assertFalse(wrap.isSelected());
      activity.onConfigurationChanged(activity.getResources().getConfiguration());
      assertFalse(button(activity.getWindow().getDecorView(), "Wrap").isSelected());
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
  public void settingsUsesCategoryPagesAndRendersNatively() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      View root = controller.get().getWindow().getDecorView();
      button(root, "Settings").performClick();
      AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      View settings = dialog.getWindow().getDecorView();
      assertFalse(button(settings, "Share debug log (.gz)").isShown());
      button(settings, "About & diagnostics").performClick();
      assertTrue(button(settings, "Share debug log (.gz)").isShown());
      button(settings, "Appearance").performClick();
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
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  @Config(sdk = 28, qualifiers = "w480dp-h1000dp")
  public void searchRibbonHasNativeColorPopupAndLabeledClear() throws Exception {
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
      AlertDialog before = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
      button(root, "Search").performClick();
      assertNull(button(root, "Find"));
      assertSame(
          "Search stays inline",
          before,
          org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
      View controls = root.findViewWithTag("searchPanel");
      assertNotNull(controls);
      EditText query = find(controls, EditText.class);
      query.setText("alpha");
      awaitMatches(editor, 3);
      button(controls, "Next").performClick();
      assertTrue(editor.getCursor().isSelected());
      button(controls, "Match case").performClick();
      awaitMatches(editor, 1);
      assertTrue(button(root, "Search").isSelected());
      assertNull(button(controls, "Pink highlight"));
      java.lang.reflect.Field pages = MainActivity.class.getDeclaredField("ribbonPages");
      pages.setAccessible(true);
      assertNull(
          ((java.util.List<View>) pages.get(activity)).get(0).findViewWithTag("searchPanel"));
      LineSelectionEditor annotated = (LineSelectionEditor) editor;
      annotated.highlights.apply(0, 5, 0x707ad957);
      button(controls, "Search highlight color").performClick();
      java.lang.reflect.Field popupField = MainActivity.class.getDeclaredField("highlightPopup");
      popupField.setAccessible(true);
      PopupWindow popup = (PopupWindow) popupField.get(activity);
      assertTrue(popup.isShowing());
      assertNotNull(find(popup.getContentView(), SeekBar.class));
      assertSame(before, org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
      assertNull(button(popup.getContentView(), "Pink"));
      find(popup.getContentView(), EditText.class).setText("#FF66BB");
      button(popup.getContentView(), "Apply highlight").performClick();
      int color = 0x70ff66bb;
      assertEquals(
          color,
          editor
              .getColorScheme()
              .getColor(
                  io.github.rosemoe.sora.widget.schemes.EditorColorScheme.MATCHED_TEXT_BACKGROUND));
      assertEquals(0x707ad957, annotated.highlights.ranges().get(0).color);
      assertEquals(2, annotated.highlights.ranges().size());
      assertEquals(6, annotated.highlights.ranges().get(1).start);
      assertEquals(11, annotated.highlights.ranges().get(1).end);
      assertEquals(color, annotated.highlights.ranges().get(1).color);
      assertTrue(button(controls, "Clear search").getText().toString().contains("Clear search"));
      assertEquals(
          AppTheme.resolve("Carbon", false).surface,
          ((android.graphics.drawable.ColorDrawable) controls.getBackground()).getColor());
      root.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 480, 1000);
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
      android.graphics.Bitmap bitmap =
          android.graphics.Bitmap.createBitmap(480, 1000, android.graphics.Bitmap.Config.ARGB_8888);
      root.draw(new android.graphics.Canvas(bitmap));
      try (FileOutputStream out = new FileOutputStream("build/native-search.png")) {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
      }

      button(controls, "Regular expression").performClick();
      query.setText("ALPHA");
      awaitMatches(editor, 1);
      assertEquals(color, annotated.highlights.ranges().get(1).color);
      query.setText("[");
      shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));
      assertNotNull(query.getError());
      assertFalse(editor.getSearcher().hasQuery());
      button(controls, "Clear search").performClick();
      button(controls, "Next").performClick();
      assertFalse(editor.getSearcher().hasQuery());
      assertEquals("", query.getText().toString());
      assertFalse(editor.getCursor().isSelected());
      assertEquals(0x707ad957, annotated.highlights.ranges().get(0).color);
      assertEquals(2, annotated.highlights.ranges().size());
      assertEquals(6, annotated.highlights.ranges().get(1).start);
      assertEquals(11, annotated.highlights.ranges().get(1).end);
      assertEquals(color, annotated.highlights.ranges().get(1).color);
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
  @Config(sdk = 28, qualifiers = "w480dp-h1000dp")
  public void selectedTextColorPickerAppliesPersistentNativeHighlight() throws Exception {
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(
                Intent.EXTRA_TEXT,
                "class Highlight {\n    int first = 1;\n    int second = 2;\n}\n");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      LineSelectionEditor editor = find(root, LineSelectionEditor.class);
      java.lang.reflect.Field ready = MainActivity.class.getDeclaredField("syntaxReady");
      ready.setAccessible(true);
      for (int i = 0; i < 100; i++) {
        shadowOf(Looper.getMainLooper()).idle();
        if (ready.getBoolean(activity)) break;
        Thread.sleep(20);
      }
      root.measure(
          View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 480, 1000);
      String source = editor.getText().toString();
      editor.setSelectionRegion(1, 0, 2, 8);
      io.github.rosemoe.sora.widget.component.EditorTextActionWindow toolbar =
          editor.getComponent(io.github.rosemoe.sora.widget.component.EditorTextActionWindow.class);
      toolbar.displayWindow();
      button(toolbar.getView(), "Highlight color").performClick();
      java.lang.reflect.Field popupField = MainActivity.class.getDeclaredField("highlightPopup");
      popupField.setAccessible(true);
      PopupWindow picker = (PopupWindow) popupField.get(activity);
      assertTrue(picker.isShowing());
      assertNotNull(find(picker.getContentView(), SeekBar.class));
      assertNull(button(picker.getContentView(), "Pink"));
      find(picker.getContentView(), EditText.class).setText("#FF66BB");
      button(picker.getContentView(), "Apply highlight").performClick();
      assertEquals(1, editor.highlights.ranges().size());
      assertEquals(0x70ff66bb, editor.highlights.ranges().get(0).color);
      assertFalse(editor.getCursor().isSelected());
      assertEquals(source, editor.getText().toString());
      activity.onConfigurationChanged(activity.getResources().getConfiguration());
      assertEquals(1, editor.highlights.ranges().size());
      for (int i = 0; i < 30; i++) {
        Thread.sleep(20);
        shadowOf(Looper.getMainLooper()).idle();
      }
      android.graphics.Bitmap bitmap =
          android.graphics.Bitmap.createBitmap(480, 1000, android.graphics.Bitmap.Config.ARGB_8888);
      root.draw(new android.graphics.Canvas(bitmap));
      try (FileOutputStream out = new FileOutputStream("build/native-highlight.png")) {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
      }
      editor.setSelectionRegion(1, 0, 2, 8);
      assertTrue(button(toolbar.getView(), "Clear colors").isEnabled());
      button(toolbar.getView(), "Clear colors").performClick();
      assertTrue(editor.highlights.ranges().isEmpty());
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
            ViewGroup groups = (ViewGroup) ((ViewGroup) page).getChildAt(0);
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

  @Test
  public void sharedFileStartupSkipsWelcomeAndKeepsSelectionWhenSyntaxArrives() throws Exception {
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "test.java")
            .putExtra(Intent.EXTRA_TEXT, "class Test {\n  int value;\n}\n");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      MainActivity activity = controller.get();
      java.lang.reflect.Field docs = MainActivity.class.getDeclaredField("documents");
      docs.setAccessible(true);
      assertEquals(1, ((java.util.List<?>) docs.get(activity)).size());
      CodeEditor editor = find(activity.getWindow().getDecorView(), CodeEditor.class);
      editor.setSelection(1, 3);
      java.lang.reflect.Field ready = MainActivity.class.getDeclaredField("syntaxReady");
      ready.setAccessible(true);
      for (int i = 0; i < 200 && !ready.getBoolean(activity); i++) {
        shadowOf(Looper.getMainLooper()).idle();
        Thread.sleep(20);
      }
      assertTrue(ready.getBoolean(activity));
      assertEquals(1, editor.getCursor().getLeftLine());
      assertEquals(3, editor.getCursor().getLeftColumn());
      assertSame(
          FontCache.get(activity, "fonts/JetBrainsMono.ttf"),
          FontCache.get(activity, "fonts/JetBrainsMono.ttf"));
      assertNotNull(button(activity.getWindow().getDecorView(), "Privacy"));
    }
  }

  @Test
  public void formatRibbonsFollowTheCurrentFile() throws Exception {
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "README.md")
            .putExtra(Intent.EXTRA_TEXT, "# Hello");
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class, intent).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      assertEquals(View.VISIBLE, button(root, "MD").getVisibility());
      assertEquals(View.GONE, button(root, "JSON").getVisibility());
      expandTab(root, "MD");
      assertTrue(button(root, "Preview").isShown());
      java.lang.reflect.Method add =
          MainActivity.class.getDeclaredMethod(
              "addDocument", String.class, String.class, String.class, String.class);
      add.setAccessible(true);
      add.invoke(activity, "data.json", "{\"x\":1}", "UTF-8", "");
      assertEquals(View.GONE, button(root, "MD").getVisibility());
      assertEquals(View.VISIBLE, button(root, "JSON").getVisibility());
      expandTab(root, "JSON");
      assertTrue(button(root, "View JSON tree").isShown());
      add.invoke(activity, "plain.txt", "hello", "UTF-8", "");
      assertEquals(View.GONE, button(root, "MD").getVisibility());
      assertEquals(View.GONE, button(root, "JSON").getVisibility());
    }
  }

  @Test
  public void manualCollapseIgnoresScrollUntilExplicitlyExpanded() throws Exception {
    try (ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).setup()) {
      MainActivity activity = controller.get();
      View root = activity.getWindow().getDecorView();
      expandTab(root, "File");
      java.lang.reflect.Method scroll =
          MainActivity.class.getDeclaredMethod("ribbonScroll", int.class, int.class);
      scroll.setAccessible(true);
      for (String control : new String[] {"Collapse ribbon", "File"}) {
        button(root, control).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250));
        CollapsingRibbon ribbon = find(root, CollapsingRibbon.class);
        assertEquals(0f, ribbon.fraction(), .001f);
        scroll.invoke(activity, -120, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG);
        scroll.invoke(activity, -120, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_FLING);
        assertEquals(0f, ribbon.fraction(), .001f);
        button(root, "Expand ribbon").performClick();
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250));
        assertEquals(1f, ribbon.fraction(), .001f);
        scroll.invoke(activity, 30, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG);
        float partial = ribbon.fraction();
        scroll.invoke(activity, -10, io.github.rosemoe.sora.event.ScrollEvent.CAUSE_USER_DRAG);
        assertTrue(ribbon.fraction() > partial);
      }
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
      java.lang.reflect.Field ready = MainActivity.class.getDeclaredField("syntaxReady");
      ready.setAccessible(true);
      for (int i = 0; i < 100 && !ready.getBoolean(activity); i++) {
        shadowOf(Looper.getMainLooper()).idle();
        Thread.sleep(20);
      }
      assertTrue("Syntax startup completed before the gesture", ready.getBoolean(activity));
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
