package com.dadoutek.uled.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.DbModel.DBUtils;
import com.dadoutek.uled.DbModel.DbGroup;
import com.dadoutek.uled.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.adapter.GroupsRecyclerViewAdapter;
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener;
import com.dadoutek.uled.model.Cmd;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.StringUtils;
import com.dadoutek.uled.util.TimeUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.LeScanEvent;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeAutoConnectParameters;
import com.telink.bluetooth.light.LeRefreshNotifyParameters;
import com.telink.bluetooth.light.LeScanParameters;
import com.telink.bluetooth.light.LeUpdateParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.Parameters;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by hejiajun on 2018/5/21.
 */

public class DeviceScanningNewActivity extends TelinkMeshErrorDealActivity
        implements AdapterView.OnItemClickListener, EventListener<String> {
    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.rvToolbar)
    RelativeLayout rvToolbar;
    @BindView(R.id.recycler_view_groups)
    RecyclerView recyclerViewGroups;
    @BindView(R.id.add_group_layout)
    LinearLayout addGroupLayout;
    @BindView(R.id.groups_bottom)
    LinearLayout groupsBottom;
    @BindView(R.id.tv_num_lights)
    TextView tvNumLights;
    @BindView(R.id.light_num_layout)
    LinearLayout lightNumLayout;
    @BindView(R.id.list_devices)
    GridView listDevices;
    @BindView(R.id.btn_log)
    Button btnLog;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.btn_add_groups)
    Button btnAddGroups;
    @BindView(R.id.grouping_completed)
    Button groupingCompleted;
    @BindView(R.id.topView)
    ConstraintLayout topView;

    private TelinkLightApplication mApplication;
    private RxPermissions mRxPermission;
    private Handler mHandler = new Handler();
    private static final String TAG = DeviceScanningNewActivity.class.getSimpleName();
    private static final int SCAN_TIMEOUT_SECOND = 10;
    //防止内存泄漏
    CompositeDisposable mDisposable = new CompositeDisposable();
    private Dialog loadDialog;
    private int preTime = 0;
    private int nextTime = 0;
    private Timer timer;
    private final DeviceScanningNewActivity.MyHandler handler = new DeviceScanningNewActivity.MyHandler(this);
    private boolean canStartTimer = true;
    //分组所含灯的缓存
    private List<DbLight> nowLightList;
    private LayoutInflater inflater;
    private boolean grouping;
    private DeviceListAdapter adapter;
    boolean isFirtst = true;
    //标记登录状态
    private boolean isLoginSuccess = false;
    private GridView deviceListView;

    private GroupsRecyclerViewAdapter groupsRecyclerViewAdapter;
    private List<DbGroup> groups;

    //当前所选组index
    private int currentGroupIndex = -1;

    private List<DbLight> updateList;

    private ArrayList<Integer> indexList = new ArrayList<>();

    //对一个灯重复分组时记录上一次分组
    private int originalGroupID = -1;

    private Disposable mGroupingDisposable;

    //灯的mesh地址
    private int dstAddress;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DbLight light = this.adapter.getItem(position);
        light.selected = !light.selected;
        DeviceItemHolder holder = (DeviceItemHolder) view.getTag();
        holder.selected.setChecked(light.selected);

        if (light.selected) {
            this.updateList.add(light);
            nowLightList.get(position).selected = true;
//            getDeviceGroup(light);
            checkSelectLamp(light);
        } else {
            nowLightList.get(position).selected = false;
            this.updateList.remove(light);
        }
    }

    private void checkSelectLamp(DbLight light) {
        if (groups.size() == 0) {
//            mDataManager.creatGroup(true, 0);
            groups = new ArrayList<>();
            ToastUtils.showLong(R.string.tip_add_group);
            return;
        }
//        groups = mDataManager.initGroupsChecked();
        DbGroup group = groups.get(0);
        Log.d("ScanGroup", "checkSelectLamp: " + groups.size());

        int groupAddress = group.getMeshAddr();
        int dstAddress = light.getMeshAddr();
        byte opcode = (byte) 0xD7;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};
        params[0] = 0x01;
        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
    }

    @Override
    protected void onLocationEnable() {

    }

    private static class MyHandler extends Handler {
        //防止内存溢出
        private final WeakReference<DeviceScanningNewActivity> mWeakActivity;

        private MyHandler(DeviceScanningNewActivity mWeakActivity) {
            this.mWeakActivity = new WeakReference<>(mWeakActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DeviceScanningNewActivity activity = mWeakActivity.get();
            switch (msg.what) {
                case Cmd.SCANCOMPLET:
                    if (msg.arg1 == Cmd.SCANFAIL) {
                        activity.scanFail();
                    } else if (msg.arg1 == Cmd.SCANSUCCESS) {
                        Log.d(TAG, "Cmd.SCANSUCCESS");
                        activity.scanSuccess();
                    }
                    break;

                case Cmd.UPDATEDATA:
                    break;
            }
        }
    }

    //扫描失败处理方法
    private void scanFail() {
        btnAddGroups.setVisibility(View.VISIBLE);
        groupingCompleted.setVisibility(View.GONE);
        btnAddGroups.setText(R.string.rescan);
        btnAddGroups.setOnClickListener(new View.OnClickListener() {
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
        //判断是否是第一次使用app，启动导航页
        boolean mIsFirstData = SharedPreferencesHelper.getBoolean(DeviceScanningNewActivity.this,
                SplashActivity.IS_FIRST_LAUNCH, true);
        if (mIsFirstData) {
            startActivity(new Intent(DeviceScanningNewActivity.this, SplashActivity.class));
            finish();
        } else {
            finish();
        }
        canStartTimer = false;
        nextTime = 0;
    }

    //处理扫描成功后
    private void scanSuccess() {

        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowLightList != null && nowLightList.size() > 0) {
            nowLightList.clear();
        }
        nowLightList.addAll(adapter.getLights());

        //先连接灯。
        autoConnect();

        btnAddGroups.setVisibility(View.VISIBLE);
        btnAddGroups.setText(R.string.start_group_bt);

        btnAddGroups.setOnClickListener(v -> {
            if (isLoginSuccess) {
                //进入分组
                startGrouping();
            } else {
                openLoadingDialog(getResources().getString(R.string.device_login_tip));
                Consumer<Boolean> loginConsumer = aBoolean -> {
                    if (aBoolean) {
                        //收到登录成功的时间后关闭dialog并自动进入分组流程。
                        closeDialog();
                        startGrouping();
                    }
                };
                mDisposable.add(Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                    //循环检测isLoginSuccess
                    while (true) {
                        Thread.sleep(20);   //两次检测之间的延时是必须的
                        Log.d("Saw", "isLoginSuccess = " + isLoginSuccess);
                        //如果isLoginSuccess为 true，则发射事件并退出循环检测。
                        if (isLoginSuccess) {
                            emitter.onNext(true);
                            emitter.onComplete();
                            break;
                        }
                    }
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(loginConsumer));
            }
        });
        if (timer != null) {
            timer.cancel();
        }
        nextTime = 0;
        closeDialog();
        canStartTimer = false;
    }

    /**
     * 开始分组
     */
    private void startGrouping() {
        changeGroupView();
        //完成分组
        groupingCompleted.setOnClickListener(v -> {
            //判定是否还有灯没有分组，如果没有允许跳转到下一个页面
            if (checkLightsHaveGroup()) {//所有灯都有分组可以跳转
                showToast(getString(R.string.group_completed));
                //页面跳转前进行分组数据保存
//                mDataManager.saveGroups(groups);
//                mDataManager.updateLights(nowLightList);

                TelinkLightService.Instance().idleMode(true);
                //目前测试调到主页
                if (ActivityUtils.isActivityExistsInStack(MainActivity.class))
                    ActivityUtils.finishToActivity(MainActivity.class, false, true);
                else {
                    ActivityUtils.startActivity(MainActivity.class);
                    finish();
                }
//                Intent intent = new Intent(DeviceScanningActivity.this, MainActivity.class);
//                startActivity(intent);
//                finish();
            } else {
                showToast(getString(R.string.have_lamp_no_group_tip));
            }
        });

        //确定当前分组
        btnAddGroups.setText(R.string.sure_group);
        btnAddGroups.setOnClickListener(v -> {
            sureGroups();
            checkLightsHaveGroup();
        });
    }

    private void sureGroups() {
        boolean hasBeSelected = false;//有无被勾选的用来分组的灯
        if (updateList != null && updateList.size() != 0) {
            hasBeSelected = true;
        }

        if (hasBeSelected) {
            //进行分组操作
            //获取当前选择的分组
            DbGroup group = getCurrentGroup();
            if (group.getMeshAddr() == 0xffff) {
                ToastUtils.showLong(R.string.tip_add_gp);
                return;
            }
            //获取当前勾选灯的列表
            List<DbLight> selectLights = getCurrentSelectLights();

            openLoadingDialog(getResources().getString(R.string.grouping_wait_tip,
                    selectLights.size() + ""));
            //将灯列表的灯循环设置分组
            setGroups(group, selectLights);

        } else if (!hasBeSelected) {
            showToast(getString(R.string.selected_lamp_tip));
        }
    }

    private void setGroups(DbGroup group, List<DbLight> selectLights) {
        if (group == null) {
            Toast.makeText(DeviceScanningNewActivity.this, R.string.select_group_tip, Toast.LENGTH_SHORT).show();
            return;
        }


        for (int i = 0; i < indexList.size(); i++) {
            nowLightList.get(indexList.get(i)).hasGroup = true;

            nowLightList.get(indexList.get(i)).setBelongGroupId(group.getId());
            DBUtils.updateLight(nowLightList.get(indexList.get(i)));
        }

        mGroupingDisposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            int index = 0;
            while (index < selectLights.size()) {
                DbLight light = selectLights.get(index);
//                deletePreGroup(light);
//                saveLightAddrToGroup(light);
                //每个灯发3次分组的命令，确保灯能收到命令.
                for (int i = 0; i < 3; i++) {
                    sendGroupData(light, group, index);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                index++;
            }
            emitter.onNext(true);
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    if (o) {
                        for (int i = 0; i < selectLights.size(); i++) {
                            selectLights.get(i).selected = false;
                        }
                        adapter.notifyDataSetChanged();
                        closeDialog();
                    }
                });

        mDisposable.add(mGroupingDisposable);
//
//        new Handler().postDelayed(() -> {
//
//            closeDialog();
//
//        }, selectLights.size() * 3 * 300);
    }

    private void sendGroupData(DbLight light, DbGroup group, int index) {
        int groupAddress = group.getMeshAddr();
        dstAddress = light.getMeshAddr();
        byte opcode = (byte) 0xD7;          //0xD7 代表添加组的指令
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),      //0x01 代表添加组
                (byte) (groupAddress >> 8 & 0xFF)};

