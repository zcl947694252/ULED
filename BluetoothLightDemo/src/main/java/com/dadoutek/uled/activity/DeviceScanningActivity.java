package com.dadoutek.uled.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Mesh;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.LeScanEvent;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeScanParameters;
import com.telink.bluetooth.light.LeUpdateParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.Parameters;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.model.Light;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

public final class DeviceScanningActivity extends TelinkMeshErrorDealActivity implements AdapterView.OnItemClickListener, EventListener<String> {

    private ImageView backView;
    private Button btnScan;

    private LayoutInflater inflater;
    private DeviceListAdapter adapter;

    private TelinkLightApplication mApplication;
    private List<DeviceInfo> updateList;

    private boolean isInit = false;
    private RxPermissions mRxPermission;


    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
//                TelinkLightService.Instance().idleMode();
                finish();
            } else if (v == btnScan) {
                finish();
                //stopScanAndUpdateMesh();
            } else if (v.getId() == R.id.btn_log) {
                startActivity(new Intent(DeviceScanningActivity.this, LogInfoActivity.class));
            }
        }
    };
    private Handler mHandler = new Handler();

    @Override
    public void onBackPressed() {
//        TelinkLightService.Instance().idleMode();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRxPermission = new RxPermissions(this);
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_device_scanning);

        //监听事件
        this.mApplication = (TelinkLightApplication) this.getApplication();
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(MeshEvent.UPDATE_COMPLETED, this);
        this.mApplication.addEventListener(MeshEvent.ERROR, this);

        this.inflater = this.getLayoutInflater();
        this.adapter = new DeviceListAdapter();

        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);
        findViewById(R.id.btn_log).setOnClickListener(this.clickListener);
        this.btnScan = (Button) this.findViewById(R.id.btn_scan);
        this.btnScan.setOnClickListener(this.clickListener);
        this.btnScan.setEnabled(false);
        this.btnScan.setBackgroundResource(R.color.gray);

        GridView deviceListView = (GridView) this
                .findViewById(R.id.list_devices);
        deviceListView.setAdapter(this.adapter);
        deviceListView.setOnItemClickListener(this);

        this.updateList = new ArrayList<>();

        checkPermission();
        checkSupport();

        initView();

