package com.willowtreeapps.examples;

/**
 * User: charlie Date: 12/17/12 Time: 9:21 PM
 */
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

// Basic audio recorder class that adapts and refactors some code from MediaRecorder()
public class AudioRecorder {

    private MediaRecorder recorder;
    private String path;
    private int encoding= 0;
    private boolean isRecording = false;

    /**
     * Creates a new audio recording at the given path (relative to root of SD card).
     */
    public AudioRecorder() {
    }

    public void setPath(String path)
    {
        this.path = sanitizePath(path);
    }


    private String sanitizePath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.contains(".")) {
            path += ".3gp";
        }
        // Uncomment to save to SD card for testing - this will break playback, and you'll have to adjust
        // the playback filepath correspondingly if you want the device to play from the SD card
//        return Environment.getExternalStorageDirectory().getAbsolutePath() + path;
        return path;
    }

    public void setEncoding(int encoding)
    {
        this.encoding = encoding;
    }

    /**
     * Starts a new recording.
     */
    public void start() throws IOException {
        // make sure the directory we plan to store the recording in exists
        File directory = new File(path).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Path to file could not be created.");
        }

        //Don't start a recording if we're already recording
        if(isRecording || path == null)
        {
            return;
        }
        AudioManager am;
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(encoding);
        recorder.setAudioEncodingBitRate(16);
        recorder.setAudioSamplingRate(44100);
        recorder.setOutputFile(path);
        recorder.prepare();
        recorder.start();
        isRecording = true;
    }

    /**
     * Stops a recording that has been previously started.
     */
    public void stop() {
        isRecording = false;
        try
        {
            recorder.stop();
            recorder.release();
        }
        catch(IllegalStateException ise){
            Log.e("dunnwell","This would have crashed!");
        }
    }

    public String getPath() {
        return path;
    }

    public MediaRecorder getRecorder() {
        return this.recorder;
    }

    public boolean isRecording()
    {
        return isRecording;
    }
}

