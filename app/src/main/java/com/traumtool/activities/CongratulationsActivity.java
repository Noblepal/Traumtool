package com.traumtool.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.traumtool.R;

import static com.traumtool.utils.AppUtils.RANDOM_PIC_URL;

public class CongratulationsActivity extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_congratulations);

        KenBurnsView backGround = findViewById(R.id.imageViewKB);

        findViewById(R.id.rootView).setOnTouchListener((v, event) -> {
            startActivity(new Intent(CongratulationsActivity.this, CategoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        });

        Glide.with(this).load(RANDOM_PIC_URL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.day)
                .fallback(R.drawable.day)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .into(backGround);

        findViewById(R.id.imgBackCongratulations).setOnClickListener(v -> {
            startActivity(new Intent(CongratulationsActivity.this, CategoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(CongratulationsActivity.this, CategoryActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
