package com.dadoutek.uled.model;

import com.dadoutek.uled.model.DbModel.DbGroup;

import java.util.List;

public class ItemTypeGroup {
    String name;
    List<DbGroup> list;
    int icon;

    public ItemTypeGroup() {
    }

    public ItemTypeGroup(String name, List<DbGroup> list) {
        this.name = name;
        this.list = list;
    }

    public ItemTypeGroup(String name, List<DbGroup> list, int icon) {
        this.name = name;
        this.list = list;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DbGroup> getList() {
        return list;
    }

    public void setList(List<DbGroup> list) {
        this.list = list;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
