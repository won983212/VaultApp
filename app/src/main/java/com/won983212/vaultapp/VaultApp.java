package com.won983212.vaultapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

public class VaultApp extends Application {
    private static final Handler HANDLER = new Handler(Looper.myLooper());
    private static VaultApp instance;

    private MediaDatabaseManager dataManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        dataManager = new MediaDatabaseManager();
    }

    public MediaDatabaseManager getDataManager() {
        return dataManager;
    }

    public static VaultApp getInstance() {
        return instance;
    }

    public static void post(Runnable runnable) {
        HANDLER.post(runnable);
    }
}
