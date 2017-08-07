package com.github.daawa.lib.viewcontainer.springcontainer;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ziv-zh on 2016/12/29.
 * Custom your header view or footer view by implements this interface
 */

public interface ISpringView {

    /**
     * create a custom view to be displayed on top or bottom of SpringContainer, which can be updated or animated at different state
     * @param springView SpringContainer's headerContainer or footerContainer
     * @return a view
     */
    View onCreateSpringView(ViewGroup springView);

    /**
     * @deprecated just return null is ok
     * @param springView SpringContainer's headerContainer or footerContainer
     * @return background view for view returned by {@link #onCreateSpringView(ViewGroup) onCreateSpringView()}
     */
    View onCreateSpringViewBackground(ViewGroup springView);

    /**
     * when the state of SpringContainer changed, this method would be called.
     * @param old
     * @param current
     * @param afterTransferAction  once the transformation is done， call the {@param afterTransferAction} so as to make SpringContainer work well.
     */
    void onStateChanged(int old, int current, Runnable afterTransferAction);


    /**
     * when the height of SpringContainer's headerContainer changed, this method would be called
     * @param cur  current height of SpringContainer's headerContainer
     */
    void onHeightChanged(int cur);

    /**
     * invoked when user's finger is released from the SpringContainer
     * @param springView
     * @return true if you deal with {@param springView} yourself, else the SpringContainer will animate the {@param springView} to being gone.
     */
    boolean onRelease(ViewGroup springView);
}
