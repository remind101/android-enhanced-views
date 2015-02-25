package com.remind101.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
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
    private float radius;
    private Paint[] activePaint;
    private Paint inactivePaint;
    private int gravity;

    public CircleIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CircleIndicatorView, 0, 0);
        try {
            pageCount = a.getInteger(R.styleable.CircleIndicatorView_circleCount, 1);
            radius = a.getDimension(R.styleable.CircleIndicatorView_radius, 0);
            int inactiveColor = a.getColor(R.styleable.CircleIndicatorView_inactiveColor, 0);
            inactivePaint = new Paint();
            inactivePaint.setColor(inactiveColor);
            inactivePaint.setStyle(Paint.Style.FILL);
            int activeColorRes = a.getResourceId(R.styleable.CircleIndicatorView_activeColor, 0);
            int[] colorsArray = a.getResources().getIntArray(activeColorRes);
            activePaint = new Paint[colorsArray.length];
            for (int i = 0; i < colorsArray.length; i++) {
                int color = colorsArray[i];
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                activePaint[i] = paint;
            }
            gravity = a.getInt(R.styleable.CircleIndicatorView_gravity, Gravity.NO_GRAVITY);
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
            return specSize;
        } else {
            if (radius <= 0) {
                result = getPaddingLeft() + getPaddingRight() + ((pageCount - 1) * inActiveDrawableWidth) + activeDrawableWidth + ((pageCount - 1) * drawablePadding);
            } else {
                result = (int) (getPaddingLeft() + getPaddingRight() + (2 * radius * pageCount) + drawablePadding * (pageCount - 1));
            }
            if (specMode == MeasureSpec.AT_MOST) {
                return Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        } else {
            if (radius <= 0) {
                result = getPaddingTop() + getPaddingBottom() + Math.max(inActiveDrawableHeight, activeDrawableHeight);
            } else {
                result = (int) (getPaddingTop() + getPaddingBottom() + 2 * radius);
            }
            if (specMode == MeasureSpec.AT_MOST) {
                return Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int i;
        int left, top, right, bottom;
        int paddingTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();

        switch (gravity) {
            case Gravity.TOP:
                paddingTop = 0;
                break;
            case Gravity.CENTER:
                paddingTop = (int) (getHeight() / 2 - (radius <= 0 ?
                        Math.max(activeDrawableHeight, inActiveDrawableHeight) / 2 :
                        radius));
                paddingLeft = (int) (getWidth() / 2 - (radius <= 0 ?
                        ((pageCount - 1) * (inActiveDrawableWidth + drawablePadding) + activeDrawableWidth) / 2 :
                        (2 * radius * pageCount + (pageCount - 1) * drawablePadding) / 2));
                break;
            case Gravity.BOTTOM:
                paddingTop = (int) (getHeight() - (radius <= 0 ? Math.max(activeDrawableHeight, inActiveDrawableHeight) : 2 * radius));
                break;
        }

        for (i = 0; i < pageCount; i++) {
            if (i == currentActive) {
                if (radius <= 0) {
                    left = paddingLeft + (i * activeDrawableWidth) + (i * drawablePadding);
                    top = paddingTop;
                    right = left + activeDrawableWidth;
                    bottom = activeDrawableHeight + top;
                    activeDrawable.setBounds(left, top, right, bottom);
                    activeDrawable.draw(canvas);
                } else {
                    float cx = paddingLeft + radius + (2 * radius * i) + (drawablePadding * i);
                    float cy = paddingTop + radius;
                    canvas.drawCircle(cx, cy, radius, activePaint[i - activePaint.length * (i / activePaint.length)]);
                }
            } else {
                if (radius <= 0) {
                    left = paddingLeft + (i * inActiveDrawableWidth) + (i * drawablePadding);
                    top = paddingTop;
                    right = left + inActiveDrawableWidth;
                    bottom = inActiveDrawableHeight + top;
                    inactiveDrawable.setBounds(left, top, right, bottom);
                    inactiveDrawable.draw(canvas);
                } else {
                    float cx = paddingLeft + radius + (2 * radius * i) + (drawablePadding * i);
                    float cy = paddingTop + radius;
                    canvas.drawCircle(cx, cy, radius, inactivePaint);
                }
            }
        }
    }
}