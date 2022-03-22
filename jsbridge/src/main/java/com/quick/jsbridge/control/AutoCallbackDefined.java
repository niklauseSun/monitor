package com.quick.jsbridge.control;

import java.util.Map;

/**
 * Created by dailichun on 2017/12/6.
 * 按照注册回调事件方式可分为自动注册事件以及主动注册事件。
 * 自动注册事件在调用对应API开启事件时自动注册对应回调事件，并在响应事件时主动回调传值；
 * 主动注册事件需要调用{@link }进行事件注册，
 * 在响应事件时主动回调传值，如果需要取消注册可调用{@link }；
 */

public interface AutoCallbackDefined {

    //下拉刷新时主动回调
    String OnSwipeRefresh = "OnSwipeRefresh";

    void onSwipeRefresh();

    //获取上一个页面关闭后回传值时主动回调
    String OnPageResult = "OnPageResult";

    void onPageResult(Map<String, Object> object);

    //页面重新可见时主动回调
    String OnPageResume = "OnPageResume";

    void onPageResume();

    //页面不可见时主动回调
    String OnPagePause = "OnPagePause";

    void onPagePause();

    //导航栏返回按钮
    String OnClickNbBack = "OnClickNbBack";

    void onClickNbBack();

    //导航栏左侧按钮(非返回按钮)
    String OnClickNbLeft = "OnClickNbLeft";

    void onClickNbLeft();

    //导航栏标题按钮
    String OnClickNbTitle = "OnClickNbTitle";

    void onClickNbTitle(int which);

    //导航栏最右侧按钮
    String OnClickNbRight = "OnClickNbRight";

    void onClickNbRight(int which);

    //系统返回按钮（物理返回键）
    String OnClickBack = "OnClickBack";

    void onClickBack();

    //导航栏搜索
    String OnSearch = "OnSearch";

    void onSearch(Map<String, Object> object);

    //导航栏title切换
    String OnTitleChanged = "OnTitleChanged";

    void onTitleChanged(Map<String, Object> object);

    //扫描二维码
    String OnScanCode = "OnScanCode";

    void onScanCode(Map<String, Object> object);

    //选择或者预览图片,包括拍照、选择相册
    String OnChoosePic = "OnChoosePic";

    void onChoosePic(Map<String, Object> object);

    //选择文件
    String OnChooseFile = "OnChooseFile";

    void onChooseFile(Map<String, Object> object);

    //网络状态改变
    String OnNetChanged = "OnNetChanged";

    void onNetChanged(Map<String, Object> object);

    //选择通讯录回调
    String OnChooseContact= "OnChooseContact";

    void onChooseContact(Map<String, Object> object);


    // 加入channel回调
    String onJoinChannel = "onJoinChannel";

    void onJoinChannel(Map<String, Object> object);

    String onLeaveChannel = "onLeaveChannel";

    void onLeaveChannel(Map<String, Object> object);

    String onReceiveFromGroup = "onReceiveFromGroup";

    void onReceiveFromGroup(Map<String, Object> object);

    String onReceiveFromPeer = "onReceiveFromPeer";

    void onReceiveFromPeer(Map<String, Object> object);

    // 初始化成功
    String onInitRTMSuccess = "onInitRTMSuccess";

    void onInitRTMSuccess(Map<String, Object> object);

    // 登录成功
    String onLoginSuccess = "onLoginSuccess";

    void onLoginSuccess(Map<String, Object> object);

    // 登出成功
    String onLogoutSuccess = "onLogoutSuccess";

    void onLogoutSuccess(Map<String, Object> object);

    // 发送消息
    String onSendPeerMessage = "onSendPeerMessage";

    void onSendPeerMessage(Map<String, Object> object);

    // 发送群组消息
    String onSendGroupMessage = "onSendGroupMessage";

    void onSendGroupMessage(Map<String, Object> object);

    // 监听call user 进入离开
    String onCallUserState = "onCallUserState";

    void onCallUserState(Map<String, Object> object);

    // 监听call 静音
    String onCallUserMute = "onCallUserMute";
    void onCallUserMute(Map<String, Object> object);

    String onChannelJoinChanged = "onChannelJoinChanged";
    void onChannelJoinChanged(Map<String, Object> object);

    String onInitCall = "onInitCall";
    void onInitCall(Map<String, Object> object);

    String onJoinCallChannel = "onJoinCallChannel";
    void onJoinCallChannel(Map<String, Object> object);

    String onLeaveCallChannel = "onLeaveCallChannel";
    void onLeaveCallChannel(Map<String, Object> object);

    String onUploadSuccess = "onUploadSuccess";
    void onUploadSuccess(Map<String, Object> object);
}
