package com.dadoutek.uled.network;

import android.text.TextUtils;

import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkFactory {
    private static final long DEFAULT_TIMEOUT = 15;
    private static RequestInterface api;
    private static Converter.Factory gsonConverterFactory = GsonConverterFactory.create();
    private static CallAdapter.Factory rxJavaCallAdapterFactory =
            RxJava2CallAdapterFactory.create();
    private static OkHttpClient okHttpClient;

    private static OkHttpClient initHttpClient() {

        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) { }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) { }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(Constant
        // .isDebug?HttpLoggingInterceptor.Level.BODY:HttpLoggingInterceptor.Level.NONE);
        HttpLoggingInterceptor logging =
                new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS) //设置连接超时 30秒
                .writeTimeout(3, TimeUnit.MINUTES)
                .addInterceptor(new CommonParamsInterceptor())
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier((hostname, session) -> true)
                .retryOnConnectionFailure(true);

        // if (BuildConfig.DEBUG)
        okHttpBuilder.addInterceptor(logging);

        return okHttpBuilder.build();
    }


    public static RequestInterface getApi() {
        if (okHttpClient == null) {
            okHttpClient = initHttpClient();
        }
        /**
         * 记得修改stomp的前缀地址 StompManager不能是测试的  还有dadousmart
         */
        /*if (Constant.isDebug) {
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(Constant.BASE_URL2)
                    .addConverterFactory(gsonConverterFactory)
                    .addCallAdapterFactory(rxJavaCallAdapterFactory)
                    .build();

            api = retrofit.create(RequestInterface.class);
        } else*/
        if (null == api || SharedPreferencesUtils.getTestType()) {
            Constant.BASE_URL = Constant.isDebug ? Constant.BASE_DEBUG_URL : Constant.BASE_URL_JAVA;
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(Constant.BASE_URL)
                    .addConverterFactory(gsonConverterFactory)
                    .addCallAdapterFactory(rxJavaCallAdapterFactory)
                    .build();

            api = retrofit.create(RequestInterface.class);
            SharedPreferencesUtils.setTestType(false);
        }
        return api;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5;
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
