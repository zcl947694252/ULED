package com.dadoutek.uled.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.fragments.DeviceListFragment;
import com.dadoutek.uled.fragments.GroupListFragment;
import com.dadoutek.uled.fragments.MeFragment;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.FragmentFactory;
import com.jakewharton.rxbinding2.view.RxView;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.ErrorReportEvent;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.event.ServiceEvent;
import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.ErrorReportInfo;
import com.telink.bluetooth.light.GetAlarmNotificationParser;
import com.telink.bluetooth.light.LeAutoConnectParameters;
import com.telink.bluetooth.light.LeRefreshNotifyParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.NotificationInfo;
import com.telink.bluetooth.light.OnlineStatusNotificationParser;
import com.telink.bluetooth.light.Parameters;
import com.telink.util.BuildUtils;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public final class MainActivity extends TelinkMeshErrorDealActivity implements EventListener<String> {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int UPDATE_LIST = 0;
    @BindView(R.id.content)
    FrameLayout content;
    @BindView(R.id.tab_devices)
    ImageView tabDevices;
    @BindView(R.id.tab_groups)
    ImageView tabGroups;
    @BindView(R.id.tab_account)
    ImageView tabAccount;
    @BindView(R.id.tabs)
    ConstraintLayout tabs;
    @BindView(R.id.tvLight)
    TextView tvLight;
    @BindView(R.id.tvGroups)
    TextView tvGroups;
    @BindView(R.id.tvAccount)
    TextView tvAccount;

    private Dialog loadDialog;
    private FragmentManager fragmentManager;
    private DeviceListFragment deviceFragment;
    private GroupListFragment groupFragment;
    private MeFragment meFragment;

    private Fragment mContent;


    private TelinkLightApplication mApplication;

    private OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {

            if (checkedId == R.id.tab_devices) {
                switchContent(mContent, deviceFragment);
            } else if (checkedId == R.id.tab_groups) {
                switchContent(mContent, groupFragment);
            } else if (checkedId == R.id.tab_account) {
                switchContent(mContent, meFragment);
            }
        }
    };

    private int connectMeshAddress;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_LIST:
                    deviceFragment.notifyDataSetChanged();
                    break;
            }
        }
    };

    private Handler mDelayHandler = new Handler();
    private int delay = 200;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "蓝牙开启");
                        TelinkLightService.Instance().idleMode(true);
                        autoConnect();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "蓝牙关闭");
                        break;
                }
            }
        }
    };
    private Disposable mDisposableDevices;
    private Disposable mDisposableGroup;
    private Disposable mDisposableDevicesText;
    private Disposable mDisposableGroupsText;
    private Disposable mDisposableAcount;
    private Disposable mDisposableAcountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        //TelinkLog.ENABLE = false;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initBottomIconStatus();
        initBottomIconClickListener();

        this.mApplication = (TelinkLightApplication) this.getApplication();

        this.fragmentManager = this.getFragmentManager();

        this.deviceFragment = (DeviceListFragment) FragmentFactory
                .createFragment(R.id.tab_devices);
        this.groupFragment = (GroupListFragment) FragmentFactory
                .createFragment(R.id.tab_groups);
        this.meFragment = (MeFragment) FragmentFactory
                .createFragment(R.id.tab_account);

//        this.tabs = (ConstraintLayout) this.findViewById(R.id.tabs);
//        this.tabs.setOnCheckedChangeListener(this.checkedChangeListener);


        if (savedInstanceState == null) {

            FragmentTransaction transaction = this.fragmentManager
                    .beginTransaction();
            transaction.add(R.id.content, this.deviceFragment).commit();

            this.mContent = this.deviceFragment;
        }

