package com.ziv.sample.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.abc.viewcontainer.springcontainer.Drag2LoadListener;
import com.abc.viewcontainer.springcontainer.Pull2RefreshListener;
import com.abc.viewcontainer.springcontainer.SampleFooterView;
import com.abc.viewcontainer.springcontainer.SampleHeaderView;
import com.abc.viewcontainer.springcontainer.SpringContainer;
import com.ziv.sample.R;

public class RecyclerviewActivity extends AppCompatActivity {
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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(listView.getContext(),"onclick:" + position, Toast.LENGTH_SHORT).show();
            }
        });
        spring.setHeaderView(new SampleHeaderView(this));
        spring.setOnRefreshListener(new Pull2RefreshListener() {
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

        spring.setFooterView(new SampleFooterView());
        spring.setOnLoadListener(new Drag2LoadListener() {
            @Override
            public void load(SpringContainer v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spring.finishLoadingMore();
                    }
                },1000);
            }
        });
    }
}
