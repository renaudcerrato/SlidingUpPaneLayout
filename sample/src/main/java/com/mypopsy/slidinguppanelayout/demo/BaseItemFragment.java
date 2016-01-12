package com.mypopsy.slidinguppanelayout.demo;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mypopsy.slidinguppanelayout.extra.SlidingUpFragment;

import butterknife.Bind;
import butterknife.ButterKnife;

abstract public class BaseItemFragment<T extends ViewGroup> extends SlidingUpFragment {

    private static final int DEFAULT_ITEM_COUNT = 42;

    public interface OnItemClickListener {
        void onItemClick(ViewGroup parent, View view, int position);
    }

    @Bind(android.R.id.list)
    protected T mScrollableView;

    private LayoutInflater mInflater;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mInflater = LayoutInflater.from(view.getContext());
        populate(mScrollableView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getActivity(), getClass().getSimpleName() + " destroyed", Toast.LENGTH_SHORT).show();
    }

    protected abstract void populate(T scrollableView);

    protected View createItemView(ViewGroup parent) {
        return mInflater.inflate(R.layout.listitem, parent, false);
    }

    protected int getItemCount() {
        return DEFAULT_ITEM_COUNT;
    }

    protected View bindItemView(View view, int position) {
        int color = MaterialColorGenerator.MATERIAL.getColor(position);
        ColorDrawable background = new ColorDrawable(color);
        view.setBackgroundDrawable(background);
        ((TextView) view.findViewById(android.R.id.text1)).setText(String.format("#%06X", color & 0xffffff));
        return view;
    }

    protected void onItemClick(View view, int position) {
        if(getActivity() instanceof OnItemClickListener)
            ((OnItemClickListener) getActivity()).onItemClick(mScrollableView, view, position);
    }
}
