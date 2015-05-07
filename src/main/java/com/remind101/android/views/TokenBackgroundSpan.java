package com.remind101.android.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class TokenBackgroundSpan<T> extends ReplacementSpan {
    public final T tokenValue;
    private final EnhancedTextView container;

    private RectF rect = new RectF();
    private final Paint backgroundPaint;
    private final Paint borderPaint;
    private final float paddingHorizontal;
    private final float paddingVertical;
    private final float rounding;

    private boolean selected;

    public TokenBackgroundSpan(T tokenValue, EnhancedTextView container) {
        this.tokenValue = tokenValue;
        this.container = container;
        this.paddingHorizontal = container.getTokenHorizontalPadding();
        this.paddingVertical = container.getTokenVerticalPadding();
        this.rounding = container.getTokenBorderRadius();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(container.getTokenBackgroundColor());

        borderPaint = new Paint();
        borderPaint.setColor(container.getTokenBorderColor());
        borderPaint.setStrokeWidth(2);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + paddingHorizontal * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        final int belowBaseline = (int) paint.getFontMetrics().bottom;
        final int width = getSize(paint, text, start, end, null);

        rect.set(x,
                top - paddingVertical,
                x + width,
                y + belowBaseline + paddingVertical);

        canvas.drawRoundRect(rect, rounding, rounding, backgroundPaint);
        canvas.drawRoundRect(rect, rounding, rounding, borderPaint);

        if (selected && container.getTokenSelectedTextColor() != -1) {
            paint.setColor(container.getTokenSelectedTextColor());
        } else if (!selected && container.getTokenTextColor() != -1) {
            paint.setColor(container.getTokenTextColor());
        }

        canvas.drawText(text, start, end, x + paddingHorizontal, y, paint);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            backgroundPaint.setColor(container.getTokenSelectedBackgroundColor());
            borderPaint.setColor(container.getTokenSelectedBorderColor());
        } else {
            backgroundPaint.setColor(container.getTokenBackgroundColor());
            borderPaint.setColor(container.getTokenBorderColor());
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
