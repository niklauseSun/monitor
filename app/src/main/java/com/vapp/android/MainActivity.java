package com.vapp.android;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.view.FloatingService;
import com.quick.jsbridge.view.QuickWebLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends FrmBaseActivity {

    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;

    private Button inputButton = null;
    private Button defaultButton = null;
    private Button prevButton = null;
    private Button scanButton = null;
    private Button callTest = null;
    private Button goToMessageButton = null;
    private Button selectImageButton = null;
    private Button jumpToShowVr = null;
    private ImageView showImage = null;

    private Context mContext = this;

    private String ChannleId = "channelId";

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private static String prevUrlKey = "prevUrl";
    private static String defaultUrl = "http://10.12.254.231:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        SharedPreferences sp = getSharedPreferences("ipAddress", MODE_PRIVATE);
        String localUrl = sp.getString("localIp", "");
        Log.i("loadUrl", localUrl);
//        requestCodeQRCodePermissions();

//        Toast.makeText(mContext, "加载中", Toast.LENGTH_SHORT).show();
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Boolean isUrl = checkUrl(localUrl, 30 *1000);

                if (isUrl) {
                    nomalInit(localUrl);
                } else {
                    nomalInit("file:///android_asset/web/index.html");
                }
            }
        }, 3000);



    }
    private void nomalInit(String url) {

        Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);

        QuickBean bean = new QuickBean(url);
        bean.pageStyle = -1;
        mintent.putExtra("bean", bean);
        mintent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        startActivity(mintent);
        this.finish();
    }

    public static Boolean checkUrl(String address, int waitMilliSecond) {
        try {
            URL url = new URL(address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(waitMilliSecond);
            connection.setReadTimeout(waitMilliSecond);
                    try {
                        connection.connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            int code = connection.getResponseCode();
            if ((code >= 100) && (code < 400)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void compareUrl(String newUrl) {
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context .MODE_PRIVATE);
        String oldUrl = sharedPreferences.getString("url","https://m.mspace.com.sg/mobile/pages/client/home");
        if (!newUrl.equals(oldUrl)) {
            nomalInit(newUrl);

            //步骤1：创建一个SharedPreferences对象
            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
            //步骤2： 实例化SharedPreferences.Editor对象
            SharedPreferences.Editor editor = share.edit();
            //步骤3：将获取过来的值放入文件
            editor.putString("baseReqUrl", newUrl);
            //步骤4：提交
            editor.commit();
        } else {
            nomalInit(newUrl);
        }
    }

    private void testInit() {
        setContentView(R.layout.activity_main);

        requestCodeQRCodePermissions();

        inputButton = findViewById(R.id.inputButton);
        defaultButton = findViewById(R.id.defaultButton);
        prevButton = findViewById(R.id.prevButton);
        scanButton = findViewById(R.id.scan_button);
        selectImageButton = findViewById(R.id.selectImage);
        showImage = findViewById(R.id.showImage);

        inputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNotificationChannel();
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getPrveUrl();
                if (!url.isEmpty()) {
                    jumpToWebView(mContext, url);
                } else {
                    Toast.makeText(mContext, "未输入过网址！",Toast.LENGTH_SHORT).show();
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);
                QuickBean bean = new QuickBean("https://www.baidu.com");
                mintent.putExtra("bean", bean);
                startActivity(mintent);
            }
        });

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //单选
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(true)  //设置是否单选
                        .canPreview(true) //是否可以预览图片，默认为true
                        .start(getActivity(), ImageSelector.RESULT_CODE); // 打开相册

            }
        });