//        this.mApplication.doInit();

        TelinkLog.d("-------------------------------------------");
        TelinkLog.d(Build.MANUFACTURER);
        TelinkLog.d(Build.TYPE);
        TelinkLog.d(Build.BOOTLOADER);
        TelinkLog.d(Build.DEVICE);
        TelinkLog.d(Build.HARDWARE);
        TelinkLog.d(Build.SERIAL);
        TelinkLog.d(Build.BRAND);
        TelinkLog.d(Build.DISPLAY);
        TelinkLog.d(Build.FINGERPRINT);

        TelinkLog.d(Build.PRODUCT + ":" + Build.VERSION.SDK_INT + ":" + Build.VERSION.RELEASE + ":" + Build.VERSION.CODENAME + ":" + Build.ID);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(mReceiver, filter);

        checkPermission();
    }

    /**
     * 设置底部Icon的默认状态，默认让的一个Icon作为选中Icon
     */
    private void initBottomIconStatus() {
        tabDevices.setSelected(true);
        tabDevices.setColorFilter(getResources().getColor(R.color.theme_positive_color));
    }

    /**
     * 初始化底部Icon的点击事件监听
     */
    private void initBottomIconClickListener() {
        //获取APP主颜色
        int positiveColor = getResources().getColor(R.color.theme_positive_color);
        Consumer<Object> devicesConsumer = o -> {
            if (!tabDevices.isSelected()) {
                tabDevices.setSelected(true);
                tabGroups.setSelected(false);
                tabAccount.setSelected(false);
                tabDevices.setColorFilter(positiveColor);
                tabGroups.setColorFilter(Color.GRAY);
                tabAccount.setColorFilter(Color.GRAY);

                tvLight.setTextColor(positiveColor);
                tvGroups.setTextColor(Color.GRAY);
                tvAccount.setTextColor(Color.GRAY);

                switchContent(mContent, deviceFragment);
            }
        };

        Consumer<Object> groupConsumer = o -> {
            if (!tabGroups.isSelected()) {
                tabDevices.setSelected(false);
                tabGroups.setSelected(true);
                tabAccount.setSelected(false);
                tabDevices.setColorFilter(Color.GRAY);
                tabGroups.setColorFilter(positiveColor);
                tabAccount.setColorFilter(Color.GRAY);

                tvLight.setTextColor(Color.GRAY);
                tvGroups.setTextColor(positiveColor);
                tvAccount.setTextColor(Color.GRAY);

                switchContent(mContent, groupFragment);
            }
        };

        Consumer<Object> acountConsumer = o -> {
            if (!tabAccount.isSelected()) {
                tabDevices.setSelected(false);
                tabGroups.setSelected(false);
                tabAccount.setSelected(true);
                tabDevices.setColorFilter(Color.GRAY);
                tabGroups.setColorFilter(Color.GRAY);
                tabAccount.setColorFilter(positiveColor);

                tvLight.setTextColor(Color.GRAY);
                tvGroups.setTextColor(Color.GRAY);
                tvAccount.setTextColor(positiveColor);

                switchContent(mContent, meFragment);
            }
        };

        mDisposableDevices = RxView.clicks(tabDevices)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(devicesConsumer);
        mDisposableDevicesText = RxView.clicks(tvLight)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(devicesConsumer);

        mDisposableGroup = RxView.clicks(tabGroups)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(groupConsumer);
        mDisposableGroupsText = RxView.clicks(tvGroups)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(groupConsumer);

        mDisposableAcount = RxView.clicks(tabAccount)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(acountConsumer);
        mDisposableAcountText = RxView.clicks(tvAccount)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(acountConsumer);

    }


    int PERMISSION_REQUEST_CODE = 0x10;

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    // 显示解释权限用途的界面，然后再继续请求权限
                } else {
                    // 没有权限，直接请求权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                            PERMISSION_REQUEST_CODE);
                }
            }
        }

    }

    @Override
    protected void onStart() {

        super.onStart();

        Log.d(TAG, "onStart");

        int result = BuildUtils.assetSdkVersion("4.4");
        Log.d(TAG, " Version : " + result);

        // 监听各种事件
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(NotificationEvent.ONLINE_STATUS, this);
        this.mApplication.addEventListener(NotificationEvent.GET_ALARM, this);
        this.mApplication.addEventListener(NotificationEvent.GET_DEVICE_STATE, this);
        this.mApplication.addEventListener(ServiceEvent.SERVICE_CONNECTED, this);
        this.mApplication.addEventListener(MeshEvent.OFFLINE, this);

        this.mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this);

        this.autoConnect();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(getApplicationContext())) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        //如果蓝牙没开，则弹窗提示用户打开蓝牙
        if (!LeBluetooth.getInstance().isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.openBluetooth));
            builder.setNeutralButton(R.string.btn_cancel, (dialog, which) -> finish());
            builder.setNegativeButton(R.string.btn_ok, (dialog, which) -> LeBluetooth.getInstance().enable(getApplicationContext()));
            builder.show();
        }

        DeviceInfo deviceInfo = this.mApplication.getConnectDevice();

        if (deviceInfo != null) {
            this.connectMeshAddress = this.mApplication.getConnectDevice().meshAddress & 0xFF;
        }

        Log.d(TAG, "onResume");
    }

    public static void getAlarm() {
        TelinkLightService.Instance().sendCommandNoResponse((byte) 0xEC, 0x0000, new byte[]{0x10, (byte) 0x00});
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        this.mApplication.removeEventListener(this);
        TelinkLightService.Instance().disableAutoRefreshNotify();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        this.mApplication.doDestroy();
        this.mDelayHandler.removeCallbacksAndMessages(null);
        //移除事件
        this.mApplication.removeEventListener(this);
        Lights.getInstance().clear();
    }

    /**
     * 自动重连
     */
    private void autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().getMode() != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connect_state));
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY,false);

                if (this.mApplication.isEmptyMesh())
                    return;

