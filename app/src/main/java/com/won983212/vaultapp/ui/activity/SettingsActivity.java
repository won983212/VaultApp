package com.won983212.vaultapp.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.won983212.vaultapp.ContentFileHandler;
import com.won983212.vaultapp.R;
import com.won983212.vaultapp.VaultApp;
import com.won983212.vaultapp.database.MediaFileDatabase;
import com.won983212.vaultapp.util.Usefuls;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQ_RESET_ROOT_DIR = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment(this))
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, R.string.toast_error_cannot_retrieve_data, Toast.LENGTH_LONG).show();
                return;
            }
            if (requestCode == REQ_RESET_ROOT_DIR) {
                Usefuls.newTask(() -> {
                    MediaFileDatabase db = MediaFileDatabase.getInstance(this);
                    if (db.backupDatabaseFile(this)) {
                        VaultApp.post(() -> ContentFileHandler.getInstance().parseActivityResult(this, data));
                    } else {
                        Usefuls.threadSafeToast(this, R.string.toast_error_cannot_backup_db, Toast.LENGTH_LONG);
                    }
                });
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, R.string.toast_info_cancelled_select, Toast.LENGTH_SHORT).show();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final AppCompatActivity activity;

        private SettingsFragment(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            findPreference("change_working_dir").setOnPreferenceClickListener(preference -> {
                ContentFileHandler fileManager = ContentFileHandler.getInstance();
                fileManager.clearSelectedURI();
                fileManager.requestSetRootPath(activity, REQ_RESET_ROOT_DIR);
                return true;
            });

            findPreference("change_pwd").setOnPreferenceClickListener(preference -> {
                SharedPreferences preferences = activity.getSharedPreferences("password", MODE_PRIVATE);
                preferences.edit().clear().apply();
                Intent intent = new Intent(activity, LoginActivity.class);
                startActivity(intent);
                return true;
            });

            findPreference("reset_view_count").setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("모든 파일의 조회수가 0으로 초기화됩니다. 진행하시겠습니까?")
                        .setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> Usefuls.newTask(() -> {
                            VaultApp.getInstance().getDataManager().clearViewCount();
                            Usefuls.threadSafeToast(getContext(), R.string.toast_info_cleared_view_count, Toast.LENGTH_SHORT);
                        }))
                        .setNegativeButton(R.string.dialog_button_cancel, null).show();
                return true;
            });

            findPreference("backup_now").setOnPreferenceClickListener(preference -> {
                final Context ctx = getContext();
                final ProgressDialog dialog = Usefuls.createIndeterminateProgressDialog(ctx, R.string.dialog_message_loading_db);
                Usefuls.newTask(() -> {
                    MediaFileDatabase db = MediaFileDatabase.getInstance(ctx);
                    if (db.backupDatabaseFile(ctx, "backup")) {
                        Usefuls.threadSafeToast(ctx, R.string.toast_info_backup_complete, Toast.LENGTH_LONG);
                    } else {
                        Usefuls.threadSafeToast(ctx, R.string.toast_error_cannot_backup_db, Toast.LENGTH_LONG);
                    }
                    dialog.dismiss();
                });
                return true;
            });
        }
    }
}