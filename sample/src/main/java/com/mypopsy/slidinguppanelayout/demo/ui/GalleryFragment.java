package com.mypopsy.slidinguppanelayout.demo.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.mypopsy.slidinguppanelayout.demo.R;
import com.mypopsy.slidinguppanelayout.demo.utils.LoremPixel;
import com.mypopsy.slidinguppanelayout.demo.utils.MaterialColorGenerator;
import com.mypopsy.slidinguppanelayout.demo.widget.BetterRecyclerView;

import java.util.ArrayList;
import java.util.Collection;

import butterknife.Bind;
import butterknife.ButterKnife;

public class GalleryFragment extends Fragment {

    private static final int COUNT = 120;
    private static final int DIMENSION = 300;

    @Bind(android.R.id.list) RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mRecyclerView.setAdapter(new GalleryAdapter(LoremPixel.random(COUNT, DIMENSION, DIMENSION)));
    }

    private class GalleryAdapter extends BetterRecyclerView.DragAndDropAdapter<ImageViewHolder> {

        private ArrayList<String> mItems = new ArrayList<>();

        GalleryAdapter(Collection<String> url) {
            mItems.addAll(url);
            setHasStableIds(true);
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ImageViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).hashCode();
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.listitem_image;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public boolean onCheckCanStartDrag(ImageViewHolder holder, int position, int x, int y) {
            return true;
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(ImageViewHolder holder, int position) {
            return null;
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            mItems.set(toPosition, mItems.remove(fromPosition));
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    class ImageViewHolder extends BetterRecyclerView.DragAndDropViewHolder {

        @Bind(android.R.id.icon) ImageView image;

        private final ColorDrawable placeholder;

        public ImageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            placeholder = new ColorDrawable();
        }

        void bind(String url) {
            int color = MaterialColorGenerator.getInstance().getColor(url);
            placeholder.setColor(color);
            Glide.with(GalleryFragment.this).load(url).placeholder(placeholder).into(image);
        }
    }
}
