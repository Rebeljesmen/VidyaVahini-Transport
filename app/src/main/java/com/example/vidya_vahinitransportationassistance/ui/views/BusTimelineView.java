package com.example.vidya_vahinitransportationassistance.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.vidya_vahinitransportationassistance.R;

public class BusTimelineView extends View {
    private Paint linePaint;
    private Paint circlePaint;
    private float animatedProgress = 0.25f; // Initial progress
    private int stopCount = 5;
    private Drawable busIcon;
    private int iconSize;

    public BusTimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(8f);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);

        busIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_bus_logo);
        iconSize = (int) (32 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float centerY = getHeight() / 2f;
        float padding = 40f;
        float effectiveWidth = width - (padding * 2);

        // 1. Draw connecting lines
        for (int i = 0; i < stopCount - 1; i++) {
            float startX = padding + (i / (float) (stopCount - 1)) * effectiveWidth;
            float endX = padding + ((i + 1) / (float) (stopCount - 1)) * effectiveWidth;

            // Line is green if both stops are completed or current
            if (startX <= padding + (animatedProgress * effectiveWidth)) {
                linePaint.setColor(Color.parseColor("#2E7D32")); // Success Green
            } else {
                linePaint.setColor(Color.LTGRAY);
            }
            canvas.drawLine(startX, centerY, endX, centerY, linePaint);
        }

        // 2. Draw stop circles
        for (int i = 0; i < stopCount; i++) {
            float x = padding + (i / (float) (stopCount - 1)) * effectiveWidth;
            float progressThreshold = (float) i / (stopCount - 1);

            if (animatedProgress >= progressThreshold) {
                // Completed stop
                circlePaint.setColor(Color.parseColor("#2E7D32"));
            } else if (Math.abs(animatedProgress - progressThreshold) < 0.01f) {
                // Current stop
                circlePaint.setColor(Color.parseColor("#1976D2")); // Blue
            } else {
                // Upcoming stop
                circlePaint.setColor(Color.LTGRAY);
            }
            canvas.drawCircle(x, centerY, 15f, circlePaint);
        }

        // 3. Draw animated bus icon
        if (busIcon != null) {
            float busX = padding + (animatedProgress * effectiveWidth);
            int left = (int) (busX - iconSize / 2);
            int top = (int) (centerY - iconSize - 10); // Position above the line
            busIcon.setBounds(left, top, left + iconSize, top + iconSize);
            busIcon.setTint(Color.parseColor("#1976D2"));
            busIcon.draw(canvas);
        }
    }

    /**
     * Smoothly animates the bus to a new progress position.
     * @param targetProgress Value between 0.0 and 1.0
     */
    public void animateToProgress(float targetProgress) {
        ValueAnimator animator = ValueAnimator.ofFloat(animatedProgress, targetProgress);
        animator.setDuration(1500); // 1.5 seconds animation
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setProgress(float progress) {
        this.animatedProgress = progress;
        invalidate();
    }
}