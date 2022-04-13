package com.vapp.android;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.quick.jsbridge.notification.WsManager;

public class MainApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        initAppStatusListener();

    }

    private void initAppStatusListener() {
        ForegroundCallbacks.init(this).addListener(new ForegroundCallbacks.Listener() {
            @Override
            public void onBecameForeground() {
                Log.i("WsManager", "应用回到前台调用重连方法");
                WsManager.getInstance().reconnect();
            }

            @Override
            public void onBecameBackground() {

            }
        });
    }

    public static Context getContext() {
        return context;
    }

}
