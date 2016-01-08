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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.mypopsy.slidinguppanelayout.R;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by renaud on 07/01/16.
 */
public class SlidingUpPaneLayout extends ViewGroup {

    private static final String TAG = SlidingUpPaneLayout.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Retention(SOURCE)
    @IntDef({IntState.EXPANDED, IntState.ANCHORED, IntState.COLLAPSED, IntState.HIDDEN})
    private @interface IntState {
        int EXPANDED = 0;
        int ANCHORED = 1;
        int COLLAPSED = 2;
        int HIDDEN = 3;
    }

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    private static final float DEFAULT_DRAG_SENSITIVITY = 1f;
    private static final float DEFAULT_ANCHOR_POINT = 1f;

    /**
     * Current state of the bottom panel.
     */
    public enum State {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        DRAGGING,
        HIDDEN,
    }

    /**
     * Listener for monitoring events about the sliding panel.
     */
    public interface PanelSlideListener {
        void onPanelSlide(View panel, float slideOffset, int slidePixels);
        void onPanelCollapsed(View panel);
        void onPanelExpanded(View panel);
        void onPanelAnchored(View panel);
        void onPanelHidden(View panel);
    }

    static public class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset, int slidePixels) {}
        @Override
        public void onPanelCollapsed(View panel) {}
        @Override
        public void onPanelExpanded(View panel) {}
        @Override
        public void onPanelAnchored(View panel) {}
        @Override
        public void onPanelHidden(View panel) {}
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
     * How far the panel is visible when collapsed.
     */
    private int mVisibleOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    private int mSlideRange;

    /**
     * Anchor point.
     * range [0, 1] where 0 = closed, 1 = open.
     */
    private float mAnchorPoint = 0;

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

    @SuppressWarnings("ResourceType")
    private void applyXmlAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SlidingUpPaneLayout, defStyleAttr, 0);
        setAnchorPoint(a.getFloat(R.styleable.SlidingUpPaneLayout_supl_anchor, DEFAULT_ANCHOR_POINT));
        setContentScrim(a.getDrawable(R.styleable.SlidingUpPaneLayout_supl_contentScrim));
        setShadowDrawable(a.getDrawable(R.styleable.SlidingUpPaneLayout_supl_shadow));
        setState(a.getInt(R.styleable.SlidingUpPaneLayout_supl_initialState, IntState.COLLAPSED), false);
        mVisibleOffset = a.getDimensionPixelSize(R.styleable.SlidingUpPaneLayout_supl_visibleOffset, 0);
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

    public float getVisibleOffset() {
        return mVisibleOffset;
    }

    public void setVisibleOffset(int visibleOffset) {
        if(mVisibleOffset != visibleOffset) {
            mVisibleOffset = visibleOffset;
            requestLayout();
        }
    }

    public boolean isExpanded() {
        return mState == State.EXPANDED;
    }

    public boolean isVisible() {
        return !isHidden();
    }

    public boolean isHidden() {
        return mState == State.HIDDEN || (mState == State.COLLAPSED && mVisibleOffset <= 0);
    }

    public boolean isCollapsed() {
        return mState == State.COLLAPSED || (mState == State.HIDDEN && mVisibleOffset <= 0);
    }

    public boolean isAnchored() {
        return mState == State.ANCHORED;
    }

    public void setState(@NonNull State state) {
        setState(state, true);
    }

    private void setState(@IntState int state, boolean animate) {
        State s;
        switch (state) {
            case IntState.EXPANDED: s = State.EXPANDED; break;
            case IntState.ANCHORED: s = State.ANCHORED; break;
            case IntState.COLLAPSED: s = State.COLLAPSED; break;
            case IntState.HIDDEN: s = State.HIDDEN; break;
            default: throw new IllegalArgumentException("unknown state "+state);
        }
        setState(s, animate);
    }

    @SuppressWarnings("ConstantConditions")
    public void setState(@NonNull State state, boolean animate) {
        if(DEBUG) Log.d(TAG, "-----setState("+state+",animate="+animate+")");

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
                case HIDDEN:
                    int newTop = computePanelTopPosition(0) + mVisibleOffset;
                    settleTo(computeSlideOffset(newTop), animate);
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

    private void updateScrimAlpha() {
        int alpha = (int)(mSlideOffset * 255f + .5f);
        if (alpha != mScrimAlpha) {
            mScrimAlpha = Math.min(255, Math.max(0, alpha));
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if(getChildCount() == 2) mFirstLayout = true;
    }

    @Nullable
    public View getSlidingPanel() {
        return mSlideableView;
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
                if (isVisible()) {
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
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        if(DEBUG) Log.d(TAG, "-----onMeasure()");

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

                if(DEBUG) {
                    final String w = MeasureSpec.toString(childWidthMeasureSpec);
                    final String h = MeasureSpec.toString(childHeightMeasureSpec);
                    Log.d(TAG, "child.measure("+i+", "+w+","+h+")");
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        if(getChildCount() == 2) {
            mSlideableView = getChildAt(1);
            mSlideRange = mSlideableView.getMeasuredHeight() - mVisibleOffset;
            // if the sliding panel is not visible, then force hidden state
            if (mSlideableView.getVisibility() != VISIBLE) {
                mState = State.HIDDEN;
            }
        }else {
            mSlideableView = null;
            mSlideRange = 0;
        }

        if(DEBUG) Log.d(TAG, "mSlideRange="+mSlideRange);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(DEBUG) Log.d(TAG, "-----onLayout()");

        if (mFirstLayout) {
            switch (mState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    int newTop = computePanelTopPosition(0) + mVisibleOffset;
                    mSlideOffset = computeSlideOffset(newTop);
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        layoutChildren(l, t, r, b);

        if(mFirstLayout) {
            updateObscuredViewVisibility();
            updateScrimAlpha();
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

                if (child == mSlideableView) {
                    childTop += computePanelTopPosition(mSlideOffset);
                }

                if(DEBUG) Log.d(TAG, "layoutChildren("+i+",["+childLeft+","+childTop+":"+width+"x"+height+")");
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
        if(mSlideableView != null) mSlideableView.postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mShadowDrawable != null && mSlideableView != null) {

            final int shadowHeight = mShadowDrawable.getIntrinsicHeight();
            final int top = mSlideableView.getTop() - shadowHeight;
            final int bottom = mSlideableView.getTop();

            if(bottom < getTop() || mSlideableView.getTop() < getBottom()) {
                final int left = mSlideableView.getLeft();
                final int right = mSlideableView.getRight();
                mShadowDrawable.setBounds(left, top, right, bottom);
                mShadowDrawable.draw(canvas);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if(DEBUG) Log.d(TAG, "-----drawChild("+(child == mSlideableView ? "panel":"main")+")");

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
        ss.visibleOffset = mVisibleOffset;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mState = ss.state != null ? ss.state : State.COLLAPSED;
        mVisibleOffset = ss.visibleOffset;
    }

    private boolean settleTo(float slideOffset, boolean animate) {
        if(DEBUG) Log.d(TAG, "-----settleTo("+slideOffset+", animate="+animate+")");

        if (mSlideableView != null) {
            mSlideableView.setVisibility(View.VISIBLE);
        }

        if(animate) {
            if (mSlideableView != null) {
                int targetTop = computePanelTopPosition(slideOffset);
                if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), targetTop)) {
                    setAllChildrenVisible();
                    ViewCompat.postInvalidateOnAnimation(this);
                    return true;
                }
            }
        }

        mSlideOffset = slideOffset;

        if (mSlideOffset == 1) {
            if (mState != State.EXPANDED) {
                updateObscuredViewVisibility();
                mState = State.EXPANDED;
                dispatchOnPanelExpanded();
            }
        }else if (mSlideOffset < 0 || (mSlideOffset == 0 && mVisibleOffset <= 0)) {
            mState = State.HIDDEN;
            if(mSlideableView != null) mSlideableView.setVisibility(View.INVISIBLE);
            dispatchOnPanelHidden();
        }else if (mSlideOffset == 0) {
            if (mState != State.COLLAPSED) {
                mState = State.COLLAPSED;
                dispatchOnPanelCollapsed();
            }
        }else if (mState != State.ANCHORED) {
            updateObscuredViewVisibility();
            mState = State.ANCHORED;
            dispatchOnPanelAnchored();
        }

        updateScrimAlpha();
        invalidate();
        return true;
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        return getMeasuredHeight() - getPaddingBottom() - mVisibleOffset - slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private float computeSlideOffset(int topPosition) {
        final int topBoundCollapsed = computePanelTopPosition(0);
        return (float) (topBoundCollapsed - topPosition) / mSlideRange;
    }

    private void dispatchOnPanelSlide() {
        if(mSlideableView == null) return;
        if(DEBUG) Log.d(TAG, "dispatchOnPanelSlide("+mSlideOffset+")");
        for(PanelSlideListener listener: mListeners)
            listener.onPanelSlide(mSlideableView, mSlideOffset, (int) (mSlideOffset*mSlideRange + .5f));
    }

    private void dispatchOnPanelExpanded() {
        if(mSlideableView == null) return;
        if(DEBUG) Log.d(TAG, "dispatchOnPanelExpanded()");
        for(PanelSlideListener listener: mListeners) listener.onPanelExpanded(mSlideableView);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnPanelCollapsed() {
        if(mSlideableView == null) return;
        if(DEBUG) Log.d(TAG, "dispatchOnPanelCollapsed()");
        for(PanelSlideListener listener: mListeners) listener.onPanelCollapsed(mSlideableView);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnPanelAnchored() {
        if(mSlideableView == null) return;
        if(DEBUG) Log.d(TAG, "dispatchOnPanelAnchored()");
        for(PanelSlideListener listener: mListeners) listener.onPanelAnchored(mSlideableView);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnPanelHidden() {
        if(mSlideableView == null) return;
        if(DEBUG) Log.d(TAG, "dispatchOnPanelHidden()");
        for(PanelSlideListener listener: mListeners) listener.onPanelHidden(mSlideableView);
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

        if(DEBUG) Log.d(TAG, "-----updateObscuredViewVisibility("+(child==mSlideableView ? "panel":"main")+
                ","+(vis == VISIBLE ? "VISIBLE" : "INVISIBLE")+")");

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
            updateScrimAlpha();
            dispatchOnPanelSlide();
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
            final int collapsedTop = computePanelTopPosition(0);
            final int expandedTop = computePanelTopPosition(1);
            return Math.min(Math.max(top, expandedTop), collapsedTop);
        }
    }

    static class SavedState extends BaseSavedState {
        State state;
        int visibleOffset;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            state = (State) in.readSerializable();
            visibleOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSerializable(state == null ? null : state.toString());
            out.writeInt(visibleOffset);
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
