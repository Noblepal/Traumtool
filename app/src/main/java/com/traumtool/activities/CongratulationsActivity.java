package com.traumtool.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.traumtool.R;

import static com.traumtool.utils.AppUtils.RANDOM_PIC_URL;

public class CongratulationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_congratulations);

        KenBurnsView backGround = findViewById(R.id.imageViewKB);

        Glide.with(this).load(RANDOM_PIC_URL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.day)
                .fallback(R.drawable.day)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .into(backGround);

        findViewById(R.id.imgBackCongratulations).setOnClickListener(v -> onBackPressed());

    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
