package com.dadoutek.uled.tellink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.util.ContextUtil;
import com.telink.util.Event;
import com.telink.util.EventListener;


// 添加 扫描过程中出现的因定位未开启而导致的扫描不成功问题
public abstract class TelinkMeshErrorDealActivity extends TelinkBaseActivity implements EventListener<String> {
//如果这个子类不是一个抽象类,那么你必须实现你所继承的所有抽象方法
    protected final static int ACTIVITY_REQUEST_CODE_LOCATION = 0x11;
    private AlertDialog mErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TelinkLightApplication) getApplication()).addEventListener(MeshEvent.ERROR, this);
    }

    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case MeshEvent.ERROR:
                onMeshError((MeshEvent) event);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((TelinkLightApplication) getApplication()).removeEventListener(this);
        if (mErrorDialog != null)
            mErrorDialog.dismiss();
    }

    private void dismissDialog() {

        if (mErrorDialog != null && mErrorDialog.isShowing()) {
            mErrorDialog.dismiss();
        }
    }

    protected void onMeshError(MeshEvent event) {
        if (event.getArgs() == LeBluetooth.SCAN_FAILED_LOCATION_DISABLE) {
            if (mErrorDialog == null) {
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance != null)
                    instance.idleMode(true);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Error")
                        .setMessage(this.getString(R.string.open_gps_tip))
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(this.getString(R.string.go_open), (dialogInterface, i) -> {
                            Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(enableLocationIntent, ACTIVITY_REQUEST_CODE_LOCATION);
                        });
                mErrorDialog = dialogBuilder.create();
            }
            mErrorDialog.show();
        } else {
            mErrorDialog = new AlertDialog.Builder(this).setMessage("蓝牙出问题了，重启蓝牙试试!!").create();
            mErrorDialog.show();
        }
    }

    protected abstract void onLocationEnable();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_LOCATION) {
            if (ContextUtil.isLocationEnable(this)) {
                dismissDialog();
                onLocationEnable();
            }
        }
    }
}
