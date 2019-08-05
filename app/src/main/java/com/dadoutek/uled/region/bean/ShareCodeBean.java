package com.dadoutek.uled.region.bean;

/**
 * 创建者     ZCL
 * 创建时间   2019/8/2 14:30
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class ShareCodeBean {

    /**
     * data : {"expire":86400,"type":1,"code":"dadoueyJhbGciOiJIUzI1NiJ9.eyJyZWdpb25faWQiOjEsImF1dGhvcml6ZXJfaWQiOjMwMDUxNCwibGV2ZWwiOjF9.IaT-nXYdttXXqkAoK1960L-du05qLmdf71OhSXenDvosmartlight"}
     * expire : 86400
     * type : 1
     * code : dadoueyJhbGciOiJIUzI1NiJ9.eyJyZWdpb25faWQiOjEsImF1dGhvcml6ZXJfaWQiOjMwMDUxNCwibGV2ZWwiOjF9.IaT-nXYdttXXqkAoK1960L-du05qLmdf71OhSXenDvosmartlight
     */
    private int expire;
    private int type;
    private String code;

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
