package com.rollncode.backtube;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.rollncode.backtube.type.LinkType;
import com.rollncode.backtube.type.ServiceAction;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */
public final class PlayerActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 0xA;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);

        final Intent intent = super.getIntent();
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction()) && !intent.getBooleanExtra(Intent.EXTRA_INTENT, false)) {
            final Intent youtube = new Intent(Intent.ACTION_MAIN).setPackage("com.google.android.youtube").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (youtube.resolveActivity(getPackageManager()) != null) {
                super.startActivity(youtube);
            }
            super.finish();

        } else if (canDrawOverlays()) {
            startService();

        } else {
            super.startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQUEST_OVERLAY_PERMISSION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlayServiceAction(ServiceAction.SHOW);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overlayServiceAction(ServiceAction.HIDE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_close, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close:
                serviceAction(ServiceAction.STOP);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OVERLAY_PERMISSION:
                if (canDrawOverlays()) {
                    startService();

                } else {
                    serviceAction(ServiceAction.STOP);
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void startService() {
        final Intent intent = super.getIntent();
        if (intent == null || intent.getBooleanExtra(Intent.EXTRA_INTENT, false)) {
            serviceAction(ServiceAction.SHOW);
            return;
        }
        String uri = intent.getDataString();
        if (uri != null && (uri.contains(LinkType.NORMAL) || uri.contains(LinkType.SHORT))) {
            startService(uri);
            return;
        }
        uri = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(uri)) {
            if (!uri.startsWith("http") && (uri.contains(LinkType.NORMAL) || uri.contains(LinkType.SHORT))) {
                uri = uri.substring(uri.indexOf("http"));
            }
            startService(uri);
            return;
        }
        super.finish();
    }

    private void startService(@NonNull String url) {
        PlayerService.start(this, url);
    }

    private void overlayServiceAction(@ServiceAction String action) {
        if (canDrawOverlays()) {
            serviceAction(action);
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void serviceAction(@ServiceAction String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
        if (ServiceAction.STOP.equals(action)) {
            super.finish();
        }
    }
}