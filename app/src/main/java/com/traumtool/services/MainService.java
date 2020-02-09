package com.traumtool.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.traumtool.R;
import com.traumtool.utils.SharedPrefsManager;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static com.traumtool.utils.AppUtils.logThis;

public class MainService extends Service {
    private final static int FOREGROUND_ID = 999;
    private static final String TAG = "MainService";
    private Uri audioFileUri = null;
    private String audioFileUrl = "";
    Intent broadCastIntent = null;
    private boolean streamAudio = false;
    private MediaPlayer audioPlayer = null;
    private boolean isAudioPlaying = false, isNewSong = true;
    private Context context = null;
    NotificationCompat.Builder builder;
    NotificationManager manager;
    private Handler audioProgressUpdateHandler;
    private Handler secondaryAudioProgressUpdateHandler;

    PendingIntent nextPendingIntent, playPausePendingIntent;

    public static final int notify = 1000;  //interval between two services(Here Service run every 5 seconds)
    int count = 0;  //number of times service is display
    private Handler mHandler = new Handler();   //run on another Thread to avoid crash
    private Timer mTimer = null;    //timer handling

    public final int UPDATE_AUDIO_PROGRESS_BAR = 1;
    public int bufferProgressPercent = 0;
    private int length = 0;

    //For notifications
    private static final String CHANNEL_ID = "channel_id01";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = MainService.this;
        if (intent != null) {
            if (intent.hasExtra("isStreaming")) {
                if (intent.getBooleanExtra("isStreaming", false)) {
                    setAudioFileUrl(intent.getStringExtra("url"));
                    setStreamAudio(true);
                } else {
                    setAudioFileUri(Uri.parse(intent.getStringExtra("uri")));
                    setStreamAudio(false);
                }
                initAudioPlayer();
                //showNotification();
            } else if (intent.hasExtra("play_pause")) {
                if (audioPlayer != null)
                    if (audioPlayer.isPlaying())
                        pauseAudio();
                    else
                        continuePlayback();
            } else if (intent.hasExtra("action")) {
                switch (intent.getStringExtra("action")) {
                    case "forward":
                        logThis(TAG, 0, "forwarding...");
                        forwardAudio();
                        break;
                    case "rewind":
                        logThis(TAG, 0, "rewinding...");
                        rewindAudio();
                        break;
                    case "stop":
                        logThis(TAG, 1, "Stopping media player, reason: Received category and current category do not match");
                        stopAudio();
                        break;
                }
            } else if (intent.hasExtra("request")) {
                isPlayerPlaying();
            } else {
                Log.i(TAG, "onStartCommand: No extra");
            }

        } else {
            Log.i(TAG, "onStartCommand: No intent");
        }

