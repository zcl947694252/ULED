package com.dadoutek.uled.util;

import android.content.Context;
import android.util.Log;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/3/27.
 * 用于系统自动生成一些初始化数据
 */

public class DataCreater {


    public static Group createAllLightController(Context context){
        Group groupAllLights = new Group();
        groupAllLights.name = context.getString(R.string.allLight);
        groupAllLights.meshAddress = 0xFFFF;
        groupAllLights.brightness = 100;
        groupAllLights.temperature = 100;
        groupAllLights.color = 0xFFFFFF;
        return groupAllLights;
    }
    /**
     * 创建分组
     * @param automaticCreat 是否系统默认自己创建
     * @param number 当前不是系统创建 automaticCreat为false，填写创建分组数量
     */
    public static void creatGroup(Context context, boolean automaticCreat, int number){
        Groups.getInstance().clear();
        Groups groups=Groups.getInstance();
        int groupNum=0;


        if(automaticCreat){
            groupNum=16;
        }else{
            groupNum=number;
        }

        for(int i=1;i<=groupNum;i++){
            Group group=new Group();
            group.name = context.getString(R.string.group)+i;
            group.meshAddress = 0x8001+i;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            group.icon= R.drawable.ic_group_white_48dp;
            group.checked=false;
            groups.add(group);
        }

        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.GROUPS_KEY,groups);

        Log.d("test", "creatGroup: "+groups.size());
    }

    public static void updateLights(Lights lights){
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.Lights_KEY,lights);
    }

    public static void updateGroup(Groups groups){
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.GROUPS_KEY,groups);
    }

//    /**
//     * 更改指定组的信息
//     * @param group
//     * @param position
//     */
//    public static void updateGroup(Groups group,int position){
//        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.GROUPS_KEY,groups);
//    }

    /**
     * 创建分组
     * @param automaticCreat 是否系统默认自己创建
     * @param number 当前不是系统创建 automaticCreat为false，填写创建分组数量
     * @param groupNameList  自定义组名
     */
    public static void creatGroup(boolean automaticCreat, int number, List<String> groupNameList){
        Groups.getInstance().clear();
        int groupNum=0;

        if(automaticCreat){
            groupNum=16;
        }else{
            groupNum=number;
        }

        for(int i=0;i<groupNum;i++){
            Group group=new Group();
            group.name = groupNameList.get(i);
            group.meshAddress = 0x8001+i;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            Groups.getInstance().add(group);
        }

        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(), Constant.GROUPS_KEY,Groups.getInstance());
    }

    public static Groups getGroups(){
        Groups groups;
        groups=(Groups) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.GROUPS_KEY);

        if(groups!=null&&groups.size()>0){
            return groups;
        }else{
            return  Groups.getInstance();
        }
    }

    public static Lights getLights(){
        Lights lights;
        lights=(Lights) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.GROUPS_KEY);

        if(lights!=null&&lights.size()>0){
            return lights;
        }else{
            return  Lights.getInstance();
        }
    }

    public static Groups getInitGroups(){
        Groups groups;
        groups=(Groups) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.GROUPS_KEY);

        if(groups!=null&&groups.size()>0){
            for(int j=0;j<groups.size();j++){
                if(j==0){
                    Group group=groups.get(j);
                    group.checked=true;
                    groups.set(j,group);
                }else
                groups.get(j).checked=false;
            }
            return groups;
        }else{
            return  Groups.getInstance();
        }
    }
}