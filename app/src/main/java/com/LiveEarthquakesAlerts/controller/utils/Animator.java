package com.LiveEarthquakesAlerts.controller.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.LiveEarthquakesAlerts.R;


/**
 * Created by upg on 11/1/16.
 */

public class Animator extends Animation {
    private static Animation anim;
    private static Animator animator = null;
    public boolean isSetAnimation = false;
    private int animationTime;
    private View view;


    private Animator(View view) {

        this.view = view;
        anim = new AlphaAnimation(0.0f, 1.0f);

    }

    public static Animator getAnimator(View view) {
        if (animator == null) {
            animator = new Animator(view);
        }
        return animator;
    }

    public void setAnimation(int animationTime1) {
        animationTime = animationTime1;
        if (anim != null) {
            anim.setDuration(animationTime); //You can manage the time of the blink with this parameter
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.danger, 0, 0, 0);
                textView.startAnimation(anim);
            }
            this.isSetAnimation = true;
        }
    }

    public void stopAnimation(TextView tvBanner1) {
        anim.cancel();
        anim.reset();
        view.clearAnimation();
        this.isSetAnimation = false;
    }
}
