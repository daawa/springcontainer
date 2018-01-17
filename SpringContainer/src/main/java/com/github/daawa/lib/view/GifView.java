package com.github.daawa.lib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;


import com.github.daawa.lib.viewcontainer.R;

import java.io.InputStream;

/**
 * Created by zhangzhenwei on 16/5/24.
 */

public class GifView extends AppCompatImageView {

    static final int center = 0;
    static final int fit_start = 1;
    static final int fit_end = 2;
    static final int fit_xy = 3;

    private Movie mMovie;
    private long movieStart;

    private GifListener listener;
    private boolean mLoop = true;

    int src_id;
    int scale_mode;

    public GifView(Context context) {
        this(context, null);
    }

    public GifView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(11)
    public GifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GifView);
        src_id = ta.getResourceId(R.styleable.GifView_raw_src, 0);
        scale_mode = ta.getInt(R.styleable.GifView_scale_mode, center);
        setInputSrc(src_id);
    }

    public void setInputSrc(InputStream is) {
        if (is == null) {
            mMovie = null;
        }
        mMovie = Movie.decodeStream(is);
        movieStart = 0;
    }

    public void setInputSrc(int raw_src_id) {
        if (src_id > 0) {
            try {
                InputStream is = getContext().getResources().openRawResource(src_id);
                setInputSrc(is);
            } catch (Resources.NotFoundException e) {

            }
        } else {
            setInputSrc((InputStream) null);
        }
    }

    public void setInputSrc(byte[] data) {
        if (data != null && data.length > 0) {
            mMovie = Movie.decodeByteArray(data, 0, data.length);
        } else {
            setInputSrc((InputStream) null);
        }
    }

    public void setGifListener(GifListener l) {
        listener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mMovie != null) {
            int width = getResovledDim(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + mMovie.width());
            int height = getResovledDim(heightMeasureSpec, getPaddingBottom() + getPaddingTop() + mMovie.height());
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }


    private int getResovledDim(int spec, int expected) {
        int specMode = MeasureSpec.getMode(spec);
        int size = MeasureSpec.getSize(spec);
        if (mMovie != null) {
            switch (specMode) {
                case MeasureSpec.UNSPECIFIED:
                    size = expected;
                    break;
                case MeasureSpec.AT_MOST:
                    size = size > expected ? expected : size;
                    break;
//                case MeasureSpec.EXACTLY:
//                default:
//                    break;
            }
        }

        return size;
    }


    protected void onDraw(Canvas canvas) {

        if (mMovie == null) {
            super.onDraw(canvas);
            return;
        }
        canvas.drawColor(Color.TRANSPARENT);

        long now = android.os.SystemClock.uptimeMillis();
        if (movieStart == 0) {
            movieStart = (int) now;
        }

        long round = (now - movieStart) / mMovie.duration();

        if (mLoop && round > 0 && listener != null) {
            mLoop = !listener.onGifFinished(round);
        }

        if (!mLoop) {
            mMovie.setTime(mMovie.duration() - 1);
        } else {
            int relTime = (int) ((now - movieStart) % mMovie.duration());
            mMovie.setTime(relTime);
        }

        float x = 0, y = 0;
        switch (scale_mode) {
            case center:
                x = (getWidth() - mMovie.width()) / 2;
                y = (getHeight() - mMovie.height()) / 2;
                break;
            case fit_start:
                x = getPaddingLeft();
                y = getPaddingTop();
                break;
            case fit_end:
                x = getWidth() - mMovie.width() - getPaddingRight();
                y = getHeight() - mMovie.height() - getPaddingBottom();
                break;
            case fit_xy:
                y = x = 0;
                float sx = (((float) getWidth()) / mMovie.width());
                float sy = (((float) getHeight()) / mMovie.height());
                canvas.scale(sx, sy);
                break;
        }
        mMovie.draw(canvas, x, y);
        this.invalidate();

    }

    public interface GifListener {
        /**
         * @param round the target has displayed #round times
         * @return return true to stop playback
         */
        boolean onGifFinished(long round);

        //void onGifPlayed(long elapsed, long duration);
    }
}