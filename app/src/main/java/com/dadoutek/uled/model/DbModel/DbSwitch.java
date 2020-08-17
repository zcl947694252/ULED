package com.dadoutek.uled.model.DbModel;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

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

    public int rssi =1000;
    private String keys = "";
    @Nullable
    public String groupIds;
    @Nullable
    public String sceneIds;
    public  String controlGroupAddrs;
    public String version;
    public boolean isMostNew = false;
    private String boundMac ="";
    @Nullable
    public  String routerName;
    @Nullable
    public  long routerId = 1;
    public Boolean isChecked;

    public String getBoundMac() {
        return boundMac;
    }

    public void setBoundMac(String boundMac) {
        this.boundMac = boundMac;
    }

    public boolean isSupportOta() {
        return isSupportOta;
    }

    public void setSupportOta(boolean supportOta) {
        isSupportOta = supportOta;
    }

    public boolean isSupportOta =true;
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
    public int icon = R.drawable.icon_switch;//灯状态显示图
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int connectionStatus = 1;//链接状态
    /**
     * 是否是配置八鍵群組開關
     * 0 八鍵群組開關
     * 1八鍵場景開關
     * 2八键单调光
     */
    public int type;


    @Generated(hash = 362553851)
    public DbSwitch(Long id, int meshAddr, String name, int controlGroupAddr,
            String macAddr, int productUUID, String controlSceneId, int index,
            Long belongGroupId, int rssi, String keys, String groupIds,
            String sceneIds, String controlGroupAddrs, String version,
            boolean isMostNew, String boundMac, String routerName, long routerId,
            Boolean isChecked, boolean isSupportOta, int type) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.controlGroupAddr = controlGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.controlSceneId = controlSceneId;
        this.index = index;
        this.belongGroupId = belongGroupId;
        this.rssi = rssi;
        this.keys = keys;
        this.groupIds = groupIds;
        this.sceneIds = sceneIds;
        this.controlGroupAddrs = controlGroupAddrs;
        this.version = version;
        this.isMostNew = isMostNew;
        this.boundMac = boundMac;
        this.routerName = routerName;
        this.routerId = routerId;
        this.isChecked = isChecked;
        this.isSupportOta = isSupportOta;
        this.type = type;
    }

    @Generated(hash = 1179115222)
    public DbSwitch() {
    }


    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getControlGroupAddrs() {
        return controlGroupAddrs;
    }

    public void setControlGroupAddrs(String controlGroupAddrs) {
        this.controlGroupAddrs = controlGroupAddrs;
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
            this.icon = R.drawable.icon_switch_off;
        } else if (this.connectionStatus == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_switch;
        }
    }

    @Override
    public String toString() {
        return "DbSwitch{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", controlGroupAddr=" + controlGroupAddr +
                ", macAddr='" + macAddr + '\'' +
                ", productUUID=" + productUUID +
                ", controlSceneId='" + controlSceneId + '\'' +
                ", index=" + index +
                ", belongGroupId=" + belongGroupId +
                ", rssi=" + rssi +
                ", keys='" + keys + '\'' +
                ", groupIds='" + groupIds + '\'' +
                ", sceneIds='" + sceneIds + '\'' +
                ", controlGroupAddrs='" + controlGroupAddrs + '\'' +
                ", selected=" + selected +
                ", version='" + version + '\'' +
                ", hasGroup=" + hasGroup +
                ", textColor=" + textColor +
                ", icon=" + icon +
                ", connectionStatus=" + connectionStatus +
                ", type=" + type +
                '}';
    }

    public int getRssi() {
        return this.rssi;
    }
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
    public String getKeys() {
        return this.keys;
    }
    public void setKeys(String keys) {
        this.keys = keys;
    }
    public String getGroupIds() {
        return this.groupIds;
    }
    public void setGroupIds(String groupIds) {
        this.groupIds = groupIds;
    }
    public String getSceneIds() {
        return this.sceneIds;
    }
    public void setSceneIds(String sceneIds) {
        this.sceneIds = sceneIds;
    }
    public int getType() {
        return this.type;
    }
    public void setType(int type) {
        this.type = type;
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

    public String getRouterName() {
        return this.routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public long getRouterId() {
        return this.routerId;
    }

    public void setRouterId(long routerId) {
        this.routerId = routerId;
    }

    public Boolean getIsChecked() {
        return this.isChecked;
    }

    public void setIsChecked(Boolean isChecked) {
        this.isChecked = isChecked;
    }
}
