package com.traumtool.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.traumtool.R;
import com.traumtool.activities.DreamActivity;
import com.traumtool.activities.PlayerActivity;
import com.traumtool.activities.SelfReflectionActivity;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private int[] images;
    private String[] categories;
    private static final String TAG = "CategoryAdapter";

    public CategoryAdapter(Context context, String[] categories, int[] images) {
        this.context = context;
        this.images = images;
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.category.setText(categories[position]);
        holder.image.setImageResource(images[position]);
        Intent intent = new Intent();
        holder.cardView.setOnClickListener(v -> {
            if (categories[position].toLowerCase().contains("dream")) {
                intent.setClass(context, DreamActivity.class);
                intent.putExtra("category", "dreamtravel");
            } else if (categories[position].toLowerCase().contains("reflection")) {
                intent.putExtra("category", "self_reflection");
                intent.setClass(context, SelfReflectionActivity.class);
            } else if (categories[position].toLowerCase().contains("meditation")) {
                intent.putExtra("category", "meditation");
                intent.setClass(context, PlayerActivity.class);
            } else if (categories[position].toLowerCase().contains("muscle")) {
                intent.putExtra("category", "muscle_relaxation");
                intent.setClass(context, PlayerActivity.class);
            } else if (categories[position].toLowerCase().contentEquals("relaxation")) {
                intent.putExtra("category", "relaxation");
                intent.setClass(context, PlayerActivity.class);
            } else {
                Log.d(TAG, "Skipping intent: no parameters defined");
            }

            Log.d(TAG, "onBindViewHolder: Starting activity with parameters: " + categories[position]);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return categories.length;
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView category;
        MaterialCardView cardView;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.imageViewCategory);
            category = itemView.findViewById(R.id.tvCategory);
            cardView = itemView.findViewById(R.id.cvCategory);
        }
    }
}
