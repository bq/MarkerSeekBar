package com.bq.markerseekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


/**
 * This view adds a visual marker indicator on top of the progress bar.
 * <p/>
 * The view is a subclass of {@link AppCompatSeekBar}, so you can just replace the references
 * in your xml to <code>com.bq.markerseekbar.MarkerSeekBar</code>
 * and everything should work as expected.
 * <p/>
 * You can show custom text using {@link ProgressToTextTransformer} and modify colors and other marker
 * properties in {@link MarkerView}.
 * <ul>
 * <p/>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_showMarkerOnTouch}. * Automatic toggle. Default true.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_smoothTracking}. * Animate popup position. Default false.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerTextAppearance}. * The style of the text inside the marker.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerTextColor}. * The color of the text inside the marker.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerColor}. * The marker background color. Default accent color</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerShadowRadius}. * The marker shadow radius. Use 0 to disable shadows. It affects marker size. Default 4dp.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerShadowColor}. * The marker shadow color. Default #331d1d1d.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerPopUpWindowSize}. * The popup size, its constant and measured to fit the longest possible text. Default 80dp.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerHorizontalOffset}. * Horizontal offset to align the marker tip and the progress thumb. Default empirical -8.5dp.</li>
 * <li>attr {@link com.bq.markerseekbar.R.styleable#MarkerSeekBar_markerVerticalOffset}. * Vertical offset to align the marker tip and the progress thumb. Default empirical -6dp.</li>
 * </ul>
 *
 * @see MarkerView
 */
public class MarkerSeekBar extends AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener {

    private static final int ANIMATION_SHOW_DURATION = 300;
    private static final int MARKER_TOGGLE_DELAY = 333;
    private static final int ANIMATION_HIDE_DURATION = 200;

    private static final Interpolator ANIMATION_SHOW_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator ANIMATION_HIDE_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator POP_UP_POSITION_INTERPOLATOR = new LinearInterpolator();
    //Can't use IntArrayEvaluator since it requires API 21
    private static final IntArrayEvaluatorCompat INT_ARRAY_EVALUATOR_COMPAT = new IntArrayEvaluatorCompat();

    private final int[] windowLocation = new int[2];
    private PopupWindow popupWindow;
    private final ViewGroup popUpRootView;
    private final MarkerView markerView;
    private final TextView markerTextView;

    private boolean showMarkerOnTouch;
    private boolean smoothTracking;

    private float markerAnimationFrame = 0;
    private int popupVerticalOffset;
    private int popupHorizontalOffset;
    private int popupWindowSize;

    private ValueAnimator popUpPositionAnimator;
    private int[] popUpHolderStartAux = new int[2];
    private int[] popUpHolderEndAux = new int[2];
    private int[][] popUpHolderStartAndEndAux = new int[2][1];

    private int popUpX = Integer.MIN_VALUE;
    private int popUpY = Integer.MIN_VALUE;

    private ProgressToTextTransformer progressToTextTransformer = new ProgressToTextTransformer.Default();

    public MarkerSeekBar(Context context) {
        this(context, null);
    }

    public MarkerSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnSeekBarChangeListener(null);

