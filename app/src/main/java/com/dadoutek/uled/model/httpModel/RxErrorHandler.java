package com.dadoutek.uled.model.httpModel;

import android.content.Context;

import com.blankj.utilcode.util.LogUtils;
import com.dadoutek.uled.communicate.exception.ApiException;
import com.dadoutek.uled.communicate.exception.BaseException;
import com.dadoutek.uled.communicate.exception.ErrorMessageFactory;
import com.dadoutek.uled.util.TmtUtils;

import org.json.JSONException;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import retrofit2.HttpException;

/**
 * 创建者     ZCL
 * 创建时间   2018/4/4 19:05
 * 描述	      ${一个用于判断异常并给出友好提示的类,我们需要getSTrina资源所以要传入上下文,所以我们要在appComponent提供
 * 在present里面提供}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */
public class RxErrorHandler {

    private static final String TAG = "RxErrorHandler";
    private Context context;

    public RxErrorHandler(Context context) {
        this.context = context;
    }

    public BaseException create(Throwable e) {

        BaseException baseException = new BaseException();
        if (e instanceof ApiException)
            baseException.setCode(((ApiException) e).getCode());

        else if (e instanceof SocketException)//链接
            baseException.setCode(BaseException.SOCKET_ERROR);

        else if (e instanceof SocketTimeoutException)//链接超时
            baseException.setCode(BaseException.SOCKET_TIMEOUT_ERROR);

        else if (e instanceof HttpException)//网络错误
            baseException.setCode(((HttpException) e).code());

        else if (e instanceof JSONException)//解析错误
            baseException.setCode(BaseException.JSON_ERROR);

        else
            baseException.setCode(BaseException.UNKNOWN_ERROR);//未知错误

        //创建友好错误提示工厂
        baseException.setDisplayMessage(ErrorMessageFactory.create(context, baseException.getCode()));

        LogUtils.d(TAG,"返回数据是------:"+e.getMessage());
        return baseException;
    }

    //进行友好提示
    public void showErrorMessage(BaseException e) {
        TmtUtils.midToast(context, e.getDisplayMessage());
    }
}
