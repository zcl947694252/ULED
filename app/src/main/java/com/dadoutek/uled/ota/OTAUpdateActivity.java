package com.dadoutek.uled.ota;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.OtaDevice;
import com.dadoutek.uled.network.NetworkFactory;
import com.dadoutek.uled.othersview.FileSelectActivity;
import com.dadoutek.uled.othersview.MainActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.util.AppUtils;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.StringUtils;
import com.dinuscxj.progressbar.CircleProgressBar;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.LeScanEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeScanParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.OtaDeviceInfo;
import com.telink.bluetooth.light.Parameters;
import com.telink.util.Event;
import com.telink.util.EventListener;
import com.telink.util.Strings;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 升级页面
 * 思路：
 * 1.由于demo中没有对设备持久化操作，只有通过online_status 来添加和更新设备，
 * 而online_status返回的数据中只有meshAddress能判断设备唯一性
 * 在OTA升级过程中会保存此时现在的所有设备信息（onlineLights），
 * 如果是从MainActivity页面跳转而来（1.自动连接的设备有上报MeshOTA进度信息；2主动连接之前本地保存的设备），需要读取一次版本信息\n
 * 并初始化onlineLights
 * 1. {@link MainActivity#onNotificationEvent(NotificationEvent)}；
 * 2. {@link MainActivity#onDeviceStatusChanged(DeviceEvent)}；
 * <p>
 * 在开始OTA或者MeshOTA之前都会获取当前设备的OTA状态信息 {@link OTAUpdateActivity#sendGetDeviceOtaStateCommand()},
 * \n\t 并通过 {@link OTAUpdateActivity#onNotificationEvent(NotificationEvent)}返回状态， 处理不同模式下的不同状态
 * 在continue MeshOTA和MeshOTA模式下 {@link OTAUpdateActivity#MODE_CONTINUE_MESH_OTA},{@link OTAUpdateActivity#MODE_MESH_OTA}
 * <p>
 * <p>
 * 校验通过后，会开始动作
 * <p>
 * <p>
 * Action Start by choose correct bin file!
 * <p>
 * Created by Administrator on 2017/4/20.
 */
public class OTAUpdateActivity extends TelinkMeshErrorDealActivity implements EventListener<String> {
    @BindView(R.id.progress_view)
    CircleProgressBar progress_view;
    @BindView(R.id.text_info)
    TextView text_info;
    @BindView(R.id.select)
    LinearLayout select;
    @BindView(R.id.btn_start_update)
    Button btn_start_update;
    @BindView(R.id.tvFile)
    TextView tvFile;
    @BindView(R.id.local_version)
    TextView local_version;
    @BindView(R.id.server_version)
    TextView server_version;
    private int mode = MODE_IDLE;
    private static final int MODE_IDLE = 1;
    private static final int MODE_OTA = 2;
    private static final int MODE_MESH_OTA = 4;
    private static final int MODE_CONTINUE_MESH_OTA = 8;
    private static final int MODE_COMPLETE = 16;

    public static final String INTENT_KEY_CONTINUE_MESH_OTA = "com.telink.bluetooth.light.INTENT_KEY_CONTINUE_MESH_OTA";
    // 有进度状态上报 时跳转进入的
    public static final int CONTINUE_BY_REPORT = 0x21;

    // 继续之前的OTA操作，连接指定设备
    public static final int CONTINUE_BY_PREVIOUS = 0x22;

    private int continueType = 0;

    private static final int REQUEST_CODE_CHOOSE_FILE = 11;

    private PowerManager.WakeLock mWakeLock = null;

    //    private static final int MODE_SCAN = 8;
    private byte[] mFirmwareData;
    private List<DbLight> onlineLights;
    private DbLight dbLight;
    private Mesh mesh;
    private String mPath;
    private SimpleDateFormat mTimeFormat;
    private int successCount = 0;

    private TextView otaProgress;
    private Toolbar toolbar;
    private TextView meshOtaProgress;
    private TextView tv_log, tv_version;
    private ScrollView sv_log;

    private static final int MSG_OTA_PROGRESS = 11;
    private static final int MSG_MESH_OTA_PROGRESS = 12;
    private static final int MSG_LOG = 13;
    private static final int TIME_OUT_SCAN = 20;
    private static final int TIME_OUT_CONNECT = 15;
    private Disposable mSendataDisposal;
    private long TIME_OUT_SENDDATA = 10;

    private Handler delayHandler = new Handler();
    private Handler visibleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ((View) msg.obj).setVisibility(msg.what);
        }
    };
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_OTA_PROGRESS:
                    if ((Integer) msg.obj < 100) {
                        btn_start_update.setText(R.string.updating);
                    }
                    otaProgress.setText(getString(R.string.progress_ota, msg.obj.toString()));
                    progress_view.setProgress((Integer) msg.obj);
                    break;

                case MSG_MESH_OTA_PROGRESS:
                    meshOtaProgress.setText(getString(R.string.progress_mesh_ota, msg.obj.toString()));
                    progress_view.setProgress((Integer) msg.obj);
                    break;

                case MSG_LOG:
                    if (SharedPreferencesUtils.isDeveloperModel()) {
                        sv_log.setVisibility(View.VISIBLE);
                        String time = mTimeFormat.format(Calendar.getInstance().getTimeInMillis());
                        tv_log.append("\n" + time + ":" + msg.obj.toString());
//
////                    int scroll_amount = tv_log.getBottom();
////                    tv_log.scrollTo(0, scroll_amount);
                        sv_log.fullScroll(View.FOCUS_DOWN);
////                    ((ScrollView) tv_log.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
                        LogUtils.d("\n" + time + ":" + msg.obj.toString());
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_update);
        ButterKnife.bind(this);
        mesh = TelinkLightApplication.getApp().getMesh();
        if (mesh == null || TextUtils.isEmpty(mesh.getName()) || TextUtils.isEmpty(mesh.getPassword())) {
            toast("Mesh Error!");
            finish();
            return;
        }

        initData();
        initView();
    }

    private void initData() {
        dbLight = (DbLight) getIntent().getSerializableExtra(Constant.UPDATE_LIGHT);
        log("current-light-mesh" + dbLight.getMeshAddr());

        onlineLights = new ArrayList<>();
        onlineLights.add(dbLight);

        log("onlineLights:" + onlineLights.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }

    private void initView() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        addEventListener();
        TelinkLightService.Instance().enableNotification();
        mTimeFormat = new SimpleDateFormat("HH:mm:ss.S");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar();
        otaProgress = (TextView) findViewById(R.id.progress_ota);
        meshOtaProgress = (TextView) findViewById(R.id.progress_mesh_ota);
        tv_log = (TextView) findViewById(R.id.tv_log);
        sv_log = (ScrollView) findViewById(R.id.sv_log);
        tv_version = (TextView) findViewById(R.id.tv_version);

        if (!SharedPreferencesUtils.getUpdateFilePath().isEmpty()) {
            mPath = SharedPreferencesUtils.getUpdateFilePath();
            tvFile.setText(getString(R.string.select_file, mPath));
            btn_start_update.setVisibility(View.VISIBLE);
            local_version.setVisibility(View.VISIBLE);
            local_version.setText(getString(R.string.local_version, dbLight.version));
            server_version.setVisibility(View.VISIBLE);
            server_version.setText(getString(R.string.server_version, StringUtils.versionResolutionURL(mPath, 2)));
        }
    }

        private void initToolbar() {
        toolbar.setTitle(R.string.ota_update_title);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

        @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.select, R.id.btn_start_update})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.select:
                chooseFile();
                break;
            case R.id.btn_start_update:
                updateStep1();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onLocationEnable() {
        startScan();
    }

    private void addEventListener() {
        TelinkLightApplication.getApp().addEventListener(LeScanEvent.LE_SCAN, this);
//        TelinkLightApplication.getApp().addEventListener(LeScanEvent.LE_SCAN_COMPLETED, this);
        TelinkLightApplication.getApp().addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        TelinkLightApplication.getApp().addEventListener(DeviceEvent.STATUS_CHANGED, this);
        TelinkLightApplication.getApp().addEventListener(NotificationEvent.GET_DEVICE_STATE, this);
    }


    /**
     * Action start: after get versions
     * hasHigh Confirm action OTA or MeshOTA
     * hasLow Confirm OTA needed
     */
    private void start() {

        boolean hasHigh = false;
        boolean hasLow = false;
        for (DbLight light : onlineLights) {
            if (light.version == null || light.version.equals("")) continue;
            int compare = compareVersion(light.version, mFileVersion);
            if (compare == 0) {
                hasHigh = true;
            } else if (compare == 1) {
                hasLow = true;
            }
        }

        if (hasLow) {
//            if (hasHigh) {
//                startMeshOTA();
//            } else {
            this.mode = MODE_OTA;

            int curMeshAddress = TelinkLightApplication.getApp().getConnectDevice().meshAddress;
            DbLight light = getLightByMeshAddress(curMeshAddress);
            if (light != null && compareVersion(light.version, mFileVersion) == 1) {
                sendGetDeviceOtaStateCommand();
            } else {
                startScan();
            }


//            }
        } else {
            text_info.setVisibility(View.VISIBLE);
            text_info.setText(R.string.the_last_version);
            btn_start_update.setVisibility(View.GONE);
            log("No device need OTA! Idle");
            select.setEnabled(true);
            this.mode = MODE_IDLE;
        }
    }


    /**
     * 判断当前连接的设备是否是高版本
     * true: sendCommand
     * false: connectDevice
     */
    private void startMeshOTA() {
        this.mode = MODE_MESH_OTA;
        DeviceInfo deviceInfo = TelinkLightApplication.getApp().getConnectDevice();
        boolean action = false;
        if (deviceInfo != null) {
            for (DbLight light : onlineLights) {
                if (light.getMeshAddr() == deviceInfo.meshAddress && light.version != null && light.version.equals(mFileVersion)) {
                    action = true;
                    break;
                }
            }
        }

        if (action) {
            sendGetDeviceOtaStateCommand();
        } else {
            // scan and connect high version
//            startScan();
        }
    }

    private int otaStateTimeout = 0;
    private int OTA_STATE_TIMEOUT_MAX = 3;

    // 获取本地设备OTA状态信息
    private void sendGetDeviceOtaStateCommand() {
        otaStateTimeout = 0;
        delayHandler.post(deviceOtaStateTimeoutTask);
    }

    private Runnable deviceOtaStateTimeoutTask = new Runnable() {
        @Override
        public void run() {

            if (otaStateTimeout < OTA_STATE_TIMEOUT_MAX) {
                byte opcode = (byte) 0xC7;
                int address = 0x0000;
                byte[] params = new byte[]{0x20, 0x05};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params);
                log("SendCommand 0xC7 getDeviceOtaState");
                otaStateTimeout++;
                delayHandler.postDelayed(this, 3000);
            } else {
                log("SendCommand 0xC7 getDeviceOtaState fail");
                delayHandler.removeCallbacks(this);
                if (mode == MODE_OTA) {
                    startOTA();
                } else if (mode == MODE_MESH_OTA) {
                    sendStartMeshOTACommand();
                } else if (mode == MODE_CONTINUE_MESH_OTA) {
                    sendGetVersionCommand();
                }
            }

        }
    };


    private List<Integer> versionDevices = new ArrayList<>();
    private int retryCount = 0;

    private void sendGetVersionCommand() {
        versionDevices.clear();
        byte opcode = (byte) 0xC7;
        int address = dbLight.getMeshAddr();
        byte[] params = new byte[]{0x20, 0x00};
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params);
        log("SendCommand 0xC7 getVersion");
        // 转发次数 * () * interval + 500
        if (this.mode != MODE_COMPLETE)
            delayHandler.postDelayed(getVersionTask, 0x20 * 2 * 40 + 500);
    }

    private Runnable getVersionTask = new Runnable() {
        @Override
        public void run() {
            if (versionDevices.size() == onlineLights.size() || retryCount >= 2) {
                retryCount = 0;
                if (mode == MODE_IDLE) {
                    start();
                } else if (mode == MODE_CONTINUE_MESH_OTA || mode == MODE_MESH_OTA) {
                    if (hasLow()) {
                        sendStartMeshOTACommand();
                    } else {
                        log("No device need OTA! Stop");
                        doFinish();
                    }
                }
            } else {
                retryCount++;
                log("get version retry");
                sendGetDeviceOtaStateCommand();
            }
        }
    };

    private boolean hasLow() {
        boolean hasLow = false;
        for (DbLight light : onlineLights) {
            if (light.version == null || light.version.equals("")) continue;
            if (compareVersion(light.version, mFileVersion) == 1) {
                hasLow = true;
            }
        }

        return hasLow;
    }

    private String mFileVersion;


    public void connectDevice(String mac) {
        log("connectDevice :" + mac);
        btn_start_update.setText(R.string.start_connect);
        TelinkLightService.Instance().connect(mac, TIME_OUT_CONNECT);
    }

    private void login() {
        log("login");
        String account = DBUtils.INSTANCE.getLastUser().getAccount();
        String pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16);
        TelinkLightService.Instance().login(Strings.stringToBytes(account, 16), Strings.stringToBytes(pwd, 16));
    }

    private boolean isConnectting = false;

    @Override
    protected void onPause() {
        super.onPause();
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }

        stopConnectTimer();
        stopScanTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LeBluetooth.getInstance().stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TelinkLog.i("OTAUpdate#onStop#removeEventListener");
        TelinkLightApplication.getApp().removeEventListener(this);
        if (this.delayHandler != null) {
            this.delayHandler.removeCallbacksAndMessages(null);
        }
        TelinkLightApplication.getApp().removeEventListener(this);
    }

    private void updateSuccess() {
        doFinish();
        text_info.setVisibility(View.VISIBLE);
        text_info.setText(R.string.updateSuccess);
        btn_start_update.setVisibility(View.GONE);

        ToastUtils.showLong(R.string.exit_update);
        new Handler().postDelayed(() -> {
            finish();
        }, 2000);
    }

    private void doFinish() {
        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("Finish: Success Count : " + successCount);
        if (successCount == 0) {
            startScan();
        }
    }

    AlertDialog.Builder mScanTimeoutDialog;

    private void log(String log) {
        msgHandler.obtainMessage(MSG_LOG, log).sendToTarget();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finish() {
        super.finish();
        if (this.mode == MODE_MESH_OTA || this.mode == MODE_CONTINUE_MESH_OTA) {
            this.sendStopMeshOTACommand();
        }
        this.mode = MODE_COMPLETE;
        TelinkLightService.Instance().idleMode(true);
        TelinkLog.i("OTAUpdate#onStop#removeEventListener");
        TelinkLightApplication.getApp().removeEventListener(this);
    }

    AlertDialog.Builder mCancelBuilder;

    public void back() {
        if (this.mode == MODE_COMPLETE || this.mode == MODE_IDLE) {
            finish();
        } else {
            if (mCancelBuilder == null) {
                mCancelBuilder = new AlertDialog.Builder(this);
                mCancelBuilder.setTitle(getString(R.string.warning));
                mCancelBuilder.setMessage(getString(R.string.is_exit_ota));
                mCancelBuilder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                    sendStopMeshOTACommand();
                    Mesh mesh = TelinkLightApplication.getApp().getMesh();
                    mesh.setOtaDevice(null);
                    mesh.saveOrUpdate(OTAUpdateActivity.this);
                    dialog.dismiss();
                    finish();
                });
                mCancelBuilder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) ->
                        dialog.dismiss()
                );
            }
            mCancelBuilder.show();
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void parseFile() {
        try {
            byte[] version = new byte[4];
            InputStream stream = new FileInputStream(mPath);
            int length = stream.available();
            mFirmwareData = new byte[length];
            stream.read(mFirmwareData);

            stream.close();
            System.arraycopy(mFirmwareData, 2, version, 0, 4);
            mFileVersion = new String(version);
        } catch (Exception e) {
            mFileVersion = null;
            mFirmwareData = null;
            mPath = null;
        }
        //  || mFileVersion.charAt(0) != 'V'
        if (mFileVersion == null) {
            Toast.makeText(this, "File parse error!", Toast.LENGTH_SHORT).show();
            this.mPath = null;
            mFileVersion = null;
            tvFile.setText(getString(R.string.select_file, "NULL"));
            tv_version.setText("File parse error!");
        } else {
            tv_version.setText("File Version: " + mFileVersion);
            tv_version.setVisibility(View.GONE);
            select.setEnabled(false);
            text_info.setVisibility(View.GONE);
            btn_start_update.setVisibility(View.VISIBLE);
            btn_start_update.setClickable(false);
//            btn_start_update.setText(R.string.updating);
//            btn_start_update.setText(R.string.scan_and_connect);
//            if (TelinkLightApplication.getApp().getConnectDevice() != null) {
//                sendGetVersionCommand();
//            } else {
            if (TelinkLightApplication.getInstance().getConnectDevice() != null &&
                    TelinkLightApplication.getInstance().getConnectDevice().meshAddress == dbLight.getMeshAddr()) {
                startOTA();
            } else {
                startScan();
            }
//            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_FILE && resultCode == RESULT_OK) {
            mPath = data.getStringExtra("path");
            tvFile.setText(getString(R.string.select_file, mPath));
            SharedPreferencesUtils.saveUpdateFilePath(mPath);
            btn_start_update.setVisibility(View.VISIBLE);
        }
    }

