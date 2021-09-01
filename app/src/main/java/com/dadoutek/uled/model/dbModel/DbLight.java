package com.dadoutek.uled.model.dbModel;

import com.blankj.utilcode.util.LogUtils;
import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/14.
 */
@Entity
public class DbLight implements Serializable {
    static final long serialVersionUID = -15515456L;
    @Id(autoincrement = true)
    private Long id;
    private int meshAddr;
    private String name;
    private String groupName;
    private int brightness = 1;
    private int colorTemperature= 1;
    private String macAddr;
    private String sixMac;
    private int meshUUID;
    private int productUUID;
    private Long belongGroupId;//belongGroupId如果等于1则标识没有群组
    private int index;
    private String boundMac;

    public int color=0xffffff;//颜色
    public String version;
    public String boundMacName;

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean selected;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean hasGroup = false;//当前灯是否有被分组
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int textColor;//文字颜色

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean isSeek = true;//选择色温还是亮度

    public int status = 1;//连接状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_light_no_circle;//灯状态显示图
    public int rssi =1000;
    public boolean isSupportOta =true;
    public boolean isMostNew= false;
    public boolean isGetVersion= false;
    public int belongRegionId =0;
    public int uid =0;

    public int isEnableWhiteBright = 1;
    public int isEnableBright = 1;

    @Generated(hash = 697736828)
    public DbLight(Long id, int meshAddr, String name, String groupName,
            int brightness, int colorTemperature, String macAddr, String sixMac,
            int meshUUID, int productUUID, Long belongGroupId, int index,
            String boundMac, int color, String version, String boundMacName,
            int status, int rssi, boolean isSupportOta, boolean isMostNew,
            boolean isGetVersion, int belongRegionId, int uid,
            int isEnableWhiteBright, int isEnableBright) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.groupName = groupName;
        this.brightness = brightness;
        this.colorTemperature = colorTemperature;
        this.macAddr = macAddr;
        this.sixMac = sixMac;
        this.meshUUID = meshUUID;
        this.productUUID = productUUID;
        this.belongGroupId = belongGroupId;
        this.index = index;
        this.boundMac = boundMac;
        this.color = color;
        this.version = version;
        this.boundMacName = boundMacName;
        this.status = status;
        this.rssi = rssi;
        this.isSupportOta = isSupportOta;
        this.isMostNew = isMostNew;
        this.isGetVersion = isGetVersion;
        this.belongRegionId = belongRegionId;
        this.uid = uid;
        this.isEnableWhiteBright = isEnableWhiteBright;
        this.isEnableBright = isEnableBright;
    }

    @Generated(hash = 2075223479)
    public DbLight() {
    }

    public int getIsEnableWhiteBright() {
        return isEnableWhiteBright;
    }

    public void setIsEnableWhiteBright(int isEnableWhiteBright) {
        this.isEnableWhiteBright = isEnableWhiteBright;
    }

    public int getIsEnableBright() {
        return isEnableBright;
    }

    public void setIsEnableBright(int isEnableBright) {
        this.isEnableBright = isEnableBright;
    }

    public boolean isGetVersion() {
        return isGetVersion;
    }

    public void setGetVersion(boolean getVersion) {
        isGetVersion = getVersion;
    }

    public String getSixMac() {
        return sixMac;
    }

    public void setSixMac(String sixMac) {
        this.sixMac = sixMac;
    }

    public String getBoundMac() {
        return boundMac;
    }

    public void setBoundMac(String boundMac) {
        this.boundMac = boundMac;
    }

    public String getBoundMacName() {
        return boundMacName;
    }

    public void setBoundMacName(String boundMacName) {
        this.boundMacName = boundMacName;
    }

    public boolean isMostNew() {
        return isMostNew;
    }

    public void setMostNew(boolean mostNew) {
        isMostNew = mostNew;
    }
    public boolean isSupportOta() {
        return isSupportOta;
    }

    public void setSupportOta(boolean supportOta) {
        isSupportOta = supportOta;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMeshAddr() {
        return meshAddr;
    }

    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        if (brightness==0||brightness==1)
            LogUtils.v("zcl-----------调节开关输出错误-------");
        this.brightness = brightness;
    }

    public int getColorTemperature() {
        if (colorTemperature==0)
            colorTemperature = 1;
        return colorTemperature;
    }

    public void setColorTemperature(int colorTemperature) {
        if (colorTemperature==0)
            colorTemperature = 1;
        this.colorTemperature = colorTemperature;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public int getMeshUUID() {
        return meshUUID;
    }

    public void setMeshUUID(int meshUUID) {
        this.meshUUID = meshUUID;
    }

    public int getProductUUID() {
        return productUUID;
    }

    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }

    public Long getBelongGroupId() {
        return belongGroupId;
    }

    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public int getConnectionStatus() {
        return status;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.status = connectionStatus;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void updateIcon() {
        if (this.status == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_light_close_g;
        } else if (this.status == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_light_close_g;
        } else if (this.status == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_light_no_circle;
        }
    }

    public void updateRgbIcon(){
        if (this.status == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_rgb_close;
        } else if (this.status == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_rgb_close;
        } else if (this.status == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_rgb_no_circle;
        }
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setSeek(boolean seek) {
        this.isSeek = seek;
    }

    public boolean isSeek() {
        return isSeek;
    }

    @Override
    public String toString() {
        return "DbLight{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", groupName='" + groupName + '\'' +
                ", brightness=" + brightness +
                ", colorTemperature=" + colorTemperature +
                ", macAddr='" + macAddr + '\'' +
                ", sixMac='" + sixMac + '\'' +
                ", meshUUID=" + meshUUID +
                ", productUUID=" + productUUID +
                ", belongGroupId=" + belongGroupId +
                ", index=" + index +
                ", boundMac='" + boundMac + '\'' +
                ", color=" + color +
                ", version='" + version + '\'' +
                ", boundMacName='" + boundMacName + '\'' +
                ", selected=" + selected +
                ", hasGroup=" + hasGroup +
                ", textColor=" + textColor +
                ", isSeek=" + isSeek +
                ", status=" + status +
                ", icon=" + icon +
                ", rssi=" + rssi +
                ", isSupportOta=" + isSupportOta +
                ", isMostNew=" + isMostNew +
                ", isGetVersion=" + isGetVersion +
                ", belongRegionId=" + belongRegionId +
                ", uid=" + uid +
                ", isEnableWhiteBright=" + isEnableWhiteBright +
                ", isEnableBright=" + isEnableBright +
                '}';
    }


    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean getIsSupportOta() {
        return this.isSupportOta;
    }

    public void setIsSupportOta(boolean isSupportOta) {
        this.isSupportOta = isSupportOta;
    }

    public boolean getIsMostNew() {
        return this.isMostNew;
    }

    public void setIsMostNew(boolean isMostNew) {
        this.isMostNew = isMostNew;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getBelongRegionId() {
        return this.belongRegionId;
    }

    public void setBelongRegionId(int belongRegionId) {
        this.belongRegionId = belongRegionId;
    }

    public int getUid() {
        return this.uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public boolean getIsGetVersion() {
        return this.isGetVersion;
    }

    public void setIsGetVersion(boolean isGetVersion) {
        this.isGetVersion = isGetVersion;
    }
}
