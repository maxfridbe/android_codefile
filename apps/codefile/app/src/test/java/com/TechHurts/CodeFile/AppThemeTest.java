package com.TechHurts.CodeFile;

import static org.junit.Assert.*;

import android.graphics.Color;
import io.github.rosemoe.sora.widget.CodeEditor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AppThemeTest {
  @Test
  public void everyPaletteLoadsNativeSyntaxAndHasReadableText() throws Exception {
    NativeSyntax syntax = new NativeSyntax();
    syntax.load(RuntimeEnvironment.getApplication());
    assertEquals("Only the startup theme is loaded eagerly", 1, syntax.loadedThemeCount());
    CodeEditor editor = new CodeEditor(RuntimeEnvironment.getApplication());
    int lights = 0, darks = 0;
    try {
      for (AppTheme theme : AppTheme.ALL) {
        syntax.apply(editor, "java", theme.syntax);
        assertTrue(
            theme.name,
            androidx.core.graphics.ColorUtils.calculateContrast(theme.text, theme.background)
                >= 4.5);
        assertTrue(
            theme.name,
            androidx.core.graphics.ColorUtils.calculateContrast(theme.accent, theme.background)
                >= 3);
        if (theme.light) lights++;
        else darks++;
      }
      assertTrue(lights >= 5);
      assertTrue(darks >= 9);
      assertEquals(Color.parseColor("#FAFAF7"), AppTheme.resolve("System", true).background);
      assertEquals(AppTheme.ALL[0], AppTheme.resolve("System", false));
    } finally {
      editor.release();
    }
  }
}
