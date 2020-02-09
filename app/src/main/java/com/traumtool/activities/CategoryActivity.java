package com.traumtool.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.utils.SharedPrefsManager;

public class CategoryActivity extends AppCompatActivity implements View.OnClickListener {

    Switch mSwitch;
    ImageButton imgBack;
    String TAG = "Category activity";
    MaterialCardView cv_relaxation, cv_dreamtrips, cv_meditation, cv_selfreflection, cv_musclerelaxation;
    TextView tvImprint;

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
        tvImprint = findViewById(R.id.tvLink);

        cv_relaxation.setOnClickListener(this);
        cv_dreamtrips.setOnClickListener(this);
        cv_meditation.setOnClickListener(this);
        cv_selfreflection.setOnClickListener(this);
        cv_musclerelaxation.setOnClickListener(this);

        //Set switch to previously selected mode
        mSwitch = findViewById(R.id.switch_online_offline);
        imgBack = findViewById(R.id.imgBackCategory);
        mSwitch.setChecked(true);
        if (SharedPrefsManager.getInstance(this).getIsOffline()) {
            mSwitch.setChecked(false);
        }

        tvImprint.setMovementMethod(LinkMovementMethod.getInstance());

        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "onCheckedChanged: " + isChecked);
            if (isChecked) {
                SharedPrefsManager.getInstance(CategoryActivity.this).toggleOfflineMode(false);
                showCustomSnackBar("Online mode set", false, null, 0);
            } else {
                SharedPrefsManager.getInstance(CategoryActivity.this).toggleOfflineMode(true);
                showCustomSnackBar("Offline mode set", false, null, 0);
            }
        });
        imgBack.setOnClickListener(v -> onBackPressed());

    }

    private void showCustomSnackBar(String message, boolean hasAction, @Nullable String actionText, int LENGTH) {
        Snackbar snackbar = Snackbar.make(mSwitch, message, LENGTH);
        if (hasAction) {
            Log.d(TAG, "showCustomSnackBar: Has Action");
        }
        snackbar.show();
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
            mSwitch.setChecked(false);
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

        }
    }
}