/*    private float getVersionValue(String version) {
        return Float.valueOf(version.substring(1));
    }*/

    private DbLight getLightByMeshAddress(int meshAddress) {
        if (onlineLights == null || onlineLights.size() == 0) return null;
        for (DbLight light : onlineLights) {
            if (light.getMeshAddr() == meshAddress) {
                return light;
            }
        }
        return null;
    }

    private void chooseFile() {
        startActivityForResult(new Intent(this, FileSelectActivity.class), REQUEST_CODE_CHOOSE_FILE);
    }

    private boolean hasLight(int meshAddress) {
        for (DbLight light : onlineLights) {
            if (light.getMeshAddr() == meshAddress) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否可升级
     * 0:最新， 1：可升级， -n：null
     */
    public int compareVersion(String lightVersion, String newVersion) {

//        return lightVersion.equals(newVersion) ? 0 : 1;
        return 1;
    }

    private void updateStep1() {
        if (mPath != null && !mPath.isEmpty()) {
            parseFile();
        } else {
            ToastUtils.showLong(R.string.tip_select_file);
        }
    }

    /**
     * ****************************************泰凌微升级逻辑********************************************
     */


    /**
     * action startScan
     */
    private synchronized void startScan() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceName(DBUtils.INSTANCE.getLastUser().getAccount())
                .build();
        scanFilters.add(scanFilter);
        btn_start_update.setText(R.string.start_scan);
        TelinkLightService.Instance().idleMode(true);
        LeScanParameters params = Parameters.createScanParameters();
        if(!AppUtils.isExynosSoc()){
            params.setScanFilters(scanFilters);
        }
        params.setMeshName(mesh.getName());
        params.setTimeoutSeconds(TIME_OUT_SCAN);
        params.setScanMac(dbLight.getMacAddr());
        TelinkLightService.Instance().startScan(params);
        startScanTimer();
        log("startScan ");
    }

    Disposable mScanDisposal;
    @SuppressLint("CheckResult")
    private void startScanTimer(){
        if (mScanDisposal != null) {
            mScanDisposal.dispose();
        }
            mScanDisposal=Observable.timer(TIME_OUT_SCAN,TimeUnit.SECONDS).subscribe(aLong -> {
                    onScanTimeout();
            });
    }

    private void stopScanTimer() {
        if(mScanDisposal!=null){
            mScanDisposal.dispose();
        }
    }

    private void startOTA() {
        this.runOnUiThread(() -> {
            text_info.setVisibility(View.GONE);
            btn_start_update.setVisibility(View.VISIBLE);
            btn_start_update.setText(R.string.updating);
        });

        this.mode = MODE_OTA;
//        otaProgress.setVisibility(View.VISIBLE);
        visibleHandler.obtainMessage(View.GONE, otaProgress).sendToTarget();

        if (TelinkLightApplication.getApp().getConnectDevice() != null) {
            TelinkLightService.Instance().startOta(mFirmwareData);
        } else {
            startScan();
        }
        log("startOTA ");
    }

    @Override
    public void performed(Event<String> event) {
//        if (this.mode == MODE_COMPLETE) return;
        switch (event.getType()) {
            case LeScanEvent.LE_SCAN:
                onLeScan((LeScanEvent) event);
                break;
            case LeScanEvent.LE_SCAN_COMPLETED:
                log("scan complete");
//                onScanComplet();
                break;
            case LeScanEvent.LE_SCAN_TIMEOUT:
                // scan complete without results
                log("scan TIMEOUT");
                onScanTimeout();
                break;
            case DeviceEvent.STATUS_CHANGED:
                onDeviceEvent((DeviceEvent) event);
                break;
            case NotificationEvent.GET_DEVICE_STATE:
                onNotificationEvent((NotificationEvent) event);
                break;
        }
    }


    boolean connectStart = false;

    private void onLeScan(LeScanEvent event) {
        DeviceInfo deviceInfo = event.getArgs();
        Log.e("ota progress", "LE_SCAN : " + deviceInfo.macAddress);
        log("on scan : " + deviceInfo.macAddress);

        new Thread(() -> {
            DbLight dbLight = DBUtils.INSTANCE.getLightByMeshAddr(deviceInfo.meshAddress);
            if (dbLight != null && dbLight.getMacAddr().equals("0")) {
                dbLight.setMacAddr(deviceInfo.macAddress);
                DBUtils.INSTANCE.updateLight(dbLight);
            }
        }).start();

        if (this.mode == MODE_OTA) {
            DbLight light = getLightByMeshAddress(deviceInfo.meshAddress);
            if (light != null && light.getMeshAddr() == dbLight.getMeshAddr() && compareVersion(light.version, mFileVersion) == 1) {
                log("onLeScan" + "connectDevice1");
                connectDevice(deviceInfo.macAddress);
            }
        } else if (this.mode == MODE_IDLE) {
            if (dbLight.getMeshAddr() == deviceInfo.meshAddress) {
                log("onLeScan" + "connectDevice2");
                stopScanTimer();
                if (!connectStart) {
                    LeBluetooth.getInstance().stopScan();
                    connectDevice(deviceInfo.macAddress);
                    connectRetryCount=1;
//                    startConnectTimer();
                }
                connectStart = true;
            }
        }
    }

    public void onScanComplet() {
//        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("onScanComplet : " + successCount);

        if (connectRetryCount == 0 && TelinkLightApplication.getInstance().getConnectDevice() == null) {
            showUpdateFailView();
        }
    }

    private void showUpdateFailView() {
        runOnUiThread(() -> {
            text_info.setVisibility(View.VISIBLE);
            text_info.setText(R.string.update_fail);
            btn_start_update.setVisibility(View.GONE);
            btn_start_update.setClickable(false);
            mode = MODE_IDLE;
            TelinkLightApplication.getApp().removeEventListener(this);
            addEventListener();
        });
    }

    public void onScanTimeout() {
        stopScanTimer();
        LeBluetooth.getInstance().stopScan();
        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("Finish: Success Count : " + successCount);
        showUpdateFailView();
    }

    private void onNotificationEvent(NotificationEvent event) {
        // 解析版本信息
        byte[] data = event.getArgs().params;
        if (data[0] == NotificationEvent.DATA_GET_VERSION) {
            String version = Strings.bytesToString(Arrays.copyOfRange(data, 1, 5));

            int meshAddress = event.getArgs().src;
//            meshAddress = src & 0xFF;
            if (meshAddress == dbLight.getMeshAddr() && !versionDevices.contains(meshAddress)) {
                versionDevices.add(meshAddress);
            }

            TelinkLog.w(" src:" + meshAddress + " get version success: " + version);
            log("getVersion:" + Integer.toHexString(meshAddress) + "  version:" + version);
            for (DbLight light : onlineLights) {
                if (light.getMeshAddr() == meshAddress) {
//                        log("version: " + version + " -- light version:" + light.version + " --mode: " + this.mode);
                    if (this.mode == MODE_COMPLETE) {
                        if (!version.equals(light.version)) {
                            successCount++;
                        }
                    }
                    light.version = version;
                }
            }
        } else if (data[0] == NotificationEvent.DATA_GET_OTA_STATE) {
            delayHandler.removeCallbacks(deviceOtaStateTimeoutTask);
            int otaState = data[1];
            log("OTA State response--" + otaState);

            if (mSendataDisposal != null) {
                mSendataDisposal.dispose();
            }
            mSendataDisposal = Observable.timer(TIME_OUT_SENDDATA, TimeUnit.SECONDS).subscribe(aLong -> {
                if (progress_view.getProgress() <= 0) {
                    showUpdateFailView();
                }
            });
            if (otaState == NotificationEvent.OTA_STATE_IDLE) {
                if (this.mode == MODE_OTA) {
                    startOTA();
                }
            } else {
                log("OTA State response: Busy!!! Stopped!--" + otaState);
                doFinish();
            }
        }

    }

    Disposable mConnectDisposal;

    private void startConnectTimer() {
        if (mConnectDisposal != null) {
            mConnectDisposal.dispose();
        }
        mConnectDisposal = Observable.timer((long) TIME_OUT_CONNECT, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (TelinkLightApplication.getApp().getConnectDevice() == null) {
                        connectDevice(dbLight.getMacAddr());
                    } else {
                        login();
                    }
                });
    }

    private void stopConnectTimer() {
        if (mConnectDisposal != null) {
            mConnectDisposal.dispose();
        }
    }

    private int connectRetryCount = 0;

    //    private boolean loginStart=false;
    private void onDeviceEvent(DeviceEvent event) {
        int status = event.getArgs().status;
        switch (status) {
            case LightAdapter.STATUS_LOGOUT:
                TelinkLog.i("OTAUpdate#STATUS_LOGOUT");
                log("logout +connectRetryCount=" + connectRetryCount);

                if (connectRetryCount > 0) {
                    if (connectRetryCount >= 3) {
//                    btn_start_update.setText(R.string.update_fail);
                        showUpdateFailView();
                        stopConnectTimer();
                    } else {
                        startConnectTimer();
                    }
                }

                connectRetryCount++;
                break;

            case LightAdapter.STATUS_LOGIN:
                TelinkLog.i("OTAUpdate#STATUS_LOGIN");
                log("login success");
                stopConnectTimer();
                connectRetryCount = 0;
                if (this.mode == MODE_COMPLETE) return;
                TelinkLightService.Instance().enableNotification();
//                if (this.mode == MODE_OTA) {
//                    sendGetDeviceOtaStateCommand();
//                } else if (this.mode == MODE_IDLE) {
//                    if (this.mFirmwareData != null) {
//                        sendGetVersionCommand();
//                    }
//                }
                startOTA();
                break;

            case LightAdapter.STATUS_CONNECTED:
                log("connected");
                if (this.mode != MODE_COMPLETE)
//                    if(!loginStart){
                    login();
//                        loginStart=true;
//                    }

                break;

            case LightAdapter.STATUS_OTA_PROGRESS:
                OtaDeviceInfo deviceInfo = (OtaDeviceInfo) event.getArgs();
//                log("ota progress :" + deviceInfo.progress + "%");
                msgHandler.obtainMessage(MSG_OTA_PROGRESS, deviceInfo.progress).sendToTarget();
                break;

            case LightAdapter.STATUS_OTA_COMPLETED:
                log("OTA complete");
                msgHandler.obtainMessage(MSG_OTA_PROGRESS, 100).sendToTarget();
                DeviceInfo deviceInfo_1 = event.getArgs();
                for (DbLight light : onlineLights) {
                    if (light.getMeshAddr() == deviceInfo_1.meshAddress) {
                        light.version = mFileVersion;
                    }
                }

                successCount++;
                if (onlineLights.size() <= successCount) {
                    updateSuccess();
                } else {
//                    this.mode = MODE_MESH_OTA;
                }
                break;

            case LightAdapter.STATUS_OTA_FAILURE:
                log("OTA fail");
                showUpdateFailView();
                progress_view.setProgress(0);
                if (this.mode == MODE_COMPLETE) {
                    log("OTA FAIL COMPLETE");
                    return;
                }
//                startScan();
                break;
        }
    }

    // start
    private void sendStartMeshOTACommand() {
        // save mesh info
        String account = DBUtils.INSTANCE.getLastUser().getAccount();
        String pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account);

        mesh.setOtaDevice(new OtaDevice());
        DeviceInfo curDevice = TelinkLightApplication.getApp().getConnectDevice();
        mesh.getOtaDevice().mac = curDevice.macAddress;
        mesh.getOtaDevice().meshName = account;
        mesh.getOtaDevice().meshPwd = pwd;
        mesh.saveOrUpdate(this);

        visibleHandler.obtainMessage(View.VISIBLE, meshOtaProgress).sendToTarget();
//        meshOtaProgress.setVisibility(View.VISIBLE);
        byte opcode = (byte) 0xC6;
        int address = 0x0000;
        byte[] params = new byte[]{(byte) 0xFF, (byte) 0xFF};
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params);
        log("SendCommand 0xC6 startMeshOTA");
    }

    // stop
    private void sendStopMeshOTACommand() {
        byte opcode = (byte) 0xC6;
        int address = 0xFFFF;
        byte[] params = new byte[]{(byte) 0xFE, (byte) 0xFF};
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                params);
    }

}
