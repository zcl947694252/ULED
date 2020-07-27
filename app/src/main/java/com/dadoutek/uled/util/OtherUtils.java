package com.dadoutek.uled.util;

import android.content.Context;
import android.graphics.Color;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.InstallDeviceModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OtherUtils {

    /**
     * 获取图片名称获取图片的资源id的方法
     * @param imageName
     * @return
     */
    public static int  getResourceId(String imageName,Context context) {
        int resId;
        try {
            resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
        }catch (Exception e){
            resId = 0;
        }
        return resId;
    }


    /**
     * 获取图片名称
     * @param resid
     * @return
     */
    public static String getResourceName(int resid, Context context){
        try {
            return   context.getResources().getResourceName(resid);
        }catch (Exception e){
            return   "";
        }

    }

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

    public static boolean isCurtain(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_CURTAIN.intValue()){
            return true;
        }else{
            return false;
        }
    }

    public static boolean  isConnector(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_CONNECTOR.intValue()){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isRGBGroup(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_LIGHT_RGB.intValue()){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isNormalGroup(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_LIGHT_NORMAL.intValue()){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isAllRightGroup(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_NO.intValue()){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isDefaultGroup(DbGroup dbGroup){
        if(dbGroup.getDeviceType()!=null && dbGroup.getDeviceType().intValue()== Constant.DEVICE_TYPE_DEFAULT_ALL.intValue()){
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
            if (dbLights.get(i).getBelongGroupId().longValue() == dbGroup.getId().longValue()) {
                return false;
            }
        }
        return true;
    }

    //初始颜色预设值
    public static int getCreateInitColor(int type){
        switch (type){
//            case 0:return Color.RED;
//            case 1:return Color.BLUE;
//            case 2:return Color.GREEN;
//            case 3:return Color.YELLOW;
//            case 4:return Color.parseColor("#800080");

            case 0:return Color.parseColor("#ff4f4f");
            case 1:return Color.parseColor("#ff439b");
            case 2:return Color.parseColor("#4FFFE0");
            case 3:return Color.parseColor("#FFF94F");
        }
        return 0;
    }




    /**
     * 获取要安装的设备的名字和描述
     * @return  要安装的设备的名字和描述的list
     */
    public static ArrayList<InstallDeviceModel> getInstallDeviceList(Context context){
        ArrayList<InstallDeviceModel> list = new ArrayList<>();
        InstallDeviceModel installDeviceModel1=new InstallDeviceModel(context.getString(R.string.normal_light),context.getString(R.string.normal_light_describe));
        InstallDeviceModel installDeviceModel2=new InstallDeviceModel(context.getString(R.string.rgb_light),context.getString(R.string.rgb_light_describe));
        InstallDeviceModel installDeviceModel3=new InstallDeviceModel(context.getString(R.string.switch_title),context.getString(R.string.switch_describe));
        InstallDeviceModel installDeviceModel4=new InstallDeviceModel(context.getString(R.string.sensor),context.getString(R.string.sensor_describe));
        InstallDeviceModel installDeviceModel5=new InstallDeviceModel(context.getString(R.string.curtain),context.getString(R.string.smart_curtain));
        InstallDeviceModel installDeviceModel6=new InstallDeviceModel(context.getString(R.string.relay),context.getString(R.string.for_connector));
        InstallDeviceModel installDeviceModel7=new InstallDeviceModel(context.getString(R.string.Gate_way),context.getString(R.string.for_connector));
        list.add(installDeviceModel1);
        list.add(installDeviceModel2);
        list.add(installDeviceModel3);
        list.add(installDeviceModel4);
        list.add(installDeviceModel5);
        list.add(installDeviceModel6);
        list.add(installDeviceModel7);
        return list;
    }


    public static ArrayList<DbLight> sortList(@NotNull ArrayList<DbLight> arr) {
        int min;
        DbLight temp;
        for (int i = 0; i < arr.size(); i++) {
            min = i;
            for (int j = i + 1; j < arr.size(); j++) {
                if (arr.get(j).getBelongGroupId() < arr.get(min).getBelongGroupId()) {
                    min = j;
                }
            }
            if (arr.get(i).getBelongGroupId() > arr.get(min).getBelongGroupId()) {
                temp = arr.get(i);
                arr.set(i, arr.get(min));
                arr.set(min, temp);
            }
        }
        return arr;
    }

}
