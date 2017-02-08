package com.abc.viewcontainer.verticalveiwpager;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.abc.viewcontainer.scrollhelper.IVerticalScrollHelper;
import com.abc.viewcontainer.verticalveiwpager.transformer.DefaultTransformer;

/**
 * Created by zhangzhenwei on 16/8/5.
 */
public class VerticalViewPager extends ViewPager {

    IVerticalScrollHelper mScrollHelper;


    public VerticalViewPager(Context context) {
        this(context, null);
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPageTransformer(false, new DefaultTransformer());

        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                if (getAdapter() != null && getAdapter() instanceof VerticalAdapter) {
                    VerticalAdapter adapter = (VerticalAdapter) getAdapter();
                    mScrollHelper = adapter.getVerticalScrollHelper(position);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        updateScrollHelper();
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
        updateScrollHelper();
    }

    private void updateScrollHelper() {
        int cur = getCurrentItem();
        if (getAdapter() != null && getAdapter() instanceof VerticalAdapter) {
            VerticalAdapter adapter = (VerticalAdapter) getAdapter();
            mScrollHelper = adapter.getVerticalScrollHelper(cur);
        }

    }

    private MotionEvent swapXY(MotionEvent event) {
        float width = getWidth();
        float height = getHeight();

        float swappedX = (event.getY() / height) * width;
        float swappedY = (event.getX() / width) * height;

        event.setLocation(swappedX, swappedY);

        return event;
    }


    float mDownY = 0;
    float mDownX = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                mDownX = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDownY = 0;
                mDownX = 0;
        }

        if (mScrollHelper != null && action != MotionEvent.ACTION_DOWN && mDownY > 0) {
            //only intercept vertical scroll event
            if (Math.abs(mDownX - ev.getX()) < Math.abs(mDownY - ev.getY())) {
                if (ev.getY() > mDownY) {
                    return  !mScrollHelper.canScrollUp();

                } else {
                    return  !mScrollHelper.canScrollDown();
                }
            }
        }


        boolean intercept = super.onInterceptTouchEvent(swapXY(ev));
        swapXY(ev);
        return intercept;


    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        return super.onTouchEvent(swapXY(ev));
    }


}