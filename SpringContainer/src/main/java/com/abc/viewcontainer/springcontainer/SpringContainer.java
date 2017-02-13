package com.abc.viewcontainer.springcontainer;

/**
 * Created by zhangzhenwei on 16/7/26.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
//import android.util.Log;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abc.viewcontainer.R;
import com.abc.viewcontainer.scrollhelper.DefaultScrollHelper;
import com.abc.viewcontainer.scrollhelper.IHorizontalScrollHelper;
import com.abc.viewcontainer.scrollhelper.IVerticalScrollHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;


/**
 * Created by mae on 15/11/21.
 * TODO: sometime in list view, redundant itemClick event happens 
 */
public class SpringContainer extends FrameLayout {

    private static String TAG = SpringContainer.class.getSimpleName();

    private boolean mSpringEnabled = true;

    public static final int STATUS_PULL_TO_REFRESH = 0;// pulling
    public static final int STATUS_RELEASE_TO_REFRESH = 1;
    public static final int STATUS_REFRESHING = 2;
    public static final int STATUS_REFRESH_FINISHED = 3;
    public static final int STATUS_REFRESH_CANCELED = 4;

    //=================drag 2 load more ===============//
    public static final int STATUS_DRAG_TO_LOAD = 10;
    public static final int STATUS_RELEASE_TO_LOAD = 11;
    public static final int STATUS_LOADING = 12;
    public static final int STATUS_LOAD_FINISHED = 13;
    public static final int STATUS_LOAD_CANCELED = 14;



    private Pull2RefreshListener mRefreshAction;
    private Drag2LoadListener mDrag2LoadAction;


    /**
     * height threshold of headerContainer view,
     * at which the spring container will transfer to {@link #STATUS_RELEASE_TO_REFRESH } state
     */
    private int HeightThreshold = 240;

    /**
     * headerContainer view
     */
    private ViewGroup headerContainer;
    private ViewGroup.LayoutParams headerLayoutParams;
    private Animator hideHeader;
    /**
     * headerContainer background, can be zoomed while pulling
     */
    View headerBackground;

    private ViewGroup footerContainer;
    private ViewGroup.LayoutParams footerLayoutParams;
    private Animator hideFooter;

    /**
     * Content View
     */
    private List<View> contentViews = new ArrayList<>(3);

    IVerticalScrollHelper touchDownChildScrollHelper;
    private boolean mAble2PullWhenTouchDown;
    private boolean mAble2PushWhenTouchDown;
    WeakHashMap<View, IVerticalScrollHelper> verticalScrollHelperWeakHashMap = new WeakHashMap<>(3);
    //todo:
    WeakHashMap<View, IHorizontalScrollHelper> horizontalScrollHelperWeakHashMap = new WeakHashMap<>(3);

    public void addChildVerticalScrollHelper(View child, IVerticalScrollHelper childScrollHelper) {
        this.verticalScrollHelperWeakHashMap.put(child, childScrollHelper);
    }

    public void addChildHorizontalScrollHelper(View child, IHorizontalScrollHelper childScrollHelper) {
        this.horizontalScrollHelperWeakHashMap.put(child, childScrollHelper);
    }


    private int currentRefreshingStatus = STATUS_REFRESH_FINISHED;
    private int currentLoadingStatus = STATUS_LOAD_FINISHED;
    private boolean isLoading = false;

    private boolean isRefreshing = false;

    private int mInitialYDown;
    private int mInitialXDown; //todo:
    private int touchSlop;

    private View pTarget;


    public SpringContainer(Context context) {
        this(context, null);
    }

    public SpringContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public SpringContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHeaderView(new ISpringView() {
            @Override
            public View onCreateSpringView(ViewGroup headerContainer) {
                return null;
            }

            @Override
            public View onCreateSpringViewBackground(ViewGroup headerContainer) {
                return null;
            }

            @Override
            public void onStateChanged(int old, int state) {

            }

            @Override
            public void onHeightChanged(int cur) {

            }

            @Override
            public void onRefreshFinished() {

            }
        });

