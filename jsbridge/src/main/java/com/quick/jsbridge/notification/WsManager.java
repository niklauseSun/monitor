package com.quick.jsbridge.notification;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.view.QuickWebLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okio.Timeout;
import quick.com.jsbridge.R;


public class WsManager {

    /**
     * channel config
     */

    private String channelId = "channel_id";
    private String channelName = "监控";
    private String channelDescription = "监控的通知";

    private static WsManager mInstance;

    private final  String TAG = this.getClass().getSimpleName();

    private Context webLoader;

    /**
     * WebSocket config
     */
    private static final int FRAME_QUEUE_SIZE = 5;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final String DEF_TEST_URL = "ws://ddns.99rongle.com:7780";
    private static final String DEF_URL = DEF_TEST_URL;
    private String userToken;
    private String userName;
    private String url;

    private String lastEventId;

    private WsStatus mStatus;
    private WsListener mListener;

    private WebSocket ws;

    private Integer statusInt;

    private WsManager() {}

    public static WsManager getInstance() {
        if (mInstance == null) {
            synchronized (WsManager.class) {
                if (mInstance == null) {
                    mInstance = new WsManager();
                }
            }
        }
        return mInstance;
    }

    public void init(String token, String name, Context context) {
        userToken = token;
        userName = name;
        webLoader = context;
        try {
//            String configUrl = "";
//            SharedPreferences.Editor editor = getSharedPreferences("ipAddress", MODE_PRIVATE).edit();
//            SharedPreferences sp = SharedPreferences();
            SharedPreferences sharedPreferences = webLoader.getSharedPreferences("ipAddress", Context .MODE_PRIVATE);
            String socket = sharedPreferences.getString("socketIp", DEF_URL);
            url = TextUtils.isEmpty(socket) ? DEF_URL: socket;
            String desUrl = url + "/?" + "userToken=" + token + "&" + "userName=" + name + "&EIO=3&transport=websocket";
            Log.i("init", desUrl);
            ws = new WebSocketFactory().createSocket(desUrl, CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)
                    .setMissingCloseFrameAllowed(false)
                    .addListener(mListener = new WsListener())
                    .connectAsynchronously();// 异步连接
            setStatus(WsStatus.CONNECTING);
            Log.i(TAG, "第一次连接");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class WsListener extends WebSocketAdapter {
        @Override
        public void onTextMessage(com.neovisionaries.ws.client.WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            Log.i(TAG, text);
            parseJson(text);
        }

        @Override
        public void onConnected(com.neovisionaries.ws.client.WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            Log.i(TAG, "连接成功");
            setStatus(WsStatus.CONNECT_SUCCESS);
            cancelReconnect();//连接成功的时候取消重连,初始化连接次数
        }

        @Override
        public void onConnectError(com.neovisionaries.ws.client.WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            Log.i(TAG, "连接错误");
            exception.printStackTrace();
            setStatus(WsStatus.CONNECT_FAIL);
            reconnect();//连接错误的时候调用重连方法
        }

        @Override
        public void onDisconnected(com.neovisionaries.ws.client.WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.i(TAG, "断开连接");
            setStatus(WsStatus.CONNECT_FAIL);
            reconnect();//连接断开的时候调用重连方法
        }
    }

    private void setStatus(WsStatus status) {
        this.mStatus = status;
    }

    private WsStatus getStatus() {
        return mStatus;
    }

    private void disconnect() {
        if (ws != null) {
            ws.disconnect();
        }
    }

    private static final int REQUEST_TIMEOUT = 10000;//请求超时时间
    private AtomicLong seqId = new AtomicLong(SystemClock.uptimeMillis());//每个请求的唯一标识

    public void sendReq(Action action, Object req, ICallback callback) {
        sendReq(action, req, callback, REQUEST_TIMEOUT);
    }


    public void sendReq(Action action, Object req, ICallback callback, long timeout) {
        sendReq(action, req, callback, timeout, 1);
    }

    private final int SUCCESS_HANDLE = 0x01;
    private final int ERROR_HANDLE = 0x02;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Map<Long, CallbackWrapper> callbacks = new HashMap<>();

    @SuppressWarnings("unchecked")
    private <T> void sendReq(Action action, T req, final ICallback callback, final long timeout, int reqCount) {
        if (!isNetConnect()) {
            callback.onFail("网络不可用");
            return;
        }

        Request request = new Request.Builder<T>()
                .action(action.getAction())
                .reqEvent(action.getReqEvent())
                .seqId(seqId.getAndIncrement())
                .reqCount(reqCount)
                .req(req)
                .build();

        ScheduledFuture timeoutTask = enqueueTimeout(request.getSeqId(), timeout);//添加超时任务

        IWsCallback tempCallback = new IWsCallback() {

            @Override
            public void onSuccess(Object o) {
                mHandler.obtainMessage(SUCCESS_HANDLE, new CallbackDataWrapper(callback, o))
                        .sendToTarget();
            }


            @Override
            public void onError(String msg, Request request, Action action) {
                mHandler.obtainMessage(ERROR_HANDLE, new CallbackDataWrapper(callback, msg))
                        .sendToTarget();
            }


            @Override
            public void onTimeout(Request request, Action action) {
                timeoutHandle(request, action, callback, timeout);
            }
        };


        callbacks.put(request.getSeqId(),
                new CallbackWrapper(tempCallback, timeoutTask, action, request));

        Log.i("send text : ", new Gson().toJson(request));
        ws.sendText(new Gson().toJson(request));
    }

    /**
     * 添加超时任务
     */
    private ScheduledFuture enqueueTimeout(final long seqId, long timeout) {
        return executor.schedule(new Runnable() {
            @Override
            public void run() {
                CallbackWrapper wrapper = callbacks.remove(seqId);
                if (wrapper != null) {
//                    Logger.t(TAG).d("(action:%s)第%d次请求超时", wrapper.getAction().getAction(), wrapper.getRequest().getReqCount());
                    wrapper.getTempCallback().onTimeout(wrapper.getRequest(), wrapper.getAction());
                }
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 超时处理
     */
    private void timeoutHandle(Request request, Action action, ICallback callback, long timeout) {
        if (request.getReqCount() > 3) {
            Log.i(TAG, action.getAction() + "连续3次请求超时 执行http请求");
//            Logger.t(TAG).d("(action:%s)连续3次请求超时 执行http请求", action.getAction());
            //走http请求
        } else {
            sendReq(action, request.getReq(), callback, timeout, request.getReqCount() + 1);
            Log.i(TAG, "action:" + action.getAction() + "发起第" + request.getReqCount() + "次请求");
//            Logger.t(TAG).d("(action:%s)%d次请求", , request.getReqCount());
        }
    }

    private void doAuth() {
        sendReq(Action.LOGIN, null, new ICallback() {
            @Override
            public void onSuccess(Object o) {
                setStatus(WsStatus.AUTH_SUCCESS);
                startHeartbeat();
                delaySyncData();
            }


            @Override
            public void onFail(String msg) {

            }
        });
    }


    //同步数据
    private void delaySyncData() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendReq(Action.SYNC, null, new ICallback() {
                    @Override
                    public void onSuccess(Object o) {

                    }


                    @Override
                    public void onFail(String msg) {

                    }
                });
            }
        }, 300);
    }

    private static final long HEARTBEAT_INTERVAL = 30000;//心跳间隔


    private void startHeartbeat() {
        mHandler.postDelayed(heartbeatTask, HEARTBEAT_INTERVAL);
    }


    private void cancelHeartbeat() {
        heartbeatFailCount = 0;
        mHandler.removeCallbacks(heartbeatTask);
    }

    private int heartbeatFailCount = 0;
    private Runnable heartbeatTask = new Runnable() {
        @Override
        public void run() {
            sendReq(Action.HEARTBEAT, null, new ICallback() {
                @Override
                public void onSuccess(Object o) {
                    heartbeatFailCount = 0;
                }


                @Override
                public void onFail(String msg) {
                    heartbeatFailCount++;
                    if (heartbeatFailCount >= 3) {
                        reconnect();
                    }
                }
            });

            mHandler.postDelayed(this, HEARTBEAT_INTERVAL);
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS_HANDLE:
                    CallbackDataWrapper successWrapper = (CallbackDataWrapper) msg.obj;
                    successWrapper.getCallback().onSuccess(successWrapper.getData());
                    break;
                case ERROR_HANDLE:
                    CallbackDataWrapper errorWrapper = (CallbackDataWrapper) msg.obj;
                    errorWrapper.getCallback().onFail((String) errorWrapper.getData());
                    break;
            }
        }
    };

    private int reconnectCount = 0;//重连次数
    private long minInterval = 3000;//重连最小时间间隔
    private long maxInterval = 60000;//重连最大时间间隔

    public void reconnect() {
        String desUrl = url + "/?" + "userToken=" + userToken + "&" + "userName=" + userName + "&EIO=3&transport=websocket";
        if (!isNetConnect()) {
            reconnectCount = 0;
            Log.i(TAG,"重连失败网络不可用");
            return;
        }

        //这里其实应该还有个用户是否登录了的判断 因为当连接成功后我们需要发送用户信息到服务端进行校验
        //由于我们这里是个demo所以省略了
        if (ws != null &&
                !ws.isOpen() &&//当前连接断开了
                getStatus() != WsStatus.CONNECTING) {//不是正在重连状态

            reconnectCount++;
            setStatus(WsStatus.CONNECTING);
            cancelHeartbeat();//取消心跳

            long reconnectTime = minInterval;
            if (reconnectCount > 3) {
                long temp = minInterval * (reconnectCount - 2);
                reconnectTime = temp > maxInterval ? maxInterval : temp;
            }

            Log.i(TAG, "准备开始第" + reconnectCount + "次重连,重连间隔" + reconnectTime + " -- url:" + desUrl);
            mHandler.postDelayed(mReconnectTask, reconnectTime);
        }
    }

    private Runnable mReconnectTask = new Runnable() {

        @Override
        public void run() {
            try {
                String desUrl = url + "/?" + "userToken=" + userToken + "&" + "userName=" + userName + "&EIO=3&transport=websocket";
                ws = new WebSocketFactory().createSocket(desUrl, CONNECT_TIMEOUT)
                        .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                        .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                        .addListener(mListener = new WsListener())//添加回调监听
                        .connectAsynchronously();//异步连接
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    private void cancelReconnect() {
        reconnectCount = 0;
        mHandler.removeCallbacks(mReconnectTask);
    }

    private boolean isNetConnect() {
        ConnectivityManager connectivity = (ConnectivityManager) webLoader.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    private void parseJson(String text) {

        if (!text.contains("beginTime")) return;

        if (text.isEmpty()) return;

        if (text.contains("[")) {
            Integer startIndex = text.indexOf("[");
            String status = text.substring(0, startIndex);
            String jsonString = text.substring(startIndex);

            Log.i("parseJson", status);
            Log.i("parseJson", jsonString);
            try {
                JSONArray array = new JSONArray(jsonString);
                String event = array.getString(0);
                Log.i("event", event);
                String objString = array.getString(1);
                Log.i("objString", objString);
                if (event.equals("push_event")) {
                    JSONObject obj = new JSONObject(objString);
                    Log.i("obj", obj.toString());
                    String eventId = obj.getString("eventId");

                    if (eventId.equals(lastEventId)) return;
                    lastEventId = eventId;
                    String eventType = obj.getString("eventType");
                    String showEventInfo = obj.getString("showEventInfo");

                    createNotificationChannel(eventType, showEventInfo, "https://paasapp.traefik.99rongle.com/?" + "event_id=" + eventId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            statusInt = Integer.parseInt(text);
        }
    }

    private void sendToNotification() {
        String title= "通知标题";
        String content = "通知内容";
        String url = "https://www.baidu.com?event_id=34A51AC8-7F61-214F-A298-42C0B4B7C689";
        createNotificationChannel(title, content, url);
    }

    private void createNotificationChannel(String title, String content, String url) {

//        String title = "通知标题" ;
//        String content = "通知内容" ;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(webLoader.getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(content)
                .setVibrate(new long[]{100,200,300,400,500,400,300,200,400})
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channelName;
            String description = channelDescription;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,400});
            channel.enableVibration(true);
            channel.setDescription(description);
            channel.setBypassDnd(true);//设置是否绕过免打扰模式
            channel.setShowBadge(false);

            Intent notifyIntent = new Intent(webLoader.getApplicationContext(), QuickWebLoader.class);
            QuickBean bean = new QuickBean(url);
            notifyIntent.putExtra("bean", bean);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                    webLoader, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
            );

            builder.setContentIntent(notifyPendingIntent);
            NotificationManager notificationManager = webLoader.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Integer index = getCurrentIndex();
            // 发送消息
            notificationManager.notify(index, builder.build());
            saveCurrentBadge(index + 1);
        }
    }

    private int getCurrentIndex() {
        SharedPreferences sharedPreferences = webLoader.getSharedPreferences("badge", Context .MODE_PRIVATE);
        Integer index = sharedPreferences.getInt("badgeIndex", 0);
        return index;
    }

    private void saveCurrentBadge(Integer index) {
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= webLoader.getSharedPreferences("badge",Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("badgeIndex", index);
        //步骤4：提交
        editor.commit();
    }

    public void sendNotification() {
        Log.i("sendNotificationInfo", "11");
        NotificationManager nm = (NotificationManager) webLoader.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = "通知标题" ;
        String content = "通知内容" ;

        //1.实例化一个通知，指定图标、概要、时间

        //2.指定通知的标题、内容和intent

        Intent intent = new Intent(webLoader.getApplicationContext(), QuickWebLoader.class);

        PendingIntent pi= PendingIntent.getActivity(webLoader, 0, intent, 0);

        Notification n = new Notification.Builder(webLoader)
                .setContentTitle("AAA")
//                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("AAAAAAAAAAAAAAAA")
                .setWhen(5000)
                .setContentIntent(pi)
                .build();

        //3.指定声音
        n.defaults = Notification.DEFAULT_SOUND;

        //4.发送通知
        nm.notify(1, n);
    }

    public enum WsStatus {
        CONNECT_SUCCESS, // 连接成功
        CONNECT_FAIL, // 连接失败
        CONNECTING,
        AUTH_SUCCESS
    }

}

