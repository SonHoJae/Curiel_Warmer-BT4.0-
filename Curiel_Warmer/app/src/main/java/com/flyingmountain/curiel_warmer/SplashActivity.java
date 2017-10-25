package com.flyingmountain.curiel_warmer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        final LinearLayout splash_layout = (LinearLayout)findViewById(R.id.splash_layout);

        new Handler().post(new Runnable() {

            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade);
                splash_layout.startAnimation(animation);

            }
        });
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                finish();
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            }
        },2500);

    }
    @Override
    protected void onResume(){
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();

    }

}
