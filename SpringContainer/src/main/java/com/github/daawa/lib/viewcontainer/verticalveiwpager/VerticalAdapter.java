package com.github.daawa.lib.viewcontainer.verticalveiwpager;

import com.github.daawa.lib.viewcontainer.scrollhelper.IVerticalScrollHelper;

/**
 * Created by zhangzhenwei on 16/8/5.
 */
public interface  VerticalAdapter {
   public abstract IVerticalScrollHelper getVerticalScrollHelper(int page);
}
