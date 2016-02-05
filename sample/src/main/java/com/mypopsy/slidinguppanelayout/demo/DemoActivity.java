package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.mypopsy.slidinguppanelayout.demo.ui.GalleryFragment;
import com.mypopsy.slidinguppanelayout.demo.ui.ListViewFragment;
import com.mypopsy.slidinguppanelayout.demo.ui.WebViewFragment;

import butterknife.Bind;

public class DemoActivity extends BaseActivity {

    private static final String GITHUB_PAGE = "https://github.com/renaudcerrato";

    @Bind(R.id.tablayout) TabLayout mTabLayout;
    @Bind(R.id.viewpager) ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mViewPager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setOffscreenPageLimit(3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_extras:
                startActivity(new Intent(this, AdvancedFragmentDemoActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class PageAdapter extends FragmentPagerAdapter {

        public PageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: return WebViewFragment.newInstance(GITHUB_PAGE);
                case 1: return new GalleryFragment();
                case 2: return new ListViewFragment();
            }
            throw new IllegalStateException();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "WebView";
                case 1: return "RecyclerView";
                case 2: return "ListView";
            }
            throw new IllegalStateException();
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
