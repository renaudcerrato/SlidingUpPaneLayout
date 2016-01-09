package com.mypopsy.slidinguppanelayout.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.mypopsy.slidinguppanelayout.extra.SlidingUpFragment;
import com.mypopsy.widget.SlidingUpPaneLayout;
import com.mypopsy.widget.SlidingUpPaneLayout.State;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.ANCHORED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.COLLAPSED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.HIDDEN;

public class MainActivity extends AppCompatActivity implements BaseItemFragment.OnItemClickListener, SlidingUpPaneLayout.PanelSlideListener {

    private static final String GITHUB_PAGE = "https://github.com/renaudcerrato";

    @Bind(R.id.root)
    SlidingUpPaneLayout mSlidingUpPaneLayout;

    @Bind(R.id.fab)
    View mFab;

    @Bind(R.id.spinner)
    Spinner mSpinner;

    private Toast mToast;
    private View mCustomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        mSlidingUpPaneLayout.addPaneListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSlidingUpPaneLayout.setState(HIDDEN);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() == 0)
                    toast("fragment destroyed");
            }
        });
    }

    private void onSelection(int position, State state) {
        switch (position) {
            case 0:
                addView(getCustomView());
                break;
            case 1:
                addFragment(new RecyclerViewFragment(), state);
                break;
            case 2:
                addFragment(new ListViewFragment(), state);
                break;
            case 3:
                addFragment(new ScrollViewFragment(), state);
                break;
        }
    }

    public void addView(View view) {
        if (mSlidingUpPaneLayout.getSlidingPanel() != null) return;
        mSlidingUpPaneLayout.addView(view);
    }

    public void addFragment(final SlidingUpFragment fragment, State state) {
        final FragmentManager fm = getSupportFragmentManager();
        if (mSlidingUpPaneLayout.getSlidingPanel() != null) mSlidingUpPaneLayout.removeViewAt(1);
        FragmentTransaction transaction = fm.beginTransaction().addToBackStack(null);
        fragment.show(transaction, mSlidingUpPaneLayout.getId(), state);
    }

    public void onExpandClick(View v) {
        ensureSlideView(EXPANDED);
        mSlidingUpPaneLayout.setState(EXPANDED);
    }

    public void onAnchorClick(View v) {
        ensureSlideView(ANCHORED);
        mSlidingUpPaneLayout.setState(ANCHORED);
    }

    public void onCollapseClick(View v) {
        ensureSlideView(COLLAPSED);
        mSlidingUpPaneLayout.setState(COLLAPSED);
    }

    public void onHideClick(View v) {
        mSlidingUpPaneLayout.setState(HIDDEN);
    }

    private void ensureSlideView(State state) {
        if(mSlidingUpPaneLayout.getSlidingPanel() == null) onSelection(mSpinner.getSelectedItemPosition(), state);
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset, int slidePixels) {
        int visibleOffset = mSlidingUpPaneLayout.getVisibleHeight();
        float y = visibleOffset + slidePixels;
        if (slideOffset > 0) y = Math.min(visibleOffset, y);
        ViewCompat.setTranslationY(mFab, -y);
    }

    @Override
    public void onPanelCollapsed(View panel) {
        ViewCompat.animate(mFab).translationY(-mSlidingUpPaneLayout.getVisibleHeight()).start();
    }

    @Override
    public void onPanelExpanded(View panel) {

    }

    @Override
    public void onPanelAnchored(View panel) {

    }

    @Override
    public void onPanelHidden(View panel) {
        if(mSlidingUpPaneLayout.getSlidingPanel() == mCustomView) {
            mSlidingUpPaneLayout.removeView(mCustomView);
        }
        ViewCompat.animate(mFab).translationY(0).start();
    }

    @Override
    public void onItemClick(final ViewGroup parent, final View view, final int position) {
        scrollTo(parent, view);
        setBackground(view.getBackground());
        mSlidingUpPaneLayout.setState(COLLAPSED);
        toast("clicked " + position);
    }

    private void toast(String text) {
        if(mToast == null)
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        else{
            mToast.setText(text);
        }
        mToast.show();
    }

    private void scrollTo(ViewGroup parent, View view) {
        if(parent instanceof ScrollView)
            ((ScrollView) parent).smoothScrollTo(0, view.getTop());
        else if(parent instanceof RecyclerView)
            ((RecyclerView) parent).smoothScrollBy(0, view.getTop());
        else
            parent.scrollBy(0, view.getTop());
    }

    public void onFabClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PROJECT_URL)));
    }

    private void setBackground(Drawable drawable) {
        findViewById(android.R.id.content).setBackgroundDrawable(drawable);
    }

    private View getCustomView() {
        if(mCustomView != null) return mCustomView;

        mCustomView = LayoutInflater.from(mSlidingUpPaneLayout.getContext()).inflate(R.layout.customview, mSlidingUpPaneLayout, false);
        final WebView webView = (WebView) mCustomView.findViewById(R.id.webview);
        final Toolbar toolbar = (Toolbar) mCustomView.findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(GITHUB_PAGE);
            }
        });

        webView.loadUrl(GITHUB_PAGE);

        webView.setWebViewClient(new WebViewClient() {
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
                    d.setTargetDensity(3*getResources().getDisplayMetrics().densityDpi);
                    toolbar.setNavigationIcon(d);
                } else
                    toolbar.setNavigationIcon(null);
            }
        });

        return mCustomView;
    }
}
