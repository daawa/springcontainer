package com.ziv.sample.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ziv.lib.viewcontainer.springcontainer.ISpringView;
import com.ziv.lib.viewcontainer.springcontainer.SpringContainer;
import com.ziv.sample.R;

public class SwitchPageActivity extends AppCompatActivity {

    SpringContainer one;
    SpringContainer two;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_page);

        FrameLayout root = (FrameLayout) findViewById(R.id.root);
         one = (SpringContainer) root.findViewById(R.id.page_one);
        two = (SpringContainer)root.findViewById(R.id.page_two);
         //two = (SpringContainer) LayoutInflater.from(this).inflate(R.layout.page_two, root, false);

        one.setFooterView(new ASpring(one));
        one.setHeaderView(new ASpring(one));

//        two.setFooterView(new ASpring(two));
//        two.setHeaderView(new ASpring(two));

    }

    class ASpring implements ISpringView{

        SpringContainer container;
        TextView hint;

        public ASpring(SpringContainer container){
            this.container = container;
        }

        @Override
        public View onCreateSpringView(ViewGroup springView) {
            hint = new TextView(springView.getContext());
            hint.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            hint.setGravity(Gravity.CENTER);
            hint.setText("~~~~~~~\n~~~~~~~~");
            return hint;
        }

        @Override
        public View onCreateSpringViewBackground(ViewGroup springView) {
            return null;
        }

        @Override
        public void onStateChanged(int old, int current) {

        }

        @Override
        public void onHeightChanged(int cur) {
            if(cur > 300){
                hint.setText("release to switch page");
            } else {
                hint.setText("~~~~~~~\n~~~~~~~~");
            }
        }

        @Override
        public boolean onRelease(ViewGroup springView) {
            if(springView.getHeight() >= 700){
                two.setVisibility(View.VISIBLE);
                TranslateAnimation aone = new TranslateAnimation(
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,-1);
                aone.setDuration(1000);

                aone.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        one.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                final TranslateAnimation atwo = new TranslateAnimation(
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,1,
                        TranslateAnimation.RELATIVE_TO_PARENT,0);
                atwo.setDuration(1000);
//                atwo.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        two.setVisibility(View.GONE);
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//
//                    }
//                });

                one.startAnimation(aone);
                two.startAnimation(atwo);


                return true;
            }
            return false;
        }
    }
}
