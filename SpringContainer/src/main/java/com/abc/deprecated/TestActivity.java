package com.abc.deprecated;

/**
 * Created by zhangzhenwei on 16/7/26.
 */


        import android.app.Activity;
        import android.os.Bundle;
        import android.view.Window;
        import android.widget.ArrayAdapter;
        import android.widget.ListView;

        import com.abc.viewcontainer.R;


public class TestActivity extends Activity {

    Pull2RefreshListView refreshableListView;
    ListView listView;
    ArrayAdapter<String> adapter;
    String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.pull2refresh_test_main);
        refreshableListView = (Pull2RefreshListView) findViewById(R.id.refreshable_view);
        listView = (ListView) findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        refreshableListView.setPull2RefreshListener(new Pull2RefreshListView.Pull2RefreshListener() {
            @Override
            public void onRefresh(Pull2RefreshListView v) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshableListView.finishRefreshing();
            }
        }, "tag");
    }

}

