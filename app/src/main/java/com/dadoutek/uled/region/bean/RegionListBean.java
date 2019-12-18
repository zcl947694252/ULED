package com.dadoutek.uled.region.bean;

import java.util.List;

/**
 * 创建者     ZCL
 * 创建时间   2019/12/17 16:28
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class RegionListBean {
    /**
     * data : ["e8e54634354663b2","9df2a4c38f390319"]
     * errorCode : 0
     * message : get region list succeed!
     * serverTime : 1576637639145
     */

    private int errorCode;
    private String message;
    private long serverTime;
    private List<String> data;

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

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RegionListBean{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", serverTime=" + serverTime +
                ", data=" + data +
                '}';
    }
}
