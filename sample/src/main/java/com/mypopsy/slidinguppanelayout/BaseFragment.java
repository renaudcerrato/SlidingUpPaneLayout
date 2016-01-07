package com.mypopsy.slidinguppanelayout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

abstract public class BaseFragment<T extends ViewGroup> extends Fragment {

    private static final int DEFAULT_ITEM_COUNT = 25;

    @Bind(android.R.id.list)
    protected T mScrollableView;

    private LayoutInflater mInfltater;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mInfltater = LayoutInflater.from(view.getContext());
        populate(mScrollableView);
    }

    protected abstract void populate(T scrollableView);

    protected View createItemView(ViewGroup parent) {
        return mInfltater.inflate(R.layout.listitem, parent, false);
    }

    protected View bindItemView(View view, int position) {
        ((TextView)view.findViewById(android.R.id.text1)).setText(String.valueOf(position));
        view.setBackgroundColor(MaterialColorGenerator.MATERIAL.getColor(position));
        return view;
    }

    protected int getItemCount() {
        return DEFAULT_ITEM_COUNT;
    }
}
