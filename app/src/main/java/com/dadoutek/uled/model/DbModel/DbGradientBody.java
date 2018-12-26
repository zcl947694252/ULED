package com.dadoutek.uled.model.DbModel;

import java.util.List;

public class DbGradientBody {

    private String name;

    private int type;

    private int speed;

    private List<DbColorNode> colorNodes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public List<DbColorNode> getColorNodes() {
        return colorNodes;
    }

    public void setColorNodes(List<DbColorNode> colorNodes) {
        this.colorNodes = colorNodes;
    }

    @Override
    public String toString() {
        return "DbGradientBody{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", speed=" + speed +
                ", colorNode=" + colorNodes +
                '}';
    }
}
