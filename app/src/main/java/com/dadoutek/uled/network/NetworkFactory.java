package com.dadoutek.uled.network;

import android.text.TextUtils;
import android.util.Log;

import com.dadoutek.uled.BuildConfig;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkFactory {
    private static final long DEFAULT_TIMEOUT = 15;
    private static RequestInterface api;
    private static Converter.Factory gsonConverterFactory = GsonConverterFactory.create();
    private static CallAdapter.Factory rxJavaCallAdapterFactory = RxJava2CallAdapterFactory.create();
    private static OkHttpClient okHttpClient;

    private static OkHttpClient initHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        DbUser user = DBUtils.INSTANCE.getLastUser();
        Log.e("zcl","zcl***************************"+user==null?user.toString():"null");

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS) //设置连接超时 30秒
                .writeTimeout(3, TimeUnit.MINUTES)
                .addInterceptor(chain -> {  //添加请求头
                    Request request = chain.request();
                    String token = request.header("token");
                    Request build;
                    Request.Builder builder1 = request.newBuilder();
                  // if (token == null || token.isEmpty())
                  //     builder1.addHeader("token", user.getToken());

                  // String last_region_id = user!=null?user.getLast_region_id():"1";
                  // build = builder1.addHeader("region-id", last_region_id==null?"1":last_region_id)
                  //         .build();
                    return chain.proceed(request.newBuilder().build());
                })
                .retryOnConnectionFailure(true);

        if (BuildConfig.DEBUG)
            okHttpBuilder.addInterceptor(logging);

        return okHttpBuilder.build();
    }


    public static RequestInterface getApi() {

        if (okHttpClient == null) {
            okHttpClient = initHttpClient();
        }
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(Constant.BASE_URL)
                    //.baseUrl(Constant.BASE_DEBUG_URL)
                    .addConverterFactory(gsonConverterFactory)
                    .addCallAdapterFactory(rxJavaCallAdapterFactory)
                    .build();
            api = retrofit.create(RequestInterface.class);
        }

        return api;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
