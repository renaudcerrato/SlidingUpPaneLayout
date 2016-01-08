package com.mypopsy.slidinguppanelayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.mypopsy.slidinguppanelayout.demo.R;
public class ScrollViewFragment extends BaseFragment<ScrollView> {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_scrollview, container, false);
    }

    @Override
    protected void populate(ScrollView scrollView) {
        ViewGroup content = ((ViewGroup)scrollView.getChildAt(0));
        for(int i = 0; i < getItemCount(); i++) {
            final int position = i;
            View view = bindItemView(createItemView(content), i);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(position);
                }
            });
            content.addView(view, i);
        }
    }
}
