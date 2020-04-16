package com.dadoutek.uled.model.DbModel;

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
 //firmwareVersion	否	String	固件版本号
 //keys	否	String	八键开关keys
 //type
    public String firmwareVersion;
    public String keys;
    public int type; // 群组模式 = 0，场景模式 =1 ，自定义模式= 2，非八键开关 = 3


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
