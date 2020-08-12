package com.app.afinador_rockstar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by lucas on 04/06/2017.
 */

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                showTunerActivity();
            }
        }, 1500);
    }

    private void showTunerActivity() {
        Intent intent = new Intent(SplashScreenActivity.this,
                TunerActivity.class);
        startActivity(intent);
        finish();
    }
}
