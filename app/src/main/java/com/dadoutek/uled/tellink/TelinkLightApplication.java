package com.dadoutek.uled.tellink;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.DaoSessionUser;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.mob.MobSDK;
import com.telink.TelinkApplication;
import com.telink.bluetooth.TelinkLog;
import com.tencent.bugly.crashreport.CrashReport;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public final class TelinkLightApplication extends TelinkApplication {

    private Mesh mesh;
    private StringBuilder logInfo;
    private static TelinkLightApplication thiz;
//    private List<String> macFilters = new ArrayList<>();

    private Toast toast;
    private int onlineCount = 0;

//    private BluetoothStateBroadcastReceive mReceive;

    private static DaoSession daoSession;
    private Object Toolbar;

    @SuppressLint("SdCardPath")
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "ea665087a5", false);
        DaoSessionInstance.checkAndUpdateDatabase();
        DaoSessionUser.checkAndUpdateDatabase();

        Utils.init(this);
        if (!AppUtils.isAppDebug()) {
            LogUtils.getConfig().setLogSwitch(false);
//            LogUtils.getConfig().setLog2FileSwitch(false);
        }else{
//            LogUtils.getConfig().setLog2FileSwitch(true);
            //        LogUtils.getConfig().setDir("/mnt/sdcard/log");
        }
//        registerBluetoothReceiver();
        MobSDK.init(this);
//        CrashReport.testJavaCrash();

        logInfo = new StringBuilder("log:");
        thiz = this;
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

//    private void registerBluetoothReceiver(){
//        if(mReceive == null){
//            mReceive = new BluetoothStateBroadcastReceive();
//        }
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
//        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
//        registerReceiver(mReceive, intentFilter);
//    }

    public static TelinkLightApplication getApp() {
        return thiz;
    }

    @Override
    public void doInit() {
        super.doInit();
        //AES.Security = true;

        long currentRegionID = SharedPreferencesUtils.getCurrentUseRegion();


        if (currentRegionID != -1) {
            DbRegion dbRegion = DBUtils.INSTANCE.getCurrentRegion(currentRegionID);

            if (dbRegion != null) {
                String name = dbRegion.getControlMesh();
                String pwd = dbRegion.getControlMeshPwd();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {
                    mesh = new Mesh();
                    mesh.setName(name);
                    mesh.setPassword(pwd);
                    mesh.setFactoryName(Constant.DEFAULT_MESH_FACTORY_NAME);
                    mesh.setFactoryPassword(Constant.DEFAULT_MESH_FACTORY_PASSWORD);
                    setupMesh(mesh);
                }
            }
        }

        //启动LightService
        this.startLightService(TelinkLightService.class);
    }

    @Override
    public void doDestroy() {
        TelinkLog.onDestroy();
        super.doDestroy();
//        unregisterBluetoothReceiver();
    }

//    private void unregisterBluetoothReceiver(){
//        if(mReceive != null){
//            unregisterReceiver(mReceive);
//            mReceive = null;
//        }
//    }

    public Mesh getMesh() {
        if (this.mesh == null) {
            this.mesh = new Mesh();
            this.mesh.setFactoryName(Constant.DEFAULT_MESH_FACTORY_NAME);
            this.mesh.setFactoryPassword(Constant.DEFAULT_MESH_FACTORY_PASSWORD);
        }else{
            this.mesh.setFactoryName(Constant.DEFAULT_MESH_FACTORY_NAME);
            this.mesh.setFactoryPassword(Constant.DEFAULT_MESH_FACTORY_PASSWORD);
        }
        return this.mesh;
    }

    public void setupMesh(Mesh mesh) {
        this.mesh = mesh;
        refreshLights();
    }

    public void refreshLights() {
//        if (mesh != null && mesh.devices != null) {
//            Lights.getInstance().clear();
//            Light light;
//            for (DeviceInfo deviceInfo : mesh.devices) {
//                light = new Light();
//                light.meshAddress = deviceInfo.meshAddress;
//                light.brightness = 0;
//                light.status = ConnectionStatus.OFFLINE;
//                light.textColor = this.getResources().getColor(
//                        R.color.black);
//                light.updateIcon();
//
//                Lights.getInstance().add(light);
//            }
//        }
    }


    public boolean isEmptyMesh() {

        return this.mesh == null || TextUtils.isEmpty(mesh.getName()) || TextUtils.isEmpty(mesh.getPassword());
    }

    /**********************************************
     * Log api
     **********************************************/

//    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.S");

    @Override
    public void saveLog(String action) {

//        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
//        Date date = sdf.parse(dateInString);
        ;
        String time = format.format(Calendar.getInstance().getTimeInMillis());
        logInfo.append("\n\t").append(time).append(":\t").append(action);
        /*if (Looper.myLooper() == Looper.getMainLooper()) {
            showToast(action);
        }*/

        TelinkLog.w("SaveLog: " + action);
    }

    public void saveLogInFile(String fileName, String logInfo) {
        if (FileSystem.writeAsString(fileName + ".txt", logInfo)) {
            showToast("save success --" + fileName);
        }
    }

//    public class BluetoothStateBroadcastReceive extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            switch (action){
//                case BluetoothDevice.ACTION_ACL_CONNECTED:
//                    Toast.makeText(context , "蓝牙设备:" + device.getName() + "已链接", Toast.LENGTH_SHORT).show();
//                    break;
//                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
//                    Toast.makeText(context , "蓝牙设备:" + device.getName() + "已断开", Toast.LENGTH_SHORT).show();
//                    break;
//                case BluetoothAdapter.ACTION_STATE_CHANGED:
//                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
//                    switch (blueState){
//                        case BluetoothAdapter.STATE_OFF:
//                            Toast.makeText(context , "蓝牙已关闭", Toast.LENGTH_SHORT).show();
//////                            (ImageView)findViewById(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no);
//                            break;
//                        case BluetoothAdapter.STATE_ON:
//                            Toast.makeText(context , "蓝牙已开启"  , Toast.LENGTH_SHORT).show();
//                            break;
//                    }
//                    break;
//            }
//        }
//
//    }


    public void showToast(CharSequence s) {

        if (this.toast != null) {
            this.toast.setView(this.toast.getView());
            this.toast.setDuration(Toast.LENGTH_SHORT);
            this.toast.setText(s);
            this.toast.show();
        }
    }

    public String getLogInfo() {
        return logInfo.toString();
    }

    public void clearLogInfo() {
//        logInfo.delete(0, logInfo.length() - 1);
        logInfo = new StringBuilder("log:");
    }

}
