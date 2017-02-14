package com.abc.viewcontainer.verticalveiwpager;

import com.abc.viewcontainer.scrollhelper.IVerticalScrollHelper;

/**
 * Created by zhangzhenwei on 16/8/5.
 */
public interface  VerticalAdapter {
   public abstract IVerticalScrollHelper getVerticalScrollHelper(int page);
}
