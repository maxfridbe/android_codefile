package com.TechHurts.CodeFile;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Only temporary exported copies, never arbitrary app files. */
public class ShareProvider extends ContentProvider {
  public boolean onCreate() {
    return true;
  }

  private File resolve(Uri uri) throws FileNotFoundException {
    try {
      File root = new File(getContext().getCacheDir(), "shares").getCanonicalFile();
      File file = new File(root, uri.getLastPathSegment()).getCanonicalFile();
      if (!root.equals(file.getParentFile()) || !file.isFile()) throw new FileNotFoundException();
      return file;
    } catch (Exception e) {
      throw new FileNotFoundException("Missing shared copy");
    }
  }

  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
    return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
  }

  public String getType(Uri uri) {
    return uri.getLastPathSegment() != null && uri.getLastPathSegment().endsWith(".gz")
        ? "application/gzip"
        : "text/plain";
  }

  public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
    String[] cols =
        projection == null
            ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
            : projection;
    MatrixCursor cursor = new MatrixCursor(cols);
    try {
      File f = resolve(uri);
      Object[] row = new Object[cols.length];
      for (int i = 0; i < cols.length; i++) {
        if (OpenableColumns.DISPLAY_NAME.equals(cols[i]))
          row[i] = f.getName().replaceFirst("^[0-9a-f-]{36}_", "");
        if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
      }
      cursor.addRow(row);
    } catch (FileNotFoundException ignored) {
    }
    return cursor;
  }

  public Uri insert(Uri uri, ContentValues values) {
    throw new UnsupportedOperationException();
  }

  public int update(Uri uri, ContentValues v, String s, String[] a) {
    throw new UnsupportedOperationException();
  }

  public int delete(Uri uri, String s, String[] a) {
    throw new UnsupportedOperationException();
  }
}
