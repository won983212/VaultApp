package com.won983212.vaultapp.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;
import com.won983212.vaultapp.ContentFileHandler;
import com.won983212.vaultapp.MediaDatabaseManager;
import com.won983212.vaultapp.MediaFileActionExecutor;
import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.MultipleSelection;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.ui.ItemEventCallback;
import com.won983212.vaultapp.ui.MediaListAdapter;
import com.won983212.vaultapp.ui.MediaListViewHolder;
import com.won983212.vaultapp.ui.dialog.MediaDetailDialog;
import com.won983212.vaultapp.ui.dialog.TagPresetDialog;
import com.won983212.vaultapp.util.MediaItemSorts;
import com.won983212.vaultapp.util.Usefuls;

import java.util.ArrayList;
import java.util.Stack;

import moe.feng.common.view.breadcrumbs.BreadcrumbsCallback;
import moe.feng.common.view.breadcrumbs.BreadcrumbsView;
import moe.feng.common.view.breadcrumbs.model.BreadcrumbItem;

public class MediaListActivity extends AppCompatActivity implements ItemEventCallback, AutoPermissionsListener, ActionMode.Callback {
    private static final int REQ_PICK_FILE = 101;
    private static final int REQ_PICK_FOLDER = 102;
    private static final int REQ_SET_ROOT_FILE_PATH = 103;
    private static final int REQ_PICK_EJECT_DEST = 104;
    private static SharedPreferences privatePreferences;

    private BreadcrumbsView breadcrumbs;
    private ChipGroup tagFilters;
    private CoordinatorLayout rootLayout;

    private GridLayoutManager layoutManager;
    private MediaListAdapter adapter;
    private MediaDatabaseManager dataManager;

    private final Stack<Integer> scrollStack = new Stack<>();
    private Snackbar fileMoveSnackbar = null;
    private ActionMode actionMode = null;
    private TextView actionBarSelectionTextView = null;

    private static final ContentFileHandler ROOT_FILE_MANAGER = ContentFileHandler.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        rootLayout = findViewById(R.id.root_layout);

