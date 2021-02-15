package com.won983212.vaultapp.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.won983212.vaultapp.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String PWD_KEY = "pwd";
    private static final String RETRY_TIMER_KEY = "retryTimer";
    private static final int MAX_RETRY = 3;
    private static final int RETRY_TIME_SEC = 60;

    private TextView alertTextView;
    private SharedPreferences preferences;
    private final RadioButton[] buttons = new RadioButton[4];
    private final char[] password = new char[4];
    private String initialPwd = null;
    private int cursor = 0;
    private int tryCount = 0;
    private int pwdMode = 0; // 0: set initlal pwd, 1: input pwd again, 2: input pwd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = getSharedPreferences("password", MODE_PRIVATE);
        alertTextView = findViewById(R.id.alertTextView);

        TableLayout numberPad = findViewById(R.id.numberPad);
        for (int i = 0; i < numberPad.getChildCount(); i++) {
            TableRow row = (TableRow) numberPad.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                row.getChildAt(j).setOnClickListener(this);
            }
        }

        LinearLayout passwordViewer = findViewById(R.id.passwordViewer);
        for (int i = 0; i < passwordViewer.getChildCount(); i++) {
            buttons[i] = (RadioButton) passwordViewer.getChildAt(i);
        }

        checkRetryable();

        if (!preferences.contains(PWD_KEY)) {
            pwdMode = 0;
            alertTextView.setText(R.string.login_set_initial_pwd);
        } else {
            pwdMode = 2;
        }
    }

    private boolean checkRetryable() {
        if (preferences.contains(RETRY_TIMER_KEY)) {
            long curr = System.currentTimeMillis();
            long diff = curr - preferences.getLong(RETRY_TIMER_KEY, curr);
            if (diff < RETRY_TIME_SEC * 1000) {
                tryCount = MAX_RETRY;
                alertTextView.setText(getResources().getString(R.string.login_retry_later, MAX_RETRY, RETRY_TIME_SEC - (int) Math.ceil(diff / 1000.0)));
                return false;
            } else {
                tryCount = 0;
                preferences.edit().remove(RETRY_TIMER_KEY).apply();
            }
        }
        return true;
    }

    private void success() {
        Intent intent = new Intent(this, MediaListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private String encrypt(String pwd) {
        MessageDigest md;
        try {
            String salt = Base64.encodeToString(pwd.getBytes(), Base64.DEFAULT);
            md = MessageDigest.getInstance("SHA-256");
            md.update((pwd + salt).getBytes());
            return String.format("%064x", new BigInteger(1, md.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean checkPassword() {
        return Objects.equals(encrypt(String.valueOf(password)), preferences.getString(PWD_KEY, ""));
    }

    private void processCheckPassword() {
        if (!checkRetryable())
            return;
        if (pwdMode == 0) {
            if (initialPwd == null) {
                alertTextView.setText(R.string.login_set_pwd_again);
                initialPwd = String.valueOf(password);
            } else {
                if (String.valueOf(password).equals(initialPwd)) {
                    alertTextView.setText(R.string.login_set_complete_pwd);
                    preferences.edit().putString(PWD_KEY, encrypt(initialPwd)).apply();
                    success();
                } else {
                    alertTextView.setText(R.string.login_not_match_pwd);
                }
                initialPwd = null;
            }
        } else if (pwdMode == 2) {
            if (checkPassword()) {
                alertTextView.setText(R.string.login_go_app);
                success();
            } else {
                if (++tryCount < MAX_RETRY) {
                    alertTextView.setText(R.string.login_retry_put_pwd);
                } else {
                    alertTextView.setText(getResources().getString(R.string.login_retry_later, MAX_RETRY, RETRY_TIME_SEC));
                    preferences.edit().putLong(RETRY_TIMER_KEY, System.currentTimeMillis()).apply();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        char buttonLabel = ((Button) v).getText().charAt(0);
        if (buttonLabel >= '0' && buttonLabel <= '9') {
            buttons[cursor].setChecked(true);
            password[cursor++] = buttonLabel;
            if (cursor >= 4) {
                processCheckPassword();
                for (RadioButton button : buttons)
                    button.setChecked(false);
                cursor = 0;
            }
        } else {
            if (cursor > 0) {
                buttons[--cursor].setChecked(false);
            }
        }
    }
}