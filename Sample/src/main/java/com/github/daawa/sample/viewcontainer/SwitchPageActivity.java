package com.github.daawa.sample.viewcontainer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.daawa.lib.viewcontainer.springcontainer.ISpringView;
import com.github.daawa.lib.viewcontainer.springcontainer.SpringContainer;

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
        one.setBottomThreshold(500);

//        two.setHeaderView(new ASpring(two));
//        two.setTopThreshold(500);

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
            hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP,42);
            return hint;
        }

        @Override
        public View onCreateSpringViewBackground(ViewGroup springView) {
            return null;
        }

        @Override
        public void onStateChanged(int old, int current, Runnable runnable) {
            if(current == SpringContainer.STATUS_BOTTOM_RELEASE_TO_LINGER){
                hint.setText("release to switch page");
            } else {
                hint.setText("~~~~~~~\n~~~~~~~~");
            }

        }

        @Override
        public void onHeightChanged(int cur) {

        }

        @Override
        public boolean onRelease(ViewGroup springView) {
            if(container.getBottomState() == SpringContainer.STATUS_BOTTOM_RELEASE_TO_LINGER){
                two.setVisibility(View.VISIBLE);
                TranslateAnimation aone = new TranslateAnimation(
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,0,
                        TranslateAnimation.RELATIVE_TO_PARENT,-1);
                aone.setDuration(500);

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
                atwo.setDuration(500);

                one.startAnimation(aone);
                two.startAnimation(atwo);


                return true;
            }
            return false;
        }
    }
}