//                Lights.getInstance().clear();
                this.mApplication.refreshLights();


                this.deviceFragment.notifyDataSetChanged();

                Mesh mesh = this.mApplication.getMesh();

                if (TextUtils.isEmpty(mesh.name) || TextUtils.isEmpty(mesh.password)) {
                    TelinkLightService.Instance().idleMode(true);
                    return;
                }

                //自动重连参数
                LeAutoConnectParameters connectParams = Parameters.createAutoConnectParameters();
                connectParams.setMeshName(mesh.name);
                connectParams.setPassword(mesh.password);
                connectParams.autoEnableNotification(true);

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh.isOtaProcessing()) {
                    connectParams.setConnectMac(mesh.otaDevice.mac);
                    saveLog("Action: AutoConnect:" + mesh.otaDevice.mac);
                } else {
                    saveLog("Action: AutoConnect:NULL");
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams);
            }

            //刷新Notify参数
            LeRefreshNotifyParameters refreshNotifyParams = Parameters.createRefreshNotifyParameters();
            refreshNotifyParams.setRefreshRepeatCount(2);
            refreshNotifyParams.setRefreshInterval(2000);
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams);
        }
    }

    private void switchContent(Fragment from, Fragment to) {

        if (this.mContent != to) {
            this.mContent = to;

            FragmentTransaction transaction = this.fragmentManager
                    .beginTransaction();

            if (!to.isAdded()) {
                transaction.hide(from).add(R.id.content, to);
            } else {
                transaction.hide(from).show(to);
            }

            transaction.commit();
        }
    }

    private void onDeviceStatusChanged(DeviceEvent event) {

        DeviceInfo deviceInfo = event.getArgs();

        switch (deviceInfo.status) {
            case LightAdapter.STATUS_LOGIN:
                ToastUtils.showLong(getString(R.string.connect_success));
                SharedPreferencesHelper.putBoolean(this,Constant.CONNECT_STATE_SUCCESS_KEY,true);
                DeviceInfo connectDevice = this.mApplication.getConnectDevice();
                if (connectDevice != null) {
                    this.connectMeshAddress = connectDevice.meshAddress;
//                this.showToast("login success");
                    if (TelinkLightService.Instance().getMode() == LightAdapter.MODE_AUTO_CONNECT_MESH) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                TelinkLightService.Instance().sendCommandNoResponse((byte) 0xE4, 0xFFFF, new byte[]{});
                            }
                        }, 3 * 1000);
                    }

                    if (TelinkLightApplication.getApp().getMesh().isOtaProcessing() && foreground) {
                        startActivity(new Intent(this, OTAUpdateActivity.class)
                                .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_PREVIOUS));
                    }
                }
                break;
            case LightAdapter.STATUS_CONNECTING:
