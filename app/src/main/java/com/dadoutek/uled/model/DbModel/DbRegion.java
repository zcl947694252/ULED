package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by hejiajun on 2018/5/9.
 */

@Entity
public class DbRegion {
    @Id(autoincrement = true)
    private Long id;

    private String controlMesh;
    private String controlMeshPwd;
    private String installMesh;
    private String installMeshPwd;

    private String belongAccount;

    @Generated(hash = 1441573477)
    public DbRegion(Long id, String controlMesh, String controlMeshPwd,
            String installMesh, String installMeshPwd, String belongAccount) {
        this.id = id;
        this.controlMesh = controlMesh;
        this.controlMeshPwd = controlMeshPwd;
        this.installMesh = installMesh;
        this.installMeshPwd = installMeshPwd;
        this.belongAccount = belongAccount;
    }

    @Generated(hash = 2106128169)
    public DbRegion() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getControlMesh() {
        return this.controlMesh;
    }

    public void setControlMesh(String controlMesh) {
        this.controlMesh = controlMesh;
    }

    public String getControlMeshPwd() {
        return this.controlMeshPwd;
    }

    public void setControlMeshPwd(String controlMeshPwd) {
        this.controlMeshPwd = controlMeshPwd;
    }

    public String getInstallMesh() {
        return this.installMesh;
    }

    public void setInstallMesh(String installMesh) {
        this.installMesh = installMesh;
    }

    public String getInstallMeshPwd() {
        return this.installMeshPwd;
    }

    public void setInstallMeshPwd(String installMeshPwd) {
        this.installMeshPwd = installMeshPwd;
    }

    public String getBelongAccount() {
        return this.belongAccount;
    }

    public void setBelongAccount(String belongAccount) {
        this.belongAccount = belongAccount;
    }
}
