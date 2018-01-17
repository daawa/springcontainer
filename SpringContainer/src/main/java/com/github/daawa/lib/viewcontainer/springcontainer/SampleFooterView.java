package com.github.daawa.lib.viewcontainer.springcontainer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.daawa.lib.viewcontainer.R;

import static com.github.daawa.lib.viewcontainer.springcontainer.SpringContainer.STATUS_BOTTOM_DRAG_TO_LINGER;
import static com.github.daawa.lib.viewcontainer.springcontainer.SpringContainer.STATUS_BOTTOM_LINGERING;
import static com.github.daawa.lib.viewcontainer.springcontainer.SpringContainer.STATUS_BOTTOM_RELEASE_TO_LINGER;

/**
 * Created by ziv-zh on 2017/2/8.
 */

public class SampleFooterView implements ISpringView {
    public String DRAG_TO_LOAD_TIP = "keep pushing to onLoading more";
    public String RELEASE_TO_LOAD_TIP = "release to onLoading more";
    public String LOADING_TIP = "loading..";

    private TextView footerDes;
    private ProgressBar footerProgressBar;

    private int currentLoadingStatus = SpringContainer.STATUS_BOTTOM_LINGER_FINISHED;
    private int lastLoadingStatus = currentLoadingStatus;
    @Override
    public View onCreateSpringView(ViewGroup springView) {
        View footer =  LayoutInflater.from(springView.getContext()).inflate(R.layout.springcontainer_sample_footer, springView,false);
        footerDes = footer.findViewById(R.id.footer_description);
        footerProgressBar = footer.findViewById(R.id.footer_progress_bar);
        return footer;
    }

    @Override
    public View onCreateSpringViewBackground(ViewGroup springView) {
        return null;
    }

    @Override
    public void onStateChanged(int old, int state, final Runnable postTransformAction) {
        currentLoadingStatus = state;
        updateFooterView();
        if (state == SpringContainer.STATUS_BOTTOM_LINGER_FINISHED) {

            footerDes.setText("HOLDING...");
            footerDes.postDelayed(new Runnable() {
                @Override
                public void run() {
                    postTransformAction.run();
                }
            }, 2000);
        } else {
            postTransformAction.run();
        }


    }

    @Override
    public void onHeightChanged(int cur) {

    }


    private void updateFooterView() {
        if (lastLoadingStatus != currentLoadingStatus) {
            if (currentLoadingStatus == STATUS_BOTTOM_DRAG_TO_LINGER) {
                footerDes.setText(DRAG_TO_LOAD_TIP);
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_BOTTOM_RELEASE_TO_LINGER) {
                footerDes.setText(RELEASE_TO_LOAD_TIP);
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_BOTTOM_LINGERING) {
                footerDes.setText(LOADING_TIP);
                footerProgressBar.setVisibility(View.VISIBLE);
            }

            lastLoadingStatus = currentLoadingStatus;
        }

    }

    @Override
    public boolean onRelease(ViewGroup springView) {
        return false;
    }
}
