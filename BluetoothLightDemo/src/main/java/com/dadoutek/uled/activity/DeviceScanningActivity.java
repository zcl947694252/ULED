package com.dadoutek.uled.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.adapter.GroupsRecyclerViewAdapter;
import com.dadoutek.uled.model.Cmd;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.DataCreater;
import com.dadoutek.uled.util.TimeUtil;
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
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import io.reactivex.functions.Consumer;

public final class DeviceScanningActivity extends TelinkMeshErrorDealActivity implements AdapterView.OnItemClickListener, EventListener<String> {

//    @Bind(R.id.recycler_view_groups)
    android.support.v7.widget.RecyclerView recyclerViewGroups;
//    @Bind(R.id.groups_bottom)
    LinearLayout groupsBottom;
    private ImageView backView;
    private Button btnScan;
    private Button btnLog;
    private Button btnAddGroups;

    private LayoutInflater inflater;
    private DeviceListAdapter adapter;

    private TelinkLightApplication mApplication;
    private List<DeviceInfo> updateList;

    private boolean isInit = false;
    private RxPermissions mRxPermission;
    private GridView deviceListView;
    private Handler mHandler = new Handler();

    private int preTime = 0;
    private int nextTime = 0;
    private boolean canStartTimer = true;
    private Timer timer;
    private Dialog loadDialog;
    private Groups groups;

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

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Cmd.SCANCOMPLET:
                    if (msg.arg1 == Cmd.SCANFAIL) {
                        btnAddGroups.setVisibility(View.VISIBLE);
                        btnAddGroups.setText("重新扫描");
                        btnAddGroups.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startScan(0);
                                btnAddGroups.setVisibility(View.GONE);
                            }
                        });
                        if (timer != null) {
                            timer.cancel();
                        }
                        closeDialog();
                        showToast(getString(R.string.scan_end));
                        canStartTimer = false;
                        nextTime = 0;
                    } else if (msg.arg1 == Cmd.SCANSUCCESS) {
                        btnAddGroups.setVisibility(View.VISIBLE);
                        btnAddGroups.setText("开始分组");
                        btnAddGroups.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //实现分组方案
                                startGrouping();
                            }
                        });
                        if (timer != null) {
                            timer.cancel();
                        }
                        nextTime = 0;
                        closeDialog();
                        canStartTimer = false;
                    }

                    break;
            }
        }
    };

    private void startGrouping() {
        btnAddGroups.setVisibility(View.VISIBLE);
        btnAddGroups.setText("确定分组");
        btnAddGroups.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Light>list=adapter.getLights();
                boolean hasData=false;//有无被选择分组的选项
                boolean isEnd=false;//是否分组结束
                for(int i=0;i<list.size();i++){
                    if(list.get(i).selected=true){
                        hasData=true;
                        break;
                    }else if(!list.get(i).hasGroup){
                        hasData=false;
                        break;
                    }else if(list.get(i).hasGroup&&i==list.size()-1){
                        isEnd=true;
                    }
                }

                if(hasData){
                    //进行分组操作
                }else if(!hasData&&!isEnd){
                    showToast("请至少选择一个灯！");
                }else if(!hasData&&isEnd){
                    //分组结束，进入下一个开关分组页面
                }
            }
        });
        groupsBottom.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        //设置RecyclerView 布局
        recyclerViewGroups.setLayoutManager(layoutmanager);
        //设置Adapter
        GroupsRecyclerViewAdapter adapter = new GroupsRecyclerViewAdapter(groups);
        recyclerViewGroups.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
//        TelinkLightService.Instance().idleMode();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRxPermission = new RxPermissions(this);
        setContentView(R.layout.activity_device_scanning);
        groups=DataCreater.getGroups();
        checkPermission();
        checkSupport();
        initView();
        initClick();
