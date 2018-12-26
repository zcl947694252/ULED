package com.dadoutek.uled.network;

import com.dadoutek.uled.model.DbModel.DbDeleteGradientBody;
import com.dadoutek.uled.model.DbModel.DbDiyGradient;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.model.Response;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
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
    @POST("api/ext/soybean/region/add/{rid}")
    Observable<Response<String>> addRegion(@Header("token") String token,
                                           @Body DbRegion dbRegion,
//                                           @Field("controlMesh") String controlMesh,
//                                           @Field("controlMeshPwd") String controlMeshPwd,
//                                           @Field("installMesh") String installMesh,
//                                           @Field("installMeshPwd") String installMeshPwd,
                                           @Path("rid") int rid);

    //获取区域列表
    @GET("api/ext/soybean/region/list")
    Observable<Response<List<DbRegion>>> getRegionList(@Header("token") String token);

    //更新区域
    @PUT("api/ext/soybean/region/update/{rid}")
    Observable<Response<String>> updateRegion(@Header("token") String token,
                                              @Path("rid") int rid,
                                              @Body DbRegion dbRegion
//                                              @Query("controlMesh") String controlMesh,
//                                              @Query("controlMeshPwd") String controlMeshPwd,
//                                              @Query("installMesh") String installMesh,
//                                              @Query("installMeshPwd") String installMeshPwd
    );

    //删除区域
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/region/remove")
    Observable<Response<String>> deleteRegion(@Header("token") String token,
                                              @Path("rid") int rid);

    //组相关接口

    //添加组
    @POST("api/ext/soybean/group/add/{region_id}/{gid}")
    Observable<Response<String>> addGroup(@Header("token") String token,
                                          @Body DbGroup dbGroup,
//                                          @Field("meshAddr") int meshAddr,
//                                          @Field("name") String name,
//                                          @Field("brightness") int brightness,
//                                          @Field("colorTemperature") int colorTemperature,
                                          @Path("region_id") int region_id,
                                          @Path("gid") int gid);

    //获取组列表
    @GET("api/ext/soybean/group/list")
    Observable<Response<List<DbGroup>>> getGroupList(@Header("token") String token);

    //更新组
    @PUT("api/ext/soybean/group/update/{rid}")
    Observable<Response<String>> updateGroup(@Header("token") String token,
                                             @Path("rid") int rid,
                                             @Body DbGroup dbGroup
//                                             @Query("name") String name,
//                                             @Query("brightness") int brightness,
//                                             @Query("colorTemperature") int colorTemperature
    );

    //删除组
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/group/remove/{rid}")
    Observable<Response<String>> deleteGroup(@Header("token") String token,
                                             @Path("rid") int rid);

    //灯相关接口

    //添加灯
    @POST("api/ext/soybean/light/add/{lid}")
    Observable<Response<String>> addLight(@Header("token") String token,
                                          @Body DbLight dbLight,
//                                          @Field("meshAddr") int meshAddr,
//                                          @Field("name") String name,
//                                          @Field("brightness") int brightness,
//                                          @Field("colorTemperature") int colorTemperature,
//                                          @Field("macAddr") String macAddr,
//                                          @Field("meshUUID") int meshUUID,
//                                          @Field("productUUID") int productUUID,
//                                          @Field("belongGroupId") int belongGroupId,
                                          @Path("lid") int lid);

    //获取灯列表
    @GET("api/ext/soybean/light/list")
    Observable<Response<List<DbLight>>> getLightList(@Header("token") String token);

    //更新灯
    @PUT("api/ext/soybean/light/update/{lid}")
    Observable<Response<String>> updateLight(@Header("token") String token,
                                             @Path("lid") int lid,
                                             @Body DbLight dbLight
//                                             @Query("name") String name,
//                                             @Query("brightness") int brightness,
//                                             @Query("colorTemperature") int colorTemperature,
//                                             @Query("belongGroupId") int belongGroupId
    );

    //删除灯
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/light/remove/{lid}")
    Observable<Response<String>> deleteLight(@Header("token") String token,
                                             @Path("lid") int lid);

    //场景相关接口

    //添加场景
    @POST("api/ext/soybean/scene/add/{sid}")
    Observable<Response<String>> addScene(@Header("token") String token,
                                          @Body RequestBody body,
                                          @Path("sid") int sid);

    //获取场景列表
    @GET("api/ext/soybean/scene/list")
    Observable<Response<List<DbScene>>> getSceneList(@Header("token") String token);

    //更新场景
    @PUT("api/ext/soybean/scene/update/{rid}")
    Observable<Response<String>> updateScene(@Header("token") String token,
                                             @Path("rid") int rid,
                                             @Body RequestBody body);

    //删除场景
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("api/ext/soybean/scene/remove/{rid}")
    Observable<Response<String>> deleteScene(@Header("token") String token,
                                             @Path("rid") int rid);

    //添加渐变
    @POST("api/ext/soybean/dynamic-change/{did}")
    Observable<Response<String>> addGradient(@Header("token") String token,
                                          @Body RequestBody body,
                                          @Path("did") int did);

    //获取渐变列表
    @GET("api/ext/soybean/dynamic-change/list")
    Observable<Response<List<DbDiyGradient>>> getGradientList(@Header("token") String token);

    //更新渐变
    @PUT("api/ext/soybean/dynamic-change/update/{did}")
    Observable<Response<String>> updateGradient(@Header("token") String token,
                                             @Path("rid") int rid,
                                             @Body RequestBody body);

    //删除渐变
    @HTTP(method = "DELETE",path = "api/ext/soybean/dynamic-change/remove",hasBody = true)
//    @DELETE("api/ext/soybean/dynamic-change/remove")
    Observable<Response<String>> deleteGradients(@Header("token") String token,
                                                 @Body DbDeleteGradientBody body);

    //获取用户信息
    @GET("api/user/info/mine")
    Observable<Response<DbUser>> getUserInfo(@Header("token") String token);

    //修改用户信息
    @PUT("api/user/update")
    Observable<Response<String>> updateUser(@Header("token") String token,
                                            @Query("avatar") String avatar,
                                            @Query("name") String name,
                                            @Query("email") String email,
                                            @Query("introduction") String introduction);

    //获取场景列表
    @POST("api/ext/soybean/clear")
    Observable<Response<String>> clearUserData(@Header("token") String token);

    //获取下载链接
    @GET("api/ext/soybean/download/bin/{l1}/{l2}")
    Observable<Response<String>> getFirmwareUrl(@Path("l1") int l1,@Path("l2") int l2);
}
