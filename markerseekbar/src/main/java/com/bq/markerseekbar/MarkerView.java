package com.bq.markerseekbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public final class MarkerView extends View {

    private static final float SQRT_2 = 1.4142135f;

    //Aux
    private final Matrix matrix = new Matrix();
    private final RectF rect = new RectF();

    //Drawing
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path markerPath = new Path();
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //Properties
    private float width, height;
    private float padding; //calculated
    private float shadowRadius = 5;
    private float rad;

    private int shadowColor = Color.GRAY;

    private Bitmap shadowBitmap;

    public MarkerView(Context context) {
        this(context, null);
    }

    public MarkerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(Color.WHITE);
    }

    private void buildShadowBitmap() {
        if (shadowBitmap != null) return;

        shadowBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        if (isInEditMode()) return;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(markerPaint.getColor());
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(shadowBitmap);
        Path shadowPath = new Path();

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        //Draw the shadow with the shape filled
        computeConvexPath(shadowPath, rad);
        paint.setShadowLayer(shadowRadius, 0, 0, shadowColor);
        canvas.drawPath(shadowPath, paint);
        paint.setShadowLayer(0, 0, 0, 0);

        //Remove the inside
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        computeConvexPath(shadowPath, rad);
        canvas.drawPath(shadowPath, paint);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        buildShadowBitmap();
        canvas.drawBitmap(shadowBitmap, 0, 0, shadowPaint);
        canvas.drawPath(markerPath, markerPaint);
    }

    private void computeConvexPath(Path path, float rad) {
        path.reset();
        matrix.reset();

        //The tear is a rounded square with 0 radius in the top left corner,
        rect.set(0, 0, 2 * rad, 2 * rad);
        float[] rads = new float[]{0, 0, rad, rad, rad, rad, rad, rad};
        path.addRoundRect(rect, rads, Path.Direction.CCW);

        //Align to bottom and center
        matrix.postRotate(45 + 180, 0, 0);
        matrix.postTranslate(width / 2, height - padding);
        path.transform(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        this.width = w;
        this.height = h;
        this.padding = shadowRadius;
        float halfSide = Math.min(width / 2, height / 2);
        //Subtract the distance from the enclosing square to the circle, after rotation
        //We have to make sure the shape fits
        rad = (0.5f * SQRT_2 * halfSide) - padding;

        computeConvexPath(markerPath, rad);
        rebuildShadowBitmap();
    }

    /**
     * Change the shadow color. The draw cache will be rebuilt,
     * use {@link #setShadowColorFilter(ColorFilter)} instead for better performance.
     */
    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
        rebuildShadowBitmap();
    }

    /** Update the marker shadow. The draw cache will be rebuilt. */
    public void setShadowRadius(float shadowRadius) {
        this.shadowRadius = shadowRadius;
        onSizeChanged((int) width, (int) height, 0, 0);
        requestLayout();
    }

    /** Set the marker background color. You can animate this property. */
    public void setMarkerColor(int color) {
        this.markerPaint.setColor(color);
        invalidate();
    }

    public int getMarkerColor() {
        return this.markerPaint.getColor();
    }

    /** Apply a color filter to the marker background. Has no effect on the shadow */
    public void setMarkerColorFilter(ColorFilter colorFilter) {
        this.markerPaint.setColorFilter(colorFilter);
        invalidate();
    }

    /** Apply a color filter to the cached shadow. Has no effect on the marker. */
    public void setShadowColorFilter(ColorFilter colorFilter) {
        this.shadowPaint.setColorFilter(colorFilter);
        invalidate();
    }

    /** The visual center of the tear */
    public int getCircleCenterY() {
        //The view is top aligned, so rad + padding gives the center
        //of the oval
        return (int) (height / 2 + padding);
    }

    public float getCircleRad() {
        return rad;
    }


    private void rebuildShadowBitmap() {
        if (shadowBitmap != null) {
            shadowBitmap.recycle();
        }
        shadowBitmap = null;
        invalidate();
    }
}
