package com.won983212.vaultapp.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.won983212.vaultapp.MediaDatabaseManager;
import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.util.Usefuls;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MediaDetailDialog extends Dialog {
    private final MediaItem item;
    private boolean isEditMode = false;
    private ChipGroup tagGroup;
    private TextView noTagText;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};

    public MediaDetailDialog(@NonNull Context context, MediaItem item) {
        super(context);
        this.item = item;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        setContentView(R.layout.media_detail_dialog);

        ImageView titleIcon = findViewById(R.id.detail_dialog_titleIcon);
        if (item.isDirectory()) {
            titleIcon.setImageResource(R.drawable.ic_folder);
        } else if (item.isImage()) {
            titleIcon.setImageResource(R.drawable.ic_photo);
        } else if (item.isVideo()) {
            titleIcon.setImageResource(R.drawable.ic_movie);
        }

        TextView title = findViewById(R.id.detail_dialog_titleText);
        title.setText(item.title);

        TextView lastModified = findViewById(R.id.detail_dialog_modifiedDateText);
        lastModified.setText(getFormattedLastModified());

        TextView pathText = findViewById(R.id.detail_dialog_pathText);
        pathText.setText(item.getFormattedPath());

        TextView sizeText = findViewById(R.id.detail_dialog_sizeText);
        if (item.isDirectory()) {
            sizeText.setVisibility(View.GONE);
        } else {
            sizeText.setText(getFormattedFileSize());
        }

        TextView viewCountText = findViewById(R.id.detail_dialog_viewCountText);
        viewCountText.setText(getContext().getString(R.string.detail_dialog_view_count, item.viewCount));

        noTagText = findViewById(R.id.detail_dialog_noTagText);
        tagGroup = findViewById(R.id.detail_dialog_tagGroup);
        for (String tag : item.tags) {
            addTag(tag);
        }

        updateTagGroupVisibility();

        Button addTagButton = findViewById(R.id.detail_dialog_addTag);
        Button editTagButton = findViewById(R.id.detail_dialog_editTag);

        addTagButton.setOnClickListener((v) -> Usefuls.createInputDialog(getContext(), R.string.detail_dialog_add_tag, R.string.detail_dialog_add_tag_message, "", input -> {
            String tags = input.trim().replaceAll(",", " ");
            if (tags.length() > 0) {
                for (String tag : tags.split(" "))
                    addTag(tag);
                updateTagGroupVisibility();
            } else {
                Usefuls.threadSafeToast(getContext(), R.string.toast_error_empty_tag_name, Toast.LENGTH_LONG);
            }
        }));

        editTagButton.setOnClickListener((v) -> {
            Context ctx = getContext();
            isEditMode = !isEditMode;
            if (isEditMode) {
                addTagButton.setVisibility(View.VISIBLE);
                editTagButton.setText(ctx.getString(R.string.detail_dialog_edit_tag_complete));
                iterateTagView(tagView -> tagView.setCloseIconVisible(true));
            } else {
                saveToDatabase();
                addTagButton.setVisibility(View.INVISIBLE);
                editTagButton.setText(ctx.getString(R.string.detail_dialog_edit_tag));
                iterateTagView(tagView -> tagView.setCloseIconVisible(false));
            }
        });
    }

    private void updateTagGroupVisibility() {
        if (tagGroup.getChildCount() == 0) {
            noTagText.setVisibility(View.VISIBLE);
            tagGroup.setVisibility(View.INVISIBLE);
        } else {
            noTagText.setVisibility(View.INVISIBLE);
            tagGroup.setVisibility(View.VISIBLE);
        }
    }

    private void iterateTagView(TagIterateCallback callback) {
        for (int len = tagGroup.getChildCount(), i = 0; i < len; i++)
            callback.iterate((Chip) tagGroup.getChildAt(i));
    }

    private void addTag(String tag) {
        Chip chip = new Chip(getContext());
        chip.setCloseIconResource(R.drawable.ic_delete);
        chip.setCloseIconTintResource(R.color.gray_700);
        chip.setCloseIconVisible(isEditMode);
        chip.setOnCloseIconClickListener(v -> {
            tagGroup.removeView(v);
            updateTagGroupVisibility();
        });
        chip.setText(tag);
        tagGroup.addView(chip);
    }

    private void saveToDatabase() {
        ArrayList<String> tagList = new ArrayList<>();
        MediaDatabaseManager dataManager = VaultApp.getInstance().getDataManager();
        iterateTagView(tagView -> tagList.add(tagView.getText().toString()));
        Usefuls.newTask(() -> {
            dataManager.changeTags(item, tagList.toArray(new String[0]));
            Usefuls.threadSafeToast(getContext(), R.string.toast_info_tags_saved, Toast.LENGTH_SHORT);
        });
    }

    private String getFormattedLastModified() {
        return DATE_FORMAT.format(new Date(item.lastModified));
    }

    private String getFormattedFileSize() {
        double size = item.getDocumentFile(getContext()).length();
        int sizeIdx = 0;
        while (size >= 1024) {
            size /= 1024;
            sizeIdx++;
        }
        return ((int) (size * 100) / 100.0) + " " + SIZE_UNITS[sizeIdx];
    }

    private interface TagIterateCallback {
        void iterate(Chip view);
    }
}
