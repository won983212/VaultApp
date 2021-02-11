package com.won983212.vaultapp.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.won983212.vaultapp.MediaItem;

import java.util.List;

@Dao
public interface MediaFileInfoDAO {
    @Query("SELECT * FROM MediaFileInfoEntity")
    List<MediaFileInfoEntity> getAllMediaInfo();

    @Query("SELECT * FROM MediaFileInfoEntity WHERE path = :path")
    List<MediaFileInfoEntity> getAllFilesOnPath(String path);

    @Query("SELECT * FROM MediaFileInfoEntity WHERE path = :path and mime_type <> '" + MediaItem.DIRECTORY_MIME + "'")
    List<MediaFileInfoEntity> getMediasOnPath(String path);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMediaInfo(List<MediaFileInfoEntity> ents);

    @Query("DELETE FROM MediaFileInfoEntity WHERE obfuscated_name IN (:idList)")
    void deleteMediaInfo(List<String> idList);

    @Query("DELETE FROM MediaFileInfoEntity WHERE path LIKE :folderName || '%'")
    void deleteFolderContents(String folderName);

    @Query("UPDATE MediaFileInfoEntity SET thumbnail_ptr = :targetObfuscatedName WHERE obfuscated_name = :folder")
    void updateFolderThumbnail(String folder, String targetObfuscatedName);

    @Query("UPDATE mediafileinfoentity SET path = :path WHERE obfuscated_name = :obfuscatedName")
    void updatePath(String obfuscatedName, String path);

    @Query("UPDATE mediafileinfoentity SET actual_name = :title WHERE obfuscated_name = :obfuscatedName")
    void updateTitle(String obfuscatedName, String title);

    @Query("UPDATE mediafileinfoentity SET sort_type = :sortType WHERE obfuscated_name = :obfuscatedName")
    void updateSortType(String obfuscatedName, int sortType);

    @Query("UPDATE mediafileinfoentity SET view_count = :viewCount WHERE obfuscated_name = :obfuscatedName")
    void updateViewCount(String obfuscatedName, int viewCount);

    @Query("UPDATE mediafileinfoentity SET tags = :tags WHERE obfuscated_name = :obfuscatedName")
    void updateTags(String obfuscatedName, String tags);

    @Query("UPDATE mediafileinfoentity SET view_count = 0")
    void clearViewCount();
}
