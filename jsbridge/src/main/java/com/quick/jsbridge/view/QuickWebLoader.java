package com.quick.jsbridge.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.control.AutoCallbackDefined;
import com.quick.jsbridge.control.WebloaderControl;

import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import quick.com.jsbridge.R;


/**
 * Created by dailichun on 2017/12/7.
 * 如果需要自定义quick容器请继承QuickWebLoader，在布局文件中必须定义QuickFragment的容器FrameLayout控件
 */
public class QuickWebLoader extends FrmBaseActivity implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;

    public QuickFragment fragment;

    public QuickBean bean;

    private Handler mHandler;
    private Handler handler;
    private int timeCount = 5;
    private Button countButton;

    private ImageView spImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initQuickBean(savedInstanceState);
        setLayout(R.layout.quick_activity);
        addFragment(R.id.frgContent);
        requestCodeQRCodePermissions();
    }

    /**
     * 初始化页面参数
     *
     * @param savedInstanceState
     */
    public void initQuickBean(Bundle savedInstanceState) {
        if (null != savedInstanceState && savedInstanceState.containsKey("bean")) {
            bean = (QuickBean) savedInstanceState.getSerializable("bean");
        } else if (getIntent().hasExtra("bean")) {
            bean = (QuickBean) getIntent().getSerializableExtra("bean");
        }

        if (bean == null) {
            toast(getString(R.string.status_data_error));
            finish();
            return;
        }
//        floatWindow();
    }

    /**
     * 添加QuickFragment容器
     *
     * @param containerViewId FrameLayout的视图id
     */
    public void addFragment(int containerViewId) {
        //初始化页面
        fragment = QuickFragment.newInstance(bean);
        getFragmentManager().beginTransaction().add(containerViewId, fragment).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (bean != null) {
            outState.putSerializable("bean", bean);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("bean")) {
            bean = (QuickBean) savedInstanceState.getSerializable("bean");
        }
    }

    @Override
    public void onBackPressed() {
        WebloaderControl control = fragment.getWebloaderControl();
        if (control != null) {
            if (control.autoCallbackEvent.isRegist(AutoCallbackDefined.OnClickBack)) {
                control.autoCallbackEvent.onClickBack();
            } else {
                control.loadLastPage(false);
            }
        } else {
            super.onNbBack();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fragment.getQuickWebView().canGoBack()) {
                fragment.getQuickWebView().goBack();
            } else {
                SharedPreferences pref = getSharedPreferences("clickData",MODE_PRIVATE);
                long current = new Date().getTime();
                long lastTime = pref.getLong("lastClick", 0);
                if ((current - lastTime) < 3000) {
                    this.onBackPressed();
                } else {
                    SharedPreferences.Editor editor = getSharedPreferences("clickData",MODE_PRIVATE).edit();
                    editor.putLong("lastClick",current);
                    editor.commit();
                    Toast.makeText(fragment.getContext(), "Press back button again to exit program", Toast.LENGTH_SHORT).show();
                }

            }
        }
        return false;
    }

    public static void go(Context context, QuickBean bean) {
        Intent intent = new Intent(context, QuickWebLoader.class);
        intent.putExtra("bean", bean);
        context.startActivity(intent);
    }



    public static void go(Context context, String url) {
        go(context, new QuickBean(url));
    }

    private void requestCodeQRCodePermissions() {
        String [] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }

    private void initBean(String baseUrl) {
        QuickBean be = new QuickBean(baseUrl);
        be.pageStyle = -1;
        bean = be;
        addFragment(R.id.frgContent);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_QRCODE_PERMISSIONS) {
//            floatWindow();
        }
    }

    public void floatWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
//                Toast.makeText(this, "当前无权限 canDrawOverlays", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQUEST_CODE_QRCODE_PERMISSIONS);
            } else {
//                Log.i("fff"," hhh");
            }
        }
    }
}
