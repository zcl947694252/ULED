package com.dadoutek.uled.ota;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DadouDeviceInfo;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.network.NetworkFactory;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.LeScanEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeOtaParameters;
import com.telink.bluetooth.light.LeScanParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.Manufacture;
import com.telink.bluetooth.light.OtaDeviceInfo;
import com.telink.bluetooth.light.Parameters;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OtaActivity extends TelinkBaseActivity implements EventListener<String>, View.OnClickListener {

    private static final int FILE_SELECT_CODE = 1;
    private TelinkLightApplication mApp;
    private byte[] firmware;
    private int meshAddress;
    private String version;
    private String macAddress;
    private DadouDeviceInfo selectedDevice;
    private TextView name;
    private TextView mac;
    private TextView tip;
    private TextView progress;
    private EditText otaDelay;
    private EditText otaSize;
    private boolean flag;

    private View chooseOta;
    private View startOta;

    private String path;
    private boolean otaCompleted;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            Toast.makeText(OtaActivity.this, "蓝牙关闭", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Toast.makeText(OtaActivity.this, "蓝牙开启", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Toast.makeText(OtaActivity.this, "正在关闭蓝牙", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Toast.makeText(OtaActivity.this, "正在打开蓝牙", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_ota);
        this.meshAddress = this.getIntent().getIntExtra("meshAddress", 0);
        macAddress = getIntent().getStringExtra(Constant.OTA_MAC);
        version = getIntent().getStringExtra(Constant.OTA_VERSION);
        this.mApp = (TelinkLightApplication) this.getApplication();

        //监听事件
        this.mApp.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this);

        this.name = (TextView) this.findViewById(R.id.name);
        this.mac = (TextView) this.findViewById(R.id.mac);
        this.tip = (TextView) this.findViewById(R.id.tip);
        this.progress = (TextView) this.findViewById(R.id.progress);
        this.otaDelay = (EditText) this.findViewById(R.id.otadelay);
        this.otaSize = (EditText) this.findViewById(R.id.otaSize);

        this.chooseOta = this.findViewById(R.id.chooseFile);
        this.chooseOta.setOnClickListener(this);
        this.startOta = this.findViewById(R.id.startOta);
        this.startOta.setOnClickListener(this);

        Mesh mesh = this.mApp.getMesh();

        this.selectedDevice = mesh.getDevice(this.meshAddress);

        if (this.selectedDevice == null || TextUtils.isEmpty(this.selectedDevice.macAddress)) {
            Toast.makeText(this, "ota升级,需要把灯加入到网络!", Toast.LENGTH_SHORT).show();
            return;
        }

        this.name.setText(this.selectedDevice.deviceName);
        this.mac.setText(this.selectedDevice.macAddress);

        //this.readFirmware("light_8267_ota.bin");
        //TelinkLightService.Instance().startOta(firmware);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    private void append(String msg) {
        this.tip.append(msg + "\r\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        //移除事件并断开连接
        this.mApp.removeEventListener(this);

        // idle
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.idleMode(true);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "请选择Firmware文件"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            return;

        Uri uri = data.getData();
        this.path = uri.getPath();
        TelinkLog.d(path);
    }

    /**
     * 开始扫描
     */
    private void startScan() {
        LeScanParameters params = Parameters.createScanParameters();
        params.setMeshName(this.selectedDevice.meshName);
        params.setTimeoutSeconds(20);
        /*params.set(Parameters.PARAM_MESH_NAME, this.selectedDevice.meshName);
        params.set(Parameters.PARAM_SCAN_TIMEOUT_SECONDS, 10);*/
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.startScan(params);
    }

    /**
     * 开始OTA
     */
    private void startOta() {
        otaCompleted = false;
        Mesh currentMesh = this.mApp.getMesh();
        LeOtaParameters params = LeOtaParameters.create();
        //("currentMesh name = " + currentMesh.getName());
        params.setMeshName(currentMesh.getName());
        params.setPassword(NetworkFactory.md5(NetworkFactory.md5(currentMesh.getName()) + currentMesh.getName()).substring(0, 16));
        params.setLeScanTimeoutSeconds(10);

        Manufacture.Builder builder = new Manufacture.Builder();
        builder.setOtaDelay(Integer.parseInt(this.otaDelay.getText().toString()));
        builder.setOtaSize(Integer.parseInt(this.otaSize.getText().toString()));
        Manufacture.setManufacture(builder.build());
        OtaDeviceInfo deviceInfo = new OtaDeviceInfo();
        deviceInfo.macAddress = this.selectedDevice.macAddress;
        deviceInfo.firmware = this.firmware;
        params.setDeviceInfo(deviceInfo);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance != null)
            instance.startOta(params);
    }

    /**
     * 事件处理方法
     *
     * @param event
     */
    @Override
    public void performed(Event<String> event) {
        if (event instanceof LeScanEvent) {
            this.onLeScanEvent((LeScanEvent) event);
        } else if (event instanceof DeviceEvent) {
            this.onDeviceEvent((DeviceEvent) event);
        }
    }

    private void readFirmware(String fileName) {
        try {
            InputStream stream = new FileInputStream(fileName);
            int length = stream.available();
            this.firmware = new byte[length];
            stream.read(this.firmware);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理LeScanEvent事件
     *
     * @param event
     */
    private void onLeScanEvent(LeScanEvent event) {
        String type = event.getType();

        switch (type) {
            case LeScanEvent.LE_SCAN:
                //处理扫描到的设备
                DeviceInfo deviceInfo = event.getArgs();
                Toast.makeText(this, deviceInfo.meshName + "-" + deviceInfo.macAddress, Toast.LENGTH_SHORT).show();
                if (deviceInfo.macAddress.equals(this.selectedDevice.macAddress)) {
                    this.flag = true;
                    this.append("connecting");
                    TelinkLightService instance = TelinkLightService.Instance();
                    if (instance != null) {
                        instance.idleMode(true);
                        instance.connect(deviceInfo.macAddress, 10);
                    }
                }

                break;
            case LeScanEvent.LE_SCAN_TIMEOUT:
                if (!this.flag) {
                    this.append("not found");
                }
                break;
        }
    }

    /**
     * 处理DeviceEvent事件
     *
     * @param event
     */
    private void onDeviceEvent(DeviceEvent event) {
        String type = event.getType();
        switch (type) {
            case DeviceEvent.STATUS_CHANGED:
                int status = event.getArgs().status;
                if (status == LightAdapter.STATUS_OTA_PROGRESS) {
                    OtaDeviceInfo deviceInfo = (OtaDeviceInfo) event.getArgs();
                    this.progress.setText("ota progress :" + deviceInfo.progress + "%");
                } else if (status == LightAdapter.STATUS_CONNECTED) {
                    //连接成功后获取firmware信息
                    this.append("connected");
                    this.append("get firmware");
                    TelinkLightService instance = TelinkLightService.Instance();
                    if (instance != null) {
                        instance.getFirmwareVersion();
                    }
                } else if (status == LightAdapter.STATUS_LOGOUT) {
                    if (otaCompleted) {
                        this.append("ota success");
                    }
                } else if (status == LightAdapter.STATUS_OTA_COMPLETED) {
                    otaCompleted = true;
                    this.append("otaCompleted");
                } else if (status == LightAdapter.STATUS_OTA_FAILURE) {
                    this.append("ota fail");
                } else if (status == LightAdapter.STATUS_GET_FIRMWARE_COMPLETED) {
                    //firmware信息获取成功，比较版本后,开始ota
                    DeviceInfo deviceInfo = event.getArgs();
                    this.append("firmware :" + deviceInfo.firmwareRevision);
                    this.readFirmware(this.path);
                    this.append("start ota");
                    this.startOta();
                } else if (status == LightAdapter.STATUS_GET_FIRMWARE_FAILURE) {
                    this.append("get firmware fail");
                    this.append("ota fail");
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.chooseOta) {
            this.showFileChooser();
        } else if (v == this.startOta) {
            this.tip.setText("");
            this.append("scanning");
            this.startScan();
        }
    }
}
