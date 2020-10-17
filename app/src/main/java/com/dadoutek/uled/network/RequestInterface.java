package com.dadoutek.uled.network;

import com.dadoutek.uled.gateway.bean.ClearGwBean;
import com.dadoutek.uled.gateway.bean.DbGateway;
import com.dadoutek.uled.gateway.bean.DbRouter;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.model.ResponseVersionAvailable;
import com.dadoutek.uled.model.dbModel.DbConnector;
import com.dadoutek.uled.model.dbModel.DbCurtain;
import com.dadoutek.uled.model.dbModel.DbDeleteGradientBody;
import com.dadoutek.uled.model.dbModel.DbDiyGradient;
import com.dadoutek.uled.model.dbModel.DbGroup;
import com.dadoutek.uled.model.dbModel.DbLight;
import com.dadoutek.uled.model.dbModel.DbRegion;
import com.dadoutek.uled.model.dbModel.DbScene;
import com.dadoutek.uled.model.dbModel.DbSensor;
import com.dadoutek.uled.model.dbModel.DbSensorChild;
import com.dadoutek.uled.model.dbModel.DbSwitch;
import com.dadoutek.uled.model.dbModel.DbSwitchChild;
import com.dadoutek.uled.model.dbModel.DbUser;
import com.dadoutek.uled.model.httpModel.BatchRemove8kBody;
import com.dadoutek.uled.model.httpModel.RemoveCodeBody;
import com.dadoutek.uled.model.routerModel.RouteStasusBean;
import com.dadoutek.uled.model.routerModel.UpdateGradientBean;
import com.dadoutek.uled.network.bean.RegionAuthorizeBean;
import com.dadoutek.uled.network.bean.TransferRegionBean;
import com.dadoutek.uled.region.RegionBcBean;
import com.dadoutek.uled.region.bean.ParseCodeBean;
import com.dadoutek.uled.region.bean.RegionBean;
import com.dadoutek.uled.region.bean.ShareCodeBean;
import com.dadoutek.uled.region.bean.TransferBean;
import com.dadoutek.uled.rgb.AddGradientBean;
import com.dadoutek.uled.router.DelGradientBodyBean;
import com.dadoutek.uled.router.GroupBlinkBodyBean;
import com.dadoutek.uled.router.SceneAddBodyBean;
import com.dadoutek.uled.router.bean.MacResetBody;
import com.dadoutek.uled.router.bean.RouteScanResultBean;
import com.dadoutek.uled.router.bean.RouterBatchGpBean;
import com.dadoutek.uled.router.bean.RouterVersionsBean;
import com.dadoutek.uled.router.bean.ScanDataBean;
import com.dadoutek.uled.switches.RouterListBody;

