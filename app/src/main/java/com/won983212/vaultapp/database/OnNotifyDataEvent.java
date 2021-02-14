package com.won983212.vaultapp.database;

import com.won983212.vaultapp.MediaItem;

import java.util.List;

public interface OnNotifyDataEvent {
    void onDataUpdated(int position, int count);

    void onDataSetUpdated();

    void onNotify(List<MediaItem> list, Runnable callback);
}
