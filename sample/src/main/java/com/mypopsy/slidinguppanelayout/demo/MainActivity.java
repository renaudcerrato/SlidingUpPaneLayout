package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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

//import com.mypopsy.slidinguppanelayout.demo.BuildConfig;
public class MainActivity extends AppCompatActivity {

    @Bind(R.id.root)
    SlidingUpPaneLayout mSlidingUpPaneLayout;

    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    @Bind(R.id.tabs)
    TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
    }

    public void onFabClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL)));
    }

    public void onExpand(View v) {
        mSlidingUpPaneLayout.setState(EXPANDED);
    }

    public void onAnchor(View v) {
        mSlidingUpPaneLayout.setState(ANCHORED);
    }

    public void onCollapse(View v) {
        mSlidingUpPaneLayout.setState(COLLAPSED);
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
