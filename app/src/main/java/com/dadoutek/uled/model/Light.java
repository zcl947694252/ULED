package com.dadoutek.uled.model;

import com.dadoutek.uled.R;
import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.DeviceInfo;

import java.io.Serializable;
import java.util.ArrayList;

public final class Light implements Serializable, Cloneable {

    public String name;//设备名
    public String macAddress;//蓝牙地址
    public int meshAddress;//mesh地址
    public int brightness;//亮度
    public int color;//颜色
    public int temperature;//温度
    public ConnectionStatus status;//链接状态
    public DeviceInfo raw;//设备信息
    public boolean selected;//选择状态？
    public int textColor;//文字颜色
    public int icon = R.drawable.icon_light_on;//灯状态显示图
    public String version;
    public boolean isLamp = true;
    public boolean hasGroup = false;//当前灯是否有被分组
    public ArrayList<String> belongGroups = new ArrayList<>();//所属分组的Mesh地址,每个灯最多属于8个分组

    public ArrayList<String> getBelongGroups() {
        return belongGroups;
    }

    public void setBelongGroups(ArrayList<String> belongGroups) {
        this.belongGroups = belongGroups;
    }

    public String getLabel() {
        return Integer.toString(this.meshAddress, 16) + ":" + this.brightness;
    }

    public String getLabel1() {
        return "bulb-" + Integer.toString(this.meshAddress, 16);
    }

    public String getLabel2() {
        return Integer.toString(this.meshAddress, 16);
    }

    public void updateIcon() {

        if (this.status == ConnectionStatus.OFFLINE) {
            this.icon = R.drawable.icon_light_offline;
        } else if (this.status == ConnectionStatus.OFF) {
            this.icon = R.drawable.icon_light_off;
        } else if (this.status == ConnectionStatus.ON) {
            this.icon = R.drawable.icon_light_on;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getMeshAddress() {
        return meshAddress;
    }

    public void setMeshAddress(int meshAddress) {
        this.meshAddress = meshAddress;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public DeviceInfo getRaw() {
        return raw;
    }

    public void setRaw(DeviceInfo raw) {
        this.raw = raw;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isLamp() {
        return isLamp;
    }

    public void setLamp(boolean lamp) {
        isLamp = lamp;
    }

    public boolean isHasGroup() {
        return hasGroup;
    }

    public void setHasGroup(boolean hasGroup) {
        this.hasGroup = hasGroup;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
}
