package com.dadoutek.uled.ota;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbConnector;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.OtaDevice;
import com.dadoutek.uled.network.NetworkFactory;
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

import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class OTAConnectorActivity extends TelinkMeshErrorDealActivity implements EventListener<String> {
    private int mode = MODE_IDLE;
    private static final int MODE_IDLE = 1;
    private static final int MODE_OTA = 2;
    private static final int MODE_MESH_OTA = 4;
    private static final int MODE_CONTINUE_MESH_OTA = 8;
    private static final int MODE_COMPLETE = 16;

    public static final String INTENT_KEY_CONTINUE_MESH_OTA = "com.telink.bluetooth.light" +
            ".INTENT_KEY_CONTINUE_MESH_OTA";
    // 有进度状态上报 时跳转进入的
    public static final int CONTINUE_BY_REPORT = 0x21;

    // 继续之前的OTA操作，连接指定设备
    public static final int CONTINUE_BY_PREVIOUS = 0x22;

    private int continueType = 0;

    private static final int REQUEST_CODE_CHOOSE_FILE = 11;

    private PowerManager.WakeLock mWakeLock = null;

    //    private static final int MODE_SCAN = 8;
    private byte[] mFirmwareData;
    private List<DbConnector> onlineLights;
    private DbConnector dbLight;
    private Mesh mesh;
    private String mPath;
    private SimpleDateFormat mTimeFormat;
    private int successCount = 0;

    private TextView otaProgress;
    private Toolbar toolbar;
    private TextView toolbarTV;
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
    private boolean OTA_IS_HAVEN_START = false;

    CircleProgressBar progress_view;
    TextView text_info;
    LinearLayout select;
    Button btn_start_update;
    TextView tvFile;
    TextView local_version;
    TextView server_version;
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
                        btn_start_update.setText(R.string.updating);
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
                        TelinkLightService instance = TelinkLightService.Instance();
                        if (instance != null)
                            instance.idleMode(true);
                        LeBluetooth.getInstance().stopScan();
                    }
                    case BluetoothAdapter.STATE_OFF: {
                        log("蓝牙关闭");
                        ToastUtils.showLong(R.string.tip_phone_ble_off);
                        TelinkLightService instance = TelinkLightService.Instance();
                        if (instance != null)
                            instance.idleMode(true);
                        showUpdateFailView();
                    }
                }
            }
        }
    };
    private boolean developerModel;

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

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(mReceiver, filter);
        mesh = TelinkLightApplication.Companion.getApp().getMesh();
        if (mesh == null || TextUtils.isEmpty(mesh.getName()) || TextUtils.isEmpty(mesh.getPassword())) {
            toast("Mesh Error!");
            finish();
            return;
        }

        initData();
        initView();
    }

    private void initData() {
        dbLight = (DbConnector) getIntent().getSerializableExtra(Constant.UPDATE_LIGHT);
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
         toolbarTV = findViewById(R.id.toolbarTv);
        initToolbar();
        otaProgress = findViewById(R.id.progress_ota);
        meshOtaProgress = findViewById(R.id.progress_mesh_ota);
        tv_log = findViewById(R.id.tv_log);
        sv_log = findViewById(R.id.sv_log);
        tv_version = findViewById(R.id.tv_version);

        developerModel = SharedPreferencesUtils.isDeveloperModel();
        select.setVisibility(developerModel ? View.VISIBLE : View.GONE);

        if (!SharedPreferencesUtils.getUpdateFilePath().isEmpty()) {
            mPath = SharedPreferencesUtils.getUpdateFilePath();
            tvFile.setText(getString(R.string.select_file, mPath));
            btn_start_update.setVisibility(View.VISIBLE);
            local_version.setVisibility(View.VISIBLE);
            local_version.setText(getString(R.string.local_version, dbLight.version));
            server_version.setVisibility(View.VISIBLE);
            server_version.setText(getString(R.string.server_version,
                    StringUtils.versionResolutionURL(mPath, 2)));
        }
    }

    private void initToolbar() {
        toolbarTV.setText(R.string.ota_update_title);
        toolbar.setNavigationIcon(R.drawable.icon_return);
      toolbar.setNavigationOnClickListener(v -> finish());
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
        //        startScan();
    }

    private void addEventListener() {
        TelinkLightApplication.Companion.getApp().addEventListener(LeScanEvent.LE_SCAN, this);
        //        TelinkLightApplication.Companion.getApp().addEventListener(LeScanEvent
        //        .LE_SCAN_COMPLETED, this);
        TelinkLightApplication.Companion.getApp().addEventListener(LeScanEvent.LE_SCAN_TIMEOUT,
                this);
        TelinkLightApplication.Companion.getApp().addEventListener(DeviceEvent.STATUS_CHANGED,
                this);
        TelinkLightApplication.Companion.getApp().addEventListener(NotificationEvent.GET_DEVICE_STATE, this);
    }


    /**
     * Action start: after get versions
     * hasHigh Confirm action OTA or MeshOTA
     * hasLow Confirm OTA needed
     */
    private void start() {

        boolean hasHigh = false;
        boolean hasLow = false;
        for (DbConnector light : onlineLights) {
            if (light.version == null || light.version.equals(""))
                continue;
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

            int curMeshAddress =
                    TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress;
            DbConnector light = getLightByMeshAddress(curMeshAddress);
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
        DeviceInfo deviceInfo = TelinkLightApplication.Companion.getApp().getConnectDevice();
        boolean action = false;
        if (deviceInfo != null) {
            for (DbConnector light : onlineLights) {
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
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance != null)
                    instance.sendCommandNoResponse(opcode, address,
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
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.sendCommandNoResponse(opcode, address,
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
        for (DbConnector light : onlineLights) {
            if (light.version == null || light.version.equals(""))
                continue;
            if (compareVersion(light.version, mFileVersion) == 1) {
                hasLow = true;
            }
        }

        return hasLow;
    }

    private String mFileVersion;


    public void connectDevice(String mac) {
        log("connectDevice :" + mac);
        btn_start_update.setText(R.string.connecting_tip);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.connect(mac, TIME_OUT_CONNECT);
    }

    private void login() {
        log("login");
        String account = DBUtils.INSTANCE.getLastUser().getAccount();
        String pwd = NetworkFactory.md5(NetworkFactory.md5(account) + account).substring(0, 16);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.login(Strings.stringToBytes(account, 16), Strings.stringToBytes(pwd, 16));
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
        unregisterReceiver(mReceiver);
        TelinkLightApplication.Companion.getApp().removeEventListener(this);
        if (this.delayHandler != null) {
            this.delayHandler.removeCallbacksAndMessages(null);
        }
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
        Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
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
                            mesh.saveOrUpdate(OTAConnectorActivity.this);
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
            select.setEnabled(false);
            text_info.setVisibility(View.GONE);
            btn_start_update.setVisibility(View.VISIBLE);
            btn_start_update.setClickable(false);
            //            btn_start_update.setText(R.string.updating);
            //            if (TelinkLightApplication.Companion.getApp().getConnectDevice() !=
            //            null) {
            //                sendGetVersionCommand();
            //            } else {
            if (TelinkLightApplication.Companion.getApp().getConnectDevice() != null &&
                    TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress == dbLight.getMeshAddr()) {
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
            Uri uri = data.getData();
            mPath = getPath(this, uri);
            tvFile.setText(getString(R.string.select_file, mPath));
            SharedPreferencesUtils.saveUpdateFilePath(mPath);
            btn_start_update.setVisibility(View.VISIBLE);
        }
    }

    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

/*    private float getVersionValue(String version) {
        return Float.valueOf(version.substring(1));
    }*/

    private DbConnector getLightByMeshAddress(int meshAddress) {
        if (onlineLights == null || onlineLights.size() == 0)
            return null;
        for (DbConnector light : onlineLights) {
            if (light.getMeshAddr() == meshAddress) {
                return light;
            }
        }
        return null;
    }

    private void chooseFile() {
        //        startActivityForResult(new Intent(this, FileSelectActivity.class),
        //        REQUEST_CODE_CHOOSE_FILE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_FILE);

    }

    private boolean hasLight(int meshAddress) {
        for (DbConnector light : onlineLights) {
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
        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
        scanFilterBuilder.setDeviceName(DBUtils.INSTANCE.getLastUser().getAccount());
        if (dbLight.getMacAddr().length() > 16) {
            scanFilterBuilder.setDeviceAddress(dbLight.getMacAddr());
        }
        ScanFilter scanFilter = scanFilterBuilder.build();
        scanFilters.add(scanFilter);
        btn_start_update.setText(R.string.start_scan);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.idleMode(true);
        LeScanParameters params = Parameters.createScanParameters();
        if (!AppUtils.Companion.isExynosSoc()) {
            params.setScanFilters(scanFilters);
        }
        params.setMeshName(mesh.getName());
        params.setTimeoutSeconds(TIME_OUT_SCAN);
        if (dbLight.getMacAddr().length() > 16) {
            params.setScanMac(dbLight.getMacAddr());
        }
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
        mScanDisposal = Observable.timer(TIME_OUT_SCAN, TimeUnit.SECONDS).subscribe(aLong -> {
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
            btn_start_update.setText(R.string.updating);
        });

        this.mode = MODE_OTA;
        //        otaProgress.setVisibility(View.VISIBLE);
        visibleHandler.obtainMessage(View.GONE, otaProgress).sendToTarget();

        if (TelinkLightApplication.Companion.getApp().getConnectDevice() != null) {
            OTA_IS_HAVEN_START = true;
            TelinkLightService instance = TelinkLightService.Instance();
            if (instance != null)
                instance.startOta(mFirmwareData);
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
            DbConnector light = getLightByMeshAddress(deviceInfo.meshAddress);
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
                    connectRetryCount = 1;
                    //                    startConnectTimer();
                }
                connectStart = true;
            }
        }
    }

    public void onScanComplet() {
        //        this.mode = MODE_COMPLETE;
        Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
        mesh.setOtaDevice(null);
        mesh.saveOrUpdate(this);
        log("onScanComplet : " + successCount);

        if (connectRetryCount == 0 && TelinkLightApplication.Companion.getApp().getConnectDevice() == null) {
            showUpdateFailView();
        }
    }

    private void showUpdateFailView() {
        runOnUiThread(() -> {
            text_info.setVisibility(View.VISIBLE);
            text_info.setText(R.string.update_fail);
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
                btn_start_update.setText(getString(R.string.re_upgrade));
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
            for (DbConnector light : onlineLights) {
                if (light.getMeshAddr() == meshAddress) {
                    //                        log("version: " + version + " -- light version:" +
                    //                        light.version + " --mode: " + this.mode);
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
            mSendataDisposal =
                    Observable.timer(TIME_OUT_SENDDATA, TimeUnit.SECONDS).subscribe(aLong -> {
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
                    if (TelinkLightApplication.Companion.getApp().getConnectDevice() == null) {
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
                LeBluetooth.getInstance().stopScan();
                stopConnectTimer();
                connectRetryCount = 0;
                if (this.mode == MODE_COMPLETE)
                    return;
                TelinkLightService instance = TelinkLightService.Instance();
                if (instance != null)
                    instance.enableNotification();
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
                for (DbConnector light : onlineLights) {
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
        DeviceInfo curDevice = TelinkLightApplication.Companion.getApp().getConnectDevice();
        mesh.getOtaDevice().mac = curDevice.macAddress;
        mesh.getOtaDevice().meshName = account;
        mesh.getOtaDevice().meshPwd = pwd;
        mesh.saveOrUpdate(this);

        visibleHandler.obtainMessage(View.VISIBLE, meshOtaProgress).sendToTarget();
        //        meshOtaProgress.setVisibility(View.VISIBLE);
        byte opcode = (byte) 0xC6;
        int address = 0x0000;
        byte[] params = new byte[]{(byte) 0xFF, (byte) 0xFF};
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.sendCommandNoResponse(opcode, address, params);
        log("SendCommand 0xC6 startMeshOTA");
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
}
