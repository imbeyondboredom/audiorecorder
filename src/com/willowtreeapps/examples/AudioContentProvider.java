package com.willowtreeapps.examples;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * User: charlie Date: 12/14/12 Time: 4:36 PM
 */
public class AudioContentProvider extends ContentProvider {

    // public constants for client development
    public static final String AUTHORITY = "com.willowtreeapps.examples.audio.provider.audio";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + AudioFiles.CONTENT_PATH);

    private static final int AUDIOFILE_LIST = 1;
    private static final int AUDIOFILE_ID = 2;
    private static final UriMatcher URI_MATCHER;

    public static interface AudioFiles extends BaseColumns {
        public static final Uri CONTENT_URI = AudioContentProvider.CONTENT_URI;
        public static final String ID = AudioDbHelper.KEY_ROWID;
        public static final String TYPE = AudioDbHelper.KEY_TYPE;
        public static final String PATH = AudioDbHelper.KEY_PATH;
        public static final String DATE = AudioDbHelper.KEY_DATE;
        public static final String CONTENT_PATH = "recordings";
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.willowtreeapps.audiofiles";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.willowtreeapps.audiofile";
        public static final String[] PROJECTION_ALL = {_ID, TYPE, PATH, DATE};
        public static final String SORT_ORDER_DEFAULT = TYPE + " ASC";
    }

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, AudioFiles.CONTENT_PATH, AUDIOFILE_LIST);
        URI_MATCHER.addURI(AUTHORITY, AudioFiles.CONTENT_PATH + "/#", AUDIOFILE_ID);
    }

    private AudioDbHelper dbHelper = null;
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        dbHelper = new AudioDbHelper(getContext());
        db = dbHelper.getDb();
        if (db == null) {
            return false;
        }
        if (db.isReadOnly()) {
            db.close();
            db = null;
            return false;
        }
        return true;

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(AudioDbHelper.TBL_FILES);
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = AudioFiles.SORT_ORDER_DEFAULT;
        }
        switch (URI_MATCHER.match(uri)) {
            case AUDIOFILE_LIST:
                // all nice and well
                break;
            case AUDIOFILE_ID:
                // limit query to one row at most:
                builder.appendWhere(AudioFiles._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        // if we want to be notified of any changes:
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case AUDIOFILE_LIST:
                return AudioFiles.CONTENT_TYPE;
            case AUDIOFILE_ID:
                return AudioFiles.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (URI_MATCHER.match(uri) != AUDIOFILE_LIST) {
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }
        long id = db.insert(AudioDbHelper.TBL_FILES, null, contentValues);
        if (id > 0) {
            // notify all listeners of changes and return itemUri:
            Uri itemUri = ContentUris.withAppendedId(uri, id);
            getContext().getContentResolver().notifyChange(itemUri, null);
            return itemUri;
        }
        // s.th. went wrong:
        throw new SQLException("Problem while inserting into " + AudioDbHelper.TBL_FILES + ", uri: " + uri); // use another exception here!!!
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int delCount = 0;
        switch (URI_MATCHER.match(uri)) {
            case AUDIOFILE_LIST:
                delCount = db.delete(AudioDbHelper.TBL_FILES, selection, selectionArgs);
                break;
            case AUDIOFILE_ID:
                String idStr = uri.getLastPathSegment();
                String where = AudioFiles._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                delCount = db.delete(AudioDbHelper.TBL_FILES, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        // notify all listeners of changes:
        if (delCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return delCount;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int updateCount = 0;
        switch (URI_MATCHER.match(uri)) {
            case AUDIOFILE_LIST:
                updateCount = db.update(AudioDbHelper.TBL_FILES, values, selection, selectionArgs);
                break;
            case AUDIOFILE_ID:
                String idStr = uri.getLastPathSegment();
                String where = AudioFiles._ID + " = " + idStr;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                }
                updateCount = db.update(AudioDbHelper.TBL_FILES, values, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        // notify all listeners of changes:
        if (updateCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updateCount;
    }
}
