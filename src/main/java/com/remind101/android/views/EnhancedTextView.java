package com.remind101.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Join;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.remind101.android.enhancedviews.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

public class EnhancedTextView extends TextView implements View.OnTouchListener {

    boolean isDrawableSticky;
    private ArrayList<Shadow> outerShadows;
    private ArrayList<Shadow> innerShadows;
    private ArrayList<BlurMaskFilter> innerShadowFilters;
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
    private TextPaint textPaint;
    private TextView tv;
    private Rect reusableRect; // Avoid allocation inside onDraw()
    private static final PorterDuffXfermode SRC_ATOP_XFER_MODE = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    private static final PorterDuffXfermode DST_OUT_XFER_MODE = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    private OnDrawableClick onDrawableClickListener;
    private Rect textBounds;
    private boolean consumeLeftDrawableTouch;
    private boolean consumeRightDrawableTouch;

    public interface OnDrawableClick {
        public void onRightDrawableClick(EnhancedTextView textView);

        public void onLeftDrawableClick(EnhancedTextView textView);
    }

    public OnDrawableClick getOnDrawableClickListener() {
        return onDrawableClickListener;
    }

    public void setOnDrawableClickListener(OnDrawableClick onDrawableClickListener) {
        this.onDrawableClickListener = onDrawableClickListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (getCompoundDrawables() != null) {
            Drawable rightDrawable = getCompoundDrawables()[2];
            if (rightDrawable != null) {
                boolean isInRightDrawable = event.getX() > (getWidth() - getPaddingRight() - rightDrawable.getIntrinsicWidth());
                if (isInRightDrawable) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            rightDrawable.setState(new int[]{android.R.attr.state_pressed});
                            rightDrawable.invalidateSelf();
                            break;
                        case MotionEvent.ACTION_UP:
                            rightDrawable.setState(null);
                            rightDrawable.invalidateSelf();
                            if (onDrawableClickListener != null) {
                                onDrawableClickListener.onRightDrawableClick(this);
                            }
                            break;
                    }
                    return consumeRightDrawableTouch || super.onTouchEvent(event);
                }
            }

            Drawable leftDrawable = getCompoundDrawables()[0];
            if (leftDrawable != null) {
                boolean isInLeftDrawable = event.getX() < (getPaddingLeft() + leftDrawable.getIntrinsicWidth());
                if (isInLeftDrawable) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            leftDrawable.setState(new int[]{android.R.attr.state_pressed});
                            leftDrawable.invalidateSelf();
                            break;
                        case MotionEvent.ACTION_UP:
                            leftDrawable.setState(null);
                            leftDrawable.invalidateSelf();
                            if (onDrawableClickListener != null) {
                                onDrawableClickListener.onLeftDrawableClick(this);
                            }
                            break;
                    }
                    return consumeLeftDrawableTouch || super.onTouchEvent(event);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public Rect getLeftDrawableBounds() {
        int[] wholeViewPos = new int[2];
        getLocationInWindow(wholeViewPos);
        int[] drawablePos = new int[2];
        Drawable leftDrawable = getCompoundDrawables()[0];
        drawablePos[0] = wholeViewPos[0] + getPaddingLeft();
        drawablePos[1] = wholeViewPos[1] + getHeight() / 2 - leftDrawable.getIntrinsicHeight() / 2;
        return new Rect(drawablePos[0],
                drawablePos[1],
                drawablePos[0] + leftDrawable.getIntrinsicWidth(),
                drawablePos[1] + leftDrawable.getIntrinsicHeight());
    }

    public Rect getRightDrawableBounds() {
        int[] wholeViewPos = new int[2];
        getLocationInWindow(wholeViewPos);
        int[] drawablePos = new int[2];
        Drawable rightDrawable = getCompoundDrawables()[2];
        drawablePos[0] = wholeViewPos[0] + getWidth() - getPaddingRight() - rightDrawable.getIntrinsicWidth();
        drawablePos[1] = wholeViewPos[1] + getHeight() / 2 - rightDrawable.getIntrinsicHeight() / 2;
        return new Rect(drawablePos[0],
                drawablePos[1],
                drawablePos[0] + rightDrawable.getIntrinsicWidth(),
                drawablePos[1] + rightDrawable.getIntrinsicHeight());
    }