        findViewById(R.id.add_new_file).setOnClickListener((v) -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, ""), REQ_PICK_FILE);
        });

        findViewById(R.id.add_new_folder).setOnClickListener((v) -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK_FOLDER);
        });

        dataManager = VaultApp.getInstance().getDataManager();
        privatePreferences = getSharedPreferences("privatePrefs", MODE_PRIVATE);

        tagFilters = findViewById(R.id.tagFilterGroup);
        breadcrumbs = findViewById(R.id.breadcrumbsView);
        breadcrumbs.addItem(BreadcrumbItem.createSimpleItem("Root"));
        breadcrumbs.setCallback(new BreadcrumbsCallback<BreadcrumbItem>() {
            @Override
            public void onItemClick(@NonNull BreadcrumbsView view, int position) {
                if (position != view.getItems().size() - 1) {
                    int prevLen = view.getItems().size();
                    view.removeItemAfter(position + 1);

                    final int count = prevLen - view.getItems().size();
                    dataManager.exploreBack(count, () -> {
                        int scroll = RecyclerView.NO_POSITION;
                        for (int i = 0; i < count; i++) {
                            scroll = scrollStack.pop();
                        }
                        if (scroll != RecyclerView.NO_POSITION) {
                            layoutManager.scrollToPosition(scroll);
                        }
                    });
                }
            }

            @Override
            public void onItemChange(@NonNull BreadcrumbsView breadcrumbsView, int i, @NonNull BreadcrumbItem breadcrumbItem) {
            }
        });

        if (dataManager.hasInitialzed()) {
            dataManager.setTagString(null);
        } else if (ROOT_FILE_MANAGER.requestSetRootPath(this, REQ_SET_ROOT_FILE_PATH)) {
            dataManager.setupDatas(this);
        }

        int orientation = getResources().getConfiguration().orientation;

        adapter = new MediaListAdapter(dataManager);
        adapter.setItemEventListener(this);
        layoutManager = new GridLayoutManager(this, orientation == Configuration.ORIENTATION_LANDSCAPE ? 8 : 4);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        AutoPermissions.Companion.loadAllPermissions(this, 101);

        if (savedInstanceState != null) {
            int pathCnt = savedInstanceState.getInt("pathCount");
            for (int i = 0; i < pathCnt; i++) {
                scrollStack.push(0);
            }
            String path = savedInstanceState.getString("path");
            if (path != null) {
                dataManager.setPath(path);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("path", dataManager.getPath());
        outState.putInt("pathCount", scrollStack.size());
    }

    @Override
    public void onBackPressed() {
        if (dataManager.hasTagFilter()) {
            clearTagList();
        } else if (!dataManager.exploreBack(1, () -> layoutManager.scrollToPosition(scrollStack.pop()))) {
            super.onBackPressed();
        } else {
            breadcrumbs.removeLastItem();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean visible = !dataManager.getPath().equals("/");
        menu.findItem(R.id.menu_clear_thumbnail).setVisible(visible);
        menu.findItem(R.id.menu_sorting).setVisible(visible);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_create_folder) {
            Usefuls.createInputDialog(this, R.string.dialog_title_create_folder, R.string.dialog_message_create_folder, null, (name) -> {
                if (name.length() == 0) {
                    Toast.makeText(MediaListActivity.this, R.string.toast_error_folder_name_empty, Toast.LENGTH_LONG).show();
                    return;
                }

                MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(this);
                Usefuls.newTask(() -> {
                    if (fileExecutor.begin().createDirectory(name, dataManager.getPath()).commit()) {
                        showInfoToast(R.string.toast_info_folder_created);
                    } else {
                        showErrorToast(R.string.toast_error_cannot_create_folder);
                    }
                });
            });
            return true;
        } else if (id == R.id.menu_clear_thumbnail) {
            final String folderName = Usefuls.getLastFolderName(dataManager.getPath());
            Usefuls.newTask(() -> {
                if (dataManager.changeThumbnail(folderName, null)) {
                    showInfoToast(R.string.toast_info_cleared_thumbnail);
                } else {
                    showErrorToast(R.string.toast_error_cannot_clear_thumbnail);
                }
            });
            return true;
        } else if (id == R.id.menu_sorting) {
            MediaItem folder = dataManager.getByHash(Usefuls.getLastFolderName(dataManager.getPath()));
            int selected = folder.sortType >= 0 && folder.sortType < MediaItemSorts.COMPARATOR_LABELS.length ? folder.sortType : 0;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_title_select_sort_type);
            builder.setSingleChoiceItems(MediaItemSorts.COMPARATOR_LABELS, selected, null);
            builder.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> {
                ListView lw = ((AlertDialog) dialog).getListView();
                Usefuls.newTask(() -> {
                    dataManager.changeSortType(folder, lw.getCheckedItemPosition());
                    VaultApp.post(() -> layoutManager.scrollToPositionWithOffset(0, 0));
                });
            });
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.show();
            return true;
        } else if (id == R.id.menu_tag_filter) {
            Usefuls.createInputDialog(this, R.string.dialog_title_tag_filter, R.string.dialog_message_tag_filter, dataManager.getTagString(), input -> {
                dataManager.setTagString(input);
                updateTagList(input);
            });
            return true;
        } else if (id == R.id.menu_refresh) {
            dataManager.setPath(dataManager.getPath());
            return true;
        } else if (id == R.id.menu_show_all_tags) {
            TagPresetDialog dialog = new TagPresetDialog(this);
            dialog.setTagSelectedListener((tags) -> {
                if (tags.length() > 0) {
                    tagFilters.setVisibility(View.VISIBLE);
                    dataManager.setTagString(tags);
                    for (String tag : tags.split(" "))
                        addTagImpl(tag);
                }
            });
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, R.string.toast_error_cannot_retrieve_data, Toast.LENGTH_LONG).show();
                return;
            }

            if (requestCode == REQ_PICK_FILE || requestCode == REQ_PICK_FOLDER) {
                final ProgressDialog dialog = Usefuls.createProgressDialog(this, R.string.dialog_message_copying_file);
                if (requestCode == REQ_PICK_FILE) {
                    MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(this);
                    fileExecutor.setProgressListener(dialog::setProgress);

                    ClipData clipData = data.getClipData();
                    Uri rawData = data.getData();
                    if (clipData != null) {
                        int count = clipData.getItemCount();
                        String path = dataManager.getPath();
                        Usefuls.newTask(() -> {
                            fileExecutor.begin();
                            for (int i = 0; i < count; i++) {
                                fileExecutor.obfuscate(clipData.getItemAt(i).getUri(), path);
                            }
                            if (fileExecutor.commit()) {
                                showInfoToast(R.string.toast_info_added_file);
                            } else {
                                showErrorToast(R.string.toast_error_cannot_add_file);
                            }
                            VaultApp.post(dialog::dismiss);
                        });
                    } else if (rawData != null) {
                        String contentType = getContentResolver().getType(rawData);
                        if (!contentType.startsWith("image") && !contentType.startsWith("video")) {
                            Toast.makeText(this, R.string.toast_error_not_supported_format, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Usefuls.newTask(() -> {
                            if (fileExecutor.begin().obfuscate(rawData, dataManager.getPath()).commit()) {
                                showInfoToast(R.string.toast_info_added_file);
                            } else {
                                showErrorToast(R.string.toast_error_cannot_add_file);
                            }
                            VaultApp.post(dialog::dismiss);
                        });
                    }
                } else {
                    Usefuls.newTask(() -> {
                        MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(this);
                        DocumentFile srcFolder = DocumentFile.fromTreeUri(this, data.getData());

                        fileExecutor.setProgressListener(dialog::setProgress);
                        fileExecutor.begin();
                        fileExecutor.obfuscateFolder(srcFolder);

                        if (fileExecutor.commit()) {
                            showInfoToast(R.string.toast_info_added_folder);
                        } else {
                            showErrorToast(R.string.toast_error_cannot_add_folder);
                        }

                        VaultApp.post(dialog::dismiss);
                    });
                }
            } else if (requestCode == REQ_SET_ROOT_FILE_PATH) {
                if (ROOT_FILE_MANAGER.parseActivityResult(this, data)) {
                    dataManager.setupDatas(this);
                }
            } else if (requestCode == REQ_PICK_EJECT_DEST) {
                SparseBooleanArray selection = adapter.getSelection().getCheckedPositions();
                final int count = selection.size();

                DocumentFile destFolder = DocumentFile.fromTreeUri(this, data.getData());
                ProgressDialog progressDialog = Usefuls.createProgressDialog(this, R.string.dialog_message_ejecting_file);

                MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(MediaListActivity.this);
                fileExecutor.setProgressListener(progressDialog::setProgress);

                fileExecutor.begin();
                for (int i = 0; i < count; i++) {
                    MediaItem item = dataManager.get(i);
                    int idx = selection.keyAt(i);
                    if (item.isDirectory()) {
                        fileExecutor.deobfuscateFolder(idx, destFolder);
                    } else {
                        fileExecutor.deobfuscate(idx, destFolder);
                    }
                }

                adapter.clearSelection();

                Usefuls.newTask(() -> {
                    if (fileExecutor.commit()) {
                        showInfoToast(getString(R.string.toast_info_ejected_file, count));
                    } else {
                        showErrorToast(R.string.toast_error_cannot_eject);
                    }
                    VaultApp.post(progressDialog::dismiss);
                });
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQ_SET_ROOT_FILE_PATH) {
                finishAffinity();
            } else {
                Toast.makeText(this, R.string.toast_info_cancelled_select, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClickItem(MediaListViewHolder holder, int position) {
        if (position < dataManager.size() && position >= 0) {
            MediaItem item = dataManager.get(position);
            if (!adapter.getSelection().isMultiSelectionEnabled()) {
                if (item.isDirectory()) {
                    if (dataManager.explore(item)) {
                        breadcrumbs.addItem(BreadcrumbItem.createSimpleItem(item.title));
                        scrollStack.push(layoutManager.findLastCompletelyVisibleItemPosition());
                    } else {
                        Toast.makeText(this, R.string.toast_error_failed_explore, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    dataManager.showMedia(this, item);
                }
                if (!item.isVideo()) {
                    dataManager.increaseViewCountAsync(item);
                }
            }
        }
    }

    @Override
    public void onItemSelectionChanged(MediaListViewHolder holder, int position) {
        MultipleSelection selection = adapter.getSelection();
        if (selection.isMultiSelectionEnabled()) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(this);

                @SuppressLint("InflateParams")
                View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.actionbar_selection, null);
                view.findViewById(R.id.selectAllCheckbox).setOnClickListener((v) -> {
                    if (((CheckBox) v).isChecked()) {
                        adapter.selectAll();
                    } else {
                        actionMode.finish();
                    }
                });

                actionBarSelectionTextView = view.findViewById(R.id.selectionTextView);
                actionMode.setCustomView(view);
            }
            if (actionMode != null) {
                int count = selection.getCheckedCount();
                SparseBooleanArray selected = selection.getCheckedPositions();

                actionBarSelectionTextView.setText(getString(R.string.actionbar_n_selected, count));

                View view = actionMode.getCustomView();
                if (view != null) {
                    CheckBox checkBox = view.findViewById(R.id.selectAllCheckbox);
                    checkBox.setChecked(dataManager.size() == count);
                }

                boolean vThumbnailMenu = !dataManager.getPath().equals("/") && count == 1;
                vThumbnailMenu = vThumbnailMenu && !dataManager.get(selected.keyAt(0)).isDirectory();

                boolean vThumbnailAutoMenu = true;
                for (int i = 0; i < selected.size(); i++) {
                    MediaItem item = dataManager.get(selected.keyAt(i));
                    if (!item.isDirectory()) {
                        vThumbnailAutoMenu = false;
                        break;
                    }
                }

                Menu menu = actionMode.getMenu();
                menu.findItem(R.id.set_thumbnail_auto_menu).setVisible(vThumbnailAutoMenu);
                menu.findItem(R.id.set_thumbnail_menu).setVisible(vThumbnailMenu);
                menu.findItem(R.id.rename_menu).setVisible(count == 1);
                menu.findItem(R.id.detail_menu).setVisible(count == 1);
            }
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @Override
    public void onItemCleared() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int i, String[] strings) {
        if (strings.length > 0) {
            showErrorToast(R.string.toast_error_cannot_get_permission);
        }
    }

    @Override
    public void onGranted(int i, @NonNull String[] strings) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int itemId = item.getItemId();

        SparseBooleanArray selection = adapter.getSelection().getCheckedPositions();
        final int count = selection.size();

        if (itemId == R.id.delete_menu) {
            DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        ProgressDialog progressDialog = Usefuls.createProgressDialog(this, R.string.dialog_message_deleting_file);

                        MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(MediaListActivity.this);
                        fileExecutor.setProgressListener(progressDialog::setProgress);

                        fileExecutor.begin();
                        for (int i = 0; i < count; i++) {
                            fileExecutor.delete(selection.keyAt(i));
                        }

                        adapter.clearSelection();

                        Usefuls.newTask(() -> {
                            if (fileExecutor.commit()) {
                                showInfoToast(getString(R.string.toast_info_deleted_files, count));
                            } else {
                                showErrorToast(R.string.toast_error_cannot_delete_file);
                            }
                            VaultApp.post(progressDialog::dismiss);
                        });
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.dialog_message_want_to_delete_files, count))
                    .setPositiveButton(R.string.dialog_button_yes, onClickListener)
                    .setNegativeButton(R.string.dialog_button_no, onClickListener).show();
            return true;
        } else if (itemId == R.id.set_thumbnail_menu) {
            final MediaItem selected = dataManager.get(selection.keyAt(0));
            if (selected != null) {
                final String folderName = selected.getParentFolderName();
                if (folderName != null) {
                    Usefuls.newTask(() -> {
                        if (dataManager.changeThumbnail(folderName, selected.obfuscatedName)) {
                            showInfoToast(R.string.toast_info_changed_thumbnail);
                        } else {
                            showErrorToast(R.string.toast_error_cannot_change_thumbnail);
                        }
                    });
                }
            }
            adapter.clearSelection();
            return true;
        } else if (itemId == R.id.set_thumbnail_auto_menu) {
            final SparseBooleanArray cpy_selection = selection.clone();
            Usefuls.newTask(() -> {
                for (int i = 0; i < cpy_selection.size(); i++) {
                    int idx = cpy_selection.keyAt(i);
                    MediaItem ent = dataManager.get(idx);
                    if (!dataManager.changeThumbnailToFirstMedia(ent.obfuscatedName)) {
                        showErrorToast(getString(R.string.toast_error_folder_is_not_in_db, ent.title));
                        return;
                    }
                    VaultApp.post(() -> adapter.onDataUpdated(idx, 1));
                }
                showInfoToast(getString(R.string.toast_info_set_thumbnail_auto, cpy_selection.size()));
            });
            adapter.clearSelection();
            return true;
        } else if (itemId == R.id.move_menu) {
            if (fileMoveSnackbar == null && actionMode != null) {
                final ArrayList<MediaItem> files = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    files.add(dataManager.get(selection.keyAt(i)));
                }

                String fileName = count == 1 ? dataManager.get(selection.keyAt(0)).title : getString(R.string.move_n_selected, count);
                fileMoveSnackbar = Snackbar.make(rootLayout, fileName, Snackbar.LENGTH_INDEFINITE);

                final String originalPath = dataManager.getPath();
                fileMoveSnackbar.setAction(R.string.move_select_here, (v) -> {
                    String path = dataManager.getPath();

                    if (originalPath.equals(path)) {
                        showErrorToast(R.string.toast_error_cannot_move_to_original);
                        return;
                    }

                    for (MediaItem file : files) {
                        if (file.isDirectory()) {
                            if (path.contains(file.obfuscatedName)) {
                                showErrorToast(getString(R.string.toast_error_cannot_move_to_in_same, file.title));
                                return;
                            }
                        }
                    }

                    ProgressDialog progressDialog = Usefuls.createProgressDialog(this, R.string.dialog_message_moving_file);
                    MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(this);

                    fileExecutor.setProgressListener(progressDialog::setProgress);
                    fileExecutor.begin();

                    for (MediaItem file : files) {
                        if (file.isDirectory()) {
                            fileExecutor.moveFolder(file.getDocumentFile(this));
                        } else {
                            fileExecutor.move(file.obfuscatedName, path);
                        }
                    }

                    Usefuls.newTask(() -> {
                        if (fileExecutor.commit()) {
                            showInfoToast(getString(R.string.toast_info_file_moved, count));
                            dataManager.setPath(dataManager.getPath());
                        } else {
                            showErrorToast(R.string.toast_error_cannot_move_file);
                        }
                        VaultApp.post(progressDialog::dismiss);
                    });

                    fileMoveSnackbar.dismiss();
                });

                fileMoveSnackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        fileMoveSnackbar = null;
                    }
                });

                fileMoveSnackbar.show();
                actionMode.finish();
            }
            return true;
        } else if (itemId == R.id.rename_menu) {
            int index = selection.keyAt(0);
            final MediaItem selected = dataManager.get(index);
            if (selected != null) {
                Usefuls.createInputDialog(this, R.string.dialog_title_change_name, R.string.dialog_message_change_name, selected.title, (name) -> {
                    if (name.length() == 0) {
                        Toast.makeText(MediaListActivity.this, R.string.toast_error_name_is_empty, Toast.LENGTH_LONG).show();
                    } else {
                        Usefuls.newTask(() -> {
                            if (dataManager.changeTitle(index, name)) {
                                showInfoToast(R.string.toast_info_name_changed);
                            } else {
                                showErrorToast(R.string.toast_error_cannot_change_name);
                            }
                        });
                    }
                });
            }
            adapter.clearSelection();
            return true;
        } else if (itemId == R.id.eject_menu) {
            DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivityForResult(intent, REQ_PICK_EJECT_DEST);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.dialog_message_eject_file, count))
                    .setPositiveButton(R.string.dialog_button_ok, onClickListener)
                    .setNegativeButton(R.string.dialog_button_cancel, onClickListener).show();
            return true;
        } else if (itemId == R.id.detail_menu) {
            int index = selection.keyAt(0);
            MediaDetailDialog dialog = new MediaDetailDialog(this, dataManager.get(index));
            dialog.show();
            adapter.clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.clearSelection();
        actionMode = null;
        actionBarSelectionTextView = null;
    }

    public static SharedPreferences getPrivatePreferences() {
        return privatePreferences;
    }

    public void updateTagList(String tags) {
        String[] tagArr = tags.trim().split(" ");
        if (tagArr.length > 0) {
            tagFilters.setVisibility(View.VISIBLE);
            tagFilters.removeAllViews();
            for (String tag : tagArr) {
                addTagImpl(tag);
            }
        } else {
            tagFilters.setVisibility(View.GONE);
        }
    }

    public void clearTagList() {
        tagFilters.setVisibility(View.GONE);
        tagFilters.removeAllViews();
        dataManager.setTagString(null);
    }

    private void addTagImpl(String tag) {
        if (tag.length() > 0) {
            Chip tagChip = new Chip(this);
            tagChip.setText(tag);
            tagChip.setCloseIconVisible(true);
            tagChip.setOnCloseIconClickListener(v -> {
                tagFilters.removeView(v);
                StringBuilder sb = new StringBuilder();
                for (int cnt = tagFilters.getChildCount(), i = 0; i < cnt; i++) {
                    sb.append(((Chip) tagFilters.getChildAt(i)).getText());
                    if (i < cnt - 1)
                        sb.append(' ');
                }
                dataManager.setTagString(sb.toString());
            });
            tagFilters.addView(tagChip);
        }
    }

    private void showInfoToast(@StringRes int resId) {
        Usefuls.threadSafeToast(this, resId, Toast.LENGTH_SHORT);
    }

    private void showInfoToast(String message) {
        Usefuls.threadSafeToast(this, message, Toast.LENGTH_SHORT);
    }

    private void showErrorToast(@StringRes int resId) {
        Usefuls.threadSafeToast(this, resId, Toast.LENGTH_LONG);
    }

    private void showErrorToast(String message) {
        Usefuls.threadSafeToast(this, message, Toast.LENGTH_LONG);
    }
}