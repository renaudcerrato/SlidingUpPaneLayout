package com.mypopsy.slidinguppanelayout.demo.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mypopsy.slidinguppanelayout.demo.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class WebViewFragment extends Fragment {

    private  static final String ARG_URL = "url";

    @Bind(R.id.webview) WebView mWebView;
    @Bind(R.id.toolbar) Toolbar mToolbar;

    private String mUrl;

    public static WebViewFragment newInstance(String url) {
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = savedInstanceState;
        if(args == null) args = getArguments();
        if (args != null && args.containsKey(ARG_URL))
            loadUrl(args.getString(ARG_URL));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_URL, mWebView.getUrl());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(mUrl);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mToolbar.setTitle("Loading...");
                mToolbar.setSubtitle(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mToolbar.setTitle(view.getTitle());
            }

        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onReceivedTitle(WebView view, String title) {
                mToolbar.setTitle(view.getTitle());
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {

                if (icon != null) {
                    BitmapDrawable d = new BitmapDrawable(icon);
                    d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    d.setTargetDensity(3 * getResources().getDisplayMetrics().densityDpi);
                    mToolbar.setNavigationIcon(d);
                } else
                    mToolbar.setNavigationIcon(null);
            }
        });

        mWebView.loadUrl(mUrl);
    }

    public void loadUrl(String url) {
        mUrl = url;
        if(mWebView != null) mWebView.loadUrl(url);
    }
}
