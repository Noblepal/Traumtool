package com.traumtool.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.adapters.MusicAdapter;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.FileResponse;
import com.traumtool.models.Music;
import com.traumtool.services.MainService;
import com.traumtool.utils.AppUtils;
import com.traumtool.utils.RecyclerItemClickListener;
import com.traumtool.utils.SharedPrefsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.traumtool.utils.AppUtils.formatStringToTime;
import static com.traumtool.utils.AppUtils.logThis;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<Music> musicArrayList = new ArrayList<>();
    private ArrayList<Music> offlineFiles = new ArrayList<>();
    private MusicAdapter musicAdapter;
    private static final String TAG = "PlayerActivity";
    private ImageButton rewind, forward, download, backButton;
    private ToggleButton play_pause;
    ImageView topImage;
    Switch goOnlineSwitch;
    LinearLayout noOfflineFiles;
    private SeekBar seekBar;
    private TextView realTime, audioName, mainCategory;
    private ProgressBar bufferProgressBar, downloadProgress, loadingProgressBar;
    private RecyclerView recyclerView;
    private boolean isPlayerPlaying = false, isDownloaded = false, isOfflineFromPrefs;
    //private MediaPlayer mediaPlayer;
    private Music currentAudio;
    private int mediaFileLength = 0;
    private int realTimeLength = 0;
    private String category;
    private DownloadManager mgr = null;
    private long lastDownload = -1L;
    AsyncTask<String, String, String> streamTask;
    private static final int PERMISSION_REQUEST = 100;
    final Handler handler = new Handler();
    private boolean isFirstTimeLaunch = true;
    int decrementCounter = 0;
    int finalDestination = 0;

    //For notifications
    private static final String CHANNEL_ID = "channel_id01";
    public static final int NOTIFICATION_ID = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //Extract category from incoming intent (Category Activity)
        try {
            category = getIntent().getStringExtra("category");
            //Set current category to sharedPrefs for retrieving from notification later

            Log.e(TAG, "onCreate: Received category: " + category);
            Log.e(TAG, "onCreate: Prefs category: " + SharedPrefsManager.getInstance(this).getCurrentCategory());

            /*Check if received category is same as cached category*/
            if (!category.equals(SharedPrefsManager.getInstance(this).getCurrentCategory())
                    && SharedPrefsManager.getInstance(this).isBackGroundAudioPlaying()) {
                //Cancel media player
                showAlertDialog();
            } else {
                Log.i(TAG, "onCreate: Received same category");
            }

            Log.d(TAG, "onCreate: category -> " + category);
            SharedPrefsManager.getInstance(getApplicationContext()).setCurrentCategory(category);
        } catch (Exception e) {
            Log.d(TAG, "onCreate: not received any category title");
            e.printStackTrace();
        }

        requestPermission();
        findViews();
        initializeStuff();
        setUpDownloadManager();

        //Get online/offline boolean from shared preferences
        isOfflineFromPrefs = SharedPrefsManager.getInstance(this).getIsOffline();

        if (isOfflineFromPrefs) {
            //Retrieve locally downloaded files
            //download.setImageResource(R.drawable.ic_check_circle);
            getFiles();
        } else {
            //Retrieve files from server
            retrieveAudioFiles();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Progress"));
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Stop playing audio?");
        builder.setMessage("Do you want to stop current playing audio?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent cancelIntent = new Intent(this, MainService.class);
            cancelIntent.putExtra("action", "stop");
            startService(cancelIntent);
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
            onBackPressed();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            isPlayerPlaying = intent.getBooleanExtra("isPlaying", false);
            int primaryProgress = intent.getIntExtra("progress", 0);
            int secondaryProgress = intent.getIntExtra("sec_progress", 0);
            resetLengthsFirst();
            mediaFileLength = intent.getIntExtra("dur", 0);
            realTimeLength = mediaFileLength - (decrementCounter += 1000);
            realTime.setText(formatStringToTime(mediaFileLength));
            //  ... react to local broadcast message


            if (primaryProgress == 100) {
                logThis(TAG, 0, "temp: " + finalDestination);
                logThis(TAG, 0, "final: " + primaryProgress);
                play_pause.setChecked(true);
            }

            seekBar.setProgress(primaryProgress);
            logThis(TAG, 3, "primaryProgress: " + primaryProgress);
            seekBar.setSecondaryProgress(secondaryProgress);
            logThis(TAG, 2, "secondaryProgress: " + secondaryProgress);
        }
    };

    private void resetLengthsFirst() {
        if (isFirstTimeLaunch) {
            realTimeLength = 0;
            mediaFileLength = 0;
            isFirstTimeLaunch = false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void findViews() {
        rewind = findViewById(R.id.img_rewind);
        topImage = findViewById(R.id.imageView2);
        play_pause = findViewById(R.id.btn_play_pause);
        forward = findViewById(R.id.img_forward);
        bufferProgressBar = findViewById(R.id.buffering_progress_bar);
        loadingProgressBar = findViewById(R.id.retrieve_filenames_progress_bar);
        downloadProgress = findViewById(R.id.download_progress_bar);
        realTime = findViewById(R.id.tvRealTime);
        download = findViewById(R.id.img_download);
        mainCategory = findViewById(R.id.tvCategoryTitle);
        //mainCategory.setText(AppUtils.capitalizeEachWord(category));
        backButton = findViewById(R.id.imgBackPlayer);
        noOfflineFiles = findViewById(R.id.ll_no_offline_files);
        goOnlineSwitch = findViewById(R.id.switch_go_online);
        goOnlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
//                hideView(noOfflineFiles);
//                retrieveAudioFiles();
//                SharedPrefsManager.getInstance(PlayerActivity.this).toggleOfflineMode(true);
            }
        });
        backButton.setOnClickListener(v -> onBackPressed());

        play_pause.setOnClickListener(this);

        audioName = findViewById(R.id.tvAudioName);

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setMax(99);//100% (0 - 99)
        seekBar.setOnTouchListener((v, event) -> true);
        forward.setOnClickListener(this);
        rewind.setOnClickListener(this);
    }

    private void initializeStuff() {

        Glide.with(this).load("https://source.unsplash.com/random/?nature,water")
                .fallback(R.drawable.self_reflection)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .placeholder(R.drawable.self_reflection)
                .into(topImage);
    }

    //Download audio list from server
    private void retrieveAudioFiles() {
        showView(loadingProgressBar);
        ApiService service = AppUtils.getApiService();
        service.getFileList(category)
                .enqueue(new Callback<FileResponse>() {
                    @Override
                    public void onResponse(Call<FileResponse> call, Response<FileResponse> response) {
                        Toast.makeText(PlayerActivity.this, "Complete", Toast.LENGTH_SHORT).show();
                        hideView(loadingProgressBar);
                        if (response.isSuccessful()) {
                            try {
                                List<Music> audios = response.body().getAudio();
                                musicArrayList.addAll(audios); //Add all audio names to arrayList
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: List null or non existent");
                            }
                            //Add filenames fo recyclerView
                            populateRecyclerView(musicArrayList);

                            loadFirstAudioFile(musicArrayList);
                            //loadFileIntoPlayer(musicArrayList.get(0));//Load first audio file into player
                        } else {
                            showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Retry", -2);
                        }
                    }

                    @Override
                    public void onFailure(Call<FileResponse> call, Throwable t) {
                        hideView(loadingProgressBar);
                        showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Retry", -2);
                        Log.d(TAG, "onFailure: " + t.getMessage());
                    }
                });
    }

    /*Retrieve all offline files*/
    private void getFiles() {
        File path = PlayerActivity.this.getExternalFilesDir("Download/" + category);
        String uri = String.valueOf(path);
        File file = new File(uri);
        File[] files = file.listFiles();

        int id = 0;
        for (File f : files) {
            Log.d(TAG, "getFiles: " + f.getName());
            if (f.getName() == "" || f.getName().isEmpty()) {
                return;
            }
            Music music = new Music(
                    id++,
                    f.getName(),
                    category,
                    getFileDuration(path + "/" + f.getName()),
                    path + "/" + f.getName(),
                    0
            );

            if (music.getFilename().isEmpty() || music.getFilename() == null) {
                Log.d(TAG, "getFiles: Is empty");
            } else {
                offlineFiles.add(music);
            }
        }
        if (files.length > 0) {

            populateRecyclerView(offlineFiles);
        } else {
            //Toast.makeText(this, "No offline files", Toast.LENGTH_LONG).show();
            showCustomSnackBar("No offline files", false, null, -2);
        }
    }

    private void populateRecyclerView(ArrayList<Music> musics) {
        if (isOfflineFromPrefs && musics.size() == 0) {
//            showView(noOfflineFiles);
        } else {
            hideView(noOfflineFiles);
        }
        musicAdapter = new MusicAdapter(this, musics);
        Log.d(TAG, "populateRecyclerView: Size: " + musics.size());
        recyclerView = findViewById(R.id.recyclerViewMusic);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(musicAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(PlayerActivity.this, recyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        //Get selected item
                        currentAudio = musics.get(position);
                        currentAudio.addVisualizer();
                        musicAdapter.notifyDataSetChanged();

                        //play selected audio
                        playSelectedAudio(currentAudio);

                        Log.d(TAG, "onItemClick:  " + currentAudio.toString());
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                }));
    }

    private void loadFirstAudioFile(ArrayList<Music> musicList) {
        currentAudio = musicList.get(0);
        loadFileIntoPlayer(currentAudio);
    }

    /*Can be used by background service to load file into player...*/
    private void loadFileIntoPlayer(Music audio) {
        audioName.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        Log.d(TAG, "loadFileIntoPlayer: url: " + audio.getFileUrl());
        Log.d(TAG, "loadFileIntoPlayer: Audio: " + audio.getFilename());
        if (isOfflineFromPrefs) {
            download.setImageResource(R.drawable.ic_check_circle);
            disableDownloadButton();
        } else {
            download.setImageResource(R.drawable.ic_file_download);
            enableDownloadButton();
        }
        //playSelectedAudio(audio);
    }

    //Use AudioService to play selected audio from recyclerView
    private void playSelectedAudio(Music audio) {
        play_pause.setChecked(false);
        audioName.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));

        Intent intent = new Intent(PlayerActivity.this, MainService.class);
        if (isAvailableOffline(audio)) {
            audio = currentAudio;
            intent.putExtra("isStreaming", false);
            intent.putExtra("uri", audio.getFileUrl());

            SharedPrefsManager.getInstance(PlayerActivity.this).setIsAudioStreaming(true);
            disableDownloadButton();

        } else {
            intent.putExtra("isStreaming", true);
            intent.putExtra("url", audio.getFileUrl());
            SharedPrefsManager.getInstance(PlayerActivity.this).setIsAudioStreaming(false);
            enableDownloadButton();
        }

        SharedPrefsManager.getInstance(PlayerActivity.this).setAudioFileUriOrUrl(audio.getFileUrl());
        SharedPrefsManager.getInstance(PlayerActivity.this).setCurrentAudioName(audio.getFilename());


        startService(intent); //Start intent activity with the parameters given

    }

    private void disableDownloadButton() {
        try {
            download.setEnabled(false);
            download.setImageResource(R.drawable.ic_check_circle);
        } catch (Exception e) {
            Log.i(TAG, "disableDownloadButton: Already disabled");
        }
    }

    private void enableDownloadButton() {
        try {
            download.setEnabled(true);
            download.setImageResource(R.drawable.ic_file_download);
            download.setOnClickListener(v -> startDownload(currentAudio));
        } catch (Exception e) {
            Log.i(TAG, "enableDownloadButton: Already enabled");
        }

    }

    private void showView(View v) {
        if (v.getVisibility() == View.INVISIBLE || v.getVisibility() == View.GONE)
            v.setVisibility(View.VISIBLE);
    }

    private void hideView(View v) {
        if (v.getVisibility() == View.VISIBLE)
            v.setVisibility(View.GONE);
    }

    private boolean isAvailableOffline(Music file) {
        File path = PlayerActivity.this.getExternalFilesDir("Download/" + currentAudio.getCategory() + "/");
        File mFile = new File(path, file.getFilename());
        if (mFile.exists())
            currentAudio.setFileUrl(String.valueOf(mFile));
        else
            Log.d(TAG, "isAvailableOffline: Offline file found: " + mFile);
        return mFile.exists();
    }

    private int getFileDuration(String uri) {
        if (uri == null)
            return 0;
        Log.d(TAG, "getFileDuration: 513 Path : " + uri);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return (Integer.parseInt(time) / 1000);
    }

    private void setUpDownloadManager() {
        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick,
                new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    public void startDownload(Music audio) {
        showView(downloadProgress);
        Uri uri = Uri.parse(audio.getFileUrl());
        Log.d(TAG, "startDownload: Attempting to download: " + uri);

        lastDownload = mgr.enqueue(new DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                        DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(audio.getFilename())
                .setDescription("Downloading...")
                .setDestinationInExternalFilesDir(PlayerActivity.this, DIRECTORY_DOWNLOADS,
                        File.separator + audio.getCategory() + File.separator + audio.getFilename())
        );
        download.setEnabled(false);
    }


    public void queryStatus(View v) {
        Cursor c = mgr.query(new DownloadManager.Query().setFilterById(lastDownload));

        if (c == null) {
            Toast.makeText(this, "Download not found!", Toast.LENGTH_LONG).show();
        } else {
            c.moveToFirst();

            Log.d(getClass().getName(), "COLUMN_ID: " +
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
            Log.d(getClass().getName(), "COLUMN_BYTES_DOWNLOADED_SO_FAR: " +
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
            Log.d(getClass().getName(), "COLUMN_LAST_MODIFIED_TIMESTAMP: " +
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
            Log.d(getClass().getName(), "COLUMN_LOCAL_URI: " +
                    c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            Log.d(getClass().getName(), "COLUMN_STATUS: " +
                    c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            Log.d(getClass().getName(), "COLUMN_REASON: " +
                    c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
            showCustomSnackBar(statusMessage(c), false, null, 0);
        }
    }

    public void viewLog(View v) {
        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
    }

    private String statusMessage(Cursor c) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete!";
                break;

            default:
                msg = "Download is nowhere in sight";
                break;
        }

        return (msg);
    }


    BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            enableDownloadButton();
            hideView(downloadProgress);
            showCustomSnackBar("Download Complete", false, null, 0);
        }
    };

    BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Toast.makeText(ctxt, "Downloading!", Toast.LENGTH_LONG).show();
        }
    };


    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted");
                } else {
                    Log.e(TAG, "Permission denied");
                }
        }
    }

    private void showCustomSnackBar(String message, boolean hasAction,
                                    @Nullable String actionText, int LENGTH) {
        Snackbar snackbar = Snackbar.make(download, message, LENGTH);
        if (hasAction) {
            snackbar.setAction(actionText, v -> retrieveAudioFiles());
        }
        snackbar.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy: ");

        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);

        PlayerActivity.this.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onClick(View v) {
        if (v == play_pause) {
            Intent intent = new Intent(PlayerActivity.this, MainService.class);

            intent.putExtra("play_pause", "play_pause");
            startService(intent);
        } else if (v == forward) {
            //int currentSongIndex = currentAudio.getId();
            //if (currentSongIndex < musicArrayList.size() - 1) {
            Intent intent = new Intent(PlayerActivity.this, MainService.class);
            intent.putExtra("action", "forward");
            startService(intent);
            //} else {
            // showCustomSnackBar("Cannot forward", false, null, 0);
            //}
        } else if (v == rewind) {
            //int currentSongIndex = currentAudio.getId();
            //if (currentSongIndex > 0) {
            Intent intent = new Intent(PlayerActivity.this, MainService.class);
            intent.putExtra("action", "rewind");
            startService(intent);
            //} else {
            //showCustomSnackBar("Cannot rewind", false, null, 0);
            //}
        }
    }

    @Override
    protected void onResume() {
        try {
            if (category.equals(SharedPrefsManager.getInstance(this).getCurrentCategory())) {
                if (SharedPrefsManager.getInstance(this).isBackGroundAudioPlaying())
                    seekBar.setProgress(SharedPrefsManager.getInstance(this).getCurrentPosition());
                audioName.setText(SharedPrefsManager.getInstance(this).getCurrentAudioName());
                //play_pause.setChecked(false);
            } else {
                seekBar.setProgress(0);
                //play_pause.setChecked(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "onResume: Caught:: " + e.getLocalizedMessage());
        }
        super.onResume();
    }
}