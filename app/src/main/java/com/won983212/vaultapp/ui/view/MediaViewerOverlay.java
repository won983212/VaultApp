package com.won983212.vaultapp.ui.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.stfalcon.imageviewer.StfalconImageViewer;
import com.won983212.vaultapp.MediaFileActionExecutor;
import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.ui.dialog.MediaDetailDialog;
import com.won983212.vaultapp.util.Usefuls;

public class MediaViewerOverlay extends ConstraintLayout {
    private TextView title;
    private StfalconImageViewer<MediaItem> imageViewer = null;

    public MediaViewerOverlay(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MediaViewerOverlay(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaViewerOverlay(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = View.inflate(context, R.layout.media_view_overlay, this);
        title = view.findViewById(R.id.overlay_titleTextView);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void update(MediaItem item) {
        title.setText(item.title);

        findViewById(R.id.overlay_showDetailView).setOnClickListener((v) -> {
            MediaDetailDialog dialog = new MediaDetailDialog(getContext(), item);
            dialog.show();
        });

        findViewById(R.id.overlay_deleteFile).setOnClickListener((v) -> {
            Context ctx = getContext();
            DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        int index = VaultApp.getInstance().getDataManager().indexOf(item);
                        if (index >= 0) {
                            ProgressDialog progressDialog = Usefuls.createProgressDialog(ctx, R.string.dialog_message_deleting_file);

                            MediaFileActionExecutor fileExecutor = new MediaFileActionExecutor(ctx);
                            fileExecutor.setProgressListener(progressDialog::setProgress);

                            fileExecutor.begin();
                            fileExecutor.delete(index);

                            Usefuls.newTask(() -> {
                                boolean success = fileExecutor.commit();
                                VaultApp.post(() -> {
                                    if (success) {
                                        Toast.makeText(ctx, R.string.toast_info_deleted_file, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ctx, R.string.toast_error_cannot_delete_file, Toast.LENGTH_LONG).show();
                                    }
                                    progressDialog.dismiss();
                                    if (imageViewer != null)
                                        imageViewer.dismiss();
                                });
                            });
                        } else {
                            Toast.makeText(ctx, R.string.toast_error_cannot_delete_file, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setMessage(R.string.dialog_message_want_to_delete_file)
                    .setPositiveButton(R.string.dialog_button_yes, onClickListener)
                    .setNegativeButton(R.string.dialog_button_no, onClickListener).show();
        });

        Button playVideoButton = findViewById(R.id.overlay_playVideoButton);
        if (item.isVideo()) {
            playVideoButton.setVisibility(VISIBLE);
        } else {
            playVideoButton.setVisibility(INVISIBLE);
        }

        playVideoButton.setOnClickListener((v) -> {
            // increasing view count of video is only when you play video.
            VaultApp.getInstance().getDataManager().increaseViewCountAsync(item);

            // play video
            Intent mWatch = new Intent(Intent.ACTION_VIEW);
            mWatch.setDataAndType(item.getDocumentURI(), item.type);
            mWatch.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(mWatch);
        });
    }

    public void setImageViewer(StfalconImageViewer<MediaItem> imageViewer) {
        this.imageViewer = imageViewer;
    }
}
