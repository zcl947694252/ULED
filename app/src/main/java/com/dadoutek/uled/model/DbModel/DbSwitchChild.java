package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.io.Serializable;

public class DbSwitchChild implements Serializable {

    static final long serialVersionUID = -15515456L;

    private long id;
    @NotNull
    private int meshAddr;
    private String name;
    private int controlGroupAddr;
    private String macAddr;
    private int productUUID;
    private String list;
    private int index;
    private Long belongGroupId;
    @Generated(hash = 811812800)
    public DbSwitchChild(Long id, int meshAddr, String name, int controlGroupAddr,
                    String macAddr, int productUUID, String controlSceneId, int index,
                    Long belongGroupId) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.controlGroupAddr = controlGroupAddr;
        this.macAddr = macAddr;
        this.productUUID = productUUID;
        this.list = controlSceneId;
        this.index = index;
        this.belongGroupId = belongGroupId;
    }
    @Generated(hash = 1179115222)
    public DbSwitchChild() {
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
    public String getList() {
        return this.list;
    }
    public void setList(String controlSceneId) {
        this.list = controlSceneId;
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
}
