package com.dadoutek.uled.model.dbModel;

import com.dadoutek.uled.R;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DbSensor implements Serializable {

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name;
    private String controlGroupAddr;
    private String macAddr;
    private int productUUID;
    private int index;
    private Long belongGroupId;
    private String version = "";
    public int rssi =1000;
    private int openTag = 1; //1代表开 0代表关
    private int setType = 1; //0代表群组模式 1代表场景模式
    private int sceneId = 0; //场景id
    private String boundMac ="";
    public String boundMacName;
    public Boolean isChecked;
    public boolean selected;
    @Transient
    public int icon = R.drawable.icon_sensor;//灯状态显示图
    public boolean isMostNew = false;
    public boolean isSupportOta =true;
    public int belongRegionId =0;
    public int uid =0;
    public String list ="";
    public boolean isGetVersion= false;


    @Generated(hash = 1466174835)
    public DbSensor(Long id, int meshAddr, String name, String controlGroupAddr,
            String macAddr, int productUUID, int index, Long belongGroupId,
            String version, int rssi, int openTag, int setType, int sceneId,
            String boundMac, String boundMacName, Boolean isChecked,
            boolean selected, boolean isMostNew, boolean isSupportOta,
            int belongRegionId, int uid, String list, boolean isGetVersion) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.controlGroupAddr = controlGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.index = index;
        this.belongGroupId = belongGroupId;
        this.version = version;
        this.rssi = rssi;
        this.openTag = openTag;
        this.setType = setType;
        this.sceneId = sceneId;
        this.boundMac = boundMac;
        this.boundMacName = boundMacName;
        this.isChecked = isChecked;
        this.selected = selected;
        this.isMostNew = isMostNew;
        this.isSupportOta = isSupportOta;
        this.belongRegionId = belongRegionId;
        this.uid = uid;
        this.list = list;
        this.isGetVersion = isGetVersion;
    }

    @Generated(hash = 295132781)
    public DbSensor() {
    }


    public boolean isGetVersion() {
        return isGetVersion;
    }

    public void setGetVersion(boolean getVersion) {
        isGetVersion = getVersion;
    }

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
    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void updateIcon() {
        if (this.openTag == ConnectionStatus.OFFLINE.getValue()) {
            this.icon = R.drawable.icon_sensor;
        } else if (this.openTag == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_sensor_close;
        } else if (this.openTag == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_sensor;
        }
    }

    public int getOpenTag() {
        return openTag;
    }

    public void setOpenTag(int openTag) {
        this.openTag = openTag;
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
    public String getControlGroupAddr() {
        return this.controlGroupAddr;
    }
    public void setControlGroupAddr(String controlGroupAddr) {
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

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isMostNew() {
        return isMostNew;
    }

    public void setMostNew(boolean mostNew) {
        isMostNew = mostNew;
    }

    public Boolean getChecked() {
        return isChecked;
    }

    public void setChecked(Boolean checked) {
        isChecked = checked;
    }

    @Override
    public String toString() {
        return "DbSensor{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", controlGroupAddr='" + controlGroupAddr + '\'' +
                ", macAddr='" + macAddr + '\'' +
                ", productUUID=" + productUUID +
                ", index=" + index +
                ", belongGroupId=" + belongGroupId +
                ", version='" + version + '\'' +
                ", rssi=" + rssi +
                ", openTag=" + openTag +
                ", setType=" + setType +
                ", sceneId=" + sceneId +
                ", icon=" + icon +
                '}';
    }

    public int getRssi() {
        return this.rssi;
    }
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getSetType() {
        return this.setType;
    }

    public void setSetType(int setType) {
        this.setType = setType;
    }

    public int getSceneId() {
        return this.sceneId;
    }

    public void setSceneId(int sceneId) {
        this.sceneId = sceneId;
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

    public String getBoundMacName() {
        return boundMacName;
    }

    public void setBoundMacName(String boundMacName) {
        this.boundMacName = boundMacName;
    }

    public Boolean getIsChecked() {
        return this.isChecked;
    }

    public void setIsChecked(Boolean isChecked) {
        this.isChecked = isChecked;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected() {
        return this.selected;
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

    public String getList() {
        return this.list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public boolean getIsGetVersion() {
        return this.isGetVersion;
    }

    public void setIsGetVersion(boolean isGetVersion) {
        this.isGetVersion = isGetVersion;
    }
}
