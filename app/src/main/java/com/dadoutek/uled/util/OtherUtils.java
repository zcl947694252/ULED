package com.dadoutek.uled.util;

import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;

import java.util.List;

public class OtherUtils {

    /**
     * 判断灯进行的分组是否成立
     *
     * @param productUUID 预分组灯的UUID
     * @param dbGroup     分组
     * @return
     */
    public static boolean whetherTheGroupIsEstablished(int productUUID, DbGroup dbGroup) {
        if (groupIsEmpty(dbGroup)) {
            //如果当前组没有任何灯，直接返回分组成立
            return true;
        } else {
            if (getFirstLightOfGroupUUID(dbGroup) == productUUID) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean isRGBGroup(DbGroup dbGroup){
        if(getFirstLightOfGroupUUID(dbGroup)==Constant.RGB_UUID){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isNormalGroup(DbGroup dbGroup){
        if(getFirstLightOfGroupUUID(dbGroup)==Constant.NORMAL_UUID){
            return true;
        }else{
            return false;
        }
    }

    private static int getFirstLightOfGroupUUID(DbGroup dbGroup) {
        List<DbLight> lights = DBUtils.INSTANCE.getLightByGroupID(dbGroup.getId());
        if (lights.size() > 0) {
            return lights.get(0).getProductUUID();
        } else {
            return -1;
        }
    }

    //判断当前组内是否为空
    public static boolean groupIsEmpty(DbGroup dbGroup) {
        List<DbLight> dbLights = DBUtils.INSTANCE.getAllLight();
        for (int i = 0; i < dbLights.size(); i++) {
            if (dbLights.get(i).getBelongGroupId() == dbGroup.getId()) {
                return false;
            }
        }
        return true;
    }
}
