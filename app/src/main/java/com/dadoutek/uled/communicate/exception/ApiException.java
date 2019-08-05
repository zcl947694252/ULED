package com.dadoutek.uled.communicate.exception;

/**
 * 创建者     ZCL
 * 创建时间   2018/4/4 17:58
 * 描述	      ${自定义异常类}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */

public class ApiException extends BaseException {
    public ApiException(int code, String displayMessage) {
        super(code, displayMessage);
    }
}
