package com.rollncode.youtube.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.rollncode.youtube.R;
import com.rollncode.youtube.application.AContext;
import com.rollncode.youtube.service.PlayerService;
import com.rollncode.youtube.types.LinkType;
import com.rollncode.youtube.types.PlayerAction;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class PlayerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 132;
    private static final String PERMISSION_OVERLAY = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
    private static final String EXTRA_TEXT = "android.intent.extra.TEXT";

    private boolean mStartActivityForResult;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        checkPermission();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                sendBroadcastHide();
            }
        } else {
            sendBroadcastHide();
        }

        if (!mStartActivityForResult) {
            mStartActivityForResult = false;
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (Settings.canDrawOverlays(this)) {
                    startYoutube();
                    mStartActivityForResult = false;

                } else {
                    sendBroadcastStop();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // TODO: why public? You also do no need the method which is used just in one place
    public void checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startYoutube();

        } else if (Settings.canDrawOverlays(this)) {
            startYoutube();
            mStartActivityForResult = false;

        } else {
            mStartActivityForResult = true;
            startActivityForResult(new Intent(PERMISSION_OVERLAY, Uri.parse("package:" + getPackageName())), REQUEST_CODE);
        }
    }

    private void startYoutube() {
        final Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            if (url != null && url.contains(LinkType.NORMAL)) {
                startPlayerService(url);

            } else if (intent.hasExtra(EXTRA_TEXT)) {
                url = intent.getStringExtra(EXTRA_TEXT);
                startPlayerService(url);

            } else if (intent.getAction().equals(PlayerAction.START_YOUTUBE.getName())) {
                sendBroadcastShow();

            } else {
                finish();
            }

        } else {
            sendBroadcastShow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close:
                sendBroadcastStop();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendBroadcastStop() {
        AContext.sendLocalBroadcast(new Intent(PlayerAction.STOP_YOUTUBE.getName()));
        finish();
    }

    private void sendBroadcastHide() {
        AContext.sendLocalBroadcast(new Intent(PlayerAction.HIDE_YOUTUBE.getName()));
    }

    private void sendBroadcastShow() {
        AContext.sendLocalBroadcast(new Intent(PlayerAction.SHOW_YOUTUBE.getName()));
    }

    private void startPlayerService(@NonNull String url) {
        PlayerService.start(this, url);
    }
}
