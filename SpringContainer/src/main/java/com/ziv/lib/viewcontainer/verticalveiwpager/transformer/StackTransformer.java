package com.ziv.lib.viewcontainer.verticalveiwpager.transformer;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by zhangzhenwei on 16/8/5.
 */

public class StackTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        page.setTranslationX(page.getWidth() * -position);
        page.setTranslationY(position < 0 ? position * page.getHeight() : 0f);
    }
}