//        Log.d("Scanner", "checkSelectLamp: " + "opcode:" + opcode + ";  dstAddress:" + dstAddress + ";  params:" + params.toString());
//        Log.d("groupingCC", "sendGroupData: "+"----dstAddress:"+dstAddress+";  group:name=="+group.name+";  group:name=="+group.meshAddress+";  lighthas"+light.hasGroup);

        if (group.checked) {
            params[0] = 0x01;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);

        }
    }

    private List<DbLight> getCurrentSelectLights() {
        ArrayList<DbLight> arrayList = new ArrayList<>();
        indexList.clear();
        for (int i = 0; i < nowLightList.size(); i++) {
            if (nowLightList.get(i).selected && !nowLightList.get(i).hasGroup) {
                arrayList.add(nowLightList.get(i));
                indexList.add(i);
            } else if (nowLightList.get(i).selected && nowLightList.get(i).hasGroup) {
                originalGroupID = Integer.parseInt(String.valueOf(nowLightList.get(i).getBelongGroupId()));
                //如果所选灯已有分组，清空后再继续添加到新的分组
//                nowLightList.get(i).belongGroups.clear();
                arrayList.add(nowLightList.get(i));
                indexList.add(i);
            }
        }
        return arrayList;
    }

    private DbGroup getCurrentGroup() {
        if (currentGroupIndex == -1) {
            ToastUtils.showLong(R.string.tip_add_gp);
            return null;
        }
        return groups.get(currentGroupIndex);
    }

    /**
     * 检测是否还有没有分组的灯
     *
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private boolean checkLightsHaveGroup() {
        for (int j = 0; j < nowLightList.size(); j++) {
            if (nowLightList.get(j).getBelongGroupId() == null) {
                groupingCompleted.setBackgroundColor(getResources().getColor(R.color.gray));
                return false;
            }
        }
        groupingCompleted.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        return true;
    }

    //分组页面调整
    private void changeGroupView() {
        grouping = true;
        deviceListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        btnAddGroups.setVisibility(View.VISIBLE);
        groupingCompleted.setVisibility(View.VISIBLE);
        groupsBottom.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewGroups.setLayoutManager(layoutmanager);
        groupsRecyclerViewAdapter = new GroupsRecyclerViewAdapter(groups, onRecyclerviewItemClickListener);
        recyclerViewGroups.setAdapter(groupsRecyclerViewAdapter);
        tvNumLights.setVisibility(View.VISIBLE);
        tvNumLights.setText(getString(R.string.scan_lights_num, nowLightList.size() + ""));
    }

    @OnClick(R.id.add_group_layout)
    public void onViewClicked() {
        addNewGroup();
    }

    private void addNewGroup() {
        final EditText textGp = new EditText(this);
        new AlertDialog.Builder(DeviceScanningNewActivity.this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(R.string.btn_sure), (dialog, which) -> {
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.getText().toString().trim())) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check));
                    } else {
                        DBUtils.addNewGroup(textGp.getText().toString().trim(), groups, this);
                        refreshView();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> {
                    // TODO Auto-generated method stub
                    dialog.dismiss();
                }).show();
    }

    private void refreshView() {
        currentGroupIndex = groups.size() - 1;
        for (int i = groups.size() - 1; i >= 0; i--) {
            if (i == groups.size() - 1) {
                groups.get(i).checked = true;
            } else {
                groups.get(i).checked = false;
            }
        }

        recyclerViewGroups.setAdapter(groupsRecyclerViewAdapter);
        recyclerViewGroups.smoothScrollToPosition(groups.size() - 1);
        groupsRecyclerViewAdapter.notifyDataSetChanged();
        SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex);
    }

    private OnRecyclerviewItemClickListener onRecyclerviewItemClickListener = new OnRecyclerviewItemClickListener() {
        @Override
        public void onItemClickListener(View v, int position) {
            currentGroupIndex = position;
            for (int i = groups.size() - 1; i >= 0; i--) {
                if (i != position && groups.get(i).checked) {
                    updateData(i, false);
                } else if (i == position && !groups.get(i).checked) {
                    updateData(i, true);
                } else if (i == position && groups.get(i).checked) {
                    updateData(i, true);
                }
            }

            groupsRecyclerViewAdapter.notifyDataSetChanged();
//            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
//                    Constant.GROUPS_KEY, groups);
            SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                    Constant.DEFAULT_GROUP_ID, currentGroupIndex);
        }
    };

    private void updateData(int position, boolean checkStateChange) {
        groups.get(position).checked = checkStateChange;
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            nextTime = TimeUtil.getNowSeconds();
            Log.d("DeviceScanning", "timer: " + "nextTime=" + nextTime + ";preTime=" + preTime);
            if (preTime > 0 && nextTime - preTime >= SCAN_TIMEOUT_SECOND) {
                creatMessage(Cmd.SCANCOMPLET, Cmd.SCANSUCCESS);
            }
        }
    };

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private void autoConnect() {
        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().getMode() != LightAdapter.MODE_AUTO_CONNECT_MESH) {


                if (this.mApplication.isEmptyMesh())
                    return;

//                Lights.getInstance().clear();
                this.mApplication.refreshLights();

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
                //连接之前安装的第一个灯，因为第一个灯的信号一般会比较好。
                connectParams.setConnectMac(adapter.getItem(0).getMacAddr());

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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRxPermission = new RxPermissions(this);
        //设置屏幕常亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_device_scanning);
        ButterKnife.bind(this);
        initData();
        initView();
        initClick();
        startScan(0);
    }

    private void initClick() {
        this.imgHeaderMenuLeft.setOnClickListener(this.clickListener);
        this.btnScan.setOnClickListener(this.clickListener);
        this.btnLog.setOnClickListener(this.clickListener);
        deviceListView.setOnItemClickListener(this);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == imgHeaderMenuLeft) {
                finish();
            } else if (v == btnScan) {
                finish();
                //stopScanAndUpdateMesh();
            } else if (v.getId() == R.id.btn_log) {
                startActivity(new Intent(DeviceScanningNewActivity.this, LogInfoActivity.class));
            }
        }
    };

    private void initView() {
        //监听事件
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(MeshEvent.UPDATE_COMPLETED, this);
        this.mApplication.addEventListener(MeshEvent.ERROR, this);
        this.inflater = this.getLayoutInflater();
        this.adapter = new DeviceListAdapter();

        this.imgHeaderMenuLeft = (ImageView) this.findViewById(R.id.img_header_menu_left);
        groupsBottom = findViewById(R.id.groups_bottom);
        recyclerViewGroups = findViewById(R.id.recycler_view_groups);
        this.btnAddGroups = findViewById(R.id.btn_add_groups);
        this.groupingCompleted = findViewById(R.id.grouping_completed);
        this.groupingCompleted.setBackgroundColor(getResources().getColor(R.color.gray));
        this.btnLog = findViewById(R.id.btn_log);
        this.btnScan = (Button) this.findViewById(R.id.btn_scan);
        this.btnScan.setEnabled(false);
        this.btnScan.setBackgroundResource(R.color.gray);
        deviceListView = (GridView) this.findViewById(R.id.list_devices);
        deviceListView.setAdapter(this.adapter);
        this.updateList = new ArrayList<>();

        btnScan.setVisibility(View.GONE);
        btnLog.setVisibility(View.GONE);
        btnAddGroups.setVisibility(View.GONE);
        groupingCompleted.setVisibility(View.GONE);
        tvNumLights.setVisibility(View.GONE);

        currentGroupIndex = -1;
    }

    private void initData() {
        this.mApplication = (TelinkLightApplication) this.getApplication();
        nowLightList=new ArrayList<>();
        if(groups==null){
            groups=new ArrayList<>();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.updateList = null;
        if (timer != null)
            timer.cancel();
        canStartTimer = false;
        nextTime = 0;
        this.mApplication.removeEventListener(this);
        this.mHandler.removeCallbacksAndMessages(null);
        mDisposable.dispose();  //销毁时取消订阅.
    }

    public void openLoadingDialog(String content) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialogview, null);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);
        TextView tvContent = (TextView) v.findViewById(R.id.tvContent);
        tvContent.setText(content);

        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);

        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
                R.animator.load_animation);

        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = new Dialog(this,
                    R.style.FullHeightDialog);
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog.isShowing()) {
            loadDialog.setCancelable(true);
            loadDialog.setCanceledOnTouchOutside(false);
            loadDialog.setContentView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            loadDialog.show();
        }
    }

    public void closeDialog() {
        if (loadDialog != null) {
            loadDialog.dismiss();
        }
    }

    private static class DeviceItemHolder {
        public ImageView icon;
        public TextView txtName;
        public CheckBox selected;
    }

    final class DeviceListAdapter extends BaseAdapter {

        private List<DbLight> lights;

        public DeviceListAdapter() {

        }

        @Override
        public int getCount() {
            return this.lights == null ? 0 : this.lights.size();
        }

        @Override
        public DbLight getItem(int position) {
            return this.lights.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            DeviceScanningNewActivity.DeviceItemHolder holder;

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

            convertView.setTag(holder);


            DbLight light = this.getItem(position);

            holder.txtName.setText(light.getName());
            holder.icon.setImageResource(R.drawable.icon_light_on);
            holder.selected.setChecked(light.selected);

            if (light.hasGroup) {
//                holder.txtName.setVisibility(View.GONE);
//                holder.icon.setVisibility(View.GONE);
//                holder.selected.setVisibility(View.GONE);
                holder.txtName.setText(DBUtils.getGroupNameByID(light.getBelongGroupId()));
                holder.icon.setVisibility(View.VISIBLE);
                holder.selected.setVisibility(View.VISIBLE);
            } else {
                holder.txtName.setVisibility(View.VISIBLE);
                holder.icon.setVisibility(View.VISIBLE);
                if (grouping) {
                    holder.selected.setVisibility(View.VISIBLE);
                } else {
                    holder.selected.setVisibility(View.GONE);
                }
            }

            return convertView;
        }

        public void add(DbLight light) {

            if (this.lights == null)
                this.lights = new ArrayList<>();
            DBUtils.saveLight(light);
            this.lights.add(light);
        }

        public DbLight get(int meshAddress) {

            if (this.lights == null)
                return null;

            for (DbLight light : this.lights) {
                if (light.getMeshAddr() == meshAddress) {
                    return light;
                }
            }

            return null;
        }

        public List<DbLight> getLights() {
            return lights;
        }
    }

    /*********************************泰麟威后台数据部分*********************************************/

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
            case NotificationEvent.GET_GROUP:
