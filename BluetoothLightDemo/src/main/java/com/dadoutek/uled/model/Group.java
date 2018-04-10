package com.dadoutek.uled.model;

import android.content.res.ColorStateList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable{

	public String name;
	public int meshAddress;
	public int brightness;
	public int color;
	public int temperature;

	public int icon;
	public ColorStateList textColor;

	public boolean checked;

	public List<Light> containsLightList=new ArrayList<>();
}
