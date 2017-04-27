package com.rollncode.youtube.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.rollncode.youtube.R;
import com.rollncode.youtube.types.PlayerAction;
import com.rollncode.youtube.utility.Utils;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class PlayerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 132;
    private boolean mStartActivityForResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onPause() {
        super.onPause();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (Settings.canDrawOverlays(this)) {
//                hideYoutubeWindow();
//            }
//        } else {
            hideYoutubeWindow();
//        }

        if (!mStartActivityForResult) {
            Log.d("tagger", "MainActivity onPause finish()");
            mStartActivityForResult = false;
            finish();
        }
    }

    public void checkPermission() {
        Log.d("tagger", "ACTIVITY CHECK PERMISSION");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startYoutube();

        } else if (Settings.canDrawOverlays(this)) {
            startYoutube();
            mStartActivityForResult = false;

        } else {
            mStartActivityForResult = true;
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())), REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (Settings.canDrawOverlays(this)) {
            Log.d("tagger", "ON ACTIVITY RESULT ALLOW");
            startYoutube();
            mStartActivityForResult = false;
            return;
        }
        Log.d("tagger", "ON ACTIVITY RESULT DENY");
        stopYoutubeWindow();
    }

    private void hideYoutubeWindow() {
        Log.d("tagger", "ACTIVITY HIDE_YOUTUBE");
        startService(new Intent().setAction(PlayerAction.HIDE_YOUTUBE.getName()).setPackage(getPackageName()));
    }

    private void showYoutubeWindow() {
        Log.d("tagger", "ACTIVITY SHOW_YOUTUBE");
        startService(new Intent().setAction(PlayerAction.SHOW_YOUTUBE.getName()).setPackage(getPackageName()));
    }

    private void startYoutubeWindow(String url) {
        startService(new Intent(PlayerAction.START_YOUTUBE.getName()).putExtra("EXTRA_LINK", url).setPackage(getPackageName()));
    }

    private void stopYoutubeWindow() {
        Log.d("tagger", "ACTIVITY STOP_YOUTUBE");
        startService(new Intent().setAction(PlayerAction.STOP_YOUTUBE.getName()).setPackage(getPackageName()));
        finish();
    }

    private void startYoutube() {
        Intent intent = getIntent();
        if (intent != null) {
            Log.d("tagger", "ACTIVITY START_YOUTUBE WITH INTENT");
            String url = intent.getDataString();
            if (url != null && url.contains(Utils.LinkType.NORMAL)) {
                startYoutubeWindow(url);
                return;

            } else if (intent.hasExtra("android.intent.extra.TEXT")) {
                url = intent.getStringExtra("android.intent.extra.TEXT");
                Utils.toLog(url);
                startYoutubeWindow(url);
                return;

            } else if (intent.getAction().equals("ACTION_OPEN")) {
                showYoutubeWindow();
                return;

            } else {
                Log.d("tagger", "ACTIVITY START YOUTUBE WINDOW FINISH");
                finish();
                return;
            }
        }
        showYoutubeWindow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close /*2131427435*/:
                stopYoutubeWindow();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
