package org.local.karbalascreen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bundled, attributed city photographs. Never downloads or decodes on a clock tick. */
final class PhotoStore {
    static final class Photo {
        final String file, caption, source, credit, period, license, changes;
        Photo(String file, String caption, String source, String credit, String period, String license, String changes) {
            this.file = file; this.caption = caption; this.source = source; this.credit = credit; this.period = period; this.license = license; this.changes = changes;
        }
    }
    static List<Photo> load(Context context) {
        List<Photo> photos = new ArrayList<>();
        try (InputStream input = context.getAssets().open("wallpapers.json")) {
            JSONArray array = new JSONObject(StreamText.read(input)).getJSONArray("photos");
            for (int i = 0; i < array.length(); i++) {
                JSONObject value = array.getJSONObject(i);
                String file = value.optString("file"), caption = value.optString("caption"), source = value.optString("source"), credit = value.optString("credit");
                if (!file.matches("wallpapers/[A-Za-z0-9_-]+\\.(?:jpg|jpeg|png)") || !source.startsWith("https://") || caption.trim().isEmpty() || credit.trim().isEmpty()) continue;
                photos.add(new Photo(file, caption, source, credit, value.optString("period", "night"), value.optString("license_url"), value.optString("changes")));
            }
        } catch (Exception ignored) { /* Missing bundled images keep the solid dark background. */ }
        return photos;
    }
    static Bitmap decode(Context context, Photo photo) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        try (InputStream input = context.getAssets().open(photo.file)) { BitmapFactory.decodeStream(input, null, options); }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null;
        int sample = 1;
        while (Math.ceil(options.outWidth / (double) sample) > 1280 || Math.ceil(options.outHeight / (double) sample) > 720) sample *= 2;
        if (Thread.currentThread().isInterrupted()) return null;
        options.inJustDecodeBounds = false; options.inSampleSize = sample; options.inPreferredConfig = Bitmap.Config.RGB_565; options.inDither = true;
        Bitmap bitmap;
        try (InputStream input = context.getAssets().open(photo.file)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
        if (bitmap != null && bitmap.getAllocationByteCount() > 2 * 1024 * 1024) { bitmap.recycle(); return null; }
        if (Thread.currentThread().isInterrupted()) { if (bitmap != null) bitmap.recycle(); return null; }
        return bitmap;
    }
}
