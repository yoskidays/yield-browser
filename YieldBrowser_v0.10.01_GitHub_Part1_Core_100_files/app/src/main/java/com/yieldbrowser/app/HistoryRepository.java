package com.yieldbrowser.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Persistent, single-source browser history backend.
 *
 * <p>All database work is serialized on one background executor. Results are delivered on the
 * main thread. The public API intentionally exposes keyset pagination rather than OFFSET so a
 * newly-recorded visit cannot make an already-visible page jump or duplicate rows.</p>
 */
final class HistoryRepository {
    interface ResultCallback<T> {
        void onResult(T result);
    }

    interface CompletionCallback {
        void onComplete(boolean success);
    }

    private static final String DB_NAME = "yield_history_v2.db";
    private static final int DB_VERSION = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private static volatile HistoryRepository instance;

    private final HistoryDatabase helper;
    private final ExecutorService executor;
    private final Handler mainHandler;

    static HistoryRepository getInstance(Context context) {
        HistoryRepository current = instance;
        if (current != null) return current;
        synchronized (HistoryRepository.class) {
            current = instance;
            if (current == null) {
                current = new HistoryRepository(context.getApplicationContext());
                instance = current;
            }
            return current;
        }
    }

    private HistoryRepository(Context context) {
        helper = new HistoryDatabase(context);
        try {
            helper.setWriteAheadLoggingEnabled(true);
        } catch (Exception ignored) {
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Yield-History-V2");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        mainHandler = new Handler(Looper.getMainLooper());
    }

    void recordVisit(String title, String url, long visitTime) {
        final String cleanUrl = safe(url);
        if (cleanUrl.isEmpty()) return;
        final String cleanTitle = safe(title).isEmpty() ? cleanUrl : safe(title);
        final String host = extractHost(cleanUrl);
        final long safeTime = visitTime > 0L ? visitTime : System.currentTimeMillis();

        executor.execute(() -> {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = helper.getWritableDatabase();
                db.beginTransaction();

                long existingId = -1L;
                long previousTime = 0L;
                int visitCount = 0;
                cursor = db.query(
                        HistoryDatabase.TABLE,
                        new String[]{"id", "last_visit_time", "visit_count"},
                        "url = ?",
                        new String[]{cleanUrl},
                        null,
                        null,
                        null,
                        "1"
                );
                if (cursor.moveToFirst()) {
                    existingId = cursor.getLong(0);
                    previousTime = cursor.getLong(1);
                    visitCount = cursor.getInt(2);
                }
                cursor.close();
                cursor = null;

                ContentValues values = new ContentValues();
                values.put("title", cleanTitle);
                values.put("url", cleanUrl);
                values.put("host", host);
                values.put("last_visit_time", Math.max(previousTime, safeTime));
                // Lifecycle callbacks can report the same page several times in quick succession.
                // Treat those as one visit while still refreshing title and timestamp.
                int nextCount = safeTime - previousTime < 1500L
                        ? Math.max(1, visitCount)
                        : Math.max(1, visitCount + 1);
                values.put("visit_count", nextCount);

                if (existingId >= 0L) {
                    db.update(HistoryDatabase.TABLE, values, "id = ?",
                            new String[]{String.valueOf(existingId)});
                } else {
                    values.put("visit_count", 1);
                    db.insertOrThrow(HistoryDatabase.TABLE, null, values);
                }

                db.setTransactionSuccessful();
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception ignored) {
                    }
                }
                if (db != null && db.inTransaction()) {
                    try {
                        db.endTransaction();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    void queryPage(String query, long beforeTime, long beforeId, int requestedLimit,
                   ResultCallback<List<HistoryItemData>> callback) {
        final String normalizedQuery = safe(query).trim();
        final long cursorTime = beforeTime <= 0L ? Long.MAX_VALUE : beforeTime;
        final long cursorId = beforeId <= 0L ? Long.MAX_VALUE : beforeId;
        final int limit = Math.max(1, Math.min(MAX_PAGE_SIZE, requestedLimit));

        executor.execute(() -> {
            ArrayList<HistoryItemData> result = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = helper.getReadableDatabase();
                StringBuilder selection = new StringBuilder(
                        "(last_visit_time < ? OR (last_visit_time = ? AND id < ?))"
                );
                ArrayList<String> args = new ArrayList<>();
                args.add(String.valueOf(cursorTime));
                args.add(String.valueOf(cursorTime));
                args.add(String.valueOf(cursorId));

                if (!normalizedQuery.isEmpty()) {
                    selection.append(" AND (title COLLATE NOCASE LIKE ? ESCAPE '\\'")
                            .append(" OR url COLLATE NOCASE LIKE ? ESCAPE '\\'")
                            .append(" OR host COLLATE NOCASE LIKE ? ESCAPE '\\')");
                    String like = "%" + escapeLike(normalizedQuery) + "%";
                    args.add(like);
                    args.add(like);
                    args.add(like);
                }

                cursor = db.query(
                        HistoryDatabase.TABLE,
                        new String[]{"id", "title", "url", "host", "last_visit_time", "visit_count"},
                        selection.toString(),
                        args.toArray(new String[0]),
                        null,
                        null,
                        "last_visit_time DESC, id DESC",
                        String.valueOf(limit)
                );

                while (cursor.moveToNext()) {
                    result.add(new HistoryItemData(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4),
                            cursor.getInt(5)
                    ));
                }
            } catch (Exception ignored) {
                result.clear();
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            postResult(callback, result);
        });
    }

    void deleteById(long id, CompletionCallback callback) {
        if (id <= 0L) {
            postCompletion(callback, false);
            return;
        }
        executor.execute(() -> {
            boolean success = false;
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                success = db.delete(HistoryDatabase.TABLE, "id = ?",
                        new String[]{String.valueOf(id)}) > 0;
            } catch (Exception ignored) {
            }
            postCompletion(callback, success);
        });
    }

    void clearAll(CompletionCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                db.delete(HistoryDatabase.TABLE, null, null);
                db.setTransactionSuccessful();
                success = true;
                db.endTransaction();
            } catch (Exception ignored) {
                try {
                    SQLiteDatabase db = helper.getWritableDatabase();
                    if (db.inTransaction()) db.endTransaction();
                } catch (Exception ignored2) {
                }
            }
            postCompletion(callback, success);
        });
    }

    private <T> void postResult(ResultCallback<T> callback, T result) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onResult(result));
    }

    private void postCompletion(CompletionCallback callback, boolean success) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onComplete(success));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static final class HistoryDatabase extends SQLiteOpenHelper {
        static final String TABLE = "history_entries";

        HistoryDatabase(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            try {
                db.setForeignKeyConstraintsEnabled(true);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "url TEXT NOT NULL UNIQUE,"
                    + "host TEXT NOT NULL DEFAULT '',"
                    + "last_visit_time INTEGER NOT NULL,"
                    + "visit_count INTEGER NOT NULL DEFAULT 1"
                    + ")");
            db.execSQL("CREATE INDEX index_history_time ON " + TABLE
                    + " (last_visit_time DESC, id DESC)");
            db.execSQL("CREATE INDEX index_history_host ON " + TABLE + " (host)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Future schema versions must add explicit, non-destructive migrations here.
            // Version 1 is intentionally left untouched so app updates preserve all history.
        }
    }
}
