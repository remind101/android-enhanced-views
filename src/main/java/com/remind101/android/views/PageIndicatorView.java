package com.remind101.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.remind101.android.enhancedviews.R;


/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/12/13
 * Time: 2:41 PM
 */
public class PageIndicatorView extends View {

    private int circleRadius;
    private int offset;
    private Drawable shadowDrawable;
    private Drawable doneImage;
    private Drawable activeCircle;
    private Drawable inactiveCircle;
    private int pageCount;
    private int currentPage;
    private Paint inactivePaint;
    private Paint numberPaint;
    private int firstCircleX;
    private int firstCircleY;
    private int dwPerPage;
    private Rect tempRect = new Rect();

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.PageIndicatorViewStyle);
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        inactivePaint = new Paint();
        inactivePaint.setStyle(Paint.Style.FILL);
        inactivePaint.setColor(0xFFDDDCD8);
        inactivePaint.setStrokeWidth(2);
        inactivePaint.setAntiAlias(true);

        numberPaint = new Paint();
        numberPaint.setColor(getResources().getColor(android.R.color.white));
        int textSizeDP = 18;
        numberPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                textSizeDP, getResources().getDisplayMetrics()));
        numberPaint.setAntiAlias(true);

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PageIndicatorView, defStyle, 0);
        try {
            pageCount = a.getInteger(R.styleable.PageIndicatorView_pageCount, 1);
            currentPage = a.getInteger(R.styleable.PageIndicatorView_currentPage, 1);

            String typefaceName = a.getString(R.styleable.PageIndicatorView_font);
            if (typefaceName != null && !typefaceName.equals("") && !isInEditMode()) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                numberPaint.setTypeface(tf);
            }
            shadowDrawable = a.getDrawable(R.styleable.PageIndicatorView_shadowDrawable);
            if (shadowDrawable != null && !shadowDrawable.isStateful()) {
                ((BitmapDrawable) shadowDrawable).setTileModeX(Shader.TileMode.REPEAT);
            }
            doneImage = a.getDrawable(R.styleable.PageIndicatorView_doneDrawable);
            activeCircle = a.getDrawable(R.styleable.PageIndicatorView_activeCircleDrawable);
            inactiveCircle = a.getDrawable(R.styleable.PageIndicatorView_inActiveCircleDrawable);
            circleRadius = Math.max(Math.max(activeCircle.getIntrinsicHeight(), activeCircle.getIntrinsicWidth()),
                    Math.max(inactiveCircle.getIntrinsicHeight(), inactiveCircle.getIntrinsicWidth())) / 2;
            offset = (int) (4 * circleRadius / 2.5f);
        } finally {
            a.recycle();
        }
    }

    /**
     * This calculates minimum view estate needed for placing it's content (dots with numbers) when size is not set exactly in xml
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int otherPages = 0;
        for (int i = 0; i < pageCount; i++) {
            otherPages += offset + 4 * circleRadius;
        }
        int w = resolveSize(4 * circleRadius + otherPages + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        int h = resolveSize(4 * circleRadius + getPaddingTop() + getPaddingBottom() + (shadowDrawable == null ? 0 : shadowDrawable.getIntrinsicHeight()), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        dwPerPage = (width - getPaddingLeft() - getPaddingRight()) / pageCount;
        firstCircleX = width / 2 - ((pageCount - 1) * dwPerPage / 2);
        firstCircleY = (height - getPaddingBottom() + getPaddingTop() - (shadowDrawable == null ? 0 : shadowDrawable.getIntrinsicHeight())) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < pageCount; i++) {
            String pageName = String.valueOf(i + 1);
            numberPaint.getTextBounds(pageName, 0, pageName.length(), tempRect);
            int currentCircleCenterX = firstCircleX + i * (dwPerPage);

            if (i == currentPage - 1) {
                int circleLeft = currentCircleCenterX - activeCircle.getIntrinsicWidth() / 2;
                int circleTop = firstCircleY - activeCircle.getIntrinsicHeight() / 2;
                activeCircle.setBounds(
                        circleLeft,
                        circleTop,
                        circleLeft + activeCircle.getIntrinsicWidth(),
                        circleTop + activeCircle.getIntrinsicHeight());
                activeCircle.draw(canvas);
                canvas.drawText(pageName, currentCircleCenterX - tempRect.width() / 2, firstCircleY + tempRect.height() / 2, numberPaint);
            } else {
                int circleLeft = currentCircleCenterX - inactiveCircle.getIntrinsicWidth() / 2;
                int circleTop = firstCircleY - inactiveCircle.getIntrinsicHeight() / 2;
                inactiveCircle.setBounds(
                        circleLeft,
                        circleTop,
                        circleLeft + inactiveCircle.getIntrinsicWidth(),
                        circleTop + inactiveCircle.getIntrinsicHeight());
                inactiveCircle.draw(canvas);
                if (i > currentPage - 1) {
                    canvas.drawText(pageName, (float) (currentCircleCenterX - Math.ceil(tempRect.width() / 2f)), firstCircleY + tempRect.height() / 2f, numberPaint);
                } else {
                    int checkLeft = currentCircleCenterX - doneImage.getIntrinsicWidth() / 2;
                    int checkTop = firstCircleY - doneImage.getIntrinsicHeight() / 2;
                    doneImage.setBounds(
                            checkLeft,
                            checkTop,
                            checkLeft + doneImage.getIntrinsicWidth(),
                            checkTop + doneImage.getIntrinsicHeight());
                    doneImage.draw(canvas);
                }
            }

            if (i == pageCount - 1) { //if last circle is being draw, draw the shadow and exit
                if (shadowDrawable != null) {
                    shadowDrawable.setBounds(0, 0, getWidth(), shadowDrawable.getIntrinsicHeight());
                    canvas.save();
                    canvas.translate(0, getHeight() - shadowDrawable.getIntrinsicHeight());
                    shadowDrawable.draw(canvas);
                    canvas.restore();
                }
                break;
            }
            canvas.drawLine(currentCircleCenterX + dwPerPage / 2f - offset / 2f, 0,
                    currentCircleCenterX + dwPerPage / 2f + offset / 2f, firstCircleY, inactivePaint);
            canvas.drawLine(currentCircleCenterX + dwPerPage / 2f - offset / 2f, 2 * firstCircleY,
                    currentCircleCenterX + dwPerPage / 2f + offset / 2f, firstCircleY, inactivePaint);
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

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        invalidate();
        requestLayout();
    }


}
