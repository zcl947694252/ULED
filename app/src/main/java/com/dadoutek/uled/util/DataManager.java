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

import java.util.List;

/**
 * Created by hejiajun on 2018/3/27.
 * 用于系统自动生成一些初始化数据
 */

public class DataManager {
    private Context mContext;
    private String mMeshName;
    private String mPwd;

    /**
     * 构造函数
     * @param context   Activity或Application的Context
     * @param meshName  要管理哪个MeshName里的数据
     * @param pwd       要管理哪个pwd里的数据
     */
    public DataManager(Context context, String meshName, String pwd) {
        mContext = context;
        mMeshName = meshName;
        mPwd = pwd;
    }

    /**
     * 创建一个控制所有灯的分组
     * @return  group对象
     */
    public Group createAllLightControllerGroup() {
        Group groupAllLights = new Group();
        groupAllLights.name = mContext.getString(R.string.allLight);
        groupAllLights.meshAddress = 0xFFFF;
        groupAllLights.brightness = 100;
        groupAllLights.temperature = 100;
        groupAllLights.color = 0xFFFFFF;
        return groupAllLights;
    }

    /**
     * 创建分组
     *
     * @param automaticCreat 是否系统默认自己创建
     * @param number         当前不是系统创建 automaticCreat为false，填写创建分组数量
     */
    public void creatGroup(boolean automaticCreat, int number) {
        Groups.getInstance().clear();
        Groups groups = Groups.getInstance();
        int groupNum = 0;


        if (automaticCreat) {
            groupNum = 16;
        } else {
            groupNum = number;
        }

        for (int i = 1; i <= groupNum; i++) {
            Group group = new Group();
            group.name = mContext.getString(R.string.group) + i;
            group.meshAddress = 0x8001 + i;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            group.icon = R.drawable.ic_group_white_48dp;
            group.checked = false;
            groups.add(group);
        }

        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.GROUPS_KEY, groups);

        Log.d("test", "creatGroup: " + groups.size());
    }

    public void updateLights(Lights lights) {
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.LIGHTS_KEY, lights);
    }

    public void updateGroup(Groups groups) {
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.GROUPS_KEY, groups);
    }

//    /**
//     * 更改指定组的信息
//     * @param group
//     * @param position
//     */
//    public void updateGroup(Groups group,int position){
//        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),Constant.GROUPS_KEY,groups);
//    }

    /**
     * 创建分组
     *
     * @param automaticCreat 是否系统默认自己创建
     * @param number         当前不是系统创建 automaticCreat为false，填写创建分组数量
     * @param groupNameList  自定义组名
     */
    public void creatGroup(boolean automaticCreat, int number, List<String> groupNameList) {
        Groups.getInstance().clear();
        int groupNum = 0;

        if (automaticCreat) {
            groupNum = 16;
        } else {
            groupNum = number;
        }

        for (int i = 0; i < groupNum; i++) {
            Group group = new Group();
            group.name = groupNameList.get(i);
            group.meshAddress = 0x8001 + i;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            Groups.getInstance().add(group);
        }

        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.GROUPS_KEY, Groups.getInstance());
    }

    public Groups getGroups() {
        Groups groups;
        groups = (Groups) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.GROUPS_KEY);

        if (groups != null && groups.size() > 0) {
            return groups;
        } else {
            creatGroup(true, 0);
            return Groups.getInstance();
        }
    }

    public Lights getLights() {
        Lights lights;
        lights = (Lights) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.LIGHTS_KEY);

        if (lights != null) {
            return lights;
        } else {
            return Lights.getInstance();
        }
    }

    public Groups initGroupsChecked() {
        Groups groups;
        groups = Groups.getInstance();
        if (groups != null && groups.size() > 0)
            for (int j = 0; j < groups.size(); j++) {
                if (j == 0) {
                    Group group = groups.get(j);
                    group.checked = true;
                    groups.set(j, group);
                } else
                    groups.get(j).checked = false;
            }
        return groups;
    }
}