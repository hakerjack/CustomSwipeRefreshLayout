package com.hakerjack.experiments;

import android.content.Context;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * Created by kj on 9/28/15.
 */
public class CustomCircleView extends ImageView {

    private Animation.AnimationListener mListener;

    public CustomCircleView(Context context, int color, final float radius) {
        super(context);
    }

    private boolean elevationSupported() {
        return android.os.Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!elevationSupported()) {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
        }
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }

}
