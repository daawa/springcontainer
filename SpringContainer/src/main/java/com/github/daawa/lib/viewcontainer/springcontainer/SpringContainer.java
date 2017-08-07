package com.github.daawa.lib.viewcontainer.springcontainer;

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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.github.daawa.lib.viewcontainer.R;
import com.github.daawa.lib.viewcontainer.scrollhelper.DefaultScrollHelper;
import com.github.daawa.lib.viewcontainer.scrollhelper.IHorizontalScrollHelper;
import com.github.daawa.lib.viewcontainer.scrollhelper.IVerticalScrollHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.RunnableFuture;


/**
 * Created by mae on 15/11/21.
 * TODO: sometime in list view, redundant itemClick event happens
 */
public class SpringContainer extends FrameLayout {

    private static String TAG = SpringContainer.class.getSimpleName();

    private boolean mSpringEnabled = true;

    public static final int STATUS_TOP_PULL_TO_LINGER = 0;// pulling
    public static final int STATUS_TOP_RELEASE_TO_LINGER = 1;
    public static final int STATUS_TOP_LINGERING = 2;
    public static final int STATUS_TOP_LINGER_FINISHED = 3;
    public static final int STATUS_TOP_LINGER_CANCELED = 4;

    public static final int STATUS_BOTTOM_DRAG_TO_LINGER = STATUS_TOP_PULL_TO_LINGER;
    public static final int STATUS_BOTTOM_LINGERING = STATUS_TOP_LINGERING;
    public static final int STATUS_BOTTOM_RELEASE_TO_LINGER = STATUS_TOP_RELEASE_TO_LINGER;
    public static final int STATUS_BOTTOM_LINGER_FINISHED = STATUS_TOP_LINGER_FINISHED;
    public static final int STATUS_BOTTOM_LINGER_CANCELED = STATUS_TOP_LINGER_CANCELED;

    private int currentTopStatus = STATUS_TOP_LINGER_FINISHED;
    private int currentBottomStatus = STATUS_BOTTOM_LINGER_FINISHED;

    ISpringView headerView;
    ISpringView footerView;
    private RefreshingStateListener mRefreshAction;
    private LoadingStateListener mDrag2LoadAction;


    /**
     * height threshold of headerContainer view,
     * at which the spring container will transfer to {@link #STATUS_TOP_RELEASE_TO_LINGER } state
     */
    //private int HeightThreshold = 240;
    private int TopThreshold = 240;
    private int BottomThreshold = 240;

    private int mReboundBaseTime = 300;

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
    private boolean mEverConsumedMoveEvent = false;

    private WeakHashMap<View, IVerticalScrollHelper> verticalScrollHelperWeakHashMap = new WeakHashMap<>(3);
    //todo:
    private WeakHashMap<View, IHorizontalScrollHelper> horizontalScrollHelperWeakHashMap = new WeakHashMap<>(3);

    private boolean isBottomLingering = false;
    private boolean isTopLingering = false;

    private int mInitialYDown;
    private int mInitialXDown; //todo:
    private int touchSlop;
    private int mScrollPointerId;

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
            public void onStateChanged(int old, int state, Runnable runnable) {

            }

            @Override
            public void onHeightChanged(int cur) {

            }

