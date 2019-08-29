package com.dadoutek.uled.tellink;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.DaoSessionUser;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.mob.MobSDK;
import com.telink.TelinkApplication;
import com.telink.bluetooth.TelinkLog;
import com.tencent.bugly.crashreport.CrashReport;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

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
    static TelinkLightApplication mInstance = null;

    private Intent intent;

    public static Context getContext() {
        return mInstance;
    }

    @SuppressLint("SdCardPath")
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "ea665087a5", false);
        DaoSessionInstance.checkAndUpdateDatabase();
        DaoSessionUser.checkAndUpdateDatabase();
        ZXingLibrary.initDisplayOpinion(this);

        if (null == mInstance) {
            mInstance = this;
        }

        Utils.init(this);
        LogUtils.getConfig().setBorderSwitch(false);
        if (!AppUtils.isAppDebug()) {
//            LogUtils.getConfig().setLogSwitch(false);
//            LogUtils.getConfig().setLog2FileSwitch(false);
        } else {
            LogUtils.getConfig().setLog2FileSwitch(true);
            //        LogUtils.getConfig().setDir("/mnt/sdcard/log");
        }
//        registerBluetoothReceiver();
        MobSDK.init(this);
//        CrashReport.testJavaCrash();

        logInfo = new StringBuilder("log:");
        thiz = this;
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

//        intent = new Intent(this,SendLightsInfo.class);
//        startService(intent);

    }

    public static TelinkLightApplication getApp() {
        return thiz;
    }

    @Override
    public void doInit() {
        super.doInit();
        //AES.Security = true;

        long currentRegionID = SharedPreferencesUtils.getCurrentUseRegion();


        // 此处直接赋值是否可以 --->原逻辑 保存旧的区域信息 保存 区域id  通过区域id查询 再取出name pwd  直接赋值
        //切换区域记得断开连接

        if (currentRegionID != -1) {
            DbRegion dbRegion = DBUtils.INSTANCE.getCurrentRegion(currentRegionID);
            //if (dbRegion != null) {
            DbUser lastUser = DBUtils.INSTANCE.getLastUser();
            if (lastUser == null) return;
            String name = lastUser.getControlMeshName();//dbRegion.getControlMesh();
            String pwd = lastUser.getControlMeshPwd();//dbRegion.getControlMeshPwd();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {
                //("mesh.setPassword = " + name);
                mesh = new Mesh();
                mesh.setName(name);
                mesh.setPassword(name);
                mesh.setFactoryName(Constant.DEFAULT_MESH_FACTORY_NAME);
                mesh.setFactoryPassword(Constant.DEFAULT_MESH_FACTORY_PASSWORD);
                setupMesh(mesh);
            }
            //}
        }

        //启动LightService
        this.startLightService(TelinkLightService.class);
    }

    @Override
    public void doDestroy() {
        TelinkLog.onDestroy();
        super.doDestroy();
//        stopService(intent);
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
        }
        this.mesh.setFactoryName(Constant.DEFAULT_MESH_FACTORY_NAME);
        this.mesh.setFactoryPassword(Constant.DEFAULT_MESH_FACTORY_PASSWORD);

        return this.mesh;
    }

    public void setupMesh(Mesh mesh) {
        this.mesh = mesh;
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
