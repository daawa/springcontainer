package com.abc.viewcontainer.springcontainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abc.viewcontainer.R;

import static com.abc.viewcontainer.springcontainer.SpringContainer.STATUS_PULL_TO_REFRESH;
import static com.abc.viewcontainer.springcontainer.SpringContainer.STATUS_REFRESHING;
import static com.abc.viewcontainer.springcontainer.SpringContainer.STATUS_REFRESH_FINISHED;
import static com.abc.viewcontainer.springcontainer.SpringContainer.STATUS_RELEASE_TO_REFRESH;

/**
 * Created by hzzhangzhenwei on 2016/12/29.
 */

public class SampleHeaderView implements iHeaderView {
    private static final String PREFERENCE_NAME = "com.abc.spring.container";
    private static final String KEY_UPDATED_AT = "updated_at";

    public String PULL_TO_RELEASE_TIP = "下拉可以刷新";
    public String RELEASE_TO_REFRESH_TIP = "释放立即刷新";
    public String REFRESHING_TIP = "正在刷新…";

    // time interval by milliseconds
    public static final long ONE_MINUTE = 60 * 1000;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    public static final long ONE_MONTH = 30 * ONE_DAY;
    public static final long ONE_YEAR = 12 * ONE_MONTH;

    View headerView;

    /**
     * used to store the time of last update
     */
    private SharedPreferences preferences;

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
    private String mId4UpdateTime = "sample.header";

    private int currentRefreshingStatus = STATUS_REFRESH_FINISHED;
    private int lastRefreshingStatus = currentRefreshingStatus;

    Context context;
    public SampleHeaderView(Context context){
        this.context = context;
        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void setTag(String tag){
        if(tag == null) return;
        this.mId4UpdateTime = tag;
    }
    @Override
    public View onCreateSpringView(ViewGroup headerContainer) {
        headerView =  LayoutInflater.from(headerContainer.getContext()).inflate(R.layout.springcontainer_sample_header, headerContainer,false);

        progressBar = (ProgressBar) headerView.findViewById(R.id.progress_bar);
        arrow = (ImageView) headerView.findViewById(R.id.arrow);
        description = (TextView) headerView.findViewById(R.id.description);
        updateAt = (TextView) headerView.findViewById(R.id.updated_at);

        return headerView;
    }

    @Override
    public View onCreateSpringViewBackground(ViewGroup headerContainer) {
        return null;
    }

    @Override
    public void onStateChanged(int old, int state) {
        currentRefreshingStatus = state;
        updateHeaderView();
    }

    @Override
    public void onHeightChanged(int cur) {
        headerView.getLayoutParams().height = cur;
    }

    @Override
    public void onRefreshFinished() {
        preferences.edit().putLong(KEY_UPDATED_AT + mId4UpdateTime, System.currentTimeMillis()).commit();
    }

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

            lastRefreshingStatus = currentRefreshingStatus;
        }
    }

    /**
     * rotate the arrow in headerContainer view
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
     * update the desc and time in headerContainer view
     */
    private void refreshUpdatedAtValue() {
        lastUpdateTime = preferences.getLong(KEY_UPDATED_AT + mId4UpdateTime, -1);
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastUpdateTime;
        long timeIntoFormat;
        String updateAtValue;
        Context context = this.context;
        if (lastUpdateTime == -1) {
            updateAtValue = context.getResources().getString(R.string.not_updated_yet);
        } else if (timePassed < 0) {
            updateAtValue = context.getResources().getString(R.string.time_error);
        } else if (timePassed < ONE_MINUTE) {
            updateAtValue = context.getResources().getString(R.string.updated_just_now);
        } else if (timePassed < ONE_HOUR) {
            timeIntoFormat = timePassed / ONE_MINUTE;
            String value = timeIntoFormat + "分钟";
            updateAtValue = String.format(context.getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_DAY) {
            timeIntoFormat = timePassed / ONE_HOUR;
            String value = timeIntoFormat + "小时";
            updateAtValue = String.format(context.getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_MONTH) {
            timeIntoFormat = timePassed / ONE_DAY;
            String value = timeIntoFormat + "天";
            updateAtValue = String.format(context.getResources().getString(R.string.updated_at), value);
        } else if (timePassed < ONE_YEAR) {
            timeIntoFormat = timePassed / ONE_MONTH;
            String value = timeIntoFormat + "个月";
            updateAtValue = String.format(context.getResources().getString(R.string.updated_at), value);
        } else {
            timeIntoFormat = timePassed / ONE_YEAR;
            String value = timeIntoFormat + "年";
            updateAtValue = String.format(context.getResources().getString(R.string.updated_at), value);
        }
        updateAt.setText(updateAtValue);
    }
}