import java.util.List;
import java.util.Map;

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
 * 创建者     zcl
 * 创建时间   2019/8/7 11:54
 * 描述	      ${
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${使用post传递body参数一种是使用@body传递封装的Requestbody对象 一种是使用@FormUrlEncoded+@Field表单模式}$
 */

public interface RequestInterface {

    //用户登录接口
    @FormUrlEncoded
    @POST("auth/login")
    Observable<Response<DbUser>> login(@Field("account") String account,
                                       @Field("password") String password,
                                       @Field("nativePassword") String nativePassword);


    @FormUrlEncoded
    @POST("auth/forget")
    Observable<Response<DbUser>> putPassword(@Field("account") String account,
                                             @Field("password") String password);

    //获取salt值
    @GET("auth/salt")
    //@Headers("Content-Type:application/text; charset=utf-8")
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
    Observable<Response<String>> getAccount(@Query("phone") String phone,
                                            @Query("channel") String channel);

    //区域相关接口

    //添加区域
    @POST("region/add/{rid}")
    Observable<Response<String>> addRegion(@Header("token") String token, @Body DbRegion dbRegion,
                                           @Path("rid") int rid);  //添加区域

    //添加区域
    @POST("region/add/{rid}")
    Observable<Response<Object>> addRegionNew(@Header("token") String token,
                                              @Body DbRegion dbRegion,
                                              @Path("rid") long rid);

    // http://47.107.227.130/smartlight/ auth/authorization/code/generate/{rid}
    //https://dev.dadoutek.com/smartlight/
    //获取区域授权码
    @GET("auth/authorization/code/generate/{rid}")
    Observable<Response<ShareCodeBean>> regionAuthorizationCode(@Path("rid") long rid);

    //使一个授权码过期
    //请求URL：DELETE  http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/{rid}/{type}
    @DELETE("auth/authorization/code/remove/{rid}/{type}")
    Observable<Response<ShareCodeBean>> authorizationCodeExpired(@Path("rid") long rid, @Path(
            "type") long type);

    //获取区域列表 (已过期) 仅用于获取自己的区域
    @GET("region/list")
    Observable<Response<List<DbRegion>>> getOldRegionList(@Header("token") String token);

    //8、获取单个区域(又有改动) region/get/300975/1
    @GET("region/get/{uid}/{rid}")
    Observable<Response<DbRegion>> getRegionInfo(@Path("uid") String uid, @Path("rid") String rid);

    //47.获取区域列表 区域activity内使用
    // http://dev.dadoutek.com/smartlight/auth/region/list
    @GET("auth/region/list")
    Observable<Response<List<RegionBean>>> gotRegionActivityList();

    //53授权区域列表
    //http://dev.dadoutek.com/smartlight/auth/authorization/authorizer-region/list
    @GET("auth/authorization/authorizer-region/list")
    Observable<Response<List<RegionAuthorizeBean>>> gotAuthorizerList();

    /**
     * 添加/更新一个区域 http://dev.dadoutek.com/smartlight/region/add/{rid}  POST 更新区域
     * installMesh	否	string	默认mesh，目前固定dadousmart
     * installMeshPwd	否	string	默认meshPassword,目前固定123
     * name	否	string	区域的名称。默认”未命名区域”
     * lastGenMeshAddr	否	int	区域mesh累加, 不传默认0, 0不会进行处理
     * "installMesh":"dadousmart",
     * "installMeshPwd":"123456",
     * "name":"a region",
     * "lastGenMeshAddr": 0
     */
    @POST("region/add/{rid}")
    Observable<Response<RegionBcBean>> updateRegion(@Path("rid") int rid,
                                                    @Body DbRegion dbRegion);


    //删除区域
    //    @HTTP(method = "DELETE", path = "api/ext/soybean/region/remove", hasBody = true)
    @DELETE("region/remove/{rid}")
    Observable<Response<String>> deleteRegion(@Header("token") String token,
                                              @Path("rid") int rid);

    //57、授权码过期
    //使一个授权码过期
    //http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/{rid}/{type}
    //区域id和码种类type，在url中
    //http://dev.dadoutek.com/smartlight/auth/authorization/code/remove/1/1
    @DELETE("auth/authorization/code/remove/{rid}/{type}")
    Observable<Response<String>> removeAuthorizeCode(@Path("rid") Long rid,
                                                     @Path("type") int type); //57、授权码过期

    /**
     * 58、移交码过期
     * 使一个移交码过期
     * http://dev.dadoutek.com/smartlight/auth/transfer/code/remove
     * DELETE
     */
    @DELETE("auth/transfer/code/remove")
    Observable<Response<String>> removeTransferCode();

    //6、清除当前区域和其下数据
    //清除当前区域和其下数据，区域本身(除区域一)也会被删除。
    //http://dev.dadoutek.com/smartlight/auth/region/clear
    //DELETE

    @DELETE("auth/region/clear/{rid}")
    Observable<Response<String>> clearRegion(@Path("rid") int rid);

    //6.5.删除区域
    //删除一个区域
    //http://dev.dadoutek.com/smartlight/auth/region/remove/{rid}
    //rid：区域id
    //请求方式：DELETE
    @DELETE("auth/region/remove/{rid}")
    Observable<Response<String>> removeRegion(@Path("rid") int rid);

    //组相关接口
    //添加组
    //@POST("group/add/{region_id}/{gid}")
    @POST("group/add/{gid}")
    Observable<Response<String>> addGroup(/*@Header("token") String token,*/
            @Body DbGroup dbGroup,
            //                                          @Field
            //                                          ("meshAddr") int meshAddr,
            //                                          @Field
            //                                          ("name")
            //                                          String name,
            //                                          @Field
            //                                          ("brightness") int brightness,
            //                                          @Field
            //                                          ("colorTemperature") int
            //                                          colorTemperature,
            //@Path("region_id") int region_id,
            @Path("gid") int gid);

    /**
     * 批量添加组  http://dev.dadoutek.com/smartlight/group/add-batch POST
     * groups	是	数组	多个组
     * { groups:[{"id":1, "meshAddr":1,  "name":"group10","brightness":1, "colorTemperature":1,
     * "color":1,"index":1, "deviceType":4,"status":1, "slowUpSlowDownStatus": 0,
     * "slowUpSlowDownSpeed": 1 } ]}
     */
    //@FormUrlEncoded
    @POST("group/add-batch")
    Observable<Response> batchUpGroupList(@Body GroupListBodyBean  bean/*@Field("groups") List<DbGroup> listGp*/);

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
                                          //                                          @Field
                                          //                                          ("meshAddr") int meshAddr,
                                          //                                          @Field
                                          //                                          ("name")
                                          //                                          String name,
                                          //                                          @Field
                                          //                                          ("brightness") int brightness,
                                          //                                          @Field
                                          //                                          ("colorTemperature") int
                                          //                                          colorTemperature,
                                          //                                          @Field
                                          //                                          ("macAddr")
                                          //                                          String
                                          //                                          macAddr,
                                          //                                          @Field
                                          //                                          ("meshUUID") int meshUUID,
                                          //                                          @Field
                                          //                                          ("productUUID") int productUUID,
                                          //                                          @Field
                                          //                                          ("belongGroupId") int belongGroupId,
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

    /**
     * https://dev.dadoutek.com/xxxx/auth/reset/{rid}
     *
     * @param regionId
     * @return
     */
    @GET("auth/reset/{rid}")
    Observable<Response<String>> clearUserRegionData(@Path("rid") int regionId);


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
        //    @HTTP(method = "GET",path = "app/isAvailable",hasBody = true)   todo 此处报错  用户不存在
    Observable<Response<ResponseVersionAvailable>> isAvailavle(@Query("platform") int device,
                                                               @Query("currentVersion") String version);

    //用于检测是都注册
    @GET("auth/isRegister")
    Observable<Response<Boolean>> isRegister(@Query("phone") String phoneNumber);

    //59、解析码
    //http://dev.dadoutek.com/smartlight/auth/code/parse
    //POST
    //content-type : application/json
    //token:eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MzAwNzI5fQ.YY-872ZqbqZjvCUxJjLyyBj1kbD-Mu2pgq4_2NS47sg (例)
    //code	是	string	码的值
    //password	是	string
    //{"code":"dadoueyJhbGciOiJIUzI1NiJ9
    // .eyJhdXRob3JpemVyX2lkIjoyNjQ0NywicmVnaW9uX2lkIjoxLCJsZXZlbCI6MX0
    // .ys4q7YTbaDD56IaDHUfqJftl86_yFWKHWkgH1zFYwHosmartlight"}

    @FormUrlEncoded
    @POST("auth/code/parse")
    Observable<Response<ParseCodeBean>> parseQRCode(@Field("code") String code,
                                                    @Field("password") String password);

    /**
     * 62、取消授权(主动方:授权者)
     * 取消一个区域对一个用户的授权。如:A授权了区域1给B，B接受了。有一天A不想让B用区域1了，A调用这个接口
     * http://dev.dadoutek.com/smartlight/auth/authorization/cancel/{ref_id}/{rid}
     * DELETE
     * ref_id为被授权者id
     * rid为区域id
     * http://dev.dadoutek.com/smartlight/auth/authorization/cancel/300600/1
     */
    @DELETE("auth/authorization/cancel/{ref_id}/{rid}")
    Observable<Response<String>> cancelAuthorize(@Path("ref_id") int ref_id, @Path("rid") int rid);

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
    Observable<Response<String>> dropAuthorize(@Path("authorizer_id") int authorizer_id, @Path(
            "rid") int rid);

    /**
     * 56、生成移交码
     * http://dev.dadoutek.com/smartlight/auth/transfer/code/generate
     * GET
     */
    @GET("auth/transfer/code/generate")
    Observable<Response<TransferBean>> makeTransferCode();

    /**
     * 59、获取区域授权码信息(新增)
     * http://dev.dadoutek.com/smartlight/auth/authorization/code/info/{rid}
     * GET
     * http://dev.dadoutek.com/smartlight/auth/authorization/code/info/1
     */
    @GET("auth/authorization/code/info/{rid}")
    Observable<Response<ShareCodeBean>> mlookAuthroizeCode(@Path("rid") Long rid);

    /**
     * 60、获取用户移交码信息(新增)
     * http://dev.dadoutek.com/smartlight/auth/transfer/code/info
     * GET
     */
    @GET("auth/transfer/code/info")
    Observable<Response<TransferBean>> mGetTransferCode();

    /**
     * 1、生成区域移交码（new）
     * https://dev.dadoutek.com/smartlight_java/auth/transfer/code/generate/{rid}
     * GET
     */
    @GET("auth/transfer/code/generate/{rid}")
    Observable<Response<TransferRegionBean>> mGetTransferRegionQR(@Path("rid") Long rid);

    /**
     * 2 获取区域移交码信息
     * https://dev.dadoutek.com/smartlight_java/auth/transfer/code/info/{rid}
     * GET
     */
    @GET("auth/transfer/code/info/{rid}")
    Observable<Response<TransferRegionBean>> mlookRegionTransferCodeBean(@Path("rid") Long regionId);


    /**
     * 删除一个区域的移交码
     * https://dev.dadoutek.com/smartlight_java/auth/transfer/code/remove/{rid}
     * DELETE
     * content-type : application/json
     * token:eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MzAwNzI5fQ.YY-872ZqbqZjvCUxJjLyyBj1kbD-Mu2pgq4_2NS47sg (例)
     * https://dev.dadoutek.com/smartlight_java/auth/transfer/code/remove/1
     */
    @DELETE("auth/transfer/code/remove/{rid}")
    Observable<Response<TransferRegionBean>> removeTransferRegionCode(@Path("rid") Long rid);

    /**
     * 三码删除统一接口，可以通过code删除
     * * https://dev.dadoutek.com/smartlight_java/auth/code/remove
     * * DELETE
     * * code string
     */
    @HTTP(method = "DELETE", path = "auth/code/remove", hasBody = true)
    //@DELETE("auth/code/remove")
    //添加数据进body 少量参数可以使用field
    Observable<Response> allCodeRemove(@Body() RemoveCodeBody body);

    /**
     * 60、获取区域controlMesh列表
     * https://dev.dadoutek.com/smartlight_java/region/list/controlMesh
     * GET
     * controlMesh字符串数组
     */
    @GET("region/list/controlMesh")
    Observable<Response<List<String>>> regionNameList();


    /**
     * 42、获取最新版本       GET
     * 获取app最新版本，app更新升级检查
     * http://dev.dadoutek.com/smartlight/app/getNewVersion
     * currentVersion	是	string	当前app版本，满足x.x.x
     * platform	是	int	平台标识，安卓0，ios1
     * lang	否	int	语言标识，中文0，英文1。后台默认0
     */
    @GET("app/getNewVersion")
    Observable<Response<VersionBean>> haveNewVerison(@Query("currentVersion") String currentVersion
            , @Query("platform") int AndroidZero, @Query("lang") int zhOrEnglish);

    /**
     * 6、添加/更新八键开关（new） POST
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/add/{swid}
     * firmwareVersion	否	   string	固件版本号
     * meshAddr	    是	   int	    mesh地址
     * name	       是	 string	   名字
     * macAddr	      是	string	   mac地址
     * productUUID	 是	    int	productUUID
     * index	否	int	排序
     * keys	是	list or string	key数组或者json格式的字符串
     * key既可以是对象数组，也可以是json格式的字符串。
     */
    @FormUrlEncoded
    @POST("switch/8ks/add/{swid}")
    Observable<Response<String>> addSwitch8k(@Path("swid") Long swid,
                                             @Field("firmwareVersion") String firmwareVersion,
                                             @Field("meshAddr") int meshAddr,
                                             @Field("name") String name,
                                             @Field("macAddr") String macAddr, @Field(
            "productUUID") int productUUID, @Field(
            "index") int index,
                                             @Field("keys") String keys);

    /**
     * 7、批量添加/更新八键开关（new）  POST
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/add-batch
     * eightKeySwitches	是	list	八键开关数组
     */
    @FormUrlEncoded
    @POST("switch/8ks/add-batch")
    Observable<Response<String>> batchAdd8kSwitch(@Field("eightKeySwitches") List<DbSwitch> eightKeySwitches);

    /**
     * 8、获取八键开关列表（new） GET
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/list
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/list?isKeySerialized=true
     * isKeySerialized	否	boolean（其实是string）	是否序列化八键开关的keys。默认true（会序列化）
     */
    @GET("switch/8ks/list")
    Observable<Response<List<DbSwitch>>> getSwitch8kList(@Query("isKeySerialized") boolean isKeySerialized);

    /**
     * 9、删除一个八键开关（new）
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/remove/{swid}
     * DELETE
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/remove/1
     */
    @DELETE("switch/8ks/remove/{swid}")
    Observable<Response<String>> removeSwitch8k(@Path("swid") Long swid);

    /**
     * 10、批量删除八键开关（new）
     * https://dev.dadoutek.com/smartlight_java/switch/8ks/remove
     * DELETE
     * idList	是	list	id数组
     * 传参示例
     * {
     * "idList": [1, 2, 3]
     * }
     */
    @HTTP(method = "DELETE", path = "switch/8ks/remove", hasBody = true)
    Observable<Response<String>> removeSwitch8kList(@Body BatchRemove8kBody body);

    /**
     * 6、添加网关（new）
     * 简要描述：添加一条网关信息
     * https://dev.dadoutek.com/xxxx/gateway/add/{gatewayId}
     * 请求方式： POST
     * 参数：
     * meshAddr	是	int	mesh地址
     * name	否	string	网关名，后台默认未命名
     * type	否	byte/number	网关类型，后台默认0
     * macAddr	是	string	设备mac地址
     * productUUID	是	int	设备productUUID
     * version	否	string	版本号，后台默认空串
     * tags	是	string	json格式字符串
     */
    @POST("gateway/add/{gatewayId}")
    Observable<Response<DbGateway>> addGw(@Path("gatewayId") long gwId, @Body DbGateway dbGateway);

    /**
     * 7、网关列表（new）查询网关列表
     * 请求URL： https://dev.dadoutek.com/xxxx/gateway/list
     * 正式服 smartlight_java 替换xxxx
     * 测试服 smartlight_test 替换xxxx
     * 请求方式： GET
     */
    @GET("gateway/list")
    Observable<Response<List<DbGateway>>> getGWList();

    /**
     * 8、删除网关（new）删除多条网关信息
     * 请求URL： https://dev.dadoutek.com/xxxx/gateway/delete
     * 正式服 smartlight_java 替换xxxx
     * 测试服 smartlight_test 替换xxxx
     * 请求方式：DELETE
     * idlist	是	int数组	需要删除的网关id
     */
    @HTTP(method = "DELETE", path = "gateway/delete", hasBody = true)
    Observable<Response<String>> deleteGw(@Body GwGattBody body);

    /**
     * 9、下发标签给网关（new）
     * 简要描述：添加一条网关信息
     * https://dev.dadoutek.com/xxxx/mqtt/tag/pub
     * 请求方式：POST
     * 参数：
     * macAddr	是	string	mac地址
     * cmd	是	int	指令
     * data	是	string	data
     */
    @POST("mqtt/tag/pub")
    Observable<Response<String>> sendGwToService(@Body GwGattBody body);

    /**
     * 10、下发控制指令让网关转发（new）
     * 简要描述：下发一条控制指令，网关接受后转发至对应的mesh网络
     * 请求URL： https://dev.dadoutek.com/xxxx/mqtt/control
     * 请求方式 POST
     * content-type : application/json
     * token:eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MzAwNzI5fQ.YY-872ZqbqZjvCUxJjLyyBj1kbD-Mu2pgq4_2NS47sg (例)
     * 参数名	必选	类型	说明
     * data	是	string	byte[]经过Base64编码得到的字符串
     * ser_id	是	string	会话id，推送中回传
     * cmd	是	int	操作类型，推送中回传
     * meshAddr	是	int	设备or组的mesh地址，推送中回传
     */
    @POST("mqtt/control")
    Observable<Response<String>> sendDeviceToMqtt(@Body GwGattBody body);

    /**
     * 12、网关数据复位（new）
     * 请求URL： https://dev.dadoutek.com/xxxx/gateway/reset/{gid}
     * GET  content-type : application/json
     * (从该接口开始未注明这条信息的接口不需要加region-id和authorizer-user-id)
     * region-id : 80(例)
     * authorizer-user-id : 300460(例)
     * token:eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MzAwNzI5fQ.YY-872ZqbqZjvCUxJjLyyBj1kbD-Mu2pgq4_2NS47sg (例)
     * https://dev.dadoutek.com/smartlight_test/gateway/reset/102
     */
    @GET("gateway/reset/{gid}")
    Observable<Response<ClearGwBean>> clearGwInfo(@Path("gid") long gwId);

    @GET("bin/latest/version")
    Observable<Response<Map<String, Integer>>> getBinList();

    /**
     * 获取路由模式下状态，用来恢复一些状态。比如扫描时杀掉APP后恢复至扫描页面，OTA时杀掉APP后恢复至OTA等待页面
     * https://dev.dadoutek.com/xxxx/router/status/get GET
     */
    @GET("router/status/get")
    Observable<RouteStasusBean> getRouterStatus();

    /**
     * 获取未确认扫描结果
     * https://dev.dadoutek.com/xxxx/scan/result/get GET
     */
    @GET("scan/result/get")
    Observable<RouteScanResultBean> getScanResult();

    /**
     * 请求开始扫描设备
     * https://dev.dadoutek.com/xxxx/router/scan-device POST
     * scanType	是	int	扫描设备类型 普通灯 = 4彩灯 = 6蓝牙连接器 = 5窗帘 = 16开关 = 99 传感器 = 98
     * scanName	是	string	扫描设备的蓝牙名
     * ser_id	是	string	会话id，推送中回传
     * "scanType": 4,
     * "scanName": "dadousmart"
     * "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/scan-device")
    Observable<Response<ScanDataBean>> routeScanDevcie(@Field("scanType") int scanType, @Field(
            "scanName") String scanName, @Field("ser_id") String ser_id);

    /**
     * 停止扫描
     * https://dev.dadoutek.com/xxxx/router/stop-scan POST
     * ser_id	是	string	app会话id，推送时回传
     * scanSerId	是	int	扫描关联id。可以在成功开始扫描推送和获取未确认扫描结果时获得
     * "ser_id": "app会话id，自己维护",
     * "scanSerId": -1000000
     */
    @FormUrlEncoded
    @POST("router/stop-scan")
    Observable<Response<RouterTimeoutBean>> routeStopScanDevcie(@Field("ser_id") String ser_id,
                                                                @Field(
                                                                        "scanSerId") long scanSerId);

    /**
     * 确认扫描结果，如果有未确认的扫描结果是不能再扫描的
     * https://dev.dadoutek.com/xxxx/scan/result/confirm  DELETE
     */
    @DELETE("scan/result/confirm")
    Observable<Response<RouterTimeoutBean>> tellServerClearScanning();

    /**
     * 通过路由进行设备分组
     * https://dev.dadoutek.com/xxxx/router/regroup POST
     * targetGroupMeshAddr	是	int	目标组meshAddr
     * deviceMeshAddrs	是	list	需要分组的设备meshAddr列表
     * meshType	是	int	设备类型  普通灯 = 4  彩灯 = 6 连接器 = 5 窗帘 = 16
     * ser_id	是	string	app会话id，推送时回传
     * "targetGroupMeshAddr" : 32769,
     * "deviceMeshAddrs": [1, 2, 3, 4, 5],
     * "meshType": 4,
     * "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/regroup")
    Observable<Response<RouterBatchGpBean>> routerBatchGp(@Field("targetGroupMeshAddr") int targetGroupMeshAddr,
                                                          @Field("deviceMeshAddrs") List<Integer> deviceMeshAddrs,
                                                          @Field("meshType") int meshType,
                                                          @Field("ser_id") String ser_id);

    @POST("router/regroup")
    Observable<Response<RouterBatchGpBean>> routerBatchGpN(@Body GroupBodyBean body);

    /**
     * 闪灯  https://dev.dadoutek.com/xxxx/router/device/flash POST
     * meshAddrs	是	list	选择的设备meshAddr
     * meshType	是	int	meshAddr类型  "meshAddrs": [1, 2, 3],"meshType": 6
     */
    @POST("router/device/flash")
    Observable<Response<RouterBatchGpBean>> routerBatchGpBlink(@Body GroupBlinkBodyBean body);

    /**
     * 获取路由列表
     * https://dev.dadoutek.com/xxxx/router/list GET
     */
    @GET("router/list")
    Observable<Response<List<DbRouter>>> getRouterList();

    /**
     * 路由软件恢复出厂 DELETE
     * https://dev.dadoutek.com/smartlight_test/router/router-reset
     * ser_id	是	string	app会话id，推送时回传
     * macAddr	是	string	需要恢复出厂的路由mac地址
     * "ser_id": "app会话id，自己维护",
     * "macAddr": "0102030405",
     */
    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "router/router-reset", hasBody = true)
    Observable<Response<Long>> routerReset(@Body() MacResetBody body);

    /**
     * 路由更新
     * https://dev.dadoutek.com/xxxx/router/update/{id}
     * Post
     * name	否	string	名称
     * {"name": "sdasdadas"}
     */
    @FormUrlEncoded
    @PUT("router/update/{id}")
    Observable<Response> updateRouter(@Path("id") long id, @Field("name") String name);

    /**
     * 设备绑定路由
     * https://dev.dadoutek.com/xxxx/router/device/bound   POST
     * meshAddrs	是	int	需要绑定设备的meshAddr
     * meshType	是	int	meshAddr类型
     * macAddr	是	int	绑定目标路由的macAddr
     * {
     * "meshAddrs": [1, 2, 3],
     * "meshType": 4,
     * "macAddr": "0102030405",
     * }
     * meshType
     * 普通灯 = 4
     * 彩灯 = 6
     * 蓝牙连接器 = 5
     * 窗帘 = 16
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25
     * 传感器 = 98 或 0x23 或 0x24  method = "DELETE", path = "router/del-group", hasBody = true
     */
    @FormUrlEncoded
    @POST("router/device/bound")
    Observable<Response> bindRouter(@Field("meshAddrs") List<Integer> meshAddrs,
                                    @Field("meshType") int meshType,
                                    @Field("macAddr") String macAddr);

    /**
     * 设备解绑路由
     * https://dev.dadoutek.com/xxxx/router/device/unbound  DELETE
     * meshAddrs	是	list	关联设备或者组的meshAddr
     * meshType	是	int	meshAddr类型
     * "meshAddrs": [1, 2, 3],
     * "meshType": 4
     * meshType普通灯 = 4彩灯 = 6 蓝牙连接器 = 5 窗帘 = 16
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24
     */
    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "router/device/unbound", hasBody = true)
    Observable<Response> unbindRouter(@Body GroupBlinkBodyBean bodyBean);


    /**
     * 路由入账号
     * https://dev.dadoutek.com/xxxx/router/access-in POST
     * macAddr	是	string	扫码得到的macAddr
     * ser_id	是	string	会话id，推送中回传
     * timeZoneHour	是	int	时区小时数。+8:00北京时区就是8，-4:30委内瑞拉时区就是-4
     * timeZoneMin	是	int	时区分钟数。+8:00北京时区就是0，-4:30委内瑞拉时区就是30
     * {
     * "macAddr": "0102030405",
     * "timeZoneHour": 8,
     * "timeZoneMin": 0,
     * "ser_id": "app会话id，自己维护"
     * }
     */
    @FormUrlEncoded
    @POST("router/access-in")
    Observable<Response<Integer>> routerAccessIn(@Field("macAddr") String macAddr, @Field(
            "timeZoneHour")
            int timeZoneHour, @Field("timeZoneMin") int timeZoneMin,
                                                 @Field("ser_id") String ser_id);

    /**
     * 配置路由wifi信息。路由可以配置wifi也可以接网线
     * https://dev.dadoutek.com/xxxx/router/wifi-configure  POST
     * ser_id	是	string	app会话id，推送时回传
     * macAddr	是	string	目标路由macAddr
     * ssid	是	string	wifi账号，只支持英文字母
     * pwd	是	string	wifi密码
     * timeZoneHour	是	int	时区小时数。+8:00北京时区就是8，-4:30委内瑞拉时区就是-4
     * timeZoneMin	是	int	时区分钟数。+8:00北京时区就是0，-4:30委内瑞拉时区就是30
     * "ser_id": "app会话id，自己维护",
     * "macAddr": "010203040506",
     * "ssid": "dadou",
     * "pwd": "Dadoutek2018",
     * "timeZoneHour": 8,
     * "timeZoneMin": 0
     */

    @FormUrlEncoded
    @POST("router/wifi-configure")
    Observable<Response<RouterTimeoutBean>> routerConfigWifi(@Field("macAddr") String macAddr,
                                                             @Field("ssid") String ssid,
                                                             @Field("pwd") String pwd, @Field(
            "timeZoneHour") int timeZoneHour,
                                                             @Field("timeZoneMin") int timeZoneMin, @Field("ser_id") String ser_id);

    /**
     * 获取模式，不同账号不同区域有不同的模式。比如300460用户区域1是蓝牙模式，区域2可能是路由模式
     * https://dev.dadoutek.com/xxxx/auth/settings GET
     * https://dev.dadoutek.com/smartlight_test/auth/settings
     */
    @GET("auth/settings")
    Observable<Response<ModeStatusBean>> getAllModeStatus();

    /**
     * 0.保存用户喜好设置
     * https://dev.dadoutek.com/xxxx/auth/settings/save  GET
     * https://dev.dadoutek.com/smartlight_test/auth/settings/save?auxiliaryFunction=false //
     * auxiliaryFunction设置为false
     * https://dev.dadoutek.com/smartlight_test/auth/settings/save?mode=0 // mode设置为0
     * https://dev.dadoutek.com/smartlight_test/auth/settings/save?auxiliaryFunction=false&mode=0
     * // auxiliaryFunction和false分别设置为false和0,支持多个
     * https://dev.dadoutek.com/smartlight_test/auth/settings/save?lalala=rarara // 以后可能的用户配置
     * "data": null,
     * "errorCode": 0,
     * "message": "save settings succeed!"
     */
    @GET("auth/settings/save")
    Observable<Response> updateAllModeStatus(@Query("auxiliaryFunction") boolean auxiliaryFunction,
                                             @Query("mode") int modeNum);

    /**
     * 通过路由删除组
     * https://dev.dadoutek.com/xxxx/router/del-group  DELETE
     * meshAddr	是	int	目标组meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * "targetGroupMeshAddr" : 32769,
     * "ser_id": "app会话id，自己维护"
     */
    @HTTP(method = "DELETE", path = "router/del-group", hasBody = true)
    Observable<Response<RouterTimeoutBean>> routerDeleteGroup(@Body RouterDelGpBody body);

    /**
     * 通过路由添加场景
     * 请求URL
     * https://dev.dadoutek.com/xxxx/router/add-scene POST
     * name	是	string	场景名称
     * imgName	是	string	场景展示icon名
     * actions	是	List<Action>	本地生成的actions直接上传即可，已做好兼容`
     * ser_id	是	string	app会话id，推送时回传
     * "name": "场景1",
     * "imgName": "icon_out",
     * "actions": [
     * // 都兼容
     * {"id": 1, "isOn": false, "color": 16777215, "groupAddr": 32769, "brightness": 100,
     * "deviceType": 4, "belongSceneId": 2, "colorTemperature": 100},
     * {"id": 2, "color": 0, "status": 1, "rgbType": 0, "groupAddr": 32792, "brightness":
     * 90, "gradientId": 1, "gradientName": "七彩渐变", "gradientType": 2, "belongSceneId":
     * 7, "gradientSpeed": 50, "colorTemperature": 50}
     * ],
     * "ser_id": "app会话id，自己维护"
     */
    @POST("router/add-scene")
    Observable<Response<RouterTimeoutBean>> routerAddScene(@Body SceneAddBodyBean body);

    /**
     * 更新场景
     * 请求URL
     * https://dev.dadoutek.com/xxxx/router/update-scene PUT
     * sid	是	int	场景id
     * actions	是	List<Action>	新场景数据
     * ser_id	是	string	app会话id，推送时回传
     * "sid" : 1,
     * "actions": [
     * {  "id": 1,"isOn": false, "color": 16777215,   "groupAddr": 32769, "brightness": 100,
     * "deviceType": 4,"colorTemperature": 100 }, ] "ser_id": "app会话id，自己维护"
     */
    @PUT("router/update-scene")
    Observable<Response<RouterTimeoutBean>> routerUpdateScene(@Body RouterUpDateSceneBody body);

    /**
     * 通过路由删除组
     * 请求URL
     * https://dev.dadoutek.com/xxxx/router/del-scene DELETE
     * sid	是	int	目标场景id
     * "sid" : 1,
     * "ser_id": "app会话id，自己维护"
     */
    @HTTP(method = "DELETE", path = "router/del-scene", hasBody = true)
    Observable<Response<RouterTimeoutBean>> routerDelScene(@Body() SceneIdBodyBean body);

    /**
     * 路由器获取版本号   https://dev.dadoutek.com/xxxx/router/device-version  POST
     * "meshAddrs" : [1, 2, 3],
     * "meshType": 4,
     * "ser_id": "app会话id，自己维护"
     * meshType
     * 普通灯 = 4 彩灯 = 6 蓝牙连接器 = 5 窗帘 = 16 传感器 = 98 或 0x23 或 0x24n
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25
     */
    @FormUrlEncoded
    @POST("router/device-version")
    Observable<Response<RouterVersionsBean>> routerGetDevicesVersion(@Field("meshAddrs") List<Integer> meshAddrs,
                                                                     @Field("meshType") int meshType,
                                                                     @Field("ser_id") String ser_id);

    /**
     * 创建ota升级任务  https://dev.dadoutek.com/xxxx/router/device-ota POST
     * meshAddrs	是	list	选择设备的meshAddr
     * meshType	是	int	设备类型
     * start	是	long	用于查询的时间戳，单位ms，ota结果记录的start字段会存储这个值
     * "meshAddrs" : [1, 2, 3],
     * "meshType": 4,
     * "start": 1597046661669
     * meshType
     * 普通灯 = 4 彩灯 = 6 蓝牙连接器 = 5 窗帘 = 16
     * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25  传感器 = 98 或 0x23 或 0x24
     */
    @FormUrlEncoded
    @POST("router/device-ota")
    Observable<Response> routerToDevicesOta(@Field("meshAddrs") List<Integer> meshAddrs,
                                            @Field("meshType") int meshType,
                                            @Field("start") long start);

    /**
     * 获取ota升级结果
     * 请求URL
     * https://dev.dadoutek.com/xxxx/ota/result/list  GET
     * page	否	int	页码，可能数量过大需要分页查询。默认1
     * size	否	int	当前页数量。默认Integer的最大值，即0x7fffffff（有符号）
     * start	否	long	查询条件: 开始时间戳，单位毫秒
     * 其他查询条件可扩展
     * https://dev.dadoutek.com/smartlight_test/ota/result/list?page=1&size=50 #查询第一页数据，每页50条
     * https://dev.dadoutek.com/smartlight_test/ota/result/list?page=1&size=50&start
     * =1597046991063 #查询#查询第一页数据，每页50条，创建时间为2020-07-20 11:29:20
     */

    @GET("ota/result/list")
    Observable<Response<List<RouterOTAResultBean>>> routerGetOTAResult(@Query("page") int page,
                                                                       @Query("size") int size,
                                                                       @Query("start") long start);

    /**
     * 停止ota升级  https://dev.dadoutek.com/xxxx/router/stop-ota  POST
     * ser_id	是	string	app会话id，推送时回传
     * start	是	long	时间戳，本地存储或者获取路由模式下状态后得到。要停止就全部停止
     * "ser_id": "app会话id，自己维护",
     * "start": 1597046661669
     */
    @FormUrlEncoded
    @POST("router/stop-ota")
    Observable<Response<RouterTimeoutBean>> routerStopOTA(@Field("ser_id") String ser_id, @Field(
            "start") long start);

    /**
     * 添加自定义渐变  https://dev.dadoutek.com/xxxx/router/add-custom-dc  POST
     * ser_id	是	string	app会话id，推送时回传   name	是	string	名称
     * type	是	int	渐变类型  speed是int渐变速度  colorNodes	是	List<ColorNode>	colorNodes
     * meshAddr	是	int	关联设备或者组的meshAddr
     * meshType	是	int	meshAddr类型
     * {  "ser_id": "app会话id，自己维护","name": "自定义渐变", "type": 0,"speed": 5,
     * "colorNodes": [{"brightness": 100, "colorTemperature": 0, "rgbw": 16731983,"index": 0,"id": 1
     * },  { "brightness": 50, "colorTemperature": 0,"rgbw": 16728987,"index": 1, "id": 2}]
     * "meshAddr": 1,"meshType": 6}
     * meshType 彩灯 = 6 组 = 9
     */
    @POST("router/add-custom-dc")
    Observable<Response<RouterTimeoutBean>> routerAddCustomGradient(@Body AddGradientBean addGradientBean);

    /**
     * 更新自定义渐变  https://dev.dadoutek.com/xxxx/router/update-custom-dc PUT
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	自定义渐变id
     * type	是	int	渐变类型
     * colorNodes	是	List<ColorNode>	colorNodes
     * meshType	是	int	meshAddr类型
     * {"ser_id": "app会话id，自己维护",   "type": 0,"colorNodes": [ {"brightness": 100,
     * "colorTemperature": 0,
     * "rgbw": 16731983,"index": 0,"id": 1} ] "meshAddr": 1,"meshType": 6 }
     * meshType 彩灯 = 6  组 = 97
     */
    @PUT("router/update-custom-dc")
    Observable<Response<RouterTimeoutBean>> routerUpdateCustomGradient(@Body UpdateGradientBean bean);

    /**
     * 删除自定义渐变 https://dev.dadoutek.com/xxxx/router/del-custom-dc DELETE
     * ser_id	是	string	app会话id，推送时回传
     * idList	是	list	自定义渐变id列表
     * meshAddr	是	int	关联设备或者组的meshAddr
     * meshType	是	int	meshAddr类型
     * meshType 彩灯 = 6 组 = 97
     */
    @HTTP(method = "DELETE", path = "router/del-custom-dc", hasBody = true)
    Observable<Response<RouterTimeoutBean>> routerDelCustomGradient(@Body DelGradientBodyBean body);

    /**
     * 设备&组应用自定义渐变  https://dev.dadoutek.com/xxxx/router/control/dynamic-change/custom/apply POST
     * meshAddr	是	int	目标meshAddr
     *      * ser_id	是	string	app会话id，推送时回传
     *      * meshType	是	int	mesh地址类型   meshType 彩灯 = 6 组 = 97
     *      * id	是	int	自定义渐变id
     */
    @FormUrlEncoded
    @POST("router/control/dynamic-change/custom/apply")
    Observable<Response<RouterTimeoutBean>> routerApplyDiyGradient(@Field("id") int id,
                                                                   @Field("meshAddr") Integer meshAddr,
                                                                   @Field("meshType") Integer meshType,
                                                                   @Field("ser_id") String ser_id);

    /**
     * 设备&组应用内置渐变
     * https://dev.dadoutek.com/xxxx/router/control/dynamic-change/built-in/apply  POST
     * ser_id	是	string	app会话id，推送时回传
     * meshAddr	是	int	目标meshAddr
     * meshType	是	int	mesh地址类型  彩灯 = 6 组 = 97
     * id	是	int	内置渐变id
     * speed	是	int	速度
     *
     *"meshAddr" : 1,  "meshType": 6, "meshAddr": 1,   "id": 1, "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/control/dynamic-change/built-in/apply")
    Observable<Response<RouterTimeoutBean>> routerApplySystemGradient(@Field("id") int id,
                                                                      @Field("meshAddr") Integer meshAddr,
                                                                      @Field("meshType") Integer meshType,
                                                                      @Field("speed") Integer speed,
                                                                      @Field("ser_id") String ser_id);

    /**
     * 渐变停止 https://dev.dadoutek.com/xxxx/router/control/dynamic-change/stop  POST
     * meshAddr	是	int	目标设备或者组的meshAddr
     * meshType	是	int	meshAddr类型
     * ser_id	是	string	app会话id，推送时回传
     * "meshAddr" : 1,
     * "meshType": 6,
     * "ser_id": "app会话id，自己维护"
     * meshType 彩灯 = 6 组 = 97
     */
    @FormUrlEncoded
    @POST("router/control/dynamic-change/stop")
    Observable<Response<RouterTimeoutBean>> routerStopDynamic(@Field("meshAddr") int meshAddr,
                                                              @Field("meshType") int meshType,
                                                              @Field("ser_id") String ser_id);

    /**
     * 直连开关&传感器 https://dev.dadoutek.com/xxxx/router/device-connect POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int 	开关or传感器id
     * meshType	是	int	设备类型
     * meshType 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24
     */
    @FormUrlEncoded
    @POST("router/device-connect")
    Observable<Response<RouterTimeoutBean>> routerConnectSwOrSensor(@Field("id") int id, @Field(
            "meshType") Integer meshType, @Field("ser_id") String ser_id);

    /**
     * 普通开关配置。触摸&太阳能的调光调色&单调光 https://dev.dadoutek.com/xxxx/router/normal-switch/configure  POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	开关id
     * groupMeshAddr	是	int	配置组的meshAddr
     */
    @FormUrlEncoded
    @POST("router/normal-switch/configure")
    Observable<Response<RouterTimeoutBean>> configNormalSw(@Field("id") int id, @Field(
            "groupMeshAddr") Integer groupMeshAddr, @Field("ser_id") String ser_id);

    /**
     * 双组开关配置。触摸双组开关  https://dev.dadoutek.com/xxxx/router/double-group-switch/configure POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	开关id
     * groupMeshAddrs	是	list<int>	配置双组的meshAddr
     * groupMeshAddrs 长度必须是2未配置填0
     */
    @POST("router/double-group-switch/configure")
    Observable<Response<RouterTimeoutBean>> configDoubleSw(@Body RouterListBody body/*@Field("id") int id, @Field(
            "groupMeshAddrs") List<Integer> groupMeshAddrs, @Field("ser_id") String ser_id*/);

    /**
     * 场景开关配置。触摸&太阳能的场景开关 https://dev.dadoutek.com/xxxx/router/scene-switch/configure  POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	开关id
     * sceneIds	是	list	场景id
     * sceneIds长度必须为4 不配置填0
     * "sceneIds": [1, 2, 3, 4],
     */
    @POST("router/scene-switch/configure")
    Observable<Response<RouterTimeoutBean>> configSceneSw(@Body SwSceneListBody body/*@Field("id") int id,
                                                          @Field("sceneIds") List<Integer> sceneIds, @Field("ser_id") String ser_id*/);

    /**
     * 八键开关配置 https://dev.dadoutek.com/xxxx/router/eight-key-switch/configure POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	开关id
     * keys	是	List<Key>	原keys模型
     * { "ser_id": "app会话id，自己维护","id": 1,"keys": [   {
     * "reserveValue_A":0,
     * "keyId":0,
     * "name":"四楼小孩房25%",
     * "reserveValue_B":31,
     * "featureId":0  }]}
     */
    @POST("router/eight-key-switch/configure")
    Observable<Response<RouterTimeoutBean>> configEightSw(@Body SwEightBody body/*@Field("id") int id,
                                                          @Field("keys") List<KeyBean> keys,
                                                          @Field("ser_id") String ser_id*/);

    /**
     * 传感器配置
     * https://dev.dadoutek.com/xxxx/router/sensor/configure POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	传感器id
     * configuration	是	Configuration	配置数据
     * mode	是	int	0群组，1场景
     * condition	是	int	触发条件。0全天，1白天，2夜晚
     * durationTimeUnit	是	int	持续时间单位。0秒，1分钟
     * durationTimeValue	是	int	持续时间
     * action	否	int	触发时执行逻辑。0开，1关，2自定义亮度。仅在群组模式下需要该配置
     * brightness	否	int	自定义亮度值。仅在群组模式下需要该配置
     * groupMeshAddrs	否	list	配置组meshAddr，可多个。仅在群组模式下需要该配置
     * sid	否	int	配置场景id。仅在场景模式下需要该配置
     * 群组模式示例
     * { "ser_id": "app会话id，自己维护", "id": 1,
     * "configuration": {
     * "mode": 0,   "condition": 0,  "durationTimeUnit": 0, "durationTimeValue": 10,
     * "action": 0,"brightness": 0,  "groupMeshAddrs": [32799, 32800]}}
     * 场景模式示例
     * {"ser_id": "app会话id，自己维护", "id": 1,
     * "configuration": { "mode": 0, "condition": 0, "durationTimeUnit": 0,
     * "durationTimeValue": 10,"sid": 1 }}
     */
    @FormUrlEncoded
    @POST("router/sensor/configure")
    Observable<Response<RouterTimeoutBean>> configSensor(@Field("id") int id,
                                                         @Field("configuration") List<ConfigurationBean> keys,
                                                         @Field("ser_id") String ser_id);

    /*
    -------------------------------------控制指令相关-------------------------------------------------------*/



    /**
     * 传感器开关  https://dev.dadoutek.com/xxxx/router/control/sensor/status POST
     * ser_id	是	string	app会话id，推送时回传
     * id	是	int	传感器id  status	是	int	开关状态。1开，0关
     * "ser_id": "app会话id，自己维护",
     * "id": 1,
     * "status": 1
     */
    @FormUrlEncoded
    @POST("router/control/sensor/status")
    Observable<Response<RouterTimeoutBean>> routerSwitchSensor(@Field("id") int id,
                                                               @Field("status") int status,
                                                               @Field("ser_id") String ser_id);

    /**
     * 窗帘&组控制集合 https://dev.dadoutek.com/xxxx/router/control/curtain POST
     * meshAddr	是	int	目标组meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	mesh地址类型
     * controlCmd	是	int	窗帘具体控制操作cmd
     * value	否	int	只需在调节窗帘速度时需要
     * { "meshAddr" : 1, "meshType": 4,"controlCmd": 1,"value": 10"ser_id": "app会话id，自己维护"}
     * controlCmd  开 = 0x0a  暂停 = 0x0b  关 = 0x0c  调节速度 = 0x15  恢复出厂 = 0xec   重启 = 0xea
     * value 调节速度值 = 1~3  其他 = 不填或随意
     * meshType 窗帘 = 16 组 = 97
     */
    @FormUrlEncoded
    @POST("router/control/curtain")
    Observable<Response<RouterTimeoutBean>> routerControlCurtain(@Field("meshAddr") int meshAddr,
                                                                 @Field("meshType") int meshType,
                                                                 @Field("controlCmd") int controlCmd,
                                                                 @Field("value") int speedValue,
                                                                 @Field("ser_id") String ser_id);

    /**
     * .用户复位 https://dev.dadoutek.com/xxxx/router/control/user-reset  DELETE
     * ser_id	是	string	app会话id，推送时回传
     * "ser_id": "app会话id，自己维护"
     */
    @HTTP(method = "DELETE", path = "router/control/user-reset", hasBody = true)
    Observable<Response<RouterTimeoutBean>> routerUserReset(@Body RouterDelGpBody  body);


    /**
     * 安全锁https://dev.dadoutek.com/xxxx/router/control/device/rgb  POST
     * status	是	int	1开 2关
     * ser_id	是	string	app会话id，推送时回传
     * "status" : 2,
     * "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/control/device/rgb")
    Observable<Response<RouterTimeoutBean>> routeOpenOrCloseSafeLock(@Field("status") int status,
                                                             @Field("ser_id") String ser_id);

    /**
     * 设备&组开关灯 https://dev.dadoutek.com/xxxx/router/control/status POST
     * meshAddr	是	int	目标meshAddr  ser_id 是	string	app会话id，推送时回传 meshType	是	int	mesh地址类型
     * status	是	int	0关1开  meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
     * {"data": {   "timeout": 8,},  "errorCode": 0,"message": "发送协议成功，等待路由回复"}
     * timeout	int	等待推送的超时时间，服务器给，不用计算了
     */
    @FormUrlEncoded
    @POST("router/control/status")
    Observable<Response<RouterTimeoutBean>> routeOpenOrClose(@Field("meshAddr") int meshAddr,
                                                             @Field("meshType") int meshType,
                                                             @Field("status") int status,
                                                             @Field("ser_id") String ser_id);

    /**
     * 设备&组亮度调节  https://dev.dadoutek.com/xxxx/router/control/brightness POST
     * meshAddr	是	int	目标meshAddr ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	mesh地址类型 brightness	是	int	亮度值
     * "meshAddr" : 1,"brightness": 50,"meshType": 4,"ser_id": "app会话id，自己维护"
     * meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
     */
    @FormUrlEncoded
    @POST("router/control/brightness")
    Observable<Response<RouterTimeoutBean>> routeConfigBrightness(@Field("meshAddr") int meshAddr,
                                                                  @Field("meshType") int meshType,
                                                                  @Field("brightness") int brightness,
                                                                  @Field("ser_id") String ser_id);

    /**
     * 设备&组色温调借 https://dev.dadoutek.com/xxxx/router/control/colorTemperature POST
     * meshAddr	是	int	目标meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	mesh地址类型
     * colorTemperature	是	int	色温值
     * { "meshAddr" : 1,
     * "meshType": 4,
     * "colorTemperature": 50,
     * "ser_id": "app会话id，自己维护"}
     * meshType普通灯 = 4 彩灯 = 6连接器 = 5组 = 97
     */
    @FormUrlEncoded
    @POST("router/control/colorTemperature")
    Observable<Response<RouterTimeoutBean>> routeConfigColorTemp(@Field("meshAddr") int meshAddr,
                                                                 @Field("meshType") int meshType,
                                                                 @Field("colorTemperature") int brightness,
                                                                 @Field("ser_id") String ser_id);

    /**
     * 设备&组w值调节
     * https://dev.dadoutek.com/xxxx/router/control/w  POST
     * <p>
     * meshAddr	是	int	目标meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	mesh地址类型
     * color	是	int	原来逻辑生成的color值
     * "meshAddr" : 1, "meshType": 6,"color": 16777215 // w = 0 , "ser_id": "app会话id，自己维护"
     * meshType彩灯 = 6组 = 97
     */
    @FormUrlEncoded
    @POST("router/control/w")
    Observable<Response<RouterTimeoutBean>> routeConfigWhiteNum(@Field("meshAddr") int meshAddr,
                                                                @Field("meshType") int meshType,
                                                                @Field("color") int color,
                                                                @Field("ser_id") String ser_id);

    /**
     * 设备&组rgb值调节  https://dev.dadoutek.com/xxxx/router/control/rgb POST
     * meshAddr	是	int	目标meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	mesh地址类型  彩灯 = 6组 = 97
     * color	是	int	原来逻辑生成的color值
     *     "meshAddr" : 1,
     *     "meshType": 6,
     *     "color": 16777215 // r = 255, g = 255, b = 255
     *     "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/control/rgb")
    Observable<Response<RouterTimeoutBean>> routeConfigRGBNum(@Field("meshAddr") int meshAddr,
                                                                @Field("meshType") int meshType,
                                                                @Field("color") int color,
                                                                @Field("ser_id") String ser_id);

    /**
     * 缓期缓灭开关 https://dev.dadoutek.com/xxxx/router/control/slow-up-slow-down/status POST
     * status	是	int	1开 2关 (特别注意2才是关)
     * ser_id	是	string	app会话id，推送时回传
     * {"status" : 1,//特别注意2才是关  "ser_id": "app会话id，自己维护"}
     */
    @FormUrlEncoded
    @POST("router/control/slow-up-slow-down/status")
    Observable<Response<RouterTimeoutBean>> routeSlowUpSlowDownSwitch(@Field("status") int status,
                                                                      @Field("ser_id") String ser_id);

    /**
     * 缓起缓灭速度 https://dev.dadoutek.com/xxxx/router/control/slow-up-slow-down/speed POST
     * speed	是	int	速度值
     * ser_id	是	string	app会话id，推送时回传
     */
    @FormUrlEncoded
    @POST("router/control/slow-up-slow-down/speed")
    Observable<Response<RouterTimeoutBean>> routeSlowUpSlowDownSpeed(@Field("speed") int speed,
                                                                     @Field("ser_id") String ser_id);

    /**
     * 恢复出厂设置 https://dev.dadoutek.com/xxxx/router/control/reset DELETE
     * meshAddr	是	int	目标meshAddr
     * ser_id	是	string	app会话id，推送时回传
     * meshType	是	int	meshAddr类型  普通灯 = 4彩灯 = 6 蓝牙连接器 = 5
     * * 开关 = 99 或 0x20 或 0x22 或 0x21 或 0x28 或 0x27 或 0x25 传感器 = 98 或 0x23 或 0x24 组 = 97 全部 = 100
     * * 不支持窗帘  meshType=97&meshAddr=65535时效果与meshType=100一致
     */
    @HTTP(method ="DELETE" ,path = "router/control/reset",hasBody = true)
    Observable<Response<RouterTimeoutBean>> routeResetFactory(@Body MacResetBody body);

    /**
     * 路由软件恢复出厂 https://dev.dadoutek.com/xxxx/router/router-reset  DELETE
     * ser_id	是	string	app会话id，推送时回传
     * macAddr	是	string	需要恢复出厂的路由mac地址
     * { "ser_id": "app会话id，自己维护",
     *     "macAddr": "0102030405",}
     */
    @POST("router/router-reset")
    Observable<Response<RouterTimeoutBean>> routeResetFactoryBySelf(@Body MacResetBody body);

    /**
     * 获取路由版本号，路由升级时先获取路由版本号，成功后再进行升级
     * https://dev.dadoutek.com/xxxx/router/router-version POST
     * macAddr	是	string	目标路由macAddr
     * ser_id	是	string	app会话id，推送时回传
     * "macAddr" : "0102030405",
     * "ser_id": "app会话id，自己维护"
     */
    @FormUrlEncoded
    @POST("router/router-version")
    Observable<Response<RouterTimeoutBean>> routeGetVersion(@Field("macAddr") long macAddr,
                                                            @Field("ser_id") String ser_id);

    /**
     * 路由升级，升级前先获取路由版本号
     * https://dev.dadoutek.com/xxxx/router/router-ota  POST
     * start	是	long	用于查询的时间戳，单位ms，ota结果记录的start字段会存储这个值
     * macAddr	是	string	目标路由macAddr
     * "start": 1597046661669,
     * "macAddr": "0102030405",
     */
    @FormUrlEncoded
    @POST("router/router-ota")
    Observable<Response<RouterTimeoutBean>> routeOtaRouter(@Field("start") long startTime,
                                                           @Field("macAddr") long macAddr);

    /**
     * 灯更新  请求URL  https://dev.dadoutek.com/xxxx/light/update/{id} PUT
     * 参数名	必选	类型	说明
     * index	否	int	排序值 暂时不用
     * name	否	string	名称
     */
    @FormUrlEncoded
    @PUT("light/update/{id}")
    Observable<Response> routeUpdateLight(@Path("id") long id, @Field("name") String name);

    /**
     * 窗帘更新  https://dev.dadoutek.com/xxxx/curtain/update/{id}* PUT
     * https://dev.dadoutek.com/smartlight_test/curtain/update/1
     */
    @FormUrlEncoded
    @PUT("curtain/update/{id}")
    Observable<Response> routeUpdateCurtain(@Path("id") long id, @Field("name") String name);

    /**
     * 连接器更新*https://dev.dadoutek.com/xxxx/relay/update/{id}
     */
    @FormUrlEncoded
    @PUT("relay/update/{id}")
    Observable<Response> routeUpdateRelay(@Path("id") long id, @Field("name") String name);

    /**
     * 灯更新 https://dev.dadoutek.com/xxxx/sensor/update/{id}
     */
    @FormUrlEncoded
    @PUT("sensor/update/{id}")
    Observable<Response> routeUpdateSensor(@Path("id") long id, @Field("name") String name);

    /**
     * 开关更新  https://dev.dadoutek.com/xxxx/switch/update/{id}
     */
    @FormUrlEncoded
    @PUT("switch/update/{id}")
    Observable<Response> routeUpdateSwitch(@Path("id") long id, @Field("name") String name);

    /**
     * 应用场景https://dev.dadoutek.com/xxxx/router/control/scene/apply  POST
     * id	是	int	场景id
     * ser_id	是	string	app会话id，推送时回传
     */
    @FormUrlEncoded
    @POST("router/control/scene/apply")
    Observable<Response<RouterTimeoutBean>> routeApplyScene(@Field("id") long id,
                                                            @Field("ser_id") String ser_id);


}
