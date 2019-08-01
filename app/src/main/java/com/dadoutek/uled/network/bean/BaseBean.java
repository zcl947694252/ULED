package com.dadoutek.uled.network.bean;

import java.io.Serializable;

/**
 * 创建者     ZCL
 * 创建时间   2019/7/25 16:38
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class BaseBean<T> implements Serializable {
    /**
     * data : null
     * errorCode : 0
     * message : add region succeed!
     * serverTime : 1564489496087
     */
    private T data;
    private int errorCode;
    private String message;
    private long serverTime;
    private boolean isSucess;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
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

    public boolean isSucess() {
        return errorCode==0;
    }
}