//                this.showToast("login");
                break;
            case LightAdapter.STATUS_LOGOUT:
//                this.showToast("disconnect");
                onLogout();
                break;

            case LightAdapter.STATUS_ERROR_N:
                onNError(event);
            default:
                break;
        }
    }

    private void onNError(final DeviceEvent event) {

        ToastUtils.showLong(getString(R.string.connect_fail));
        SharedPreferencesHelper.putBoolean(this,Constant.CONNECT_STATE_SUCCESS_KEY,false);

        TelinkLightService.Instance().idleMode(true);
        TelinkLog.d("DeviceScanningActivity#onNError");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("当前环境:Android7.0!连接重试:" + " 3次失败!");
        builder.setNegativeButton("confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void onLogout() {
        List<Light> lights = Lights.getInstance().get();
        for (Light light : lights) {
            light.status = ConnectionStatus.OFFLINE;
            light.updateIcon();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceFragment.notifyDataSetChanged();
            }
        });
    }

    private void onAlarmGet(NotificationEvent notificationEvent) {
        GetAlarmNotificationParser.AlarmInfo info = GetAlarmNotificationParser.create().parse(notificationEvent.getArgs());
        if (info != null)
            TelinkLog.d("alarm info index: " + info.index);


    }


    /**
     * 检查是不是灯
     */
    private boolean checkIsLight(NotificationEvent event) {
        if (event != null) {
            NotificationInfo notificationInfo = event.getArgs();
            if (notificationInfo != null) {
                byte[] params = notificationInfo.params;
                if (params != null && params.length > 0) {
                    if (params[3] == UpdateStatusDeviceType.NORMAL_SWITCH)
                        return false;
                }
            }
        }
        return true;
    }


    /**
     * 处理{@link NotificationEvent#ONLINE_STATUS}事件
     */
    private synchronized void onOnlineStatusNotify(NotificationEvent event) {
        if (!checkIsLight(event))
            return;
        TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().getId());
        List<OnlineStatusNotificationParser.DeviceNotificationInfo> notificationInfoList;
        //noinspection unchecked
        notificationInfoList = (List<OnlineStatusNotificationParser.DeviceNotificationInfo>) event.parse();

        if (notificationInfoList == null || notificationInfoList.size() <= 0)
            return;


        /*if (this.deviceFragment != null) {
            this.deviceFragment.onNotify(notificationInfoList);
        }*/

        for (OnlineStatusNotificationParser.DeviceNotificationInfo notificationInfo : notificationInfoList) {

            int meshAddress = notificationInfo.meshAddress;
            int brightness = notificationInfo.brightness;

            Light light = this.deviceFragment.getDevice(meshAddress);

            if (light == null) {
                light = new Light();
                this.deviceFragment.addDevice(light);
            }

            light.meshAddress = meshAddress;
            light.brightness = brightness;
            light.status = notificationInfo.connectionStatus;

            if (light.meshAddress == this.connectMeshAddress) {
                light.textColor = this.getResources().getColor(
                        R.color.theme_positive_color);
            } else {
                light.textColor = this.getResources().getColor(
                        R.color.black);
            }

            light.updateIcon();
        }

        mHandler.obtainMessage(UPDATE_LIST).sendToTarget();
    }

    private void onServiceConnected(ServiceEvent event) {
        this.autoConnect();
    }

    private void onServiceDisconnected(ServiceEvent event) {

    }

    AlertDialog.Builder mTimeoutBuilder;

    private void onMeshOffline(MeshEvent event) {
        List<Light> lights = Lights.getInstance().get();
        Iterator<Light> it = lights.iterator();
        while (it.hasNext()) {
            Light light = it.next();
            light.status = ConnectionStatus.OFFLINE;
            light.updateIcon();
            it.remove();
        }
//        for (Light light : lights) {
////            light.status = ConnectionStatus.OFFLINE;
////            light.updateIcon();
//              lights.remove(light);
//        }
        this.deviceFragment.notifyDataSetChanged();

        if (TelinkLightApplication.getApp().getMesh().isOtaProcessing()) {
            TelinkLightService.Instance().idleMode(true);
            if (mTimeoutBuilder == null) {
                mTimeoutBuilder = new AlertDialog.Builder(this);
                mTimeoutBuilder.setTitle("AutoConnect Fail");
                mTimeoutBuilder.setMessage("Connect device:" + TelinkLightApplication.getApp().getMesh().otaDevice.mac + " Fail, Quit? \nYES: quit MeshOTA process, NO: retry");
                mTimeoutBuilder.setNeutralButton(R.string.quite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Mesh mesh = TelinkLightApplication.getApp().getMesh();
                        mesh.otaDevice = null;
                        mesh.saveOrUpdate(MainActivity.this);
                        autoConnect();
                        dialog.dismiss();
                    }
                });
                mTimeoutBuilder.setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        autoConnect();
                        dialog.dismiss();
                    }
                });
                mTimeoutBuilder.setCancelable(false);
            }
            mTimeoutBuilder.show();
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
                this.onOnlineStatusNotify((NotificationEvent) event);
                break;

            case NotificationEvent.GET_ALARM:
