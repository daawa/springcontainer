package com.abc.viewcontainer.scrollhelper;

import android.view.View;
import android.widget.GridView;

/**
 * Created by zhangzhenwei on 16/8/5.
 */

public class GridViewVerticalScrollHelper implements IVerticalScrollHelper {
    private GridView gridView;

    public GridViewVerticalScrollHelper(GridView v) {
        gridView = v;
    }

    @Override
    public boolean canScrollDown() {

        int count = gridView.getChildCount();
        View lastChild = count > 0 ? gridView.getChildAt(count - 1) : null;
        if (lastChild != null) {
            int lvp = gridView.getLastVisiblePosition();
            if (lvp == (gridView.getAdapter().getCount() - 1) && lastChild.getBottom() <= gridView.getHeight()) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canScrollUp() {

        View firstChild = gridView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = gridView.getFirstVisiblePosition();

            if (firstVisiblePos == 0 && (firstChild.getTop() >= 0) && gridView.getTop() >= 0) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }
}
