package com.mypopsy.slidinguppanelayout.extra;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.AccessFragmentInternals;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;

import com.mypopsy.widget.SlidingUpPaneLayout;
import com.mypopsy.widget.SlidingUpPaneLayout.State;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;
import static com.mypopsy.widget.SlidingUpPaneLayout.State.HIDDEN;


public final class SlidingUpFragmentDelegate extends SlidingUpPaneLayout.SimplePanelSlideListener{

    private static final String TAG = SlidingUpFragmentDelegate.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String SAVED_SHOWN_AS_SLIDING_FRAGMENT = "slidinguppanel:shownAsSliding";
    private static final String SAVED_BACK_STACK_ID = "slidinguppanel:backStackId";
    private static final String SAVED_SLIDING_PANE_LAYOUT_ID = "slidinguppanel:layoutId";
    
    @IdRes
    private int slidingPaneLayoutId = View.NO_ID;
    private SlidingUpPaneLayout slidingUpLayout;
    private boolean showAsSlidingFragment = true;
    private int backStackId = -1;
    private boolean viewDestroyed;

    private Fragment fragment;

    public static SlidingUpFragmentDelegate create(Fragment fragment) {
        return new SlidingUpFragmentDelegate(fragment);
    }

    private SlidingUpFragmentDelegate(Fragment fragment) {
        this.fragment = fragment;
    }

    /**
     * DialogFragment-like show() method for displaying this the associated sheet fragment
     *
     * @param manager FragmentManager instance
     * @param slidingPaneLayoutId Resource ID of the {@link SlidingUpPaneLayout}
     */
    public void show(FragmentManager manager, @IdRes int slidingPaneLayoutId) {
        if(DEBUG) Log.d(TAG, "-----show()");
        this.slidingPaneLayoutId = slidingPaneLayoutId;
        manager.beginTransaction()
                .add(fragment, "slidingfragment-"+slidingPaneLayoutId)
                .commit();
    }

    /**
     * DialogFragment-like show() method for displaying this the associated sheet fragment
     *
     * @param transaction FragmentTransaction instance
     * @param slidingPaneLayoutId Resource ID of the {@link SlidingUpPaneLayout}
     * @return the back stack ID of the fragment after the transaction is committed.
     */
    public int show(FragmentTransaction transaction, @IdRes int slidingPaneLayoutId) {
        if(DEBUG) Log.d(TAG, "-----show()");
        this.slidingPaneLayoutId = slidingPaneLayoutId;
        transaction.add(fragment, "slidingfragment-"+slidingPaneLayoutId);
        backStackId = transaction.commit();
        return backStackId;
    }

    /**
     * Dismiss the fragment and it's bottom sheet. If the fragment was added to the back stack, all
     * back stack state up to and including this entry will be popped. Otherwise, a new transaction
     * will be committed to remove this fragment.
     */
    public void dismiss() {
        dismissInternal(false);
    }

    public void dismissAllowingStateLoss() {
        dismissInternal(true);
    }

    private void dismissInternal(boolean allowStateLoss) {
        if(DEBUG) Log.d(TAG, "-----dismiss("+allowStateLoss+")");
        if (backStackId >= 0) {
            fragment.getFragmentManager().popBackStack(backStackId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            backStackId = -1;
        } else {
            FragmentTransaction ft = fragment.getFragmentManager().beginTransaction();
            ft.remove(fragment);
            if (allowStateLoss) {
                ft.commitAllowingStateLoss();
            } else {
                ft.commit();
            }
        }
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "-----onCreate()");
        showAsSlidingFragment = AccessFragmentInternals.getContainerId(fragment) == 0;

        if (savedInstanceState != null) {
            showAsSlidingFragment = savedInstanceState.getBoolean(SAVED_SHOWN_AS_SLIDING_FRAGMENT, showAsSlidingFragment);
            backStackId = savedInstanceState.getInt(SAVED_BACK_STACK_ID, -1);
            slidingPaneLayoutId = savedInstanceState.getInt(SAVED_SLIDING_PANE_LAYOUT_ID, View.NO_ID);
        }
    }

