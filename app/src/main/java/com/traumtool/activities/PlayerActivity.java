package com.traumtool.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.traumtool.services.AudioService;
import com.traumtool.services.AudioServiceBinder;
import com.traumtool.utils.AppUtils;
import com.traumtool.utils.RecyclerItemClickListener;
import com.traumtool.utils.SharedPrefsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<Music> musicArrayList = new ArrayList<>();
    private ArrayList<Music> offlineFiles = new ArrayList<>();
    private MusicAdapter musicAdapter;
    private static final String TAG = "PlayerActivity";
    private ImageButton rewind, forward, download, backButton;
    private ToggleButton play_pause;
    ImageView topImage;
    private SeekBar seekBar;
    private TextView realTime, audioName, audioNameOverlay, audioDurationOverlay, mainCategory;
    private ProgressBar bufferProgressBar, downloadProgress, loadingProgressBar;
    private RecyclerView recyclerView;
    private boolean isStreaming = false, isDownloaded = false, isOfflineFromPrefs;
    //private MediaPlayer mediaPlayer;
    private Music currentAudio;
    private int mediaFileLength;
    private int realTimeLength;
    private String category;
    private DownloadManager mgr = null;
    private long lastDownload = -1L;
    AsyncTask<String, String, String> streamTask;
    private static final int PERMISSION_REQUEST = 100;
    final Handler handler = new Handler();

    //For notifications
    private static final String CHANNEL_ID = "channel_id01";
    public static final int NOTIFICATION_ID = 1;

    //For background playing
    private AudioServiceBinder audioServiceBinder;
    private static Handler audioProgressUpdateHandler;
    private static Handler secondaryProgressUpdateHandler;
    //Service connection object -> bridge between activity and background service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            audioServiceBinder = (AudioServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //Extract category from incoming intent (Category Activity)
        category = getIntent().getStringExtra("category");

        requestPermission();
        findViews();
        initializeStuff();
        setUpDownloadManager();
        setupBackgroundAudioService();

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
    }

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
        mainCategory.setText(AppUtils.capitalizeEachWord(category));
        backButton = findViewById(R.id.imgBackPlayer);
        backButton.setOnClickListener(v -> onBackPressed());

        play_pause.setOnClickListener(this);

        audioName = findViewById(R.id.tvAudioName);
        audioNameOverlay = findViewById(R.id.tvAudioNameOverlay);
        audioDurationOverlay = findViewById(R.id.tvAudioDurationOverlay);

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setMax(99);//100% (0 - 99)
        //TODO: Handle seekbar interactions
        /*seekBar.setOnTouchListener((v, event) -> {
            if (mediaPlayer.isPlaying()) {
                SeekBar sb = (SeekBar) v;
                int playPosition = (mediaFileLength / 100 * sb.getProgress());
                mediaPlayer.seekTo(playPosition);
            }
            return false;
        });*/
    }

    private void initializeStuff() {
        //Initialize music player
       /* mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);*/
        //play_pause.setOnClickListener(v -> playPauseAudio(mediaPlayer));

        Glide.with(this).load("https://source.unsplash.com/random/?nature,water")
                .fallback(R.drawable.self_reflection)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .placeholder(R.drawable.self_reflection)
                .into(topImage);

    }

    private void setupBackgroundAudioService() {
        // Bind background audio service when activity is created.
        bindAudioService();
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

                            //loadFirstAudioFile(musicArrayList);
                            //loadFileIntoPlayer(musicArrayList.get(0));//Load first audio file into player
                        } else {
                            showCustomSnackBar("Failed to get data", true, "Try Again", -2);
                        }
                    }

                    @Override
                    public void onFailure(Call<FileResponse> call, Throwable t) {
                        hideView(loadingProgressBar);
                        showCustomSnackBar("Failed to get data. Possibly due to network error", true, "Try Again", -2);
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
        populateRecyclerView(offlineFiles);
    }

    private void populateRecyclerView(ArrayList<Music> musics) {
        musicAdapter = new MusicAdapter(this, musics);
        Log.d(TAG, "populateRecyclerView: Size: " + musics.size());
        recyclerView = findViewById(R.id.recyclerViewMusic);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(musicAdapter);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(PlayerActivity.this, recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                //Get selected item
                                currentAudio = musics.get(position);

                                //play selected audio
                                playSelectedAudio(currentAudio);

                                /*if (isOfflineFromPrefs) {
                                    currentAudio = musics.get(position); //Load files from offline list
                                    //startOfflinePlayer(currentAudio.getFileUrl());

                                } else {
                                    currentAudio = musics.get(position);
                                    if (isAvailableOffline(currentAudio)) {
                                        download.setImageResource(R.drawable.ic_check_circle);
                                        download.setOnClickListener(null);
                                        try {
                                            //playFromOffline(currentAudio);
                                            playSelectedAudio(currentAudio);
                                            return;
                                        } catch (Exception e) {
                                            Log.d(TAG, "onPreExecute: Unable to open file reason: " + e.getLocalizedMessage());
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (isStreaming)
                                            streamTask.cancel(true);
                                        //Play(Stream) the online file
                                        //loadFileIntoPlayer(currentAudio);
                                        playSelectedAudio(currentAudio);
                                    }
                                }*/

                                Log.d(TAG, "onItemClick:  " + currentAudio.toString());

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                // do whatever
                            }
                        })
        );

    }

    private void loadFirstAudioFile(ArrayList<Music> musicList) {
        currentAudio = musicList.get(0);
        if (isAvailableOffline(currentAudio)) {
            download.setImageResource(R.drawable.ic_check_circle);
            download.setOnClickListener(null);
            try {
                playFromOffline(currentAudio);
            } catch (IOException e) {
                Log.d(TAG, "onPreExecute: Unable to open file reason: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else {
            loadFileIntoPlayer(currentAudio);
        }
    }

    /*Can be used by background service to load file into player...*/
    private void loadFileIntoPlayer(Music audio) {
        audioName.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        audioNameOverlay.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        Log.d(TAG, "loadFileIntoPlayer: url: " + audio.getFileUrl());
        Log.d(TAG, "loadFileIntoPlayer: Audio: " + audio.getFileUrl());
        if (isOfflineFromPrefs) {
            download.setImageResource(R.drawable.ic_check_circle);
            download.setOnClickListener(null);
            //playPauseAudio();
        } else {
            //Check if the selected file is already available offline before streaming
            if (isAvailableOffline(audio)) {
                download.setImageResource(R.drawable.ic_check_circle);
                download.setOnClickListener(null);
                try {
                    playFromOffline(audio);
                } catch (IOException e) {
                    Log.d(TAG, "onPreExecute: Unable to open file reason: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            } else {
                download.setImageResource(R.drawable.ic_file_download);
                download.setOnClickListener(v -> startDownload(audio));
                //streamAudioFromServer(audio.getFileUrl());
            }
        }
    }

    //Use AudioService to play selected audio from recyclerView
    private void playSelectedAudio(Music audio) {
        audioName.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        audioNameOverlay.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        audioServiceBinder.setContext(getApplicationContext());

        if (audioServiceBinder.isPlayerPlaying())
            audioServiceBinder.stopAudio();

        if (isAvailableOffline(audio)) {
            audio = currentAudio;
            audioServiceBinder.setStreamAudio(false);
            audioServiceBinder.setAudioFileUri(Uri.parse(audio.getFileUrl()));
            disableDownloadButton();
        } else {
            audioServiceBinder.setStreamAudio(true);
            audioServiceBinder.setAudioFileUrl(audio.getFileUrl());
            enableDownloadButton();
        }
        createAudioProgressbarUpdater();
        createSecondaryAudioProgressbarUpdater();
        audioServiceBinder.setAudioProgressUpdateHandler(audioProgressUpdateHandler);
        audioServiceBinder.setSecondaryAudioProgressUpdateHandler(secondaryProgressUpdateHandler);

        // Start audio in background service.
        audioServiceBinder.startAudio();

        //Change play icon to pause icon
        //play_pause.setImageResource(R.drawable.ic_pause_main);

        audioDurationOverlay.setText(AppUtils.formatStringToTime(audioServiceBinder.getTotalAudioDuration()));

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
        } catch (Exception e) {
            Log.i(TAG, "enableDownloadButton: Already enabled");
        }

    }

    private void updateUI() {
        if (audioServiceBinder.isPlayerPlaying()) {
            audioServiceBinder.pauseAudio();
        } else {
            audioServiceBinder.continuePlayback();
        }
    }

    // I added this class to play offline mode
    //TIP new player has to be created
    /*public void startOfflinePlayer(String url) {
        if (TextUtils.isEmpty(url))
            return;
        if (mediaPlayer.isPlaying())
            try {
                stopMediaPlayer(mediaPlayer);
            } catch (Exception e) {
                Log.d(TAG, "startOfflinePlayer: Already stopped or not initialized");
            }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    //This is redundant code
                    //TODO: Will remove this after testing
                    if (extra == MediaPlayer.MEDIA_ERROR_SERVER_DIED
                            || extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
                        Log.d(TAG, "Media Player Error: " + extra);
                    } else if (extra == MediaPlayer.MEDIA_ERROR_IO) {
                        return false;
                    }
                    return false;
                }
            });
            //TODO: Change to already implemented setOnBufferingUpdateListener in this activity
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    Log.e("onBufferingUpdate", "" + percent);
                }
            });
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    playPauseAudio(mediaPlayer);
                    mediaFileLength = mp.getDuration();
                    realTimeLength = mediaFileLength;
                    audioNameOverlay.setText(AppUtils.removeFileExtensionFromString(currentAudio.getFilename()));
                    audioName.setText(AppUtils.removeFileExtensionFromString(currentAudio.getFilename()));
                    audioDurationOverlay.setText(AppUtils.formatStringToTime(realTimeLength));
                    updateSeekBar2(mp);
                }
            });
            //TODO: Change to already implemented setOnCompletionListener in this activity
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.e("onCompletion", "Yes");
                }
            });
            //TODO: Also remove this - not used at all
            mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }*/


   /* @SuppressLint("StaticFieldLeak")
    private void streamAudioFromServer(String url) {
        mediaPlayer = new MediaPlayer();
        streamTask = new AsyncTask<String, String, String>() {
            @Override
            protected void onPreExecute() {
                isStreaming = true;
                showView(bufferProgressBar);
                if (isCancelled()) {
                    isStreaming = false;
                    try {
                        stopMediaPlayer(mediaPlayer);
                        mediaPlayer.setDataSource("");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    mediaPlayer.setDataSource(strings[0]);
                    Log.d(TAG, "doInBackground: Setting data source: " + strings[0]);
                    mediaPlayer.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                hideView(bufferProgressBar);
                mediaFileLength = mediaPlayer.getDuration();
                audioDurationOverlay.setText(AppUtils.formatStringToTime(mediaFileLength));
                realTimeLength = mediaFileLength;
                playPauseAudio(mediaPlayer);
                updateSeekBar();
            }
        };
        streamTask.execute(url);
    }*/

    private void playFromOffline(Music playingAudio) throws IOException {
        showCustomSnackBar("Playing already downloaded audio", false, null, 0);
        File path = PlayerActivity.this.getExternalFilesDir("Download/" + playingAudio.getCategory() + "/");
        File file = new File(path, playingAudio.getFilename());

        //Play file from offline if it exists
        //startOfflinePlayer(String.valueOf(file));
    }

    /*private void playPauseAudio(MediaPlayer player) {
        Log.d(TAG, "playPauseAudio: media: " + mediaPlayer.getDuration());
        if (!player.isPlaying()) {
            player.seekTo(mediaPlayer.getCurrentPosition());
            player.start();
            play_pause.setImageResource(R.drawable.ic_pause_main);
        } else {
            player.pause();
            play_pause.setImageResource(R.drawable.ic_play_main);
        }
    }*/

    private void stopMediaPlayer(MediaPlayer mediaPlayer) {
        mediaPlayer.stop();
        mediaPlayer.reset();
        seekBar.setProgress(0);
        realTime.setText("00:00");
        audioDurationOverlay.setText("00:00");
    }

    private void updateSeekBar2(MediaPlayer mediaPlayer) {

        seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLength) * 100));
        Log.d(TAG, "updateSeekBar2: current" + AppUtils.formatStringToTime(realTimeLength));
        if (mediaPlayer.isPlaying()) {
            Runnable updater = () -> {
                updateSeekBar2(mediaPlayer);
                realTimeLength -= 1000; //1 second
                realTime.setText(AppUtils.formatStringToTime(realTimeLength));
            };
            handler.postDelayed(updater, 1000);
        }
    }

    /*private void updateSeekBar() {
        seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLength) * 100));
        if (mediaPlayer.isPlaying()) {
            Runnable updater = () -> {
                updateSeekBar();
                realTimeLength -= 1000; //1 second
                realTime.setText(AppUtils.formatStringToTime(realTimeLength));
            };
            handler.postDelayed(updater, 1000);
        }
    }*/


    private void showView(View v) {
        v.setVisibility(View.VISIBLE);
    }

    private void hideView(View v) {
        v.setVisibility(View.INVISIBLE);
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
       /* MediaPlayer player = MediaPlayer.create(getApplicationContext(), Uri.parse(uri));
        MediaPlayer pl2 = new MediaPlayer();*/
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

        lastDownload = mgr.enqueue(new DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                        DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(audio.getFilename())
                .setDescription("Please Wait")
                .setDestinationInExternalFilesDir(PlayerActivity.this, DIRECTORY_DOWNLOADS,
                        File.separator + audio.getCategory() + File.separator + audio.getFilename())
        );
        disableDownloadButton();
    }

    @SuppressLint("HandlerLeak")
    private void createAudioProgressbarUpdater() {
        /* Initialize audio progress handler. */
        if (audioProgressUpdateHandler == null) {
            audioProgressUpdateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // The update process message is sent from AudioServiceBinder class's thread object.
                    if (msg.what == audioServiceBinder.UPDATE_AUDIO_PROGRESS_BAR) {

                        if (audioServiceBinder != null) {
                            // Calculate the percentage.
                            int currProgress = audioServiceBinder.getAudioProgress();

                            // Update progressbar. Make the value 10 times to show more clear UI change.
                            seekBar.setProgress(currProgress);
                        }
                    }
                }
            };
        }
    }

    @SuppressLint("HandlerLeak")
    private void createSecondaryAudioProgressbarUpdater() {
        /* Initialize audio progress handler. */
        secondaryProgressUpdateHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == audioServiceBinder.UPDATE_AUDIO_PROGRESS_BAR) {

                    if (audioServiceBinder != null) {
                        // Calculate the percentage.
                        int currProgress = audioServiceBinder.getSecondaryAudioProgress();

                        // Update progressbar. Make the value 10 times to show more clear UI change.
                        seekBar.setSecondaryProgress(currProgress);
                    }
                }
            }
        };
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

            Toast.makeText(this, statusMessage(c), Toast.LENGTH_LONG).show();
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
            download.setEnabled(true);
            showCustomSnackBar("Download Complete", false, null, 0);
            hideView(downloadProgress);
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

    // Bind background service with caller activity. Then this activity can use
    // background service's AudioServiceBinder instance to invoke related methods.
    private void bindAudioService() {
        if (audioServiceBinder == null) {
            Intent intent = new Intent(PlayerActivity.this, AudioService.class);

            // Below code will invoke serviceConnection's onServiceConnected method.
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    // Unbound background audio service with caller activity.
    private void unBoundAudioService() {
        if (audioServiceBinder != null) {
            unbindService(serviceConnection);
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
            snackbar.setAction(actionText, v -> startDownload(currentAudio));
        }
        snackbar.show();
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (mediaPlayer.isPlaying()) {
            isStreaming = false;
            streamTask.cancel(true);
            mediaPlayer.stop();
        }*/
        Log.e(TAG, "onDestroy: ");

        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);
        unBoundAudioService();

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
            updateUI();
        }
    }

    public void showNotification() {
        createNotificationChannel();

        //start this(MainActivity) on by Tapping notification
        Intent playPauseIntent = new Intent(this, MainActivity.class);
        playPauseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent playPausePendingIntent = PendingIntent.getActivity(this, 0, playPauseIntent, PendingIntent.FLAG_ONE_SHOT);

        //Click Like button to start LikeActivity
        Intent nextIntent = new Intent(this, PlayerActivity.class);
        nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent nextPendingIntent = PendingIntent.getActivity(this, 0, nextIntent, PendingIntent.FLAG_ONE_SHOT);

        //Click Dislike button to start DislikeActivity
        Intent disIntent = new Intent(this, PlayerActivity.class);
        disIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent dislikePIntent = PendingIntent.getActivity(this, 0, disIntent, PendingIntent.FLAG_ONE_SHOT);


        //creating notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        //icon
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        //title
        builder.setContentTitle(AppUtils.removeFileExtensionFromString(currentAudio.getFilename()));
        //description
        builder.setContentText("Playing in background");
        //set priority
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        //dismiss on tap
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        //start intent on notification tap (MainActivity)
        builder.setContentIntent(playPausePendingIntent);

        //add action buttons to notification
        //icons will not displayed on Android 7 and above
        builder.addAction(R.drawable.ic_play_main, "Pause", nextPendingIntent);
        builder.addAction(R.drawable.ic_skip_next_, "Next", dislikePIntent);

        //notification manager
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = AppUtils.removeFileExtensionFromString(currentAudio.getFilename());
            String description = "Playing in background";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }


    @Override
    protected void onResume() {
        if (SharedPrefsManager.getInstance(getApplicationContext()).isBackGroundAudioPlaying()) {
            AudioServiceBinder binder = new AudioServiceBinder();
            binder.setContext(getApplicationContext());
            Log.d(TAG, "onResume: " + binder.getCurrentAudioPosition());
        }
        super.onResume();
    }
}