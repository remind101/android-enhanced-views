package com.remind101.android.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Join;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.widget.TextView;
import com.remind101.android.enhancedviews.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.WeakHashMap;

public class EnhancedTextView extends TextView {

    boolean isDrawableSticky;
    private ArrayList<Shadow> outerShadows;
    private ArrayList<Shadow> innerShadows;
    private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;
    private Canvas tempCanvas;
    private Bitmap tempBitmap;
    private Drawable foregroundDrawable;
    private float strokeWidth;
    private Integer strokeColor;
    private Join strokeJoin;
    private float strokeMiter;
    private int[] lockedCompoundPadding;
    private boolean frozen = false;
    private boolean decresingLineSpace;
    private Drawable[] originalDrawables;
    private Paint paint;


    public EnhancedTextView(Context context) {
        this(context, null);
    }

    public EnhancedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.EnhancedTextViewStyle);
    }

    public EnhancedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void init(AttributeSet attrs, int defStyle) {
        outerShadows = new ArrayList<Shadow>();
        innerShadows = new ArrayList<Shadow>();
        if (canvasStore == null) {
            canvasStore = new WeakHashMap<String, Pair<Canvas, Bitmap>>();
        }

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EnhancedTextView, defStyle, android.R.style.Widget_TextView);
            String typefaceName = a.getString(R.styleable.EnhancedTextView_typeface);
            if (typefaceName != null && !typefaceName.equals("") && !isInEditMode()) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                this.setTypeface(tf);
            }

            if (a.hasValue(R.styleable.EnhancedTextView_drawableSticky)) {
                isDrawableSticky = a.getBoolean(R.styleable.EnhancedTextView_drawableSticky, false);
                originalDrawables = this.getCompoundDrawables();
                paint = new Paint();
                paint.setTextSize(this.getTextSize());
                paint.setTypeface(this.getTypeface());
                ColorDrawable bd0 = new ColorDrawable(Color.TRANSPARENT);
                if (originalDrawables[0] != null) {
                    bd0.setBounds(0, 0, originalDrawables[0].getIntrinsicWidth(), originalDrawables[0].getIntrinsicHeight());
                }
                ColorDrawable bd2 = new ColorDrawable(Color.TRANSPARENT);
                if (originalDrawables[2] != null) {
                    bd2.setBounds(0, 0, originalDrawables[2].getIntrinsicWidth(), originalDrawables[2].getIntrinsicHeight());
                }
                this.setCompoundDrawables(bd0, originalDrawables[1], bd2, originalDrawables[3]);
            }

            if (a.hasValue(R.styleable.EnhancedTextView_textForeground)) {
                Drawable foreground = a.getDrawable(R.styleable.EnhancedTextView_textForeground);
                if (foreground != null) {
                    this.setForegroundDrawable(foreground);
                } else {
                    this.setTextColor(a.getColor(R.styleable.EnhancedTextView_textForeground, 0xff000000));
                }
            }

            if (a.hasValue(R.styleable.EnhancedTextView_textBackground)) {
                Drawable background = a.getDrawable(R.styleable.EnhancedTextView_textBackground);
                if (background != null) {
                    this.setBackgroundDrawable(background);
                } else {
                    this.setBackgroundColor(a.getColor(R.styleable.EnhancedTextView_textBackground, 0xff000000));
                }
            }


            if (a.hasValue(R.styleable.EnhancedTextView_innerShadowColor)) {
                this.addInnerShadow(a.getFloat(R.styleable.EnhancedTextView_innerShadowRadius, 0),
                        a.getFloat(R.styleable.EnhancedTextView_innerShadowDx, 0),
                        a.getFloat(R.styleable.EnhancedTextView_innerShadowDy, 0),
                        a.getColor(R.styleable.EnhancedTextView_innerShadowColor, 0xff000000));
            }

            if (a.hasValue(R.styleable.EnhancedTextView_outerShadowColor)) {
                this.addOuterShadow(a.getFloat(R.styleable.EnhancedTextView_outerShadowRadius, 0),
                        a.getFloat(R.styleable.EnhancedTextView_outerShadowDx, 0),
                        a.getFloat(R.styleable.EnhancedTextView_outerShadowDy, 0),
                        a.getColor(R.styleable.EnhancedTextView_outerShadowColor, 0xff000000));
            }

            if (a.hasValue(R.styleable.EnhancedTextView_strokeColor)) {
                float strokeWidth = a.getFloat(R.styleable.EnhancedTextView_strokeWidth, 1);
                int strokeColor = a.getColor(R.styleable.EnhancedTextView_strokeColor, 0xff000000);
                float strokeMiter = a.getFloat(R.styleable.EnhancedTextView_strokeMiter, 10);
                Join strokeJoin = null;
                switch (a.getInt(R.styleable.EnhancedTextView_strokeJoinStyle, 0)) {
                    case (0):
                        strokeJoin = Join.MITER;
                        break;
                    case (1):
                        strokeJoin = Join.BEVEL;
                        break;
                    case (2):
                        strokeJoin = Join.ROUND;
                        break;
                }
                this.setStroke(strokeWidth, strokeColor, strokeJoin, strokeMiter);
            }
            a.recycle();
        }
    }

    public void setStroke(float width, int color, Join join, float miter) {
        strokeWidth = width;
        strokeColor = color;
        strokeJoin = join;
        strokeMiter = miter;
    }

    public void setStroke(float width, int color) {
        setStroke(width, color, Join.MITER, 10);
    }

    public void addOuterShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        outerShadows.add(new Shadow(r, dx, dy, color));
    }

    public void addInnerShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        innerShadows.add(new Shadow(r, dx, dy, color));
    }

    public void clearInnerShadows() {
        innerShadows.clear();
    }

    public void clearOuterShadows() {
        outerShadows.clear();
    }

    public void setForegroundDrawable(Drawable d) {
        this.foregroundDrawable = d;
    }

    public Drawable getForeground() {
        return this.foregroundDrawable == null ? this.foregroundDrawable : new ColorDrawable(this.getCurrentTextColor());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mSpacingAddFloat = 0;
        try {
            TextView tv = new TextView(getContext());
            Field mSpacingAdd = tv.getClass().getDeclaredField("mSpacingAdd");
            mSpacingAdd.setAccessible(true);
            mSpacingAddFloat = Math.round(mSpacingAdd.getFloat(this));
            decresingLineSpace = mSpacingAddFloat < 0.0f;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (decresingLineSpace) {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() - mSpacingAddFloat);
            this.setGravity(getGravity() | Gravity.TOP);
        }
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        if (add < 0.0f || mult < 1.0f) {
            decresingLineSpace = true;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        freeze();
        CharSequence restoreText = this.getText();
        Drawable[] restoreDrawables = this.getCompoundDrawables();
        ColorStateList restoreColor = this.getTextColors();

        this.setCompoundDrawables(null, null, null, null);

        for (Shadow shadow : outerShadows) {
            this.setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
            super.onDraw(canvas); //Draws regular shadow on top of the text if shadows are set
        }
        this.setShadowLayer(0, 0, 0, 0);
        this.setTextColor(restoreColor);//And draw colored text on top

        if (this.foregroundDrawable != null && this.foregroundDrawable instanceof BitmapDrawable) {
            generateTempCanvas();
            super.onDraw(tempCanvas);
            Paint paint = ((BitmapDrawable) this.foregroundDrawable).getPaint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            this.foregroundDrawable.setBounds(canvas.getClipBounds());
            this.foregroundDrawable.draw(tempCanvas);
            canvas.drawBitmap(tempBitmap, 0, 0, null);
            tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

        if (strokeColor != null) {
            TextPaint paint = this.getPaint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(strokeJoin);
            paint.setStrokeMiter(strokeMiter);
            this.setTextColor(strokeColor);
            paint.setStrokeWidth(strokeWidth);
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL);
            this.setTextColor(restoreColor);
        }
        if (innerShadows.size() > 0) {
            generateTempCanvas();
            TextPaint paint = this.getPaint();
            for (Shadow shadow : innerShadows) {
                this.setTextColor(shadow.color);
                super.onDraw(tempCanvas);
                this.setTextColor(0xFF000000);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                paint.setMaskFilter(new BlurMaskFilter(shadow.r, BlurMaskFilter.Blur.NORMAL));

                tempCanvas.save();
                tempCanvas.translate(shadow.dx, shadow.dy);
                super.onDraw(tempCanvas);
                tempCanvas.restore();
                canvas.drawBitmap(tempBitmap, 0, 0, null);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                paint.setXfermode(null);
                paint.setMaskFilter(null);
                this.setTextColor(restoreColor);
                this.setShadowLayer(0, 0, 0, 0);
            }
        }


        if (isDrawableSticky && originalDrawables != null) {
            if (originalDrawables[0] != null) {
                this.setText(null);
                this.setCompoundDrawables(originalDrawables[0], null, null, null);
                canvas.save();
                canvas.translate(getPaddingLeft() + getWidth() / 2 -
                        (paint.measureText(restoreText, 0, restoreText.length())
                                + getCompoundDrawablePadding() + originalDrawables[0].getIntrinsicWidth() ) / 2, 0);
                super.onDraw(canvas);
                canvas.restore();
                this.setText(restoreText);
            }
            if (originalDrawables[2] != null) {
                this.setText(null);
                this.setCompoundDrawables(null, null, originalDrawables[2], null);
                canvas.save();
                canvas.translate(getPaddingRight() - getWidth() / 2 +
                        (paint.measureText(restoreText, 0, restoreText.length())
                                + getCompoundDrawablePadding()  + originalDrawables[2].getIntrinsicWidth()) / 2, 0);
                super.onDraw(canvas);
                canvas.restore();
                this.setText(restoreText);
            }
        }
        if (restoreDrawables != null) {
            this.setCompoundDrawables(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
        }
        unfreeze();
    }

    private void generateTempCanvas() {
        String key = String.format("%dx%d", getWidth(), getHeight());
        Pair<Canvas, Bitmap> stored = canvasStore.get(key);
        if (stored != null) {
            tempCanvas = stored.first;
            tempBitmap = stored.second;
        } else {
            tempCanvas = new Canvas();
            tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBitmap);
            canvasStore.put(key, new Pair<Canvas, Bitmap>(tempCanvas, tempBitmap));
        }
    }

    // Keep these things locked while onDraw in processing
    public void freeze() {
        lockedCompoundPadding = new int[]{
                getCompoundPaddingLeft(),
                getCompoundPaddingRight(),
                getCompoundPaddingTop(),
                getCompoundPaddingBottom()
        };
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }

    @Override
    public void requestLayout() {
        if (!frozen) super.requestLayout();
    }

    @Override
    public void postInvalidate() {
        if (!frozen) super.postInvalidate();
    }

    @Override
    public void postInvalidate(int left, int top, int right, int bottom) {
        if (!frozen) super.postInvalidate(left, top, right, bottom);
    }

    @Override
    public void invalidate() {
        if (!frozen) super.invalidate();
    }

    @Override
    public void invalidate(Rect rect) {
        if (!frozen) super.invalidate(rect);
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if (!frozen) super.invalidate(l, t, r, b);
    }

    @Override
    public int getCompoundPaddingLeft() {
        return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
    }

    @Override
    public int getCompoundPaddingRight() {
        return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
    }

    @Override
    public int getCompoundPaddingTop() {
        return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
    }

    @Override
    public int getCompoundPaddingBottom() {
        return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
    }

    public static class Shadow {
        float r;
        float dx;
        float dy;
        int color;

        public Shadow(float r, float dx, float dy, int color) {
            this.r = r;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }
    }
}