        setFooterView(new ISpringView() {
            @Override
            public View onCreateSpringView(ViewGroup headerContainer) {
                return null;
            }

            @Override
            public View onCreateSpringViewBackground(ViewGroup headerContainer) {
                return null;
            }

            @Override
            public void onStateChanged(int old, int state) {

            }

            @Override
            public void onHeightChanged(int cur) {

            }

            @Override
            public void onRefreshFinished() {

            }
        });

        if (null != attrs) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RefreshableListView);
            mSpringEnabled = attributes.getBoolean(R.styleable.RefreshableListView_springEnabled, true);
            attributes.recycle();
        }

        LayoutInflater.from(context).inflate(R.layout.springcontainer_header_container, this, true);
        headerContainer = (ViewGroup) findViewById(R.id.spring_container_header);
        headerLayoutParams = headerContainer.getLayoutParams();

        LayoutInflater.from(context).inflate(R.layout.springcontainer_footer_container, this, true);
        footerContainer = (ViewGroup) findViewById(R.id.spring_container_footer);
        footerLayoutParams = footerContainer.getLayoutParams();

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    ISpringView headerView;
    ISpringView footerView;


    public void setHeaderView(ISpringView hd) {
        this.headerView = hd;

        View v = headerView.onCreateSpringViewBackground(headerContainer);
        if (v != null && v.getParent() == null) {
            headerContainer.addView(v);
        } else if (v != null && v.getParent() != headerContainer) {
            throw new RuntimeException("headerView has been attatched to another parent");
        }

        v = headerView.onCreateSpringView(headerContainer);
        if (v != null && v.getParent() == null) {
            headerContainer.addView(v);
        } else if (v != null && v.getParent() != headerContainer) {
            throw new RuntimeException("headerView has been attatched to another parent");
        }
    }

    public void setFooterView(ISpringView ft) {
        this.footerView = ft;

        View v = footerView.onCreateSpringViewBackground(headerContainer);
        if (v != null && v.getParent() == null) {
            footerContainer.addView(v);
        } else if (v != null && v.getParent() != footerContainer) {
            throw new RuntimeException("footerView has been attatched to another parent");
        }

        v = footerView.onCreateSpringView(footerContainer);
        if (v != null && v.getParent() == null) {
            footerContainer.addView(v);
        } else if (v != null && v.getParent() != footerContainer) {
            throw new RuntimeException("footerView has been attatched to another parent");
        }
    }

    public List<View> getContentViews() {
        return contentViews;
    }


    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0
                && index < 0
                && child.getId() != R.id.spring_container_header
                && child.getId() != R.id.spring_container_footer) {

            if (child.getId() == R.id.spring_container_header_background) {
                headerBackground = child;
                super.addView(child, 0, params);
            } else {
                super.addView(child, getChildCount() - 1, params);
                contentViews.add(child);
                DefaultScrollHelper defaultScrollHelper = new DefaultScrollHelper(child);
                addChildVerticalScrollHelper(child, defaultScrollHelper);
                addChildHorizontalScrollHelper(child, defaultScrollHelper);

            }

        } else {
            super.addView(child, index, params);
        }

