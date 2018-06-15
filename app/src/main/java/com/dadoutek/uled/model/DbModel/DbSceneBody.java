package com.dadoutek.uled.model.DbModel;

import java.util.List;

public class DbSceneBody {

    private String name;

    private Long belongRegionId;

    private List<DbSceneActions> actions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBelongRegionId() {
        return belongRegionId;
    }

    public void setBelongRegionId(Long belongRegionId) {
        this.belongRegionId = belongRegionId;
    }

    public List<DbSceneActions> getActions() {
        return actions;
    }

    public void setActions(List<DbSceneActions> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "DbSceneBody{" +
                "name='" + name + '\'' +
                ", belongRegionId=" + belongRegionId +
                ", actions=" + actions +
                '}';
    }
}
