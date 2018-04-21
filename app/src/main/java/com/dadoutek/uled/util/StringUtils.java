package com.dadoutek.uled.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hejiajun on 2018/4/21.
 */

public class StringUtils {
    /**
     * @prama: str 要判断是否包含特殊字符的目标字符串
     */

    public static boolean compileExChar(String str) {

        String limitEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";

        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);

        if (m.find()||str.isEmpty()) {
            return true;
        }
        return false;
    }
}
