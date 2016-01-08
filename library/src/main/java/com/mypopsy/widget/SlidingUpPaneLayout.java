package com.mypopsy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by renaud on 07/01/16.
 */
public class SlidingUpPaneLayout extends ViewGroup {

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * Drag sensitivity.
     */
    private static final float DEFAULT_DRAG_SENSITIVITY = 1f;
    public static final float DEFAULT_ANCHOR_POINT = .333f;

    /**
     * Current state of the bottom panel.
     */
    public enum State {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        DRAGGING
    }

    /**
     * Listener for monitoring events about the sliding panel.
     */
    public interface PanelSlideListener {
        void onPanelSlide(View panel, float slideOffset);
        void onPanelCollapsed(View panel);
        void onPanelExpanded(View panel);
        void onPanelAnchored(View panel);
    }

    /**
     * The child view that can slide, if any.
     */
    @Nullable
    private View mSlideableView;

    /**
     * How far the panel is offset from its closed position.
     * range [0, 1] where 0 = closed, 1 = open.
     */
    private float mSlideOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    private int mSlideRange;

    /**
     * Anchor point.
     * range [0, 1] where 0 = closed, 1 = open.
     */
    private float mAnchorPoint = .5f;

    /**
     * True if the main content must be clipped to the top of the slidable view.
     */
    private boolean mClip = false;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    private boolean mIsUnableToDrag;

    /**
     * Content scrim.
     */
    @Nullable
    private Drawable mContentScrim;

    /**
     * Scrim alpha
     */
    private int mScrimAlpha;

    /**
     * Shadow drawable.
     */
    @Nullable
    private Drawable mShadowDrawable;


    private ViewDragHelper mDragHelper;
    private List<PanelSlideListener> mListeners = new ArrayList<>();
    private boolean mFirstLayout = true;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private State mState = State.COLLAPSED;

