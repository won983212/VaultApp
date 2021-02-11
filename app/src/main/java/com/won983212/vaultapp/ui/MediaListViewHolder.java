package com.won983212.vaultapp.ui;

import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.MultipleSelection;
import com.won983212.vaultapp.R;

public class MediaListViewHolder extends RecyclerView.ViewHolder {
    // main views
    private final ImageView iconView;
    private final ImageView pictureView;
    private final TextView titleView;
    private final TextView timeLengthView;

    // check state views
    private final CheckBox checkBox;
    private final FrameLayout topSelectionPanel;

    private final ItemEventCallback callback;
    private final MultipleSelection selection;

    public MediaListViewHolder(@NonNull View itemView, MultipleSelection selection, ItemEventCallback callback) {
        super(itemView);

        this.selection = selection;
        this.callback = callback;

        iconView = itemView.findViewById(R.id.iconImageView);
        pictureView = itemView.findViewById(R.id.pictureImageView);
        titleView = itemView.findViewById(R.id.titleTextView);
        timeLengthView = itemView.findViewById(R.id.timeLengthTextView);
        checkBox = itemView.findViewById(R.id.checkbox);
        topSelectionPanel = itemView.findViewById(R.id.topSelectionPanel);

        itemView.setOnClickListener((v) -> {
            int position = getAdapterPosition();
            if (callback != null)
                callback.onClickItem(this, position);
            if (selection.isMultiSelectionEnabled()) {
                setCheckedState(!selection.isChecked(position), true);
            }
        });

        itemView.setOnLongClickListener((v) -> {
            if (!selection.isMultiSelectionEnabled()) {
                setCheckedState(true, true);
            }
            return true;
        });
    }

    public void setCheckedState(boolean value, boolean notify) {
        int visibility = value ? View.VISIBLE : View.INVISIBLE;
        topSelectionPanel.setVisibility(visibility);
        checkBox.setVisibility(visibility);
        checkBox.setChecked(value);

        if (notify) {
            int position = getAdapterPosition();
            selection.setChecked(position, value);

            if (callback != null)
                callback.onItemSelectionChanged(this, position);
        }
    }

    private void setTimeLength(String length) {
        if (length != null) {
            timeLengthView.setVisibility(View.VISIBLE);
            timeLengthView.setText(length);
        } else {
            timeLengthView.setVisibility(View.INVISIBLE);
        }
    }

    public void setItem(MediaItem item) {
        Uri thumbnail;

        if (item.isDirectory()) {
            thumbnail = item.getFolderThumbnailUri();
        } else {
            thumbnail = item.getDocumentURI();
        }

        if (thumbnail != null) {
            Glide.with(pictureView)
                    .load(thumbnail)
                    .thumbnail(0.1f)
                    .override(300)
                    .into(pictureView);
            iconView.setVisibility(View.INVISIBLE);
            pictureView.setVisibility(View.VISIBLE);
        } else {
            iconView.setImageResource(item.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_photo);
            iconView.setVisibility(View.VISIBLE);
            pictureView.setVisibility(View.INVISIBLE);
        }

        if (item.isDirectory()) {
            setTimeLength(null);
            titleView.setText(item.title);
            titleView.setVisibility(View.VISIBLE);
        } else if (item.isImage()) {
            setTimeLength(null);
            titleView.setVisibility(View.INVISIBLE);
        } else if (item.isVideo()) {
            setTimeLength(item.videoLength);
            titleView.setVisibility(View.INVISIBLE);
        }
    }
}
