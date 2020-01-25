package com.traumtool.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.traumtool.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    ImageView logo, backGround;
    RelativeLayout rl_main;
    TextView startApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.img_logo);
        rl_main = findViewById(R.id.rl_start_app);
        startApp = findViewById(R.id.tv_start_app);
        backGround = findViewById(R.id.imageView);

        Glide.with(this).load("https://source.unsplash.com/random/?nature,water")
                .fallback(R.drawable.relaxation)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.relaxation)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .into(backGround);

        rl_main.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setClass(this, CategoryActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

}
