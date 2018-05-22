package com.dadoutek.uled.util;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;

/**
 * Created by hejiajun on 2018/5/22.
 */

public class SharedPreferencesUtils {

    public static void saveCurrentUseRegion(long id){
        SharedPreferencesHelper.putLong(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_REGION_KEY,id);
    }

    public static long getCurrentUseRegion(){
        return SharedPreferencesHelper.getLong(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_REGION_KEY,-1);
    }
}
