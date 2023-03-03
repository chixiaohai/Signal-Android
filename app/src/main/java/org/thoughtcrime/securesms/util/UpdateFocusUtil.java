package org.thoughtcrime.securesms.util;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import org.thoughtcrime.securesms.R;

public final class UpdateFocusUtil {
    private int mFocusHeight;
    private int mNormalHeight;
    private int mFocusTextSize ;
    private int mNormalTextSize;
    private int mFocusPaddingX;
    private int mNormalPaddingX;

    public UpdateFocusUtil(Resources res){
        mFocusHeight = res.getDimensionPixelSize(R.dimen.focus_item_height);
        mNormalHeight = res.getDimensionPixelSize(R.dimen.item_height);
        mFocusTextSize = res.getDimensionPixelSize(R.dimen.focus_item_textsize);
        mNormalTextSize = res.getDimensionPixelSize(R.dimen.item_textsize);
        mFocusPaddingX = res.getDimensionPixelSize(R.dimen.focus_item_padding_x);
        mNormalPaddingX = res.getDimensionPixelSize(R.dimen.item_padding_x);
    }

    public void updateFocusView(View parent, boolean hasFocus,TextView tv,UpdateListener listener) {
        ValueAnimator va;
        if (hasFocus) {
            va = ValueAnimator.ofFloat(0, 1);
        } else {
            va = ValueAnimator.ofFloat(1, 0);
        }
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float scale = (float) valueAnimator.getAnimatedValue();
                float textSize = ((float) (mFocusTextSize - mNormalTextSize)) * (scale) + mNormalTextSize;
                float padding = (float) mNormalPaddingX - ((float) (mNormalPaddingX - mFocusPaddingX)) * (scale);
                int alpha = (int) ((float) 0x81 + (float) ((0xff - 0x81)) * (scale));
                int color = alpha * 0x1000000 + 0xffffff;

                listener.onUpdate(scale);

                tv.setTextColor(color);
                tv.setTextSize(textSize);
                parent.setPadding((int) padding, parent.getPaddingTop(), parent.getPaddingRight(), parent.getPaddingBottom());
            }
        });
        FastOutLinearInInterpolator FastOutLinearInInterpolator = new FastOutLinearInInterpolator();
        va.setInterpolator(FastOutLinearInInterpolator);
        va.setDuration(270);
        va.start();
    }

    public void updateFocusView(CheckedTextView view, boolean hasFocus, UpdateListener listener){
        ValueAnimator va;
        if (hasFocus) {
            va = ValueAnimator.ofFloat(0, 1);
        } else {
            va = ValueAnimator.ofFloat(1, 0);
        }
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float scale = (float) valueAnimator.getAnimatedValue();
                float height = ((float) (mFocusHeight - mNormalHeight)) * (scale) + (float) mNormalHeight;
                float textSize = ((float) (mFocusTextSize - mNormalTextSize)) * (scale) + (float) mNormalTextSize;
                float padding = (float) mNormalPaddingX - ((float) (mNormalPaddingX - mFocusPaddingX)) * (scale);
                int alpha = (int) ((float) 0x81 + (float) ((0xff - 0x81)) * (scale));
                int color = alpha * 0x1000000 + 0xffffff;
                view.setBackgroundColor(Color.argb(0, 0, 0, 0));

                view.setTextColor(color);
                view.setPadding((int) padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
                view.setTextSize((int) textSize);
                view.getLayoutParams().height = (int) height;
            }
        });
        FastOutLinearInInterpolator FastOutLinearInInterpolator = new FastOutLinearInInterpolator();
        va.setInterpolator(FastOutLinearInInterpolator);
        va.setDuration(270);
        va.start();
    }

    public interface  UpdateListener{
        void onUpdate(float scale);
    }

}
