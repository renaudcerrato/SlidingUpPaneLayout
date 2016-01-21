package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DemoActivity extends BaseActivity {

    private static final String GITHUB_PAGE = "https://github.com/renaudcerrato";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        initCustomView(mSlidingUpPaneLayout.getChildAt(1));
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
                startActivity(new Intent(this, DemoFragmentActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initCustomView(View view) {

        final WebView webView = (WebView) view.findViewById(R.id.webview);
        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(GITHUB_PAGE);
            }
        });

        webView.loadUrl(GITHUB_PAGE);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                toolbar.setTitle("Loading...");
                toolbar.setSubtitle(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                toolbar.setTitle(view.getTitle());
            }

        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onReceivedTitle(WebView view, String title) {
                toolbar.setTitle(view.getTitle());
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {

                if (icon != null) {
                    BitmapDrawable d = new BitmapDrawable(icon);
                    d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    d.setTargetDensity(3*getResources().getDisplayMetrics().densityDpi);
                    toolbar.setNavigationIcon(d);
                } else
                    toolbar.setNavigationIcon(null);
            }
        });
    }
}
