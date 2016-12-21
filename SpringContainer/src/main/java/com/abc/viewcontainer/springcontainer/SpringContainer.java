package com.abc.viewcontainer.springcontainer;

/**
 * Created by zhangzhenwei on 16/7/26.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
//import android.util.Log;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.abc.viewcontainer.R;
import com.abc.viewcontainer.verticalscrollhelper.GridViewVerticalScrollHelper;
import com.abc.viewcontainer.verticalscrollhelper.IVerticalScrollHelper;
import com.abc.viewcontainer.verticalscrollhelper.ListViewVerticalScrollHelper;
import com.abc.viewcontainer.verticalscrollhelper.RecyclerViewVerticalScrollHelper;
import com.abc.viewcontainer.verticalscrollhelper.ScrollViewVerticalScrollHelper;
import com.abc.viewcontainer.verticalscrollhelper.WebViewVerticalScrollHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by mae on 15/11/21.
 */
public class SpringContainer extends FrameLayout {

    private static String TAG = SpringContainer.class.getSimpleName();
    private static final String PREFERENCE_NAME = "com.abc.spring.container";
    private static final String KEY_UPDATED_AT = "updated_at";

    public String PULL_TO_RELEASE_TIP = "下拉可以刷新";
    public String RELEASE_TO_REFRESH_TIP = "释放立即刷新";
    public String REFRESHING_TIP = "正在刷新…";

    public String DRAG_TO_LOAD_TIP = "继续上拉加载更多";
    public String RELEASE_TO_LOAD_TIP = "释放加载更多";
    public String LOADING_TIP = "正在加载…";

    private boolean mSpringEnabled = true;

    public static final int STATUS_PULL_TO_REFRESH = 0;// pulling
    public static final int STATUS_RELEASE_TO_REFRESH = 1;
    public static final int STATUS_REFRESHING = 2;
    public static final int STATUS_REFRESH_FINISHED = 3;
    public static final int STATUS_REFRESH_CANCELED = 4;

    //=================drag 2 load more ===============//
    public static final int STATUS_DRAG_TO_LOAD = STATUS_PULL_TO_REFRESH;
    public static final int STATUS_RELEASE_TO_LOAD = STATUS_RELEASE_TO_REFRESH;
    public static final int STATUS_LOADING = STATUS_REFRESHING;
    public static final int STATUS_LOAD_FINISHED = STATUS_REFRESH_FINISHED;
    public static final int STATUS_LOAD_CANCELED = STATUS_REFRESH_CANCELED;
    //=================drag 2 load more ===============//

    // time interval by milliseconds
    public static final long ONE_MINUTE = 60 * 1000;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    public static final long ONE_MONTH = 30 * ONE_DAY;
    public static final long ONE_YEAR = 12 * ONE_MONTH;

    private Pull2RefreshListener mRefreshAction;
    private Drag2LoadListener mDrag2LoadAction;

    /**
     * used to store the time of last update
     */
    private SharedPreferences preferences;

    /**
     * height threshold of header view,
     * at which the spring container will transfer to {@link #STATUS_RELEASE_TO_REFRESH } state
     */
    private int HeightThreshold = 240;

    /**
     * header view
     */
    private View header;
    private ViewGroup.LayoutParams headerLayoutParams;
    private Animator hideHeader;

    /**
     * header back ground, can be zoomed while pulling
     */
    View headerBack;


    private View footer;
    private ViewGroup.LayoutParams footerLayoutParams;
    private Animator hideFooter;

    private TextView footerDes;
    private ProgressBar footerProgressBar;

    /**
     * Content View
     */
    private List<View> contentViews = new ArrayList<>(3);

    IVerticalScrollHelper childScrollHelper;
    public void setChildScrollHelper(IVerticalScrollHelper childScrollHelper) {
        this.childScrollHelper = childScrollHelper;
    }


    /**
     * progress bar while refreshing
     */
    private ProgressBar progressBar;
    private ImageView arrow;
    private TextView description;
    private TextView updateAt;
    private long lastUpdateTime;

    /**
     * different pages have different ids
     */
    private String mId4UpdateTime = "";

