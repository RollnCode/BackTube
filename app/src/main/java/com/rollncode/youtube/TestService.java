package com.rollncode.youtube;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * This is temporary service realization to experiment how works alert window
 * in this way
 */

public class TestService extends Service {

    TestView mView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getBaseContext(),"onCreate", Toast.LENGTH_SHORT).show();
        int width = -(int)getResources().getDimension(R.dimen.view_width);
        int height = -(int)getResources().getDimension(R.dimen.view_height);
        mView = new TestView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                600,
                600,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.RGB_888);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(),"onDestroy", Toast.LENGTH_LONG).show();
        if(mView != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
    }
}

class TestView extends ViewGroup {
    private Paint mLoadPaint;

    public TestView(Context context) {
        super(context);
        Toast.makeText(getContext(),"HUDView_2", Toast.LENGTH_LONG).show();

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(true);
        mLoadPaint.setTextSize(25);
        mLoadPaint.setARGB(255, 255, 12, 125);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText("Hello World", 5, 15, mLoadPaint);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        Toast.makeText(getContext(),"onTouchEvent", Toast.LENGTH_LONG).show();
//        return true;
//    }
}