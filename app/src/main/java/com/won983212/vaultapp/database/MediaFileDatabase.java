package com.won983212.vaultapp.database;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.won983212.vaultapp.ContentFileHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Database(entities = {MediaFileInfoEntity.class}, version = 1)
public abstract class MediaFileDatabase extends RoomDatabase {
    private static MediaFileDatabase INSTANCE = null;
    private static final String DB_NAME = "vault";

    public abstract MediaFileInfoDAO mediaFileInfoDao();

    public boolean backupDatabaseFile(Context context) {
        return backupDatabaseFile(context, null);
    }

    public boolean backupDatabaseFile(Context context, String suffix) {
        return copyDBFile(context, suffix, false);
    }

    public boolean restoreDatabaseFile(Context context) {
        close();
        context.deleteDatabase("vault");
        boolean success = copyDBFile(context, null, true);
        if (success)
            INSTANCE = null;
        return success;
    }

    public static boolean hasDBFile(Context context) {
        ContentFileHandler fileHandler = ContentFileHandler.getInstance();
        return fileHandler.getFile(context, "." + DB_NAME + ".db") != null;
    }

    private static boolean copyDBFile(Context context, String suffix, boolean reverse) {
        try {
            if (suffix == null)
                suffix = "";
            else
                suffix = "-" + suffix;
            copy(context, DB_NAME, "." + DB_NAME + suffix + ".db", reverse);
            copy(context, DB_NAME + "-shm", "." + DB_NAME + "-shm" + suffix + ".db", reverse);
            copy(context, DB_NAME + "-wal", "." + DB_NAME + "-wal" + suffix + ".db", reverse);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void copy(Context context, String name, String outputName, boolean reverse) throws IOException {
        ContentFileHandler fileHandler = ContentFileHandler.getInstance();
        DocumentFile root = fileHandler.getRootDir();
        DocumentFile file = fileHandler.getFile(context, outputName);
        if (file == null) {
            if (reverse) throw new IOException("There is no db file");
            file = root.createFile("application/vnd.sqlite3", outputName);
        }
        if (file == null) {
            throw new IOException("Can't find or create backup db file: " + outputName);
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            if (reverse) {
                is = context.getContentResolver().openInputStream(file.getUri());
                os = new FileOutputStream(context.getDatabasePath(name));
            } else {
                is = new FileInputStream(context.getDatabasePath(name));
                os = context.getContentResolver().openOutputStream(file.getUri());
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static MediaFileDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MediaFileDatabase.class) {
                INSTANCE = Room.databaseBuilder(context, MediaFileDatabase.class, DB_NAME).build();
            }
        }
        return INSTANCE;
    }
}
