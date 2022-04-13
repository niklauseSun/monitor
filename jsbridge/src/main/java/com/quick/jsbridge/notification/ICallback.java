package com.quick.jsbridge.notification;

public interface ICallback<T> {

    void onSuccess(T t);

    void onFail(String msg);
}
