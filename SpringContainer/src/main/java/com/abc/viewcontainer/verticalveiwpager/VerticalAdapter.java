package com.abc.viewcontainer.verticalveiwpager;

import android.support.v4.view.PagerAdapter;

import com.abc.viewcontainer.verticalscrollhelper.IVerticalScrollHelper;

/**
 * Created by zhangzhenwei on 16/8/5.
 */
public interface  VerticalAdapter {
   public abstract IVerticalScrollHelper getVerticalScrollHelper(int page);
}
