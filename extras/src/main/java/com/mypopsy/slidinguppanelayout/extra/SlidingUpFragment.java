package com.mypopsy.slidinguppanelayout.extra;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;

import com.mypopsy.widget.SlidingUpPaneLayout;

/**
 * A fragment that shows itself in a {@link com.mypopsy.widget.SlidingUpPaneLayout}. Like a {@link
 * android.support.v4.app.DialogFragment}, you can show this either in a bottom sheet by using
 * {@link #show(FragmentManager, int)} or attach it to a view with the normal fragment transaction
 * methods.
 * <p>
 * If you don't want to extend from this for your fragment instance, you can use {@link SlidingUpFragmentDelegate}
 * in your fragment implementation instead. You must, however, still implement {@link SlidingUpFragmentInterface}.
 */
public class SlidingUpFragment extends Fragment implements SlidingUpFragmentInterface {

    private SlidingUpFragmentDelegate delegate;

    public SlidingUpFragment() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void show(FragmentManager manager, @IdRes int slidingPaneLayoutId, SlidingUpPaneLayout.State state) {
        getDelegate().show(manager, slidingPaneLayoutId, state);
    }

    public void show(FragmentManager manager, @IdRes int slidingPaneLayoutId) {
        show(manager, slidingPaneLayoutId, SlidingUpPaneLayout.State.EXPANDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int show(FragmentTransaction transaction, @IdRes int slidingPaneLayoutId, SlidingUpPaneLayout.State state) {
        return getDelegate().show(transaction, slidingPaneLayoutId, state);
    }

    public int show(FragmentTransaction transaction, @IdRes int slidingPaneLayoutId) {
        return getDelegate().show(transaction, slidingPaneLayoutId, SlidingUpPaneLayout.State.EXPANDED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dismiss() {
        getDelegate().dismiss();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dismissAllowingStateLoss() {
        getDelegate().dismissAllowingStateLoss();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getDelegate().onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getDelegate().onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDelegate().onCreate(savedInstanceState);
    }

    @Override
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState) {
        return getDelegate().getLayoutInflater(savedInstanceState, super.getLayoutInflater(savedInstanceState));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDelegate().onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        getDelegate().onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getDelegate().onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        getDelegate().onDestroyView();
        super.onDestroyView();
    }

    private SlidingUpFragmentDelegate getDelegate() {
        if (delegate == null) {
            delegate = SlidingUpFragmentDelegate.create(this);
        }
        return delegate;
    }
}