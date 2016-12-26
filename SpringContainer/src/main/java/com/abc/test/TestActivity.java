package com.abc.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.abc.viewcontainer.R;
import com.abc.viewcontainer.springcontainer.Pull2RefreshListener;
import com.abc.viewcontainer.springcontainer.SpringContainer;

public class TestActivity extends AppCompatActivity {
    SpringContainer spring;
    ListView listView;
    ArrayAdapter<String> adapter;
    String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L" ,"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L","A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L","A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_test);

        spring = (SpringContainer) findViewById(R.id.refreshable_view);
        listView = (ListView) findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        spring.setOnRefreshListener("tag", new Pull2RefreshListener() {
            @Override
            public void onRefresh(SpringContainer v) {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spring.finishRefreshing();
                    }
                },1000);

            }
        });
    }
}
