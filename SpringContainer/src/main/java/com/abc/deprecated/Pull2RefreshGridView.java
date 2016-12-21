package com.abc.deprecated;

/**
 * Created by zhangzhenwei on 16/7/26.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abc.viewcontainer.R;


/**
 * Created by mae on 15/11/21.
 */
public class Pull2RefreshGridView extends LinearLayout {

    private static String TAG = Pull2RefreshGridView.class.getSimpleName();

    private boolean enablePull2Refresh = false;
    private boolean enalbleDrag2LoadMore = false;

    // 下拉状态
    public static final int STATUS_PULL_TO_REFRESH = 0;
    // 释放立即刷新状态
    public static final int STATUS_RELEASE_TO_REFRESH = 1;
    // 正在刷新状态
    public static final int STATUS_REFRESHING = 2;
    // 刷新完成或未刷新状态
    public static final int STATUS_REFRESH_FINISHED = 3;
    // 取消刷新
    public static final int STATUS_REFRESH_CANCELED = 4;


    //=================drag 2 load more ===============//
    public static final int STATUS_DRAG_TO_LOAD = STATUS_PULL_TO_REFRESH;
    public static final int STATUS_RELEASE_TO_LOAD = STATUS_RELEASE_TO_REFRESH;
    public static final int STATUS_LOADING = STATUS_REFRESHING;
    public static final int STATUS_LOAD_FINISHED = STATUS_REFRESH_FINISHED;
    public static final int STATUS_LOAD_CANCELED = STATUS_REFRESH_CANCELED;
    //=================drag 2 load more ===============//


    /**
     * 下拉头部回滚的速度
     */
    public static final int SCROLL_SPEED = -20;

    /**
     * 一分钟的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_MINUTE = 60 * 1000;

    /**
     * 一小时的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;

    /**
     * 一天的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /**
     * 一月的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_MONTH = 30 * ONE_DAY;

    /**
     * 一年的毫秒值，用于判断上次的更新时间
     */
    public static final long ONE_YEAR = 12 * ONE_MONTH;

    /**
     * 上次更新时间的字符串常量，用于作为SharedPreferences的键值
     */
    private static final String UPDATED_AT = "updated_at";

    /**
     * 下拉刷新的回调接口
     */
    private PullToRefreshListener mRefreshAction;
    private Drag2LoadListener mDrag2LoadAction;

    /**
     * 用于存储上次更新时间
     */
    private SharedPreferences preferences;

    /**
     * 下拉头的高度阈值，大于此值时松手刷新
     */
    private int HeightThreshold = 180;
    /**
     * 下拉头的View
     */
    private View header;
    private ViewGroup.LayoutParams headerLayoutParams;

    /**
     * 下拉头的View
     */
    private View footer;
    private ViewGroup.LayoutParams footerLayoutParams;
    private TextView footerDes;
    private ProgressBar footerProgressBar;

    /**
     * 需要去下拉刷新的ListView
     */
    private GridView gridView;

    /**
     * 刷新时显示的进度条
     */
    private ProgressBar progressBar;

    /**
     * 指示下拉和释放的箭头
     */
    private ImageView arrow;

    /**
     * 指示下拉和释放的文字描述
     */
    private TextView description;

    /**
     * 上次更新时间的文字描述
     */
    private TextView updateAt;

    private LinearLayout emptyView;
    private TextView emptyMessageView;
    private Button empptyActionBtn;
    private Button empptySubActionBtn;


    /**
     * 上次更新时间的毫秒值
     */
    private long lastUpdateTime;

    /**
     * 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分
     */
    private String mId = "";

    /**
     * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
     * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
     */
    private int currentRefreshingStatus = STATUS_REFRESH_FINISHED;
    private int currentLoadingStatus = STATUS_LOAD_FINISHED;
    ;

    /**
     * 记录上一次的状态是什么，避免进行重复操作
     */
    private int lastRereshingStatus = currentRefreshingStatus;
    private int lastLoadingStatus = currentLoadingStatus;

    /**
     * 手指按下时的屏幕纵坐标
     */
    private float yDown;

    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;

    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;

    /**
     * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
     */
    //private boolean ableToPull;

    boolean mShowEmptyView;
    int mColumnWidth;

    public Pull2RefreshGridView(Context context) {
        this(context, null);
    }