//        onLeScan(null);
        this.startScan(0);
    }

    public void checkSupport() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(getApplicationContext())) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        if (!LeBluetooth.getInstance().isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("开启蓝牙，体验智能灯!");
            builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setNegativeButton("enable", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LeBluetooth.getInstance().enable(getApplicationContext());
                }
            });
            builder.show();
        }
    }

    int PERMISSION_REQUEST_CODE = 0x10;

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("douda", "checkPerm: " + checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) + "");
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

    private void initView() {
//        try{
//            Intent intent=getIntent();
//            isInit= (boolean) intent.getExtras().get("isInit");
//        }catch (Exception e){
//            e.printStackTrace();
//        }finally {
//            if(isInit){
//
//            }else{
//
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.updateList = null;
        this.mApplication.removeEventListener(this);
        this.mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onLocationEnable() {
        startScan(50);
    }

    /**
     * 开始扫描
     */
    private void startScan(final int delay) {
        mRxPermission.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean granted) throws Exception {
                if (granted) {
                    TelinkLightService.Instance().idleMode(true);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mApplication.isEmptyMesh())
                                return;
                            Mesh mesh = mApplication.getMesh();
                            //扫描参数
                            LeScanParameters params = LeScanParameters.create();
                            params.setMeshName(mesh.factoryName);
                            params.setOutOfMeshName("out_of_mesh");
                            params.setTimeoutSeconds(10);
                            params.setScanMode(true);
//                params.setScanMac("FF:FF:7A:68:6B:7F");
                            TelinkLightService.Instance().startScan(params);
                        }
                    }, delay);
                } else {
                    // TODO: 2018/3/26 弹框提示为何需要此权限，点击确定后再次申请权限，点击取消退出.
                    AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceScanningActivity.this);
                    dialog.setMessage("扫描灯具需要");
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startScan(0);
                        }
                    });
                    dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    });
                }
            }
        });


    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    private void onLeScan(final LeScanEvent event) {

        final Mesh mesh = this.mApplication.getMesh();
        final int meshAddress = mesh.getDeviceAddress();

        if (meshAddress == -1) {
            this.showToast("哎呦，网络里的灯泡太多了！目前可以有256灯");
            this.finish();
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //更新参数
                LeUpdateParameters params = Parameters.createUpdateParameters();
                params.setOldMeshName(mesh.factoryName);
                params.setOldPassword(mesh.factoryPassword);
                params.setNewMeshName(mesh.name);
                params.setNewPassword(mesh.password);

                DeviceInfo deviceInfo = event.getArgs();
                deviceInfo.meshAddress = meshAddress;
                params.setUpdateDeviceList(deviceInfo);
//        params.setUpdateMeshIndex(meshAddress);
                //params.set(Parameters.PARAM_DEVICE_LIST, deviceInfo);
//        TelinkLightService.Instance().idleMode(true);
                //加灯
                TelinkLightService.Instance().updateMesh(params);
            }
        }, 200);


    }

    /**
     * 扫描不到任何设备了
     *
     * @param event
     */
    private void onLeScanTimeout(LeScanEvent event) {
        this.btnScan.setEnabled(true);
        this.btnScan.setBackgroundResource(R.color.theme_positive_color);
    }

    private void onDeviceStatusChanged(DeviceEvent event) {

        DeviceInfo deviceInfo = event.getArgs();

        switch (deviceInfo.status) {
            case LightAdapter.STATUS_UPDATE_MESH_COMPLETED:
                //加灯完成继续扫描,直到扫不到设备
                com.dadoutek.uled.model.DeviceInfo deviceInfo1 = new com.dadoutek.uled.model.DeviceInfo();
                deviceInfo1.deviceName = deviceInfo.deviceName;
                deviceInfo1.firmwareRevision = deviceInfo.firmwareRevision;
                deviceInfo1.longTermKey = deviceInfo.longTermKey;
                deviceInfo1.macAddress = deviceInfo.macAddress;
                TelinkLog.d("deviceInfo-Mac:" + deviceInfo.productUUID);
                deviceInfo1.meshAddress = deviceInfo.meshAddress;
                deviceInfo1.meshUUID = deviceInfo.meshUUID;
                deviceInfo1.productUUID = deviceInfo.productUUID;
                deviceInfo1.status = deviceInfo.status;
                deviceInfo1.meshName = deviceInfo.meshName;
                this.mApplication.getMesh().devices.add(deviceInfo1);
                this.mApplication.getMesh().saveOrUpdate(this);
                int meshAddress = deviceInfo.meshAddress & 0xFF;
                Light light = this.adapter.get(meshAddress);

                if (light == null) {
                    light = new Light();
                    light.name = deviceInfo.meshName;
                    light.meshAddress = meshAddress;
                    light.textColor = this.getResources().getColorStateList(
                            R.color.black);
                    light.selected = false;
                    light.raw = deviceInfo;
                    this.adapter.add(light);
                    this.adapter.notifyDataSetChanged();
                }

                this.startScan(1000);
                break;
            case LightAdapter.STATUS_UPDATE_MESH_FAILURE:
                //加灯失败继续扫描
                this.startScan(1000);
                break;

            case LightAdapter.STATUS_ERROR_N:
                this.onNError(event);
                break;
        }
    }

    private void onNError(final DeviceEvent event) {

        TelinkLightService.Instance().idleMode(true);
        TelinkLog.d("DeviceScanningActivity#onNError");

        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanningActivity.this);
        builder.setMessage("当前环境:Android7.0!加灯时连接重试: 3次失败!");
        builder.setNegativeButton("confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setCancelable(false);
        builder.show();

    }

    private void onMeshEvent(MeshEvent event) {
        new AlertDialog.Builder(this).setMessage("重启蓝牙,更好地体验智能灯!").show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Light light = this.adapter.getItem(position);
        light.selected = !light.selected;
        DeviceItemHolder holder = (DeviceItemHolder) view.getTag();
        holder.selected.setChecked(light.selected);

        if (light.selected) {
            this.updateList.add(light.raw);
        } else {
            this.updateList.remove(light.raw);
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
            case LeScanEvent.LE_SCAN:
                this.onLeScan((LeScanEvent) event);
                break;
            case LeScanEvent.LE_SCAN_TIMEOUT:
                this.onLeScanTimeout((LeScanEvent) event);
                break;
            case DeviceEvent.STATUS_CHANGED:
                this.onDeviceStatusChanged((DeviceEvent) event);
                break;
            case MeshEvent.ERROR:
                this.onMeshEvent((MeshEvent) event);
                break;
        }
    }

    private static class DeviceItemHolder {
        public ImageView icon;
        public TextView txtName;
        public CheckBox selected;
    }

    final class DeviceListAdapter extends BaseAdapter {

        private List<Light> lights;

        public DeviceListAdapter() {

        }

        @Override
        public int getCount() {
            return this.lights == null ? 0 : this.lights.size();
        }

        @Override
        public Light getItem(int position) {
            return this.lights.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            DeviceItemHolder holder;

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.device_item, null);
                ImageView icon = (ImageView) convertView
                        .findViewById(R.id.img_icon);
                TextView txtName = (TextView) convertView
                        .findViewById(R.id.txt_name);
                CheckBox selected = (CheckBox) convertView.findViewById(R.id.selected);

                holder = new DeviceItemHolder();

                holder.icon = icon;
                holder.txtName = txtName;
                holder.selected = selected;
                holder.selected.setVisibility(View.GONE);

                convertView.setTag(holder);
            } else {
                holder = (DeviceItemHolder) convertView.getTag();
            }

            Light light = this.getItem(position);

            holder.txtName.setText(light.name);
            holder.icon.setImageResource(R.drawable.icon_light_on);
            holder.selected.setChecked(light.selected);

            return convertView;
        }

        public void add(Light light) {

            if (this.lights == null)
                this.lights = new ArrayList<>();

            this.lights.add(light);
        }

        public Light get(int meshAddress) {

            if (this.lights == null)
                return null;

            for (Light light : this.lights) {
                if (light.meshAddress == meshAddress) {
                    return light;
                }
            }

            return null;
        }
    }
}
