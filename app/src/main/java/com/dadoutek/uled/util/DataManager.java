package com.dadoutek.uled.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.activity.RenameActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.google.gson.Gson;

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
     *
     * @param context  Activity或Application的Context
     * @param meshName 要管理哪个MeshName里的数据
     * @param pwd      要管理哪个pwd里的数据
     */
    public DataManager(Context context, String meshName, String pwd) {
        mContext = context;
        mMeshName = meshName;
        mPwd = pwd;
    }

    /**
     * 创建一个控制所有灯的分组
     *
     * @return group对象
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

    public void updateGroup(Group group,Context context) {
        if(group.meshAddress==0xFFFF){
            SharedPreferencesHelper.putObject(context,mMeshName + mPwd + Constant.GROUPS_KEY_ALL,group);
        }

        Groups groups=getGroups();
        for(int i=0;i<groups.size();i++){
            if(groups.get(i).meshAddress==group.meshAddress){
                groups.set(i,group);
                SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                        mMeshName + mPwd + Constant.GROUPS_KEY, groups);
            }
        }
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


    /**
     * 用户手动创建分组
     *
     * @param name   自定义组名
     * @param groups 当前组集
     */
    public void creatGroup(String name, Groups groups,Context context) {
        if(!checkRepeat(groups,context,name)){
            int count = groups.size();
            int newMeshAdress = ++count;
            Group group = new Group();
            group.name = name;
            group.meshAddress = 0x8001 + newMeshAdress;
            group.brightness = 100;
            group.temperature = 100;
            group.color = 0xFFFFFF;
            groups.add(group);
            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                    mMeshName + mPwd + Constant.GROUPS_KEY, groups);
        }
    }

    public boolean checkRepeat(Groups groups,Context context,String newName){
        for (
                int k = 0; k < groups.size(); k++)

        {
            if (groups.get(k).name.equals(newName)) {
                Toast.makeText(context, R.string.creat_group_fail_tip, Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    public boolean checkRepeat(Lights lights,Context context,String newName){
        for (
                int k = 0; k < lights.size(); k++)

        {
            if(lights.get(k).name==null){
                return false;
            }
            if (lights.get(k).name.equals(newName)) {
                Toast.makeText(context, "创建失败,名称已存在", Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    public Groups getGroups() {
        Groups groups;
        groups = (Groups) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                mMeshName + mPwd + Constant.GROUPS_KEY);

        if (groups != null && groups.size() > 0) {
            return groups;
        } else {
            creatGroup(false, 1);
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
        groups = getGroups();
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

    public String getGroupNameByAdress(int groupAdress) {
        Groups groups = getGroups();
        for (int j = 0; j < groups.size(); j++) {
            if (groups.get(j).meshAddress == groupAdress) {
                return groups.get(j).name;
            }
        }
        return "null";
    }

    public Group getGroup(int groupAdress,Context context) {
        Groups groups = getGroups();
        for (int j = 0; j < groups.size(); j++) {
            if (groups.get(j).meshAddress == groupAdress) {
                return groups.get(j);
            }
        }

        if(groupAdress==0xFFFF){
            Group group=(Group) SharedPreferencesHelper.getObject(context,mMeshName + mPwd + Constant.GROUPS_KEY_ALL);
            if(group==null){
                return createAllLightControllerGroup();
            }
           return group;
        }else{
            return null;
        }
    }

    public String getLightName(Light light){
        Lights lights=getLights();
        for(int i=0;i<lights.size();i++){
            if(lights.get(i).name!=null&&!lights.get(i).name.isEmpty()){
                if(light.meshAddress==lights.get(i).meshAddress){
                    return lights.get(i).name;
                }
            }
        }
        return light.getLabel();
    }
}