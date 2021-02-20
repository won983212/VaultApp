package com.won983212.vaultapp.ui.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.ui.activity.MediaListActivity;
import com.won983212.vaultapp.util.Usefuls;

import java.util.Set;

public class AllTagsDialog extends Dialog {
    private final MediaListActivity listActivity;
    private TextView noTagText;
    private ChipGroup tagGroup;

    public AllTagsDialog(@NonNull MediaListActivity listActivity) {
        super(listActivity);
        this.listActivity = listActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // background blur
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        // apply layout file
        setContentView(R.layout.tags_view_dialog);

        noTagText = findViewById(R.id.tag_view_dialog_noTagText);
        tagGroup = findViewById(R.id.tag_view_dialog_tagGroup);
        loadAllTags();
    }

    private void updateTagGroupVisibility() {
        if (tagGroup.getChildCount() == 0) {
            noTagText.setVisibility(View.VISIBLE);
            tagGroup.setVisibility(View.GONE);
        } else {
            noTagText.setVisibility(View.GONE);
            tagGroup.setVisibility(View.VISIBLE);
        }
    }

    private void addTag(String tag) {
        Chip chip = new Chip(getContext());
        chip.setOnClickListener((v) -> {
            listActivity.addTagToList(((Chip) v).getText().toString());
            dismiss();
        });
        chip.setText(tag);
        tagGroup.addView(chip);
    }

    private void loadAllTags() {
        final ProgressDialog dialog = Usefuls.createIndeterminateProgressDialog(getContext(), R.string.tag_view_dialog_loading_tags);
        Usefuls.newTask(() -> {
            Set<String> tags = VaultApp.getInstance().getDataManager().retrieveAllTags();
            VaultApp.post(() -> {
                for (String tag : tags) {
                    addTag(tag);
                }
                updateTagGroupVisibility();
                dialog.dismiss();
            });
        });
    }
}
