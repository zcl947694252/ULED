package com.dadoutek.uled.util;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;

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

import java.io.File;

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
//            //1.获取服务器版本url
            switch (StringUtils.versionResolution(localVersion, 0)) {
                case "L":
                    getServerVersion(context, Constant.LIGHT, Constant.LIGHT_TYPE_STROBE, localVersion, otaPrepareListner);
                    break;
                case "LN":
                    getServerVersion(context, Constant.LIGHT, Constant.LIGHT_TYPE_NO_STROBO_DIMMING, localVersion, otaPrepareListner);
                    break;
                case "LNS":
                    getServerVersion(context, Constant.LIGHT, Constant.LIGHT_TYPE_NO_STROBOSCOPIC_MONOTONE_LIGHT, localVersion, otaPrepareListner);
                    break;
                case "C":
                    getServerVersion(context, Constant.CONTROLLER, Constant.CONTROLLER_TYPE_NO_STROBO_DIMMING, localVersion, otaPrepareListner);
                    break;
                case "CS":
                    getServerVersion(context, Constant.CONTROLLER, Constant.CONTROLLER_TYPE_NO_STROBOSCOPIC_MONOTONE_LIGHT, localVersion, otaPrepareListner);
                    break;
                default:
                    ToastUtils.showLong(R.string.error_pack);
                    break;
            }
//            transformView();
        } else {
            ToastUtils.showLong(R.string.network_disconect);
        }
    }

    private boolean checkHaveNetWork(Context context) {
        return NetWorkUtils.isNetworkAvalible(context);
    }

    private String getServerVersion(Context context, int type, int detailType, String localVersion, OtaPrepareListner otaPrepareListner) {
        otaPrepareListner.startGetVersion();
        DownLoadFileModel.INSTANCE.getUrl(type, detailType).subscribe(new NetworkObserver<String>() {
            @Override
            public void onNext(String s) {
                otaPrepareListner.getVersionSuccess(s);
               /* localPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                        + "/" + StringUtils.versionResolutionURL(s, 2);*/
                localPath = context.getFilesDir()+ "/" + StringUtils.versionResolutionURL(s, 2);
                compareServerVersion(s, localVersion, otaPrepareListner, context);
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                otaPrepareListner.getVersionFail();
                ToastUtils.showLong(R.string.get_server_version_fail);
            }
        });
        return "";
    }

    //2.对比服务器和本地版本大小
    private boolean compareServerVersion(String serverVersionUrl, String localVersion, OtaPrepareListner otaPrepareListner, Context context) {
        if (!serverVersionUrl.isEmpty() && !localVersion.isEmpty()) {
            int serverVersionNum = Integer.parseInt(StringUtils.versionResolutionURL(serverVersionUrl, 1));
            int localVersionNum = Integer.parseInt(StringUtils.versionResolution(localVersion, 1));
            if (serverVersionNum > localVersionNum) {
                //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                File file = new File(localPath);
                if (file.exists()) {
                    otaPrepareListner.downLoadFileSuccess();
                    SharedPreferencesUtils.saveUpdateFilePath(localPath);
                } else {
                    download(serverVersionUrl, otaPrepareListner, context);
                }
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
                return true;
            } else {
                //4.本地已经是最新直接跳转升级页面
//                transformView();
                ToastUtils.showLong(R.string.the_last_version);
                return false;
            }
        } else {
            ToastUtils.showLong(R.string.getVsersionFail);
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
                        otaPrepareListner.downLoadFileSuccess();
                        mProgressBarDialog.dismiss();
                        SharedPreferencesUtils.saveUpdateFilePath(localPath);
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
    public Boolean checkSupportOta(String localVersion) {
        int localVersionNum = Integer.parseInt(StringUtils.versionResolution(localVersion, 1));
        if ((localVersion.contains("L-") || localVersion.contains("LNS-")
                || localVersion.contains("LN-") || localVersion.contains("C-") || localVersion.contains("CS-"))
                && localVersionNum >= Constant.OTA_SUPPORT_LOWEST_VERSION) {
            return true;
        } else {
            return false;
        }
    }
}
