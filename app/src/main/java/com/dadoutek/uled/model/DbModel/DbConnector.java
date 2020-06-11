package com.dadoutek.uled.model.DbModel;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

@Entity
public class DbConnector implements Serializable {
    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    private int meshAddr;
    private String name;
    private boolean open;
    private String macAddr;
    private int meshUUID;
    private int productUUID;
    private Long belongGroupId;
    private int index;
    public String groupName;
    public int color=0xffffff;//颜色

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean selected;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public String version;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean hasGroup = false;//当前灯是否有被分组
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int textColor;//文字颜色

    public int status = 1;//链接状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_light_on;//灯状态显示图
    public int rssi =1000;
    public boolean isSupportOta =true;


    @Generated(hash = 1251145309)
    public DbConnector(Long id, int meshAddr, String name, boolean open,
            String macAddr, int meshUUID, int productUUID, Long belongGroupId,
            int index, String groupName, int color, int status, int rssi,
            boolean isSupportOta) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.open = open;
        this.macAddr = macAddr;
        this.meshUUID = meshUUID;
        this.productUUID = productUUID;
        this.belongGroupId = belongGroupId;
        this.index = index;
        this.groupName = groupName;
        this.color = color;
        this.status = status;
        this.rssi = rssi;
        this.isSupportOta = isSupportOta;
    }

    @Generated(hash = 1212725637)
    public DbConnector() {
    }


    public boolean isSupportOta() {
        return isSupportOta;
    }

    public void setSupportOta(boolean supportOta) {
        isSupportOta = supportOta;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getMeshAddr() {
        return this.meshAddr;
    }
    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddr() {
        return this.macAddr;
    }
    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }
    public int getMeshUUID() {
        return this.meshUUID;
    }
    public void setMeshUUID(int meshUUID) {
        this.meshUUID = meshUUID;
    }
    public int getProductUUID() {
        return this.productUUID;
    }
    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }
    public Long getBelongGroupId() {
        return this.belongGroupId;
    }
    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
    }
    public int getIndex() {
        return this.index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public int getColor() {
        return this.color;
    }
    public void setColor(int color) {
        this.color = color;
    }
    public int getConnectionStatus() {
        return this.status;
    }
    public void setConnectionStatus(int connectionStatus) {
        this.status = connectionStatus;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }



    public void updateIcon() {

        if (this.status == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_controller;
        } else if (this.status == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_controller;
        } else if (this.status == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_controller_open;
        }
    }
    public boolean getOpen() {
        return this.open;
    }
    public void setOpen(boolean open) {
        this.open = open;
    }
    public int getStatus() {
        return this.status;
    }
    public void setStatus(int status) {
        this.status = status;
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
}
