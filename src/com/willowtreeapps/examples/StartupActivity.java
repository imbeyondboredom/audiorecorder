package com.willowtreeapps.examples;

import com.actionbarsherlock.app.ActionBar;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;

import roboguice.inject.InjectView;

public class StartupActivity extends RoboSherlockFragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    @InjectView(R.id.audioList1) ListView files;
    @InjectView(R.id.encoding_spinner) Spinner spinner;
    ActionBar bar;
    private static final int LOAD_AUDIO_RECORDINGS = 1;
    AudioCursorAdapter adapter;

    private static AudioRecorder recorder = new AudioRecorder();
    private static MediaPlayer player = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MainApp.TAG, "onCreate");
        setContentView(R.layout.startup);

        Button insert = (Button)findViewById(R.id.insert_button);
        insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values = new ContentValues();
                values.put(AudioContentProvider.AudioFiles.TYPE,"mp3");
                values.put(AudioContentProvider.AudioFiles.PATH,"/sdcard/test.mp3");
                values.put(AudioContentProvider.AudioFiles.DATE,"12/12/12");
                getContentResolver().insert(AudioContentProvider.AudioFiles.CONTENT_URI,values);
            }
        });

        Button delete = (Button)findViewById(R.id.delete_button);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> paths = new ArrayList<String>(adapter.getCount());
                Cursor cursor = adapter.getCursor();
                cursor.moveToPosition(-1);
                while(cursor.moveToNext())
                {
                    paths.add(cursor.getString(
                            cursor.getColumnIndex(AudioContentProvider.AudioFiles.PATH)));
                }
                DeleteFiles deleteTask = new DeleteFiles();
                deleteTask.setContentResolver(getApplicationContext().getContentResolver());
                deleteTask.execute((String[])paths.toArray(new String[paths.size()]));
            }
        });

        ((Button)findViewById(R.id.stop_music)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.isPlaying())
                {
                    player.stop();
                }
            }
        });

        Button recordButton = (Button)findViewById(R.id.record_button);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder.isRecording()) {
                    recorder.stop();
                    ContentValues values = new ContentValues();
                    values.put(AudioContentProvider.AudioFiles.DATE, Calendar.getInstance().getTimeInMillis());
                    values.put(AudioContentProvider.AudioFiles.TYPE, "recording");
                    values.put(AudioContentProvider.AudioFiles.PATH, recorder.getPath());
                    getContentResolver().insert(AudioContentProvider.CONTENT_URI,values);
                    ((Button)view).setText(R.string.start_recording);
                }
                else {
                    //Get the path
                    Calendar c = Calendar.getInstance();
                    String path = getFilesDir()+"/"+c.getTimeInMillis()+".3gp";
                    recorder.setPath(path);

                    //Get the encoding
                    int encoding = 0;
                    try
                    {
                        Class t_class = MediaRecorder.AudioEncoder.class;
                        encoding = t_class.getField((String)spinner.getSelectedItem()).getInt(null);
                    }
                    catch(Exception e){
                        Log.e(MainApp.TAG,"Problem getting field: "+spinner.getSelectedItem(),e);
                    }
                    recorder.setEncoding(encoding);

                    try
                    {
                        recorder.start();
                        ((Button)view).setText(R.string.stop_recording);
                    }
                    catch(Exception e)
                    {
                        Log.e(MainApp.TAG,"Problem starting recorder",e);
                    }
                }
            }
        });

        if(recorder.isRecording())
        {
            recordButton.setText(R.string.stop_recording);
        }
        else
        {
            recordButton.setText(R.string.start_recording);
        }

        files.setAdapter(adapter);
        files.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (player.isPlaying()) {
                    player.stop();
                }
                try {
                    Cursor c = adapter.getCursor();
                    c.moveToPosition(i);
                    String path = c
                            .getString(c.getColumnIndex(AudioContentProvider.AudioFiles.PATH));
                    player.reset();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                        }
                    });
                    player.setDataSource(path);
                    player.prepareAsync();
                } catch (Exception e) {
                    Log.e(MainApp.TAG, "Problem playing sound", e);
                }
            }
        });
        getSupportLoaderManager().initLoader(LOAD_AUDIO_RECORDINGS,null, this);


        //Get encodings
        Class c = MediaRecorder.AudioEncoder.class;
        Field[] fields = c.getFields();
        ArrayList<String> t_encodingList = new ArrayList<String>();

        for(Field f: fields)
        {
            try
            {
                t_encodingList.add(f.getName());
            }
            catch(Exception e)
            {
                Log.e("WTA_AudioRecorder","Problem getting encodings",e);
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,t_encodingList);
        spinner.setAdapter(spinnerAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(player.isPlaying())
        {
            player.stop();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        Uri uri = AudioContentProvider.CONTENT_URI;
        String selection = null;
        String[] projection = {
                AudioContentProvider.AudioFiles._ID,
                AudioContentProvider.AudioFiles.TYPE,
                AudioContentProvider.AudioFiles.PATH,
                AudioContentProvider.AudioFiles.DATE
        };
        String[] args = null;
        String sortOrder = AudioContentProvider.AudioFiles.TYPE;


        return new CursorLoader(this,uri,projection,selection,args,sortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> objectLoader, Cursor cursor) {
        adapter = new AudioCursorAdapter(this,cursor,true);
        files.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> objectLoader) {
        //Don't need to do anything
    }

    private class AudioCursorAdapter extends CursorAdapter {

        public AudioCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getLayoutInflater().inflate(android.R.layout.simple_list_item_1,null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView t = (TextView)view.findViewById(android.R.id.text1);
            t.setText(cursor.getString(cursor.getColumnIndex(AudioContentProvider.AudioFiles.PATH)));
        }
    }

    private static class DeleteFiles extends AsyncTask<String,Void,Void>
    {
        private ContentResolver resolver;

        @Override
        protected Void doInBackground(String... strings) {
            if(strings == null || strings.length ==0)
                return null;

            StringBuilder builder = new StringBuilder();

            for(String path : strings)
            {
                File f = new File(path);
                if(f.exists())
                {
                    f.delete();
                }
                builder.append(AudioContentProvider.AudioFiles.PATH+"=? OR ");
            }
            builder.delete(builder.length()-4,builder.length());

            resolver.delete(AudioContentProvider.CONTENT_URI,builder.toString(),strings);

            return null;
        }

        public void setContentResolver(ContentResolver resolver)
        {
            this.resolver = resolver;
        }
    }
}

