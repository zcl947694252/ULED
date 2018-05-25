package com.dadoutek.uled.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hejiajun on 2018/5/23.
 */

@Entity
public class DbDataChange {
    @Id(autoincrement = true)
    private Long id;

    private Long changeId;

    private String tableName;

    private String changeType;

    @Generated(hash = 668824277)
    public DbDataChange(Long id, Long changeId, String tableName,
            String changeType) {
        this.id = id;
        this.changeId = changeId;
        this.tableName = tableName;
        this.changeType = changeType;
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
}