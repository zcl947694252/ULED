package com.dadoutek.uled.util;

import android.content.Context;
import android.graphics.Color;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.OtaPrepareListner;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.HttpModel.DownLoadFileModel;
import com.dadoutek.uled.network.NetworkObserver;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.maning.mndialoglibrary.MProgressBarDialog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

public class OtaPrepareUtils {

    private static OtaPrepareUtils instance = null;
    private static MProgressBarDialog mProgressBarDialog;
    private static String localPath;

    private OtaPrepareUtils() {
    }

    public static OtaPrepareUtils instance() {
        if (null == instance) {
            instance = new OtaPrepareUtils();
        }
        return instance;
    }

    public void gotoUpdateView(Context context, String localVersion, OtaPrepareListner otaPrepareListner) {
        if (checkHaveNetWork(context)) {
            getServerVersionNew(context,localVersion,otaPrepareListner);
        } else {
            ToastUtils.showLong(R.string.network_disconect);
        }
    }

    private boolean checkHaveNetWork(Context context) {
        return NetWorkUtils.isNetworkAvalible(context);
    }

    private String getServerVersionNew(Context context,String localVersion, OtaPrepareListner otaPrepareListner) {
        otaPrepareListner.startGetVersion();
        DownLoadFileModel.INSTANCE.getUrlNew(localVersion).subscribe(new NetworkObserver<Object>() {
            @Override
            public void onNext(Object s) {
//                otaPrepareListner.getVersionSuccess(s);
               /* localPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                        + "/" + StringUtils.versionResolutionURL(s, 2);*/
                JSONObject jsonObject=new JSONObject((Map) s);
                try {
                    String data=jsonObject.getString("url");
                    localPath = context.getFilesDir()+ "/" + StringUtils.versionResolutionURL(data, 2);
                    LogUtils.e("zcl版本升级localPath-----------"+localVersion);
                    compareServerVersion(data, otaPrepareListner, context);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                otaPrepareListner.getVersionFail();
                ToastUtils.showLong(e.getMessage());
            }
        });
        return "";
    }

    //2.对比服务器和本地版本大小
    private boolean compareServerVersion(String serverVersionUrl, String localVersion, OtaPrepareListner otaPrepareListner, Context context) {
        if (!serverVersionUrl.isEmpty() && !localVersion.isEmpty()) {
            int serverVersionNum = Integer.parseInt(StringUtils.versionResolutionURL(serverVersionUrl, 1));
            int localVersionNum = Integer.parseInt(StringUtils.versionResolution(localVersion, 1));

            //开发者模式可以任意升级版本
            if(SharedPreferencesUtils.isDeveloperModel()){
                //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                File file = new File(localPath);
                if (file.exists()) {
                    SharedPreferencesUtils.saveUpdateFilePath(localPath);
                    otaPrepareListner.downLoadFileSuccess();
                } else {
//                    otaPrepareListner.getVersionSuccess("");
                    download(serverVersionUrl, otaPrepareListner, context);
                }
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
            }else{
                //正常模式只能升级服务器最新版本
                if (serverVersionNum > localVersionNum) {
                    //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                    File file = new File(localPath);
                    if (file.exists()) {
                        otaPrepareListner.downLoadFileSuccess();
                        SharedPreferencesUtils.saveUpdateFilePath(localPath);
                    } else {
//                        otaPrepareListner.getVersionSuccess("");
                        download(serverVersionUrl, otaPrepareListner, context);
                    }
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
                    return true;
                } else {
                    //4.本地已经是最新直接跳转升级页面
//                transformView();
                    otaPrepareListner.getVersionSuccess("");
                    ToastUtils.showLong(R.string.the_last_version);
                    return false;
                }
            }
        } else {
            ToastUtils.showLong(R.string.getVsersionFail);
        }
        return false;
    }

    //2.对比服务器和本地版本大小
    private boolean compareServerVersion(String serverVersionUrl, OtaPrepareListner otaPrepareListner, Context context) {
            //开发者模式可以任意升级版本
            if(SharedPreferencesUtils.isDeveloperModel()){
                //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                File file = new File(localPath);
                if (file.exists()) {
                    SharedPreferencesUtils.saveUpdateFilePath(localPath);
                    otaPrepareListner.downLoadFileSuccess();
                } else {
//                    otaPrepareListner.getVersionSuccess("");
                    download(serverVersionUrl, otaPrepareListner, context);
                }
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
            }else{
                //正常模式只能升级服务器最新版本
                    //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                    File file = new File(localPath);
                    if (file.exists()) {
                        otaPrepareListner.downLoadFileSuccess();
                        SharedPreferencesUtils.saveUpdateFilePath(localPath);
                    } else {
//                        otaPrepareListner.getVersionSuccess("");
                        download(serverVersionUrl, otaPrepareListner, context);
                    }
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
                    return true;
            }
        return false;
    }

    private void creatProgressDialog(Context mContext) {
        //新建一个Dialog
        mProgressBarDialog = new MProgressBarDialog.Builder(mContext)
                .setStyle(MProgressBarDialog.MProgressBarDialogStyle_Horizontal)
//                //全屏背景窗体的颜色
//                .setBackgroundWindowColor(mContext.getResources().getColor(R.color.white))
//                //View背景的颜色
//                .setBackgroundViewColor(mContext.getResources().getColor(R.color.white))
//                //字体的颜色
//                .setTextColor(mContext.getResources().getColor(R.color.primary))
//                //View边框的颜色
//                .setStrokeColor(mContext.getResources().getColor(R.color.primary))
                //View边框的宽度
                .setStrokeWidth(2)
                //View圆角大小
                .setCornerRadius(10)
                //ProgressBar背景色
                .setProgressbarBackgroundColor(Color.BLUE)
                //ProgressBar 颜色
                .setProgressColor(Color.GREEN)
                //圆形内圈的宽度
                .setCircleProgressBarWidth(4)
                //圆形外圈的宽度
                .setCircleProgressBarBackgroundWidth(4)
                //水平进度条Progress圆角
                .setProgressCornerRadius(0)
                //水平进度条的高度
                .setHorizontalProgressBarHeight(10)
//                //dialog动画
//                .setAnimationID(R.style.animate_dialog_custom)
                .build();
    }

    private void download(String url, OtaPrepareListner otaPrepareListner, Context context) {
//        String[] downUrl = new String[]{url};
        creatProgressDialog(context);

        FileDownloader.setup(context);
        FileDownloader.getImpl().create(url)
                .setPath(localPath)
                .setForceReDownload(true)
                .setCallbackProgressMinInterval(50)
                .setListener(new FileDownloadListener() {
                    //等待
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {

                    }

                    //下载进度回调
                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
//                        progressDialog.setProgress((soFarBytes * 100 / totalBytes));
                        mProgressBarDialog.showProgress((soFarBytes * 100 / totalBytes), context.getString(R.string.downloading) + (soFarBytes * 100 / totalBytes));
                    }

                    //完成下载
                    @Override
                    protected void completed(BaseDownloadTask task) {
                        mProgressBarDialog.dismiss();
                        SharedPreferencesUtils.saveUpdateFilePath(localPath);
                        otaPrepareListner.downLoadFileSuccess();
                    }

                    //暂停
                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {

                    }

                    //下载出错
                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        mProgressBarDialog.dismiss();
                        otaPrepareListner.downLoadFileFail(e.getMessage());
                    }

                    //已存在相同下载
                    @Override
                    protected void warn(BaseDownloadTask task) {
                    }
                }).start();
    }

    //检查是否支持OTA 返回true支持  返回false不支持
    public Boolean checkSupportOta(String localVersion) {//LA\LAS    CC\CCS   LX\LXS
        int localVersionNum = Integer.parseInt(StringUtils.versionResolution(localVersion, 1));
        boolean oldSuportVersion = (localVersion.contains("L-") || localVersion.contains("LNS-")
                || localVersion.contains("LN-") || localVersion.contains("C-") || localVersion.contains("CS-")
                || localVersion.contains("CR-") || localVersion.contains("LC-")||localVersion.contains("LA-")
                || localVersion.contains("LA-") || localVersion.contains("LAS-")||localVersion.contains("CC-")
                || localVersion.contains("CCS-") || localVersion.contains("LX-")||localVersion.contains("LXS-")
                || localVersion.contains("LG-")
                || localVersion.contains("LCS-") || localVersion.contains("L36-")) && localVersionNum >= Constant.OTA_SUPPORT_LOWEST_VERSION && localVersionNum != -1;
        boolean newSuport = localVersion.contains("PR-") || localVersion.contains("B")||localVersion.contains("E-GW")||localVersion.contains("NPR");
        if (oldSuportVersion||newSuport) {
            return true;
        } else {
            return false;
        }

    }
}
