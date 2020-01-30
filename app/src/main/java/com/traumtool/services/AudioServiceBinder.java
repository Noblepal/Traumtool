package com.traumtool.services;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.traumtool.utils.SharedPrefsManager;

import java.io.IOException;

public class AudioServiceBinder extends Binder {

    // Save local audio file uri ( local storage file. ).
    private Uri audioFileUri = null;

    private static final String TAG = "AudioServiceBinder";

    // Save web audio file url.
    private String audioFileUrl = "";

    // Check if stream audio.
    private boolean streamAudio = false;

    // Media player that play audio.
    private MediaPlayer audioPlayer = null;

    //Boolean to check whether player is playing
    private boolean isAudioPlaying = false;

    // Caller activity context, used when play local audio file.
    private Context context = null;

    // This Handler object is a reference to the caller activity's Handler.
    // In the caller activity's handler, it will update the audio play progress.
    private Handler audioProgressUpdateHandler;
    private Handler secondaryAudioProgressUpdateHandler;

    // This is the message signal that inform audio progress updater to update audio progress.
    public final int UPDATE_AUDIO_PROGRESS_BAR = 1;

    public int bufferProgressPercent = 0;

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

    public void setSecondaryAudioProgressUpdateHandler(Handler secondaryAudioProgressUpdateHandler) {
        this.secondaryAudioProgressUpdateHandler = secondaryAudioProgressUpdateHandler;
    }

    public boolean isPlayerPlaying() {
        return isAudioPlaying;
    }

    // Initialise audio player.
    private void initAudioPlayer() {
        try {
            audioPlayer = new MediaPlayer();

            if (!TextUtils.isEmpty(getAudioFileUrl())) {
                if (isStreamAudio()) {
                    audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    Log.d(TAG, "Attempting to set url -> File Url: " + getAudioFileUrl());
                    audioPlayer.setDataSource(getAudioFileUrl());
                }
            } else if (!getAudioFileUri().toString().equals("")) {
                Log.d(TAG, "Attempting to set uri -> File URI: " + getAudioFileUri());
                audioPlayer.setDataSource(getContext(), getAudioFileUri());
            } else {
                Toast.makeText(context, "Null file url / uri", Toast.LENGTH_SHORT).show();
            }
            audioPlayer.prepare();

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
                Log.e("onBufferingUpdate", "" + percent);
                bufferProgressPercent += percent;
            });
            audioPlayer.setOnPreparedListener(mp -> {

            });
            //TODO: Change to already implemented setOnCompletionListener in this activity
            audioPlayer.setOnCompletionListener(mp -> Log.e("onCompletion", "Yes"));
            //TODO: Also remove this - not used at all
            audioPlayer.setOnInfoListener((mp, what, extra) -> false);


            // This thread object will send update audio progress message to caller activity every 1 second.
            Thread updateAudioProgressThread = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        // Create update audio progress message.
                        Message updateAudioProgressMsg = new Message();
                        updateAudioProgressMsg.what = UPDATE_AUDIO_PROGRESS_BAR;

                        // Send the message to caller activity's update audio prgressbar Handler object.
                        audioProgressUpdateHandler.sendMessage(updateAudioProgressMsg);

                        // Sleep one second.
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            // Run above thread object.
            updateAudioProgressThread.start();

            // This thread object will send update audio progress message to caller activity every 1 second.
            Thread updateSecondaryAudioProgressThread = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        // Create update audio progress message.
                        Message updateAudioProgressMsg = new Message();
                        updateAudioProgressMsg.what = UPDATE_AUDIO_PROGRESS_BAR;

                        // Send the message to caller activity's update audio progressbar Handler object.
                        secondaryAudioProgressUpdateHandler.sendMessage(updateAudioProgressMsg);

                        // Sleep one, second..
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            // Run above thread object.
            updateSecondaryAudioProgressThread.start();

            isAudioPlaying = true;

        } catch (
                IOException ex) {
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
        return ret;
    }

    // Return total audio file duration.
    public int getTotalAudioDuration() {
        int ret = 0;
        if (audioPlayer != null) {
            ret = audioPlayer.getDuration();
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
        }
        return ret;
    }

    // Start play audio.
    public void startAudio() {
        initAudioPlayer();
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
            SharedPrefsManager.getInstance(context).setIsBackgroundAudioPlaying(false);
            destroyAudioPlayer();
        }
    }

}