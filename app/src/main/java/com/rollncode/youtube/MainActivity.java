package com.rollncode.youtube;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 132;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkPermission();
        } else {
            start();
        }
    }

    /**
     * Get the result of check system permission to draw overlay window
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                start();
            } else {
                Toast.makeText(getBaseContext(), "you deny to draw overlay window", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Check system permission to draw overlay window
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {
            if (Settings.canDrawOverlays(this)) {
                start();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
    }

    /**
     * Current activity starts service and finish itself
     */
    private void start(){
        startService(new Intent(this, HUD.class));
        finish();
    }
}
