package com.dadoutek.uled.util;

import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;

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
            Groups.getInstance().add(group);
        }

    }
}