        return START_STICKY;
    }

    public void showNotification() {
        createNotificationChannel();

        //start PlayerActivity on by Tapping notification
        Intent playPauseIntent = new Intent(context, MainService.class);
        playPauseIntent.putExtra("play_pause", "play_pause");
        playPausePendingIntent = PendingIntent.getService(context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

       /* //Click Play/Pause button to start pause/play audio
        Intent nextIntent = new Intent(context, PlayerActivity.class);
        nextIntent.putExtra("play_pause", "play_pause");
        nextPendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_ONE_SHOT);*/

        //creating notification
        builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        //icon
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        //title
        builder.setContentTitle("Traumtool");
        //description
        builder.setContentText("Playing in the background");
        //set priority
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        //dismiss on tap
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        //start intent on notification tap (MainActivity)
        builder.setContentIntent(playPausePendingIntent);

        updateNotification();
    }

    private String getCurrentAudioName() {
        return null;
    }

    private void setCurrentAudioName(String _name) {

    }

    private void updateNotification() {
        //add action buttons to notification
        //icons will not displayed on Android 7 and above
        if (audioPlayer.isPlaying())
            builder.addAction(R.drawable.ic_pause, "Pause", playPausePendingIntent);
        else
            builder.addAction(R.drawable.ic_play, "Play", playPausePendingIntent);

        //builder.addAction(R.drawable.ic_skip_next_, "Next", nextPendingIntent);

        //notification manager
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Traumtool";
            String description = "Playing in background";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    // Initialise audio player.
    private void initAudioPlayer() {
        if (audioPlayer == null) {
            logThis(TAG, 0, "Player is null");
            audioPlayer = new MediaPlayer();
            logThis(TAG, 0, "New player initialized");
        }
        if (isPlayerPlaying()) {
            stopAudio();
            //Play new audio
            logThis(TAG, 0, "Stopped playing audio");
        } else {
            //Play new audio directly
            logThis(TAG, 0, "No audio playing");
        }

        try {
            stopAudio();
            if (!TextUtils.isEmpty(getAudioFileUrl())) {
                if (isStreamAudio()) {
                    audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    Log.d(TAG, "Attempting to set url -> File Url: " + getAudioFileUrl());
                    audioPlayer.setDataSource(getAudioFileUrl());
                } else {
                    audioPlayer.setDataSource(context, getAudioFileUri());
                }
            } else if (!getAudioFileUri().toString().equals("")) {
                Log.d(TAG, "Attempting to set uri -> File URI: " + getAudioFileUri());
                audioPlayer.setDataSource(context, getAudioFileUri());
            } else {
                Toast.makeText(context, "Null file url / uri", Toast.LENGTH_SHORT).show();
            }
            audioPlayer.prepare();
            Log.i(TAG, "initAudioPlayer: time:::" + audioPlayer.getDuration());

            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                //This is redundant code
                //TODO: Will remove this after testing
                if (extra == MediaPlayer.MEDIA_ERROR_SERVER_DIED
                        || extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
                    Log.d(TAG, "Media Player Error: " + extra);
                } else if (extra == MediaPlayer.MEDIA_ERROR_IO) {
                    return false;
                }
                return false;
            });
            //TODO: Change to already implemented setOnBufferingUpdateListener in this activity
            audioPlayer.setOnBufferingUpdateListener((mp, percent) -> {
                Log.i("onBufferingUpdate", "" + percent);
                if (isNewSong) {
                    bufferProgressPercent = 0;
                    isNewSong = false;
                } else {
                    bufferProgressPercent += percent;
                }
            });
            audioPlayer.setOnPreparedListener(mp -> {
                startAudio();
                showNotification();
                isAudioPlaying = true;
            });
            //TODO: Change to already implemented setOnCompletionListener in this activity
            audioPlayer.setOnCompletionListener(mp -> {
                //stopAudio();
            });
            //TODO: Also remove this - not used at all
            audioPlayer.setOnInfoListener((mp, what, extra) -> false);


            Log.i(TAG, "initAudioPlayer: time2:::" + audioPlayer.getDuration());
            isNewSong = true;
            startAudioProgressUpdateThread();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Destroy audio player.
    private void destroyAudioPlayer() {
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stop();
                isAudioPlaying = false;
            }
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    // Return current audio play position.
    public int getCurrentAudioPosition() {
        int ret = 0;
        if (audioPlayer != null) {
            ret = audioPlayer.getCurrentPosition();
        }
        Log.i(TAG, "getCurrentAudioPosition: " + ret);
        return ret;
    }

    // Return total audio file duration.
    public int getTotalAudioDuration() {
        int ret = 0;
        if (audioPlayer != null) {
            ret = audioPlayer.getDuration();
            Log.i(TAG, "initAudioPlayer: time7:::" + ret);
        }
        return ret;
    }

    // Return current audio player progress value.
    public int getAudioProgress() {
        int ret = 0;
        int currAudioPosition = getCurrentAudioPosition();
        int totalAudioDuration = getTotalAudioDuration();
        if (totalAudioDuration > 0) {
            ret = (currAudioPosition * 100) / totalAudioDuration;
            Log.i(TAG, "getAudioProgress: progress: " + ret);
        }
        return ret;
    }

    // Start play audio.
    public void startAudio() {
        //initAudioPlayer();
        if (audioPlayer != null) {
            audioPlayer.start();
            isAudioPlaying = true;
            SharedPrefsManager.getInstance(context).setIsBackgroundAudioPlaying(true);
        }
    }

    // Pause playing audio.
    public void pauseAudio() {
        if (audioPlayer != null) {
            isAudioPlaying = false;
            length = audioPlayer.getCurrentPosition();
            audioPlayer.pause();
        }
    }

    public void continuePlayback() {
        if (audioPlayer != null) {
            isAudioPlaying = false;
            audioPlayer.start();
        }
    }

    // Stop play audio.
    public void stopAudio() {
        if (audioPlayer != null) {
            isAudioPlaying = false;
            audioPlayer.stop();
            logThis(TAG, 0, "Stopping audio player");
            audioPlayer.reset();
            logThis(TAG, 0, "Resetting audio player");
            SharedPrefsManager.getInstance(context).setIsBackgroundAudioPlaying(false);
            SharedPrefsManager.getInstance(context).setCurrentPosition(audioPlayer.getCurrentPosition());
            /*Delete variables from local storage*/
            //SharedPrefsManager.getInstance(context).clearEverything();
            clearNotification();
        }
    }

    private void clearNotification() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.cancel(NOTIFICATION_ID);

        try {
            manager.cancelAll();
        } catch (Exception e) {
            Log.i(TAG, "clearNotification: Notification not running");
        }
    }

    public void forwardAudio() {
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                int current_position = audioPlayer.getCurrentPosition();
                current_position += 5000;
                if (current_position >= audioPlayer.getDuration()) {
                    logThis(TAG, 1, "Cannot forward anymore");
                } else {
                    audioPlayer.seekTo(current_position);
                }

            }
        }
    }

    public void rewindAudio() {
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                int current_position = audioPlayer.getCurrentPosition();
                current_position -= 5000;
                if (current_position <= 0) {
                    logThis(TAG, 1, "Cannot rewind anymore");
                } else {
                    audioPlayer.seekTo(current_position);
                }
            }

        }
    }

    class TimeDisplay extends TimerTask {
        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isStreamAudio()) {
                        broadCastIntent.putExtra("progress", getAudioProgress());
                        broadCastIntent.putExtra("sec_progress", getSecondaryAudioProgress());
                    } else {
                        broadCastIntent.putExtra("progress", getAudioProgress());
                    }
                    if (audioPlayer.isPlaying())
                        broadCastIntent.putExtra("isPlaying", true);
                    else
                        broadCastIntent.putExtra("isPlaying", false);

                    broadCastIntent.putExtra("dur", getTotalAudioDuration());

                    if (audioPlayer.isPlaying())
                        sendPositionBroadcast(broadCastIntent);


                    Log.i(TAG, "run: new position:: " + audioPlayer.getCurrentPosition());
                    SharedPrefsManager.getInstance(context).setCurrentPosition(audioPlayer.getCurrentPosition());
                }
            });
        }

    }

    private void startAudioProgressUpdateThread() {
        broadCastIntent = new Intent("Progress");
        if (mTimer != null) { // Cancel if already existed
            mTimer.cancel();
            mTimer.purge();
        }
        mTimer = new Timer();   //recreate new
        mTimer.scheduleAtFixedRate(new TimeDisplay(), 0, notify);   //Schedule task
    }


    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getAudioFileUrl() {
        return audioFileUrl;
    }

    public void setAudioFileUrl(String audioFileUrl) {
        this.audioFileUrl = audioFileUrl;
    }

    public boolean isStreamAudio() {
        return streamAudio;
    }

    public void setStreamAudio(boolean streamAudio) {
        this.streamAudio = streamAudio;
    }

    public Uri getAudioFileUri() {
        return audioFileUri;
    }

    public void setAudioFileUri(Uri audioFileUri) {
        this.audioFileUri = audioFileUri;
    }

    public Handler getAudioProgressUpdateHandler() {
        return audioProgressUpdateHandler;
    }

    public void setAudioProgressUpdateHandler(Handler audioProgressUpdateHandler) {
        this.audioProgressUpdateHandler = audioProgressUpdateHandler;
    }

    public Handler getSecondaryAudioProgressUpdateHandler() {
        return secondaryAudioProgressUpdateHandler;
    }

    // Return current audio player progress value.
    public int getSecondaryAudioProgress() {
        return bufferProgressPercent;
    }

    public void setSecondaryAudioProgressUpdateHandler(Handler
                                                               secondaryAudioProgressUpdateHandler) {
        this.secondaryAudioProgressUpdateHandler = secondaryAudioProgressUpdateHandler;
    }

    public boolean isPlayerPlaying() {
        return isAudioPlaying;
    }

    private void sendPositionBroadcast(Intent intent) {
        Log.i(TAG, "initAudioPlayer: time4:::" + audioPlayer.getDuration());
        intent.putExtra("progress", getAudioProgress());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: Called");
        mTimer.cancel();
        stopForeground(true);
        manager.cancelAll();
        destroyAudioPlayer();

        /*Delete variables from local storage*/
        SharedPrefsManager.getInstance(context).clearEverything();
    }
}
