package com.traumtool.activities;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.adapters.CategoryAdapter;
import com.traumtool.interfaces.NetworkModeChangeListener;
import com.traumtool.libs.GridRecyclerView;
import com.traumtool.utils.SharedPrefsManager;

public class CategoryActivity extends AppCompatActivity implements NetworkModeChangeListener {

    Switch mSwitch;
    String TAG = "Category activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        initializeData();
    }

    private void initializeData() {
        //Set switch to previously selected mode
        mSwitch = findViewById(R.id.switch_online_offline);
        mSwitch.setChecked(SharedPrefsManager.getInstance(this).getIsOffline());
        String[] categories = {"Relaxation", "Dream Trips", "Meditation", "Self-reflection", "Muscle Relaxation"};
        int[] images = {R.drawable.relaxation,
                R.drawable.dream_trip, R.drawable.meditation,
                R.drawable.self_reflection, R.drawable.muscle_meditation};

        GridRecyclerView recyclerViewCategories = findViewById(R.id.recyclerViewCategories);
        recyclerViewCategories.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.grid_layout_animation_from_bottom));
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewCategories.setHasFixedSize(true);

        recyclerViewCategories.setAdapter(new CategoryAdapter(this, categories, images));

        TextView linkText = findViewById(R.id.tvLink);
        linkText.setMovementMethod(LinkMovementMethod.getInstance());

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

    }

    private void showCustomSnackBar(String message, boolean hasAction, @Nullable String actionText, int LENGTH) {
        Snackbar snackbar = Snackbar.make(mSwitch, message, LENGTH);
        if (hasAction) {
            Log.d(TAG, "showCustomSnackBar: Has Action");
        }
        snackbar.show();
    }

    @Override
    public boolean isOfflineEnabled(boolean isOffline) {
        return isOffline;
    }
}