//        if (child instanceof RecyclerView) {
//            addChildVerticalScrollHelper(new RecyclerViewVerticalScrollHelper((RecyclerView) child));
//        } else if (child instanceof ListView) {
//            addChildVerticalScrollHelper(new ListViewVerticalScrollHelper((ListView) child));
//        } else if (child instanceof ScrollView) {
//            addChildVerticalScrollHelper(new ScrollViewVerticalScrollHelper((ScrollView) child));
//        } else if (child instanceof GridView) {
//            addChildVerticalScrollHelper(new GridViewVerticalScrollHelper((GridView) child));
//        } else if (child instanceof WebView) {
//            addChildVerticalScrollHelper(new WebViewVerticalScrollHelper((WebView) child));
//        }


    }


    public void setSpringEnabled(boolean enable) {
        mSpringEnabled = enable;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int contentOffset = 0;
        if (headerContainer.getHeight() > 0) {
            contentOffset = headerContainer.getHeight();
        } else if (footerContainer.getHeight() > 0) {
            contentOffset = -footerContainer.getHeight();
            footerContainer.offsetTopAndBottom(this.getHeight() - footerContainer.getHeight());
        }

        if (!contentViews.isEmpty())
            for (View v : contentViews)
                v.offsetTopAndBottom(contentOffset);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        float curX = MotionEventCompat.getX(event, pointerIndex) + 0.5f;
        float curY = MotionEventCompat.getY(event, pointerIndex) + 0.5f;
        //logAction(action,pointerIndex,curX,curY);

        int distanceY = 0, distanceX = 0;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                mInitialYDown = (int) curY;
                mInitialXDown = (int) curX;
                pTarget = getTargetChild(curX, curY);
                touchDownChildScrollHelper = verticalScrollHelperWeakHashMap.get(pTarget);
                mAble2PullWhenTouchDown = mSpringEnabled && (isAbleToPull(touchDownChildScrollHelper) || headerContainer.getHeight() > 0);
                mAble2PushWhenTouchDown = mSpringEnabled && (isAbleToPush(touchDownChildScrollHelper) || footerContainer.getHeight() > 0);
                break;
            }


            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                mInitialYDown = (int) curY;
                mInitialXDown = (int) curX;
                break;
            }


            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(event);
                break;
            }


            case MotionEvent.ACTION_MOVE: {
                stopHeaderFooterAnim();
                boolean interceptedPull = false;
                //boolean fakeDown = false;
                boolean interceptedPush = false;

                final int index = MotionEventCompat.findPointerIndex(event, mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                int yMove = (int) (int) (MotionEventCompat.getY(event, index) + 0.5f);
                int xMove = (int) (int) (MotionEventCompat.getX(event, index) + 0.5f);
                distanceY = yMove - mInitialYDown;
                distanceX = xMove - mInitialXDown;
                mInitialYDown = yMove;
                mInitialXDown = (int) curX;

                if (Math.abs(distanceX) > Math.abs(distanceY) /*&& targetCanScrollHorizontally(distanceX)*/) {
                    super.dispatchTouchEvent(event);
                    return true;
                }

                boolean tmpAblePull = isAble2PullNow(curX, curY);
                if (tmpAblePull != mAble2PullWhenTouchDown) {
                    mAble2PullWhenTouchDown = tmpAblePull;//isAble2PullNow(curX, curY);
                    if (mAble2PullWhenTouchDown) {
                        mInitialYDown = (int) curY;
                        interceptedPull = true;
                    } else {
                        //fakeDown = true;
                    }
                }

                boolean tmpAblePush = isAble2PushNow(curX, curY);
                if (tmpAblePush != mAble2PushWhenTouchDown) {
                    mAble2PushWhenTouchDown = tmpAblePush;//isAble2PushNow(curX, curY);
                    if (mAble2PushWhenTouchDown) {
                        mInitialYDown = (int) curY;
                        interceptedPush = true;
                    } else {
                        //fakeDown = true;
                    }
                }

                if (mAble2PullWhenTouchDown || mAble2PushWhenTouchDown) {

                    boolean consumed = false;
                    //boolean fakeCancel = false;
                    if (mAble2PullWhenTouchDown) {
                        consumed = updateHeaderLayout(distanceY);
                        //fakeCancel = consumed && interceptedPull;
                    }
                    if (mAble2PushWhenTouchDown) {
                        consumed = consumed || updateFooterLayout(-distanceY);
                        //fakeCancel = fakeCancel || (consumed && interceptedPush);
                    }

//                    if (fakeCancel) {
//                        MotionEvent cancelEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), MotionEvent.ACTION_CANCEL/*MotionEvent.ACTION_UP*/, event.getX(), event.getY(), 0);
//                        if (pTarget != null)
//                            pTarget.dispatchTouchEvent(cancelEvent);
//                        return true;
//                    } else
                    if (consumed) {
                        return true;
                    }
                }

//                else if (fakeDown) {
//                    MotionEvent downEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), MotionEvent.ACTION_DOWN, event.getX(), event.getY(), 0);
////                    if (pTarget != null)
////                        pTarget.dispatchTouchEvent(newEv);
//                    super.dispatchTouchEvent(downEvent);
//                    return true;
//                }
                break;
            }

            case MotionEvent.ACTION_UP:
            default: {
                if (mAble2PullWhenTouchDown) {
                    hideHeader = createHideHeaderAnimaton();
                    hideHeader.start();
                }
                if (mAble2PushWhenTouchDown) {
                    hideFooter = createHideFooterAnimation();
                    hideFooter.start();
                }

                mInitialYDown = 0;
                break;
            }
        }

        // may return false, ( e.g happened in headerView or footerView, or the content view is not interested in any event),
        // so subsequent Action_move or Action_up events would not be received.
        boolean sret = super.dispatchTouchEvent(event);