//        jumpToShowVr.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//            }
//        });
    }

    private void createNotificationChannel() {

//        NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = "通知标题" ;
        String content = "通知内容" ;

        Intent fullScreenIntent = new Intent(this, SplashActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        File soundFile = new File("https://studio.xiaowai.co/blockly/media/skins/bounce/1_goal.mp3");
        Uri uri = Uri.fromFile(soundFile);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ChannleId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(content)
//                .setSound(uri)
                .setVibrate(new long[]{100,200,300,400,500,400,300,200,400})
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ChannleId, name, importance);
            AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,400});
            channel.enableVibration(true);
            channel.setDescription(description);
            channel.setBypassDnd(true);//设置是否绕过免打扰模式

            // 点击跳转
            Intent notifyIntent = new Intent(this, QuickWebLoader.class);
            // Set the Activity to start in a new, empty task

            QuickBean bean = new QuickBean("https://www.baidu.com?event_id=34A51AC8-7F61-214F-A298-42C0B4B7C689");
            notifyIntent.putExtra("bean", bean);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Create the PendingIntent
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                    this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
            );

            builder.setContentIntent(notifyPendingIntent);


            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

//            NotificationCompat notificationManager = NotificationManagerCompat.from(this);



            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    notificationManager.notify(0, builder.build());
                }
            }, 5000);

        }
    }
    private void showInputDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(getContext());
        builder.setTitle("输入网址")
                .setPlaceholder("在此输入您要跳转的网址")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                    }
                })
                .addAction("确定", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        CharSequence text = builder.getEditText().getText();
                        if (isUrl(text.toString())) {
                            // 如果是URL 需要将这个保存下来并跳转

                            saveUrl(text.toString());
                            Log.d("MainActivity", text.toString());
                            jumpToWebView(mContext, text.toString());
                        } else {
                            // 提示不是
                            Toast.makeText(mContext, "请输入正确的地址！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        builder.show();
    }

    private void saveUrl(String url) {
        // 步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("data",Context.MODE_PRIVATE);
        // 步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // 步骤3：将获取过来的值放入文件
        editor.putString(prevUrlKey, url);
        // 提交
        editor.commit();
    }

    private String getPrveUrl() {
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("data",Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        String url = sharedPreferences.getString(prevUrlKey, "");
        return url;
    }

    private void jumpToWebView(Context context, String url) {
        Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);
        QuickBean bean = new QuickBean(url);
        mintent.putExtra("bean", bean);
        startActivity(mintent);
        requestCodeQRCodePermissions();
    }

    private static String pattern = "^([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$";

    /**
     * 判断 url 是否合法
     */
    public static boolean isUrl(String url) {
        Pattern httpPattern = Pattern.compile(pattern);
        if (httpPattern.matcher(url).matches()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void requestCodeQRCodePermissions() {
        String [] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImageSelector.RESULT_CODE) {
            //选择或预览图片回传值
            ArrayList<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                Uri uri = Uri.parse(photos.get(0));
                showImage.setImageURI(uri);
            }
        }

//        if (requestCode == REQUEST_CODE_QRCODE_PERMISSIONS) {
//            floatWindow();
//        }
    }

    private void requestBaseUrl() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                // Do network action in this function
//                String url = "https://jiance.99rongle.com/prod-api/mate-component/config/get-h5-url";
                String url = "https://console.mspace.com.sg/prod-api/mate-system/dict/list-value?code=appconf";
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                        .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                        .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                        .build();
                Request request = new Request.Builder().url(url).get().build();
                Log.i("test", "requestBaseUrl");
                try (Response response= client.newCall(request).execute()) {
                    JSONObject obj = new JSONObject(response.body().string());
                    Log.i("test", obj.toString());
                    JSONArray array = obj.optJSONArray("data");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject st = array.getJSONObject(i);
                        String dictKey = st.optString("dictKey");
                        String dictValue = st.optString("dictValue");

                        if (dictKey.equals("home")) {
                            compareUrl(dictValue);
                        } else if (dictKey.equals("guide")) {
                            // 更改guide
                            //步骤1：创建一个SharedPreferences对象
                            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
                            //步骤2： 实例化SharedPreferences.Editor对象
                            SharedPreferences.Editor editor = share.edit();
                            //步骤3：将获取过来的值放入文件
                            editor.putString("guideImage", dictValue);
                            //步骤4：提交
                            editor.commit();
                        }

                        Log.i("key", dictKey);
                        Log.i("value", dictValue);
                    }
                } catch (Exception e) {
                    Log.i("test", "test");
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
