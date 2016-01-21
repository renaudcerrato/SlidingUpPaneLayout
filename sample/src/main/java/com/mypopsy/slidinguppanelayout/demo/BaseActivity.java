package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.mypopsy.widget.SlidingUpPaneLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.ANCHORED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.COLLAPSED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.HIDDEN;

public class BaseActivity extends AppCompatActivity implements SlidingUpPaneLayout.PanelSlideListener {

    @Bind(R.id.root)
    SlidingUpPaneLayout mSlidingUpPaneLayout;

    @Bind(R.id.fab)
    View mFab;

    @Bind(R.id.button_collapse)
    View mCollapseButton;

    @Bind(R.id.button_switch)
    CompoundButton mCollapsible;

    @Nullable private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);

        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        mCollapsible.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateVisibleHeight();
            }
        });

        mSlidingUpPaneLayout.addPaneListener(this);
    }

    @CallSuper
    @OnClick(R.id.button_expand)
    public void onExpandClick(View v) {
        mSlidingUpPaneLayout.setState(EXPANDED);
    }

    @CallSuper
    @OnClick(R.id.button_anchor)
    public void onAnchorClick(View v) {
        mSlidingUpPaneLayout.setState(ANCHORED);
    }

    @CallSuper
    @OnClick(R.id.button_collapse)
    public void onCollapseClick(View v) {
        mSlidingUpPaneLayout.setState(COLLAPSED);
    }

    @CallSuper
    @OnClick(R.id.button_hide)
    public void onHideClick(View v) {
        mSlidingUpPaneLayout.setState(HIDDEN);
    }

    @Override
    public void onBackPressed() {
        if(mSlidingUpPaneLayout.isVisible())
            mSlidingUpPaneLayout.setState(HIDDEN);
        else
            super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateFabPosition();
    }

    @Override
    @CallSuper
    public void onPanelSlide(View panel, float slideOffset, int slidePixels) {
        updateFabPosition();
    }

    @Override
    @CallSuper
    public void onPanelCollapsed(View panel) {
        ViewCompat.animate(mFab).translationY(-mSlidingUpPaneLayout.getVisibleHeight()).start();
    }

    @Override
    @CallSuper
    public void onPanelExpanded(View panel) {

    }

    @Override
    @CallSuper
    public void onPanelAnchored(View panel) {

    }

    @Override
    @CallSuper
    public void onPanelHidden(View panel) {
        ViewCompat.animate(mFab).translationY(0).start();
    }

    final public void onFabClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL)));
    }

    private void updateVisibleHeight() {
        boolean collapsible = mCollapsible.isChecked();
        mCollapseButton.setVisibility(collapsible ? View.VISIBLE : View.GONE);

        TypedArray a = getTheme().obtainStyledAttributes(R.style.AppTheme, new int[]{R.attr.actionBarSize});
        int actionBarSize = a.getDimensionPixelSize(0, 0);
        mSlidingUpPaneLayout.setVisibleHeight(collapsible ? actionBarSize : 0);

        updateFabPosition();
    }

    private void updateFabPosition() {
        int visibleOffset = mSlidingUpPaneLayout.getVisibleHeight();
        int slidePixels = mSlidingUpPaneLayout.getSlidePixels();
        float y = visibleOffset + slidePixels;
        if (slidePixels > 0) y = Math.min(visibleOffset, y);
        ViewCompat.setTranslationY(mFab, -y);
    }

    final protected void toast(String text) {
        if(mToast == null)
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        else{
            mToast.setText(text);
        }
        mToast.show();
    }
}
