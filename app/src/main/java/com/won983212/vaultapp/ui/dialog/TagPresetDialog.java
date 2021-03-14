package com.won983212.vaultapp.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.util.Usefuls;

import java.util.ArrayList;

public class TagPresetDialog extends Dialog {
    private static final String KEY_GROUPS = "groups";
    private static final String KEY_TAGS = "tags";

    private TextView noTagText;
    private SharedPreferences preferences;
    private LinearLayout tagContainer;
    private TagSelectedListener listener;
    private boolean deleteMode = false;
    private final ArrayList<TagGroup> groups = new ArrayList<>();

    public TagPresetDialog(Context context) {
        super(context);
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

        // load preferences
        preferences = getContext().getSharedPreferences("tagPresets", Context.MODE_PRIVATE);

        noTagText = findViewById(R.id.tag_view_dialog_noTagText);
        tagContainer = findViewById(R.id.tag_view_tagContainer);

        findViewById(R.id.tag_view_addTagGroup).setOnClickListener((v) -> Usefuls.createInputDialog(getContext(), R.string.tag_view_add_tag_group_title, R.string.tag_view_add_tag_group_message, "", input -> {
            String[] arr = _iterateTagInput(input, R.string.toast_error_empty_tag_group_name);
            if (arr != null) {
                for (String name : arr)
                    addTagGroup(name);
                updateTagGroupVisibility();
                saveTagPresets();
            }
        }));

        findViewById(R.id.tag_view_removeTagGroup).setOnClickListener((v) -> {
            if (groups.size() > 0) {
                Usefuls.createSelectDialog(getContext(), R.string.tag_view_remove_tag_group_title, R.string.tag_view_remove_tag_group_message, groups, selected -> {
                    TagGroup group = groups.remove(selected);
                    tagContainer.removeViewAt(tagContainer.indexOfChild(group.view) - 1);
                    tagContainer.removeView(group.view);
                    updateTagGroupVisibility();
                    saveTagPresets();
                });
            }
        });

        findViewById(R.id.tag_view_addTag).setOnClickListener((v) -> {
            if (groups.isEmpty()) {
                Toast.makeText(getContext(), R.string.toast_error_empty_tag_group, Toast.LENGTH_LONG).show();
            } else {
                createAddTagDialog((group, tags) -> {
                    String[] arr = _iterateTagInput(tags, R.string.toast_error_empty_tag_name);
                    if (arr != null) {
                        for (String name : arr)
                            addTag(group, name);
                        updateTagGroupVisibility();
                        saveTagPresets();
                    }
                });
            }
        });

        findViewById(R.id.tag_view_deleteTag).setOnClickListener((v) -> {
            deleteMode = !deleteMode;
            for (TagGroup group : groups) {
                for (int i = 0; i < group.view.getChildCount(); i++) {
                    Chip chip = (Chip) group.view.getChildAt(i);
                    chip.setCloseIconVisible(deleteMode);
                    if (deleteMode)
                        chip.setChecked(false);
                    chip.setCheckable(!deleteMode);
                }
            }
        });

        findViewById(R.id.tag_view_okButton).setOnClickListener((v) -> {
            if (listener != null) {
                StringBuilder sb = new StringBuilder();
                for (TagGroup group : groups) {
                    for (int i = 0; i < group.view.getChildCount(); i++) {
                        Chip chip = (Chip) group.view.getChildAt(i);
                        if (chip.isChecked()) {
                            sb.append(chip.getText());
                            sb.append(' ');
                        }
                    }
                }
                listener.onSelected(sb.toString().trim());
            }
            dismiss();
        });

        loadTagPresets();
    }

    public void setTagSelectedListener(TagSelectedListener listener) {
        this.listener = listener;
    }

