package com.quick.jsbridge.view;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class FloatingService extends Service {
    private WindowManager.LayoutParams layoutParams;
    private WindowManager manager;
    private Button btn;
    private Boolean isMoved = false;
    private Handler mHandler = new Handler();
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingView();
        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingView() {
        Log.i("showFloatView", "ff");

        if (btn == null) {
            btn = new Button(getApplicationContext());
            btn.setText("IP");
            btn.setBackgroundColor(Color.WHITE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isMoved) {
                        Intent intent = new Intent(getApplicationContext(), UpdateIP.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                }
            });

            if (Settings.canDrawOverlays(getApplicationContext())) {


                // WindowManager 对象
                manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                // 创建布局参数
                layoutParams = new WindowManager.LayoutParams();
                // 设置参数
                layoutParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
                layoutParams.format = PixelFormat.RGBA_8888;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                layoutParams.alpha = 0.6f;

                layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

                //窗口的左上角坐标
                layoutParams.x = 0;
                layoutParams.y = 0;
                //设置窗口的宽高,这里为自动
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                btn.setOnTouchListener(new FloatingOnTouchListener());

                Log.i("add button", "success");
                // 添加进WindowManager
                manager.addView(btn, layoutParams);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) motionEvent.getRawX();
                    y = (int) motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:

                    int nowX = (int) motionEvent.getRawX();
                    int nowY = (int) motionEvent.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    if (Math.abs(movedX) > 0 || Math.abs(movedY) > 0) {
                        isMoved = true;
                    } else {

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isMoved = false;
                            }
                        }, 500);

                    }
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;

                    manager.updateViewLayout(view, layoutParams);
                    break;

                default:
                    break;

            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler = null;
    }
}