    /**
     * Retrieves the appropriate layout inflater, either the sheet's or the view's super container. Note that you should
     * handle the result of this in your getLayoutInflater method.
     *
     * @param savedInstanceState Instance state, here to match Fragment API but unused.
     * @param superInflater The result of the view's inflater, usually the result of super.getLayoutInflater()
     * @return the layout inflater to use
     */
    @CheckResult
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState, LayoutInflater superInflater) {
        if (!showAsSlidingFragment) {
            return superInflater;
        }
        slidingUpLayout = getSlidingUpLayout();
        if (slidingUpLayout != null) {
            return LayoutInflater.from(slidingUpLayout.getContext());
        }
        return LayoutInflater.from(fragment.getContext());
    }

    public void onAttach(Context context) {
        if(DEBUG) Log.d(TAG, "-----onAttach()");
    }

    public void onDetach() {
        if(DEBUG) Log.d(TAG, "-----onDetach()");
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "-----onActivityCreated()");

        if (!showAsSlidingFragment) {
            return;
        }

        View view = fragment.getView();
        if (view != null && view.getParent() != null) {
                throw new IllegalStateException("SlidingUpFragment can not be attached to a container view");
        }
    }

    public void onStart() {
        if(DEBUG) Log.d(TAG, "-----onStart()");
        if(showAsSlidingFragment && slidingUpLayout != null) {
            slidingUpLayout.addView(fragment.getView());
            slidingUpLayout.addPaneListener(this);
            //TODO: save/restore state
            scheduleExpand(EXPANDED);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if(DEBUG) Log.d(TAG, "-----onSaveInstanceState()");
        if (!showAsSlidingFragment) {
            outState.putBoolean(SAVED_SHOWN_AS_SLIDING_FRAGMENT, false);
        }
        if (backStackId != -1) {
            outState.putInt(SAVED_BACK_STACK_ID, backStackId);
        }
        if (slidingPaneLayoutId != View.NO_ID) {
            outState.putInt(SAVED_SLIDING_PANE_LAYOUT_ID, slidingPaneLayoutId);
        }
    }

    public void onDestroyView() {
        if(DEBUG) Log.d(TAG, "-----onDestroyView("+fragment.getView()+")");
        viewDestroyed = true;
        if(slidingUpLayout != null) {
            slidingUpLayout.setState(HIDDEN);
        }
    }

    @Override
    @CallSuper
    public void onPanelHidden(View view) {
        if(DEBUG) Log.d(TAG, "-----onPanelHidden("+view+")");
        slidingUpLayout.removeView(view);
        slidingUpLayout.removePaneListener(this);
        if(!viewDestroyed) dismissInternal(true);
    }

    private void scheduleExpand(final State state) {
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                if(slidingUpLayout.getSlidingPanel() == fragment.getView())
                    slidingUpLayout.setState(state);
                return false;
            }
        });
    }

    private SlidingUpPaneLayout getSlidingUpLayout() {
        if (slidingUpLayout == null) {
            slidingUpLayout = findSlidingUpLayout();
        }
        return slidingUpLayout;
    }

    @Nullable
    private SlidingUpPaneLayout findSlidingUpLayout() {
        Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment != null) {
            View view = parentFragment.getView();
            if (view != null) {
                return (SlidingUpPaneLayout) view.findViewById(slidingPaneLayoutId);
            } else {
                return null;
            }
        }
        Activity parentActivity = fragment.getActivity();
        if (parentActivity != null) {
            return (SlidingUpPaneLayout) parentActivity.findViewById(slidingPaneLayoutId);
        }
        return null;
    }

    @Nullable
    private ViewTreeObserver getViewTreeObserver() {
        View view = fragment.getView();
        if(view == null) return null;
        return view.getViewTreeObserver();
    }
}