package com.liskovsoft.smartyoutubetv2.common.app.models.playback.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;

import java.util.ArrayList;
import java.util.List;

/** Disk-backed history; the small VideoStateService list remains only a recent-items cache. */
public final class PersonalHistoryDatabase extends SQLiteOpenHelper {
    public PersonalHistoryDatabase(Context context) {
        super(context, "personal_history.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history (profile TEXT NOT NULL, video_id TEXT NOT NULL, "
                + "state TEXT NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY (profile, video_id))");
        db.execSQL("CREATE INDEX history_recent ON history(profile, updated)");
        db.execSQL("CREATE TABLE migrated_profiles (profile TEXT PRIMARY KEY NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported history database upgrade");
    }

    public void importOnce(String profile, List<State> legacyStates) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor cursor = db.query("migrated_profiles", new String[]{"profile"},
                    "profile = ?", new String[]{profile}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    db.setTransactionSuccessful();
                    return;
                }
            }
            for (State state : legacyStates) {
                save(profile, state);
            }
            ContentValues values = new ContentValues();
            values.put("profile", profile);
            db.insertOrThrow("migrated_profiles", null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void save(String profile, State state) {
        if (state.video.videoId == null || state.video.videoId.isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("profile", profile);
        values.put("video_id", state.video.videoId);
        values.put("state", state.toString());
        values.put("updated", state.timestamp);
        getWritableDatabase().replaceOrThrow("history", null, values);
    }

    public State find(String profile, String videoId) {
        if (videoId == null) {
            return null;
        }
        try (Cursor cursor = getReadableDatabase().query("history", new String[]{"state"},
                "profile = ? AND video_id = ?", new String[]{profile, videoId}, null, null, null)) {
            return cursor.moveToFirst() ? State.from(cursor.getString(0)) : null;
        }
    }

    public List<State> recent(String profile, int limit) {
        List<State> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("history", new String[]{"state"},
                "profile = ?", new String[]{profile}, null, null, "updated DESC, rowid DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                State state = State.from(cursor.getString(0));
                if (state != null) {
                    result.add(0, state);
                }
            }
        }
        return result;
    }

    public void remove(String profile, String videoId) {
        getWritableDatabase().delete("history", "profile = ? AND video_id = ?", new String[]{profile, videoId});
    }

    public void clear(String profile) {
        // Keep the migration marker so deleted history cannot return from legacy preferences.
        getWritableDatabase().delete("history", "profile = ?", new String[]{profile});
    }
}