//                this.onGetGroupEvent((NotificationEvent) event);
                break;
            case MeshEvent.ERROR:
                this.onMeshEvent((MeshEvent) event);
                break;
        }
    }

    private void onMeshEvent(MeshEvent event) {
        new AlertDialog.Builder(this).setMessage(R.string.restart_bluetooth).show();
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
                DbLight light = this.adapter.get(meshAddress);

                if (light == null) {
                    light = new DbLight();
                    light.setName(deviceInfo.meshName);
                    light.setMeshAddr(meshAddress);
                    light.textColor = this.getResources().getColor(
                            R.color.black);
                    light.setBelongGroupId(Long.valueOf(-1));
                    light.setMacAddr(deviceInfo.macAddress);
                    light.setMeshUUID(deviceInfo.meshUUID);
                    light.setProductUUID(deviceInfo.productUUID);
                    light.setSelected(false);
                    this.adapter.add(light);
                    this.adapter.notifyDataSetChanged();
                }

                if (canStartTimer) {
//                    startTimer();
                    canStartTimer = false;
                }

                preTime = TimeUtil.getNowSeconds();
//                this.startScan(1000);

                //扫描出灯就设置为非首次进入
                if (isFirtst) {
                    isFirtst = false;
                    SharedPreferencesHelper.putBoolean(DeviceScanningNewActivity.this, SplashActivity.IS_FIRST_LAUNCH, false);
                }

                this.startScan(200);
                break;
            case LightAdapter.STATUS_UPDATE_MESH_FAILURE:
                //加灯失败继续扫描
                this.startScan(200);
                break;

            case LightAdapter.STATUS_ERROR_N:
                this.onNError(event);
                break;
            case LightAdapter.STATUS_LOGIN:
                isLoginSuccess = true;
//                btnAddGroups.doneLoadingAnimation(R.color.black,
//                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_done_white_48dp));
                break;
            case LightAdapter.STATUS_LOGOUT:
                isLoginSuccess = false;
                break;
        }
    }

    private void onNError(final DeviceEvent event) {

        TelinkLightService.Instance().idleMode(true);
        TelinkLog.d("DeviceScanningActivity#onNError");

        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanningNewActivity.this);
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

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     *
     * @param event
     */
    private void onLeScanTimeout(LeScanEvent event) {
//        this.btnScan.setEnabled(true);
        this.btnScan.setBackgroundResource(R.color.colorPrimary);
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

    /**
     * 开始扫描
     */
    private void startScan(final int delay) {
        //添加进disposable，防止内存溢出.
        mDisposable.add(
                mRxPermission.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN).subscribe(granted -> {
                    if (granted) {
                        handleIfSupportBle();
                        TelinkLightService.Instance().idleMode(true);
                        mHandler.postDelayed(() -> {
                            if (mApplication.isEmptyMesh())
                                return;
                            Mesh mesh = mApplication.getMesh();
                            //扫描参数
                            LeScanParameters params = LeScanParameters.create();
                            params.setMeshName(mesh.factoryName);
                            params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME);
                            params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND);
                            params.setScanMode(true);
                            //                params.setScanMac("FF:FF:7A:68:6B:7F");
                            TelinkLightService.Instance().startScan(params);
                            openLoadingDialog(getString(R.string.loading));
                        }, delay);
                    } else {
                        // TODO: 2018/3/26 弹框提示为何需要此权限，点击确定后再次申请权限，点击取消退出.
                        AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceScanningNewActivity.this);
                        dialog.setMessage(getResources().getString(R.string.scan_tip));
                        dialog.setPositiveButton(R.string.btn_ok, (dialog1, which) -> startScan(0));
                        dialog.setNegativeButton(R.string.btn_cancel, (dialog12, which) -> System.exit(0));
                    }
                }));

    }

    public void handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(getApplicationContext())) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        if (!LeBluetooth.getInstance().isEnabled()) {
            LeBluetooth.getInstance().enable(getApplicationContext());
        }
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
            this.showToast(getString(R.string.much_lamp_tip));
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
                TelinkLightService.Instance().updateMesh(params);
            }
        }, 200);


    }
}
