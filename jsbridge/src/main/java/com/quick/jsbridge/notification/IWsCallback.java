package com.quick.jsbridge.notification;

public interface IWsCallback<T> {
    void onSuccess(T t);
    void onError(String msg, Request request, Action action);
    void onTimeout(Request request, Action action);
}
