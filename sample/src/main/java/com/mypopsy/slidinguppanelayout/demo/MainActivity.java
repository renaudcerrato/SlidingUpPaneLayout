package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

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

    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    @Bind(R.id.tabs)
    TabLayout mTabLayout;

    @Bind(R.id.fab)
    FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
                onTabReselected(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (mSlidingUpPaneLayout.isCollapsed()) mSlidingUpPaneLayout.setState(ANCHORED);
            }
        });

        mSlidingUpPaneLayout.addPaneListener(new SlidingUpPaneLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset, int slidePixels) {
                float visibleOffset = mSlidingUpPaneLayout.getVisibleOffset();
                float y =  -(slidePixels + visibleOffset);
                if(slideOffset <= 0) ViewCompat.setTranslationY(mFab, y);
            }

            @Override
            public void onPanelHidden(View panel) {
                mFab.animate().translationY(0).start();
            }
        });
    }

    public void onFabClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL)));
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

    private class FragmentAdapter extends FragmentStatePagerAdapter {

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: return new RecyclerViewFragment();
                case 1: return new ListViewFragment();
                case 2: return new ScrollViewFragment();
            }
            throw new IllegalStateException();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: return "RecyclerView";
                case 1: return "ListView";
                case 2: return "ScrollView ";
            }
            throw new IllegalStateException();
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
