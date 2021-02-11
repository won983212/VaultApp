package com.won983212.vaultapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import com.won983212.vaultapp.util.Logger;
import com.won983212.vaultapp.util.Usefuls;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

@SuppressWarnings("UnusedReturnValue")
public class MediaFileActionExecutor {
    public static final String EMPTY_MIME_TYPE = "application/enc";
    private static final String HASH_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    private static final int HASH_SIZE = 32;
    private static final Random RANDOM = new Random();

    private final Context context;
    private final MediaDatabaseManager dataManager;
    private final ContentResolver resolver;
    private final ContentFileHandler fileHandler;
    private ArrayList<ModifyEntry> modifies = null;
    private HashSet<String> obfuscatedNames = null;
    private OnObfuscateProgressUpdated listener = null;
    private int processed = 0;
    private int prevProgress = -1;

    public MediaFileActionExecutor(Context context) {
        this.context = context;
        this.dataManager = VaultApp.getInstance().getDataManager();
        this.resolver = context.getContentResolver();
        this.fileHandler = ContentFileHandler.getInstance();
    }

    public void setProgressListener(OnObfuscateProgressUpdated listener) {
        this.listener = listener;
    }

    private void onProgressThreadSafe(double value) {
        final int progress = (int) ((processed + value) * 100 / modifies.size());
        if (progress != prevProgress) {
            VaultApp.post(() -> {
                if (listener != null)
                    listener.onProgress(progress);
            });
            prevProgress = progress;
        }
    }

    public MediaFileActionExecutor begin() {
        processed = 0;
        modifies = new ArrayList<>();
        obfuscatedNames = new HashSet<>();
        return this;
    }

    public MediaFileActionExecutor obfuscate(Uri uri, String destPath) {
        String obfuscatedName = generateHash();
        modifies.add(new ModifyEntry(ModifyEntry.TYPE_ADD_FILE, uri, obfuscatedName, destPath));
        obfuscatedNames.add(obfuscatedName);
        return this;
    }

    public MediaFileActionExecutor obfuscateFolder(DocumentFile sourceFolder) {
        iterateFolder(sourceFolder, dataManager.getPath(), new MediaFileActionExecutor.IFolderIterateCallback() {
            @Override
            public void iterateFile(DocumentFile file, String targetPath) {
                obfuscate(file.getUri(), targetPath);
            }

            @Override
            public String iterateFolder(DocumentFile file, String path) {
                return createDirectoryWithNameCallback(file.getName(), path);
            }
        });
        return this;
    }

    public MediaFileActionExecutor deobfuscate(int idx, DocumentFile destFolder) {
        MediaItem item = dataManager.get(idx);
        if (item != null) {
            modifies.add(new ModifyEntry(ModifyEntry.TYPE_EJECT_FILE, item.obfuscatedName, item.getDocumentURI(), destFolder));
            modifies.add(new ModifyEntry(ModifyEntry.TYPE_DELETE_FILE, idx));
        }
        return this;
    }

    public MediaFileActionExecutor deobfuscateFolder(int idx, DocumentFile destFolder) {
        MediaItem item = dataManager.get(idx);
        if (item != null) {
            iterateFolder(item.getDocumentFile(context), "/", new MediaFileActionExecutor.IFolderIterateCallback() {
                @Override
                public void iterateFile(DocumentFile file, String targetPath) {
                    DocumentFile targetFolder = DocumentFile.fromTreeUri(context, Usefuls.getVirtualFileURI(destFolder.getUri(), targetPath));
                    modifies.add(new ModifyEntry(ModifyEntry.TYPE_EJECT_FILE, file.getName(), file.getUri(), targetFolder));
                }

                @Override
                public String iterateFolder(DocumentFile file, String path) {
                    MediaItem item = dataManager.getByHash(file.getName());
                    DocumentFile targetFolder = DocumentFile.fromTreeUri(context, Usefuls.getVirtualFileURI(destFolder.getUri(), path));
                    modifies.add(new ModifyEntry(ModifyEntry.TYPE_EJECT_FILE, file.getName(), file.getUri(), targetFolder));
                    return item.title;
                }
            });
            modifies.add(new ModifyEntry(ModifyEntry.TYPE_DELETE_FILE, idx));
        }
        return this;
    }

