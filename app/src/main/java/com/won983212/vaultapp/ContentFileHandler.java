package com.won983212.vaultapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.won983212.vaultapp.database.MediaFileDatabase;
import com.won983212.vaultapp.ui.activity.MediaListActivity;
import com.won983212.vaultapp.util.Usefuls;

import java.io.FileNotFoundException;

public class ContentFileHandler {
    private static final String LAST_SELECTED_URI_KEY = "lastSelectedURI";
    private static ContentFileHandler INSTANCE;
    private DocumentFile folder;

    public void clearSelectedURI() {
        SharedPreferences preferences = MediaListActivity.getPrivatePreferences();
        if (preferences.contains(LAST_SELECTED_URI_KEY)) {
            preferences.edit().remove(LAST_SELECTED_URI_KEY).apply();
        }
    }

    public boolean requestSetRootPath(AppCompatActivity context, int requestCode) {
        SharedPreferences preferences = MediaListActivity.getPrivatePreferences();
        String uriPath = preferences.getString(LAST_SELECTED_URI_KEY, "");
        if (uriPath.length() > 0) {
            Uri uri = Uri.parse(uriPath);
            try {
                folder = DocumentFile.fromTreeUri(context, uri);
                if (folder == null || !folder.exists()) {
                    throw new FileNotFoundException("Can't find saved folder: " + uri.toString());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        context.startActivityForResult(intent, requestCode);
        return false;
    }

    public boolean parseActivityResult(Context context, Intent data) {
        if (data != null) {
            Uri uri = data.getData();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, flags);

            DocumentFile folder = DocumentFile.fromTreeUri(context, uri);
            if (folder != null) {
                SharedPreferences preferences = MediaListActivity.getPrivatePreferences();
                preferences.edit().putString(LAST_SELECTED_URI_KEY, uri.toString()).apply();
                this.folder = folder;

                if (MediaFileDatabase.hasDBFile(context)) {
                    Usefuls.newTask(() -> {
                        if (MediaFileDatabase.getInstance(context).restoreDatabaseFile(context)) {
                            VaultApp.getInstance().getDataManager().setupDatas(context);
                        }
                    });
                    return false;
                }
            } else {
                Toast.makeText(context, "URI를 얻어오지 못했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    public DocumentFile getRootDir() {
        return folder;
    }

    @Nullable
    public DocumentFile getFile(Context context, String... paths) {
        if (folder != null) {
            if (paths.length == 1 && paths[0].equals("/"))
                return folder;

            DocumentFile file = DocumentFile.fromTreeUri(context, Usefuls.getVaultFolderFileURI(paths));
            if (file == null || !file.exists())
                return null;
            return file;
        }
        return null;
    }

    public static ContentFileHandler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ContentFileHandler();
        return INSTANCE;
    }
}
