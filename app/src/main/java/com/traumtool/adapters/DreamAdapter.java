package com.traumtool.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.traumtool.R;
import com.traumtool.activities.ReadDreamActivity;
import com.traumtool.models.Dream;
import com.traumtool.utils.AppUtils;

import java.util.ArrayList;
import java.util.Random;

//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class DreamAdapter extends RecyclerView.Adapter<DreamAdapter.MusicViewHolder> {

    private Activity activity;
    private LayoutInflater inflater;
    private ArrayList<Dream> dreamArrayList;

    public DreamAdapter(Activity activity, ArrayList<Dream> dreamArrayList) {
        this.activity = activity;
        this.dreamArrayList = dreamArrayList;
        inflater = LayoutInflater.from(activity);
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
        String author = currentDream.getAuthor() != null ? "by " + currentDream.getAuthor() : "Unknown author";
        holder.author.setText(author);
        Random randWords = new Random();
        int rand;
        do {
            rand = randWords.nextInt(972);
        } while (rand < 318);
        String words = currentDream.getWords() != null ? currentDream.getWords() + " words" : rand + " words";
        holder.words.setText(words);
        holder.image.setImageResource(R.drawable.ic_document);

        if (currentDream.getWords() != null) {
            holder.imagePlay.setImageResource(R.drawable.ic_check_circle);
        } else {
            holder.imagePlay.setImageResource(R.drawable.ic_play);
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ReadDreamActivity.class);
            intent.putExtra("item", currentDream);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @Override
    public int getItemCount() {
        return dreamArrayList.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder {

        ImageView image, imagePlay;
        TextView title, author, words;
        ImageButton play;
        MaterialCardView cardView;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgDreamMusicFromList);
            imagePlay = itemView.findViewById(R.id.img_play_dream_from_list);
            title = itemView.findViewById(R.id.tvDreamTitle);
            author = itemView.findViewById(R.id.tvAuthorDreamFromList);
            words = itemView.findViewById(R.id.tvListWordsTimeFromList);
            play = itemView.findViewById(R.id.img_play_dream_from_list);
            cardView = itemView.findViewById(R.id.cardViewDream);
        }
    }
}
