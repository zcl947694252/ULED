package com.dadoutek.uled.othersview;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.CleanUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.SyncCallback;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.model.HttpModel.UserModel;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.network.NetworkObserver;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.AppUtils;
import com.dadoutek.uled.util.DBManager;
import com.dadoutek.uled.util.NetWorkUtils;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.SyncDataPutOrGetUtils;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.util.Event;
import com.telink.util.EventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by hejiajun on 2018/4/16.
 */

public class MeFragment extends Fragment implements EventListener<String> {

    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.app_version_name)
    TextView appVersionName;
    @BindView(R.id.app_version)
    TextView appVersion;
    Unbinder unbinder;
    @BindView(R.id.chear_cache)
    Button chearCache;
    @BindView(R.id.update_ite)
    Button updateIte;
    @BindView(R.id.copy_data_base)
    Button copyDataBase;
    @BindView(R.id.exit_login)
    Button exitLogin;
    @BindView(R.id.one_click_backup)
    Button oneClickBackup;
    @BindView(R.id.one_click_reset)
    Button oneClickReset;
    @BindView(R.id.constant_question)
    Button constantQuestion;
    @BindView(R.id.user_icon)
    ImageView userIcon;
    @BindView(R.id.user_name)
    TextView userName;
    private LayoutInflater inflater;

    private Dialog loadDialog;
    private TelinkLightApplication mApplication;
    private DbLight currentLight;
    private boolean isDeleteSuccess = false;

    private long sleepTime = 250;
    boolean isClickExlogin = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mApplication = (TelinkLightApplication) getActivity().getApplication();
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(NotificationEvent.ONLINE_STATUS, this);

        if (Build.BRAND.contains("Huawei")) {
            sleepTime = 500;
        } else {
            sleepTime = 200;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.fragment_me,
                null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        String versionName = AppUtils.getVersionName(getActivity());
        appVersion.setText(versionName);
        //暂时屏蔽
        updateIte.setVisibility(View.GONE);
        if (SharedPreferencesUtils.isDeveloperModel()) {
            copyDataBase.setVisibility(View.VISIBLE);
            chearCache.setVisibility(View.VISIBLE);
        } else {
            copyDataBase.setVisibility(View.GONE);
            chearCache.setVisibility(View.VISIBLE);
        }

        userIcon.setBackgroundResource(R.drawable.ic_launcher);
        userName.setText(DBUtils.getLastUser().getPhone());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            DBManager.getInstance().copyDatabaseToSDCard(activity);
            ToastUtils.showShort(R.string.copy_complete);
        }
    }

    @OnClick({R.id.chear_cache, R.id.update_ite, R.id.copy_data_base, R.id.app_version, R.id.exit_login, R.id.one_click_backup, R.id.one_click_reset, R.id.constant_question})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.chear_cache:
                emptyTheCache();
