package com.dadoutek.uled.intf;

import com.dadoutek.uled.DbModel.DbGroup;
import com.dadoutek.uled.DbModel.DbLight;
import com.dadoutek.uled.DbModel.DbRegion;
import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.DbModel.DbUser;
import com.dadoutek.uled.model.Response;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Created by hejiajun on 2018/5/16.
 */

public interface RequestInterface {

    //用户登录接口
    @FormUrlEncoded
    @POST("api/auth/login")
    Observable<Response<DbUser>> login(@Field("account") String account,
                                       @Field("password") String password);

    @GET("api/auth/salt")
    Observable<Response<String>> getsalt(@Query("account") String account);

    @GET("api/auth/account")
    Observable<Response<String>> getAccount(@Query("phone") String phone, @Query("channel") String channel);

    @FormUrlEncoded
    @POST("api/ext/soybean/forget")
    Observable<Response<DbUser>> putPassword(@Field("account") String account,
                                             @Field("password") String password);

    //用户注册相关接口
    @FormUrlEncoded
    @POST("api/ext/soybean/register")
    Observable<Response<DbUser>> register(@Field("phone") String phone,
                                          @Field("password") String password,
                                          @Field("name") String name);

    //区域相关接口

    //添加区域
    @FormUrlEncoded
    @POST("api/ext/soybean/region/add")
    Observable<Response<Void>> addRegion(@Field("token") String token,
                                         @Field("controlMesh") String controlMesh,
                                         @Field("installMesh") String installMesh,
                                         @Field("installMeshPwd") String installMeshPwd);

    //获取区域列表
    @GET("api/ext/soybean/region/list")
    Observable<Response<List<DbRegion>>> getRegionList(@Query("token") String token);

    //更新区域
    @PUT("api/ext/soybean/region/update")
    Observable<Response<Void>> updateRegion(@Query("token") String token,
                                            @Query("rid") int rid,
                                            @Query("controlMesh") String controlMesh,
                                            @Query("controlMeshPwd") String controlMeshPwd,
                                            @Query("installMesh") String installMesh,
                                            @Query("installMeshPwd") String installMeshPwd);

    //删除区域
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/region/remove")
    Observable<Response<Void>> deleteRegion(@Query("token") String token,
                                            @Query("rid") int rid);

    //组相关接口

    //添加组
    @FormUrlEncoded
    @POST("api/ext/soybean/group/add")
    Observable<Response<Void>> addGroup(@Field("token") String token,
                                        @Field("meshAddr") int meshAddr,
                                        @Field("name") String name,
                                        @Field("brightness") int brightness,
                                        @Field("colorTemperature") int colorTemperature);

    //获取组列表
    @GET("api/ext/soybean/group/list")
    Observable<Response<List<DbGroup>>> getGroupList(@Query("token") String token);

    //更新组
    @PUT("api/ext/soybean/region/group/update")
    Observable<Response<Void>> updateGroup(@Query("token") String token,
                                           @Query("rid") int rid,
                                           @Query("name") String name,
                                           @Query("brightness") int brightness,
                                           @Query("colorTemperature") int colorTemperature);

    //删除组
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/group/remove")
    Observable<Response<Void>> deleteGroup(@Query("token") String token,
                                           @Query("rid") int rid);

    //灯相关接口

    //添加灯
    @FormUrlEncoded
    @POST("api/ext/soybean/light/add")
    Observable<Response<Void>> addLight(@Field("token") String token,
                                        @Field("meshAddr") int meshAddr,
                                        @Field("name") String name,
                                        @Field("brightness") int brightness,
                                        @Field("colorTemperature") int colorTemperature,
                                        @Field("macAddr") String macAddr,
                                        @Field("meshUUID") int meshUUID,
                                        @Field("productUUID") int productUUID,
                                        @Field("belongGroupId") int belongGroupId);

    //获取灯列表
    @GET("api/ext/soybean/light/list")
    Observable<Response<List<DbLight>>> getLightList(@Query("token") String token);

    //更新灯
    @PUT("api/ext/soybean/region/light/update")
    Observable<Response<Void>> updateLight(@Query("token") String token,
                                           @Query("rid") int rid,
                                           @Query("name") String name,
                                           @Query("brightness") int brightness,
                                           @Query("colorTemperature") int colorTemperature,
                                           @Query("belongGroupId") int belongGroupId);

    //删除灯
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/light/remove")
    Observable<Response<Void>> deleteLight(@Query("token") String token,
                                           @Query("rid") int rid);

    //场景相关接口

    //添加场景
    @FormUrlEncoded
    @POST("api/ext/soybean/scene/add")
    Observable<Response<Void>> addScene(@Field("token") String token,
                                        @Field("name") String name,
                                        @Field("actions") List<String> actions,
                                        @Field("belongRegionId") int belongRegionId);

    //获取场景列表
    @GET("api/ext/soybean/scene/list")
    Observable<Response<List<DbScene>>> getSceneList(@Query("token") String token);

    //更新场景
    @PUT("api/ext/soybean/region/scene/update")
    Observable<Response<Void>> updateScene(@Query("token") String token,
                                           @Query("rid") int rid,
                                           @Query("name") String name,
                                           @Query("actions") List<String> actions);

    //删除场景
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/scene/remove")
    Observable<Response<Void>> deleteScene(@Query("token") String token,
                                           @Query("rid") int rid);

    //获取用户信息
    @GET("api/user/info/mine")
    Observable<Response<DbUser>> getUserInfo(@Query("token") String token);

//    //修改用户信息
//    @PUT("api/user/update")
//    Observable<Response<Void>> updateScene(@Query("token") String token,
//                                           @Query("rid") int rid,
//                                           @Query("name") String name,
//                                           @Query("actions") List<String> actions);
}
