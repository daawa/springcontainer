package com.abc.viewcontainer.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import com.abc.viewcontainer.R;


/**
 * Created by zhangzhenwei on 16/9/2.
 */
public class AspectViewPager extends ViewPager {
    private float sizeAspect = 0;

    public AspectViewPager(Context context) {
        super(context);
    }

    public AspectViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (null != attrs) {
            int[] aspect = {R.attr.sizeAspect};
            TypedArray attributes = context.obtainStyledAttributes(attrs, aspect);
            sizeAspect = attributes.getFloat(0,0f);
            attributes.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if(width > 0 && sizeAspect > 0){
            int height = (int)(width * sizeAspect);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
