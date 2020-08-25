package com.dadoutek.uled.gateway.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 创建者     ZCL
 * 创建时间   2020/8/13 11:39
 * greendao不支持kotlin bean文件必须使用java 1.dbRouterDao创建后标记enter 2.make-project 自动生成相关dao session文件
 * 3.创建DaoSessionInstance.getInstance()管理daosession对象 对到进行操作
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
@Entity
public class DbRouter {
    @Id(autoincrement = true)
    private long id;
    private int uid;
    private int belongRegionId;
    private String name;
    private String macAddr;
    private int timeZoneHour;
    private int timeZoneMin;
    private int productUUID;
    private int state;
    private String ble_version;
    private String esp_version;
    private String lastOnlineTime;
    private String lastOfflineTime;
    private int open;
    public  boolean isChecked = false;

    @Generated(hash = 408447731)
    public DbRouter(long id, int uid, int belongRegionId, String name, String macAddr, int timeZoneHour,
            int timeZoneMin, int productUUID, int state, String ble_version, String esp_version,
            String lastOnlineTime, String lastOfflineTime, int open, boolean isChecked) {
        this.id = id;
        this.uid = uid;
        this.belongRegionId = belongRegionId;
        this.name = name;
        this.macAddr = macAddr;
        this.timeZoneHour = timeZoneHour;
        this.timeZoneMin = timeZoneMin;
        this.productUUID = productUUID;
        this.state = state;
        this.ble_version = ble_version;
        this.esp_version = esp_version;
        this.lastOnlineTime = lastOnlineTime;
        this.lastOfflineTime = lastOfflineTime;
        this.open = open;
        this.isChecked = isChecked;
    }

    @Generated(hash = 848701062)
    public DbRouter() {
    }

    public long getId() { return id;}

    public void setId(long id) { this.id = id;}

    public int getUid() { return uid;}

    public void setUid(int uid) { this.uid = uid;}

    public int getBelongRegionId() { return belongRegionId;}

    public void setBelongRegionId(int belongRegionId) { this.belongRegionId = belongRegionId;}

    public String getName() { return name;}

    public void setName(String name) { this.name = name;}

    public String getMacAddr() { return macAddr;}

    public void setMacAddr(String macAddr) { this.macAddr = macAddr;}

    public int getTimeZoneHour() { return timeZoneHour;}

    public void setTimeZoneHour(int timeZoneHour) { this.timeZoneHour = timeZoneHour;}

    public int getTimeZoneMin() { return timeZoneMin;}

    public void setTimeZoneMin(int timeZoneMin) { this.timeZoneMin = timeZoneMin;}

    public int getProductUUID() { return productUUID;}

    public void setProductUUID(int productUUID) { this.productUUID = productUUID;}

    public int getState() { return state;}

    public void setState(int state) { this.state = state;}

    public String getBle_version() { return ble_version;}

    public void setBle_version(String ble_version) { this.ble_version = ble_version;}

    public String getEsp_version() { return esp_version;}

    public void setEsp_version(String esp_version) { this.esp_version = esp_version;}

    public String getLastOnlineTime() { return lastOnlineTime;}

    public void setLastOnlineTime(String lastOnlineTime) { this.lastOnlineTime = lastOnlineTime;}

    public String getLastOfflineTime() { return lastOfflineTime;}

    public void setLastOfflineTime(String lastOfflineTime) { this.lastOfflineTime =
            lastOfflineTime;}

    public int getOpen() { return open;}

    public void setOpen(int open) { this.open = open;}

    public boolean getIsCheckedInGroup() {
        return this.isChecked;
    }

    public void setIsCheckedInGroup(boolean isCheckedInGroup) {
        this.isChecked = isCheckedInGroup;
    }

    public boolean getIsChecked() {
        return this.isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }
}
