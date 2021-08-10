package com.dadoutek.uled.model.dbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hejiajun on 2018/5/7.
 */

@Entity
public class DbSceneActions {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private long belongSceneId;
    @NotNull
    private int groupAddr;
    @NotNull
    private int colorTemperature;
    @NotNull
    private int brightness;
    public int color=855638015;//颜色;

    private boolean isOn = true;
    private boolean isEnableBright = true;
    private boolean isEnableWhiteBright = true;

    private int deviceType;
    private int circleOne;
    private int circleTwo;
    private int circleThree;
    private int circleFour;

    public int rgbType = 0   ;   //rgb 类型 0:颜色模式 1：渐变模式
    public int gradientType = 0 ;//渐变类型 1：自定义渐变  2：内置渐变
    public int gradientId = 0  ; //渐变id
    public int gradientSpeed = 0 ;//渐变速度
    public String gradientName  ;//渐变速度
    public int curtainOnOffRange = 100; //窗帘幅度，chown


    @Generated(hash = 192241967)
    public DbSceneActions(Long id, long belongSceneId, int groupAddr,
            int colorTemperature, int brightness, int color, boolean isOn,
            boolean isEnableBright, boolean isEnableWhiteBright, int deviceType,
            int circleOne, int circleTwo, int circleThree, int circleFour,
            int rgbType, int gradientType, int gradientId, int gradientSpeed,
            String gradientName, int curtainOnOffRange) {
        this.id = id;
        this.belongSceneId = belongSceneId;
        this.groupAddr = groupAddr;
        this.colorTemperature = colorTemperature;
        this.brightness = brightness;
        this.color = color;
        this.isOn = isOn;
        this.isEnableBright = isEnableBright;
        this.isEnableWhiteBright = isEnableWhiteBright;
        this.deviceType = deviceType;
        this.circleOne = circleOne;
        this.circleTwo = circleTwo;
        this.circleThree = circleThree;
        this.circleFour = circleFour;
        this.rgbType = rgbType;
        this.gradientType = gradientType;
        this.gradientId = gradientId;
        this.gradientSpeed = gradientSpeed;
        this.gradientName = gradientName;
        this.curtainOnOffRange = curtainOnOffRange;
    }

    @Generated(hash = 885919265)
    public DbSceneActions() {
    }


    public int getRgbType() {
        return rgbType;
    }

    public void setRgbType(int rgbType) {
        this.rgbType = rgbType;
    }

    public int getGradientType() {
        return gradientType;
    }

    public void setGradientType(int gradientType) {
        this.gradientType = gradientType;
    }

    public int getGradientId() {
        return gradientId;
    }

    public void setGradientId(int gradientId) {
        this.gradientId = gradientId;
    }

    public int getGradientSpeed() {
        return gradientSpeed;
    }

    public void setGradientSpeed(int gradientSpeed) {
        this.gradientSpeed = gradientSpeed;
    }

    public String getGradientName() {
        return gradientName;
    }

    public void setGradientName(String gradientName) {
        this.gradientName = gradientName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getBelongSceneId() {
        return belongSceneId;
    }

    public void setBelongSceneId(long belongSceneId) {
        this.belongSceneId = belongSceneId;
    }

    public int getGroupAddr() {
        return groupAddr;
    }

    public void setGroupAddr(int groupAddr) {
        this.groupAddr = groupAddr;
    }

    public int getColorTemperature() {
        return colorTemperature;
    }

    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
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

    public boolean getIsOn() {
        return this.isOn;
    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getCircleOne() {
        return this.circleOne;
    }

    public void setCircleOne(int circleOne) {
        this.circleOne = circleOne;
    }

    public int getCircleTwo() {
        return this.circleTwo;
    }

    public void setCircleTwo(int circleTwo) {
        this.circleTwo = circleTwo;
    }

    public int getCircleThree() {
        return this.circleThree;
    }

    public void setCircleThree(int circleThree) {
        this.circleThree = circleThree;
    }

    public int getCircleFour() {
        return this.circleFour;
    }

    public void setCircleFour(int circleFour) {
        this.circleFour = circleFour;
    }

    public boolean getIsEnableBright() {
        return this.isEnableBright;
    }

    public void setIsEnableBright(boolean isEnableBright) {
        this.isEnableBright = isEnableBright;
    }

    public boolean getIsEnableWhiteBright() {
        return this.isEnableWhiteBright;
    }

    public void setIsEnableWhiteBright(boolean isEnableWhiteBright) {
        this.isEnableWhiteBright = isEnableWhiteBright;
    }

    @Override
    public String toString() {
        return "DbSceneActions{" +
                "id=" + id +
                ", belongSceneId=" + belongSceneId +
                ", groupAddr=" + groupAddr +
                ", colorTemperature=" + colorTemperature +
                ", brightness=" + brightness +
                ", color=" + color +
                ", isOn=" + isOn +
                ", isEnableBright=" + isEnableBright +
                ", isEnableWhiteBright=" + isEnableWhiteBright +
                ", deviceType=" + deviceType +
                ", circleOne=" + circleOne +
                ", circleTwo=" + circleTwo +
                ", circleThree=" + circleThree +
                ", circleFour=" + circleFour +
                ", rgbType=" + rgbType +
                ", gradientType=" + gradientType +
                ", gradientId=" + gradientId +
                ", gradientSpeed=" + gradientSpeed +
                ", gradientName='" + gradientName + '\'' +
                ", curtainOnOffRange" + curtainOnOffRange +
                '}';
    }

    public int getCurtainOnOffRange() {
        return this.curtainOnOffRange;
    }

    public void setCurtainOnOffRange(int curtainOnOffRange) {
        this.curtainOnOffRange = curtainOnOffRange;
    }

}
