package com.mypopsy.slidinguppanelayout.extra;

import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.mypopsy.widget.SlidingUpPaneLayout;

/**
 * This interface can be applied to a {@link android.support.v4.app.Fragment} to make it compatible with
 * {@link SlidingUpFragmentDelegate}.
 */
public interface SlidingUpFragmentInterface {

    /**
     * Display the sliding panel, adding the fragment to the given FragmentManager. This does
     * <em>not</em> add the transaction to the back stack. When the fragment is dismissed, a new
     * transaction will be executed  to remove it from the activity.
     *
     * @param manager the FragmentManager this fragment will be added to.
     * @param slidingUpLayoutId the SlidingUpPaneLayout layoutId in the parent view to attach the
     * fragment to.
     * @param state the initial state
     */
    void show(FragmentManager manager, @IdRes int slidingUpLayoutId, SlidingUpPaneLayout.State state);
    /**
     * Display the sliding panel, adding the fragment using an excisting transaction and then
     * committing the transaction.
     *
     * @param transaction an existing transaction in which to add the fragment.
     * @param slidingUpLayoutId the SlidingUpPaneLayout layoutId in the parent view to attach the
     * @param state the initial state
     * fragment to.
     */
    int show(FragmentTransaction transaction, @IdRes int slidingUpLayoutId, SlidingUpPaneLayout.State state);

    /**
     * Dismiss the fragment and the sliding panel. If the fragment was added to the back stack, all
     * back stack state up to and including this entry will be popped. Otherwise, a new transaction
     * will be committed to remove this fragment.
     */
    void dismiss();

    /**
     * Version of {@link #dismiss()} that uses {@link FragmentTransaction#commitAllowingStateLoss()}.
     * See linked documentation for further details.
     */
    void dismissAllowingStateLoss();
}