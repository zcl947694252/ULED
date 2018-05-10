package com.dadoutek.uled.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/5/7.
 */

@Entity
public class DbSceneActions{
    @Id(autoincrement = true)
    private Long id;

    private Long actionId;
    @NotNull
    private int groupAddr;
    @NotNull
    private int colorTemperature;
    @NotNull
    private int brightness;
    @NotNull
    private String belongAccount;
    @Generated(hash = 364634455)
    public DbSceneActions(Long id, Long actionId, int groupAddr,
            int colorTemperature, int brightness, @NotNull String belongAccount) {
        this.id = id;
        this.actionId = actionId;
        this.groupAddr = groupAddr;
        this.colorTemperature = colorTemperature;
        this.brightness = brightness;
        this.belongAccount = belongAccount;
    }
    @Generated(hash = 885919265)
    public DbSceneActions() {
    }
    public Long getActionId() {
        return this.actionId;
    }
    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }
    public int getGroupAddr() {
        return this.groupAddr;
    }
    public void setGroupAddr(int groupAddr) {
        this.groupAddr = groupAddr;
    }
    public int getColorTemperature() {
        return this.colorTemperature;
    }
    public void setColorTemperature(int colorTemperature) {
        this.colorTemperature = colorTemperature;
    }
    public int getBrightness() {
        return this.brightness;
    }
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
    public String getBelongAccount() {
        return this.belongAccount;
    }
    public void setBelongAccount(String belongAccount) {
        this.belongAccount = belongAccount;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}
