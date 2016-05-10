package com.bq.seekbarsample;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

import com.bq.markerseekbar.MarkerSeekBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MarkerSeekBar bar1 = (MarkerSeekBar) findViewById(R.id.bar1);
        assert bar1 != null;
        bar1.setProgressToTextTransformer(new MarkerSeekBar.ProgressToTextTransformer() {
            @SuppressLint("DefaultLocale")
            @Override
            public String toText(int progress) {
                return String.format(" ¯\\_(ツ)_/¯ %d ", progress);
            }

            @Override
            public String onMeasureLongestText(int seekBarMax) {
                return toText(seekBarMax);
            }
        });


        final MarkerSeekBar bar2 = (MarkerSeekBar) findViewById(R.id.bar2);
        assert bar2 != null;
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                ContextCompat.getColor(this, R.color.colorAccent),
                Color.WHITE - ContextCompat.getColor(this, R.color.colorAccent)
        );
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.setInterpolator(new LinearInterpolator());
        colorAnim.setDuration(1000);
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bar2.getMarkerView().setMarkerColor((int) animation.getAnimatedValue());
            }
        });
        colorAnim.start();

        final MarkerSeekBar bar3 = (MarkerSeekBar) findViewById(R.id.bar3);
        assert bar3 != null;
        bar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ValueAnimator marginAnim = ValueAnimator.ofInt(0, 100, 0);
                marginAnim.setInterpolator(new LinearInterpolator());
                marginAnim.setRepeatMode(ValueAnimator.REVERSE);
                marginAnim.setRepeatCount(1);
                marginAnim.setDuration(1000);
                marginAnim.setStartDelay(1000);
                marginAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int v = (int) animation.getAnimatedValue();
                        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bar3.getLayoutParams();
                        layoutParams.setMarginStart(v);
                        bar3.setLayoutParams(layoutParams);
                    }
                });
                marginAnim.start();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
}
