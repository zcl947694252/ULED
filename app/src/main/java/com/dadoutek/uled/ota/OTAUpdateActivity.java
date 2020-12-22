package com.dadoutek.uled.ota;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DeviceType;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.dbModel.DBUtils;
import com.dadoutek.uled.model.dbModel.DbConnector;
import com.dadoutek.uled.model.dbModel.DbCurtain;
import com.dadoutek.uled.model.dbModel.DbLight;
import com.dadoutek.uled.model.dbModel.DbSwitch;
import com.dadoutek.uled.model.dbModel.DbUser;
import com.dadoutek.uled.network.NetworkFactory;
import com.dadoutek.uled.othersview.FileSelectActivity;
import com.dadoutek.uled.othersview.MainActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.util.AppUtils;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.StringUtils;
import com.dadoutek.uled.util.TmtUtils;
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 我的升级页面
 * 思路：
 * 1.由于demo中没有对设备持久化操作，只有通过online_status 来添加和更新设备，
 * 而online_status返回的数据中只有meshAddress能判断设备唯一性
 * 在OTA升级过程中会保存此时现在的所有设备信息（onlineLights），
 * 如果是从MainActivity页面跳转而来（1.自动连接的设备有上报MeshOTA进度信息；2主动连接之前本地保存的设备），需要读取一次版本信息\n
 * 并初始化onlineLights
 * 1. {@link MainActivity#//onNotificationEvent(NotificationEvent)}；
 * 2. {@link MainActivity#//onDeviceStatusChanged(DeviceEvent)}；
 * <p>s
 * 在开始OTA或者MeshOTA之前都会获取当前设备的OTA状态信息 {@link OTAUpdateActivity--sendGetDeviceOtaStateCommand},
 * \n\t 并通过 {OTAUpdateActivity的onNotificationEvent(NotificationEvent)}返回状态， 处理不同模式下的不同状态
 * 在continue MeshOTA和MeshOTA模式下 {@link OTAUpdateActivity#MODE_CONTINUE_MESH_OTA},
 * {@link OTAUpdateActivity#MODE_MESH_OTA}
 * <p>
 * 校验通过后，会开始动作
 * <p>
 * Action Start by choose correct bin file!
 * <p>
 * Created by Administrator on 2017/4/20.
 */
public class OTAUpdateActivity extends TelinkMeshErrorDealActivity implements EventListener<String>, View.OnClickListener {
    CircleProgressBar progress_view;
    TextView text_info;
    LinearLayout select;
    Button btn_start_update;
    TextView tvFile;
    TextView local_version;
    TextView server_version;
    CheckBox open_device;

    private int mode = MODE_IDLE;
    private static final int MODE_IDLE = 1;
    private static final int MODE_OTA = 2;
    private static final int MODE_MESH_OTA = 4;
    private static final int MODE_CONTINUE_MESH_OTA = 8;
    private static final int MODE_COMPLETE = 16;
    private boolean mesh;
    public static final String INTENT_KEY_CONTINUE_MESH_OTA = "com.telink.bluetooth.light" +
            ".INTENT_KEY_CONTINUE_MESH_OTA";
    // 有进度状态上报 时跳转进入的
    public static final int CONTINUE_BY_REPORT = 0x21;
    private static final int REQUEST_CODE_CHOOSE_FILE = 11;
    private PowerManager.WakeLock mWakeLock = null;
    private byte[] mFirmwareData;
    private String mPath;
    private SimpleDateFormat mTimeFormat;
    private int successCount = 0;
    DbUser user;
    private TextView otaProgress;
    private Toolbar toolbar;
    private TextView toolbarTv;
    private TextView meshOtaProgress;
    private TextView tv_log, tv_version;
    private ScrollView sv_log;
    private String mFileVersion;
    private static final int MSG_OTA_PROGRESS = 11;
    private static final int MSG_MESH_OTA_PROGRESS = 12;
    private static final int MSG_LOG = 13;
    private static final int TIME_OUT_SCAN = 20;
    private static final int TIME_OUT_CONNECT = 15;
    private boolean OTA_IS_HAVEN_START = false;
    int lightMeshAddr;
    String lightMacAddr;
    Handler handler;
    String lightVersion;
    private Handler delayHandler = new Handler();
    @SuppressLint("HandlerLeak")
    private Handler visibleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ((View) msg.obj).setVisibility(msg.what);
        }
    };
    @SuppressLint("HandlerLeak")
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_OTA_PROGRESS:
                    if ((Integer) msg.obj < 100) {
                        btn_start_update.setText(R.string.otaing);
                    }
                    otaProgress.setText(getString(R.string.progress_ota, msg.obj.toString()));
                    progress_view.setProgress((Integer) msg.obj);
                    break;

                case MSG_MESH_OTA_PROGRESS:
                    meshOtaProgress.setText(getString(R.string.progress_mesh_ota,
                            msg.obj.toString()));
                    progress_view.setProgress((Integer) msg.obj);
                    break;

                case MSG_LOG:
                    if (SharedPreferencesUtils.isDeveloperModel()) {
                        sv_log.setVisibility(View.VISIBLE);
                        String time = mTimeFormat.format(Calendar.getInstance().getTimeInMillis());
                        tv_log.append("\n" + time + ":" + msg.obj.toString());
                        sv_log.fullScroll(View.FOCUS_DOWN);
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

                switch (state) {
                    case BluetoothAdapter.STATE_ON: {
                        log("蓝牙打开");
                    }
                    case BluetoothAdapter.STATE_OFF: {
                        log("蓝牙关闭");
                        showUpdateFailView();
                    }
                }
            }
        }
    };
    private int lightType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_update);
        progress_view = findViewById(R.id.progress_view);
        text_info = findViewById(R.id.text_info);
        select = findViewById(R.id.select);
        btn_start_update = findViewById(R.id.btn_start_update);
        tvFile = findViewById(R.id.tvFile);
        local_version = findViewById(R.id.local_version);
        server_version = findViewById(R.id.server_version);
        open_device = findViewById(R.id.open_device);

        btn_start_update.setOnClickListener(this);
        select.setOnClickListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(mReceiver, filter);
        user = DBUtils.INSTANCE.getLastUser();
        if (user == null || TextUtils.isEmpty(user.getControlMeshName()) || TextUtils.isEmpty(user.getControlMeshPwd())) {
            toast("Mesh Error!");
            finish();
            return;
        }
        open_device.setVisibility(View.GONE);

        initView();
        initData();
    }

    private void initData() {
        lightMeshAddr = getIntent().getIntExtra(Constant.OTA_MES_Add, 0);
        lightMacAddr = getIntent().getStringExtra(Constant.OTA_MAC);
        lightVersion = getIntent().getStringExtra(Constant.OTA_VERSION);
        lightType = getIntent().getIntExtra(Constant.OTA_TYPE, 100);

        boolean b = "".equals(lightVersion) || lightVersion == null;
        btn_start_update.setClickable(!b);
        if (b) {
            btn_start_update.setText(getString(R.string.get_version_fail));
            TmtUtils.midToastLong(this, getString(R.string.get_server_version_fail));
        } else {
            local_version.setVisibility(View.VISIBLE);
            local_version.setText(getString(R.string.local_version, lightVersion));
        }
        log("current-light-mesh" + lightMeshAddr);
        LogUtils.v("zcl---current-light-mesh" + lightMeshAddr + "-----" + lightMacAddr + "====" + lightVersion);
        if (!SharedPreferencesUtils.isDeveloperModel()) {
            mPath = SharedPreferencesUtils.getUpdateFilePath();
            parseFile();
        }

    }

    @SuppressLint("WakelockTimeout")
    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }


    @SuppressLint({"InvalidWakeLockTag", "SimpleDateFormat"})
    private void initView() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        assert powerManager != null;
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        addEventListener();
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.enableNotification();
        mTimeFormat = new SimpleDateFormat("HH:mm:ss.S");

        toolbar = findViewById(R.id.toolbar);
        toolbarTv = findViewById(R.id.toolbarTv);
        initToolbar();
        otaProgress = findViewById(R.id.progress_ota);
        meshOtaProgress = findViewById(R.id.progress_mesh_ota);
        tv_log = findViewById(R.id.tv_log);
        sv_log = findViewById(R.id.sv_log);
        tv_version = findViewById(R.id.tv_version);

        if (SharedPreferencesUtils.isDeveloperModel()) {
            select.setVisibility(View.VISIBLE);
        } else {
            select.setVisibility(View.GONE);
        }

        open_device.setOnClickListener(v -> {
            byte[] bytes;
            if (open_device.isChecked())
                bytes = new byte[]{0x01, 0x64, 0x00};
            else
                bytes = new byte[]{0x00, 0x64, 0x00};
            if (instance != null)
                instance.sendCommandNoResponse(Opcode.LIGHT_ON_OFF, lightMeshAddr, bytes);
        });
    }


    private void initToolbar() {
        toolbarTv.setText(R.string.ota_update_title);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setNavigationIcon(R.drawable.icon_return);
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


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onLocationEnable() {
    }

    private void addEventListener() {
        TelinkLightApplication.Companion.getApp().addEventListener(LeScanEvent.LE_SCAN, this);
        TelinkLightApplication.Companion.getApp().addEventListener(LeScanEvent.LE_SCAN_TIMEOUT,
                this);
        TelinkLightApplication.Companion.getApp().addEventListener(DeviceEvent.STATUS_CHANGED,
                this);
        TelinkLightApplication.Companion.getApp().addEventListener(NotificationEvent.GET_DEVICE_STATE, this);
    }


    public void connectDevice(String mac) {
        log("connectDevice :" + mac);
        btn_start_update.setText(R.string.connecting_tip);
        btn_start_update.setClickable(false);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.connect(mac, TIME_OUT_CONNECT);
    }

    private void login() {
        log("login");
        String meshName = user.getControlMeshName();
        String pwd = NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.login(Strings.stringToBytes(meshName, 16), Strings.stringToBytes(pwd, 16));
    }

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
        unregisterReceiver(mReceiver);
        TelinkLightApplication.Companion.getApp().removeEventListener(this);
        if (this.delayHandler != null)
            this.delayHandler.removeCallbacksAndMessages(null);

        if (this.visibleHandler != null)
            this.delayHandler.removeCallbacksAndMessages(null);

        if (this.msgHandler != null)
            this.msgHandler.removeCallbacksAndMessages(null);
        if (this.handler != null)
            this.handler.removeCallbacksAndMessages(null);

        switch (lightType) {
            case DeviceType.SENSOR:
            case DeviceType.GATE_WAY:
            case DeviceType.NORMAL_SWITCH:
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance != null)
                    instance.idleMode(true);
        }

        TelinkLightApplication.Companion.getApp().removeEventListener(this);
    }

    private void updateSuccess() {
        doFinish();
        text_info.setVisibility(View.VISIBLE);
        text_info.setText(getString(R.string.updateSuccess));
        open_device.setText(lightVersion);
        btn_start_update.setVisibility(View.GONE);
        btn_start_update.setClickable(true);
        switch (lightType) {
            case DeviceType.LIGHT_NORMAL:
            case DeviceType.LIGHT_NORMAL_OLD:
            case DeviceType.LIGHT_RGB:
                DbLight light = DBUtils.INSTANCE.getLightByMeshAddr(lightMeshAddr);
                light.version = StringUtils.versionResolutionURL(mPath, 2);
                DBUtils.INSTANCE.saveLight(light, true);
                break;
            case DeviceType.SMART_CURTAIN:
                DbCurtain curtain = DBUtils.INSTANCE.getCurtainByMeshAddr(lightMeshAddr);
                curtain.version = StringUtils.versionResolutionURL(mPath, 2);
                DBUtils.INSTANCE.saveCurtain(curtain, true);
                break;
            case DeviceType.SMART_RELAY:
                DbConnector rely = DBUtils.INSTANCE.getRelyByMeshAddr(lightMeshAddr);
                rely.version = StringUtils.versionResolutionURL(mPath, 2);
                DBUtils.INSTANCE.saveConnector(rely, true);
                break;
            case DeviceType.NORMAL_SWITCH:
                DbSwitch dbSwitch = DBUtils.INSTANCE.getSwitchByMeshAddr(lightMeshAddr);
                dbSwitch.version = StringUtils.versionResolutionURL(mPath, 2);
                DBUtils.INSTANCE.saveSwitch(dbSwitch, true, lightType, dbSwitch.getKeys());
                break;
            case DeviceType.EIGHT_SWITCH:
                DbSwitch dbSwitch8 = DBUtils.INSTANCE.getSwitchByMeshAddr(lightMeshAddr);
                dbSwitch8.setVersion(StringUtils.versionResolutionURL(mPath, 2));
                DBUtils.INSTANCE.saveSwitch(dbSwitch8, true, dbSwitch8.type, dbSwitch8.getKeys());
                break;
        }
        ToastUtils.showLong(R.string.exit_update);
        handler = new Handler();
        handler.postDelayed(() -> finish(), 2000);
    }

    private void doFinish() {
        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("Finish: Success Count : " + successCount);
        if (successCount == 0) {
            startScan();
        }
    }

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
        //        TelinkLightService instance = TelinkLightService.Instance();
        //        if (instance != null)
        //            instance.idleMode(true);
        TelinkLog.i("OTAUpdate#onStop#removeEventListener");
        TelinkLightApplication.Companion.getApp().removeEventListener(this);
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
                mCancelBuilder.setPositiveButton(getString(android.R.string.ok),
                        (dialog, which) -> {
                    sendStopMeshOTACommand();
                    Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
                    mesh.setOtaDevice(null);
                    mesh.saveOrUpdate(OTAUpdateActivity.this);
                    dialog.dismiss();
                    finish();
                });
                mCancelBuilder.setNegativeButton(getString(android.R.string.cancel), (dialog,
                                                                                      which) ->
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

            server_version.setVisibility(View.VISIBLE);
            server_version.setText(getString(R.string.server_version,
                    StringUtils.versionResolutionURL(mPath, 2)));

            select.setEnabled(false);
            text_info.setVisibility(View.GONE);
            btn_start_update.setVisibility(View.VISIBLE);
            btn_start_update.setClickable(true);
            //            startOTA();
        }
    }

    private void beginToOta() {
        if (TelinkLightApplication.getInstance().getConnectDevice() != null) {
            startOTA();
        } else {
            connectOld(lightMacAddr);
        }
    }


    private int getLightByMeshAddress(int meshAddress) {
        if (lightMeshAddr == meshAddress) {
            return lightMeshAddr;
        }
        return 0;
    }

    private void chooseFile() {
        startActivityForResult(new Intent(this, FileSelectActivity.class),
                REQUEST_CODE_CHOOSE_FILE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // startActivityForResult(intent, REQUEST_CODE_CHOOSE_FILE);
    }

    /**
     * ****************************************泰凌微升级逻辑********************************************
     */

    /**
     * action startScan
     */
    private synchronized void startScan() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
        scanFilterBuilder.setDeviceName(user.getControlMeshName());
        if (String.valueOf(lightMacAddr).length() > 16) {
            scanFilterBuilder.setDeviceAddress(String.valueOf(lightMacAddr));
        }
        ScanFilter scanFilter = scanFilterBuilder.build();
        scanFilters.add(scanFilter);
        btn_start_update.setText(R.string.start_scan);
        btn_start_update.setClickable(false);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.idleMode(true);
        LeScanParameters params = Parameters.createScanParameters();
        if (!AppUtils.Companion.isExynosSoc()) {

            params.setScanFilters(scanFilters);
        }
        params.setMeshName(user.getName());
        params.setTimeoutSeconds(TIME_OUT_SCAN);
        if (String.valueOf(lightMacAddr).length() > 16)
            params.setScanMac(String.valueOf(lightMacAddr));
        if (instance != null)
            instance.startScan(params);
        startScanTimer();
        log("startScan ");
    }

    Disposable mScanDisposal;

    @SuppressLint("CheckResult")
    private void startScanTimer() {
        if (mScanDisposal != null) {
            mScanDisposal.dispose();
        }
        mScanDisposal = Observable.timer(TIME_OUT_SCAN, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(aLong -> {
                    onScanTimeout();
                });
    }

    private void stopScanTimer() {
        if (mScanDisposal != null) {
            mScanDisposal.dispose();
        }
    }

    private void startOTA() {
        this.runOnUiThread(() -> {
            text_info.setVisibility(View.GONE);
            btn_start_update.setVisibility(View.VISIBLE);
            btn_start_update.setText(R.string.otaing);
        });

        this.mode = MODE_OTA;
        visibleHandler.obtainMessage(View.GONE, otaProgress).sendToTarget();

        if (TelinkLightApplication.Companion.getApp().getConnectDevice() != null) {
            OTA_IS_HAVEN_START = true;
            //            LogUtils.e("zcl-----------升级版本" + mFirmwareData.toString()
            //            +"--------------"+TelinkLightService.Instance().isLogin());
            TelinkLightService instance = TelinkLightService.Instance();
            if (instance != null)
                instance.startOta(mFirmwareData);
        } else {
            //startScan();
            connectOld(lightMacAddr);
        }
        LogUtils.v("startOTA ");
    }

    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case LeScanEvent.LE_SCAN:
                onLeScan((LeScanEvent) event);
                break;
            case LeScanEvent.LE_SCAN_COMPLETED:
                log("scan complete");
                break;
            case LeScanEvent.LE_SCAN_TIMEOUT:
                log("scan TIMEOUT");
                onScanTimeout();
                break;
            case DeviceEvent.STATUS_CHANGED:
                onDeviceEvent((DeviceEvent) event);
                break;
            case NotificationEvent.GET_DEVICE_STATE:
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
            int deviceMeshAddr = getLightByMeshAddress(deviceInfo.meshAddress);

            if (deviceMeshAddr == lightMeshAddr) {
                log("onLeScan" + "connectDevice1");
                connectDevice(deviceInfo.macAddress);
            }
        } else if (this.mode == MODE_IDLE) {
            if (lightMeshAddr == deviceInfo.meshAddress) {
                log("onLeScan" + "connectDevice2");
                stopScanTimer();
                if (!connectStart) {
                    LeBluetooth.getInstance().stopScan();
                    connectDevice(deviceInfo.macAddress);
                    connectRetryCount = 1;
                    //                    startConnectTimer();
                }
                connectStart = true;
            }
        }
    }

    private void showUpdateFailView() {
        runOnUiThread(() -> {
            text_info.setVisibility(View.VISIBLE);
            text_info.setText(R.string.update_fail);
            select.setEnabled(true);
            mode = MODE_IDLE;
            //            TelinkLightApplication.Companion.getApp().removeEventListener(this);
            stopScanTimer();
            LeBluetooth.getInstance().stopScan();
            stopConnectTimer();
            //            addEventListener();
            if (OTA_IS_HAVEN_START) {
                btn_start_update.setVisibility(View.GONE);
                btn_start_update.setClickable(false);
                TelinkLightApplication.Companion.getApp().removeEventListener(this);
            } else {
                btn_start_update.setText(getString(R.string.retry_ota));
                btn_start_update.setVisibility(View.VISIBLE);
                btn_start_update.setClickable(true);
            }
        });
    }

    public void onScanTimeout() {
        stopScanTimer();
        LeBluetooth.getInstance().stopScan();
        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("Finish: Success Count : " + successCount);
        //        showUpdateFailView();
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
                    if (TelinkLightApplication.Companion.getApp().getConnectDevice() == null) {
                        connectDevice(lightMacAddr + "");
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

    //private boolean loginStart=false;
    private void onDeviceEvent(DeviceEvent event) {
        int status = event.getArgs().status;
        switch (status) {
            case LightAdapter.STATUS_LOGOUT:
                TelinkLog.i("OTAUpdate#STATUS_LOGOUT");
                log("logout +connectRetryCount=" + connectRetryCount);

                if (connectRetryCount > 0) {
                    if (connectRetryCount >= 3) {
                        showUpdateFailView();
                        stopConnectTimer();
                    } else {
                        startConnectTimer();
                    }
                }
                connectRetryCount++;
                break;

            case LightAdapter.STATUS_LOGIN:
                log("login success");
                hideLoadingDialog();
                LeBluetooth.getInstance().stopScan();
                stopConnectTimer();
                connectRetryCount = 0;
                if (this.mode == MODE_COMPLETE)
                    return;
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance != null)
                    instance.enableNotification();
                //                parseFile();
                break;

            case LightAdapter.STATUS_CONNECTED:
                log("connected");
                if (this.mode != MODE_COMPLETE)
                    login();
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
                if (lightMeshAddr == deviceInfo_1.meshAddress) {
                    lightVersion = mFileVersion;
                }

                successCount++;
                if (successCount >= 1)
                    updateSuccess();

                break;

            case LightAdapter.STATUS_OTA_FAILURE:
                log("OTA fail");
                showUpdateFailView();
                progress_view.setProgress(0);
                if (this.mode == MODE_COMPLETE) {
                    log("OTA FAIL COMPLETE");
                    return;
                }
                break;
        }
    }

    // stop
    private void sendStopMeshOTACommand() {
        byte opcode = (byte) 0xC6;
        int address = 0xFFFF;
        byte[] params = new byte[]{(byte) 0xFE, (byte) 0xFF};
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.sendCommandNoResponse(opcode, address, params);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //第一个是传递标记监听 第二个是回传标记监听
        if (requestCode == REQUEST_CODE_CHOOSE_FILE && resultCode == RESULT_OK) {
            Bundle b = data.getExtras(); //data为B中回传的Intent
            mPath = b.getString("path");//str即为回传的值
            tvFile.setText(mPath);
            Log.e("zcl", "返回数据是:" + requestCode + "---" + resultCode + "---mPath-" + mPath);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select:
                chooseFile();
                break;
            case R.id.btn_start_update:
                LogUtils.e("zcl 升级路径--" + mPath);
                if (SharedPreferencesUtils.isDeveloperModel() && mPath == null) {
                    TmtUtils.midToastLong(this, getString(R.string.please_select_update_file));
                    return;
                } else {
                    beginToOta();
                }
                break;
        }
    }
}