//                this.onAlarmGet((NotificationEvent) event);
                break;
            case DeviceEvent.STATUS_CHANGED:
                this.onDeviceStatusChanged((DeviceEvent) event);
                break;
            case MeshEvent.OFFLINE:
                this.onMeshOffline((MeshEvent) event);
                break;
            case ServiceEvent.SERVICE_CONNECTED:
                this.onServiceConnected((ServiceEvent) event);
                break;
            case ServiceEvent.SERVICE_DISCONNECTED:
                this.onServiceDisconnected((ServiceEvent) event);
                break;
            case NotificationEvent.GET_DEVICE_STATE:
                onNotificationEvent((NotificationEvent) event);
                break;

            case ErrorReportEvent.ERROR_REPORT:
                ErrorReportInfo info = ((ErrorReportEvent) event).getArgs();
                TelinkLog.d("MainActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
                        + " errorCode-" + info.errorCode
                        + " deviceId-" + info.deviceId);
                break;
        }
    }

    @Override
    protected void onLocationEnable() {
        autoConnect();
    }


    private void onNotificationEvent(NotificationEvent event) {
        if (!foreground) return;
        // 解析版本信息
        byte[] data = event.getArgs().params;
        if (data[0] == NotificationEvent.DATA_GET_MESH_OTA_PROGRESS) {
            TelinkLog.w("mesh ota progress: " + data[1]);
            int progress = (int) data[1];
            if (progress != 100) {
                startActivity(new Intent(this, OTAUpdateActivity.class)
                        .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_REPORT)
                        .putExtra("progress", progress));
            }
        }
    }

    //记录用户首次点击返回键的时间
    private long firstTime = 0;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long secondTime = System.currentTimeMillis();
            if (secondTime - firstTime > 2000) {
                Toast.makeText(MainActivity.this, R.string.click_double_exit, Toast.LENGTH_SHORT).show();
                firstTime = secondTime;
                return true;
            } else {
                System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}
