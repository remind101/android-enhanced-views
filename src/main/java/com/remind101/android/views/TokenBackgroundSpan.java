package com.remind101.android.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;

import com.remind101.ui.listeners.OnSelectionChangeListener;

public class TokenBackgroundSpan<T> extends ReplacementSpan {
    public final T tokenValue;
    private final EnhancedTextView container;
    private final int strokeWidth = 2;

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
        borderPaint.setStrokeWidth(strokeWidth);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);

        setupCallbacksForContainer();
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + paddingHorizontal * 2 + strokeWidth * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        final int belowBaseline = (int) paint.getFontMetrics().bottom;
        final int width = getSize(paint, text, start, end, null);

        rect.set(x + strokeWidth,
                top - paddingVertical,
                x + width + strokeWidth,
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

    private void setupCallbacksForContainer() {
        container.setOnSelectionChangeListener(new OnSelectionChangeListener() {
            @Override
            public void onSelectionChanged(EnhancedTextView view, int selectionStart, int selectionEnd) {
                Spannable spannable = (Spannable) container.getText();

                Object[] allSpans = spannable.getSpans(0, container.length(), TokenBackgroundSpan.class);
                Object[] includedSpans = spannable.getSpans(selectionStart, selectionEnd, TokenBackgroundSpan.class);

                for (Object span : allSpans) {
                    ((TokenBackgroundSpan<T>) span).setSelected(false);
                    expandSetSelectionIfInSpan(selectionStart, selectionEnd, spannable, span);
                }

                for (Object span : includedSpans) {
                    if (spannable.getSpanStart(span) < selectionEnd) {
                        ((TokenBackgroundSpan<T>) span).setSelected(true);
                    }
                }
            }

            private void expandSetSelectionIfInSpan(int selectionStart, int selectionEnd, Spannable spannable, Object span) {
                int spanStart = spannable.getSpanStart(span);
                int spanEnd = spannable.getSpanEnd(span);

                boolean isPointSelection = selectionStart == selectionEnd;

                int newSelectionStart = selectionStart;
                int newSelectionEnd = selectionEnd;

                final boolean startIsInSpan = selectionStart > spanStart && selectionStart < spanEnd;
                if (startIsInSpan && isPointSelection) {
                    newSelectionStart = spanEnd;
                } else if (startIsInSpan && !isPointSelection) {
                    newSelectionStart = spanStart;
                }

                final boolean endIsInSpan = selectionEnd > spanStart && selectionEnd < spanEnd;
                if (endIsInSpan) {
                    newSelectionEnd = spanEnd;
                }

                if (newSelectionEnd != selectionEnd || newSelectionStart != selectionStart) {
                    Selection.setSelection(spannable, newSelectionStart, newSelectionEnd);
                }
            }
        });

        container.addTextChangedListener(new TextWatcher() {
            String originalTokenText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (originalTokenText != null || !(s instanceof Spannable)) return;

                final Spannable newSpannableText = (Spannable) s;
                final int tokenStart = newSpannableText.getSpanStart(TokenBackgroundSpan.this);
                final int tokenEnd = newSpannableText.getSpanEnd(TokenBackgroundSpan.this);

                if (tokenStart == -1 || tokenEnd == -1) {
                    return;
                }

                originalTokenText = s.subSequence(tokenStart, tokenEnd).toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int tokenStart = s.getSpanStart(TokenBackgroundSpan.this);
                int tokenEnd = s.getSpanEnd(TokenBackgroundSpan.this);
                if (originalTokenText != null) {
                    if (tokenStart >= 0 && tokenEnd >= 0) {
                        final String newTokenText = s.subSequence(tokenStart, tokenEnd).toString();
                        if (!newTokenText.equals(originalTokenText)) {
                            s.removeSpan(TokenBackgroundSpan.this);
                            removeMyself();
                        }
                    } else if (tokenStart == -1 || tokenEnd == -1) {
                        removeMyself();
                    }
                }
            }

            private void removeMyself() {
                final TextWatcher me = this;
                container.post(new Runnable() {
                    @Override
                    public void run() {
                        container.removeTextChangedListener(me);
                    }
                });
            }
        });
    }
}
