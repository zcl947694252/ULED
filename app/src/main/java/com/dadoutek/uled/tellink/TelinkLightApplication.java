package com.dadoutek.uled.tellink;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Config;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.mob.MobSDK;
import com.telink.TelinkApplication;
import com.telink.bluetooth.TelinkLog;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.BuglyLog;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.dadoutek.uled.BuildConfig.DEBUG;


public final class TelinkLightApplication extends TelinkApplication {

    private Mesh mesh;
    private StringBuilder logInfo;
    private static TelinkLightApplication thiz;
//    private List<String> macFilters = new ArrayList<>();

    private Toast toast;
    private int onlineCount = 0;

    private static DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();
        //this.doInit();
        Utils.init(this);
//        if (!AppUtils.isAppDebug()) {
//            LogUtils.getConfig().setLogSwitch(false);
//        }

//        DaoSessionInstance.destroySession();
//        DaoSessionInstance.getInstance();

        MobSDK.init(this);
        CrashReport.initCrashReport(getApplicationContext(), "ea665087a5", false);
//        CrashReport.testJavaCrash();

        logInfo = new StringBuilder("log:");
        thiz = this;
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

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
    }

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
