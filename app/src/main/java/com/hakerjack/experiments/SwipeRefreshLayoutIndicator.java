
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hakerjack.experiments;


import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * The refresh indicator at the top of the {@link CustomSwipeRefreshLayout} that appears when doing pull-to-refresh
 * Modified from {@link android.support.v4.widget.CircleImageView}
 *
 * Created by kjia on 9/29/15.
 */
public class SwipeRefreshLayoutIndicator extends ImageView {
    /**
     *  Max amount of circle that can be filled by progress during swipe gesture, where 1.0 is a full circle
     */
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private RefreshProgressSpinner mProgress;

    private Animation.AnimationListener mListener;

    public SwipeRefreshLayoutIndicator(Context context, View parent) {
        super(context);
        mProgress = new RefreshProgressSpinner(context, parent);
        setImageDrawable(mProgress);
        mProgress.showArrow(true);
    }

    public void setSpinnerAlpha(int alpha) {
        mProgress.setAlpha(alpha);
    }

    public void setBackgroundColor(int color) {
        mProgress.setBackgroundColor(color);
    }

    public void startLoadingAnimation() {
        mProgress.showArrow(false);
        mProgress.start();
    }

    public void stopLoadingAnimation() {
        mProgress.stop();
        mProgress.showArrow(true);
    }

    /**
     * Method to animate the drawable based on dragging distance on the screen
     * e.g. rotation, alpha, length, etc
     * @param dragPercent percentage of dragging distance over total allowable distance
     * @param tensionPercent percentage of dragging after applying a tension
     */
    public void animateUnderDrag(float dragPercent, float tensionPercent) {
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mProgress.setProgressRotation(rotation);
    }

    /**
     * Method to animate the drawable based on dragging distance on the screen
     * only called when the dragging distance is smaller than total allowed distance
     * @param dragPercent
     * @param tensionPercent
     */
    public void animateUnderDraggableDistance(float dragPercent, float tensionPercent) {
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float strokeStart = adjustedPercent * .8f;
        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
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

