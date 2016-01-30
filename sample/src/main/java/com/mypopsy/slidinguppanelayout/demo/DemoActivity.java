package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class DemoActivity extends BaseActivity {

    private static final String GITHUB_PAGE = "https://github.com/renaudcerrato";

    private WebViewFragment mWebViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        mWebViewFragment = (WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mWebViewFragment.loadUrl(GITHUB_PAGE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_extras:
                startActivity(new Intent(this, DemoFragmentActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
