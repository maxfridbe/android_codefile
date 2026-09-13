package com.TechHurts.CodeFile;

import android.content.Context;
import android.graphics.Typeface;
import java.util.concurrent.ConcurrentHashMap;

/** Font assets are process resources; reuse them across themes and window rebuilds. */
final class FontCache {
  private static final ConcurrentHashMap<String, Typeface> FONTS = new ConcurrentHashMap<>();

  static Typeface get(Context context, String path) {
    return FONTS.computeIfAbsent(path, key -> Typeface.createFromAsset(context.getAssets(), key));
  }
}
