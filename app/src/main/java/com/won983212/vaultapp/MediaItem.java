package com.won983212.vaultapp;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.won983212.vaultapp.database.MediaFileInfoEntity;
import com.won983212.vaultapp.util.Usefuls;

import java.util.Arrays;

public class MediaItem {
    public static final String DIRECTORY_MIME = "directory";

    public final String obfuscatedName;
    public final String type;
    public final String videoLength;
    public final long lastModified;
    public String[] tags;
    public String title;
    private String path;
    public int sortType;
    public int viewCount;

    private DocumentFile file;
    private Uri fileUri;

    private Uri folderThumbnailUri;
    private String folderThumbnailName;

    public MediaItem(MediaFileInfoEntity ent) {
        this.obfuscatedName = ent.obfuscatedName;
        this.type = ent.mimeType;
        this.title = ent.actualName;
        this.lastModified = ent.lastModified;
        this.tags = ent.tagListString.trim().length() > 0 ? ent.tagListString.split(",") : new String[0];
        this.path = ent.path;
        this.videoLength = ent.videoLength;
        this.folderThumbnailName = ent.thumbnailMediaName;
        this.sortType = ent.sortType;
        this.viewCount = ent.viewCount;
    }

    public boolean isDirectory() {
        return type.equals(DIRECTORY_MIME);
    }

    public boolean isImage() {
        return type.startsWith("image");
    }

    public boolean isVideo() {
        return type.startsWith("video");
    }

    public Uri getFolderThumbnailUri() {
        if (folderThumbnailUri == null) {
            if (folderThumbnailName != null && folderThumbnailName.length() > 0) {
                MediaItem target = VaultApp.getInstance().getDataManager().getByHash(folderThumbnailName);
                if (target != null) {
                    folderThumbnailUri = target.getDocumentURI();
                } else {
                    folderThumbnailName = null;
                }
            }
        }
        return folderThumbnailUri;
    }

    public void setFolderThumbnail(String targetObfuscatedName) {
        folderThumbnailName = targetObfuscatedName;
        folderThumbnailUri = null;
    }

    public String getParentFolderName() {
        if (path.equals("/"))
            return null;
        return Usefuls.getLastFolderName(path);
    }

    public boolean isChildOf(MediaItem item) {
        return path.startsWith(item.getFileURL());
    }

    public void setPath(String path) {
        this.path = path;
        invalidateFileUri();
    }

    public String getFormattedPath() {
        if (path.equals("/"))
            return "/";

        MediaDatabaseManager dataManager = VaultApp.getInstance().getDataManager();
        StringBuilder sb = new StringBuilder();
        for (String ent : path.split("/")) {
            if (ent.length() > 0) {
                sb.append('/');
                sb.append(dataManager.getByHash(ent).title);
            }
        }
        return sb.toString();
    }

    public DocumentFile getDocumentFile(Context context) {
        if (file == null)
            file = ContentFileHandler.getInstance().getFile(context, path, obfuscatedName);
        return file;
    }

    public Uri getDocumentURI() {
        if (fileUri == null)
            fileUri = Usefuls.getVaultFolderFileURI(path, obfuscatedName);
        return fileUri;
    }

    public String getFileURL() {
        return path + obfuscatedName;
    }

    public void invalidateFileUri() {
        file = null;
        fileUri = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem item = (MediaItem) o;
        return lastModified == item.lastModified &&
                sortType == item.sortType &&
                type.equals(item.type) &&
                videoLength.equals(item.videoLength) &&
                title.equals(item.title) &&
                path.equals(item.path) &&
                Arrays.equals(tags, item.tags);
    }

    @Override
    public int hashCode() {
        return obfuscatedName.hashCode();
    }
}