    public MediaFileActionExecutor delete(int index) {
        modifies.add(new ModifyEntry(ModifyEntry.TYPE_DELETE_FILE, index));
        return this;
    }

    public MediaFileActionExecutor createDirectory(String name, String destPath) {
        createDirectoryImpl(name, destPath);
        return this;
    }

    public String createDirectoryWithNameCallback(String name, String destPath) {
        return createDirectoryImpl(name, destPath);
    }

    public MediaFileActionExecutor move(String obfuscatedName, String destPath) {
        MediaItem item = dataManager.getByHash(obfuscatedName);
        if (item != null) {
            modifies.add(new ModifyEntry(ModifyEntry.TYPE_MOVE_FILE, item, destPath));
            modifies.add(new ModifyEntry(ModifyEntry.TYPE_DELETE_FILE_ONLY, item.getDocumentFile(context)));
        }
        return this;
    }

    public MediaFileActionExecutor moveFolder(DocumentFile folder) {
        iterateFolder(folder, dataManager.getPath(), new MediaFileActionExecutor.IFolderIterateCallback() {
            @Override
            public void iterateFile(DocumentFile file, String targetPath) {
                modifies.add(new ModifyEntry(ModifyEntry.TYPE_MOVE_FILE, dataManager.getByHash(file.getName()), targetPath));
            }

            @Override
            public String iterateFolder(DocumentFile file, String path) {
                modifies.add(new ModifyEntry(ModifyEntry.TYPE_MOVE_FILE, dataManager.getByHash(file.getName()), path));
                return file.getName();
            }
        });
        modifies.add(new ModifyEntry(ModifyEntry.TYPE_DELETE_FILE_ONLY, folder));
        return this;
    }

    private String createDirectoryImpl(String name, String destPath) {
        String obfuscatedName = generateHash();
        modifies.add(new ModifyEntry(ModifyEntry.TYPE_CREATE_FOLDER, name, obfuscatedName, destPath));
        obfuscatedNames.add(obfuscatedName);
        return obfuscatedName;
    }

    public boolean commit() {
        MediaDatabaseManager.Transaction transaction = dataManager.edit();
        onProgressThreadSafe(0);

        boolean success = true;
        for (ModifyEntry ent : modifies) {
            switch (ent.type) {
                case ModifyEntry.TYPE_ADD_FILE:
                    success = processObfuscate(transaction, (Uri) ent.data[0], (String) ent.data[1], (String) ent.data[2]);
                    break;
                case ModifyEntry.TYPE_CREATE_FOLDER:
                    success = processCreateDirectory(transaction, (String) ent.data[0], (String) ent.data[1], (String) ent.data[2]);
                    break;
                case ModifyEntry.TYPE_DELETE_FILE:
                    processDelete(transaction, (int) ent.data[0]);
                    break;
                case ModifyEntry.TYPE_MOVE_FILE:
                    success = processMove((MediaItem) ent.data[0], (String) ent.data[1]);
                    break;
                case ModifyEntry.TYPE_EJECT_FILE:
                    success = processEject((String) ent.data[0], (Uri) ent.data[1], (DocumentFile) ent.data[2]);
                    break;
                case ModifyEntry.TYPE_DELETE_FILE_ONLY:
                    success = ((DocumentFile) ent.data[0]).delete();
                    break;
            }
            if (!success) {
                break;
            }
            processed++;
        }

        transaction.commit(context);
        VaultApp.post(() -> {
            if (listener != null)
                listener.onProgress(1);
        });

        modifies = null;
        return success;
    }

