package com.won983212.vaultapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.won983212.vaultapp.database.MediaFileDatabase;
import com.won983212.vaultapp.database.MediaFileInfoDAO;
import com.won983212.vaultapp.database.MediaFileInfoEntity;
import com.won983212.vaultapp.database.OnNotifyDataEvent;
import com.won983212.vaultapp.ui.view.MediaViewerOverlay;
import com.won983212.vaultapp.util.Logger;
import com.won983212.vaultapp.util.MediaItemSorts;
import com.won983212.vaultapp.util.Usefuls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class MediaDatabaseManager {
    private final HashMap<String, MediaItem> mediaItemCache = new HashMap<>();
    private final ArrayList<MediaItem> currentPathData = new ArrayList<>();
    private MediaFileDatabase db;
    private OnNotifyDataEvent dbUpdateEvent = null;
    private final Object currentPathDataLock = new Object();
    private final ArrayList<String> tagFilters = new ArrayList<>();
    private SharedPreferences preferences;

    /**
     * Path는 반드시 /으로 끝나야 함
     **/
    private String path = "/";

    public boolean hasInitialzed() {
        return db != null;
    }

    public void setupDatas(Context context) {
        db = MediaFileDatabase.getInstance(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final ProgressDialog dialog = Usefuls.createIndeterminateProgressDialog(context, R.string.dialog_message_loading_db);
        Usefuls.newTask(() -> {
            List<MediaFileInfoEntity> list = db.mediaFileInfoDao().getAllMediaInfo();
            synchronized (mediaItemCache) {
                mediaItemCache.clear();
                for (MediaFileInfoEntity ent : list) {
                    MediaItem item = new MediaItem(ent);
                    mediaItemCache.put(item.obfuscatedName, item);
                }
            }
            setPath("/");
            dialog.dismiss();
        });
    }

    public void showMedia(Context context, MediaItem item) {
        ArrayList<MediaItem> medias = new ArrayList<>();
        int index = 0;

        synchronized (currentPathDataLock) {
            for (MediaItem media : currentPathData) {
                if (item == media)
                    index = medias.size();
                if (!media.isDirectory())
                    medias.add(media);
            }
        }

        final MediaViewerOverlay overlay = new MediaViewerOverlay(context);
        overlay.update(medias.get(index));

        StfalconImageViewer<MediaItem> imageViewer = new StfalconImageViewer.Builder<>(context, medias, (imageView, media) ->
                Glide.with(context).load(media.getDocumentURI())
                        .placeholder(media.isImage() ? R.drawable.ic_photo : R.drawable.ic_movie)
                        .into(imageView))
                .withStartPosition(index)
                .withImageChangeListener(position -> {
                    MediaItem selected = medias.get(position);
                    overlay.update(selected);
                    if (selected.isImage()) {
                        increaseViewCountAsync(selected);
                    }
                })
                .withHiddenStatusBar(false)
                .withOverlayView(overlay).show();

        overlay.setImageViewer(imageViewer);
    }

    public void setDatabaseNotifyEvent(OnNotifyDataEvent e) {
        dbUpdateEvent = e;
    }

    public MediaItem get(int pos) {
        synchronized (currentPathDataLock) {
            return currentPathData.get(pos);
        }
    }

    public int indexOf(MediaItem item) {
        synchronized (currentPathDataLock) {
            return currentPathData.indexOf(item);
        }
    }

    public MediaItem getByHash(String obfuscatedName) {
        synchronized (mediaItemCache) {
            return mediaItemCache.get(obfuscatedName);
        }
    }

    public int size() {
        synchronized (currentPathDataLock) {
            return currentPathData.size();
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        setPath(path, null);
    }

    public void setPath(String path, Runnable callback) {
        this.path = path;
        Usefuls.newTask(() -> {
            List<MediaFileInfoEntity> list = db.mediaFileInfoDao().getAllFilesOnPath(path);
            synchronized (currentPathDataLock) {
                currentPathData.clear();
                for (MediaFileInfoEntity f : list) {
                    MediaItem item = mediaItemCache.get(f.obfuscatedName);
                    if (item != null) {
                        if (item.isDirectory() || tagFilters.isEmpty()) {
                            currentPathData.add(item);
                        } else {
                            int count = 0;
                            for (String tag : item.tags) {
                                if (tagFilters.contains(tag) && ++count == tagFilters.size()) break;
                            }
                            if (count == tagFilters.size())
                                currentPathData.add(item);
                        }
                    } else {
                        Logger.i("Found a file not registered in DB: " + f.obfuscatedName);
                    }
                }
            }
            sortAndNotify(callback);
        });
    }

    public void sortAndNotify(Runnable callback) {
        synchronized (currentPathDataLock) {
            if (path.equals("/")) {
                Collections.sort(currentPathData, MediaItemSorts.FILE_NAME_ASCENDING);
            } else {
                MediaItem item = getByHash(Usefuls.getLastFolderName(path));
                if (item.sortType >= 0 && item.sortType < MediaItemSorts.COMPARATOR_LABELS.length) {
                    Collections.sort(currentPathData, MediaItemSorts.COMPARATOR_OBJECTS.get(item.sortType));
                } else {
                    Collections.sort(currentPathData, MediaItemSorts.FILE_NAME_ASCENDING);
                }
            }

            VaultApp.post(() -> {
                if (dbUpdateEvent != null)
                    dbUpdateEvent.onNotify(currentPathData, callback);
            });
        }
    }

    public boolean explore(MediaItem folder) {
        if (folder.isDirectory()) {
            setPath(path + folder.obfuscatedName + "/");
            return true;
        }
        return false;
    }

    public boolean exploreBack(int count, Runnable callback) {
        String dest = path;

        if (count <= 0)
            return true;

        for (int i = 0; i < count; i++) {
            if (dest.equals("/")) return false;
            dest = dest.substring(0, dest.lastIndexOf('/', dest.length() - 2) + 1);
        }

        setPath(dest, callback);
        return true;
    }

    public void setTagString(String tagsString) {
        tagFilters.clear();
        if(tagsString != null) {
            for (String tag : tagsString.trim().split(" ")) {
                if (tag.length() > 0)
                    tagFilters.add(tag);
            }
        }
        setPath(path);
    }

    public void addTagString(String tag) {
        tagFilters.add(tag);
        setPath(path);
    }

    public boolean hasTagFilter(){
        return !tagFilters.isEmpty();
    }

    public String getTagString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String str : tagFilters) {
            sb.append(str);
            if (++i < tagFilters.size())
                sb.append(' ');
        }
        return sb.toString();
    }

    public Set<String> retrieveAllTags() {
        HashSet<String> tags = new HashSet<>();
        synchronized (currentPathDataLock) {
            for (MediaItem item : currentPathData) {
                tags.addAll(Arrays.asList(item.tags));
            }
        }
        return tags;
    }

    public boolean containsName(String obfuscatedName) {
        return mediaItemCache.containsKey(obfuscatedName);
    }

    public boolean changeThumbnail(String folderName, @Nullable String targetObfuscatedName) {
        MediaItem folder = mediaItemCache.get(folderName);
        if (folder != null && folder.isDirectory()) {
            folder.setFolderThumbnail(targetObfuscatedName);
            db.mediaFileInfoDao().updateFolderThumbnail(folderName, targetObfuscatedName == null ? "" : targetObfuscatedName);
            return true;
        } else {
            Logger.e("Not found or not directory: " + folder);
        }
        return false;
    }

    public boolean changeThumbnailToFirstMedia(String folderName) {
        MediaItem folder = mediaItemCache.get(folderName);
        String target;
        if (folder != null) {
            List<MediaFileInfoEntity> ents = db.mediaFileInfoDao().getMediasOnPath(folder.getFileURL() + "/");
            if (!ents.isEmpty()) {
                target = ents.get(0).obfuscatedName;
                return changeThumbnail(folderName, target);
            } else {
                return true;
            }
        }
        return false;
    }

    public void changePath(MediaItem item, String path) {
        item.setPath(path);
        db.mediaFileInfoDao().updatePath(item.obfuscatedName, path);
    }

    public void changeSortType(MediaItem item, int typeIndex) {
        item.sortType = typeIndex;
        db.mediaFileInfoDao().updateSortType(item.obfuscatedName, typeIndex);

        CountDownLatch latch = new CountDownLatch(1);
        sortAndNotify(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        VaultApp.post(() -> {
            if (dbUpdateEvent != null) {
                dbUpdateEvent.onDataSetUpdated();
            }
        });
    }

    public boolean changeTitle(int index, String title) {
        MediaItem item = get(index);
        if (index != -1) {
            item.title = title;
            db.mediaFileInfoDao().updateTitle(item.obfuscatedName, title);
            VaultApp.post(() -> {
                if (dbUpdateEvent != null)
                    dbUpdateEvent.onDataUpdated(index, 1);
            });
            return true;
        } else {
            return false;
        }
    }

    public void changeTags(MediaItem item, String[] tags) {
        item.tags = tags;
        StringBuilder sb = new StringBuilder();
        if (tags.length > 0) {
            for (String tag : tags) {
                sb.append(tag);
                sb.append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        db.mediaFileInfoDao().updateTags(item.obfuscatedName, sb.toString());
    }

    public void clearViewCount() {
        db.mediaFileInfoDao().clearViewCount();
    }

    public void increaseViewCountAsync(MediaItem item) {
        Usefuls.newTask(() -> db.mediaFileInfoDao().updateViewCount(item.obfuscatedName, ++item.viewCount));
    }

    public Transaction edit() {
        return new Transaction();
    }

    public class Transaction {
        private final ArrayList<MediaFileInfoEntity> putEntities = new ArrayList<>();
        private final ArrayList<Integer> deleteIndexes = new ArrayList<>();

        /**
         * @param path It must ends with '/'
         */
        public void put(String mimeType, String actualName, String obfuscatedName, String path, String videoLength, long lastModified) {
            putEntities.add(new MediaFileInfoEntity(obfuscatedName, actualName, mimeType, lastModified, path, videoLength));
        }

        public void delete(int index) {
            deleteIndexes.add(index);
        }

        /**
         * It must be called in sub thread.
         */
        public void commit(Context context) {
            if (db == null) {
                Logger.e("Database is not set yet.");
                return;
            }

            ArrayList<String> deletionList = null;
            ArrayList<MediaItem> folderDeletions = null;

            // sort deleteIndexes by descending order.
            Collections.sort(deleteIndexes, (o1, o2) -> o2.compareTo(o1));

            if (deleteIndexes.size() > 0) {
                synchronized (currentPathDataLock) {
                    deletionList = new ArrayList<>();
                    folderDeletions = new ArrayList<>();

                    for (Integer i : deleteIndexes) {
                        MediaItem item = currentPathData.get(i);
                        if (item != null) {
                            deletionList.add(item.obfuscatedName);
                            if (item.isDirectory()) {
                                folderDeletions.add(item);
                            }
                        } else {
                            Logger.e("Deletion item is null. It's a bug!");
                        }
                    }
                }
            }

            MediaFileInfoDAO dao = db.mediaFileInfoDao();

            // add to db
            if (putEntities.size() > 0)
                dao.insertMediaInfo(putEntities);

            // delete from db
            if (deletionList != null) {
                dao.deleteMediaInfo(deletionList);
                for (MediaItem item : folderDeletions) {
                    dao.deleteFolderContents(item.getFileURL());
                }
            }

            if (preferences.getBoolean("use_auto_backup", true) && !db.backupDatabaseFile(context)) {
                Usefuls.threadSafeToast(context, R.string.toast_error_cannot_backup_db, Toast.LENGTH_LONG);
            }

            // add / remove entries to static data collection
            synchronized (mediaItemCache) {
                if (deletionList != null) {
                    for (String key : deletionList) {
                        mediaItemCache.remove(key);
                    }

                    if (folderDeletions.size() > 0) {
                        Iterator<Map.Entry<String, MediaItem>> iter = mediaItemCache.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry<String, MediaItem> ent = iter.next();
                            for (int i = 0; i < folderDeletions.size(); i++) {
                                MediaItem item = folderDeletions.get(i);
                                if (ent.getValue().isChildOf(item)) {
                                    iter.remove();
                                    break;
                                }
                            }
                        }
                    }
                }

                for (MediaFileInfoEntity ent : putEntities) {
                    mediaItemCache.put(ent.obfuscatedName, new MediaItem(ent));
                }
            }

            // add / remove entries to current view
            synchronized (currentPathDataLock) {
                if (deletionList != null) {
                    for (int i : deleteIndexes) {
                        currentPathData.remove(i);
                    }
                }
                for (MediaFileInfoEntity ent : putEntities) {
                    if (ent.path.equals(path)) {
                        currentPathData.add(mediaItemCache.get(ent.obfuscatedName));
                    }
                }
            }

            // notify changes
            VaultApp.post(() -> {
                if (dbUpdateEvent != null) {
                    dbUpdateEvent.onNotify(currentPathData, null);
                }
            });
        }
    }
}