    public EnhancedTextView(Context context) {
        this(context, null);
    }

    public EnhancedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.EnhancedTextViewStyle);
    }

    public EnhancedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
        setOnTouchListener(this);
    }

    public void init(AttributeSet attrs, int defStyle) {
        tv = new TextView(getContext());
        outerShadows = new ArrayList<Shadow>();
        innerShadows = new ArrayList<Shadow>();
        innerShadowFilters = new ArrayList<BlurMaskFilter>();
        reusableRect = new Rect();
        if (canvasStore == null) {
            canvasStore = new WeakHashMap<String, Pair<Canvas, Bitmap>>();
        }

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EnhancedTextView, defStyle, android.R.style.Widget_TextView);
            String typefaceName = a.getString(R.styleable.EnhancedTextView_typeface);
            if (!TextUtils.isEmpty(typefaceName) && !isInEditMode()) {
                try {
                    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                    this.setTypeface(tf);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }

            }

            if (a.hasValue(R.styleable.EnhancedTextView_drawableSticky)) {
                isDrawableSticky = a.getBoolean(R.styleable.EnhancedTextView_drawableSticky, false);
                originalDrawables = this.getCompoundDrawables();
                textPaint = new TextPaint();
                textPaint.setTextSize(this.getTextSize());
                textPaint.setTypeface(this.getTypeface());
                PaintDrawable bd0 = null;
                if (originalDrawables[0] != null) {
                    bd0 = new PaintDrawable(Color.TRANSPARENT);
                    bd0.setBounds(0, 0, originalDrawables[0].getIntrinsicWidth(), originalDrawables[0].getIntrinsicHeight());
                }
                PaintDrawable bd1 = null;
                if (originalDrawables[1] != null) {
                    bd1 = new PaintDrawable(Color.TRANSPARENT);
                    bd1.setBounds(0, 0, originalDrawables[1].getIntrinsicWidth(), originalDrawables[1].getIntrinsicHeight());
                }
                PaintDrawable bd2 = null;
                if (originalDrawables[2] != null) {
                    bd2 = new PaintDrawable(Color.TRANSPARENT);
                    bd2.setBounds(0, 0, originalDrawables[2].getIntrinsicWidth(), originalDrawables[2].getIntrinsicHeight());
                }
                PaintDrawable bd3 = null;
                if (originalDrawables[3] != null) {
                    bd3 = new PaintDrawable(Color.TRANSPARENT);
                    bd3.setBounds(0, 0, originalDrawables[3].getIntrinsicWidth(), originalDrawables[3].getIntrinsicHeight());
                }
                textBounds = new Rect();
                this.setCompoundDrawables(bd0, bd1, bd2, bd3);
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
            consumeLeftDrawableTouch = a.getBoolean(
                    R.styleable.EnhancedTextView_consumeLeftDrawableTouch,
                    consumeLeftDrawableTouchByDefault());
            consumeRightDrawableTouch = a.getBoolean(
                    R.styleable.EnhancedTextView_consumeRightDrawableTouch,
                    consumeRightDrawableTouchByDefault());
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
        innerShadowFilters.add(new BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL));
    }

    public void clearInnerShadows() {
        innerShadows.clear();
        innerShadowFilters.clear();
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
        float mSpacingAdd = getLineSpacingExtra();
        decresingLineSpace = mSpacingAdd < 0.0f;
        if (decresingLineSpace) {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() - (int) mSpacingAdd);
            this.setGravity(getGravity() | Gravity.TOP);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public float getLineSpacingExtra() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return super.getLineSpacingExtra();
        }
        float mSpacingAddFloat = 0;
        try {
            Field mSpacingAdd = tv.getClass().getDeclaredField("mSpacingAdd");
            mSpacingAdd.setAccessible(true);
            mSpacingAddFloat = mSpacingAdd.getFloat(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mSpacingAddFloat;
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
            paint.setXfermode(SRC_ATOP_XFER_MODE);
            canvas.getClipBounds(reusableRect);
            this.foregroundDrawable.setBounds(reusableRect);
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
        if (innerShadows.size() > 0 && getWidth() > 0 && getHeight() > 0) {
            generateTempCanvas();
            TextPaint paint = this.getPaint();
            for (int i = 0; i < innerShadows.size(); i++) {
                Shadow shadow = innerShadows.get(i);
                this.setTextColor(shadow.color);
                super.onDraw(tempCanvas);
                this.setTextColor(0xFF000000);
                paint.setXfermode(DST_OUT_XFER_MODE);
                paint.setMaskFilter(innerShadowFilters.get(i));

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
            int[] stateSet = getDrawableState();
            for (Drawable d : originalDrawables) {
                if (d != null) {
                    d.setState(stateSet);
                }
            }
            textPaint.getTextBounds(restoreText.toString(), 0, restoreText.length(), textBounds);
            if (originalDrawables[0] != null) {
                int drawableLeft = getWidth() / 2 - (originalDrawables[0].getIntrinsicWidth() + getCompoundDrawablePadding() + textBounds.width()) / 2;
                int drawableTop = getHeight() / 2 - originalDrawables[0].getIntrinsicHeight() / 2;
                originalDrawables[0].setBounds(drawableLeft, drawableTop,
                        drawableLeft + originalDrawables[0].getIntrinsicWidth(), drawableTop + originalDrawables[0].getIntrinsicHeight());
                originalDrawables[0].draw(canvas);
            }
            if (originalDrawables[1] != null) {
                int drawableLeft = getWidth() / 2 - originalDrawables[1].getIntrinsicWidth() / 2;
                int drawableTop = getHeight() / 2 - (textBounds.height() + getCompoundDrawablePadding() + originalDrawables[1].getIntrinsicHeight()) / 2;
                originalDrawables[1].setBounds(drawableLeft, drawableTop,
                        drawableLeft + originalDrawables[1].getIntrinsicWidth(), drawableTop + originalDrawables[1].getIntrinsicHeight());
                originalDrawables[1].draw(canvas);
            }
            if (originalDrawables[2] != null) {
                int drawableLeft = getWidth() / 2 + (originalDrawables[2].getIntrinsicWidth() + getCompoundDrawablePadding() + textBounds.width()) / 2 - originalDrawables[2].getIntrinsicWidth();
                int drawableTop = getHeight() / 2 - originalDrawables[2].getIntrinsicHeight() / 2;
                originalDrawables[2].setBounds(drawableLeft, drawableTop,
                        drawableLeft + originalDrawables[2].getIntrinsicWidth(), drawableTop + originalDrawables[2].getIntrinsicHeight());
                originalDrawables[2].draw(canvas);
            }
            if (originalDrawables[3] != null) {
                int drawableLeft = getWidth() / 2 - originalDrawables[3].getIntrinsicWidth() / 2;
                int drawableTop = getHeight() / 2 + (textBounds.height() + getCompoundDrawablePadding() + originalDrawables[3].getIntrinsicHeight()) / 2 - originalDrawables[3].getIntrinsicHeight();
                originalDrawables[3].setBounds(drawableLeft, drawableTop,
                        drawableLeft + originalDrawables[3].getIntrinsicWidth(), drawableTop + originalDrawables[3].getIntrinsicHeight());
                originalDrawables[3].draw(canvas);
            }
        }
        if (restoreDrawables != null) {
            this.setCompoundDrawables(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
        }
        unfreeze();
    }

    private void generateTempCanvas() {
        String key = String.format(Locale.getDefault(), "%dx%d", getWidth(), getHeight());
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

    protected boolean consumeLeftDrawableTouchByDefault() {
        return false;
    }

    protected boolean consumeRightDrawableTouchByDefault() {
        return true;
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
