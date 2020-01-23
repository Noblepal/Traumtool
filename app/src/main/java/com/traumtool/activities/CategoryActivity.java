package com.traumtool.activities;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.traumtool.R;
import com.traumtool.adapters.CategoryAdapter;
import com.traumtool.libs.GridRecyclerView;
import com.traumtool.utils.SharedPrefsManager;

public class CategoryActivity extends AppCompatActivity {

    Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        initializeData();
    }

    private void initializeData() {
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

        mSwitch = findViewById(R.id.switch_online_offline);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPrefsManager.getInstance(CategoryActivity.this).setOfflineMode(isChecked);
                if (isChecked) {
                    Toast.makeText(CategoryActivity.this, "Online", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CategoryActivity.this, "Offline", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