    private String[] _iterateTagInput(String in, @StringRes int errorMessage) {
        String names = in.trim().replaceAll(",", " ");
        if (names.length() > 0) {
            return names.split(" ");
        } else {
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private void addTag(int groupIdx, String name) {
        TagGroup group = groups.get(groupIdx);

        Chip chip = new Chip(getContext());
        chip.setOnCloseIconClickListener(v -> {
            group.view.removeView(v);
            group.tags.remove(name);
            saveTagPresets();
        });
        chip.setCheckable(true);
        chip.setText(name);

        group.view.addView(chip);
        group.tags.add(name);
    }

    private void addTagGroup(String name) {
        Context ctx = getContext();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = ctx.getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        ChipGroup groupView = new ChipGroup(ctx);
        groupView.setSingleSelection(false);
        groupView.setLayoutParams(params);
        groups.add(new TagGroup(name, groupView));

        TextView groupLabel = new TextView(ctx);
        groupLabel.setTextColor(ctx.getResources().getColor(R.color.primary_500));
        groupLabel.setText(name);

        tagContainer.addView(groupLabel);
        tagContainer.addView(groupView);
        updateTagGroupVisibility();
    }

    private void loadTagPresets() {
        String groupsValue = preferences.getString(KEY_GROUPS, null).trim();
        String tagsValue = preferences.getString(KEY_TAGS, null).trim();

        if (groupsValue != null && tagsValue != null && groupsValue.length() > 0) {
            String[] tags = tagsValue.split("/", -1);
            String[] groups = groupsValue.split(",", -1);

            for (int i = 0; i < groups.length; i++) {
                addTagGroup(groups[i]);
                if (tags[i].length() > 0) {
                    for (String tag : tags[i].split(","))
                        addTag(i, tag);
                }
            }
        }
        updateTagGroupVisibility();
    }

    private void saveTagPresets() {
        StringBuilder serialized_groups = new StringBuilder();
        StringBuilder serialized_tags = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            TagGroup g = groups.get(i);
            serialized_groups.append(g.label);
            if (i < groups.size() - 1)
                serialized_groups.append(',');
            for (int j = 0; j < g.tags.size(); j++) {
                serialized_tags.append(g.tags.get(j));
                if (j < g.tags.size() - 1)
                    serialized_tags.append(',');
            }
            if (i < groups.size() - 1)
                serialized_tags.append('/');
        }

        // save to disk
        preferences.edit()
                .putString(KEY_GROUPS, serialized_groups.toString())
                .putString(KEY_TAGS, serialized_tags.toString()).apply();
    }

    private void updateTagGroupVisibility() {
        if (tagContainer.getChildCount() == 0) {
            noTagText.setVisibility(View.VISIBLE);
            tagContainer.setVisibility(View.GONE);
        } else {
            noTagText.setVisibility(View.GONE);
            tagContainer.setVisibility(View.VISIBLE);
        }
    }

    private void createAddTagDialog(AddTagDialogAcceptClickListener callback) {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.tag_view_add_tag_title);
        builder.setMessage(R.string.tag_view_add_tag_message);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = margin;

        final Spinner groupIn = new Spinner(context);
        ArrayAdapter<TagGroup> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groups);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupIn.setAdapter(arrayAdapter);
        groupIn.setLayoutParams(params);
        container.addView(groupIn);

        final EditText tagIn = new EditText(context);
        tagIn.setLayoutParams(params);
        container.addView(tagIn);

        builder.setView(container);
        builder.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> callback.onAccept(groupIn.getSelectedItemPosition(), tagIn.getText().toString()));
        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.show();
        tagIn.setOnKeyListener((v, key, e) -> {
            if (key == KeyEvent.KEYCODE_ENTER) {
                callback.onAccept(groupIn.getSelectedItemPosition(), tagIn.getText().toString());
                dialog.dismiss();
            }
            return false;
        });
    }

    public interface TagSelectedListener {
        void onSelected(String tag);
    }

    private interface AddTagDialogAcceptClickListener {
        void onAccept(int index, String input);
    }

    private static class TagGroup {
        private final String label;
        private final ChipGroup view;
        private final ArrayList<String> tags;

        public TagGroup(String label, ChipGroup view) {
            this.label = label;
            this.view = view;
            this.tags = new ArrayList<>();
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}
