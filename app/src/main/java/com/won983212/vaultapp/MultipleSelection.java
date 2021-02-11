package com.won983212.vaultapp;

import android.util.SparseBooleanArray;

public class MultipleSelection {
    private final SparseBooleanArray checkStates = new SparseBooleanArray();

    public boolean isMultiSelectionEnabled() {
        return getCheckedCount() > 0;
    }

    public boolean isChecked(int position) {
        return checkStates.get(position, false);
    }

    public void setChecked(int position, boolean value) {
        if (value) {
            checkStates.put(position, true);
        } else {
            checkStates.delete(position);
        }
    }

    public int getCheckedCount() {
        return checkStates.size();
    }

    public SparseBooleanArray getCheckedPositions() {
        return checkStates;
    }

    public void clear() {
        checkStates.clear();
    }
}
