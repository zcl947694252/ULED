package com.dadoutek.uled.switches.bean;

import java.io.Serializable;

/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 17:30
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class KeysBean implements Serializable {

    /**
     * keyId : 0
     * featureId : 28
     * reserveValue_A : 0
     * reserveValue_B : 0
     */

    private int keyId;
    private int featureId;
    private int reserveValue_A;
    private int reserveValue_B;
    private String name;

    public int getKeyId() {
        return keyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public int getFeatureId() {
        return featureId;
    }

    public void setFeatureId(int featureId) {
        this.featureId = featureId;
    }

    public int getReserveValue_A() {
        return reserveValue_A;
    }

    public void setReserveValue_A(int reserveValue_A) {
        this.reserveValue_A = reserveValue_A;
    }

    public int getReserveValue_B() {
        return reserveValue_B;
    }

    public void setReserveValue_B(int reserveValue_B) {
        this.reserveValue_B = reserveValue_B;
    }
}
