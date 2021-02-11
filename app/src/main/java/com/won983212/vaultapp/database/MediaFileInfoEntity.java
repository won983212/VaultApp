package com.won983212.vaultapp.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@SuppressWarnings("CanBeFinal")
@Entity
public class MediaFileInfoEntity implements Serializable {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "obfuscated_name")
    public final String obfuscatedName;

    @ColumnInfo(name = "actual_name")
    public String actualName;

    @ColumnInfo(name = "mime_type")
    public String mimeType;

    @ColumnInfo(name = "last_modified")
    public long lastModified;

    /**
     * For only media
     **/
    @ColumnInfo(name = "path")
    public String path;

    /**
     * For only media
     **/
    @ColumnInfo(name = "tags", defaultValue = "")
    public String tagListString;

    /**
     * For only video
     **/
    @ColumnInfo(name = "video_length", defaultValue = "00:00")
    public String videoLength;

    /**
     * For only folder
     **/
    @ColumnInfo(name = "thumbnail_ptr", defaultValue = "")
    public String thumbnailMediaName;

    /**
     * For only folder
     **/
    @ColumnInfo(name = "sort_type", defaultValue = "0")
    public int sortType;

    @ColumnInfo(name = "view_count", defaultValue = "0")
    public int viewCount;

    @Ignore
    public MediaFileInfoEntity(@NonNull String obfuscatedName, String actualName, String mimeType, long lastModified, String path, String videoLength) {
        this(obfuscatedName, actualName, mimeType, lastModified, path, "", videoLength, "", 0, 0);
    }

    public MediaFileInfoEntity(@NonNull String obfuscatedName, String actualName, String mimeType, long lastModified, String path, String tagListString, String videoLength, String thumbnailMediaName, int sortType, int viewCount) {
        this.obfuscatedName = obfuscatedName;
        this.actualName = actualName;
        this.mimeType = mimeType;
        this.lastModified = lastModified;
        this.path = path;
        this.tagListString = tagListString;
        this.videoLength = videoLength;
        this.thumbnailMediaName = thumbnailMediaName;
        this.sortType = sortType;
        this.viewCount = viewCount;
    }
}
