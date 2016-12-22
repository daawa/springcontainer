package com.abc.viewcontainer.verticalscrollhelper;

import android.view.View;
import android.widget.AbsListView;

/**
 * Created by hzzhangzhenwei on 2016/12/22.
 */

public class DefaultVerticalScrollHelper implements IVerticalScrollHelper {
    private View target;

    public DefaultVerticalScrollHelper(View v) {
        target = v;
    }

    @Override
    public boolean canScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (target instanceof AbsListView) {
                AbsListView listView = (AbsListView) target;
                int count = listView.getChildCount();
                View lastChild = count > 0 ? listView.getChildAt(count - 1) : null;
                if (lastChild != null) {
                    int lvp = listView.getLastVisiblePosition();
                    if (lvp == (listView.getAdapter().getCount() - 1) && lastChild.getBottom() <= listView.getHeight()) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }

            return target.getScrollY() < 0;

        }

        return target.canScrollVertically(1);

    }

    @Override
    public boolean canScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (target instanceof AbsListView) {
                AbsListView listView = (AbsListView) target;
                View firstChild = listView.getChildAt(0);
                if (firstChild != null) {
                    int firstVisiblePos = listView.getFirstVisiblePosition();
                    if (firstVisiblePos == 0 && firstChild.getTop() >= 0 && listView.getTop() >= 0) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }

            return target.getScrollY() > 0;

        }

        return target.canScrollVertically(-1);

    }

}