    private boolean processEject(String fileName, Uri sourceURI, DocumentFile destFolder) {
        MediaItem item = dataManager.getByHash(fileName);

        onProgressThreadSafe(0);
        if (destFolder != null) {
            DocumentFile file;
            if (item.isDirectory()) {
                file = destFolder.createDirectory(item.title);
            } else {
                file = destFolder.createFile(item.type, item.title);
            }
            if (file != null) {
                if (!item.isDirectory() && !copy(sourceURI, file.getUri())) {
                    return false;
                }
                onProgressThreadSafe(1);
                return true;
            }
        } else {
            Logger.e("Failed to find destFolder");
        }
        return false;
    }

    private boolean processMove(MediaItem item, String destPath) {
        DocumentFile actualFile = item.getDocumentFile(context);
        DocumentFile destFolder = DocumentFile.fromTreeUri(context, Usefuls.getVaultFolderFileURI(destPath));

        onProgressThreadSafe(0);
        if (actualFile != null && destFolder != null) {
            DocumentFile file;
            if (item.isDirectory()) {
                file = destFolder.createDirectory(item.obfuscatedName);
            } else {
                file = destFolder.createFile(EMPTY_MIME_TYPE, item.obfuscatedName);
            }

            if (file != null) {
                if (!item.isDirectory() && !copy(actualFile.getUri(), file.getUri())) {
                    return false;
                }

                dataManager.changePath(item, destPath);
                onProgressThreadSafe(1);
                return true;
            } else {
                Logger.e(String.format("Failed to create file or directory (actualFile: %s, destFolder: %s)", actualFile, destFolder));
            }
        } else {
            Logger.e(String.format("Failed to find actualFile or destFolder (actualFile: %s, destFolder: %s)", actualFile, destFolder));
        }

        return false;
    }

    private void processDelete(MediaDatabaseManager.Transaction transaction, int index) {
        MediaItem item = dataManager.get(index);
        DocumentFile file = item.getDocumentFile(context);

        onProgressThreadSafe(0);
        if (file != null) {
            // delete actual file
            file.delete();
            onProgressThreadSafe(0.8);
        }

        // delete from datamanager(DB)
        transaction.delete(index);
        onProgressThreadSafe(1);
    }

    private boolean processObfuscate(MediaDatabaseManager.Transaction transaction, Uri uri, String fileName, String destPath) {
        String contentType = resolver.getType(uri);
        DocumentFile parentFolder = fileHandler.getFile(context, destPath);

        onProgressThreadSafe(0);
        if (parentFolder != null) {
            DocumentFile file = parentFolder.createFile(EMPTY_MIME_TYPE, fileName);
            if (file != null) {
                if (!copy(uri, file.getUri())) {
                    return false;
                }

                Cursor c = resolver.query(uri, null, null, null, null);
                c.moveToFirst();
                String actualName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                c.close();

                long lastModified = -1;
                DocumentFile targetFile = DocumentFile.fromSingleUri(context, uri);
                if (targetFile != null) {
                    lastModified = targetFile.lastModified();
                }

                if (contentType.startsWith("image")) {
                    transaction.put(contentType, actualName, fileName, destPath, "", lastModified);
                } else if (contentType.startsWith("video")) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(context, file.getUri());
                    String duration = formatDuration(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    transaction.put(contentType, actualName, fileName, destPath, duration, lastModified);
                }
                onProgressThreadSafe(1);
                return true;
            } else {
                Logger.e("Can't create new file: " + fileName + " in " + destPath);
            }
        } else {
            Logger.e("Found incorrect path for moving media: " + destPath);
        }

        return false;
    }

