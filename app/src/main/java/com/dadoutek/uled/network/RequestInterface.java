package com.dadoutek.uled.network;

import com.dadoutek.uled.model.DbModel.DbConnector;
import com.dadoutek.uled.model.DbModel.DbCurtain;
import com.dadoutek.uled.model.DbModel.DbDeleteGradientBody;
import com.dadoutek.uled.model.DbModel.DbDiyGradient;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbSensor;
import com.dadoutek.uled.model.DbModel.DbSensorChild;
import com.dadoutek.uled.model.DbModel.DbSwitch;
import com.dadoutek.uled.model.DbModel.DbSwitchChild;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.model.ResponseVersionAvailable;
import com.dadoutek.uled.network.bean.RegionAuthorizeBean;
import com.dadoutek.uled.region.bean.RegionBean;
import com.dadoutek.uled.region.bean.ShareCodeBean;

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
    @POST("auth/login")
    Observable<Response<DbUser>> login(@Field("account") String account,
                                       @Field("password") String password);


    @FormUrlEncoded
    @POST("auth/forget")
    Observable<Response<DbUser>> putPassword(@Field("account") String account,
                                             @Field("password") String password);

    //获取salt值
    @GET("auth/salt")
    Observable<Response<String>> getsalt(@Query("account") String account);

    @FormUrlEncoded
    @POST("auth/login_SMS")
    Observable<Response<DbUser>> smsLogin(@Field("phone") String phone);


    //用户注册相关接口
    @FormUrlEncoded
    @POST("auth/register")
    Observable<Response<DbUser>> register(@Field("phone") String phone,
                                          @Field("password") String password,
                                          @Field("name") String name);

    //登录蓝牙使用
    @GET("auth/account")
    Observable<Response<String>> getAccount(@Query("phone") String phone, @Query("channel") String channel);

    //区域相关接口

    //添加区域
    @POST("region/add/{rid}")
    Observable<Response<String>> addRegion(@Header("token") String token, @Body DbRegion dbRegion,
                                           @Path("rid") int rid);  //添加区域

    //添加区域
    @POST("region/add/{rid}")
    Observable<Response<Object>> addRegionNew(@Header("token") String token, @Body DbRegion dbRegion,
                                              @Path("rid") long rid);

    // http://47.107.227.130/smartlight/ auth/authorization/code/generate/{rid}
    //https://dev.dadoutek.com/smartlight/
    //获取区域授权码
    @GET("auth/authorization/code/generate/{rid}")
    Observable<Response<ShareCodeBean>> regionAuthorizationCode(@Path("rid") long rid);

    //使一个授权码过期
    //请求URL：DELETE  http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/{rid}/{type}
    @DELETE("auth/authorization/code/remove/{rid}/{type}")
    Observable<Response<ShareCodeBean>> authorizationCodeExpired(@Path("rid") long rid, @Path("type") long type);

    //获取区域列表
    @GET("region/list")
    Observable<Response<List<DbRegion>>> getRegionList(@Header("token") String token);

    //47.获取区域列表 区域activity内使用
    // http://dev.dadoutek.com/smartlight/auth/region/list
    @GET("auth/region/list")
    Observable<Response<List<RegionBean>>> getRegionActivityList();

    //53授权区域列表
    //http://dev.dadoutek.com/smartlight/auth/authorization/authorizer-region/list
    @GET("auth/authorization/authorizer-region/list")
    Observable<Response<List<RegionAuthorizeBean>>> getAuthorizerList();

    //更新区域
    @POST("region/add/{rid}")
    Observable<Response<String>> updateRegion(@Header("token") String token,
                                              @Path("rid") int rid,
                                              @Body DbRegion dbRegion);



    //删除区域
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("region/remove/{rid}")
    Observable<Response<String>> deleteRegion(@Header("token") String token,
                                              @Path("rid") int rid);

    //57、授权码过期
    //使一个授权码过期
    //http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/{rid}/{type}
    //DELETE
    //区域id和码种类type，在url中
    //http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/1/1
    @DELETE("auth/authorization/code/remove/{rid}/{type}")
    Observable<Response<String>> removeAuthorizeCode(@Path("rid") Long rid,@Path("type") int type);

    //6、清除当前区域和其下数据
    //清除当前区域和其下数据，区域本身(除区域一)也会被删除。
    //http://dev.dadoutek.com/smartlight/auth/region/clear
    //DELETE
    @DELETE("auth/region/clear/{rid}")
    Observable<Response<String>> removeRegion(@Path("rid") int rid);

    //组相关接口
    //添加组
    @POST("group/add/{region_id}/{gid}")
    Observable<Response<String>> addGroup(@Header("token") String token,
                                          @Body DbGroup dbGroup,
//                                          @Field("meshAddr") int meshAddr,
//                                          @Field("name") String name,
//                                          @Field("brightness") int brightness,
//                                          @Field("colorTemperature") int colorTemperature,
                                          @Path("region_id") int region_id,
                                          @Path("gid") int gid);

    //获取组列表
    @GET("group/list")
    Observable<Response<List<DbGroup>>> getGroupList(@Header("token") String token);

    //更新组
    @POST("group/add-batch/{rid}")
    Observable<Response<String>> updateGroup(@Header("token") String token,
                                             @Path("rid") int rid,
                                             @Body DbGroup dbGroup
//                                             @Query("name") String name,
//                                             @Query("brightness") int brightness,
//                                             @Query("colorTemperature") int colorTemperature
    );

    //删除组
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("group/remove/{rid}")
    Observable<Response<String>> deleteGroup(@Header("token") String token,
                                             @Path("rid") int rid);

    //灯相关接口

    //添加灯
    @POST("light/add/{lid}")
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
    @GET("light/list")
    Observable<Response<List<DbLight>>> getLightList(@Header("token") String token);

    //更新灯
    @POST("light/add/{lid}")
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
    @DELETE("light/remove/{lid}")
    Observable<Response<String>> deleteLight(@Header("token") String token,
                                             @Path("lid") int lid);

    //场景相关接口

    //添加场景
    @POST("scene/add/{sid}")
    Observable<Response<String>> addScene(@Header("token") String token,
                                          @Body RequestBody body,
                                          @Path("sid") int sid);

    //获取场景列表
    @GET("scene/list")
    Observable<Response<List<DbScene>>> getSceneList(@Header("token") String token);

    //更新场景
    @POST("scene/add/{rid}")
    Observable<Response<String>> updateScene(@Header("token") String token,
                                             @Path("rid") int rid,
                                             @Body RequestBody body);

    //删除场景
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("scene/remove/{sid}")
    Observable<Response<String>> deleteScene(@Header("token") String token,
                                             @Path("sid") int sid);

    //添加渐变
    @POST("dynamic-change/add/{did}")
    Observable<Response<String>> addGradient(@Header("token") String token,
                                             @Body RequestBody body,
                                             @Path("did") int did);

    //获取渐变列表
    @GET("dynamic-change/list")
    Observable<Response<List<DbDiyGradient>>> getGradientList(@Header("token") String token);

    //更新渐变
    @POST("dynamic-change/add/{did}")
    Observable<Response<String>> updateGradient(@Header("token") String token,
                                                @Path("did") int did,
                                                @Body RequestBody body);

    //删除渐变
    @HTTP(method = "DELETE", path = "dynamic-change/remove", hasBody = true)
