package com.dadoutek.uled.intf;

import com.dadoutek.uled.DbModel.DbUser;
import com.dadoutek.uled.model.Response;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Created by hejiajun on 2018/5/16.
 */

public interface RequestInterface {
    //用户登录接口
    @FormUrlEncoded
    @POST("api/auth/login")
    Observable<Response<DbUser>> login(@Field("account") String account, @Field("password") String password);
    @GET("api/auth/salt")
    Observable<Response<String>> getsalt(@Query("account") String account);
    @GET("api/auth/account")
    Observable<Response<String>> getAccount(@QueryMap Map<String, String> params);

    //用户注册相关接口
    @FormUrlEncoded
    @POST("api/ext/soybean/register")
    Observable<Response<DbUser>> register(@Field("phone") String phone, @Field("password")
            String password, @Field("name") String name);
}
