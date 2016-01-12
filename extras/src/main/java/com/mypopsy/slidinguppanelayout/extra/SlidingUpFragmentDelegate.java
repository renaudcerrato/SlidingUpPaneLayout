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

import com.mypopsy.widget.SlidingUpPaneLayout;
import com.mypopsy.widget.SlidingUpPaneLayout.State;

import java.io.Serializable;

import static com.mypopsy.widget.SlidingUpPaneLayout.State.EXPANDED;


public final class SlidingUpFragmentDelegate extends SlidingUpPaneLayout.SimplePanelSlideListener
        implements SlidingUpFragmentInterface {

    private static final String TAG = SlidingUpFragmentDelegate.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String SAVED_SHOWN_AS_SLIDING_FRAGMENT = "slidinguppanel:shownAsSliding";
    private static final String SAVED_BACK_STACK_ID = "slidinguppanel:mBackStackId";
    private static final String SAVED_SLIDING_PANE_LAYOUT_ID = "slidinguppanel:layoutId";
    private static final String ARG_PANEL_STATE = "slidinguppanel:state";

    @IdRes
    private int mSlidingPaneLayoutId = View.NO_ID;
    @Nullable
    private SlidingUpPaneLayout mSlidingUpLayout;
    private boolean mShowSliding = true;
    private int mBackStackId = -1;
    private boolean mViewDestroyed;
    private Fragment mFragment;


    public static SlidingUpFragmentDelegate create(Fragment fragment) {
        return new SlidingUpFragmentDelegate(fragment);
    }

    private SlidingUpFragmentDelegate(Fragment fragment) {
        this.mFragment = fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show(FragmentManager manager, @IdRes int slidingPaneLayoutId, State state) {
        if(DEBUG) Log.d(TAG, "-----show("+state+")");
        this.mSlidingPaneLayoutId = slidingPaneLayoutId;
        manager.beginTransaction()
                .add(putArg(mFragment, ARG_PANEL_STATE, state), "slidingfragment-"+slidingPaneLayoutId)
                .commit();
    }

    public void show(FragmentManager manager, @IdRes int slidingPaneLayoutId) {
        show(manager, slidingPaneLayoutId, EXPANDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int show(FragmentTransaction transaction, @IdRes int slidingPaneLayoutId, State state) {
        if(DEBUG) Log.d(TAG, "-----show("+state+")");
        this.mSlidingPaneLayoutId = slidingPaneLayoutId;

        transaction.add(putArg(mFragment, ARG_PANEL_STATE, state), "slidingfragment-" + slidingPaneLayoutId);
        mBackStackId = transaction.commit();
        return mBackStackId;
    }

    public int show(FragmentTransaction transaction, @IdRes int slidingPaneLayoutId) {
        return show(transaction, slidingPaneLayoutId, EXPANDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dismiss() {
        dismissInternal(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dismissAllowingStateLoss() {
        dismissInternal(true);
    }

    private void dismissInternal(boolean allowStateLoss) {
        if(DEBUG) Log.d(TAG, "-----dismiss("+allowStateLoss+")");
        if (mBackStackId >= 0) {
            mFragment.getFragmentManager().popBackStack(mBackStackId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mBackStackId = -1;
        } else {
            FragmentTransaction ft = mFragment.getFragmentManager().beginTransaction();
            ft.remove(mFragment);
            if (allowStateLoss) {
                ft.commitAllowingStateLoss();
            } else {
                ft.commit();
            }
        }
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "-----onCreate()");
        mShowSliding = AccessFragmentInternals.getContainerId(mFragment) == 0;

        if (savedInstanceState != null) {
            mShowSliding = savedInstanceState.getBoolean(SAVED_SHOWN_AS_SLIDING_FRAGMENT, mShowSliding);
            mBackStackId = savedInstanceState.getInt(SAVED_BACK_STACK_ID, -1);
            mSlidingPaneLayoutId = savedInstanceState.getInt(SAVED_SLIDING_PANE_LAYOUT_ID, View.NO_ID);
        }
    }

    @CheckResult
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState, LayoutInflater superInflater) {
        if (!mShowSliding) {
            return superInflater;
        }
        mSlidingUpLayout = getSlidingUpLayout();
        if (mSlidingUpLayout != null) {
            return LayoutInflater.from(mSlidingUpLayout.getContext());
        }
        return LayoutInflater.from(mFragment.getContext());
    }

    public void onAttach(Context context) {
        if(DEBUG) Log.d(TAG, "-----onAttach()");
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "-----onActivityCreated()");
        mViewDestroyed = false;

        if (!mShowSliding) {
            return;
        }

        View view = mFragment.getView();
        if (view == null) {
            return;
        }

        if(view.getParent() != null)
            throw new IllegalStateException(mFragment + " can not be attached to a container view");

        if(mSlidingUpLayout == null)
            throw new IllegalStateException("can't find SlidingUpPaneLayout with id "+mSlidingPaneLayoutId);

        mSlidingUpLayout.addView(view);
        mSlidingUpLayout.addPaneListener(this);

        if(savedInstanceState == null) {
            mSlidingUpLayout.setState(getPanelState(EXPANDED));
        }
    }

    public void onStart() {
        if(DEBUG) Log.d(TAG, "-----onStart()");
    }

    public void onSaveInstanceState(Bundle outState) {
        if(DEBUG) Log.d(TAG, "-----onSaveInstanceState()");
        if (!mShowSliding) {
            outState.putBoolean(SAVED_SHOWN_AS_SLIDING_FRAGMENT, false);
        }
        if (mBackStackId != -1) {
            outState.putInt(SAVED_BACK_STACK_ID, mBackStackId);
        }
        if (mSlidingPaneLayoutId != View.NO_ID) {
            outState.putInt(SAVED_SLIDING_PANE_LAYOUT_ID, mSlidingPaneLayoutId);
        }
    }

    public void onDestroyView() {
        if(DEBUG) Log.d(TAG, "-----onDestroyView("+mFragment.getView()+")");
        mViewDestroyed = true;
        onPanelHidden(mFragment.getView());
    }

    public void onDetach() {
        if(DEBUG) Log.d(TAG, "-----onDetach()");
    }

    @Override
    @CallSuper
    public void onPanelHidden(View view) {
        if(DEBUG) Log.d(TAG, "-----onPanelHidden("+view+")");
        if(mSlidingUpLayout != null) {
            mSlidingUpLayout.removePaneListener(this);
            mSlidingUpLayout.removeView(view);
            if (!mViewDestroyed) dismissInternal(true);
        }
    }

    private SlidingUpPaneLayout getSlidingUpLayout() {
        if (mSlidingUpLayout == null) mSlidingUpLayout = findSlidingUpLayout();
        return mSlidingUpLayout;
    }

    @Nullable
    private SlidingUpPaneLayout findSlidingUpLayout() {
        Fragment parentFragment = mFragment.getParentFragment();
        if (parentFragment != null) {
            View view = parentFragment.getView();
            if (view != null) {
                return (SlidingUpPaneLayout) view.findViewById(mSlidingPaneLayoutId);
            } else {
                return null;
            }
        }
        Activity parentActivity = mFragment.getActivity();
        if (parentActivity != null) {
            return (SlidingUpPaneLayout) parentActivity.findViewById(mSlidingPaneLayoutId);
        }
        return null;
    }

    static private Fragment putArg(Fragment fragment, String key, Serializable value) {
        Bundle args = fragment.getArguments();
        if(args == null) args = new Bundle();
        args.putSerializable(key, value);
        fragment.setArguments(args);
        return fragment;
    }

    private State getPanelState(State def) {
        Bundle args = mFragment.getArguments();
        if(args == null) return def;
        State state = (State) args.getSerializable(ARG_PANEL_STATE);
        if(state == null) return def;
        return state;
    }
}