            @Override
            public boolean onRelease(ViewGroup v) {
                return false;
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
            public void onStateChanged(int old, int state, Runnable runnable) {

            }

            @Override
            public void onHeightChanged(int cur) {

            }

            @Override
            public boolean onRelease(ViewGroup v) {
                return false;
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


    /**
     * set a custom {@link IVerticalScrollHelper IVerticalScrollHelper} for the child view.
     * SpringContainer uses {@link DefaultScrollHelper DefaultScrollHelper} for every content view by default.
     *
     * @param child
     * @param childScrollHelper see {@link IVerticalScrollHelper IVerticalScrollHelper}
     */
    public void addChildVerticalScrollHelper(View child, IVerticalScrollHelper childScrollHelper) {
        this.verticalScrollHelperWeakHashMap.put(child, childScrollHelper);
    }

    //todo:
    public void addChildHorizontalScrollHelper(View child, IHorizontalScrollHelper childScrollHelper) {
        this.horizontalScrollHelperWeakHashMap.put(child, childScrollHelper);
    }

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

    /**
     * set the height threshold of headerContainer view,
     * at which the spring container's state  will transfer between {@link #STATUS_TOP_RELEASE_TO_LINGER } and {@link #STATUS_TOP_PULL_TO_LINGER } during pulling,
     * and at which the headerContainer's height will remain while SpringContainer's state is {@link #STATUS_TOP_LINGERING}
     *
     * @param height height threshold of headerContainer view
     */
    public void setTopThreshold(int height) {
        TopThreshold = height;
    }

    /**
     * set the height threshold of footerContainer view,
     * at which the spring container's state  will transfer between {@link #STATUS_BOTTOM_RELEASE_TO_LINGER } and {@link #STATUS_BOTTOM_DRAG_TO_LINGER } during pushing,
     * and at which the footerContainer's height will remain while SpringContainer's state is {@link #STATUS_BOTTOM_LINGERING}
     *
     * @param height height threshold of footerContainer view
     */
    public void setBottomThreshold(int height) {
        BottomThreshold = height;
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


    /**
     * whether or not enable the 'spring' feature, if false, SpringContainer is just a {@link FrameLayout FrameLayout}
     *
     * @param enable
     */
    public void setSpringEnabled(boolean enable) {
        mSpringEnabled = enable;
    }

    /**
     * set the time duration between the time you finger is up and the time when top or bottom view shrinking to be gone
     * if duration is less than or equal to 0, the top or bottom view will stay as it is after your finger leaves.
     *
     * @param duration milliseconds
     */
    public void setReboundBaseTime(int duration) {
        this.mReboundBaseTime = duration;
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

    //when spring container is not able to pull or push, delay the detecting during touch-move for 3 times
    private int moveCounting = 0;
    private int delayCount = 3;

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
                moveCounting = 0;
                mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                mInitialYDown = (int) curY;
                mInitialXDown = (int) curX;
                pTarget = getTargetChild(curX, curY);
                touchDownChildScrollHelper = verticalScrollHelperWeakHashMap.get(pTarget);
                mAble2PullWhenTouchDown = mSpringEnabled && (isAbleToPull(touchDownChildScrollHelper) || headerContainer.getHeight() > 0);
                mAble2PushWhenTouchDown = mSpringEnabled && (isAbleToPush(touchDownChildScrollHelper) || footerContainer.getHeight() > 0);
                mEverConsumedMoveEvent = false;
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
                if (!mAble2PullWhenTouchDown && !mAble2PushWhenTouchDown) {
                    if (moveCounting < delayCount) {
                        moveCounting++;
                        break;
                    } else {
                        moveCounting = 0;
                    }
                }
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
                    }
                }

                boolean tmpAblePush = isAble2PushNow(curX, curY);
                if (tmpAblePush != mAble2PushWhenTouchDown) {
                    mAble2PushWhenTouchDown = tmpAblePush;//isAble2PushNow(curX, curY);
                    if (mAble2PushWhenTouchDown) {
                        mInitialYDown = (int) curY;
                    }
                }

                if (mAble2PullWhenTouchDown || mAble2PushWhenTouchDown) {

                    boolean consumed = false;
                    if (mAble2PullWhenTouchDown) {
                        consumed = consumed || updateHeaderLayout(distanceY);
                    }
                    if (mAble2PushWhenTouchDown) {
                        consumed = consumed || updateFooterLayout(-distanceY);
                    }
                    mEverConsumedMoveEvent = consumed || mEverConsumedMoveEvent;

                    if (consumed) {
                        return true;
                    }
                }

                break;
            }

            case MotionEvent.ACTION_UP:
            default: {
                if (mAble2PullWhenTouchDown) {
                    if (!headerView.onRelease(headerContainer)) {
                        hideHeader = createHideHeaderAnimation();
                        hideHeader.start();
                    }
                }
                if (mAble2PushWhenTouchDown) {
                    if (!footerView.onRelease(footerContainer)) {
                        hideFooter = createHideFooterAnimation();
                        hideFooter.start();
                    }
                }

                if (mEverConsumedMoveEvent && pTarget != null) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), MotionEvent.ACTION_CANCEL/*MotionEvent.ACTION_UP*/, event.getX(), event.getY(), 0);
                    if (pTarget != null)
                        pTarget.dispatchTouchEvent(cancelEvent);
                }

                mInitialYDown = 0;
                break;
            }
        }

        // may return false, ( e.g happened in headerView or footerView, or the content view is not interested in any event),
        // so subsequent Action_move or Action_up events would not be received.
        boolean sret = super.dispatchTouchEvent(event);
        return sret || mAble2PullWhenTouchDown || mAble2PushWhenTouchDown;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return super.onInterceptTouchEvent(event);
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

        int old = currentTopStatus;
        if (currentTopStatus == STATUS_TOP_LINGERING && headerLayoutParams.height < TopThreshold) {
            currentTopStatus = STATUS_TOP_LINGER_CANCELED;
            headerView.onStateChanged(old, currentTopStatus, null);
        }
        if (currentTopStatus != STATUS_TOP_LINGERING) {
            if (headerLayoutParams.height >= TopThreshold) {
                currentTopStatus = STATUS_TOP_RELEASE_TO_LINGER;
            } else {
                currentTopStatus = STATUS_TOP_PULL_TO_LINGER;
            }
            headerView.onStateChanged(old, currentTopStatus, null);
        }

        return true;

    }

    /**
     * @param distance distance of movement
     * @return consumed the move or not
     */
    private boolean updateFooterLayout(int distance) {
        if ((distance <= 0 && footerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop / 2)) {
            return false;
        }
        footerLayoutParams.height += (distance * 4 / 5);
        if (footerLayoutParams.height < 0)
            footerLayoutParams.height = 0;

        footerContainer.setLayoutParams(footerLayoutParams);
        footerView.onHeightChanged(footerLayoutParams.height);

        int old = currentBottomStatus;
        if (currentBottomStatus == STATUS_BOTTOM_LINGERING && footerLayoutParams.height < BottomThreshold) {
            currentBottomStatus = STATUS_BOTTOM_LINGER_CANCELED;
            footerView.onStateChanged(old, currentBottomStatus, null);
        }
        if (currentBottomStatus != STATUS_BOTTOM_LINGERING) {
            if (footerLayoutParams.height >= BottomThreshold) {
                currentBottomStatus = STATUS_BOTTOM_RELEASE_TO_LINGER;
            } else {
                currentBottomStatus = STATUS_BOTTOM_DRAG_TO_LINGER;
            }
            footerView.onStateChanged(old, currentBottomStatus, null);
        }

        return true;
    }

    /**
     * set a listener for state {@link #STATUS_TOP_LINGERING}
     *
     * @param listener set {@link RefreshingStateListener}
     */
    public void setOnRefreshingStateListener(RefreshingStateListener listener) {
        //setEnablePull2Refresh(true);
        if (listener == null) {
            headerContainer.setVisibility(View.INVISIBLE);
        } else {
            headerContainer.setVisibility(View.VISIBLE);
        }

        mRefreshAction = listener;
    }

    /**
     * set a listener for state {@link #STATUS_BOTTOM_LINGERING}
     *
     * @param listener set {@link LoadingStateListener}
     */
    public void setOnLoadListener(LoadingStateListener listener) {
        //setEnalbleDrag2LoadMore(true);
        if (listener == null) {
            footerContainer.setVisibility(View.INVISIBLE);
        } else {
            footerContainer.setVisibility(View.VISIBLE);
        }
        mDrag2LoadAction = listener;
    }

    /**
     * notify spring container to change state to {@link #STATUS_TOP_LINGER_FINISHED}
     */
    public void finishTopLingering() {
        isTopLingering = false;
        int old = currentTopStatus;
        currentTopStatus = STATUS_TOP_LINGER_FINISHED;
        transferState(headerView, old, currentTopStatus, new Runnable() {
            @Override
            public void run() {
                if (mInitialYDown <= 0) {
                    hideHeader = createHideHeaderAnimation();
                    hideHeader.start();
                }
            }
        });
//        headerView.onStateChanged(old, currentTopStatus);
//        StateTransferTimeInterval sat = stateTransferTimeIntervals.get(old);
//        if(sat != null && sat.to == currentTopStatus){
//            postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (mInitialYDown <= 0) {
//                        hideHeader = createHideHeaderAnimation();
//                        hideHeader.start();
//                    }
//                }
//            },sat.timeInterval);
//        } else {
//            if (mInitialYDown <= 0) {
//                hideHeader = createHideHeaderAnimation();
//                hideHeader.start();
//            }
//        }


    }

    private void transferState(ISpringView springView, int fromState, int toState, Runnable afterTransfer) {


        StateTransferTimeInterval sat = stateTransferTimeIntervals.get(fromState);

        if (sat != null && sat.to == toState && afterTransfer != null) {
            if (sat.timeInterval > 0) {
                postDelayed(afterTransfer, sat.timeInterval);
            } else {
                springView.onStateChanged(fromState, toState, afterTransfer);
            }
        } else {
            springView.onStateChanged(fromState, toState, null);
            afterTransfer.run();
        }
    }

    /**
     * notify spring container to change state to {@link #STATUS_BOTTOM_LINGER_FINISHED}
     */
    public void finishBottomLingering() {
        isBottomLingering = false;
        int old = currentBottomStatus;
        currentBottomStatus = STATUS_BOTTOM_LINGER_FINISHED;
        footerView.onStateChanged(old, currentBottomStatus, null);
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
            v = getTargetChild(x, y);
        }

        if (v != null) {
            return (headerLayoutParams == null ? false : headerLayoutParams.height > 0)
                    || isAbleToPull(verticalScrollHelperWeakHashMap.get(v));
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
            v = getTargetChild(x, y);
        }
        if (v != null) {
            return (footerLayoutParams == null ? false : footerLayoutParams.height > 0) || isAbleToPush(verticalScrollHelperWeakHashMap.get(v));
        }

        return true;
    }

    private boolean targetCanScrollHorizontally(int j) {
        if (pTarget == null) {
            return false;
        }
        IHorizontalScrollHelper horizontalScrollHelper = horizontalScrollHelperWeakHashMap.get(pTarget);
        if (horizontalScrollHelper == null) {
            return false;
        }

        if (j > 0) {
            return horizontalScrollHelper.canScrollRight();
        } else {
            return horizontalScrollHelper.canScrollLeft();
        }

    }


    protected Animator createHideHeaderAnimation() {

        if (hideHeader != null) {
            hideHeader.cancel();
            hideHeader = null;
        }

        if (headerLayoutParams.height > TopThreshold && mRefreshAction != null) {

            hideHeader = createHeightAnimation(headerContainer, headerLayoutParams.height, TopThreshold, true);
            hideHeader.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

                    if (currentTopStatus != STATUS_TOP_LINGERING) {
                        int old = currentTopStatus;
                        currentTopStatus = STATUS_TOP_LINGERING;
                        transferState(headerView, old, currentTopStatus, new Runnable() {
                            @Override
                            public void run() {
                                if (mRefreshAction != null) {
                                    if (!isTopLingering) {
                                        isTopLingering = true;
                                        mRefreshAction.onRefreshing(SpringContainer.this);
                                    }

                                } else {
                                    SpringContainer.this.finishTopLingering();
                                }
                            }
                        });
//                        headerView.onStateChanged(old, currentTopStatus);
//                        StateTransferTimeInterval sat = stateTransferTimeIntervals.get(old);
//                        if(sat != null && sat.to == currentBottomStatus){
//                            postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (mRefreshAction != null) {
//                                        if (!isTopLingering) {
//                                            isTopLingering = true;
//                                            mRefreshAction.onRefreshing(SpringContainer.this);
//                                        }
//
//                                    } else {
//                                        SpringContainer.this.finishTopLingering();
//                                    }
//                                }
//                            },sat.timeInterval);
//                        } else {
//                            if (mRefreshAction != null) {
//                                if (!isTopLingering) {
//                                    isTopLingering = true;
//                                    mRefreshAction.onRefreshing(SpringContainer.this);
//                                }
//
//                            } else {
//                                SpringContainer.this.finishTopLingering();
//                            }
//                        }
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

        if (footerLayoutParams.height > BottomThreshold && mDrag2LoadAction != null) {

            hideFooter = createHeightAnimation(footerContainer, footerLayoutParams.height, BottomThreshold, false);
            hideFooter.addListener(new AnimatorListenerAdapter() {
                boolean canceled;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

                    if (currentBottomStatus != STATUS_BOTTOM_LINGERING) {
                        int old = currentBottomStatus;
                        currentBottomStatus = STATUS_BOTTOM_LINGERING;
                        footerView.onStateChanged(old, currentBottomStatus, null);
                        if (mDrag2LoadAction != null) {
                            if (!isBottomLingering) {
                                isBottomLingering = true;
                                mDrag2LoadAction.onLoading(SpringContainer.this);
                            }

                        } else {
                            SpringContainer.this.finishBottomLingering();
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
        int duration = mReboundBaseTime + (int) Math.sqrt(abs);

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

    private boolean pointInChildView(float x, float y, View child) {
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
     * TODO: currently only  STATUS_TOP_LINGERING and STATUS_BOTTOM_LINGERING is supported.
     * make SpringContainer transfer to the target state
     *
     * @param state target state SpringContainer will transfer to
     */
    public void setTopState(int state) {
        stopHeaderFooterAnim();
        switch (state) {
            case STATUS_TOP_LINGERING:
                headerLayoutParams.height = TopThreshold;
                headerContainer.setLayoutParams(headerLayoutParams);
                headerView.onHeightChanged(TopThreshold);
                int old = currentTopStatus;
                currentTopStatus = STATUS_TOP_LINGERING;
                headerView.onStateChanged(old, currentTopStatus, new Runnable() {
                    @Override
                    public void run() {
                        if (mRefreshAction != null) {
                            mRefreshAction.onRefreshing(SpringContainer.this);
                        }
                    }
                });

                break;

            default:
                break;
        }
    }

    public void setBottomState(int state) {
        stopHeaderFooterAnim();
        switch (state) {
            case STATUS_BOTTOM_LINGERING:
                footerLayoutParams.height = BottomThreshold;
                footerContainer.setLayoutParams(footerLayoutParams);
                footerView.onHeightChanged(BottomThreshold);
                int old = currentBottomStatus;
                currentBottomStatus = STATUS_BOTTOM_LINGERING;
                footerView.onStateChanged(old, currentBottomStatus, new Runnable() {
                    @Override
                    public void run() {
                        if (mDrag2LoadAction != null) {
                            mDrag2LoadAction.onLoading(SpringContainer.this);
                        }
                    }
                });

                break;
            default:
                break;
        }

    }

    public int getTopState() {
        return currentTopStatus;
    }

    public int getBottomState() {
        return currentBottomStatus;
    }

    /**
     * fromState and toState must be adjacent
     *
     * @param fromState
     * @param toState
     * @param timeInterval
     */
    public void setStateTransferTimeIntervale(int fromState, int toState, long timeInterval) {
//        if((fromState + 1) != toState){
//            return;
//        }
        StateTransferTimeInterval o = new StateTransferTimeInterval();
        o.from = fromState;
        o.to = toState;
        o.timeInterval = timeInterval;
        stateTransferTimeIntervals.put(fromState, o);
    }

    private SparseArray<StateTransferTimeInterval> stateTransferTimeIntervals = new SparseArray<>(5);

    class StateTransferTimeInterval {
        int from;
        int to;
        long timeInterval;
    }
}

