package com.dadoutek.uled.model;

import android.content.res.ColorStateList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 7387748275306879629L;

    public String name;
    public int meshAddress;
    public int brightness;
    public int color;
    public int temperature;

    public int icon;
    public ColorStateList textColor;

    public boolean checked;

    public ArrayList<Integer> containsLightList=new ArrayList<>();
}
