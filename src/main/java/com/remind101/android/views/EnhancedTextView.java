package com.remind101.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;

import com.remind101.android.enhancedviews.R;
import com.remind101.ui.listeners.OnSelectionChangeListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

public class EnhancedTextView extends TextView {

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

    private OnSelectionChangeListener onSelectionChangedListener;

    private OnDrawableClick onDrawableClickListener;
    private Rect textBounds;
    private boolean consumeLeftDrawableTouch;
    private boolean consumeRightDrawableTouch;

    private int tokenBackgroundColor = 0;
    private int tokenBorderColor = 0;
    private int tokenTextColor = -1;

    private int tokenSelectedTextColor = -1;
    private int tokenSelectedBackgroundColor = 0;
    private int tokenSelectedBorderColor = 0;

    private int tokenVerticalPadding;
    private int tokenHorizontalPadding;
    private int tokenBorderRadius;

    protected static final int LEFT = 0;
    protected static final int TOP = 1;
    protected static final int RIGHT = 2;
    protected static final int BOTTOM = 3;

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
    public boolean onTouchEvent(MotionEvent event) {
        if (onDrawableClickListener != null) {
            Drawable rightDrawable = getCompoundDrawables()[2];
            if (rightDrawable != null) {
                Rect rightBounds = getRightDrawableBounds();
                boolean isInRightDrawable = rightBounds.contains((int) event.getX(), (int) event.getY());
                if (isInRightDrawable) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            rightDrawable.setState(new int[]{android.R.attr.state_pressed});
                            rightDrawable.invalidateSelf();
                            break;
                        case MotionEvent.ACTION_UP:
                            rightDrawable.setState(null);
                            rightDrawable.invalidateSelf();
                            onDrawableClickListener.onRightDrawableClick(this);
                            break;
                    }
                    return consumeRightDrawableTouch || super.onTouchEvent(event);
                } else {
                    if (rightDrawable.getState() != null) {
                        rightDrawable.setState(null);
                        rightDrawable.invalidateSelf();
                    }
                }
            }

