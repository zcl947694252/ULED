package com.dadoutek.uled.util;

import android.content.Context;
import android.text.InputFilter;
import android.widget.EditText;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbConnector;
import com.dadoutek.uled.model.DbModel.DbCurtain;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbSensor;
import com.dadoutek.uled.model.DbModel.DbSwitch;
import com.dadoutek.uled.model.DeviceType;
import com.dadoutek.uled.tellink.TelinkLightApplication;

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
        editText.setSelection(editText.getText().toString().length());
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
            String regexStr = "[`~\\\\!()+=|{}':;',\\[\\].<>/?~！￥……（）——+|{}【】‘；：”“’。，、？]";
            Pattern pattern = Pattern.compile(regexStr);
            Matcher matcher = pattern.matcher(source.toString());
            if (matcher.matches() || compileExChar(source.toString())) {
                return "";
            } else {
                return null;
            }

        };

        editText.setFilters(new InputFilter[]{emojiFilter, specialCharFilter, new InputFilter.LengthFilter(50)});
    }

    public static void initEditTextFilterForRegister(EditText editText) {
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
            if (matcher.matches() || compileExChar(source.toString())) {
                return "";
            } else {
                return null;
            }

        };

        editText.setFilters(new InputFilter[]{emojiFilter, specialCharFilter, new InputFilter.LengthFilter(50)});
    }

    /**
     * 解析URL地址
     *
     * @param url     传入地址
     * @param content 返回内容，传入0返回固件类型灯或者控制器，传入1返回版本号（L-2.0.8-L208.bin）
     * @return
     */
    public static String versionResolutionURL(String url, int content) {
        String[] parts = shift(url, "/");
        String versionPart = parts[parts.length - 1];
        return versionResolution(versionPart, content);
    }

    /**
     * 解析URL地址
     *
     * @param versionPart 传入版本
     * @param content     返回内容，传入0返回固件类型灯或者控制器，传入1返回版本号（L-2.0.8-L208.bin）
     * @return
     */
    public static String versionResolution(String versionPart, int content) {
        String[] versionContent = shift(versionPart, "-");
        switch (content) {
            case 0:
                return versionContent[0];
//                String type=versionContent[0];
//                if(type.equals("L")){
//                    return Constant.FIRMWARE_TYPE_LIGHT;
//                }else if(type.equals("C")){
//                    return Constant.FIRMWARE_TYPE_CONTROLLER;
//                }
            case 1:
                String numVersion = versionContent[1].replaceAll("\\.", "").replaceAll("bin", "");
                if (isNumeric(numVersion)) {
                    return numVersion;
                } else {
                    return "-1";
                }
            case 2:
                return versionPart.replaceAll("\\.bin", "");
        }
        return "";
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
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

    public static String getLightGroupName(DbLight light) {
        if (light.getBelongGroupId() != null) {
            if (DBUtils.INSTANCE.getGroupByID(light.getBelongGroupId()) == null) {
                return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
            }
            //如果当前灯没分组  显示未分组
            if (DBUtils.INSTANCE.getGroupByID(light.getBelongGroupId()).getMeshAddr() == 0xffff) {
                return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
            } else {
                return DBUtils.INSTANCE.getGroupByID(light.getBelongGroupId()).getName();
            }
        } else {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        }
    }

    public static String getConnectorName(DbConnector connector) {
        if (DBUtils.INSTANCE.getGroupByID(connector.getBelongGroupId()) == null) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        }
        //如果当前灯没分组  显示未分组
        if (DBUtils.INSTANCE.getGroupByID(connector.getBelongGroupId()).getMeshAddr() == 0xffff) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        } else {
            return DBUtils.INSTANCE.getGroupByID(connector.getBelongGroupId()).getName();
        }
    }

    public static String getCurtainName(DbCurtain curtain) {
        if (DBUtils.INSTANCE.getGroupByID(curtain.getBelongGroupId()) == null) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        }
        //如果当前灯没分组  显示未分组
        if (DBUtils.INSTANCE.getGroupByID(curtain.getBelongGroupId()).getMeshAddr() == 0xffff) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        } else {
            return DBUtils.INSTANCE.getGroupByID(curtain.getBelongGroupId()).getName();
        }
    }
    public static String getSwitchName(DbSwitch dbSwitch) {
         Long belongGroupId = dbSwitch.getBelongGroupId();
        if (belongGroupId==null||DBUtils.INSTANCE.getGroupByID(belongGroupId) == null) {
                return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
            }

        //如果当前灯没分组  显示未分组
        if (DBUtils.INSTANCE.getGroupByID(dbSwitch.getBelongGroupId()).getMeshAddr() == 0xffff) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        } else {
            return DBUtils.INSTANCE.getGroupByID(dbSwitch.getBelongGroupId()).getName();
        }
    }
    public static String getSensorName(DbSensor dbSensor) {
        if (DBUtils.INSTANCE.getGroupByID(dbSensor.getBelongGroupId()) == null) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        }
        //如果当前灯没分组  显示未分组
        if (DBUtils.INSTANCE.getGroupByID(dbSensor.getBelongGroupId()).getMeshAddr() == 0xffff) {
            return TelinkLightApplication.Companion.getApp().getString(R.string.not_grouped);
        } else {
            return DBUtils.INSTANCE.getGroupByID(dbSensor.getBelongGroupId()).getName();
        }
    }

    public static String getInstallDescribe(int type, Context context) {
        switch (type) {
            case Constant.INSTALL_NORMAL_LIGHT:
            case Constant.INSTALL_RGB_LIGHT:
                return context.getString(R.string.guide_tip_reset_light);
            case Constant.INSTALL_SWITCH:
                return context.getString(R.string.guide_tip_reset_switch);
            case Constant.INSTALL_SENSOR:
                return context.getString(R.string.guide_tip_reset_sensor);
            case Constant.INSTALL_CURTAIN:
                return context.getString(R.string.guide_tip_reset_curtain);
            case Constant.INSTALL_CONNECTOR:
                return context.getString(R.string.guide_tip_reset_relay);
        }
        return "";
    }

    public static  String getSwitchPirDefaultName(int productUUID, Context context) {
        String startStr = "";
        switch (productUUID) {
            case DeviceType.NORMAL_SWITCH:
            case DeviceType.NORMAL_SWITCH2:
                startStr = context.getString(R.string.normal_swith);
                break;
            case DeviceType.SCENE_SWITCH:
                startStr = context.getString(R.string.scene_switch);
                break;

            case DeviceType.SMART_CURTAIN_SWITCH:
                startStr = context.getString(R.string.curtain_switch);
                break;
            case DeviceType.NIGHT_LIGHT:
                startStr = context.getString(R.string.night_light);
                break;
            case DeviceType.SENSOR:
                startStr =  context.getString(R.string.sensor);
                break;
        }

      /*  List<DbSwitch> swtitches = DBUtils.INSTANCE.getSwtitchesByProductUUID(productUUID);
        List<String> swtitchNames = new ArrayList();

        for (int i = 0; i < swtitches.size(); i++) {
            swtitchNames.add(swtitches.get(i).getName());
        }

        //开关上限暂时设置到1000方便取名
        int MaxSwitchCount = 1000;
        for (int i = 0; i < MaxSwitchCount; i++) {
            String name = startStr + i;
            if (swtitchNames.contains(name)) {
                continue;
            } else {
                return name;
            }
        }*/
        return startStr;
    }
}
