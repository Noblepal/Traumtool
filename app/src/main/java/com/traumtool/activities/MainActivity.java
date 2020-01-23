package com.traumtool.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;

import com.traumtool.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    ImageView logo;
    RelativeLayout rl_main;
    TextView startApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.img_logo);
        rl_main = findViewById(R.id.rl_start_app);
        startApp = findViewById(R.id.tv_start_app);

        rl_main.setOnClickListener(v -> {
            //noinspection unchecked
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(this,
                            new Pair<>(logo, ViewCompat.getTransitionName(logo)));
            ActivityCompat.startActivity(this, new Intent(this, CategoryActivity.class), options.toBundle());

        });
    }

}
