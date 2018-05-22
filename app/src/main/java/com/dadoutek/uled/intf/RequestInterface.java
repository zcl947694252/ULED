package com.dadoutek.uled.intf;

import com.dadoutek.uled.DbModel.DbRegion;
import com.dadoutek.uled.DbModel.DbUser;
import com.dadoutek.uled.model.Response;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    @FormUrlEncoded
    @POST("api/ext/soybean/forget")
    Observable<Response<DbUser>> putPassword(@Field("account") String account, @Field("password") String password);

    //用户注册相关接口
    @FormUrlEncoded
    @POST("api/ext/soybean/register")
    Observable<Response<DbUser>> register(@Field("phone") String phone, @Field("password")
            String password, @Field("name") String name);

    //区域相关接口
    @POST("api/ext/soybean/region/add")
    Observable<Response<DbRegion>> addRegion(@Field("token") String token, @Field("controlMesh") String controlMesh,
                                             @Field("installMesh") String installMesh, @Field("installMeshPwd") String installMeshPwd);

    @GET("api/ext/soybean/region/list")
    Observable<Response<DbRegion>> getRegionList(@Query("token") String token);
}