//                ToastUtils.showLong(R.string.devoloping);
                break;
            case R.id.update_ite:
                ToastUtils.showShort(R.string.wait_develop);
                break;
            case R.id.copy_data_base:
                verifyStoragePermissions(getActivity());
                break;
            case R.id.app_version:
                developerMode();
                break;
            case R.id.exit_login:
                exitLogin();
                break;
            case R.id.one_click_backup:
                checkNetworkAndSync(getActivity());
                break;
            case R.id.one_click_reset:
                showSureResetDialogByApp();
                break;
            case R.id.constant_question:
                startActivity(new Intent(getActivity(), AboutSomeQuestionsActivity.class));
                break;
        }

    }

    // 如果没有网络，则弹出网络设置对话框
    public void checkNetworkAndSync(final Activity activity) {
        if (!NetWorkUtils.isNetworkAvalible(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(R.string.btn_sure,
                            (dialog, whichButton) -> {
                                // 跳转到设置界面
                                activity.startActivityForResult(new Intent(
                                                Settings.ACTION_WIRELESS_SETTINGS),
                                        0);
                            }).create().show();
        } else {
            SyncDataPutOrGetUtils.Companion.syncPutDataStart(activity, syncCallback);
        }
    }

    SyncCallback syncCallback = new SyncCallback() {

        @Override
        public void start() {
            showLoadingDialog(getActivity().getString(R.string.tip_start_sync));
        }

        @Override
        public void complete() {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
                TelinkLightService.Instance().idleMode(true);

                restartApplication();
            }
            hideLoadingDialog();
        }

        @Override
        public void error(String msg) {
            isClickExlogin = false;
            Log.d("SyncLog", "error: " + msg);
            hideLoadingDialog();
        }

    };

    private void showSureResetDialogByApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.tip_reset_sure);
        builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
        });
        builder.setPositiveButton(R.string.btn_sure, (dialog, which) -> {
            if (TelinkLightApplication.getInstance().getConnectDevice() != null)
                resetAllLight();
            else {
                ToastUtils.showShort(R.string.device_not_connected);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now));
        SharedPreferencesHelper.putBoolean(getActivity(), Constant.DELETEING, true);
        new Thread(() -> {
            List<DbGroup> list = DBUtils.getGroupList();
            List<DbLight> lightList = new ArrayList<>();
            DeviceInfo deviceInfo = TelinkLightApplication.getInstance().getConnectDevice();
            if (deviceInfo == null) {
                ToastUtils.showLong(R.string.disconected);
                return;
            }

            for (int i = 0; i < list.size(); i++) {
                lightList.addAll(DBUtils.getLightByGroupID(list.get(i).getId()));
            }

            if (lightList.size() == 0) {
                for (int i = 0; i < 3; i++) {
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.KICK_OUT, 0xFFFF, null);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                hideLoadingDialog();
                return;
            }

            int index1 = 0;
            int index2 = 0;
            for (int k = 0; k < lightList.size(); k++) {
                if (lightList.get(k).getMeshAddr() == deviceInfo.meshAddress) {
                    index2 = k;
                    break;
                }
            }
            Collections.swap(lightList, index1, index2);

            for (int j = lightList.size() - 1; j >= 0; j--) {
                if (deviceInfo != null) {
                    try {
                        if (TelinkLightApplication.getInstance().getConnectDevice() == null) {
                            ToastUtils.showLong(R.string.error_disconnect_tip);
                            SharedPreferencesHelper.putBoolean(getActivity(), Constant.DELETEING, false);
                            break;
                        }

                        for (int k = 0; k < 5; k++) {
                            byte opcode = (byte) Opcode.KICK_OUT;
                            TelinkLightService.Instance().sendCommandNoResponse(opcode, lightList.get(j).getMeshAddr(), null);
                            Thread.sleep(sleepTime);
                        }
                        Thread.sleep(sleepTime);
                        DBUtils.deleteLight(lightList.get(j));
                    } catch (InterruptedException e) {
                        hideLoadingDialog();
                        e.printStackTrace();
                    } finally {
                        if (j == 0) {
                            SharedPreferencesHelper.putBoolean(getActivity(), Constant.DELETEING, false);
                            SyncDataPutOrGetUtils.Companion.syncPutDataStart(getActivity(), new SyncCallback() {
                                @Override
                                public void complete() {
                                    hideLoadingDialog();
                                    ActivityUtils.startActivity(SplashActivity.class);
                                    getActivity().finish();
                                }

                                @Override
                                public void error(String msg) {
                                    hideLoadingDialog();
                                    ToastUtils.showShort(R.string.backup_failed);
                                    ActivityUtils.startActivity(SplashActivity.class);
                                    getActivity().finish();
                                }

                                @Override
                                public void start() {

                                }
                            });

                        }
                    }
                }
            }
        }).start();
    }

    public void showLoadingDialog(String content) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialogview, null);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);
        TextView tvContent = (TextView) v.findViewById(R.id.tvContent);
        tvContent.setText(content);

        if (loadDialog == null) {
            loadDialog = new Dialog(getActivity(),
                    R.style.FullHeightDialog);
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog.isShowing()) {
            loadDialog.setCancelable(false);
            loadDialog.setCanceledOnTouchOutside(false);
            loadDialog.setContentView(layout);
            loadDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog.dismiss();
        }
    }

    /**
     * 事件处理方法
     *
     * @param event
     */
    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case NotificationEvent.ONLINE_STATUS:
