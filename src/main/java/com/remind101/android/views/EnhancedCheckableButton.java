package com.remind101.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;

import com.remind101.android.enhancedviews.R;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 5/5/13
 * Time: 8:39 PM
 */
public class EnhancedCheckableButton extends EnhancedTextView implements Checkable, View.OnClickListener {

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };
    boolean isChecked;
    private OnCheckChangeListener onCheckedChangeListener;

    public interface OnCheckChangeListener {
        public void onStateChange(View view);
    }

    public EnhancedCheckableButton(Context context) {
        this(context, null);
    }

    public EnhancedCheckableButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.EnhancedCheckableButtonStyle);
    }

    public EnhancedCheckableButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EnhancedCheckableButton, defStyle, android.R.style.Widget_Button);
            if (a.hasValue(R.styleable.EnhancedCheckableButton_checked)) {
                isChecked = a.getBoolean(R.styleable.EnhancedCheckableButton_checked, false);
                setChecked(isChecked);
            }
            a.recycle();
        }
    }

    /**
     * Because of this - the field auto-store the text of it
     */
    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (isChecked != checked) {
            isChecked = checked;
            refreshDrawableState();
        }
    }

    public void setOnCheckedChangeListener(OnCheckChangeListener listener) {
        this.setClickable(true);
        this.onCheckedChangeListener = listener;
        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        toggle();
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onStateChange(this);
        }
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected boolean consumeRightDrawableTouchByDefault() {
        return false;
    }
}
