package com.dadoutek.uled.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.SyncCallback;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.othersview.SplashActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;

import static android.provider.Settings.ACTION_BLUETOOTH_SETTINGS;

public class BluetoothConnectionFailedDialog extends AlertDialog implements View.OnClickListener {

    private TextView openBleBtn;
    private TextView restartBleBtn;
    private TextView locationBtn;
    private TextView restartAppBtn;
    private Button cancelBtn;
    private Context mContenxt;

    private boolean isClickExlogin = false;

    public BluetoothConnectionFailedDialog(Context context, int style) {
        super(context, style);
        this.mContenxt = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_connection_failed);
        openBleBtn = (TextView) findViewById(R.id.open_bluetooth);
        restartBleBtn = (TextView) findViewById(R.id.restart_bluetooth);
        locationBtn = (TextView) findViewById(R.id.open_location);
        restartAppBtn = (TextView) findViewById(R.id.restart_app);
        cancelBtn = (Button) findViewById(R.id.okBtn);

        openBleBtn.setOnClickListener(this);
        restartBleBtn.setOnClickListener(this);
        locationBtn.setOnClickListener(this);
        restartAppBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    private Intent intent;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_bluetooth:
            case R.id.restart_bluetooth:
                intent = new Intent(ACTION_BLUETOOTH_SETTINGS);
                mContenxt.startActivity(intent);
                dismiss();
                break;

            case R.id.open_location:
                intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContenxt.startActivity(intent);
                break;

            case R.id.restart_app:
                isClickExlogin = true;
                if (DBUtils.INSTANCE.getAllLight().size() == 0 && !DBUtils.INSTANCE.getDataChangeAllHaveAboutLight() && DBUtils.INSTANCE.getAllCurtain().size() == 0 && !DBUtils.INSTANCE.getDataChangeAllHaveAboutCurtain() && DBUtils.INSTANCE.getAllRely().size() == 0 && !DBUtils.INSTANCE.getDataChangeAllHaveAboutRelay()) {
                    if (isClickExlogin) {
                        SharedPreferencesHelper.putBoolean(mContenxt, Constant.IS_LOGIN, false);
                        TelinkLightService instance = TelinkLightService.Instance();
                        if (instance!=null)
                            instance.idleMode(true);

                        restartApplication();
                    }
                } else {
                    checkNetworkAndSync(mContenxt);
                }
                break;

            case R.id.okBtn:
                dismiss();
                break;

            default:
                break;
        }
    }

    private void checkNetworkAndSync(Context mContenxt) {
        if (!NetWorkUtils.isNetworkAvalible(mContenxt)) {
            new Builder(mContenxt)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        Intent intents = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        mContenxt.startActivity(intents);
                    }).show();
            // 跳转到设置界面

        } else {
            SyncDataPutOrGetUtils.Companion.syncPutDataStart(mContenxt, syncCallback);
        }
    }

    //重启app并杀死原进程
    private void restartApplication() {
//        TelinkLightApplication.Companion.getApp().releseStomp();
        ActivityUtils.finishAllActivities(true);
        ActivityUtils.startActivity(SplashActivity.class);
        TelinkLightApplication.Companion.getApp().doDestroy();
        AppUtils.relaunchApp();
    }

    private SyncCallback syncCallback = (new SyncCallback() {
        public void start() {
        }

        public void complete() {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(mContenxt, "IS_LOGIN", false);
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance!=null)
                    instance.idleMode(true);
                restartApplication();
            }
        }

        public void error(String msg) {
            if (isClickExlogin) {
                new Builder(mContenxt)
                        .setTitle(R.string.sync_error_exlogin)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(R.string.confirm, (dialog, which) -> {
                            SharedPreferencesHelper.putBoolean(mContenxt, Constant.IS_LOGIN, false);
                            TelinkLightService instance = TelinkLightService.Instance();
                            if (instance!=null)
                                instance.idleMode(true);
                            dismiss();
                            restartApplication();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dismiss();
                            isClickExlogin = false;
                        }).show();
            }
        }
    });
}
