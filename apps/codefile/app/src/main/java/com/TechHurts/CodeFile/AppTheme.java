package com.TechHurts.CodeFile;

import android.graphics.Color;

/** Shared native UI and syntax palette catalog. */
final class AppTheme {
  final String name, syntax;
  final boolean light;
  final int background, surface, text, muted, accent;

  private AppTheme(
      String name,
      String syntax,
      boolean light,
      String background,
      String surface,
      String text,
      String muted,
      String accent) {
    this.name = name;
    this.syntax = syntax;
    this.light = light;
    this.background = Color.parseColor(background);
    this.surface = Color.parseColor(surface);
    this.text = Color.parseColor(text);
    this.muted = Color.parseColor(muted);
    this.accent = Color.parseColor(accent);
  }

  static final AppTheme[] ALL = {
    new AppTheme(
        "Carbon", "theme_carbon", false, "#10141C", "#1C2330", "#E1E7EF", "#94A2B8", "#B4F272"),
    new AppTheme(
        "Midnight", "theme_midnight", false, "#0D1628", "#18243B", "#DFE9FA", "#91A6CA", "#7AB8FF"),
    new AppTheme(
        "Dracula", "theme_dracula", false, "#282A36", "#343746", "#F8F8F2", "#A2A7C2", "#BD93F9"),
    new AppTheme(
        "Forest", "theme_forest", false, "#111D19", "#1C3027", "#E0EEE4", "#91B19C", "#A6D98B"),
    new AppTheme(
        "Paper", "theme_paper", true, "#FAFAF7", "#E5E9DF", "#26322A", "#637163", "#3C6C25"),
    new AppTheme(
        "High contrast",
        "theme_contrast",
        false,
        "#000000",
        "#171717",
        "#FFFFFF",
        "#C4C4C4",
        "#FFFF00"),
    new AppTheme(
        "Nord dark",
        "theme_nord_dark",
        false,
        "#2E3440",
        "#3B4252",
        "#ECEFF4",
        "#A7B4C8",
        "#88C0D0"),
    new AppTheme(
        "Nord light",
        "theme_nord_light",
        true,
        "#ECEFF4",
        "#E5E9F0",
        "#2E3440",
        "#59677E",
        "#356D84"),
    new AppTheme(
        "Catppuccin Mocha",
        "theme_mocha",
        false,
        "#1E1E2E",
        "#313244",
        "#CDD6F4",
        "#A6ADC8",
        "#CBA6F7"),
    new AppTheme(
        "Catppuccin Latte",
        "theme_latte",
        true,
        "#EFF1F5",
        "#E6E9EF",
        "#4C4F69",
        "#6C6F85",
        "#8839EF"),
    new AppTheme(
        "Solarized dark",
        "theme_solarized_dark",
        false,
        "#002B36",
        "#073642",
        "#AFC2C4",
        "#8BA4AA",
        "#2AA198"),
    new AppTheme(
        "Solarized light",
        "theme_solarized_light",
        true,
        "#FDF6E3",
        "#EEE8D5",
        "#586E75",
        "#657B83",
        "#087E78"),
    new AppTheme(
        "Ros\u00e9 Pine",
        "theme_rose_pine",
        false,
        "#191724",
        "#26233A",
        "#E0DEF4",
        "#A19DB6",
        "#EBBCBA"),
    new AppTheme(
        "Ros\u00e9 Pine Dawn",
        "theme_rose_dawn",
        true,
        "#FAF4ED",
        "#F2E9E1",
        "#575279",
        "#797593",
        "#A34E69"),
  };

  static String[] names() {
    String[] names = new String[ALL.length + 1];
    for (int i = 0; i < 6; i++) names[i] = ALL[i].name;
    names[6] = "System";
    for (int i = 6; i < ALL.length; i++) names[i + 1] = ALL[i].name;
    return names;
  }

  static AppTheme resolve(String name, boolean systemLight) {
    if ("System".equals(name)) return ALL[systemLight ? 4 : 0];
    for (AppTheme theme : ALL) if (theme.name.equals(name)) return theme;
    return ALL[0];
  }
}
