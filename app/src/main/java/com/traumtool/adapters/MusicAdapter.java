package com.traumtool.adapters;

import android.content.Context;
import android.util.Log;
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
import com.traumtool.models.Music;
import com.traumtool.utils.AppUtils;

import java.io.File;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    private static final String TAG = "MusicAdapter";
    private ArrayList<Music> musicArrayList;

    public MusicAdapter(Context context, ArrayList<Music> musicArrayList) {
        this.context = context;
        this.musicArrayList = musicArrayList;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicViewHolder(inflater.inflate(R.layout.item_music, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music currentMusic = musicArrayList.get(position);

        holder.name.setText(AppUtils.removeFileExtensionFromString(currentMusic.getFilename()));
        holder.size.setText("0");
        if (isAvailableOffline(currentMusic)) {
            AppUtils.showView(holder.offlineChecked);
        }

        Glide.with(holder.image)
                .load(currentMusic.getFileUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_image)
                .fallback(R.drawable.ic_image)
                .into(holder.image);

        holder.duration.setText(AppUtils.formatStringToTime(currentMusic.getDuration() * 1000));

    }

    private boolean isAvailableOffline(Music file) {
        File path = context.getExternalFilesDir("Download/" + file.getCategory() + "/");
        File mFile = new File(path, file.getFilename());
        Log.d(TAG, "isAvailableOffline: Called " + mFile.getAbsolutePath());
        return mFile.exists();
    }

    @Override
    public int getItemCount() {
        return musicArrayList.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder {

        ImageView image, offlineChecked;
        TextView name, duration, size;
        ImageButton play;
        MaterialCardView cardView;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgMusicFromList);
            offlineChecked = itemView.findViewById(R.id.img_offline_check);
            name = itemView.findViewById(R.id.tvNameFromList);
            size = itemView.findViewById(R.id.tv_file_size);
            duration = itemView.findViewById(R.id.tvListMusicTimeFromList);
            play = itemView.findViewById(R.id.img_play_from_list);
            cardView = itemView.findViewById(R.id.materialCardView);
        }
    }
}
