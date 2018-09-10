package com.dadoutek.uled.util;

import android.text.InputFilter;
import android.util.Log;
import android.widget.EditText;

import com.dadoutek.uled.model.Constant;

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

//        String limitEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        String limitEx = "[`~\\\\!()+=|{}':;',\\[\\].<>/?~！￥……（）——+|{}【】‘；：”“’。，、？]";

        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);

        if (m.find() || str.isEmpty()) {
            return true;
        }
        return false;
    }

    public static void initEditTextFilter(EditText editText) {
        //表情过滤器
        InputFilter emojiFilter = (source, start, end, dest, dstart, dend) -> {
            Pattern emoji = Pattern.compile(
                    "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
            Matcher emojiMatcher = emoji.matcher(source);
            if (emojiMatcher.find()) {
                return "";
            }
            return null;
        };
        //特殊字符过滤器
        InputFilter specialCharFilter = (source, start, end, dest, dstart, dend) -> {
            String regexStr = "[`~\\\\!()+=|{}':;',\\[\\].<>/?~！￥……（）——+|{}【】‘；：”“’。，、？\\s+]";
            Pattern pattern = Pattern.compile(regexStr);
            Matcher matcher = pattern.matcher(source.toString());
            if (matcher.matches()) {
                return "";
            } else {
                return null;
            }

        };

        editText.setFilters(new InputFilter[]{emojiFilter, specialCharFilter,new InputFilter.LengthFilter(16)});
    }

    /**
     * 解析URL地址
     * @param url 传入地址
     * @param content 返回内容，传入0返回固件类型灯或者控制器，传入1返回版本号（L-2.0.8-L208.bin）
     * @return
     */
    public static String versionResolutionURL(String url,int content){
        String[] parts=shift(url,"/");
        String versionPart=parts[parts.length-1];
        return versionResolution(versionPart,content);
    }

    /**
     * 解析URL地址
     * @param versionPart 传入版本
     * @param content 返回内容，传入0返回固件类型灯或者控制器，传入1返回版本号（L-2.0.8-L208.bin）
     * @return
     */
    public static String versionResolution(String versionPart,int content){
        String[] versionContent=shift(versionPart,"-");
        switch (content){
            case 0:
                return versionContent[0];
//                String type=versionContent[0];
//                if(type.equals("L")){
//                    return Constant.FIRMWARE_TYPE_LIGHT;
//                }else if(type.equals("C")){
//                    return Constant.FIRMWARE_TYPE_CONTROLLER;
//                }
            case 1:
                return versionContent[1].replaceAll("\\.","").replaceAll("bin","");
            case 2:
                return versionPart.replaceAll("\\.bin","");
        }
        return "";
    }

    /**
     * @param str 要分离的字符串
     * @param sp  分隔符
     * @return 返回分离后的每一个对象值
     */
    public static String[] shift(String str, String sp) {
        if (str == null) return null;
        String chs1[] = str.split(sp);
        return chs1;
    }

    /**
     * @param str 要分离的字符串
     * @param sp  分隔符
     * @param i   要拿的字符数组中的index
     * @return 返回分离后的每一个对象值
     */
    public static String shift(String str, String sp, int i) {
        if (str == null) return "";
        String chs1[] = str.split(sp);
        return chs1[i];
    }
}