//                onOnlineStatusNotify((NotificationEvent)event);
                break;
        }
    }

//    private void onOnlineStatusNotify(NotificationEvent event) {
////        List<OnlineStatusNotificationParser.DeviceNotificationInfo> notificationInfoList;
//
//        List<OnlineStatusNotificationParser.DeviceNotificationInfo> notificationInfoList =
//                (List<OnlineStatusNotificationParser.DeviceNotificationInfo>) event.parse();
//
//        if (notificationInfoList.isEmpty())
//            return;
//
//        for (OnlineStatusNotificationParser.DeviceNotificationInfo notificationInfo : notificationInfoList) {
//            int  meshAddress = notificationInfo.meshAddress;
//            if(currentLight!=null && currentLight.getMeshAddr() == meshAddress){
//                isDeleteSuccess=true;
//            }else{
//                isDeleteSuccess=false;
//            }
//        }
//    }

//    private void change(String table,){
//
//    }

    private void exitLogin() {
        isClickExlogin = true;
        if (DBUtils.getAllLight().size() == 0 && !DBUtils.getDataChangeAllHaveAboutLight()) {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
                TelinkLightService.Instance().idleMode(true);

                restartApplication();
            }
            hideLoadingDialog();
        } else {
            checkNetworkAndSync(getActivity());
        }
    }

    long[] mHints = new long[6];//初始全部为0

    private void developerMode() {
        //将mHints数组内的所有元素左移一个位置
        System.arraycopy(mHints, 1, mHints, 0, mHints.length - 1);
        //获得当前系统已经启动的时间
        mHints[mHints.length - 1] = SystemClock.uptimeMillis();
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            ToastUtils.showLong(R.string.developer_mode);
            copyDataBase.setVisibility(View.VISIBLE);
            chearCache.setVisibility(View.VISIBLE);
            SharedPreferencesUtils.setDeveloperModel(true);
        }
    }

    //清空缓存初始化APP
    private void emptyTheCache() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.empty_cache_title))
                .setMessage(getActivity().getString(R.string.empty_cache_tip))
                .setNegativeButton(getActivity().getString(R.string.btn_cancel), (dialog, which) -> {
                })
                .setPositiveButton(getActivity().getString(R.string.btn_sure), (dialog, which) -> {
                    clearData();
                })
                .create().show();
    }

    private void clearData() {

//        SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
//        DBUtils.deleteAllData();
//        CleanUtils.cleanInternalSp();
//        CleanUtils.cleanExternalCache();
//        CleanUtils.cleanInternalFiles();
//        CleanUtils.cleanInternalCache();
//        ToastUtils.showShort(R.string.clean_tip);
//        hideLoadingDialog();
//        restartApplication();
//
        DbUser dbUser = DBUtils.getLastUser();

        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty);
            return;
        }

        showLoadingDialog(getString(R.string.clear_data_now));
        UserModel.INSTANCE.deleteAllData(dbUser.getToken()).subscribe(new NetworkObserver<String>() {
            @Override
            public void onNext(String s) {
                SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
                SharedPreferencesHelper.putObject(getActivity(), Constant.OLD_INDEX_DATA, null);
                DBUtils.deleteAllData();
                CleanUtils.cleanInternalSp();
                CleanUtils.cleanExternalCache();
                CleanUtils.cleanInternalFiles();
                CleanUtils.cleanInternalCache();
                ToastUtils.showShort(R.string.clean_tip);
                hideLoadingDialog();

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                restartApplication();
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                ToastUtils.showLong(R.string.clear_fail);
                hideLoadingDialog();
            }
        });
    }

    //重启app并杀死原进程
    private void restartApplication() {
        ActivityUtils.finishAllActivities(true);
        ActivityUtils.startActivity(SplashActivity.class);
    }
}