    private final Rect mTmpRect = new Rect();
    private final int[] mLocationOnScreen = new int[2];
    private final int[] mTmp = new int[2];
    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    public SlidingUpPaneLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPaneLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPaneLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final float density = getContext().getResources().getDisplayMetrics().density;
        mDragHelper = ViewDragHelper.create(this, DEFAULT_DRAG_SENSITIVITY, new DragHelperCallbacks());
        mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * density);

        applyXmlAttributes(attrs, defStyleAttr);
    }

    private void applyXmlAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SlidingUpPaneLayout, defStyleAttr, 0);
        setAnchorPoint(a.getFloat(R.styleable.SlidingUpPaneLayout_supl_anchor, DEFAULT_ANCHOR_POINT));
        setContentScrim(a.getDrawable(R.styleable.SlidingUpPaneLayout_supl_contentScrim));
        setShadowDrawable(a.getDrawable(R.styleable.SlidingUpPaneLayout_supl_shadow));
        a.recycle();
    }

    public void setAnchorPoint(float anchorPoint) {
        mAnchorPoint = anchorPoint;
    }

    public void addPaneListener(PanelSlideListener listener) {
        mListeners.add(listener);
    }

    public void removePaneListener(PanelSlideListener listener) {
        mListeners.remove(listener);
    }

    public State getState() {
        return mState;
    }

    public float getSlideOffset() {
        return mSlideOffset;
    }

    public boolean isExpanded() {
        return mState == State.EXPANDED;
    }

    public boolean isCollapsed() {
        return mState == State.COLLAPSED;
    }

    public boolean isAnchored() {
        return mState == State.ANCHORED;
    }

    public void setState(@NonNull State state) {
        setState(state, true);
    }

    @SuppressWarnings("ConstantConditions")
    public void setState(@NonNull State state, boolean animate) {
        if (state == null || state == State.DRAGGING)
            throw new IllegalArgumentException("state cannot be "+state);

        if (!isEnabled() || (!mFirstLayout && mSlideableView == null) ||
                state == mState || mState == State.DRAGGING)
            return;

        if (mFirstLayout) {
            mState = state;
        } else {
            switch (state) {
                case ANCHORED:
                    settleTo(mAnchorPoint, animate);
                    break;
                case COLLAPSED:
                    settleTo(0, animate);
                    break;
                case EXPANDED:
                    settleTo(1.0f, animate);
                    break;
            }
        }
    }

    public void setShadowDrawable(@Nullable Drawable drawable) {
        if(mShadowDrawable != drawable) {
            mShadowDrawable = drawable;
            setWillNotDraw(mShadowDrawable == null);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setShadowDrawableResource(@DrawableRes int res) {
        setShadowDrawable(getResources().getDrawable(res));
    }

    public void setContentScrimResource(@DrawableRes int res) {
        setContentScrim(getResources().getDrawable(res));
    }

    public void setContentScrimColor(@ColorInt int color) {
        setContentScrim(new ColorDrawable(color));
    }

    public void setContentScrim(@Nullable Drawable drawable) {
        if (mContentScrim != drawable) {
            if (mContentScrim != null) {
                mContentScrim.setCallback(null);
            }
            if (drawable != null) {
                mContentScrim = drawable.mutate();
                drawable.setBounds(0, 0, getWidth(), getHeight());
                drawable.setCallback(this);
                drawable.setAlpha(mScrimAlpha);
            } else {
                mContentScrim = null;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void setScrimAlpha(@IntRange(from = 0, to = 255) int alpha) {
        if (alpha != mScrimAlpha) {
            mScrimAlpha = alpha;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        if (!isEnabled()) {
            return super.onTouchEvent(ev);
        }

        final int action = ev.getAction();
        mDragHelper.processTouchEvent(ev);

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }
            case MotionEvent.ACTION_UP: {
                // close on tap outside
                if (mState != State.COLLAPSED) {
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mDragHelper.getTouchSlop();
                    if ((dx * dx + dy * dy < slop * slop) &&
                            !mDragHelper.isViewUnder(mSlideableView, (int) x, (int) y)) {
                        setState(State.COLLAPSED);
                        break;
                    }
                }
                break;
            }
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (!isEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float dy = y - mInitialMotionY;
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(dy);
                final int slop = mDragHelper.getTouchSlop();

                if ((ady > slop && adx > ady) ||
                        (dy > 0 && mState == State.EXPANDED &&
                                canScrollVertically(mSlideableView,
                                        (int) mInitialMotionX, (int) mInitialMotionY, -1))) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }

            }
        }

        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (h != oldh) {
            mFirstLayout = true;
        }

        if (mContentScrim != null) {
            mContentScrim.setBounds(0, 0, w, h);
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        if (getChildCount() > 2) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " only supports 2 children");
        }

        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

        mMatchParentChildren.clear();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final MarginLayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        count = mMatchParentChildren.size();

        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth()
                            - getPaddingLeft() - getPaddingRight()
                            - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight() +
                                    lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight()
                            - getPaddingTop() - getPaddingBottom()
                            - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            getPaddingTop() + getPaddingBottom() +
                                    lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        if(getChildCount() == 2) {
            mSlideableView = getChildAt(1);
            mSlideRange = mSlideableView.getMeasuredHeight();
        }else {
            mSlideableView = null;
            mSlideRange = 0;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mFirstLayout) {
            switch (mState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
            settleTo(mSlideOffset, false);
        }

        layoutChildren(l, t, r, b);

        if(mFirstLayout) {
            updateObscuredViewVisibility();
            mFirstLayout = false;
        }

        getLocationOnScreen(mLocationOnScreen);
    }

    @SuppressWarnings("UnusedParameters")
    void layoutChildren(int left, int top, int right, int bottom) {
        final int count = getChildCount();

        final int parentLeft = getPaddingLeft();
        final int parentTop = getPaddingTop();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft = parentLeft + lp.leftMargin;
                int childTop = parentTop + lp.topMargin;

                if (child == mSlideableView)
                    childTop += computePanelTopPosition(mSlideOffset);

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mShadowDrawable != null && mSlideOffset > 0 && mSlideOffset < 1 && mSlideableView != null) {
            final int shadowHeight = mShadowDrawable.getIntrinsicHeight();
            final int left = mSlideableView.getLeft();
            final int top = mSlideableView.getTop() - shadowHeight;
            final int right = mSlideableView.getRight();
            final int bottom = mSlideableView.getTop();
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(canvas);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {

        if (mSlideableView == null || mSlideableView == child)
            return super.drawChild(canvas, child, drawingTime);

        final boolean shouldClip = mClip;
        int save = -1;

        if(shouldClip) {
            save = canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.getClipBounds(mTmpRect);
            mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
            canvas.clipRect(mTmpRect);
        }

        boolean result = super.drawChild(canvas, child, drawingTime);

        if (mContentScrim != null && mScrimAlpha > 0) {
            mContentScrim.mutate().setAlpha(mScrimAlpha);
            mContentScrim.draw(canvas);
        }

        if(shouldClip)
            canvas.restoreToCount(save);

        return result;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        if (mState != State.DRAGGING) {
            ss.state = mState;
        } else {
            ss.state = State.COLLAPSED;
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mState = ss.state != null ? ss.state : State.COLLAPSED;
    }

    private boolean settleTo(float slideOffset, boolean animate) {

        if(animate) {
            if (mSlideableView != null) {
                int targetTop = computePanelTopPosition(slideOffset);
                if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), targetTop)) {
                    setAllChildrenVisible();
                    ViewCompat.postInvalidateOnAnimation(this);
                    return true;
                }
            }
            return false;
        }

        mSlideOffset = slideOffset;

        if (mSlideOffset == 1) {
            if (mState != State.EXPANDED) {
                updateObscuredViewVisibility();
                mState = State.EXPANDED;
                dispatchOnPanelExpanded(mSlideableView);
            }
        } else if (mSlideOffset == 0) {
            if (mState != State.COLLAPSED) {
                mState = State.COLLAPSED;
                dispatchOnPanelCollapsed(mSlideableView);
            }
        } else if (mState != State.ANCHORED) {
            updateObscuredViewVisibility();
            mState = State.ANCHORED;
            dispatchOnPanelAnchored(mSlideableView);
        }

        setScrimAlpha((int) (mSlideOffset*255f));
        invalidate();
        return true;
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        return getMeasuredHeight() - getPaddingBottom() - slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private float computeSlideOffset(int topPosition) {
        final int topBoundCollapsed = computePanelTopPosition(0);
        return (float) (topBoundCollapsed - topPosition) / mSlideRange;
    }

    private void dispatchOnPanelSlide(View panel) {
        for(PanelSlideListener listener: mListeners) listener.onPanelSlide(panel, mSlideOffset);
    }

    private void dispatchOnPanelExpanded(View panel) {
        for(PanelSlideListener listener: mListeners) listener.onPanelExpanded(panel);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnPanelCollapsed(View panel) {
        for(PanelSlideListener listener: mListeners) listener.onPanelCollapsed(panel);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnPanelAnchored(View panel) {
        for(PanelSlideListener listener: mListeners) listener.onPanelAnchored(panel);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private boolean canScrollVertically(View view, int x, int y, int direction) {

        if(view.getVisibility() != View.VISIBLE) {
            return false;
        }

        if(!isViewUnder(view, x, y)) {
            return false;
        }

        if(ViewCompat.canScrollVertically(view, direction)) {
            return true;
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (canScrollVertically(vg.getChildAt(i), x, y, direction)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) return false;
        view.getLocationOnScreen(mTmp);
        int screenX = mLocationOnScreen[0] + x;
        int screenY = mLocationOnScreen[1] + y;
        return screenX >= mTmp[0] && screenX < mTmp[0] + view.getWidth() &&
                screenY >= mTmp[1] && screenY < mTmp[1] + view.getHeight();
    }

    private void updateObscuredViewVisibility() {
        if (getChildCount() == 0) return;

        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left,right, top, bottom;

        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }

        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    private void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }

    private class DragHelperCallbacks extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !mIsUnableToDrag && child == mSlideableView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state != ViewDragHelper.STATE_IDLE) return;

            if(mSlideableView == null)
                mSlideOffset = 0;
            else
                mSlideOffset = computeSlideOffset(mSlideableView.getTop());

            settleTo(mSlideOffset, false);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mState = State.DRAGGING;
            mSlideOffset = computeSlideOffset(top);
            setScrimAlpha((int) (mSlideOffset*255f));
            dispatchOnPanelSlide(mSlideableView);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target;

            // direction is always positive if we are sliding in the expanded direction
            float direction = -yvel;

            if (direction > 0 && mSlideOffset <= mAnchorPoint) {
                // swipe up -> expand and stop at anchor point
                target = computePanelTopPosition(mAnchorPoint);
            } else if (direction > 0 && mSlideOffset > mAnchorPoint) {
                // swipe up past anchor -> expand
                target = computePanelTopPosition(1.0f);
            } else if (direction < 0 && mSlideOffset >= mAnchorPoint) {
                // swipe down -> collapse and stop at anchor point
                target = computePanelTopPosition(mAnchorPoint);
            } else if (direction < 0 && mSlideOffset < mAnchorPoint) {
                // swipe down past anchor -> collapse
                target = computePanelTopPosition(0.0f);
            } else if (mSlideOffset >= (1.f + mAnchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelTopPosition(1.0f);
            } else if (mSlideOffset >= mAnchorPoint / 2) {
                // zero velocity, and close enough to anchor point => go to anchor
                target = computePanelTopPosition(mAnchorPoint);
            } else {
                // settle at the bottom
                target = computePanelTopPosition(0.0f);
            }

            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(1.0f);
            return Math.min(Math.max(top, expandedTop), collapsedTop);
        }
    }

    static class SavedState extends BaseSavedState {
        State state;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            state = (State) in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSerializable(state == null ? null : state.toString());
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }
        public LayoutParams(int width, int height) {
            super(width, height);
        }
        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }
}
