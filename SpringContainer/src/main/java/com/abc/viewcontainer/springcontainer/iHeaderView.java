package com.abc.viewcontainer.springcontainer;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hzzhangzhenwei on 2016/12/29.
 */

public interface iHeaderView {
    View onCreateSpringView(ViewGroup headerContainer);
    View onCreateSpringViewBackground(ViewGroup headerContainer);

    void onStateChanged(int old, int state);
    void onHeightChanged(int cur);
    void onRefreshFinished();
}
