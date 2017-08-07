package com.ziv.sample.test;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.ziv.lib.viewcontainer.springcontainer.LoadingStateListener;
import com.ziv.lib.viewcontainer.springcontainer.RefreshingStateListener;
import com.ziv.lib.viewcontainer.springcontainer.SampleFooterView;
import com.ziv.lib.viewcontainer.springcontainer.SampleHeaderView;
import com.ziv.lib.viewcontainer.springcontainer.SpringContainer;
import com.ziv.sample.R;

public class RecyclerViewActivity extends AppCompatActivity {
    SpringContainer spring;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_recyclerview);
        View refresh = findViewById(R.id.refresh_btn);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spring.setTopState(SpringContainer.STATUS_TOP_LINGERING);
            }
        });
        View load = findViewById(R.id.load_btn);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spring.setBottomState(SpringContainer.STATUS_BOTTOM_LINGERING);
            }
        });
        spring = (SpringContainer) findViewById(R.id.refreshable_view);
        spring.setStateTransferTimeIntervale(SpringContainer.STATUS_TOP_LINGERING, SpringContainer.STATUS_TOP_LINGER_FINISHED, 3000);
        recyclerView = (RecyclerView) findViewById(R.id.list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ItemAdapter());
        spring.setHeaderView(new SampleHeaderView(this));
        spring.setOnRefreshingStateListener(new RefreshingStateListener() {
            @Override
            public void onRefreshing(SpringContainer v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spring.finishTopLingering();
                    }
                },1000);

            }
        });

        spring.setFooterView(new SampleFooterView());
        spring.setOnLoadListener(new LoadingStateListener() {
            @Override
            public void onLoading(SpringContainer v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spring.finishBottomLingering();
                    }
                },1000);
            }
        });
    }

    class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        String[] items = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L","1","2","3" ,"4","5","6","7","8" };

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_item, parent, false);
            return new TextViewHolder(item);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final TextViewHolder holder1 = (TextViewHolder)holder;
            holder1.v1.setText(items[position]);
            if(position < items.length -1){
                holder1.v2.setText(items[position + 1]);
            }
            if(position%2 == 0){
                holder1.itemView.setBackgroundColor(Color.GRAY);
            } else {
                holder1.itemView.setBackgroundColor(Color.WHITE);
            }

            holder1.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(),holder1.v1.getText(),Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

        class TextViewHolder extends RecyclerView.ViewHolder{
            TextView v1;
            TextView v2;
            public TextViewHolder(View itemView) {
                super(itemView);
                v1 = (TextView)itemView.findViewById(R.id.text1);
                v2 = (TextView)itemView.findViewById(R.id.text2);
            }
        }
    }
}
