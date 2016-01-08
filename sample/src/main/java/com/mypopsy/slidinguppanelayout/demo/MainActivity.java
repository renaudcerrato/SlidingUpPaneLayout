package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mypopsy.slidinguppanelayout.extra.SlidingUpFragment;
import com.mypopsy.widget.SlidingUpPaneLayout;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.ANCHORED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.COLLAPSED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.HIDDEN;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.root)
    SlidingUpPaneLayout mSlidingUpPaneLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void onFabClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL)));
    }

    public void onAddFragment(View v) {
        SlidingUpFragment fragment = new ListViewFragment();
        fragment.show(getSupportFragmentManager().beginTransaction().addToBackStack(null), mSlidingUpPaneLayout.getId());
    }

    public void onExpandClick(View v) {
        mSlidingUpPaneLayout.setState(EXPANDED);
    }

    public void onAnchorClick(View v) {
        mSlidingUpPaneLayout.setState(ANCHORED);
    }

    public void onCollapseClick(View v) {
        mSlidingUpPaneLayout.setState(COLLAPSED);
    }

    public void onHideClick(View v) {
        mSlidingUpPaneLayout.setState(HIDDEN);
    }
}
