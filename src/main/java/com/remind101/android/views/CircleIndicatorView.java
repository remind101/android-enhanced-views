package com.remind101.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.remind101.android.enhancedviews.R;

public class CircleIndicatorView extends View {
    private int pageCount;
    private int currentActive = 0;

    private Drawable activeDrawable;
    private int activeDrawableWidth = 0;
    private int activeDrawableHeight = 0;

    private Drawable inactiveDrawable;
    private int inActiveDrawableWidth = 0;
    private int inActiveDrawableHeight = 0;

    private int drawablePadding;

    public CircleIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    private void init(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CircleIndicatorView, 0, 0);
        try {
            pageCount = a.getInteger(R.styleable.CircleIndicatorView_circleCount, 1);
            activeDrawable = a.getDrawable(R.styleable.CircleIndicatorView_activeDrawable);
            if (activeDrawable != null) {
                activeDrawableWidth = activeDrawable.getIntrinsicWidth();
                activeDrawableHeight = activeDrawable.getIntrinsicHeight();
            }
            inactiveDrawable = a.getDrawable(R.styleable.CircleIndicatorView_inActiveDrawable);
            if (inactiveDrawable != null) {
                inActiveDrawableWidth = inactiveDrawable.getIntrinsicWidth();
                inActiveDrawableHeight = inactiveDrawable.getIntrinsicHeight();
            }
            drawablePadding = a.getDimensionPixelSize(R.styleable.CircleIndicatorView_drawablePadding, 10);
        } finally {
            a.recycle();
        }
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
        invalidate();
        requestLayout();
    }

    public int getCurrentActive() {
        return currentActive;
    }

    public void setCurrentActive(int currentActive) {
        this.currentActive = currentActive;
        invalidate();
    }

    public Drawable getInactiveDrawable() {
        return inactiveDrawable;
    }

    public void setInactiveDrawable(Drawable inactiveDrawable) {
        this.inactiveDrawable = inactiveDrawable;
        invalidate();
        requestLayout();

    }

    public Drawable getActiveDrawable() {
        return activeDrawable;
    }

    public void setActiveDrawable(Drawable activeDrawable) {
        this.activeDrawable = activeDrawable;
    }

    /**
     * This calculates minimum view estate needed for placing it's content (dots with numbers) when size is not set exactly in xml
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = getPaddingLeft() + getPaddingRight() + ((pageCount - 1) * inActiveDrawableWidth) + activeDrawableWidth + ((pageCount - 1) * drawablePadding);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = getPaddingTop() + getPaddingBottom() + Math.max(inActiveDrawableHeight, activeDrawableHeight);
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int i;
        int left, top, right, bottom;

        for (i = 0; i < pageCount; i++) {
            if (i == currentActive) {
                left = getPaddingLeft() + (i * activeDrawableWidth) + (i * drawablePadding);
                top = getPaddingTop();
                right = left + activeDrawableWidth;
                bottom = activeDrawableHeight + getPaddingBottom();
                activeDrawable.setBounds(left, top, right, bottom);
                activeDrawable.draw(canvas);
            } else {
                left = getPaddingLeft() + (i * inActiveDrawableWidth) + (i * drawablePadding);
                top = getPaddingTop();
                right = left + inActiveDrawableWidth;
                bottom = inActiveDrawableHeight + getPaddingBottom();
                inactiveDrawable.setBounds(left, top, right, bottom);
                inactiveDrawable.draw(canvas);
            }
        }
    }
}