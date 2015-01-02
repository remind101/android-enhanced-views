package com.remind101.android.views;

import android.content.Context;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import com.remind101.android.enhancedviews.R;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 5/5/13
 * Time: 8:39 PM
 */
public class EnhancedButton extends EnhancedTextView {

    public EnhancedButton(Context context) {
        this(context, null);
    }

    public EnhancedButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.EnhancedButtonStyle);
    }

    public EnhancedButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    protected boolean consumeRightDrawableTouchByDefault() {
        return false;
    }
}
