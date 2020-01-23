package com.traumtool.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.piasy.rxandroidaudio.PlayConfig;
import com.github.piasy.rxandroidaudio.RxAudioPlayer;
import com.google.android.material.snackbar.Snackbar;
import com.traumtool.R;
import com.traumtool.adapters.MusicAdapter;
import com.traumtool.interfaces.ApiService;
import com.traumtool.models.FileResponse;
import com.traumtool.models.Music;
import com.traumtool.utils.AppUtils;
import com.traumtool.utils.RecyclerItemClickListener;
import com.traumtool.utils.SharedPrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener,MediaPlayer.OnPreparedListener {

    private ArrayList<Music> musicArrayList = new ArrayList<>();
    ArrayList<Music> offlineFiles = new ArrayList<>();
    private static final String TAG = "PlayerActivity";
    private ImageButton rewind, play_pause, forward, download;
    private SeekBar seekBar;
    private TextView realTime, audioName, audioNameOverlay, audioDurationOverlay;
    private ProgressBar bufferProgressBar, downloadProgress;
    private RecyclerView recyclerView;
    private boolean isStreaming = false, isDownloaded = false;

    private MediaPlayer mediaPlayer;
    private Music playingAudio;
    private int mediaFileLength;
    private int realTimeLength;
    private String category;

    RxAudioPlayer player;
    AsyncTask<String, String, String> streamTask;

    private static final int PERMISSION_REQUEST = 100;
    final Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        category = getIntent().getStringExtra("category");

        requestPermission();
        findViews();
        initializeStuff();

        if (SharedPrefsManager.getInstance(this).isOffline()) {
            getFiles();
        } else {
            retrieveAudioFiles();
        }

    }

    //Download audio list from server
    private void retrieveAudioFiles() {
        if (getIntent().hasExtra("category")) {
            ApiService service = AppUtils.getApiService();
            service.getFileList(getIntent().getStringExtra("category"))
                    .enqueue(new Callback<FileResponse>() {
                        @Override
                        public void onResponse(Call<FileResponse> call, Response<FileResponse> response) {
                            Toast.makeText(PlayerActivity.this, "Retrieved audios", Toast.LENGTH_SHORT).show();

                            try {
                                List<Music> audios = response.body().getAudio();
                                musicArrayList.addAll(audios);
                            } catch (Exception e) {
                                Log.d(TAG, "onResponse: List null or non existent");
                            }

                            populateRecyclerView(musicArrayList);

                            loadFirstAudioFile();
                        }

                        @Override
                        public void onFailure(Call<FileResponse> call, Throwable t) {
                            Log.d(TAG, "onFailure: " + t.getMessage());
                        }
                    });
        }
    }

    private void loadFirstAudioFile() {
        playingAudio = musicArrayList.get(0);
        loadFileIntoPlayer(playingAudio);

    }

    private void loadFileIntoPlayer(Music audio) {
        audioName.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        audioNameOverlay.setText(AppUtils.removeFileExtensionFromString(audio.getFilename()));
        Log.d(TAG, "loadFileIntoPlayer: url: " + audio.getFileUrl());
        Log.d(TAG, "loadFileIntoPlayer: Audio: " + audio.getFileUrl());
        if (SharedPrefsManager.getInstance(getApplicationContext()).isOffline()) {
            playAudio();
        } else {
            download.setOnClickListener(v -> downloadAudio(audio));
            streamAudioFromServer(audio.getFileUrl());
        }
    }

    private void populateRecyclerView(ArrayList<Music> musics) {
        recyclerView = findViewById(R.id.recyclerViewMusic);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(new MusicAdapter(this, musics));

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(PlayerActivity.this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (SharedPrefsManager.getInstance(PlayerActivity.this).isOffline()) {
                            playingAudio = offlineFiles.get(position);
//                            PlayConfig.file(new File(playingAudio.getFileUrl())) // play a local file
//                                    //.res(getApplicationContext(), R.raw.audio_record_end) // or play a raw resource
//                                    .looping(true) // loop or not
//                                    .leftVolume(1.0F) // left volume
//                                    .rightVolume(1.0F) // right volume
//                                    .build(); // build this config and play!
//                            player= new
//                            player.play(PlayConfig.file(new File(playingAudio.getFileUrl())).looping(true).build())
//                                    .subscribeOn(Schedulers.io())
//                                    .subscribe(new Observer<Boolean>() {
//                                        @Override
//                                        public void onSubscribe(final Disposable disposable) {
//                                            Log.d(TAG, "onSubscribe: Start playing");
//                                        }
//
//                                        @Override
//                                        public void onNext(final Boolean aBoolean) {
//                                            // prepared
//                                            Log.d(TAG, "onNext: " + aBoolean);
//                                        }
//
//                                        @Override
//                                        public void onError(final Throwable throwable) {
//                                            throwable.printStackTrace();
//                                        }
//
//                                        @Override
//                                        public void onComplete() {
//                                            Log.d(TAG, "onComplete: Finished playing!");
//                                            // play finished
//                                            // NOTE: if looping, the Observable will never finish, you need stop playing
//                                            // onDestroy, otherwise, memory leak will happen!
//                                        }
//                                    });
                            try {
                                mediaPlayer.reset();

                                mediaPlayer.setDataSource(new File(playingAudio.getFileUrl()).getAbsolutePath());
                                mediaPlayer.prepareAsync();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            loadFileIntoPlayer(playingAudio);
                            playAudio();
                            updateSeekBar();
                        } else {
                            playingAudio = musicArrayList.get(position);
                            if (isStreaming)
                                streamTask.cancel(true);

                            if (isAvailableOffline(playingAudio)) {
                                try {
                                    playFromOffline(playingAudio);
                                } catch (IOException e) {
                                    Log.d(TAG, "onItemClick: Unable to open file reason: " + e.getLocalizedMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                loadFileIntoPlayer(playingAudio);
                            }
                        }

                        Log.d(TAG, "onItemClick: " + playingAudio.toString());

                        stopMediaPlayer();

                        seekBar.setProgress(0);
                        realTime.setText("00:00");
                        audioDurationOverlay.setText("00:00");
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

    }


    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }
    }

    private void initializeStuff() {
        play_pause.setOnClickListener(v -> {
            playAudio();
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);

    }

    @SuppressLint("StaticFieldLeak")
    private void streamAudioFromServer(String url) {
        streamTask = new AsyncTask<String, String, String>() {
            @Override
            protected void onPreExecute() {
                isStreaming = true;
                bufferProgressBar.setVisibility(View.VISIBLE);

                if (isCancelled()) {
                    isStreaming = false;
                    try {
                        mediaPlayer.stop();
                        mediaPlayer.setDataSource("");
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    mediaPlayer.setDataSource(strings[0]);
                    mediaPlayer.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                bufferProgressBar.setVisibility(View.GONE);
                mediaFileLength = mediaPlayer.getDuration();
                audioDurationOverlay.setText(AppUtils.formatStringToTime(mediaFileLength));
                realTimeLength = mediaFileLength;
                playAudio();
                updateSeekBar();
            }
        };

        streamTask.execute(url);
    }

    private void playFromOffline(Music playingAudio) throws IOException {
        File path = PlayerActivity.this.getExternalFilesDir("Download/" + playingAudio.getCategory() + "/");
        File file = new File(path, playingAudio.getFilename());

        mediaPlayer.setDataSource(String.valueOf(file));
        mediaFileLength = mediaPlayer.getDuration();
        audioDurationOverlay.setText(AppUtils.formatStringToTime(mediaFileLength));
        realTimeLength = mediaFileLength;
        playAudio();
        updateSeekBar();
    }


    private void playAudio() {
        Log.d(TAG, "onItemClick: media: " + mediaPlayer.getDuration());
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
            mediaPlayer.start();
            play_pause.setImageResource(R.drawable.ic_pause);
        } else {
            mediaPlayer.pause();
            play_pause.setImageResource(R.drawable.ic_play);
        }
    }

    private void stopMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    private void updateSeekBar() {
        seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLength) * 100));
        if (mediaPlayer.isPlaying()) {
            Runnable updater = () -> {
                updateSeekBar();
                realTimeLength -= 1000; //1 second
                realTime.setText(AppUtils.formatStringToTime(realTimeLength));
            };
            handler.postDelayed(updater, 1000);
        }
    }

    private void findViews() {
        rewind = findViewById(R.id.img_rewind);
        play_pause = findViewById(R.id.img_play_pause);
        forward = findViewById(R.id.img_forward);
        bufferProgressBar = findViewById(R.id.buffering_progress_bar);
        downloadProgress = findViewById(R.id.download_progress_bar);
        realTime = findViewById(R.id.tvRealTime);
        download = findViewById(R.id.img_download);

        audioName = findViewById(R.id.tvAudioName);
        audioNameOverlay = findViewById(R.id.tvAudioNameOverlay);
        audioDurationOverlay = findViewById(R.id.tvAudioDurationOverlay);

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setMax(99);//100% (0 - 99)
        //TODO: Handle seekbar interactions
        seekBar.setOnTouchListener((v, event) -> {
            if (mediaPlayer.isPlaying()) {
                SeekBar sb = (SeekBar) v;
                int playPosition = (mediaFileLength / 100 * sb.getProgress());
                mediaPlayer.seekTo(playPosition);
            }

            return false;
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        play_pause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        seekBar.setSecondaryProgress(percent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void downloadAudio(Music audio) {

        showView(downloadProgress);

        /*
         * VOLLEY CODE HERE
         * not in use though
         * but just in case retrofit stops working again !!!
         * TODO: will delete after thorough testing
         *
         */

        //String url = AppUtils.BASE_URL + "data/" + audio.getCategory() + "/" + audio.getFilename();
        /*Log.d(TAG, "downloadAudio: This is the url: " + url);
        //Try volley:
        InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {

                        hideView(downloadProgress);
                        try {
                            if (response != null) {
                                FileOutputStream outputStream;
                                String name = audio.getFilename();
                                outputStream = openFileOutput(name, Context.MODE_PRIVATE);
                                outputStream.write(response);
                                outputStream.close();
                                Toast.makeText(PlayerActivity.this, "Download complete", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PlayerActivity.this, "Bad response", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onResponse: Unable to download file: ");
                            e.printStackTrace();
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideView(downloadProgress);
                error.printStackTrace();
            }
        }, null);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext(), new HurlStack());
        queue.add(request);*/


        String url = "data/" + audio.getCategory() + "/" + audio.getFilename();
        ApiService service = AppUtils.getApiDownloadService();
        service.downloadFile(url).
                enqueue(new Callback<ResponseBody>() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        //TODO: In case it stops working (again) Delete this AsyncTask and remove the @Streaming annotation from the API
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                if (response.body() != null) {
                                    isDownloaded = writeResponseBodyToDisk(response.body());//Write downloaded file to disk
                                } else {
                                    showCustomSnackBar("Something went wrong", true, "Try Again");
                                }
                                return null;
                            }
                        }.execute();

                        hideView(downloadProgress);
                        if (isDownloaded)
                            showCustomSnackBar("Download Complete", false, null);
                        else {
                            showCustomSnackBar("Something went wrong", true, "Try Again");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d(TAG, "onFailure: " + call.toString());
                        hideView(downloadProgress);
                        Toast.makeText(PlayerActivity.this, "Failed to get file, reason: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showView(View v) {
        v.setVisibility(View.VISIBLE);
    }

    private void hideView(View v) {
        v.setVisibility(View.INVISIBLE);
    }


    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File path = PlayerActivity.this.getExternalFilesDir("Download/" + playingAudio.getCategory() + "/");

            InputStream inputStream = null;
            OutputStream outputStream = null;
            File audioFile = new File(path, playingAudio.getFilename());

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(audioFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAvailableOffline(Music file) {
        File path = PlayerActivity.this.getExternalFilesDir("Download/" + playingAudio.getCategory() + "/");
        File mFile = new File(path, file.getFilename());
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
                    path + "/" + f.getName()
            );

            if (music.getFilename().isEmpty() || music.getFilename() == null) {
                Log.d(TAG, "getFiles: Is empty");
            } else {
                offlineFiles.add(music);
            }
        }
        populateRecyclerView(offlineFiles);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

    private void showCustomSnackBar(String message, boolean hasAction, @Nullable String actionText) {
        Snackbar snackbar = Snackbar.make(download, message, Snackbar.LENGTH_SHORT);
        if (hasAction) {
            snackbar.setAction(actionText, v -> downloadAudio(playingAudio));
        }
        snackbar.show();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared: prepared listener");
        mediaPlayer.start();
    }
}
