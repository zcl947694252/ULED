package com.dadoutek.uled.model;

import android.graphics.Color;

import java.io.Serializable;

public class ItemColorPreset implements Serializable {
    int color=Color.BLUE;
    int brightness = 100;

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