        //Build the marker view
        popUpRootView = new RelativeLayout(getContext());
        popUpRootView.setLayoutParams(new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        markerView = new MarkerView(getContext());
        RelativeLayout.LayoutParams markerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        markerView.setLayoutParams(markerParams);
        popUpRootView.addView(markerView);

        markerTextView = new TextView(getContext());
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        markerTextView.setLayoutParams(textParams);
        markerTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (markerTextView.getHeight() > 0) {
                    markerTextView.setTranslationY(markerView.getCircleCenterY() - markerTextView.getHeight() / 2);
                }
            }
        });
        popUpRootView.addView(markerTextView);

        //XML Parameters
        final float density = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MarkerSeekBar);

        showMarkerOnTouch = a.getBoolean(R.styleable.MarkerSeekBar_showMarkerOnTouch, true);
        smoothTracking = a.getBoolean(R.styleable.MarkerSeekBar_smoothTracking, false);

        popupWindowSize = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerPopUpWindowSize, (int) (80 * density));
        markerView.onSizeChanged(popupWindowSize, popupWindowSize, 0, 0);

        markerView.setMarkerColor(a.getColor(R.styleable.MarkerSeekBar_markerColor, getAccentColor()));
        markerView.setShadowRadius(a.getDimension(R.styleable.MarkerSeekBar_markerShadowRadius, 4 * density));
        markerView.setShadowColor(a.getColor(R.styleable.MarkerSeekBar_markerShadowColor, Color.parseColor("#331d1d1d")));

        markerTextView.setTextColor(a.getColor(R.styleable.MarkerSeekBar_markerTextColor, Color.WHITE));
        markerTextView.setTextAppearance(context,
                a.getResourceId(R.styleable.MarkerSeekBar_markerTextAppearance, R.style.Widget_MarkerSeekBar_TextAppearance));

        //In material SeekBar thumbs is off by 8.5 dp and looks like there
        //is no way to get the real center from the thumb drawable.
        popupHorizontalOffset = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerHorizontalOffset, (int) (-8.5 * density));
        popupVerticalOffset = a.getDimensionPixelSize(R.styleable.MarkerSeekBar_markerVerticalOffset, (int) (-6 * density));

        a.recycle();

        popupWindow = new PopupWindow(popUpRootView, popupWindowSize, popupWindowSize, false);
        popupWindow.setClippingEnabled(false); //Allow to draw outside screen
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        markerTextView.setText(progressToTextTransformer.toText(progress));
        updatePopupLayout();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (showMarkerOnTouch) {
            showMarker(true, MARKER_TOGGLE_DELAY);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (showMarkerOnTouch) {
            hideMarker(true, 0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) updatePopupLayout();
    }

    private void updatePopupLayout() {
        getLocationInWindow(windowLocation);

        int oldX = popUpX;
        int oldY = popUpY;

        int thumbCenterX = getThumb() == null ? 0 : getThumb().getBounds().centerX();
        int thumbHeight = getThumb() == null ? 0 : getThumb().getIntrinsicHeight();

        popUpX = windowLocation[0] + thumbCenterX + getPaddingLeft() + popupHorizontalOffset - popupWindowSize / 2;
        popUpY = windowLocation[1] + thumbHeight / 2 + popupVerticalOffset - popupWindowSize;

        final int dx = oldX - popUpX;
        final int dy = oldY - popUpY;
        final float distance = (float) Math.sqrt(dx * dx + dy * dy);
        final float proportion = distance / getWidth();

        if (!smoothTracking //No smooth tracking
                || proportion < 0.10 //Small movements snap
                || (oldX == popUpX && oldY == popUpY) //Same position, call update in case the size changed
                || oldX == Integer.MIN_VALUE //First time showing
                || oldY == Integer.MIN_VALUE) {
            popupWindow.update(popUpX, popUpY, popupWindowSize, popupWindowSize);
        } else {
            //Stop the current animation, if any
            if (popUpPositionAnimator != null) popUpPositionAnimator.cancel();

            popUpHolderStartAux[0] = oldX;
            popUpHolderStartAux[1] = oldY;

            popUpHolderEndAux[0] = popUpX;
            popUpHolderEndAux[1] = popUpY;

            popUpHolderStartAndEndAux[0] = popUpHolderStartAux;
            popUpHolderStartAndEndAux[1] = popUpHolderEndAux;

            popUpPositionAnimator = ValueAnimator.ofObject(INT_ARRAY_EVALUATOR_COMPAT, popUpHolderStartAndEndAux);
            popUpPositionAnimator.setInterpolator(POP_UP_POSITION_INTERPOLATOR);
            popUpPositionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int[] pos = (int[]) animation.getAnimatedValue();
                    popUpX = pos[0];
                    popUpY = pos[1];
                    popupWindow.update(pos[0], pos[1], popupWindowSize, popupWindowSize);
                }
            });
            popUpPositionAnimator.setDuration((long) (proportion * 333));
            popUpPositionAnimator.start();
        }
    }

    /**
     * The listener provided is wrapped in a {@link WrappedSeekBarListener},
     * this class requires the callbacks produced by the SeekBar.
     */
    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(new WrappedSeekBarListener(listener));
    }

    /**
     * Since marker size is fixed call this method to ensure the text fits.
     * You don't need to call this method, use {@link #setProgressToTextTransformer(ProgressToTextTransformer)} instead.
     */
    public void ensureMarkerSize(String text) {
        if (markerTextView == null) return;
        Paint p = markerTextView.getPaint();
        int textSize = (int) p.measureText(text);
        if (textSize > 2 * markerView.getCircleRad()) {
            float diff = textSize - 2 * markerView.getCircleRad();
            popupWindowSize += diff * Math.sqrt(2);
            updatePopupLayout();
        }
    }

    /**
     * Show the popup and the marker. Add small delay to avoid taps showing the marker.
     * This action cancels if {@link #hideMarker(boolean, int)} (boolean, int)} is called.
     */
    public void showMarker(boolean animated, int delay) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "markerAnimationFrame", markerAnimationFrame, 1);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_SHOW_INTERPOLATOR);
        anim.setDuration(animated ? ANIMATION_SHOW_DURATION : 0);
        anim.setStartDelay(delay);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                showPopUp();
            }
        });
        anim.start();
    }

    /**
     * Hide marker and dismiss the window.
     * This action cancels if {@link #showMarker(boolean, int)} is called.
     */
    public void hideMarker(boolean animated, int delay) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, "markerAnimationFrame", markerAnimationFrame, 0);
        anim.setAutoCancel(true);
        anim.setInterpolator(ANIMATION_HIDE_INTERPOLATOR);
        anim.setDuration(animated ? ANIMATION_HIDE_DURATION : 0);
        anim.setStartDelay(delay);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hidePopUp();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                hidePopUp();
            }
        });
        anim.start();
    }

    //#########################
    // Properties
    //#########################

    @Override
    public synchronized void setMax(final int max) {
        super.setMax(max);
        if (isInEditMode()) return;

        if (markerTextView == null) { //Called during SeekBar constructor
            post(new Runnable() {
                @Override
                public void run() {
                    ensureMarkerSize(progressToTextTransformer.onMeasureLongestText(max));
                }
            });
        } else {
            ensureMarkerSize(progressToTextTransformer.onMeasureLongestText(max));
        }
    }

    public void setPopupHorizontalOffset(int popupHorizontalOffset) {
        this.popupHorizontalOffset = popupHorizontalOffset;
        updatePopupLayout();
    }

    public void setPopupVerticalOffset(int popupVerticalOffset) {
        this.popupVerticalOffset = popupVerticalOffset;
        updatePopupLayout();
    }

    /** Manually control the animation show / hide time. */
    public void setMarkerAnimationFrame(@FloatRange(from = 0, to = 1) float frame) {
        this.markerAnimationFrame = frame;
        popUpRootView.setPivotX(popUpRootView.getWidth() / 2);
        popUpRootView.setPivotY(popUpRootView.getHeight());
        popUpRootView.setScaleX(frame);
        popUpRootView.setScaleY(frame);
        popUpRootView.invalidate();
    }

    public void setProgressToTextTransformer(@NonNull ProgressToTextTransformer progressToTextTransformer) {
        this.progressToTextTransformer = progressToTextTransformer;
    }

    public void setShowMarkerOnTouch(boolean showMarkerOnTouch) {
        this.showMarkerOnTouch = showMarkerOnTouch;
    }

    public float getMarkerAnimationFrame() {
        return markerAnimationFrame;
    }

    public PopupWindow getPopupWindow() {
        return popupWindow;
    }

    public ViewGroup getPopUpRootView() {
        return popUpRootView;
    }

    public TextView getMarkerTextView() {
        return markerTextView;
    }

    public MarkerView getMarkerView() {
        return markerView;
    }

    //#########################
    // Utility
    //#########################

    private void showPopUp() {
        //No gravity and no anchor, we will place it manually so animations on the seekbar
        //work properly on layout changes
        popupWindow.showAtLocation(MarkerSeekBar.this, Gravity.NO_GRAVITY, 0, 0);
        updatePopupLayout();
    }

    private void hidePopUp() {
        popUpX = popUpY = Integer.MIN_VALUE;
        popupWindow.dismiss();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        popupWindow.dismiss();
    }

    private int getAccentColor() {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = getContext().getResources().getIdentifier("colorAccent", "attr", getContext().getPackageName());
        }
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public interface ProgressToTextTransformer {
        String toText(int progress);

        String onMeasureLongestText(int seekBarMax);

        final class Default implements ProgressToTextTransformer {

            @Override
            public String toText(int progress) {
                return String.valueOf(progress);
            }

            @Override
            public String onMeasureLongestText(int seekBarMax) {
                //All 0's
                return String.valueOf(seekBarMax).replaceAll("\\d", "0");
            }
        }
    }

    private class WrappedSeekBarListener implements OnSeekBarChangeListener {

        private final OnSeekBarChangeListener wrappedListener;

        public WrappedSeekBarListener(OnSeekBarChangeListener wrappedListener) {
            this.wrappedListener = wrappedListener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            MarkerSeekBar.this.onProgressChanged(seekBar, progress, fromUser);
            if (wrappedListener != null)
                wrappedListener.onProgressChanged(seekBar, progress, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            MarkerSeekBar.this.onStartTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStartTrackingTouch(seekBar);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MarkerSeekBar.this.onStopTrackingTouch(seekBar);
            if (wrappedListener != null)
                wrappedListener.onStopTrackingTouch(seekBar);
        }
    }

    private static class IntArrayEvaluatorCompat implements TypeEvaluator<int[]> {

        private int[] array;

        @Override
        public int[] evaluate(float fraction, int[] startValue, int[] endValue) {
            if (array == null) {
                array = new int[startValue.length];
            }
            for (int i = 0; i < array.length; i++) {
                int start = startValue[i];
                int end = endValue[i];
                array[i] = (int) (start + (fraction * (end - start)));
            }
            return array;
        }
    }
}
