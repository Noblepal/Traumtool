package com.traumtool.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.traumtool.R;
import com.traumtool.utils.SharedPrefsManager;

public class CategoryActivity extends AppCompatActivity implements View.OnClickListener {

    //Switch mSwitch;
    ToggleButton btnModeSwitch;
    ImageButton imgBack;
    String TAG = "CategoryActivity";
    MaterialCardView cv_relaxation, cv_dreamtrips, cv_meditation, cv_selfreflection, cv_musclerelaxation, cv_mode;
    TextView tvImprint, tvMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        initializeData();
    }

    private void initializeData() {
        cv_relaxation = findViewById(R.id.cvCategoryRelaxation);
        cv_dreamtrips = findViewById(R.id.cvCategoryDreamTrips);
        cv_meditation = findViewById(R.id.cvCategoryMeditation);
        cv_selfreflection = findViewById(R.id.cvCategorySelfReflection);
        cv_musclerelaxation = findViewById(R.id.cvCategoryMuscleRelaxation);
        cv_mode = findViewById(R.id.mcvMode);
        tvImprint = findViewById(R.id.tvLink);
        tvMode = findViewById(R.id.tvCategoryOnlineOffline);

        cv_relaxation.setOnClickListener(this);
        cv_dreamtrips.setOnClickListener(this);
        cv_meditation.setOnClickListener(this);
        cv_selfreflection.setOnClickListener(this);
        cv_musclerelaxation.setOnClickListener(this);
        cv_mode.setOnClickListener(this);

        btnModeSwitch = findViewById(R.id.btnToggleOnlineOffline);
        imgBack = findViewById(R.id.imgBackCategory);

        tvImprint.setMovementMethod(LinkMovementMethod.getInstance());

        btnModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged: " + isChecked);
            if (isChecked) {
                SharedPrefsManager.getInstance(CategoryActivity.this).toggleOfflineMode(false);
                setIsOnline();

            } else {
                SharedPrefsManager.getInstance(CategoryActivity.this).toggleOfflineMode(true);
                setIsOffline();
            }
        });
        imgBack.setOnClickListener(v -> onBackPressed());

    }

    private void setIsOnline() {
        btnModeSwitch.setChecked(true);
        tvMode.setText(R.string.online);
        tvMode.setTextColor(getResources().getColor(R.color.colorAccentLight));
    }

    private void setIsOffline() {
        btnModeSwitch.setChecked(false);
        tvMode.setText(R.string.offline);
        tvMode.setTextColor(getResources().getColor(R.color.greyMedium));
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

    @Override
    protected void onResume() {
        super.onResume();
        if (SharedPrefsManager.getInstance(this).getIsOffline()) {
            setIsOffline();
        } else {
            setIsOnline();
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.cvCategoryRelaxation:
                intent.setClass(this, PlayerActivity.class);
                intent.putExtra("category", "relaxation");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.cvCategoryDreamTrips:
                intent.setClass(this, DreamActivity.class);
                intent.putExtra("category", "dreamtravel");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.cvCategoryMeditation:
                intent.setClass(this, PlayerActivity.class);
                intent.putExtra("category", "meditation");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.cvCategorySelfReflection:
                intent.setClass(this, SelfReflectionActivity.class);
                intent.putExtra("category", "self_reflection");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.cvCategoryMuscleRelaxation:
                intent.setClass(this, PlayerActivity.class);
                intent.putExtra("category", "muscle_relaxation");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.mcvMode:
                btnModeSwitch.setChecked(!btnModeSwitch.isChecked());
                break;
        }
    }
}