    public boolean processCreateDirectory(MediaDatabaseManager.Transaction transaction, String name, String obfuscated, String destPath) {
        DocumentFile parentPath = fileHandler.getFile(context, destPath);

        onProgressThreadSafe(0);
        if (parentPath != null) {
            DocumentFile file = parentPath.createDirectory(obfuscated);
            if (file != null) {
                transaction.put(MediaItem.DIRECTORY_MIME, name, obfuscated, destPath, "", file.lastModified());
                onProgressThreadSafe(1);
                return true;
            }
        } else {
            Logger.e("Found incorrect path for creating new folder: " + destPath);
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean copy(Uri src, Uri dst) {
        FileInputStream iStream = null;
        FileOutputStream oStream = null;

        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            iStream = new FileInputStream(resolver.openFileDescriptor(src, "r").getFileDescriptor());
            oStream = new FileOutputStream(resolver.openFileDescriptor(dst, "w").getFileDescriptor());

            inChannel = iStream.getChannel();
            outChannel = oStream.getChannel();

            long size = inChannel.size();
            final long blockSize = 64 * 1024 * 1024;
            if (size <= blockSize) {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } else {
                long position = 0;
                while (outChannel.transferFrom(inChannel, position, blockSize) > 0) {
                    position += blockSize;
                    onProgressThreadSafe((double) position / size);
                }
            }

            onProgressThreadSafe(1);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (iStream != null)
                    iStream.close();
                if (oStream != null)
                    oStream.close();
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void iterateFolder(DocumentFile sourceFolder, String destPath, IFolderIterateCallback callback) {
        Stack<String> pathStack = new Stack<>();
        Stack<DocumentFile> folderStack = new Stack<>();

        String rootDirName = callback.iterateFolder(sourceFolder, destPath);
        pathStack.push(destPath + rootDirName + "/");
        folderStack.push(sourceFolder);

        while (!folderStack.empty()) {
            String path = pathStack.pop();
            DocumentFile dir = folderStack.pop();
            for (DocumentFile file : dir.listFiles()) {
                if (file.isDirectory()) {
                    String dirName = callback.iterateFolder(file, path);
                    pathStack.push(path + dirName + "/");
                    folderStack.push(file);
                } else {
                    callback.iterateFile(file, path);
                }
            }
        }
    }

    private static String formatDoubleZero(long i) {
        return i < 10 ? ("0" + i) : Long.toString(i);
    }

    private static String formatDuration(String durationStr) {
        long duration = Long.parseLong(durationStr);
        duration /= 1000;

        int h = -1;
        if (duration >= 3600) {
            h = (int) (duration / 3600);
            duration %= 3600;
        }

        int m = (int) (duration / 60);
        duration %= 60;

        if (h != -1) {
            return h + ":" + formatDoubleZero(m) + ":" + formatDoubleZero(duration);
        } else {
            return m + ":" + formatDoubleZero(duration);
        }
    }

    private String generateHash() {
        String name;
        do {
            StringBuilder sb = new StringBuilder(MediaFileActionExecutor.HASH_SIZE);
            for (int i = 0; i < MediaFileActionExecutor.HASH_SIZE; i++)
                sb.append(HASH_CHARACTERS.charAt(RANDOM.nextInt(HASH_CHARACTERS.length())));
            name = sb.toString();
        } while (dataManager.containsName(name) && !obfuscatedNames.contains(name));
        return name;
    }

    private static class ModifyEntry {
        public static final int TYPE_CREATE_FOLDER = 101;
        public static final int TYPE_ADD_FILE = 102;
        public static final int TYPE_DELETE_FILE = 103;
        public static final int TYPE_MOVE_FILE = 104;
        public static final int TYPE_EJECT_FILE = 105;

        public static final int TYPE_DELETE_FILE_ONLY = 106;

        public final int type;
        public final Object[] data;

        public ModifyEntry(int type, Object... data) {
            this.type = type;
            this.data = data;
        }
    }

    public interface OnObfuscateProgressUpdated {
        void onProgress(int progress);
    }

    public interface IFolderIterateCallback {
        void iterateFile(DocumentFile file, String targetPath);

        String iterateFolder(DocumentFile file, String path);
    }
}
