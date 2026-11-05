package org.local.karbalascreen;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Static center-cropped photo with one dark overlay and one retained bitmap. */
final class PhotoBackground extends Drawable {
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    private Bitmap bitmap;
    private boolean enabled = true;
    void setPhoto(Bitmap bitmap) { this.bitmap = bitmap; invalidateSelf(); }
    void setEnabled(boolean enabled) { this.enabled = enabled; invalidateSelf(); }
    @Override public void draw(Canvas canvas) {
        canvas.drawColor(Color.rgb(8, 15, 18));
        if (!enabled || bitmap == null || bitmap.isRecycled()) return;
        Rect bounds = getBounds(); if (bounds.width() <= 0 || bounds.height() <= 0) return;
        float scale = Math.max(bounds.width() / (float) bitmap.getWidth(), bounds.height() / (float) bitmap.getHeight());
        int width = Math.min(bitmap.getWidth(), Math.round(bounds.width() / scale));
        int height = Math.min(bitmap.getHeight(), Math.round(bounds.height() / scale));
        int left = (bitmap.getWidth() - width) / 2, top = (bitmap.getHeight() - height) / 2;
        source.set(left, top, left + width, top + height);
        canvas.drawBitmap(bitmap, source, bounds, paint);
        canvas.drawColor(Color.argb(185, 4, 10, 12));
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
