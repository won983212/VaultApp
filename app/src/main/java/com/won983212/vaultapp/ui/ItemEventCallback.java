package com.won983212.vaultapp.ui;

public interface ItemEventCallback {
    void onClickItem(MediaListViewHolder holder, int position);

    void onItemSelectionChanged(MediaListViewHolder holder, int position);

    void onItemCleared();
}
