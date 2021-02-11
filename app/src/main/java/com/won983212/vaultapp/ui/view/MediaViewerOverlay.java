package com.won983212.vaultapp.ui.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.won983212.vaultapp.MediaItem;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.ui.dialog.MediaDetailDialog;

public class MediaViewerOverlay extends ConstraintLayout {
    private TextView title;

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
}
