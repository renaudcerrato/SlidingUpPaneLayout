package com.mypopsy.slidinguppanelayout.demo;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.mypopsy.slidinguppanelayout.extra.SlidingUpFragment;
import com.mypopsy.widget.SlidingUpPaneLayout.State;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.ANCHORED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.COLLAPSED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.HIDDEN;

public class DemoFragmentActivity extends BaseActivity implements AdapterFragment.OnItemClickListener {

    private Spinner mSpinner;
    private int mLastSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragments);

        if(savedInstanceState != null)
            mLastSelection = savedInstanceState.getInt("selection");

        ActionBar actionbar = getSupportActionBar();
        if(actionbar == null) throw new IllegalStateException();

        actionbar.setCustomView(mSpinner = new AppCompatSpinner(actionbar.getThemedContext()));
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayShowCustomEnabled(true);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mLastSelection) mSlidingUpPaneLayout.setState(HIDDEN);
                mLastSelection = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ensureFragment(savedInstanceState == null ? HIDDEN : mSlidingUpPaneLayout.getState());
    }

    public void addFragment(final SlidingUpFragment fragment, State state) {
        final FragmentManager fm = getSupportFragmentManager();
        if (mSlidingUpPaneLayout.getSlidingPanel() != null) mSlidingUpPaneLayout.removeViewAt(1);
        fragment.show(fm, mSlidingUpPaneLayout.getId(), state);
    }

    @Override
    public void onExpandClick(View v) {
        ensureFragment(EXPANDED);
        super.onExpandClick(v);
    }

    @Override
    public void onAnchorClick(View v) {
        ensureFragment(ANCHORED);
        super.onAnchorClick(v);
    }

    @Override
    public void onCollapseClick(View v) {
        ensureFragment(COLLAPSED);
        super.onCollapseClick(v);
    }

    private void ensureFragment(State state) {
        if(mSlidingUpPaneLayout.getSlidingPanel() != null) return;

        switch (mSpinner.getSelectedItemPosition()) {
            case 0:
                addFragment(new RecyclerViewFragment(), state);
                break;
            case 1:
                addFragment(new ListViewFragment(), state);
                break;
            case 2:
                addFragment(new ScrollViewFragment(), state);
                break;
        }
    }

    @Override
    public void onItemClick(final ViewGroup parent, final View view, final int position) {
        scrollTo(parent, view);
        setBackground(view.getBackground());
        mSlidingUpPaneLayout.setState(COLLAPSED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selection", mLastSelection);
    }

    private void scrollTo(ViewGroup parent, View view) {
        if(parent instanceof ScrollView)
            ((ScrollView) parent).smoothScrollTo(0, view.getTop());
        else if(parent instanceof RecyclerView)
            ((RecyclerView) parent).smoothScrollBy(0, view.getTop());
        else
            parent.scrollBy(0, view.getTop());
    }

    private void setBackground(Drawable drawable) {
        findViewById(android.R.id.content).setBackgroundDrawable(drawable);
    }
}
