package com.won983212.vaultapp.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.won983212.vaultapp.ContentFileHandler;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;

public class Usefuls {
    public static void newTask(Runnable r) {
        Thread t = new Thread(r);
        t.start();
    }

    public static void threadSafeToast(Context context, @StringRes int resId, int length) {
        VaultApp.post(() -> Toast.makeText(context, resId, length).show());
    }

    public static void threadSafeToast(Context context, String text, int length) {
        VaultApp.post(() -> Toast.makeText(context, text, length).show());
    }

    public static ProgressDialog createIndeterminateProgressDialog(Context context, @StringRes int message) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        dialog.setMessage(context.getString(message));
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static ProgressDialog createProgressDialog(Context context, @StringRes int message) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage(context.getString(message));
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static void createInputDialog(Context context, @StringRes int title, @StringRes int message, String initialValue, DialogAcceptClickListener callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.leftMargin = margin;
        params.rightMargin = margin;

        final EditText input = new EditText(context);
        if (initialValue != null)
            input.setText(initialValue);

        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> callback.onAccept(input.getText().toString()));
        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.show();
        input.setOnKeyListener((v, key, e) -> {
            if (key == KeyEvent.KEYCODE_ENTER) {
                callback.onAccept(input.getText().toString());
                dialog.dismiss();
            }
            return false;
        });
    }

    public static String getLastFolderName(String path) {
        int splitIdx = path.lastIndexOf('/', path.length() - 2);
        return path.substring(splitIdx + 1, path.length() - 1);
    }

    public static Uri getVaultFolderFileURI(String... paths) {
        return getVirtualFileURI(ContentFileHandler.getInstance().getRootDir().getUri(), paths);
    }

    public static Uri getVirtualFileURI(Uri parent, String... paths) {
        String path = eliminateTrailingSeperator(joinPath(paths));
        String id = DocumentsContract.getTreeDocumentId(parent) + "/" + path;
        return DocumentsContract.buildDocumentUriUsingTree(parent, id);
    }

    public static String joinPath(String... paths) {
        if (paths.length == 1)
            return paths[0];

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            String str = paths[i].replaceAll("\\\\", "/");
            if (i > 0) {
                while (str.length() > 0 && str.charAt(0) == '/')
                    str = str.substring(1);
            }
            if (i < paths.length - 1) {
                while (str.length() > 0 && str.charAt(str.length() - 1) == '/')
                    str = str.substring(0, str.length() - 1);
                sb.append(str);
                sb.append('/');
            } else {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    public static String eliminateTrailingSeperator(String path) {
        while (path.length() > 0 && path.charAt(0) == '/')
            path = path.substring(1);
        while (path.length() > 0 && path.charAt(path.length() - 1) == '/')
            path = path.substring(0, path.length() - 1);
        return path;
    }

    public interface DialogAcceptClickListener {
        void onAccept(String input);
    }
}