    public Pull2RefreshGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 下拉刷新控件的构造函数，会在运行时动态添加一个下拉头的布局。
     *
     * @param context
     * @param attrs
     */
    public Pull2RefreshGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (null != attrs) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RefreshableListView);
            mShowEmptyView = attributes.getBoolean(R.styleable.RefreshableListView_enableEmptyView, true);
            mColumnWidth = attributes.getDimensionPixelSize(R.styleable.RefreshableListView_columnWidth, 0);
            attributes.recycle();
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        LayoutInflater.from(context).inflate(R.layout.springcontainer_header, this, true);

        header = findViewById(R.id.pull_to_refresh_head);
        headerLayoutParams = header.getLayoutParams();

        footer = findViewById(R.id.drag_to_refresh_footer);
        footerLayoutParams = footer.getLayoutParams();
        footerDes = (TextView) footer.findViewById(R.id.footer_description);
        footerProgressBar = (ProgressBar) footer.findViewById(R.id.footer_progress_bar);

        progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        arrow = (ImageView) header.findViewById(R.id.arrow);
        description = (TextView) header.findViewById(R.id.description);
        updateAt = (TextView) header.findViewById(R.id.updated_at);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gridView = (GridView) findViewById(R.id.refreshable_inner_grid_view);

        if (mColumnWidth > 0) {
            gridView.setColumnWidth(mColumnWidth);
        }

        emptyView = (LinearLayout) findViewById(R.id.empty_view);
        emptyMessageView = (TextView) emptyView.findViewById(R.id.message);
        empptyActionBtn = (Button) emptyView.findViewById(R.id.action);
        empptySubActionBtn = (Button) emptyView.findViewById(R.id.sub_action);
        if (mShowEmptyView)
            gridView.setEmptyView(emptyView);
        refreshUpdatedAtValue();
        setOrientation(VERTICAL);
        //addView(header, 0);
    }

    public GridView getGridView() {
        return gridView;
    }

    public LinearLayout getEmptyView() {
        return emptyView;
    }

    public void disableEmptyView(boolean disable) {
        if (disable) {
            gridView.setEmptyView(null);
        } else {
            gridView.setEmptyView(emptyView);
        }
    }

    public void setEnablePull2Refresh(boolean enale) {
        enablePull2Refresh = enale;
    }

    public void setEnalbleDrag2LoadMore(boolean enable) {
        enalbleDrag2LoadMore = enable;
    }

    public void setEmptyMessage(String message) {
        emptyMessageView.setText(message);
    }

    public void setEmpptyAction(String name, View.OnClickListener action) {
        empptyActionBtn.setVisibility(VISIBLE);
        empptyActionBtn.setText(name);
        empptyActionBtn.setOnClickListener(action);
    }

    public void setSubEmpptyAction(String name, View.OnClickListener action) {
        empptySubActionBtn.setVisibility(VISIBLE);
        empptySubActionBtn.setText(name);
        empptySubActionBtn.setOnClickListener(action);
    }

    /**
     * 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

//        listView.setBottom(this.getHeight() - footer.getHeight());
        footer.offsetTopAndBottom(-footer.getHeight());
        gridView.offsetTopAndBottom(-footer.getHeight());

    }

    /**
     * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (enablePull2Refresh && (isAbleToPull() || header.getHeight() > 0)) {
            int distance = 0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    distance = (int) (yMove - yDown);
                    // 如果手指是上滑状态，并且下拉刷新view是完全隐藏的，就屏蔽下拉事件
                    if ((distance <= 0 && headerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop)) {
                        // Log.w(TAG, "onInterceptTouchEvent, ACTION_MOVE: false");
                        break;
                    } else {
                        yDown = yMove;
                        return true;
                    }
            }

        }

        if (enalbleDrag2LoadMore && (isAble2Drag() || footer.getHeight() > 0)) {
            int distance = 0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.w(TAG, "onInterceptTouchEvent, ACTION_DOWN");
                    yDown = event.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    Log.w(TAG, "onInterceptTouchEvent, ACTION_MOVE");
                    float yMove = event.getRawY();
                    distance = (int) (-yMove + yDown);
                    Log.w(TAG, "intercept, distance:" + distance);
                    if ((distance <= 0 && footerLayoutParams.height <= 0) || (distance > 0 && distance < touchSlop)) {
                        Log.w(TAG, "onInterceptTouchEvent, ACTION_MOVE: false");
                        break;
                    } else {
                        yDown = yMove;
                        Log.w(TAG, "onInterceptTouchEvent, ACTION_MOVE: true");
                        return true;
                    }
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //     Log.e(TAG, "grid view top:" + gridView.getTop() + " header height:" + header.getHeight());

        if (enablePull2Refresh && (isAbleToPull() || header.getHeight() > 0)) {
            int distance = 0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    distance = (int) (yMove - yDown);
                    // 如果手指是上滑状态，并且下拉刷新view是完全隐藏的，就屏蔽下拉事件
                    if (distance <= 0 && headerLayoutParams.height <= 0) {
                        break;
                    }
                    headerLayoutParams.height += (distance / 2);
                    if (headerLayoutParams.height < 0)
                        headerLayoutParams.height = 0;

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
                    yDown = yMove;
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    if (headerLayoutParams.height > HeightThreshold) {
                        // 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
                        new RefreshingTask().execute();
                    } else if (headerLayoutParams.height <= HeightThreshold) {
                        // 松手时如果是下拉状态，就去调用隐藏下拉头的任务
                        new HideHeaderTask().execute();
                    }
                    break;
            }
            //更新下拉头中的信息
            if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH || currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
                updateHeaderView();
                // 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
                gridView.setPressed(false);
                gridView.setFocusable(false);
                gridView.setFocusableInTouchMode(false);
                lastRereshingStatus = currentRefreshingStatus;
                // 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
                if (distance < 0) {
                    // Log.d(TAG, "distance < 0, ontouch return false");
                    return true;
                }
            }
        }

        if (enalbleDrag2LoadMore && (isAble2Drag() || footer.getHeight() > 0)) {
//            listView.setSelection(listView.getLastVisiblePosition());
            int distance = 0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //Log.w(TAG,"onTouchEvent,ACTION_DOWN");
                    yDown = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    //Log.w(TAG,"onTouchEvent,ACTION_MOVE");
                    float yMove = event.getRawY();
                    distance = (int) (-yMove + yDown);
                    if (distance <= 0 && footerLayoutParams.height <= 0) {
                        break;
                    }
                    footerLayoutParams.height += (distance / 2);
                    Log.w("footerLayoutParams", "height:" + footerLayoutParams.height);
                    Log.w("listview", "height:" + gridView.getHeight());
                    if (footerLayoutParams.height < 0)
                        footerLayoutParams.height = 0;

                    footer.setLayoutParams(footerLayoutParams);
                    if (currentLoadingStatus == STATUS_LOADING && footerLayoutParams.height < HeightThreshold) {
                        currentLoadingStatus = STATUS_LOAD_CANCELED;
                        //TODO: cancelRefresh():
                    }
                    if (currentLoadingStatus != STATUS_LOADING) {
//                        Log.w(TAG, "layout height:" + headerLayoutParams.height + " height:" + header.getHeight());
                        if (footerLayoutParams.height >= HeightThreshold) {
                            currentLoadingStatus = STATUS_RELEASE_TO_LOAD;
                        } else {
                            currentLoadingStatus = STATUS_DRAG_TO_LOAD;
                        }
                    }
                    yDown = yMove;
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    Log.w(TAG, "default");
                    if (footerLayoutParams.height > HeightThreshold) {
                        // 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
                        new LoadingTask().execute();
                    } else if (footerLayoutParams.height <= HeightThreshold) {
                        // 松手时如果是下拉状态，就去调用隐藏下拉头的任务
                        new HideFooterTask().execute();
                    }
                    break;
            }
            //TODO: update footer
            if (currentLoadingStatus == STATUS_DRAG_TO_LOAD || currentLoadingStatus == STATUS_RELEASE_TO_LOAD) {
                updateFooterView();
                gridView.setPressed(false);
                gridView.setFocusable(false);
                gridView.setFocusableInTouchMode(false);
                lastLoadingStatus = currentLoadingStatus;
                if (distance < 0) {
                    Log.d(TAG, "distance < 0, ontouch return false");
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 给下拉刷新控件注册一个监听器。
     *
     * @param listener 监听器的实现。
     * @param tag      为了防止不同界面的下拉刷新在上次更新时间上互相有冲突， 请不同界面在注册下拉刷新监听器时一定要传入不同的id。
     */
    public void setOnRefreshListener(PullToRefreshListener listener, String tag) {
        setEnablePull2Refresh(true);
        mRefreshAction = listener;
        mId = tag;
    }

    public void setOnLoadListener(Drag2LoadListener listener) {
        setEnalbleDrag2LoadMore(true);
        mDrag2LoadAction = listener;
    }

    /**
     * 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态。
     */
    public void finishRefreshing() {
        currentRefreshingStatus = STATUS_REFRESH_FINISHED;
        preferences.edit().putLong(UPDATED_AT + mId, System.currentTimeMillis()).commit();
        new HideHeaderTask().execute();
    }

    public void finishLoading() {
        currentLoadingStatus = STATUS_LOAD_FINISHED;
        new HideFooterTask().execute();
    }

    private boolean isAbleToPull() {
        boolean ableToPull = false;
        View firstChild = gridView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = gridView.getFirstVisiblePosition();

            if (firstVisiblePos == 0 && (firstChild.getTop() >= 0) && gridView.getTop() >= 0) {
                ableToPull = true;
            } else {
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新
            ableToPull = true;
        }
        return ableToPull;
    }

    private boolean isAble2Drag() {
        boolean able2Drag = false;
        int count = gridView.getChildCount();
        View lastChild = count > 0 ? gridView.getChildAt(count - 1) : null;
        if (lastChild != null) {
            int lvp = gridView.getLastVisiblePosition();
            if (lvp == (gridView.getAdapter().getCount() - 1) && lastChild.getBottom() <= gridView.getHeight()) {
                Log.w(TAG, "able to drag up");
                return true;
            }
        }

        return able2Drag;
    }

    /**
     * 更新下拉头中的信息。
     */
    private void updateHeaderView() {
        if (lastRereshingStatus != currentRefreshingStatus) {
            if (currentRefreshingStatus == STATUS_PULL_TO_REFRESH) {
                description.setText(getResources().getString(R.string.pull_to_refresh));
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentRefreshingStatus == STATUS_RELEASE_TO_REFRESH) {
                description.setText(getResources().getString(R.string.release_to_refresh));
                arrow.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                rotateArrow();
            } else if (currentRefreshingStatus == STATUS_REFRESHING) {
                description.setText(getResources().getString(R.string.refreshing));
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
        lastUpdateTime = preferences.getLong(UPDATED_AT + mId, -1);
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
        Log.w(TAG, "updateFooterView");

        if (lastLoadingStatus != currentLoadingStatus) {
            if (currentLoadingStatus == STATUS_DRAG_TO_LOAD) {
                footerDes.setText("继续上拉加载更多");
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_RELEASE_TO_LOAD) {
                footerDes.setText("释放加载更多");
                footerProgressBar.setVisibility(View.INVISIBLE);
            } else if (currentLoadingStatus == STATUS_LOADING) {
                footerDes.setText("正在加载..");
                footerProgressBar.setVisibility(View.VISIBLE);
            }
        }

    }


    class RefreshingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int height = headerLayoutParams.height;
            while (true) {
                height += SCROLL_SPEED;
                if (height <= HeightThreshold) {
                    headerLayoutParams.height = HeightThreshold;
                    break;
                }
                publishProgress(height);
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            publishProgress(HeightThreshold);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... heights) {
            headerLayoutParams.height = heights[0];
            header.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Void v) {
            if (currentRefreshingStatus != STATUS_REFRESHING) {
                currentRefreshingStatus = STATUS_REFRESHING;
                updateHeaderView();
                if (mRefreshAction != null) {
                    mRefreshAction.onRefresh(Pull2RefreshGridView.this);
                } else {
                    Pull2RefreshGridView.this.finishRefreshing();
                }
            }
        }

    }

    class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int height = headerLayoutParams.height;
            while (true) {
                height += SCROLL_SPEED / 2;
                if (height <= 0) {
                    height = 0;
                    break;
                }
                publishProgress(height);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return height;
        }

        @Override
        protected void onProgressUpdate(Integer... height) {
            headerLayoutParams.height = height[0];
            header.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer height) {
            headerLayoutParams.height = height;
            header.setLayoutParams(headerLayoutParams);
            currentRefreshingStatus = STATUS_REFRESH_FINISHED;
        }
    }

    class LoadingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int height = footerLayoutParams.height;
            while (true) {
                height += SCROLL_SPEED;
                if (height <= HeightThreshold) {
                    footerLayoutParams.height = HeightThreshold;
                    break;
                }
                publishProgress(height);
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            publishProgress(HeightThreshold);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... heights) {
            footerLayoutParams.height = heights[0];
            footer.setLayoutParams(footerLayoutParams);
        }

        @Override
        protected void onPostExecute(Void v) {
            if (currentLoadingStatus != STATUS_LOADING) {
                currentLoadingStatus = STATUS_LOADING;
                updateFooterView();
                if (mDrag2LoadAction != null) {
                    mDrag2LoadAction.load(Pull2RefreshGridView.this);
                } else {
                    Pull2RefreshGridView.this.finishLoading();
                }
            }
        }

    }

    class HideFooterTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int height = footerLayoutParams.height;
            while (true) {
                height += SCROLL_SPEED / 2;
                if (height <= 0) {
                    height = 0;
                    break;
                }
                publishProgress(height);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return height;
        }

        @Override
        protected void onProgressUpdate(Integer... height) {
            footerLayoutParams.height = height[0];
            footer.setLayoutParams(footerLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer height) {
            footerLayoutParams.height = height;
            footer.setLayoutParams(footerLayoutParams);
            currentLoadingStatus = STATUS_LOAD_FINISHED;
        }
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface PullToRefreshListener {
        void onRefresh(Pull2RefreshGridView refreshableGridView);
    }

    public interface Drag2LoadListener {
        void load(Pull2RefreshGridView refreshableGridView);
    }

}

