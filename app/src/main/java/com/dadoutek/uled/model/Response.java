package com.dadoutek.uled.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by hejiajun on 2018/5/15.
 */

public class Response<T> {
    @SerializedName("data")
    T t;
    @SerializedName("errorCode")
    private int errorCode;       //status code
    @SerializedName("message")
    private String message;     //received tips message.
    @SerializedName("serverTime")
    private long serverTime;

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    @Override
    public String toString() {
        return "Response{" +
                "t=" + t +
                ", errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", serverTime=" + serverTime +
                '}';
    }
}
