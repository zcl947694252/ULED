package com.dadoutek.uled.network.bean;

/**
 * 创建者     ZCL
 * 创建时间   2019/8/6 16:50
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class RegionAuthorizeBean {

    /**
     * create_time : 2019-08-01 11:07:15
     * level : 1
     * count_light : 121
     * count_curtain : 0
     * count_sensor : 0
     * count_all : 121
     * installMesh : dadousmart
     * count_switch : 0
     * authorizer_id : 26447
     * belongAccount : 0611685
     * phone : 90000042
     * controlMesh : 0611685
     * installMeshPwd : 123
     * count_relay : 0
     * name : 区域一/region1
     * id : 1
     * controlMeshPwd : 0611685
     */

    private String create_time;
    private int level;
    private int count_light;
    private int count_curtain;
    private int count_sensor;
    private int count_all;
    private String installMesh;
    private int count_switch;
    private int authorizer_id;
    private String belongAccount;
    private String phone;
    private String controlMesh;
    private String installMeshPwd;
    private int count_relay;
    private String name;
    private int id;

    public Boolean getIs_selected() {
        return is_selected;
    }

    public void setIs_selected(Boolean is_selected) {
        this.is_selected = is_selected;
    }

    private String controlMeshPwd;
    private Boolean is_selected;

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCount_light() {
        return count_light;
    }

    public void setCount_light(int count_light) {
        this.count_light = count_light;
    }

    public int getCount_curtain() {
        return count_curtain;
    }

    public void setCount_curtain(int count_curtain) {
        this.count_curtain = count_curtain;
    }

    public int getCount_sensor() {
        return count_sensor;
    }

    public void setCount_sensor(int count_sensor) {
        this.count_sensor = count_sensor;
    }

    public int getCount_all() {
        return count_all;
    }

    public void setCount_all(int count_all) {
        this.count_all = count_all;
    }

    public String getInstallMesh() {
        return installMesh;
    }

    public void setInstallMesh(String installMesh) {
        this.installMesh = installMesh;
    }

    public int getCount_switch() {
        return count_switch;
    }

    public void setCount_switch(int count_switch) {
        this.count_switch = count_switch;
    }

    public int getAuthorizer_id() {
        return authorizer_id;
    }

    public void setAuthorizer_id(int authorizer_id) {
        this.authorizer_id = authorizer_id;
    }

    public String getBelongAccount() {
        return belongAccount;
    }

    public void setBelongAccount(String belongAccount) {
        this.belongAccount = belongAccount;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getControlMesh() {
        return controlMesh;
    }

    public void setControlMesh(String controlMesh) {
        this.controlMesh = controlMesh;
    }

    public String getInstallMeshPwd() {
        return installMeshPwd;
    }

    public void setInstallMeshPwd(String installMeshPwd) {
        this.installMeshPwd = installMeshPwd;
    }

    public int getCount_relay() {
        return count_relay;
    }

    public void setCount_relay(int count_relay) {
        this.count_relay = count_relay;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getControlMeshPwd() {
        return controlMeshPwd;
    }

    public void setControlMeshPwd(String controlMeshPwd) {
        this.controlMeshPwd = controlMeshPwd;
    }
}
