package com.dadoutek.uled.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.CleanUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.EmptyAddActivity;
import com.dadoutek.uled.activity.ManagerVerificationActivity;
import com.dadoutek.uled.activity.PhoneVerificationActivity;
import com.dadoutek.uled.activity.SplashActivity;
import com.dadoutek.uled.model.Cmd;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;

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
    private LayoutInflater inflater;

    private Dialog loadDialog;
    private TelinkLightApplication mApplication;
    private DbLight currentLight;
    private boolean isDeleteSuccess = false;

    private long sleepTime = 250;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mApplication = (TelinkLightApplication) getActivity().getApplication();
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(NotificationEvent.ONLINE_STATUS, this);

        if (android.os.Build.BRAND.contains("Huawei")) {
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
            chearCache.setVisibility(View.GONE);
        } else {
            copyDataBase.setVisibility(View.GONE);
            chearCache.setVisibility(View.GONE);
        }
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

    @OnClick({R.id.chear_cache, R.id.update_ite, R.id.copy_data_base, R.id.app_version, R.id.exit_login, R.id.one_click_backup, R.id.one_click_reset})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.chear_cache:
                emptyTheCache();
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
//                Intent intent=new Intent(getActivity(), ManagerVerificationActivity.class);
//                intent.putExtra(Constant.ME_FUNCTION,"me_sync");
//                startActivityForResult(intent,0);
                ToastUtils.showLong(R.string.devoloping);
                break;
            case R.id.one_click_reset:
                Intent intent1=new Intent(getActivity(), ManagerVerificationActivity.class);
                intent1.putExtra(Constant.ME_FUNCTION,"me_reset");
                startActivityForResult(intent1,0);
                break;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0 && resultCode==RESULT_OK){
           if(data.getExtras().getString(Constant.ME_FUNCTION).equals("me_sync")){
               checkNetworkAndSync(getActivity(),handler);
           }else if(data.getExtras().getString(Constant.ME_FUNCTION).equals("me_reset")){
               showSureResetDialog();
           }
        }
    }

    // 如果没有网络，则弹出网络设置对话框
    public static void checkNetworkAndSync(final Activity activity,Handler handler) {
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
        }else{
            SyncDataPutOrGetUtils.Companion.syncPutDataStart(activity,handler);
        }
    }

    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Cmd.SYNCCMD:
                    showLoadingDialog(getActivity().getString(R.string.tip_start_sync));
                    break;
                case Cmd.SYNCCOMPLETCMD:
                    hideLoadingDialog();
                    break;
                case Cmd.SYNCERRORCMD:
                    hideLoadingDialog();
                    break;
            }
        }
    };

    private void showSureResetDialog() {
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
                hideLoadingDialog();
                ToastUtils.showLong(R.string.reset_fail_tip1);
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
                            hideLoadingDialog();
                            SharedPreferencesHelper.putBoolean(getActivity(), Constant.DELETEING, false);
                            getActivity().startActivity(new Intent(getActivity(), EmptyAddActivity.class));
                            getActivity().finish();
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
        SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
        restartApplication();
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
            chearCache.setVisibility(View.GONE);
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
                    DBUtils.deleteAllData();
                    CleanUtils.cleanInternalSp();
                    CleanUtils.cleanExternalCache();
                    CleanUtils.cleanInternalFiles();
                    CleanUtils.cleanInternalCache();
                    ToastUtils.showShort(R.string.clean_tip);
                    restartApplication();
                })
                .create().show();
    }

    //重启app并杀死原进程
    private void restartApplication() {
        ActivityUtils.startActivity(SplashActivity.class);
        ActivityUtils.finishAllActivitiesExceptNewest();
    }
}