//        if(!sret){
//            mInitialYDown = 0;
//        }
        return sret || mAble2PullWhenTouchDown || mAble2PushWhenTouchDown;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        /*
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

        boolean able2pull = mSpringEnabled && (isAbleToPull() || headerContainer.getHeight() > 0);
        boolean able2push = mSpringEnabled && (isAbleToPush() || footerContainer.getHeight() > 0);
        if (able2pull || able2push) {
            int distance = 0;
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                    mInitialYDown = (int) (event.getY() + 0.5f);
                }
                break;

                case MotionEventCompat.ACTION_POINTER_DOWN: {
                    mScrollPointerId = MotionEventCompat.getPointerId(event, actionIndex);
                    mInitialYDown = (int) (MotionEventCompat.getY(event, actionIndex) + 0.5f);
                }
                break;

                case MotionEventCompat.ACTION_POINTER_UP: {
                    onPointerUp(event);
                }
                break;

                case MotionEvent.ACTION_MOVE: {
                    int index = MotionEventCompat.findPointerIndex(event, mScrollPointerId);
                    if (index < 0) {
                        Log.e(TAG, "Error processing SpringContainer; pointer index for id " +
                                mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                        return false;
                    }
                    int yMove = (int) (MotionEventCompat.getY(event, index) + 0.5f);
                    distance = (yMove - mInitialYDown);

                    if (able2pull) {
                        // 如果手指是上滑状态，并且下拉刷新view是完全隐藏的，就屏蔽下拉事件
                        if ((distance <= 0 && headerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop)) {

                        } else {
                            mInitialYDown = yMove;
                            return true;
                        }
                    }

                    if (able2push) {
                        distance = -distance;
                        if ((distance <= 0 && footerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop)) {

                        } else {
                            mInitialYDown = yMove;
                            return true;
                        }
                    }
                }

                break;

                default:
                    mInitialYDown = 0;
                    break;
            }

        }
        */

        return super.onInterceptTouchEvent(event);
    }

    int mScrollPointerId;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        /*
        stopHeaderFooterAnim();
        boolean ret = false;

        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

//        boolean able2pull = mSpringEnabled  && (mAble2PullWhenTouchDown || (isAble2PullNow(event.getX(),event.getY()) || headerContainer.getHeight() > 0));
//        boolean able2push = mSpringEnabled && (mAble2PushWhenTouchDown ||isAble2PushNow(event.getX(),event.getY()) || footerContainer.getHeight() > 0);

        //if (able2pull || able2push) {
        int distance = 0;
        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(event, mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                int yMove = (int) (MotionEventCompat.getY(event, index) + 0.5f);
                distance = yMove - mInitialYDown;
                if (mAble2PullWhenTouchDown) {
                    updateHeaderLayout(distance);
                }
                if (mAble2PushWhenTouchDown) {
                    updateFooterLayout(-distance);
                }
                mInitialYDown = yMove;
                break;
            }


            case MotionEvent.ACTION_UP:
            default: {
                if (mAble2PullWhenTouchDown) {
                    hideHeader = createHideHeaderAnimaton();
                    hideHeader.start();
                }
                if (mAble2PushWhenTouchDown) {
                    hideFooter = createHideFooterAnimation();
                    hideFooter.start();
                }

                mInitialYDown = 0;
            }
            break;
        }

        if (mAble2PullWhenTouchDown) {
            //update headerContainer view
            if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH || currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
                updateHeaderView();
                lastRefreshingStatus = currentRefreshingStatus;
                // 当前正处于下拉或释放状态，通过返回true屏蔽掉 content view 的滚动事件
                if (distance < 0) {
                    ret = true;
                }
            }
        }

        if (mAble2PushWhenTouchDown) {
            if (currentLoadingStatus == STATUS_DRAG_TO_LOAD || currentLoadingStatus == STATUS_RELEASE_TO_LOAD) {
                updateFooterView();
                lastLoadingStatus = currentLoadingStatus;
                if (distance > 0) {
                    ret = true;
                }
            }
        }

        //}

        if (ret) {
            return true;
        } else {
            return false;
        } */

        return super.onTouchEvent(event);
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (MotionEventCompat.getPointerId(e, actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = MotionEventCompat.getPointerId(e, newIndex);
            mInitialYDown = (int) (MotionEventCompat.getY(e, newIndex) + 0.5f);
        }
    }

    /**
     * @param distance distance of movement
     * @return consumed the move or not
     */
    private boolean updateHeaderLayout(int distance) {
        // if wr pushing up, and the header is invisible now, do not consume the movement
        if ((distance <= 0 && headerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop / 2)) {
            return false;
        }
        int delta = (distance * 4 / 5);
        headerLayoutParams.height += delta;
        if (headerLayoutParams.height < 0) {
            delta -= headerLayoutParams.height;
            headerLayoutParams.height = 0;
        }
        headerContainer.setLayoutParams(headerLayoutParams);
        headerView.onHeightChanged(headerLayoutParams.height);

        /*if (headerBackground != null) {
            headerBackground.setPivotY(0);
            headerBackground.setPivotX(headerBackground.getHeight() / 2);

            float scale = headerBackground.getScaleY();
            scale += distance / (float) HeightThreshold;
            if (scale > 2)
                scale = 2;
            else if (scale < 1)
                scale = 1;

            headerBackground.setScaleX(scale);
            headerBackground.setScaleY(scale);
        }*/

        int old = currentRefreshingStatus;
        if (currentRefreshingStatus == STATUS_REFRESHING && headerLayoutParams.height < HeightThreshold) {
            currentRefreshingStatus = STATUS_REFRESH_CANCELED;
            headerView.onStateChanged(old, currentRefreshingStatus);
        }
        if (currentRefreshingStatus != STATUS_REFRESHING) {
            if (headerLayoutParams.height >= HeightThreshold) {
                currentRefreshingStatus = STATUS_RELEASE_TO_REFRESH;
            } else {
                currentRefreshingStatus = STATUS_PULL_TO_REFRESH;
            }
            headerView.onStateChanged(old, currentRefreshingStatus);
        }

        return true;

    }

    /**
     * @param distance distance of movement
     * @return consumed the move or not
     */
    private boolean updateFooterLayout(int distance) {
        if ((distance <= 0 && footerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop)) {
            return false;
        }
        footerLayoutParams.height += (distance * 4 / 5);
        if (footerLayoutParams.height < 0)
            footerLayoutParams.height = 0;

        footerContainer.setLayoutParams(footerLayoutParams);
        footerView.onHeightChanged(footerLayoutParams.height);

        int old = currentLoadingStatus;
        if (currentLoadingStatus == STATUS_LOADING && footerLayoutParams.height < HeightThreshold) {
            currentLoadingStatus = STATUS_LOAD_CANCELED;
            footerView.onStateChanged(old, currentLoadingStatus);
        }
        if (currentLoadingStatus != STATUS_LOADING) {
            if (footerLayoutParams.height >= HeightThreshold) {
                currentLoadingStatus = STATUS_RELEASE_TO_LOAD;
            } else {
                currentLoadingStatus = STATUS_DRAG_TO_LOAD;
            }
            footerView.onStateChanged(old,currentLoadingStatus);
        }

        return true;
    }

    public void setOnRefreshListener(Pull2RefreshListener listener) {
        //setEnablePull2Refresh(true);
        if (listener == null) {
            headerContainer.setVisibility(View.INVISIBLE);
        } else {
            headerContainer.setVisibility(View.VISIBLE);
        }

        mRefreshAction = listener;
    }

    public void setOnLoadListener(Drag2LoadListener listener) {
        //setEnalbleDrag2LoadMore(true);
        if (listener == null) {
            footerContainer.setVisibility(View.INVISIBLE);
        } else {
            footerContainer.setVisibility(View.VISIBLE);
        }
        mDrag2LoadAction = listener;
    }

    /**
     * notify spring container to change state
     */
    public void finishRefreshing() {
        isRefreshing = false;
        int old = currentRefreshingStatus;
        currentRefreshingStatus = STATUS_REFRESH_FINISHED;
        headerView.onRefreshFinished();
        headerView.onStateChanged(old, currentRefreshingStatus);
        //preferences.edit().putLong(KEY_UPDATED_AT + mId4UpdateTime, System.currentTimeMillis()).commit();
        if (mInitialYDown <= 0) {
            hideHeader = createHideHeaderAnimaton();
            hideHeader.start();
        }

    }

    public void finishLoadingMore() {
        isLoading = false;
        currentLoadingStatus = STATUS_LOAD_FINISHED;
        if (mInitialYDown <= 0) {
            hideFooter = createHideFooterAnimation();
            hideFooter.start();
        }
    }

    private boolean isAbleToPull(IVerticalScrollHelper childScrollHelper) {
        boolean ableToPull = true;
        if (childScrollHelper != null) {
            ableToPull = !childScrollHelper.canScrollUp();
        }
        if (footerLayoutParams != null) {
            ableToPull &= !(footerLayoutParams.height > 0);
        }
        return ableToPull;

    }

    private boolean isAble2PullNow(float x, float y) {
        View v = null;
        if (pTarget != null) {
            v = pTarget;
        } else {
            getTargetChild(x, y);
        }

        if (v != null) {
            return isAbleToPull(verticalScrollHelperWeakHashMap.get(v));
        }

        return true;
    }

    private boolean isAbleToPush(IVerticalScrollHelper childScrollHelper) {
        boolean able2Push = true;
        if (childScrollHelper != null) {
            able2Push = !childScrollHelper.canScrollDown();
        }

        if (headerLayoutParams != null) {
            able2Push = (able2Push && !(headerLayoutParams.height > 0));
        }
        return able2Push;

    }

    private boolean isAble2PushNow(float x, float y) {
        View v = null;
        if (pTarget != null) {
            v = pTarget;
        } else {
            getTargetChild(x, y);
        }
        if (v != null) {
            return isAbleToPush(verticalScrollHelperWeakHashMap.get(v));
        }

        return true;
    }

    private boolean targetCanScrollHorizontally(int j){
        if(pTarget == null){
            return false;
        }
        IHorizontalScrollHelper horizontalScrollHelper = horizontalScrollHelperWeakHashMap.get(pTarget);
        if(horizontalScrollHelper == null){
            return false;
        }

        if(j > 0){
            return horizontalScrollHelper.canScrollRight();
        } else{
            return horizontalScrollHelper.canScrollLeft();
        }

    }


    protected Animator createHideHeaderAnimaton() {

        if (hideHeader != null) {
            hideHeader.cancel();
            hideHeader = null;
        }

        if (headerLayoutParams.height > HeightThreshold && mRefreshAction != null) {

            hideHeader = createHeightAnimation(headerContainer, headerLayoutParams.height, HeightThreshold, true);
            hideHeader.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

//                    if (headerBackground != null) {
//                        headerBackground.setScaleX(1);
//                        headerBackground.setScaleY(1);
//                    }

                    if (currentRefreshingStatus != STATUS_REFRESHING) {
                        int old = currentRefreshingStatus;
                        currentRefreshingStatus = STATUS_REFRESHING;
                        headerView.onStateChanged(old, currentRefreshingStatus);
                        if (mRefreshAction != null) {
                            if (!isRefreshing) {
                                isRefreshing = true;
                                mRefreshAction.onRefresh(SpringContainer.this);
                            }

                        } else {
                            SpringContainer.this.finishRefreshing();
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;

                    if (headerBackground != null) {
                        headerBackground.setScaleX(1);
                        headerBackground.setScaleY(1);
                    }

                }

            });
        } else {

            hideHeader = createHeightAnimation(headerContainer, headerLayoutParams.height, 0, true);
            hideHeader.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }
