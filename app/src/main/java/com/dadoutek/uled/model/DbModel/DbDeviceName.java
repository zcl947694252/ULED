package com.dadoutek.uled.model.DbModel;

import com.google.gson.annotations.Expose;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

public class DbDeviceName  {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean checked=false;
//    @Expose()
//    @Expose(serialize = false, deserialize = false)

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