//    @DELETE("api/ext/soybean/dynamic-changeToScene/remove")
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

    @DELETE("auth/clear")
//    @HTTP(method = "DELETE",path = "dauth/clear",hasBody = false)
    Observable<Response<String>> clearUserData(@Header("token") String token);

    //获取下载链接
    @GET("api/ext/soybean/download/bin/{l1}/{l2}")
    Observable<Response<String>> getFirmwareUrl(@Path("l1") int l1, @Path("l2") int l2);

    //获取下载链接
    @GET("bin/download")
    Observable<Response<Object>> getFirmwareUrlNew(@Query("version") String version);

    //添加开关
    @POST("switch/add/{lid}")
    Observable<Response<String>> addSwitch(@Header("token") String token,
                                           @Body DbSwitchChild dbSwitch,
                                           @Path("lid") int lid);

    //获取开关列表
    @GET("switch/list")
    Observable<Response<List<DbSwitch>>> getSwitchList(@Header("token") String token);

    //更新开关
    @POST("switch/add/{lid}")
    Observable<Response<String>> updateSwitch(@Header("token") String token,
                                              @Path("lid") int lid,
                                              @Body DbSwitchChild dbSwitch
    );

    //删除开关
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("switch/remove/{lid}")
    Observable<Response<String>> deleteSwitch(@Header("token") String token,
                                              @Path("lid") int lid);

    //添加传感器
    @POST("sensor/add/{lid}")
    Observable<Response<String>> addSensor(@Header("token") String token,
                                           @Body DbSensorChild dbSensor,
                                           @Path("lid") int lid);

    //获取传感器列表
    @GET("sensor/list")
    Observable<Response<List<DbSensor>>> getSensorList(@Header("token") String token);

    //更新传感器
    @POST("sensor/add/{lid}")
    Observable<Response<String>> updateSensor(@Header("token") String token,
                                              @Path("lid") int lid,
                                              @Body DbSensorChild dbSensor
    );

    //删除传感器
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("sensor/remove/{lid}")
    Observable<Response<String>> deleteSensor(@Header("token") String token,
                                              @Path("lid") int lid);

    //添加连接器
    @POST("relay/add/{lid}")
    Observable<Response<String>> addRely(@Header("token") String token,
                                         @Body DbConnector dbConnector,
                                         @Path("lid") int lid);

    //获取连接器列表
    @GET("relay/list")
    Observable<Response<List<DbConnector>>> getRelyList(@Header("token") String token);

    //更新连接器
    @POST("relay/add/{lid}")
    Observable<Response<String>> updateRely(@Header("token") String token,
                                            @Path("lid") int lid,
                                            @Body DbConnector dbConnector
    );

    //删除连接器
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("relay/remove/{lid}")
    Observable<Response<String>> deleteRely(@Header("token") String token,
                                            @Path("lid") int lid);

    //添加窗帘
    @POST("curtain/add/{lid}")
    Observable<Response<String>> addCurtain(@Header("token") String token,
                                            @Body DbCurtain dbCurtain,
                                            @Path("lid") int lid);

    //获取窗帘列表
    @GET("curtain/list")
    Observable<Response<List<DbCurtain>>> getCurtainList(@Header("token") String token);

    //更新窗帘
    @POST("curtain/add/{lid}")
    Observable<Response<String>> updateCurtain(@Header("token") String token,
                                               @Path("lid") int lid,
                                               @Body DbCurtain dbCurtain
    );

    //删除窗帘
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("curtain/remove/{lid}")
    Observable<Response<String>> deleteCurtain(@Header("token") String token,
                                               @Path("lid") int lid);

    @GET("app/isAvailable")
