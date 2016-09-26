package com.abc.viewcontainer.verticalscrollhelper;


import android.webkit.WebView;

/**
 * Created by zhangzhenwei on 16/8/15.
 */

public class WebViewVerticalScrollHelper implements IVerticalScrollHelper {
    WebView webView;
    public WebViewVerticalScrollHelper(WebView v){
        webView = v;
    }
    @Override
    public boolean canScrollUp() {
        return webView.canScrollVertically(1);
    }

    @Override
    public boolean canScrollDown() {
        return webView.canScrollVertically(-1);
    }
}

