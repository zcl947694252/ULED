package com.dadoutek.uled.network.bean;

import java.io.Serializable;

/**
 * 创建者     ZCL
 * 创建时间   2019/7/25 16:38
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class BaseBean<T> implements Serializable {
    private T  data;
    private int errorCode;
    private String message;
    private long serverTime;
}
