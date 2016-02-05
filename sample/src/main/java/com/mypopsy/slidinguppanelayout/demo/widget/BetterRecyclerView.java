package com.mypopsy.slidinguppanelayout.demo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.mypopsy.slidinguppanelayout.demo.R;

public class BetterRecyclerView extends RecyclerView {

    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;
    private RecyclerViewDragDropManager mRecyclerViewDragDropManager;
    private RecyclerView.Adapter mAdapter;
    private int mColumWidth;


    public BetterRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public BetterRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BetterRecyclerView, defStyle, 0);
        int itemMargin = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_item_margin, 0);
        int itemMarginLeft = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_item_marginLeft, itemMargin);
        int itemMarginTop = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_item_marginTop, itemMargin);
        int itemMarginRight = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_item_marginRight, itemMargin);
        int itemMarginBottom = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_item_marginBottom, itemMargin);
        mColumWidth = a.getDimensionPixelSize(R.styleable.BetterRecyclerView_android_columnWidth, -1);
        setHasFixedSize(a.getBoolean(R.styleable.BetterRecyclerView_hasFixedSize, false));
        a.recycle();

        if(itemMarginLeft+itemMarginTop+itemMarginRight+itemMarginBottom != 0)
            addItemDecoration(new PaddingItemDecoration(itemMarginLeft,itemMarginTop,itemMarginRight,itemMarginBottom));
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if(mColumWidth > 0 && layout instanceof GridLayoutManager)
            ((GridLayoutManager) layout).setSpanCount(1);
        super.setLayoutManager(layout);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (mColumWidth > 0) {
            LayoutManager lm = getLayoutManager();
            if(lm instanceof GridLayoutManager)
            {
                int spanCount = Math.max(1, getMeasuredWidth() / mColumWidth);
                ((GridLayoutManager)lm).setSpanCount(spanCount);
            }
        }
    }

    public void destroy() {
        if(mRecyclerViewTouchActionGuardManager != null)
            mRecyclerViewTouchActionGuardManager.release();
        if(mRecyclerViewDragDropManager != null)
            mRecyclerViewDragDropManager.release();
        mRecyclerViewTouchActionGuardManager = null;
        mRecyclerViewDragDropManager = null;
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        destroy();

        if(adapter instanceof DraggableItemAdapter) {
            // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
            mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
            mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
            mRecyclerViewTouchActionGuardManager.setEnabled(true);

            // drag & drop manager
            mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
            mRecyclerViewDragDropManager.setInitiateOnLongPress(true);
            mRecyclerViewDragDropManager.setInitiateOnMove(false);

            super.setAdapter(mRecyclerViewDragDropManager.createWrappedAdapter(mAdapter = adapter));

            mRecyclerViewTouchActionGuardManager.attachRecyclerView(this);
            mRecyclerViewDragDropManager.attachRecyclerView(this);
        }else
            super.setAdapter(mAdapter = adapter);
    }

    public RecyclerView.Adapter getWrappedAdapter() {
        return mAdapter;
    }

    public static abstract class DragAndDropAdapter<VH extends DragAndDropViewHolder> extends RecyclerView.Adapter<VH>
            implements DraggableItemAdapter<VH> {

    }

    public static class DragAndDropViewHolder extends RecyclerView.ViewHolder implements DraggableItemViewHolder {

        @DraggableItemStateFlags
        private int flags;

        public DragAndDropViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setDragStateFlags(int flags) {
            this.flags = flags;
        }

        @Override
        public int getDragStateFlags() {
            return flags;
        }
    }

    private class PaddingItemDecoration extends RecyclerView.ItemDecoration {

        private final int mLeft, mTop, mRight, mBottom;

        public PaddingItemDecoration(int left, int top, int right, int bottom) {
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(mLeft, mTop, mRight, mBottom);
        }
    }
}
