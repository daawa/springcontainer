package com.ziv.sample.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.ziv.sample.R;

public class ScrollViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_view);

//        findViewById(R.id.text_view).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(view.getContext(),"click",Toast.LENGTH_SHORT).show();
//            }
//        });
    }
}