//                    if (headerBackground != null) {
//                        headerBackground.setScaleX(1);
//                        headerBackground.setScaleY(1);
//                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;

//                    if (headerBackground != null) {
//                        headerBackground.setScaleX(1);
//                        headerBackground.setScaleY(1);
//                    }
                }
            });
        }

        return hideHeader;

    }

    protected Animator createHideFooterAnimation() {

        if (hideFooter != null) {
            hideFooter.cancel();
            hideFooter = null;
        }

        if (footerLayoutParams.height > HeightThreshold && mDrag2LoadAction != null) {

            hideFooter = createHeightAnimation(footerContainer, footerLayoutParams.height, HeightThreshold, false);
            hideFooter.addListener(new AnimatorListenerAdapter() {
                boolean canceled;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

                    if (currentLoadingStatus != STATUS_LOADING) {
                        int old = currentLoadingStatus;
                        currentLoadingStatus = STATUS_LOADING;
                        footerView.onStateChanged(old, currentLoadingStatus);
                        if (mDrag2LoadAction != null) {
                            if (!isLoading) {
                                isLoading = true;
                                mDrag2LoadAction.load(SpringContainer.this);
                            }

                        } else {
                            SpringContainer.this.finishLoadingMore();
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                }

            });
        } else {

            hideFooter = createHeightAnimation(footerContainer, footerLayoutParams.height, 0, false);

        }

        return hideFooter;

    }

    protected Animator createHeightAnimation(View view, int from, int to, boolean header) {
        ValueAnimator animation = ValueAnimator.ofInt(from, to);
        int abs = Math.abs(from - to);

        //int duration = 300 + (int)Math.log(abs + Math.E);
        int duration = 300 + (int) Math.sqrt(abs);

        animation.setDuration(duration);

        animation.addUpdateListener(new HeightUpdateListener(view, header));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        //animation.setInterpolator(new DecelerateInterpolator());

        return animation;
    }


    class HeightUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        View view;
        boolean isHeader;

        public HeightUpdateListener(View v, boolean header) {
            view = v;
            this.isHeader = header;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (int) animation.getAnimatedValue();

            if (view != null && view.getLayoutParams() != null) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = height;
                view.setLayoutParams(lp);
                if (isHeader) {
                    headerView.onHeightChanged(height);
                } else {
                    footerView.onHeightChanged(height);
                }
            }
        }

    }

    private void stopHeaderFooterAnim() {
        if (hideHeader != null) {
            hideHeader.cancel();
            hideHeader = null;
        }

        if (hideFooter != null) {
            hideFooter.cancel();
            hideFooter = null;
        }

    }


    boolean pointInChildView(float x, float y, View child) {
        x -= getScrollX();
        y -= getScrollY();
        return x > child.getLeft() && x < child.getRight()
                && y > child.getTop() && y < child.getBottom();
    }

    /**
     * Returns true if a child view can receive pointer events.
     */
    private static boolean canViewReceivePointerEvents(View child) {
        return (child.getVisibility() == VISIBLE
                || child.getAnimation() != null);
    }


    protected View getTargetChild(float x, float y) {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (pointInChildView(x, y, v) && canViewReceivePointerEvents(v))
                return v;
        }

        return null;
    }

    private void logAction(int action, int pointerIndex, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.w(TAG, "ACTION_DOWN");
                break;
//            case MotionEvent.ACTION_MOVE:
//                Log.w(TAG,"ACTION_MOVE");
//                break;
            case MotionEvent.ACTION_UP:
                Log.w(TAG, "ACTION_UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.w(TAG, "ACTION_CANCEL");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.w(TAG, "ACTION_POINTER_DOWN");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.w(TAG, "ACTION_POINTER_UP");
                break;
            case MotionEvent.ACTION_OUTSIDE:
                Log.w(TAG, "ACTION_OUTSIDE");
                break;
        }
        Log.w(TAG, "pointerIndex: " + pointerIndex + "  x:" + x + " y:" + y);
    }

    /**
     * TODO: currently only  STATUS_REFRESHING is supported.
     * @param state
     */
    public void setState(int state){
        stopHeaderFooterAnim();
        switch (state){
            case STATUS_PULL_TO_REFRESH:
                break;
            case STATUS_REFRESHING:
                headerLayoutParams.height = HeightThreshold;
                headerContainer.setLayoutParams(headerLayoutParams);
                headerView.onHeightChanged(HeightThreshold);
                int old = currentRefreshingStatus;
                currentLoadingStatus = STATUS_REFRESHING;
                headerView.onStateChanged(old, currentLoadingStatus);
                if(mRefreshAction != null){
                    mRefreshAction.onRefresh(this);
                }
                break;
        }
    }

}