//    @HTTP(method = "GET",path = "app/isAvailable",hasBody = true)
    Observable<Response<ResponseVersionAvailable>> isAvailavle(@Query("platform") int device,
                                                               @Query("currentVersion") String version);

    @GET("app/getNewVersion")
    Observable<Response<VersionBean>> getVersion(@Query("currentVersion") String version, @Query("platform") int zero, @Query("lang") int zero_one);

    @GET("auth/isRegister")
    Observable<Response<Object>> isRegister(@Query("phone") String phoneNumber);

    //59、解析码
    //http://dev.dadoutek.com/smartlight/auth/code/parse
    //POST
    //content-type : application/json
    //token:eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MzAwNzI5fQ.YY-872ZqbqZjvCUxJjLyyBj1kbD-Mu2pgq4_2NS47sg (例)
    //code	是	string	码的值
    //{"code":"dadoueyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpemVyX2lkIjoyNjQ0NywicmVnaW9uX2lkIjoxLCJsZXZlbCI6MX0.ys4q7YTbaDD56IaDHUfqJftl86_yFWKHWkgH1zFYwHosmartlight"}
    @POST("auth/code/parse")
    Observable<Response<String>> parseQRCode(@Body String code);
    /**
     * 62、取消授权(主动方:授权者)
     * 取消一个区域对一个用户的授权。如:A授权了区域1给B，B接受了。有一天A不想让B用区域1了，A调用这个接口
     * http://dev.dadoutek.com/smartlight/auth/authorization/cancel/{ref_id}/{rid}
     * DELETE
     * ref_id为被授权者id
     * rid为区域id
     * http://dev.dadoutek.com/smartlight/auth/authorization/cancel/300600/1
     */
    @POST("auth/authorization/cancel/{ref_id}/{rid}")
    Observable<Response<String>> cancelAuthorize(@Path("ref_id") int ref_id,@Path("rid") int rid);

    /**
     * 61、解除授权(主动方:被授权者)
     * 解除授权关系。主动方为被授权者。如：A授权了区域1给B，B接受了。有一天B不想要了。B调用这个接口
     * http://dev.dadoutek.com/smartlight/auth/authorization/release/{authorizer_id}/{rid}
     * DELETE
     * authorizer_id授权用户id
     * rid区域id
     * http://dev.dadoutek.com/smartlight/auth/authorization/release/300430/1
     */
    @DELETE("auth/authorization/release/{authorizer_id}/{rid}")
    Observable<Response<String>> dropAuthorize(@Path("authorizer_id") int authorizer_id,@Path("rid") int rid);

}
