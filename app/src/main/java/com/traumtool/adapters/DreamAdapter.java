package com.traumtool.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.traumtool.R;
import com.traumtool.activities.ReadDreamActivity;
import com.traumtool.models.Dream;
import com.traumtool.utils.AppUtils;

import java.util.ArrayList;

public class DreamAdapter extends RecyclerView.Adapter<DreamAdapter.MusicViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Dream> dreamArrayList;

    public DreamAdapter(Context context, ArrayList<Dream> dreamArrayList) {
        this.context = context;
        this.dreamArrayList = dreamArrayList;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicViewHolder(inflater.inflate(R.layout.item_dream_trip, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Dream currentDream = dreamArrayList.get(position);

        currentDream.setFileUrl(AppUtils.BASE_URL + currentDream.getCategory() + "/" + currentDream.getFileName());

        holder.title.setText(currentDream.getFileName());
        holder.author.setText(currentDream.getAuthor());
        holder.words.setText(currentDream.getWords() + " words");

        Glide.with(holder.image)
                .load(currentDream.getFileUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_image)
                .fallback(R.drawable.ic_image)
                .into(holder.image);

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReadDreamActivity.class);
            intent.putExtra("item", currentDream);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dreamArrayList.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title, author, words;
        ImageButton play;
        MaterialCardView cardView;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgDreamMusicFromList);
            title = itemView.findViewById(R.id.tvDreamTitle);
            author = itemView.findViewById(R.id.tvAuthorDreamFromList);
            words = itemView.findViewById(R.id.tvListWordsTimeFromList);
            play = itemView.findViewById(R.id.img_play_dream_from_list);
            cardView = itemView.findViewById(R.id.cardViewDream);
        }
    }
}