//        onLeScan(null);
        this.startScan(0);
    }

    private void initClick() {
        this.backView.setOnClickListener(this.clickListener);
        this.btnScan.setOnClickListener(this.clickListener);
        this.btnLog.setOnClickListener(this.clickListener);
        deviceListView.setOnItemClickListener(this);
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

        //监听事件
        this.mApplication = (TelinkLightApplication) this.getApplication();
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(MeshEvent.UPDATE_COMPLETED, this);
        this.mApplication.addEventListener(MeshEvent.ERROR, this);

        this.inflater = this.getLayoutInflater();
        this.adapter = new DeviceListAdapter();

        this.backView = (ImageView) this.findViewById(R.id.img_header_menu_left);
        groupsBottom=findViewById(R.id.groups_bottom);
        recyclerViewGroups=findViewById(R.id.recycler_view_groups);
        this.btnAddGroups = findViewById(R.id.btn_add_groups);
        this.btnLog = findViewById(R.id.btn_log);
        this.btnScan = (Button) this.findViewById(R.id.btn_scan);
        this.btnScan.setEnabled(false);
        this.btnScan.setBackgroundResource(R.color.gray);
        deviceListView = (GridView) this.findViewById(R.id.list_devices);
        deviceListView.setAdapter(this.adapter);
        this.updateList = new ArrayList<>();

//        groups= DataCreater.getGroups();
        try {
            Intent intent = getIntent();
            isInit = (boolean) intent.getExtras().get("isInit");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isInit) {
                backView.setVisibility(View.GONE);
                btnScan.setVisibility(View.GONE);
                btnLog.setVisibility(View.GONE);
                btnAddGroups.setVisibility(View.GONE);
            } else {
                backView.setVisibility(View.VISIBLE);
                btnScan.setVisibility(View.VISIBLE);
                btnLog.setVisibility(View.VISIBLE);
                btnAddGroups.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.updateList = null;
        timer.cancel();
        canStartTimer = false;
        nextTime = 0;
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
                            openLoadingDialog();
                        }
                    }, delay);
                } else {
                    // TODO: 2018/3/26 弹框提示为何需要此权限，点击确定后再次申请权限，点击取消退出.
                    AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceScanningActivity.this);
                    dialog.setMessage(getResources().getString(R.string.scan_tip));
                    dialog.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startScan(0);
                        }
                    });
                    dialog.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
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
        if (preTime != 0) {//表示目前已经搜到了至少有一个设备
            creatMessage(Cmd.SCANCOMPLET, Cmd.SCANSUCCESS);
        } else {
            creatMessage(Cmd.SCANCOMPLET, Cmd.SCANFAIL);
        }
    }

    private void creatMessage(int what, int arg) {
        Message message = new Message();
        message.what = what;
        message.arg1 = arg;
        handler.sendMessage(message);
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

                if (canStartTimer) {
                    startTimer();
                    canStartTimer = false;
                }

                preTime = TimeUtil.getNowSeconds();
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

    private void startTimer() {
        timer = new Timer();
        timer.schedule(task, 1000, 1000);
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            nextTime = TimeUtil.getNowSeconds();
            Log.d("DeviceScanning", "timer: " + "nextTime=" + nextTime + ";preTime=" + preTime);
            if (preTime > 0 && nextTime - preTime >= 30) {
                creatMessage(Cmd.SCANCOMPLET, Cmd.SCANSUCCESS);
            }
        }
    };

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
            getDeviceGroup(light);
            checkSelectLamp(light);
        } else {
            this.updateList.remove(light.raw);
        }
    }

    private void getDeviceGroup(Light light) {
        byte opcode = (byte) 0xDD;
        int dstAddress = light.meshAddress;
        byte[] params = new byte[]{0x08, 0x01};

        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
        TelinkLightService.Instance().updateNotification();
    }

    private void checkSelectLamp(Light light) {

        Group group=groups.get(0);

        int groupAddress = group.meshAddress;
        int dstAddress = light.meshAddress;
        byte opcode = (byte) 0xD7;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};

        Log.d("Scanner", "checkSelectLamp: "+"opcode:"+opcode+";  dstAddress:"+dstAddress+";  params:"+params.toString());
        if (!group.checked) {
            params[0] = 0x01;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);

        } else {
            params[0] = 0x00;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
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
                holder.selected.setVisibility(View.VISIBLE);

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

        public List<Light> getLights(){
            return lights;
        }
    }

    public void openLoadingDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialogview, null);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);

        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);

        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
                R.animator.load_animation);

        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = new Dialog(this,
                    R.style.FullHeightDialog);
        }
        loadDialog.setCancelable(true);
        loadDialog.show();
        loadDialog.setContentView(layout, new LinearLayout.LayoutParams(280,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public void closeDialog() {
        if (loadDialog != null) {
            loadDialog.dismiss();
        }
    }
}
