package com.dadoutek.uled.model.dbModel;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DbCurtain implements Serializable {

    static final long serialVersionUID = -15515456L;

    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int meshAddr;
    private String name;
    private int belongGroupAddr;
    private String macAddr;
    private int productUUID;
    private int status;
    private boolean inverse;
    private boolean closePull;
    private int speed;
    private boolean closeSlowStart;
    private int index;
    private Long belongGroupId;
    public String groupName;
    public String version;
    private String boundMac ="";
    public String boundMacName;
    @Expose(serialize = false, deserialize = false)//:序列化和反序列化时都不生效,即序列化和反序列化都忽略,和不加 @Expose 注解效果一样
    @Transient//标识不存入数据库
    public boolean selected;//选择状态
    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean hasGroup = false;//当前灯是否有被分组
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int textColor;//文字颜色
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_curtain_device;//灯状态显示图
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int connectionStatus = 1;//链接状态
    public int rssi =1000;
    public boolean isSupportOta =true;
    public boolean isMostNew = false;
    public int belongRegionId =0;
    public int uid =0;

    @Generated(hash = 1315904035)
    public DbCurtain(Long id, int meshAddr, String name, int belongGroupAddr, String macAddr,
            int productUUID, int status, boolean inverse, boolean closePull, int speed,
            boolean closeSlowStart, int index, Long belongGroupId, String groupName, String version,
            String boundMac, String boundMacName, int rssi, boolean isSupportOta, boolean isMostNew,
            int belongRegionId, int uid) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.belongGroupAddr = belongGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.status = status;
        this.inverse = inverse;
        this.closePull = closePull;
        this.speed = speed;
        this.closeSlowStart = closeSlowStart;
        this.index = index;
        this.belongGroupId = belongGroupId;
        this.groupName = groupName;
        this.version = version;
        this.boundMac = boundMac;
        this.boundMacName = boundMacName;
        this.rssi = rssi;
        this.isSupportOta = isSupportOta;
        this.isMostNew = isMostNew;
        this.belongRegionId = belongRegionId;
        this.uid = uid;
    }

    @Generated(hash = 303143706)
    public DbCurtain() {
    }

    public String getBoundMacName() {
        return boundMacName;
    }

    public void setBoundMacName(String boundMacName) {
        this.boundMacName = boundMacName;
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
    public int getBelongGroupAddr() {
        return this.belongGroupAddr;
    }
    public void setBelongGroupAddr(int belongGroupAddr) {
        this.belongGroupAddr = belongGroupAddr;
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
    public int getStatus() {
        return this.status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public boolean getInverse() {
        return this.inverse;
    }
    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }
    public boolean getClosePull() {
        return this.closePull;
    }
    public void setClosePull(boolean closePull) {
        this.closePull = closePull;
    }
    public int getSpeed() {
        return this.speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public boolean getCloseSlowStart() {
        return this.closeSlowStart;
    }
    public void setCloseSlowStart(boolean closeSlowStart) {
        this.closeSlowStart = closeSlowStart;
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

    public boolean isInverse() {
        return inverse;
    }

    public boolean isClosePull() {
        return closePull;
    }

    public boolean isCloseSlowStart() {
        return closeSlowStart;
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
            this.icon = R.drawable.icon_curtain_off;
        } else if (this.connectionStatus == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_curtain_off;
        } else if (this.connectionStatus == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_curtain_device;
        }
    }
    public String getGroupName() {
        return this.groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return "DbCurtain{" +
                "id=" + id +
                ", meshAddr=" + meshAddr +
                ", name='" + name + '\'' +
                ", belongGroupAddr=" + belongGroupAddr +
                ", macAddr='" + macAddr + '\'' +
                ", productUUID=" + productUUID +
                ", status=" + status +
                ", inverse=" + inverse +
                ", closePull=" + closePull +
                ", speed=" + speed +
                ", closeSlowStart=" + closeSlowStart +
                ", index=" + index +
                ", belongGroupId=" + belongGroupId +
                ", groupName='" + groupName + '\'' +
                ", selected=" + selected +
                ", version='" + version + '\'' +
                ", hasGroup=" + hasGroup +
                ", textColor=" + textColor +
                ", icon=" + icon +
                ", connectionStatus=" + connectionStatus +
                '}';
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
}
