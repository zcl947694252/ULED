package com.dadoutek.uled.util;

import android.text.InputFilter;
import android.widget.EditText;

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
}
