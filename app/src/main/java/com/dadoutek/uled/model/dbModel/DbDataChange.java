package com.dadoutek.uled.model.dbModel;

import androidx.annotation.Nullable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hejiajun on 2018/5/23.
 */

@Entity
public class DbDataChange {
    @Id(autoincrement = true)
    private Long id;//自增

    private Long changeId;//变化行id

    private String tableName;//改变的表名

    private String changeType;  //数据改变操作类型  增删改查

    public int type = 3 ;  // 群组模式 = 0，场景模式 =1 ，自定义模式= 2，非八键开关 = 3
@Nullable
    public String keys ="";

    @Generated(hash = 1326895366)
    public DbDataChange(Long id, Long changeId, String tableName, String changeType,
            int type, String keys) {
        this.id = id;
        this.changeId = changeId;
        this.tableName = tableName;
        this.changeType = changeType;
        this.type = type;
        this.keys = keys;
    }

    @Generated(hash = 571649333)
    public DbDataChange() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChangeId() {
        return this.changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getChangeType() {
        return this.changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getKeys() {
        return this.keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

}
