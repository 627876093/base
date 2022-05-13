package com.liglig.base.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.security.MessageDigest;

import jp.wasabeef.glide.transformations.BitmapTransformation;

/**
 * Glide圆角 填充为centerCrop
 */
public class GlideRoundTransform extends BitmapTransformation {

    //仅以下四条可进行 | 运算
    public static final int TOP_LEFT = 0b0001;
    public static final int TOP_RIGHT = 0b0010;
    public static final int BOTTOM_LEFT = 0b0100;
    public static final int BOTTOM_RIGHT = 0b1000;

    public static final int NONE = 0b0000;
    public static final int ALL = 0b1111;
    public static final int INVERSE_TOP_LEFT = ALL - TOP_LEFT;
    public static final int INVERSE_TOP_RIGHT = ALL - TOP_RIGHT;
    public static final int INVERSE_BOTTOM_LEFT = ALL - BOTTOM_LEFT;
    public static final int INVERSE_BOTTOM_RIGHT = ALL - BOTTOM_RIGHT;

    private int radius;
    private int corner;

    public GlideRoundTransform() {
        this(0, NONE);
    }

    public GlideRoundTransform(int radius) {
        this(radius, ALL);
    }

    public GlideRoundTransform(int radius, int corner) {
        this.radius = radius;
        this.corner = corner;
    }

    @Override
    protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
                               @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap source = TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight);
        if (source == null) {
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = pool.get(width, height, Bitmap.Config.ARGB_8888);
        if (result == null) {
            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        drawRoundRect(canvas, paint, width, height);
        return result;
    }

    private void drawRoundRect(Canvas canvas, Paint paint, int width, int height) {
        boolean TL, TR, BL, BR;
        int round = corner;
        if (round >= BOTTOM_RIGHT) {
            BR = true;
            round -= BOTTOM_RIGHT;
        } else {
            BR = false;
        }
        if (round >= BOTTOM_LEFT) {
            BL = true;
            round -= BOTTOM_LEFT;
        } else {
            BL = false;
        }
        if (round >= TOP_RIGHT) {
            TR = true;
            round -= TOP_RIGHT;
        } else {
            TR = false;
        }
        TL = round >= TOP_LEFT;

        canvas.drawRect(new RectF(0, radius, width, height - radius), paint);
        canvas.drawRect(new RectF(radius, 0, width - radius, height), paint);
        int diameter = 2 * radius;
        RectF rect = new RectF();
        rect.set(0, 0, diameter, diameter);
        if (TL) {
            canvas.drawArc(rect, 180, 90, true, paint);
        } else {
            canvas.drawRect(rect, paint);
        }
        rect.set(width - diameter, 0, width, diameter);
        if (TR) {
            canvas.drawArc(rect, 270, 90, true, paint);
        } else {
            canvas.drawRect(rect, paint);
        }
        rect.set(0, height - diameter, diameter, height);
        if (BL) {
            canvas.drawArc(rect, 90, 90, true, paint);
        } else {
            canvas.drawRect(rect, paint);
        }
        rect.set(width - diameter, height - diameter, width, height);
        if (BR) {
            canvas.drawArc(rect, 0, 90, true, paint);
        } else {
            canvas.drawRect(rect, paint);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GlideRoundTransform &&
                ((GlideRoundTransform) o).radius == radius &&
                ((GlideRoundTransform) o).corner == corner;
    }

    @Override
    public int hashCode() {
        return radius * 100 + corner * 10;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(("" + radius + corner).getBytes(CHARSET));
    }
}
