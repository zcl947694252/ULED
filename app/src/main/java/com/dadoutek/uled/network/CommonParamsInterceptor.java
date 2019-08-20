package com.dadoutek.uled.network;

import android.text.TextUtils;
import android.util.Log;

import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbUser;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * 创建者     zcl
 * 创建时间   2017/6/26 14:36
 * 描述	      ${一个添加手机公共参数的拦截器}$
 * POST、DELETE请求一律使用”application/json”
 * GET请求一律使用”application/text”
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */

public class CommonParamsInterceptor implements Interceptor {

    public static final String JSON = "application/json";
    public static final String TEXT = "application/text";
    public static final MediaType MTEXT = MediaType.parse("application/text; charset=utf-8");
    public static final MediaType MJSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request(); // 112.124.22.238:8081/course_api/cniaoplay/featured?p={'page':0}
        //请求方式
        String method = request.method();

        Request.Builder builder = request.newBuilder();

        DbUser user = DBUtils.INSTANCE.getLastUser();
        Log.e("zcl", "zcl*******method---" + method);

        //添加请求头
        builder.addHeader("Content-Type", TEXT);

        String token = request.header("token");
        String tokenNow = user == null ? "" : user.getToken();

        String oldToken = tokenNow;
        String last_region_id = user != null && user.getLast_region_id() != null && !TextUtils.isEmpty(user.getLast_region_id()) ? user.getLast_region_id() : "1";
        builder.addHeader("region-id", last_region_id);
        Log.e("zcl","zcl******region-id=="+last_region_id);

        //authorizer-user-id：80
        //在请求头加上键值对authorizer-user-id:80即可获取id为80用户的数据，如果请求头中缺少这对键值对，后台默认authorizer-user-id：自己用户的id。
        //如何用回自己的数据？
        //1、请求头缺少authorizer-user-id     2、authorizer-user-id：自己的id  3、authorizer-user-id乱传
        //region-id：2
        //在请求头加上键值对region-id:2即可获取区域2的数据，如果请求头中缺少这对键值对，后台默认region-id:1
        //以下情况使用的是区域一的数据
        //1、请求头缺少region-id  2、region-id:1   3、region-id乱传

        if (user != null &&user.getAuthorizer_user_id()!=null&&!user.getAuthorizer_user_id().equals(""))
            builder.addHeader("authorizer-user-id",user.getAuthorizer_user_id());
        Log.e("zcl","zcl******authorizer-user-id=="+user.getAuthorizer_user_id());



        if (token == null || token.isEmpty())
            builder.addHeader("token", oldToken);

        RequestBody body = request.body();
        Log.e("zcl","zcl**method.equals(GET)****"+method.equals("GET")+"00000000"+(method.equals("POST") || method.equals("DELETE"))+"----"+(body != null));
        if (body != null) {
            MediaType mediaType = body.contentType();
            try {
                Field field = mediaType.getClass().getDeclaredField("mediaType");
                field.setAccessible(true);
                if (method.equals("GET")) {
                    field.set(mediaType, TEXT);
                } else if (method.equals("POST") || method.equals("DELETE")) {
                    field.set(mediaType, JSON);
                    HashMap<String, Object> rootMap = new HashMap<>();
                    RequestBody requestBody;
                    if (body instanceof FormBody) { // form 表单
                        for (int i = 0; i < ((FormBody) body).size(); i++)
                            rootMap.put(((FormBody) body).encodedName(i), ((FormBody) body).encodedValue(i));
                        Log.e("zcl", "zcl******rootMap" + rootMap.toString());
                        requestBody = RequestBody.create(MJSON, new JSONObject(rootMap).toString());
                    } else {
                        Buffer buffer = new Buffer();
                        body.writeTo(buffer);
                        String oldJsonParams = buffer.readUtf8();
                        requestBody = RequestBody.create(MJSON, oldJsonParams);
                        requestBody.writeTo(buffer);
                        Log.e("zcl", "zcl******buffer" + buffer.readUtf8());
                    }
                    builder.post(requestBody);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return chain.proceed(builder.build());
    }
}
