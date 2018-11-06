package com.dadoutek.uled.model;

import android.graphics.Color;

import java.io.Serializable;

public class ItemColorPreset implements Serializable {
    int color=Color.BLUE;
    int brightness = 100;
    int pointX = 0;
    int pointY = 0;

    public int getPointX() {
        return pointX;
    }

    public void setPointX(int pointX) {
        this.pointX = pointX;
    }

    public int getPointY() {
        return pointY;
    }

    public void setPointY(int pointY) {
        this.pointY = pointY;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
}
