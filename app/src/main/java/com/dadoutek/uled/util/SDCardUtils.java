package com.dadoutek.uled.util;

import android.os.Environment;

/**
 * Created by lsm on 2016/3/28.
 */
public class SDCardUtils {
    /**
     * 获取SDCard的目录路径功能
     *
     * @return
     */
    public static boolean isMounted() {
        // 判断SDCard是否存在
        boolean sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        return sdcardExist;
    }
}