            Drawable leftDrawable = getCompoundDrawables()[0];
            if (leftDrawable != null) {
                Rect leftBounds = getLeftDrawableBounds();
                boolean isInLeftDrawable = leftBounds.contains((int) event.getX(), (int) event.getY());
                if (isInLeftDrawable) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            leftDrawable.setState(new int[]{android.R.attr.state_pressed});
                            leftDrawable.invalidateSelf();
                            break;
                        case MotionEvent.ACTION_UP:
                            leftDrawable.setState(null);
                            leftDrawable.invalidateSelf();
                            onDrawableClickListener.onLeftDrawableClick(this);
                            break;
                    }
                    return consumeLeftDrawableTouch || super.onTouchEvent(event);
                } else {
                    if (leftDrawable.getState() != null) {
                        leftDrawable.setState(null);
                        leftDrawable.invalidateSelf();
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public Rect getLeftDrawableBounds() {
        return getDrawableBounds(LEFT);
    }

    public Rect getRightDrawableBounds() {
        return getDrawableBounds(RIGHT);
    }

    public Rect getTopDrawableBounds() {
        return getDrawableBounds(TOP);
    }

    public Rect getBottomDrawableBounds() {
        return getDrawableBounds(BOTTOM);
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

            int tokenStyle = a.getResourceId(R.styleable.EnhancedTextView_tokenStyle, -1);
            if (tokenStyle > 0) {
                TypedArray tokenStyleArray = getContext().obtainStyledAttributes(tokenStyle, R.styleable.EnhancedTextView);

                tokenBackgroundColor = a.getColor(R.styleable.EnhancedTextView_tokenBackgroundColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenBackgroundColor, 0));
                tokenSelectedBackgroundColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedBackgroundColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenSelectedBackgroundColor, tokenBackgroundColor));
                tokenBorderColor = a.getColor(R.styleable.EnhancedTextView_tokenBorderColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenBorderColor, 0));
                tokenSelectedBorderColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedBorderColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenSelectedBorderColor, tokenBorderColor));
                tokenTextColor = a.getColor(R.styleable.EnhancedTextView_tokenTextColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenTextColor, -1));
                tokenSelectedTextColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedTextColor, tokenStyleArray.getColor(R.styleable.EnhancedTextView_tokenSelectedTextColor, tokenTextColor));
                tokenVerticalPadding = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenVerticalPadding, tokenStyleArray.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenVerticalPadding, 0));
                tokenHorizontalPadding = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenHorizontalPadding, tokenStyleArray.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenHorizontalPadding, 0));
                tokenBorderRadius = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenBorderRadius, tokenStyleArray.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenBorderRadius, 0));
                tokenStyleArray.recycle();
            } else {
                tokenBackgroundColor = a.getColor(R.styleable.EnhancedTextView_tokenBackgroundColor, 0);
                tokenSelectedBackgroundColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedBackgroundColor, tokenBackgroundColor);
                tokenBorderColor = a.getColor(R.styleable.EnhancedTextView_tokenBorderColor, 0);
                tokenSelectedBorderColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedBorderColor, tokenBorderColor);
                tokenTextColor = a.getColor(R.styleable.EnhancedTextView_tokenTextColor, -1);
                tokenSelectedTextColor = a.getColor(R.styleable.EnhancedTextView_tokenSelectedTextColor, tokenTextColor);
                tokenVerticalPadding = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenVerticalPadding, 0);
                tokenHorizontalPadding = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenHorizontalPadding, 0);
                tokenBorderRadius = a.getDimensionPixelSize(R.styleable.EnhancedTextView_tokenBorderRadius, 0);
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
    public void setText(CharSequence text, BufferType type) {
        try {
            super.setText(text, type);
        } catch (AndroidRuntimeException e) {
            // This can happen when the app is opened while the user is updating the System WebView in the Play Store, and will crash
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
            textPaint.getTextBounds(restoreText.toString(), 0, restoreText.length(), textBounds);
            final int vspace = getHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
            final int hspace = getWidth() - getCompoundPaddingRight() - getCompoundPaddingLeft();
            for (int i = 0; i < originalDrawables.length; i++) {
                if (originalDrawables[i] != null) {
                    getDrawableBounds(i, reusableRect, vspace, hspace);
                    originalDrawables[i].setBounds(reusableRect);
                    originalDrawables[i].draw(canvas);
                }
            }
        }
        if (restoreDrawables != null) {
            this.setCompoundDrawables(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
        }
        unfreeze();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (originalDrawables != null) {
            int[] stateSet = getDrawableState();
            for (Drawable drawable : originalDrawables) {
                if (drawable != null) {
                    drawable.setState(stateSet);
                }
            }
        }
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

    protected Rect getDrawableBounds(int which) {
        Rect rect = new Rect();
        getDrawableBounds(which, rect);
        return rect;
    }

    protected void getDrawableBounds(int which, Rect rect) {
        final int vspace = getHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
        final int hspace = getWidth() - getCompoundPaddingRight() - getCompoundPaddingLeft();
        getDrawableBounds(which, rect, vspace, hspace);
    }

    protected void getDrawableBounds(int which, Rect rect, int vspace, int hspace) {
        Drawable dr = isDrawableSticky ? originalDrawables[which] : getCompoundDrawables()[which];
        if (dr == null) {
            return;
        }
        switch (which) {
            case LEFT:
                if (isDrawableSticky) {
                    // TODO: This doesn't work
                    rect.left = getScrollX() + (getWidth() - dr.getIntrinsicWidth()
                            - getCompoundDrawablePadding() - getMaxLineWidth()) / 2;
                } else {
                    rect.left = getPaddingLeft();
                }
                rect.top = getScrollY() + getCompoundPaddingTop()
                        + (vspace - dr.getIntrinsicHeight()) / 2;
                break;
            case RIGHT:
                if (isDrawableSticky) {
                    // TODO: This doesn't work
                    rect.left = getScrollX() + (getWidth() + getCompoundDrawablePadding()
                            + getMaxLineWidth() - dr.getIntrinsicWidth()) / 2;
                } else {
                    rect.left = getWidth() - getPaddingRight() - dr.getIntrinsicWidth();
                }
                rect.top = getScrollY() + getCompoundPaddingTop()
                        + (vspace - dr.getIntrinsicHeight()) / 2;
                break;
            case TOP:
                rect.left = getScrollX() + getCompoundPaddingLeft()
                        + (hspace - dr.getIntrinsicWidth()) / 2;
                if (isDrawableSticky) {
                    rect.top = getScrollY() + (getHeight() - textBounds.height()
                            - getCompoundDrawablePadding() - dr.getIntrinsicHeight()) / 2;
                } else {
                    rect.top = getScrollY() + getPaddingTop();
                }
                break;
            case BOTTOM:
                rect.left = getScrollX() + getCompoundPaddingLeft()
                        + (hspace - dr.getIntrinsicWidth()) / 2;
                if (isDrawableSticky) {
                    rect.top = getScrollY() + (getHeight() + textBounds.height()
                            + getCompoundDrawablePadding() - dr.getIntrinsicHeight()) / 2;
                } else {
                    rect.top = getScrollY() + getHeight()
                            - getPaddingBottom() - dr.getIntrinsicHeight();
                }
                break;
        }
        rect.right = rect.left + dr.getIntrinsicWidth();
        rect.bottom = rect.top + dr.getIntrinsicHeight();
    }

    private int getMaxLineWidth() {
        Layout layout = getLayout();
        if (layout != null) {
            int lines = layout.getLineCount();
            if (lines > 1) {
                float maxWidth = 0f;
                for (int i = 0; i < lines; i++) {
                    if (layout.getLineWidth(i) > maxWidth) {
                        maxWidth = layout.getLineWidth(i);
                    }
                }
                return (int) FloatMath.ceil(maxWidth);
            }
        }
        return textBounds.width();
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


    public int getTokenBackgroundColor() {
        return tokenBackgroundColor;
    }

    public int getTokenBorderColor() {
        return tokenBorderColor;
    }

    public int getTokenTextColor() {
        return tokenTextColor;
    }

    public int getTokenVerticalPadding() {
        return tokenVerticalPadding;
    }

    public int getTokenHorizontalPadding() {
        return tokenHorizontalPadding;
    }

    public int getTokenBorderRadius() {
        return tokenBorderRadius;
    }

    public int getTokenSelectedTextColor() {
        return tokenSelectedTextColor;
    }

    public int getTokenSelectedBackgroundColor() {
        return tokenSelectedBackgroundColor;
    }

    public int getTokenSelectedBorderColor() {
        return tokenSelectedBorderColor;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        onSelectionChangedListener = listener;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (onSelectionChangedListener!= null) {
            onSelectionChangedListener.onSelectionChanged(this, selStart, selEnd);
        }
    }
}
