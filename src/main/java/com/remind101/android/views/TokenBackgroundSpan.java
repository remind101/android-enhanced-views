package com.remind101.android.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;

import com.remind101.ui.listeners.OnSelectionChangeListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    private Paint.FontMetrics fontMetricsReference;

    private boolean selected;
    private TextDisplayTransformation transformation;

    // minor optimization to avoid constantly allocating the same strings during onDraw
    private CharSequence lastText;
    private SpannableStringBuilder lastTransform;
    private int lastStart;
    private int lastEnd;

    private char[] bufferNew = new char[1024];
    private char[] bufferOld = new char[1024];

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

        fontMetricsReference = null;

        setupCallbacksForContainer();
    }

    private CharSequence getCachedDisplayText(CharSequence text, int start, int end) {
        if (start != lastStart || end != lastEnd || lastText != null && lastText.length() != text.length()) {
            // don't do any work if we can't cache hit
            return null;
        }

        if (text instanceof SpannableStringBuilder && lastText instanceof SpannableStringBuilder) {
            // this is silly but the only way to compare w/o malloc, thanks poor SSB implementation
            SpannableStringBuilder ssbText = (SpannableStringBuilder) text;
            SpannableStringBuilder ssbLast = (SpannableStringBuilder) lastText;

            if (ssbText.getSpanStart(this) != ssbLast.getSpanStart(this) ||
                    ssbText.getSpanEnd(this) != ssbLast.getSpanEnd(this)) {
                // if we've moved we cant match (even if the characters were updated to be identical)
                return null;
            }

            if (text.length() > bufferNew.length) {
                bufferNew = new char[(int)(text.length() * 1.1)];
            }
            if (lastText != null && lastText.length() > bufferOld.length) {
                bufferOld = new char[(int)(lastText.length() * 1.1)];
            }

            ssbText.getChars(0, ssbText.length(), bufferNew, 0);
            ssbLast.getChars(0, ssbLast.length(), bufferOld, 0);

            if (Arrays.equals(bufferNew, bufferOld)) {
                return lastTransform;
            }
        }
        return null;
    }

    private void setCachedDisplayText(CharSequence text, int start, int end, SpannableStringBuilder ssb) {
        lastText = text;
        lastTransform = ssb;
        lastStart = start;
        lastEnd = end;
    }

    private CharSequence getDisplayText(CharSequence text, int start, int end) {
        CharSequence cachedValue = getCachedDisplayText(text, start, end);
        if (cachedValue != null) {
            return cachedValue;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        if (transformation != null) {
            CharSequence transformed = transformation.transform(text.subSequence(start, end));
            ssb.replace(start, end, transformed);
        }
        setCachedDisplayText(text, start, end, ssb);
        return ssb;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        CharSequence displayText = getDisplayText(text, start, end);
        int newEnd = end + (displayText.length() - text.length());
        return Math.round(paint.measureText(displayText, start, newEnd) + paddingHorizontal * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        if (fontMetricsReference == null) {
            fontMetricsReference = paint.getFontMetrics();
        } else {
            paint.getFontMetrics(fontMetricsReference);
        }
        final int belowBaseline = (int) fontMetricsReference.bottom;
        final int width = getSize(paint, text, start, end, null);

        rect.set(x + strokeWidth,
                top - paddingVertical,
                x + width - strokeWidth,
                y + belowBaseline + paddingVertical);

        canvas.drawRoundRect(rect, rounding, rounding, backgroundPaint);
        canvas.drawRoundRect(rect, rounding, rounding, borderPaint);

        if (selected && container.getTokenSelectedTextColor() != -1) {
            paint.setColor(container.getTokenSelectedTextColor());
        } else if (!selected && container.getTokenTextColor() != -1) {
            paint.setColor(container.getTokenTextColor());
        }

        CharSequence displayText = getDisplayText(text, start, end);
        int newEnd = end + (displayText.length() - text.length());
        canvas.drawText(displayText, start, newEnd, x + paddingHorizontal, y, paint);
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


    public void setTransformation(TextDisplayTransformation transformation) {
        this.transformation = transformation;
        setCachedDisplayText(null, 0, 0, null);
    }

    public TextDisplayTransformation getTransformation() {
        return this.transformation;
    }

    public interface TextDisplayTransformation {
        CharSequence transform(CharSequence text);
    }

    public static final TextDisplayTransformation COMMA_TRANSFORM = new TextDisplayTransformation() {
        @Override
        public CharSequence transform(CharSequence text) {
            return text + ",";
        }
    };

    public static final TextDisplayTransformation PERIOD_TRANSFORM = new TextDisplayTransformation() {
        @Override
        public CharSequence transform(CharSequence text) {
            return text + ".";
        }
    };

    public static final TextDisplayTransformation NO_TRANSFORM = null;

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
                            if (originalTokenText.length() > 0 &&
                                    newTokenText.length() > 1 &&
                                    originalTokenText.startsWith(newTokenText)) {
                                s.replace(tokenStart, tokenEnd, "");
                            }
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
