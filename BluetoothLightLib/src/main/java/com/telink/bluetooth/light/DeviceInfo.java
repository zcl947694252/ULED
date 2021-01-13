/*
 * Copyright (C) 2015 The Telink Bluetooth Light Project
 *
 */
package com.telink.bluetooth.light;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * 设备信息类
 */

public class DeviceInfo implements Parcelable {
    /**
     * Mac地址
     */
    public String macAddress;

    /**
     * SixByteMac地址
     */
    public String sixByteMacAddress;

    /**
     * 设备名称
     */
    public String deviceName;

    /**
     * 网络名称
     */
    public String meshName;
    /**
     * 网络地址
     */
    public int meshAddress;
    public int meshUUID;
    /**
     * 设备的产品标识符
     */
    public int productUUID;
    public int status;
    public int rssi;
    public int openTag = 1;

    /**
     * 是否是重新配置 以及 desenor的id
     */

    public int id = 100000000;
    public int isConfirm = 0;

    public byte[] longTermKey = new byte[20];
    /**
     * 设备的firmware版本
     */
    public String firmwareRevision;
    /**
     * 获取返回的数据
     */
    public int gwWifiState;
    /**
     * 获取返回的业务数据
     */
    public int gwVoipState;
    public int index = 0;
    public int uid =0;
    public int colorTemperature;
    public int color;
    public int brightness;
    public String boundMac;
    public int belongRegionId;
    public long belongGroupId;

    public DeviceInfo() {}

    protected DeviceInfo(Parcel in) {
        macAddress = in.readString();
        sixByteMacAddress = in.readString();
        deviceName = in.readString();
        meshName = in.readString();
        meshAddress = in.readInt();
        meshUUID = in.readInt();
        productUUID = in.readInt();
        status = in.readInt();
        rssi = in.readInt();
        id = in.readInt();
        isConfirm = in.readInt();
        longTermKey = in.createByteArray();
        firmwareRevision = in.readString();
        gwWifiState = in.readInt();
        gwVoipState = in.readInt();
        index = in.readInt();
        uid = in.readInt();
        colorTemperature = in.readInt();
        color = in.readInt();
        brightness = in.readInt();
        boundMac = in.readString();
        belongRegionId = in.readInt();
        belongGroupId = in.readLong();
        openTag = in.readInt();
    }

    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "macAddress='" + macAddress + '\'' +
                ", sixByteMacAddress='" + sixByteMacAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", meshName='" + meshName + '\'' +
                ", meshAddress=" + meshAddress +
                ", meshUUID=" + meshUUID +
                ", productUUID=" + productUUID +
                ", status=" + status +
                ", rssi=" + rssi +
                ", id=" + id +
                ", isConfirm=" + isConfirm +
                ", longTermKey=" + Arrays.toString(longTermKey) +
                ", firmwareRevision='" + firmwareRevision + '\'' +
                ", gwWifiState=" + gwWifiState +
                ", gwVoipState=" + gwVoipState +
                ", index=" + index +
                ", uid=" + uid +
                ", colorTemperature=" + colorTemperature +
                ", color=" + color +
                ", brightness=" + brightness +
                ", boundMac='" + boundMac + '\'' +
                ", belongRegionId=" + belongRegionId +
                ", belongGroupId=" + belongGroupId +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(macAddress);
        dest.writeString(sixByteMacAddress);
        dest.writeString(deviceName);
        dest.writeString(meshName);
        dest.writeInt(meshAddress);
        dest.writeInt(meshUUID);
        dest.writeInt(productUUID);
        dest.writeInt(status);
        dest.writeInt(rssi);
        dest.writeInt(id);
        dest.writeInt(isConfirm);
        dest.writeByteArray(longTermKey);
        dest.writeString(firmwareRevision);
        dest.writeInt(gwWifiState);
        dest.writeInt(gwVoipState);
        dest.writeInt(index);
        dest.writeInt(uid);
        dest.writeInt(colorTemperature);
        dest.writeInt(color);
        dest.writeInt(brightness);
        dest.writeString(boundMac);
        dest.writeInt(belongRegionId);
        dest.writeLong(belongGroupId);
        dest.writeInt(openTag);
    }
}
