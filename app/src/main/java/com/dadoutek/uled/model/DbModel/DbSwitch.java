package com.dadoutek.uled.model.DbModel;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class DbSwitch implements Serializable {

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name = "";
    private int controlGroupAddr;
    private String macAddr;
    private int productUUID;
    private String controlSceneId;
    private int index;
    private Long belongGroupId;


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
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_light_on;//灯状态显示图
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int connectionStatus = 1;//链接状态

    @Generated(hash = 811812800)
    public DbSwitch(Long id, int meshAddr, String name, int controlGroupAddr,
                    String macAddr, int productUUID, String controlSceneId, int index,
                    Long belongGroupId) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.controlGroupAddr = controlGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.controlSceneId = controlSceneId;
        this.index = index;
        this.belongGroupId = belongGroupId;
    }
    @Generated(hash = 1179115222)
    public DbSwitch() {
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
    public int getControlGroupAddr() {
        return this.controlGroupAddr;
    }
    public void setControlGroupAddr(int controlGroupAddr) {
        this.controlGroupAddr = controlGroupAddr;
    }
    public String getMacAddr() {
        return this.macAddr;
    }
    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }
    public int getProductUUID() {
        return this.productUUID;
    }
    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }
    public String getControlSceneId() {
        return this.controlSceneId;
    }
    public void setControlSceneId(String controlSceneId) {
        this.controlSceneId = controlSceneId;
    }
    public int getIndex() {
        return this.index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public Long getBelongGroupId() {
        return this.belongGroupId;
    }
    public void setBelongGroupId(Long belongGroupId) {
        this.belongGroupId = belongGroupId;
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

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void updateIcon() {

        if (this.connectionStatus == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_switch;
        } else if (this.connectionStatus == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_switch;
        } else if (this.connectionStatus == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_switch;
        }
    }
}
