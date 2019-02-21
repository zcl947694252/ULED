package com.dadoutek.uled.model;

import android.content.Context;

import com.dadoutek.uled.R;

import java.util.ArrayList;

public class InstallDeviceModel {
    private String deviceType;
    private String deviceDescribeTion;

    public InstallDeviceModel() {
    }

    public InstallDeviceModel(String deviceType, String deviceDescribeTion) {
        this.deviceType = deviceType;
        this.deviceDescribeTion = deviceDescribeTion;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceDescribeTion() {
        return deviceDescribeTion;
    }

    public void setDeviceDescribeTion(String deviceDescribeTion) {
        this.deviceDescribeTion = deviceDescribeTion;
    }

    @Override
    public String toString() {
        return "InstallDeviceModel{" +
                "deviceType='" + deviceType + '\'' +
                ", deviceDescribeTion='" + deviceDescribeTion + '\'' +
                '}';
    }
}
