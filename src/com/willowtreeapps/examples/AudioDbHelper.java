package com.willowtreeapps.examples;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * User: charlie Date: 12/14/12 Time: 4:41 PM
 */
public class AudioDbHelper {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_PATH = "link";
    public static final String KEY_DATE = "date";
    private static final String TAG = "DBAdapter";

    private static final String DATABASE_NAME = "audio_recording_db";
    public static final String TBL_FILES = "audio_files";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
            "create table "+ TBL_FILES +" ("+KEY_ROWID+" integer primary key autoincrement, "
                    + KEY_TYPE+" text not null, "+KEY_PATH+" text not null, "
                    + KEY_DATE+" text not null);";

    private final Context context;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public AudioDbHelper(Context ctx)
    {
        this.context = ctx;
        dbHelper = new DatabaseHelper(context);
    }

    public SQLiteDatabase getDb()
    {
        return dbHelper.getWritableDatabase();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            Log.d("AUDIO_RECORDER","Creating the db");
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                int newVersion)
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS titles");
            onCreate(db);
        }
    }
}