    private int currentRefreshingStatus = STATUS_REFRESH_FINISHED;
    private int currentLoadingStatus = STATUS_LOAD_FINISHED;
    private boolean isLoading = false;

    private int lastRefreshingStatus = currentRefreshingStatus;
    private int lastLoadingStatus = currentLoadingStatus;
    private boolean isRefreshing = false;

    private int mInitialYDown;
    private int touchSlop;


    public SpringContainer(Context context) {
        this(context, null);
    }

    public SpringContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public SpringContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (null != attrs) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RefreshableListView);
            mSpringEnabled = attributes.getBoolean(R.styleable.RefreshableListView_springEnabled, true);
            attributes.recycle();
        }

        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        //preferences = PreferenceManager.getDefaultSharedPreferences(context);

        LayoutInflater.from(context).inflate(R.layout.springcontainer_header, this, true);
        header = findViewById(R.id.spring_container_header);
        headerLayoutParams = header.getLayoutParams();

        LayoutInflater.from(context).inflate(R.layout.springcontainer_footer, this, true);
        footer = findViewById(R.id.spring_container_footer);
        footerLayoutParams = footer.getLayoutParams();
        footerDes = (TextView) footer.findViewById(R.id.footer_description);
        footerProgressBar = (ProgressBar) footer.findViewById(R.id.footer_progress_bar);

        progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        arrow = (ImageView) header.findViewById(R.id.arrow);
        description = (TextView) header.findViewById(R.id.description);
        updateAt = (TextView) header.findViewById(R.id.updated_at);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        refreshUpdatedAtValue();
    }

    public List<View> getContentView() {
        return contentViews;
    }


    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0
                && index < 0
                && child.getId() != R.id.spring_container_header
                && child.getId() != R.id.spring_container_footer) {

            if (child.getId() == R.id.spring_container_header_background) {
                headerBack = child;
                super.addView(child, 0, params);
            } else {
                super.addView(child, getChildCount() - 1, params);
                contentViews.add(child);
            }

        } else {
            super.addView(child, index, params);
        }

        if (child instanceof RecyclerView) {
            setChildScrollHelper(new RecyclerViewVerticalScrollHelper((RecyclerView) child));
        } else if (child instanceof ListView) {
            setChildScrollHelper(new ListViewVerticalScrollHelper((ListView) child));
        } else if (child instanceof ScrollView) {
            setChildScrollHelper(new ScrollViewVerticalScrollHelper((ScrollView) child));
        } else if (child instanceof GridView) {
            setChildScrollHelper(new GridViewVerticalScrollHelper((GridView) child));
        } else if (child instanceof WebView) {
            setChildScrollHelper(new WebViewVerticalScrollHelper((WebView) child));
        }

    }


    public void setSpringEnabled(boolean enable) {
        mSpringEnabled = enable;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int contentOffset = 0;
        if (header.getHeight() > 0) {
            contentOffset = header.getHeight();
        } else if (footer.getHeight() > 0) {
            contentOffset = -footer.getHeight();
            footer.offsetTopAndBottom(this.getHeight() - footer.getHeight());
        }

        if (!contentViews.isEmpty())
            for (View v : contentViews)
                v.offsetTopAndBottom(contentOffset);

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

        boolean able2pull = mSpringEnabled && (isAbleToPull() || header.getHeight() > 0);
        boolean able2push = mSpringEnabled && (isAble2Push() || footer.getHeight() > 0);
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

        return super.onInterceptTouchEvent(event);
    }

    int mScrollPointerId;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        stopHeaderFooterAnim();
        boolean ret = false;

        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);

        boolean able2pull = mSpringEnabled && (isAbleToPull() || header.getHeight() > 0);
        boolean able2push = mSpringEnabled && (isAble2Push() || footer.getHeight() > 0);

        if (able2pull || able2push) {
            int distance = 0;
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mScrollPointerId = MotionEventCompat.getPointerId(event, 0);
                    mInitialYDown = (int) (event.getY() + 0.5f);
                }
                return true;

                case MotionEvent.ACTION_POINTER_DOWN: {
                    mScrollPointerId = MotionEventCompat.getPointerId(event, actionIndex);
                    mInitialYDown = (int) (MotionEventCompat.getY(event, actionIndex) + 0.5f);
                }
                break;

                case MotionEvent.ACTION_MOVE: {
                    final int index = MotionEventCompat.findPointerIndex(event, mScrollPointerId);
                    if (index < 0) {
                        Log.e(TAG, "Error processing SpringContainer; pointer index for id " +
                                mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                        return false;
                    }
                    int yMove = (int) (MotionEventCompat.getY(event, index) + 0.5f);
                    distance = yMove - mInitialYDown;
                    if (able2pull) {
                        updateHeaderLayout(distance);
                    }
                    if (able2push) {
                        updateFooterLayout(-distance);
                    }
                    mInitialYDown = yMove;
                }
                break;

                case MotionEventCompat.ACTION_POINTER_UP: {
                    onPointerUp(event);
                }
                break;

                case MotionEvent.ACTION_UP:
                default: {
                    if (able2pull) {
                        hideHeader = createHideHeaderAnimaton();
                        hideHeader.start();
                    }
                    if (able2push) {
                        hideFooter = createHideFooterAnimation();
                        hideFooter.start();
                    }

                    mInitialYDown = 0;
                }
                break;
            }

            if (able2pull) {
                //更新下拉头中的信息
                if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH || currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
                    updateHeaderView();
                    lastRefreshingStatus = currentRefreshingStatus;
                    // 当前正处于下拉或释放状态，通过返回true屏蔽掉 content view 的滚动事件
                    if (distance < 0) {
                        ret = true;
                    }
                }
            }

            if (able2push) {
                //TODO: update footer
                if (currentLoadingStatus == STATUS_DRAG_TO_LOAD || currentLoadingStatus == STATUS_RELEASE_TO_LOAD) {
                    updateFooterView();
                    lastLoadingStatus = currentLoadingStatus;
                    if (distance > 0) {
                        ret = true;
                    }
                }
            }

        }

        if (ret) {
            return true;
        }

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

    private void updateHeaderLayout(int distance) {
        // 如果手指是上滑状态，并且下拉刷新view是完全隐藏的，就屏蔽下拉事件
        if (distance <= 0 && headerLayoutParams.height <= 0) {
            return;
        }
        headerLayoutParams.height += (distance * 4 / 5);
        if (headerLayoutParams.height < 0)
            headerLayoutParams.height = 0;


        if (headerBack != null) {
            headerBack.setPivotY(0);
            headerBack.setPivotX(headerBack.getHeight() / 2);

            float scale = headerBack.getScaleY();
//            if (headerLayoutParams.height > HeightThreshold) {
            scale += distance / (float) HeightThreshold;
//            } else {
//                scale += headerLayoutParams.height / (float) HeightThreshold;
//            }
            if (scale > 2)
                scale = 2;
            else if (scale < 1)
                scale = 1;

            headerBack.setScaleX(scale);
            headerBack.setScaleY(scale);
        }

        header.setLayoutParams(headerLayoutParams);
        if (currentRefreshingStatus == STATUS_REFRESHING && headerLayoutParams.height < HeightThreshold) {
            currentRefreshingStatus = STATUS_REFRESH_CANCELED;
            //TODO: cancelRefresh():
        }
        if (currentRefreshingStatus != STATUS_REFRESHING) {
            if (headerLayoutParams.height >= HeightThreshold) {
                currentRefreshingStatus = STATUS_RELEASE_TO_REFRESH;
            } else {
                currentRefreshingStatus = STATUS_PULL_TO_REFRESH;
            }
        }


    }

    private void updateFooterLayout(int distance) {
        if (distance <= 0 && footerLayoutParams.height <= 0) {
            return;
        }
        footerLayoutParams.height += (distance * 4 / 5);
        if (footerLayoutParams.height < 0)
            footerLayoutParams.height = 0;

        footer.setLayoutParams(footerLayoutParams);
        if (currentLoadingStatus == STATUS_LOADING && footerLayoutParams.height < HeightThreshold) {
            currentLoadingStatus = STATUS_LOAD_CANCELED;
            //TODO: cancelRefresh():
        }
        if (currentLoadingStatus != STATUS_LOADING) {
            if (footerLayoutParams.height >= HeightThreshold) {
                currentLoadingStatus = STATUS_RELEASE_TO_LOAD;
            } else {
                currentLoadingStatus = STATUS_DRAG_TO_LOAD;
            }
        }
    }


    public void setOnRefreshListener(String tag, Pull2RefreshListener listener) {
        //setEnablePull2Refresh(true);
        if (listener == null) {
            header.setVisibility(View.INVISIBLE);
        } else {
            header.setVisibility(View.VISIBLE);
        }

        mRefreshAction = listener;
        mId4UpdateTime = tag;
    }

    public void setOnLoadListener(Drag2LoadListener listener) {
        //setEnalbleDrag2LoadMore(true);
        if (listener == null) {
            footer.setVisibility(View.INVISIBLE);
        } else {
            footer.setVisibility(View.VISIBLE);
        }
        mDrag2LoadAction = listener;
    }

    /**
     * notify spring container to change state
     */
    public void finishRefreshing() {
        isRefreshing = false;
        currentRefreshingStatus = STATUS_REFRESH_FINISHED;
        preferences.edit().putLong(KEY_UPDATED_AT + mId4UpdateTime, System.currentTimeMillis()).commit();
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

    private boolean isAbleToPull() {
        boolean ableToPull = true;
        if (childScrollHelper != null) {
            ableToPull = !childScrollHelper.canScrollDown();
        }
        if (footerLayoutParams != null) {
            ableToPull &= !(footerLayoutParams.height > 0);
        }
        return ableToPull;

    }

    private boolean isAble2Push() {
        boolean able2Push = true;
        if (childScrollHelper != null) {
            able2Push = !childScrollHelper.canScrollUp();
        }

        if (headerLayoutParams != null) {
            able2Push = (able2Push && !(headerLayoutParams.height > 0));
        }
        return able2Push;

    }

    /**
     * 更新下拉头中的信息。
     */
    private void updateHeaderView() {
        if (lastRefreshingStatus != currentRefreshingStatus) {
            if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH) {
                description.setText(PULL_TO_RELEASE_TIP);
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
                description.setText(RELEASE_TO_REFRESH_TIP);
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentRefreshingStatus == STATUS_REFRESHING) {
                description.setText(REFRESHING_TIP);
                progressBar.setVisibility(View.VISIBLE);
                arrow.clearAnimation();
                arrow.setVisibility(View.GONE);
            }
            refreshUpdatedAtValue();
        }
    }

    /**
     * 根据当前的状态来旋转箭头。
     */
    private void rotateArrow() {
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        float fromDegrees = 0f;
        float toDegrees = 0f;
        if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH) {
            fromDegrees = 180f;
            toDegrees = 360f;
        } else if (currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
            fromDegrees = 0f;
            toDegrees = 180f;
        }
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(200);
        animation.setFillAfter(true);
        arrow.startAnimation(animation);
    }

    /**
     * 刷新下拉头中上次更新时间的文字描述。
     */
    private void refreshUpdatedAtValue() {
        lastUpdateTime = preferences.getLong(KEY_UPDATED_AT + mId4UpdateTime, -1);
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastUpdateTime;
        long timeIntoFormat;
        String updateAtValue;
        if (lastUpdateTime == -1) {
            updateAtValue = getResources().getString(R.string.not_updated_yet);
        } else if (timePassed < 0) {
            updateAtValue = getResources().getString(R.string.time_error);
        } else if (timePassed < ONE_MINUTE) {
            updateAtValue = getResources().getString(R.string.updated_just_now);
        } else if (timePassed < ONE_HOUR) {
            timeIntoFormat = timePassed / ONE_MINUTE;
            String value = timeIntoFormat + "分钟";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_DAY) {
            timeIntoFormat = timePassed / ONE_HOUR;
            String value = timeIntoFormat + "小时";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_MONTH) {
            timeIntoFormat = timePassed / ONE_DAY;
            String value = timeIntoFormat + "天";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_YEAR) {
            timeIntoFormat = timePassed / ONE_MONTH;
            String value = timeIntoFormat + "个月";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        } else {
            timeIntoFormat = timePassed / ONE_YEAR;
            String value = timeIntoFormat + "年";
            updateAtValue = String.format(getResources().getString(R.string.updated_at), value);
        }
        updateAt.setText(updateAtValue);
    }

    private void updateFooterView() {
        //Log.w(TAG, "updateFooterView");

        if (lastLoadingStatus != currentLoadingStatus) {
            if (currentLoadingStatus == STATUS_DRAG_TO_LOAD) {
                footerDes.setText(DRAG_TO_LOAD_TIP);
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_RELEASE_TO_LOAD) {
                footerDes.setText(RELEASE_TO_LOAD_TIP);
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_LOADING) {
                footerDes.setText(LOADING_TIP);
                footerProgressBar.setVisibility(View.VISIBLE);
            }
        }

    }

    Animator createHideHeaderAnimaton() {

        if (hideHeader != null) {
            hideHeader.cancel();
            hideHeader = null;
        }

        if (headerLayoutParams.height > HeightThreshold && mRefreshAction != null) {

            hideHeader = createHeightAnimation(header, headerLayoutParams.height, HeightThreshold);
            hideHeader.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

                    if (headerBack != null) {
                        headerBack.setScaleX(1);
                        headerBack.setScaleY(1);
                    }

                    if (currentRefreshingStatus != STATUS_REFRESHING) {
                        currentRefreshingStatus = STATUS_REFRESHING;
                        updateHeaderView();
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

                    if (headerBack != null) {
                        headerBack.setScaleX(1);
                        headerBack.setScaleY(1);
                    }

                }

            });
        } else {

            hideHeader = createHeightAnimation(header, headerLayoutParams.height, 0);
            hideHeader.addListener(new AnimatorListenerAdapter() {
                boolean canceled = false;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }
                    if (headerBack != null) {
                        headerBack.setScaleX(1);
                        headerBack.setScaleY(1);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;

                    if (headerBack != null) {
                        headerBack.setScaleX(1);
                        headerBack.setScaleY(1);
                    }
                }
            });
        }

        return hideHeader;

    }


    Animator createHideFooterAnimation() {

        if (hideFooter != null) {
            hideFooter.cancel();
            hideFooter = null;
        }

        if (footerLayoutParams.height > HeightThreshold && mDrag2LoadAction != null) {

            hideFooter = createHeightAnimation(footer, footerLayoutParams.height, HeightThreshold);
            hideFooter.addListener(new AnimatorListenerAdapter() {
                boolean canceled;

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (canceled) {
                        return;
                    }

                    if (currentLoadingStatus != STATUS_REFRESHING) {
                        currentLoadingStatus = STATUS_REFRESHING;
                        updateFooterView();
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

            hideFooter = createHeightAnimation(footer, footerLayoutParams.height, 0);

        }

        return hideFooter;

    }

    Animator createHeightAnimation(View view, int from, int to) {
        ValueAnimator animation = ValueAnimator.ofInt(from, to);
        int abs = Math.abs(from - to);

        //int duration = 300 + (int)Math.log(abs + Math.E);
        int duration = 300 + (int) Math.sqrt(abs);

        animation.setDuration(duration);

        animation.addUpdateListener(new HeightUpdateListener(view));
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        //animation.setInterpolator(new DecelerateInterpolator());

        return animation;
    }


    class HeightUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        View view;

        public HeightUpdateListener(View v) {
            view = v;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int height = (int) animation.getAnimatedValue();

            if (view != null && view.getLayoutParams() != null) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = height;
                view.setLayoutParams(lp);
            }
        }

    }

    void stopHeaderFooterAnim() {
        if (hideHeader != null) {
            hideHeader.cancel();
            hideHeader = null;
        }

        if (hideFooter != null) {
            hideFooter.cancel();
            hideFooter = null;
        }

    }


}

