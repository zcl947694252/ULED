package com.dadoutek.uled.communicate.exception;

import android.content.Context;

import com.dadoutek.uled.R;


/**
 * 创建者     ZCL
 * 创建时间   2018/4/4 19:20
 * 描述	      ${一个根据code判断友好提示的工厂 因为要获取资源所以要上下文}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */

public class ErrorMessageFactory {


    public static String create(Context context, int code) {

        String errorMsg ="";

        switch (code){

            case BaseException.HTTP_ERROR:
                errorMsg =  context.getResources().getString(R.string.error_http);
                break;

            case BaseException.SOCKET_TIMEOUT_ERROR:

                errorMsg =  context.getResources().getString(R.string.error_socket_timeout);

                break;
            case BaseException.SOCKET_ERROR:

                errorMsg =  context.getResources().getString(R.string.error_socket_unreachable);

                break;


            case BaseException.ERROR_HTTP_400:

                errorMsg =  context.getResources().getString(R.string.error_http_400);

                break;


            case BaseException.ERROR_HTTP_404:

                errorMsg =  context.getResources().getString(R.string.error_http_404);

                break;

            case BaseException.ERROR_HTTP_500:

                errorMsg =  context.getResources().getString(R.string.error_http_500);

                break;



            case ApiException.ERROR_API_SYSTEM:
                errorMsg = context.getResources().getString(R.string.error_system);
                break;

            case ApiException.ERROR_API_NO_PERMISSION:
                errorMsg = context.getResources().getString(R.string.error_api_no_perission);
                break;

            default:
                errorMsg=context.getResources().getString(R.string.error_unkown);
                break;
        }
        return errorMsg;
    }
}
