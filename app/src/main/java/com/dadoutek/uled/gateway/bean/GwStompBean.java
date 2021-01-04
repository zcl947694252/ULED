package com.dadoutek.uled.gateway.bean;

import java.io.Serializable;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/31 16:56
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GwStompBean implements Serializable {

    /**
     * cmd : 2000
     * msg : 标签下发成功!
     * macAddr : aabbcceeddff
     * ts : 1564718475857
     * status : 0
     * ser_id : 1
     * extend : null
     */

    private int cmd;
    private String msg;
    private String macAddr;
    private String ts;
    private int status;
    private String ser_id;
    private Object extend;

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSer_id() {
        return ser_id;
    }

    public void setSer_id(String ser_id) {
        this.ser_id = ser_id;
    }

    public Object getExtend() {
        return extend;
    }

    public void setExtend(Object extend) {
        this.extend = extend;
    }

    @Override
    public String toString() {
        return "GwStompBean{" +
                "cmd=" + cmd +
                ", msg='" + msg + '\'' +
                ", macAddr='" + macAddr + '\'' +
                ", ts='" + ts + '\'' +
                ", status=" + status +
                ", ser_id='" + ser_id + '\'' +
                ", extend=" + extend +
                '}';
    }
}
