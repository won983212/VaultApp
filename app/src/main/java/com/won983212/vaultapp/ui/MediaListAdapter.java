package com.won983212.vaultapp.ui;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.won983212.vaultapp.MediaDatabaseManager;
import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.MultipleSelection;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.database.OnNotifyDataEvent;

import java.util.ArrayList;
import java.util.List;

public class MediaListAdapter extends RecyclerView.Adapter<MediaListViewHolder> implements OnNotifyDataEvent, ItemEventCallback {
    private final AsyncListDiffer<MediaItem> mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private final MediaDatabaseManager dataManager;
    private final MultipleSelection multiSelection;
    private ItemEventCallback itemEventListener;
    private RecyclerView recyclerView = null;

    public MediaListAdapter(MediaDatabaseManager dataManager) {
        this.dataManager = dataManager;
        this.dataManager.setDatabaseNotifyEvent(this);
        multiSelection = new MultipleSelection();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @NonNull
    @Override
    public MediaListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_list_item, parent, false);
        return new MediaListViewHolder(view, multiSelection, this);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaListViewHolder holder, int position) {
        holder.setItem(mDiffer.getCurrentList().get(position));
        holder.setCheckedState(multiSelection.isChecked(position), false);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @Override
    public void onDataUpdated(int position, int count) {
        if (count == 1) {
            notifyItemChanged(position);
        } else {
            notifyItemRangeChanged(position, count);
        }
    }

    @Override
    public void onDataSetUpdated() {
        notifyDataSetChanged();
    }

    @Override
    public void onNotify(List<MediaItem> list, Runnable callback) {
        mDiffer.submitList(new ArrayList<>(list), callback);
    }

    public void setItemEventListener(ItemEventCallback listener) {
        this.itemEventListener = listener;
    }

    @Override
    public void onClickItem(MediaListViewHolder holder, int position) {
        if (itemEventListener != null)
            itemEventListener.onClickItem(holder, position);
    }

    @Override
    public void onItemSelectionChanged(MediaListViewHolder holder, int position) {
        if (itemEventListener != null)
            itemEventListener.onItemSelectionChanged(holder, position);
    }

    @Override
    public void onItemCleared() {
        if (itemEventListener != null)
            itemEventListener.onItemCleared();
    }

    public MultipleSelection getSelection() {
        return multiSelection;
    }

    public void selectAll() {
        if (recyclerView != null) {
            for (int i = 0; i < dataManager.size(); i++) {
                multiSelection.setChecked(i, true);
            }
            for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i) {
                MediaListViewHolder holder = (MediaListViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                holder.setCheckedState(true, true);
            }
        }
    }

    public void clearSelection() {
        if (recyclerView != null) {
            SparseBooleanArray arr = multiSelection.getCheckedPositions();
            for (int i = 0; i < arr.size(); i++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(arr.keyAt(i));
                if (viewHolder != null) {
                    ((MediaListViewHolder) viewHolder).setCheckedState(false, false);
                }
            }
        }

        multiSelection.clear();
        onItemCleared();
    }

    private static final DiffUtil.ItemCallback<MediaItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<MediaItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
            return oldItem.obfuscatedName.equals(newItem.obfuscatedName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
