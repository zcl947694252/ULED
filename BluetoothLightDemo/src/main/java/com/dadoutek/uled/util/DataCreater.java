package com.dadoutek.uled.util;

import android.util.Log;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.util.List;

/**
 * Created by hejiajun on 2018/3/27.
 * 用于系统自动生成一些初始化数据
 */

public class DataCreater {

    /**
     * 创建分组
     * @param automaticCreat 是否系统默认自己创建
     * @param number 当前不是系统创建 automaticCreat为false，填写创建分组数量
     */
    public static void creatGroup(boolean automaticCreat,int number){
        Groups.getInstance().clear();
        Groups groups=Groups.getInstance();
        int groupNum=0;

        if(automaticCreat){
            groupNum=16;
        }else{
            groupNum=number;
        }

        for(int i=0;i<groupNum;i++){
            Group group=new Group();
            group.name = "Group"+i;
            group.meshAddress = 0x8001+i;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            group.icon= R.drawable.ic_group_black_48dp;
            groups.add(group);
        }

        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.GROUPS_KEY,groups);

        Log.d("test", "creatGroup: "+groups.size());
    }

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
        return  groups;
    }
}