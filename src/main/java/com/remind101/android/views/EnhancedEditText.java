package com.remind101.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.View;

import com.remind101.android.enhancedviews.R;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 5/30/13
 * Time: 11:30 PM
 */
public class EnhancedEditText extends EnhancedTextView implements View.OnFocusChangeListener, EnhancedTextView.OnDrawableClick {

    private Drawable xD;
    private boolean isCleanable;
    private OnFocusChangeListener f;
    private Drawable originalRightDrawable;


    public EnhancedEditText(Context context) {
        this(context, null);
    }

    public EnhancedEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.EnhancedEditTextStyle);
    }

    public EnhancedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EnhancedEditText, defStyle, android.R.style.Widget_EditText);
            if (a.getBoolean(R.styleable.EnhancedEditText_clearable, false)) {
                isCleanable = true;
                originalRightDrawable = getCompoundDrawables()[2];
                xD = a.getDrawable(R.styleable.EnhancedEditText_clearDrawable);
                if (xD == null) {
                    xD = getCompoundDrawables()[2];
                    if (getCompoundDrawables()[2] == null) {
                        xD = getResources().getDrawable(android.R.drawable.presence_offline);
                    }
                }
                xD.setBounds(0, 0, xD.getIntrinsicWidth(), xD.getIntrinsicHeight());
                setClearIconVisible(false);
                super.setOnDrawableClickListener(this);
                super.setOnFocusChangeListener(this);
                addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (isFocused()) {
                            setClearIconVisible(s.length() > 0);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                });
            }
            a.recycle();
        }
    }

    public boolean isCleanable() {
        return isCleanable;
    }


    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener f) {
        this.f = f;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            setClearIconVisible(getText().length() > 0);
        } else {
            setClearIconVisible(false);
        }
        if (f != null) {
            f.onFocusChange(v, hasFocus);
        }
    }

    protected void setClearIconVisible(boolean visible) {
        Drawable x = visible ? xD : originalRightDrawable;
        setCompoundDrawables(getCompoundDrawables()[0],
                getCompoundDrawables()[1], x, getCompoundDrawables()[3]);
    }

    @Override
    protected boolean getDefaultEditable() {
        return true;
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    public Editable getText() {
        return (Editable) super.getText();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, BufferType.EDITABLE);
    }

    public void setSelection(int start, int stop) {
        Selection.setSelection(getText(), start, stop);
    }

    public void setSelection(int index) {
        Selection.setSelection(getText(), index);
    }

    /**
     * Convenience for {@link Selection#selectAll}.
     */
    public void selectAll() {
        Selection.selectAll(getText());
    }

    public void extendSelection(int index) {
        Selection.extendSelection(getText(), index);
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt ellipsis) {
        if (ellipsis == TextUtils.TruncateAt.MARQUEE) {
            throw new IllegalArgumentException("EditText cannot use the ellipsize mode "
                    + "TextUtils.TruncateAt.MARQUEE");
        }
        super.setEllipsize(ellipsis);
    }


    @Override
    public void onRightDrawableClick(EnhancedTextView textView) {
        setText(null);
    }

    @Override
    public void onLeftDrawableClick(EnhancedTextView textView) {

    }
}
