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
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * This is a modified version of Android's {@link android.support.v4.widget.SwipeRefreshLayout}.
 * It uses our own spinner indicator instead of the android default one. It also support customized
 * indicator as you can subclass {@link SwipeRefreshLayoutIndicator} and use your own custom class for
 * drawing and loading animation.
 *
 * Created by kjia on 9/29/15.
 */
public class CustomSwipeRefreshLayout extends ViewGroup {
    private static final String LOG_TAG = CustomSwipeRefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.1f * MAX_ALPHA);

    /**
     * The diameter (size) of the spinner
     */
    private static final int SPINNER_DIAMETER = 30;

    /**
     * Distance on y until which we don't show the spinner
     */
    private static final int HIDE_SPINNER_DISTANCE = 30;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    /**
     * Default offset in dips from the top of the view to where the progress spinner should stop
     */
    private static final int DEFAULT_SPINNER_TARGET = 80;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.enabled
    };

    private SwipeRefreshLayoutIndicator mSpinner;
    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private OffsetViewListener mViewListener;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mSpinnerWidth;

    private int mSpinnerHeight;

    private float mHideSpinnerDistance;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomStart;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mSpinner.setSpinnerAlpha(MAX_ALPHA);
                mSpinner.startLoadingAnimation();
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                mSpinner.stopLoadingAnimation();
                mSpinner.setVisibility(View.GONE);
            }
            mCurrentTargetOffsetTop = mSpinner.getTop();
        }
    };

    /**
     * Simple constructor to use when creating a DoublePlaySwipeRefreshLayout from code.
     *
     * @param context
     */
    public CustomSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating DoublePlaySwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mSpinnerWidth = (int) (SPINNER_DIAMETER * metrics.density);
        mSpinnerHeight = (int) (SPINNER_DIAMETER * metrics.density);


        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_SPINNER_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
        mHideSpinnerDistance = HIDE_SPINNER_DISTANCE * metrics.density;
    }


    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *            progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mSpinner.setVisibility(View.GONE);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
        if (end >= 0) {
            mSpinnerFinalOffset = end;
        }
        mUsingCustomStart = true;
        mSpinner.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mSpinner.invalidate();
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mSpinner = new SwipeRefreshLayoutIndicator(getContext(), this);
        mSpinner.setVisibility(View.GONE);
        addView(mSpinner);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            mNotify = false;
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void startScaleUpAnimation(AnimationListener listener) {
        mSpinner.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mSpinner.setSpinnerAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mSpinner.setAnimationListener(listener);
        }
        mSpinner.clearAnimation();
        mSpinner.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            mSpinner.setSpinnerAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mSpinner, progress);
            ViewCompat.setScaleY(mSpinner, progress);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                animateOffsetToStartPosition(mCurrentTargetOffsetTop, mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mSpinner.setAnimationListener(listener);
        mSpinner.clearAnimation();
        mSpinner.startAnimation(mScaleDownAnimation);
    }


    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColorSchemeResource(int colorRes) {
        setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(int color) {
        mSpinner.setBackgroundColor(color);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mSpinner)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mSpinner.getMeasuredWidth();
        int circleHeight = mSpinner.getMeasuredHeight();
        mSpinner.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentTargetOffsetTop + circleHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mSpinner.measure(MeasureSpec.makeMeasureSpec(mSpinnerWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mSpinnerHeight, MeasureSpec.EXACTLY));
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mSpinner.getMeasuredHeight();
        }
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mSpinner) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout. This is not valid until a measure pass has
     * completed.
     *
     * @return Diameter in pixels of the progress circle view.
     */
    public int getProgressCircleDiameter() {
        return mSpinner != null ? mSpinner.getMeasuredHeight() : 0;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp() || mRefreshing) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int offset = mOriginalOffsetTop - mSpinner.getTop();
                setTargetOffsetTopAndBottom(offset, true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                    mSpinner.setSpinnerAlpha(STARTING_PROGRESS_ALPHA);
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsBeingDragged) {
                    float originalDragPercent = overscrollTop / mTotalDragDistance;
                    if (originalDragPercent < 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
                    float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset
                            - mOriginalOffsetTop : mSpinnerFinalOffset;
                    float tensionSlingshotPercent = Math.max(0,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                            (tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;

                    int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
                    // where 1.0f is a full circle
                    if (mSpinner.getVisibility() != View.VISIBLE) {
                        mSpinner.setVisibility(View.VISIBLE);
                    }
                    if (!mScale) {
                        ViewCompat.setScaleX(mSpinner, 1f);
                        ViewCompat.setScaleY(mSpinner, 1f);
                    }
                    // Animate spinner's alpha based on y position relative to max distance
                    if (overscrollTop < mTotalDragDistance) {
                        if (mScale) {
                            setAnimationProgress(overscrollTop / mTotalDragDistance);
                        }
                        float overScrollTopToShow = overscrollTop - mHideSpinnerDistance;
                        float progress;
                        if (overScrollTopToShow > 0) {
                            progress = overScrollTopToShow / (mTotalDragDistance - mHideSpinnerDistance);
                        } else {
                            progress = 0;
                        }
                        mSpinner.setSpinnerAlpha((int) (progress * MAX_ALPHA));
                        mSpinner.animateUnderDraggableDistance(dragPercent, tensionPercent);
                    }
                    mSpinner.animateUnderDrag(dragPercent, tensionPercent);

                    int offset = targetY - mCurrentTargetOffsetTop;
                    setTargetOffsetTopAndBottom(offset, true /* requires update */);

                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                if (overscrollTop > mTotalDragDistance) {
                    setRefreshing(true, true /* notify */);
                } else {
                    // cancel refresh
                    mRefreshing = false;
                    mSpinner.animateUnderDrag(0f, 0f);
                    Animation.AnimationListener listener = null;
                    if (!mScale) {
                        listener = new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                if (!mScale) {
                                    startScaleDownAnimation(null);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                        };
                    }
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mSpinner.setAnimationListener(listener);
        }
        mSpinner.clearAnimation();
        mSpinner.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mSpinner.setAnimationListener(listener);
            }
            mSpinner.clearAnimation();
            mSpinner.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            endTarget = (int) mSpinnerFinalOffset;
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mCurrentTargetOffsetTop;
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mSpinner.getTop();

        setTargetOffsetTopAndBottom(offset, false /* requires update */);

    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mStartingScale = ViewCompat.getScaleX(mSpinner);
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale  * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mSpinner.setAnimationListener(listener);
        }
        mSpinner.clearAnimation();
        mSpinner.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mSpinner.bringToFront();
        mSpinner.offsetTopAndBottom(offset);
        if (mViewListener != null) {
            mViewListener.onOffset(offset);
        }
        mCurrentTargetOffsetTop = mSpinner.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     *
     * @param listener the class that will receive the callback with an offset that the view should move.
     */
    public void setOffsetViewListener(OffsetViewListener listener) {
        mViewListener = listener;
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        void onRefresh();
    }

    /**
     * Classes that wants to offset one of its view along with the spinner should
     * implement this interface
     */
    public interface OffsetViewListener {
        /**
         * Called whenever the spinner is offsetting
         */
        void onOffset(int offset);
    }
}



