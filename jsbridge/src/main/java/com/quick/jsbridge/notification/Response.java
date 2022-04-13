package com.quick.jsbridge.notification;

import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("resp_event")
    private int respEvent;

    @SerializedName("seq_id")
    private String seqId;

    private String action;

    public int getRespEvent() {
        return respEvent;
    }

    public void setRespEvent(int respEvent) {
        this.respEvent = respEvent;
    }

    public String getSeqId() {
        return seqId;
    }

    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResp() {
        return resp;
    }

    public void setResp(String resp) {
        this.resp = resp;
    }

    private String resp;

    //省略get set方法
}
