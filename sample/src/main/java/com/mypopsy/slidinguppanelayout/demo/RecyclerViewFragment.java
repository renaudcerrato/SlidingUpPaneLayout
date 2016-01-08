package com.mypopsy.slidinguppanelayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RecyclerViewFragment extends BaseFragment<RecyclerView> {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_recyclerview, container, false);
    }

    @Override
    protected void populate(RecyclerView recyclerView) {
        recyclerView.setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<DummyViewHolder> {

        @Override
        public DummyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DummyViewHolder(createItemView(parent));
        }

        @Override
        public void onBindViewHolder(DummyViewHolder holder, int position) {
            bindItemView(holder.itemView, position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return RecyclerViewFragment.this.getItemCount();
        }
    }

    class DummyViewHolder extends RecyclerView.ViewHolder {
        public DummyViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onItemClick(getAdapterPosition());
                }
            });
        }
    }
}
