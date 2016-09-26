package com.abc.viewcontainer.verticalscrollhelper;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by zhangzhenwei on 16/8/5.
 */
public class RecyclerViewVerticalScrollHelper implements IVerticalScrollHelper {

    RecyclerView mV;

    public RecyclerViewVerticalScrollHelper(RecyclerView v) {
        mV = v;
    }

    @Override
    public boolean canScrollUp() {
        LinearLayoutManager manager = (LinearLayoutManager) mV.getLayoutManager();
        if(manager == null){
            return false;
        }
        int lastVisible = manager.findLastVisibleItemPosition();
        View lastChild = lastVisible >= 0 ? manager.findViewByPosition(lastVisible): null;
        if (lastChild != null) {
            if (lastVisible == (mV.getAdapter().getItemCount() - 1)
                    && lastChild.getBottom() <= (mV.getHeight() - mV.getPaddingBottom())) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canScrollDown() {
        LinearLayoutManager manager = (LinearLayoutManager) mV.getLayoutManager();
        if(manager == null){
            return false;
        }
        int firstVisible = manager.findFirstVisibleItemPosition();
        if(firstVisible >= 0){
            View firstChild =  manager.findViewByPosition(firstVisible);
            if (firstVisible == 0
                    && firstChild.getTop() >= (mV.getTop() - mV.getPaddingTop())) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }
}
