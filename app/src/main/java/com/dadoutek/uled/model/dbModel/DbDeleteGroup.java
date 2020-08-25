package com.dadoutek.uled.model.dbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DbDeleteGroup {

    @Id(autoincrement = true)
    private Long id;

    private int groupAress;

    @Generated(hash = 100545986)
    public DbDeleteGroup(Long id, int groupAress) {
        this.id = id;
        this.groupAress = groupAress;
    }

    @Generated(hash = 1182865327)
    public DbDeleteGroup() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getGroupAress() {
        return this.groupAress;
    }

    public void setGroupAress(int groupAress) {
        this.groupAress = groupAress;
    }

}
