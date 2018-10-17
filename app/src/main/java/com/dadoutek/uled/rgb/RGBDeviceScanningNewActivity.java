package com.dadoutek.uled.rgb;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.communicate.Commander;
import com.dadoutek.uled.group.GroupsRecyclerViewAdapter;
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener;
import com.dadoutek.uled.intf.SyncCallback;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DeviceType;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.network.NetworkFactory;
import com.dadoutek.uled.othersview.LogInfoActivity;
import com.dadoutek.uled.othersview.MainActivity;
import com.dadoutek.uled.othersview.SplashActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.util.DialogUtils;
import com.dadoutek.uled.util.NetWorkUtils;
import com.dadoutek.uled.util.OtherUtils;
import com.dadoutek.uled.util.StringUtils;
import com.dadoutek.uled.util.SyncDataPutOrGetUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.telink.bluetooth.LeBluetooth;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.ErrorReportEvent;
import com.telink.bluetooth.event.LeScanEvent;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.ErrorReportInfo;
import com.telink.bluetooth.light.LeAutoConnectParameters;
import com.telink.bluetooth.light.LeRefreshNotifyParameters;
import com.telink.bluetooth.light.LeScanParameters;
import com.telink.bluetooth.light.LeUpdateParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.NotificationInfo;
import com.telink.bluetooth.light.Parameters;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by hejiajun on 2018/5/21.
 */

public class RGBDeviceScanningNewActivity extends TelinkMeshErrorDealActivity
        implements AdapterView.OnItemClickListener, EventListener<String>, Toolbar.OnMenuItemClickListener {
    private static final int MAX_RETRY_COUNT = 5;   //update mesh failed的重试次数设置为5次
    @BindView(R.id.toolbar)
    Toolbar toolbar;
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
    @BindView(R.id.scanPb)
    MaterialProgressBar scanPb;

    private TelinkLightApplication mApplication;
    private RxPermissions mRxPermission;
    private static final String TAG = RGBDeviceScanningNewActivity.class.getSimpleName();
    private static final int SCAN_TIMEOUT_SECOND = 10;
    //防止内存泄漏
    CompositeDisposable mDisposable = new CompositeDisposable();
    private Dialog loadDialog;
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

    private Disposable mTimer;
    private int mRetryCount = 0;

    //当前所选组index
    private int currentGroupIndex = -1;

    private List<DbLight> updateList;

    private ArrayList<Integer> indexList = new ArrayList<>();

    //对一个灯重复分组时记录上一次分组
    private int originalGroupID = -1;

    private Disposable mGroupingDisposable;

    //灯的mesh地址
    private int dstAddress;
    private Disposable mConnectTimer;
    private SparseArray<Disposable> mBlinkDisposables = new SparseArray<>();
    private boolean isSelectAll = false;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DbLight light = this.adapter.getItem(position);
        light.selected = !light.selected;
        DeviceItemHolder holder = (DeviceItemHolder) view.getTag();
        holder.selected.setChecked(light.selected);

        if (light.selected) {
            this.updateList.add(light);
            nowLightList.get(position).selected = true;

            btnAddGroups.setText(R.string.set_group);

            if (hasGroup()) {
                startBlink(light);
            } else {
                ToastUtils.showLong(R.string.tip_add_group);
            }
        } else {
            nowLightList.get(position).selected = false;
            this.updateList.remove(light);
            stopBlink(light);
            if ((!isSelectLight()) && isAllLightsGrouped()) {
                btnAddGroups.setText(R.string.complete);
            }
        }
    }

    private void isSelectAll() {
        if (isSelectAll) {
            for (int j = 0; j < nowLightList.size(); j++) {
                this.updateList.add(nowLightList.get(j));
                nowLightList.get(j).selected = true;

                btnAddGroups.setText(R.string.set_group);

                if (hasGroup()) {
                    startBlink(nowLightList.get(j));
                } else {
                    ToastUtils.showLong(R.string.tip_add_group);
                }
            }

            this.adapter.notifyDataSetChanged();
        } else {
            for (int j = 0; j < nowLightList.size(); j++) {
                this.updateList.remove(nowLightList.get(j));
                nowLightList.get(j).selected = false;
                stopBlink(nowLightList.get(j));
                if ((!isSelectLight()) && isAllLightsGrouped()) {
                    btnAddGroups.setText(R.string.complete);
                }
            }

            this.adapter.notifyDataSetChanged();
        }
    }

    private boolean hasGroup() {
        if (groups.size() == 0) {
            groups = new ArrayList<>();
            return false;
        } else {
            return true;
        }

    }

    /**
     * 让灯开始闪烁
     */
    private void startBlink(DbLight light) {
//        int dstAddress = light.getMeshAddr();
        DbGroup group;
        DbGroup groupOfTheLight = DBUtils.INSTANCE.getGroupByID(light.getBelongGroupId());
        if (groupOfTheLight == null)
            group = groups.get(0);
        else
            group = groupOfTheLight;
        int groupAddress = group.getMeshAddr();
        Log.d("Saw", "startBlink groupAddresss = " + groupAddress);
        int dstAddress = light.getMeshAddr();
        byte opcode = (byte) Opcode.SET_GROUP;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};
        params[0] = 0x01;

        if (mBlinkDisposables.get(dstAddress) != null) {
            mBlinkDisposables.get(dstAddress).dispose();
        }

        //每隔1s发一次，就是为了让灯一直闪.
        mBlinkDisposables.put(dstAddress, Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
                }));
    }

    private void stopBlink(DbLight light) {
        Disposable disposable = mBlinkDisposables.get(light.getMeshAddr());
        disposable.dispose();
    }

    //扫描失败处理方法
    private void scanFail() {
//        btnAddGroups.setVisibility(View.VISIBLE);
//        groupingCompleted.setVisibility(View.GONE);
//        btnAddGroups.setText(R.string.rescan);
//        btnAddGroups.setOnClickListener(v -> {
//            startScan(1000);
//            btnAddGroups.setVisibility(View.GONE);
//        });
//        scanPb.setVisibility(View.GONE);
        showToast(getString(R.string.scan_end));
        doFinish();
    }

    private void startTimer() {
        stopTimer();
        // 防止onLescanTimeout不调用，导致UI卡住的问题。设为正常超时时间的2倍
        if (mTimer != null && !mTimer.isDisposed())
            mTimer.dispose();
        mTimer = Observable.timer(SCAN_TIMEOUT_SECOND * 2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (mRetryCount < MAX_RETRY_COUNT) {
                        mRetryCount++;
                        Log.d("ScanningTest", "rxjava timer timeout , retry count = " + mRetryCount);
                        startScan(1000);
                    } else {
                        Log.d("ScanningTest", "rxjava timer timeout , do not retry");
                        onLeScanTimeout();

                    }
//
                });

    }


    private void stopTimer() {
        if (mTimer != null && !mTimer.isDisposed()) {
//            Log.d("ScanningTest", "cancel timer");
            mTimer.dispose();
        }
    }


    private Disposable createConnectTimeout() {
        return Observable.timer(60, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Toast.makeText(mApplication, getString(R.string.connect_fail), Toast.LENGTH_SHORT).show();
                    hideLoadingDialog();
                    mConnectTimer = null;
                });
    }

    //处理扫描成功后
    private void scanSuccess() {
        //更新Title
        toolbar.setTitle(getString(R.string.title_scanned_lights_num, adapter.getCount()));

        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowLightList != null && nowLightList.size() > 0) {
            nowLightList.clear();
        }
        if (nowLightList != null)
            nowLightList.addAll(adapter.getLights());

        scanPb.setVisibility(View.GONE);

        //先连接灯。
        autoConnect();
        //倒计时10s，出问题了就超时。
        mConnectTimer = createConnectTimeout();


        btnAddGroups.setVisibility(View.VISIBLE);
        btnAddGroups.setText(R.string.start_group_bt);


        btnAddGroups.setOnClickListener(v -> {
            if (isLoginSuccess) {
                //进入分组
                startGrouping();
            } else if (mConnectTimer == null) {
                autoConnect();
                mConnectTimer = createConnectTimeout();
            } else {    //正在连接中
                showLoadingDialog(getResources().getString(R.string.connecting_tip));

            }
        });

    }

    private void doFinish() {
    if(updateList!=null&&updateList.size()>0){
            checkNetworkAndSync();
        }
        TelinkLightService.Instance().idleMode(true);
        this.mApplication.removeEventListener(this);
        this.updateList = null;
        mDisposable.dispose();  //销毁时取消订阅.
        if (mTimer != null)
            mTimer.dispose();
        if (mGroupingDisposable != null)
            mGroupingDisposable.dispose();
        if (mConnectTimer != null)
            mConnectTimer.dispose();

        for (int i = 0; i < mBlinkDisposables.size(); i++) {
            Disposable disposable = mBlinkDisposables.get(i);
            if (disposable != null)
                disposable.dispose();
        }

        if (ActivityUtils.isActivityExistsInStack(MainActivity.class))
            ActivityUtils.finishToActivity(MainActivity.class, false, true);
        else {
            ActivityUtils.startActivity(MainActivity.class);
            finish();
        }
    }

    /**
     * 开始分组
     */
    private void startGrouping() {
        LeBluetooth.getInstance().stopScan();

        //初始化分组页面
        changeGroupView();

        //完成分组跳转
        changOtherView();

        //确定当前分组
        sureGroupingEvent();
    }

    private void sureGroupingEvent() {
        btnAddGroups.setText(R.string.sure_group);
        btnAddGroups.setOnClickListener(v -> {
            if (isAllLightsGrouped() && !isSelectLight()) {
                doFinish();
            } else {
                sureGroups();
            }
        });
    }

    private void changOtherView() {
        groupingCompleted.setOnClickListener(v -> {
            //判定是否还有灯没有分组，如果没有允许跳转到下一个页面
            if (isAllLightsGrouped()) {//所有灯都有分组可以跳转
                showToast(getString(R.string.group_completed));
                //页面跳转前进行分组数据保存

                TelinkLightService.Instance().idleMode(true);
                //目前测试调到主页
                doFinish();
            } else {
                showToast(getString(R.string.have_lamp_no_group_tip));
            }
        });
    }

    /**
     * 有无被选中的用来分组的灯
     *
     * @return true: 选中了       false:没选中
     */
    private boolean isSelectLight() {
        return getCurrentSelectLights().size() > 0;

    }

    private void sureGroups() {
        if (isSelectLight()) {
            //进行分组操作
            //获取当前选择的分组
            DbGroup group = getCurrentGroup();
            if (group != null) {
                if (group.getMeshAddr() == 0xffff) {
                    ToastUtils.showLong(R.string.tip_add_gp);
                    return;
                }
                //获取当前勾选灯的列表
                List<DbLight> selectLights = getCurrentSelectLights();

                showLoadingDialog(getResources().getString(R.string.grouping_wait_tip,
                        selectLights.size() + ""));
                //将灯列表的灯循环设置分组
                setGroups(group, selectLights);
            }

        } else {
            showToast(getString(R.string.selected_lamp_tip));
        }
    }


    private void setGroupOneByOne(DbGroup dbGroup, List<DbLight> selectLights, int index) {
        DbLight dbLight = selectLights.get(index);
        int lightMeshAddr = dbLight.getMeshAddr();
        Commander.INSTANCE.addGroup(lightMeshAddr, dbGroup.getMeshAddr(), new Function0<Unit>() {
            @Override
            public Unit invoke() {
                updateGroupResult(dbLight, dbGroup);
                if (index + 1 > selectLights.size() - 1)
                    completeGroup(selectLights);
                else
                    setGroupOneByOne(dbGroup, selectLights, index + 1);
                return null;
            }
        }, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                dbLight.setName("");
                ToastUtils.showLong(R.string.group_fail_tip);
                updateGroupResult(dbLight, dbGroup);
                if (index + 1 > selectLights.size() - 1)
                    completeGroup(selectLights);
                else
                    setGroupOneByOne(dbGroup, selectLights, index + 1);
                return null;
            }
        });

    }

    private void completeGroup(List<DbLight> selectLights) {
        //取消分组成功的勾选的灯
        for (int i = 0; i < selectLights.size(); i++) {
            DbLight light = selectLights.get(i);
            light.selected = false;
        }
        adapter.notifyDataSetChanged();
        hideLoadingDialog();
        if (isAllLightsGrouped()) {
            btnAddGroups.setText(R.string.complete);
        }
    }

    private void setGroups(DbGroup group, List<DbLight> selectLights) {
        if (group == null) {
            Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSelectAll) {
            toolbar.getMenu().findItem(R.id.menu_select_all).setTitle(getString(R.string.select_all));
            isSelectAll = false;
        }

        for (int i = 0; i < selectLights.size(); i++) {
            //让选中的灯停下来别再发闪的命令了。
            stopBlink(selectLights.get(i));
        }

        setGroupOneByOne(group, selectLights, 0);
    }

    private void updateGroupResult(DbLight light, DbGroup group) {
        for (int i = 0; i < nowLightList.size(); i++) {
            if (light.getMeshAddr() == nowLightList.get(i).getMeshAddr()) {
                if (!light.getName().isEmpty()) {
                    nowLightList.get(i).hasGroup = true;
                    nowLightList.get(i).setBelongGroupId(group.getId());
                    nowLightList.get(i).setName(group.getName());
                    DBUtils.INSTANCE.updateLight(light);
                } else {
                    nowLightList.get(i).hasGroup = false;
                    nowLightList.get(i).setName(getString(R.string.grouping_fail));
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if (grouping) {
            for (int i = 0; i < getCurrentSelectLights().size(); i++) {
                //让选中的灯停下来别再发闪的命令了。
                stopBlink(getCurrentSelectLights().get(i));
            }
            doFinish();
        } else {
            new AlertDialog.Builder(this)
                    .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
//                        startActivity(new Intent(NormalDeviceScanningNewActivity.this, MainActivity.class));
//                        finish();
                        doFinish();
                    })
                    .setNegativeButton(R.string.btn_cancel, ((dialog, which) -> {
                    }))
                    .setMessage(R.string.exit_tips_in_scanning)
                    .show();
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
            if (groups.size() > 1) {
                Toast.makeText(this, R.string.please_select_group, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.tip_add_gp, Toast.LENGTH_SHORT).show();
            }
            return null;
        }
        return groups.get(currentGroupIndex);
    }

    /**
     * 是否所有灯都分了组
     *
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private boolean isAllLightsGrouped() {
        for (int j = 0; j < nowLightList.size(); j++) {
            if (DBUtils.INSTANCE.getGroupByID(nowLightList.get(j).getBelongGroupId()).getMeshAddr() == 0xffff) {
                return false;
            }
        }
        return true;
    }

    //分组页面调整
    private void changeGroupView() {
        grouping = true;
        toolbar.inflateMenu(R.menu.menu_grouping_select_all);
        toolbar.setOnMenuItemClickListener(this);
        deviceListView.setOnItemClickListener(this);
        deviceListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        btnAddGroups.setVisibility(View.VISIBLE);
        groupsBottom.setVisibility(View.VISIBLE);
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewGroups.setLayoutManager(layoutmanager);
        groupsRecyclerViewAdapter = new GroupsRecyclerViewAdapter(groups, onRecyclerviewItemClickListener);
        recyclerViewGroups.setAdapter(groupsRecyclerViewAdapter);

        disableEventListenerInGrouping();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select_all:
                if (isSelectAll) {
                    isSelectAll = false;
                    item.setTitle(R.string.select_all);
                } else {
                    isSelectAll = true;
                    item.setTitle(R.string.cancel);
                }
                isSelectAll();
                break;
        }
        return false;
    }

    private void disableEventListenerInGrouping() {
        this.mApplication.removeEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication.removeEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
    }

    @OnClick(R.id.add_group_layout)
    public void onViewClicked() {
        addNewGroup();
    }

    private void addNewGroup() {
        final EditText textGp = new EditText(this);
        StringUtils.initEditTextFilter(textGp);
        new AlertDialog.Builder(RGBDeviceScanningNewActivity.this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(R.string.btn_sure), (dialog, which) -> {
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.getText().toString().trim())) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check));
                    } else {
                        //往DB里添加组数据
                        DBUtils.INSTANCE.addNewGroup(textGp.getText().toString().trim(), groups, this);
                        refreshView();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> {
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
            SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                    Constant.DEFAULT_GROUP_ID, currentGroupIndex);
        }
    };

    private void updateData(int position, boolean checkStateChange) {
        groups.get(position).checked = checkStateChange;
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private void autoConnect() {
        if (TelinkLightService.Instance() != null) {
            if (TelinkLightService.Instance().getMode() != LightAdapter.MODE_AUTO_CONNECT_MESH) {
                showLoadingDialog(getResources().getString(R.string.connecting_tip));
                TelinkLightService.Instance().idleMode(true);

                String account = DBUtils.INSTANCE.getLastUser().getAccount();

                //自动重连参数
                LeAutoConnectParameters connectParams = Parameters.createAutoConnectParameters();
                connectParams.setMeshName(account);
                connectParams.setPassword(NetworkFactory.md5(
                        NetworkFactory.md5(account) + account).substring(0, 16));
                connectParams.autoEnableNotification(true);

                //连接，如断开会自动重连
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
        this.btnScan.setOnClickListener(this.clickListener);
        this.btnLog.setOnClickListener(this.clickListener);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == btnScan) {
                doFinish();
                //stopScanAndUpdateMesh();
            } else if (v.getId() == R.id.btn_log) {
                startActivity(new Intent(RGBDeviceScanningNewActivity.this, LogInfoActivity.class));
            }
        }
    };

    private void initView() {
        initToolbar();
        //监听事件
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN, this);
        this.mApplication.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this);
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(MeshEvent.UPDATE_COMPLETED, this);
        this.mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this);
        this.mApplication.addEventListener(NotificationEvent.GET_GROUP, this);
        this.inflater = this.getLayoutInflater();
        this.adapter = new DeviceListAdapter();

        groupsBottom = findViewById(R.id.groups_bottom);
        recyclerViewGroups = findViewById(R.id.recycler_view_groups);
        this.btnAddGroups = findViewById(R.id.btn_add_groups);
        this.groupingCompleted = findViewById(R.id.grouping_completed);
        this.groupingCompleted.setBackgroundColor(getResources().getColor(R.color.gray));
        this.btnLog = findViewById(R.id.btn_log);
        this.btnScan = (Button) this.findViewById(R.id.btn_scan);
        this.btnScan.setEnabled(false);
        this.btnScan.setBackgroundResource(R.color.gray);
        deviceListView = this.findViewById(R.id.list_devices);
        deviceListView.setAdapter(this.adapter);
        this.updateList = new ArrayList<>();

        btnScan.setVisibility(View.GONE);
        btnLog.setVisibility(View.GONE);
        btnAddGroups.setVisibility(View.GONE);
        groupingCompleted.setVisibility(View.GONE);

        lightNumLayout.setVisibility(View.GONE);
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.scanning);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
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

    private void initData() {
        this.mApplication = (TelinkLightApplication) this.getApplication();
        nowLightList = new ArrayList<>();
        if (groups == null) {
            groups = new ArrayList<>();
            List<DbGroup> list=DBUtils.INSTANCE.getGroupList();
           for(int i=0;i<list.size();i++){
               if(OtherUtils.isRGBGroup(list.get(i)) || list.get(i).getMeshAddr()==0xffff ||OtherUtils.groupIsEmpty(list.get(i))){
                   groups.add(list.get(i));
               }
           }
        }

        if (groups.size() > 1) {
            for (int i = 0; i < groups.size(); i++) {
                if (i == groups.size() - 1) {
                    groups.get(i).checked = true;
                    currentGroupIndex = i;
                    SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                            Constant.DEFAULT_GROUP_ID, currentGroupIndex);
                } else {
                    groups.get(i).checked = false;
                }
            }
        } else {
            currentGroupIndex = -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null) {
            mApplication.startLightService(TelinkLightService.class);
        }
    }

    // 如果没有网络，则弹出网络设置对话框
    public void checkNetworkAndSync() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            SyncDataPutOrGetUtils.Companion.syncPutDataStart(this, syncCallback);
        }
    }

    SyncCallback syncCallback = new SyncCallback() {

        @Override
        public void start() {
//            showLoadingDialog(getString(R.string.tip_start_sync));
//            ToastUtils.showShort(R.string.uploading_data);
        }

        @Override
        public void complete() {
//            ToastUtils.showShort(R.string.upload_data_success);
//            hideLoadingDialog();
        }

        @Override
        public void error(String msg) {
            ToastUtils.showLong(R.string.upload_data_failed);
//            hideLoadingDialog();
        }

    };


    @Override
    protected void onLocationEnable() {

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

            RGBDeviceScanningNewActivity.DeviceItemHolder holder;

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

            holder.txtName.setText(R.string.not_grouped);
            holder.icon.setImageResource(R.drawable.icon_light_on);
            holder.selected.setChecked(light.selected);

            if (light.hasGroup) {
                holder.txtName.setText(getDeviceName(light));
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
            DBUtils.INSTANCE.saveLight(light, false);
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

    private String getDeviceName(DbLight light) {
        if (light.getName().isEmpty()) {
            return DBUtils.INSTANCE.getGroupNameByID(light.getBelongGroupId());
        } else {
            return light.getName();
        }
    }

    /*********************************泰凌微后台数据部分*********************************************/

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
                stopTimer();
                this.onLeScanTimeout();
                break;
            case DeviceEvent.STATUS_CHANGED:
                this.onDeviceStatusChanged((DeviceEvent) event);
                break;
            case NotificationEvent.GET_GROUP:
                this.onGetGroupEvent((NotificationEvent) event);
                break;
            case MeshEvent.ERROR:
                this.onMeshEvent((MeshEvent) event);
                break;
            case ErrorReportEvent.ERROR_REPORT:
                ErrorReportInfo info = ((ErrorReportEvent) event).getArgs();
                onErrorReport(info);
                break;

        }
    }


    private boolean groupingSuccess = false;
    private DbLight groupingLight;
    private DbGroup groupingGroup;

    private void getDeviceGroup(DbLight light) {
        byte opcode = (byte) 0xDD;
        int dstAddress = light.getMeshAddr();
        byte[] params = new byte[]{0x08, 0x01};

        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
        TelinkLightService.Instance().updateNotification();
    }

    private void onErrorReport(ErrorReportInfo info) {
//        retryConnect()
        LogUtils.d("onErrorReport type = " + info.stateCode + "error code = " + info.errorCode);
    }


    private void onGetGroupEvent(NotificationEvent event) {
        NotificationEvent e = event;
        NotificationInfo info = e.getArgs();

        int srcAddress = info.src & 0xFF;
        byte[] params = info.params;

        if (groupingLight == null || groupingGroup == null) {
            return;
        }

        if (srcAddress != groupingLight.getMeshAddr()) {
            return;
        }

        int groupAddress;
        int len = params.length;

        for (int j = 0; j < len; j++) {

            groupAddress = params[j];

            if (groupAddress == 0x00 || groupAddress == 0xFF)
                break;

            groupAddress = groupAddress | 0x8000;

            if (groupingGroup.getMeshAddr() == groupAddress) {
                LogUtils.d(String.format("grouping success, groupAddr = %x groupingLight.meshAddr = %x", groupAddress, groupingLight.getMeshAddr()));
                groupingSuccess = true;
            }
        }
    }

    private void onMeshEvent(MeshEvent event) {
        new AlertDialog.Builder(this).setMessage(R.string.restart_bluetooth).show();
    }

    private void onNError(final DeviceEvent event) {

        TelinkLightService.Instance().idleMode(true);
        TelinkLog.d("DeviceScanningActivity#onNError");
        onLeScanTimeout();
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private void onLeScanTimeout() {
//        TelinkLightService.Instance()
        LeBluetooth.getInstance().stopScan();
        TelinkLightService.Instance().idleMode(true);
        this.btnScan.setBackgroundResource(R.color.primary);
        if (adapter.getCount() > 0) {//表示目前已经搜到了至少有一个设备
            scanSuccess();
        } else {
            scanFail();
        }
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
                        startTimer();
                        if (grouping) {
//                            Toast.makeText(this, "Grouping", Toast.LENGTH_SHORT).show();
                            LogUtils.d("Grouping");
                            return;
                        }
                        handleIfSupportBle();
                        TelinkLightService.Instance().idleMode(true);
                        if (mApplication.isEmptyMesh()) {
//                            Toast.makeText(this, "Empty Mesh", Toast.LENGTH_SHORT).show();
                            LogUtils.d("Empty Mesh");
                            return;
                        }
                        Mesh mesh = mApplication.getMesh();
                        //扫描参数
                        LeScanParameters params = LeScanParameters.create();
                        params.setMeshName(mesh.getFactoryName());
                        params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME);
                        params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND);
                        params.setScanMode(true);
                        scanPb.setVisibility(View.VISIBLE);
                        mDisposable.add(Observable.timer(delay, TimeUnit.MILLISECONDS, Schedulers.io())
                                .subscribe(aLong -> {
                                    TelinkLightService.Instance().startScan(params);
                                }));

                    } else {
                        DialogUtils.INSTANCE.showNoBlePermissionDialog(this, () -> {
                            startScan(0);
                            return null;
                        }, () -> {
                            doFinish();
                            return null;
                        });
                    }
                }));

    }

    public void handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(getApplicationContext())) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show();
            doFinish();
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
        final int meshAddress = mesh.generateMeshAddr();

        if (meshAddress == -1) {
            ToastUtils.showLong(getString(R.string.much_lamp_tip));
            if(adapter.getLights()!=null && adapter.getLights().size()>0){
                stopTimer();
                onLeScanTimeout();
                return;
            }else{
                doFinish();
            }
            return;
        }


        DeviceInfo deviceInfo = event.getArgs();

//        Log.d(TAG, "onDeviceStatusChanged_onLeScan: " + deviceInfo.meshAddress + "" +
//                "------" + deviceInfo.macAddress);
        if (checkIsLight(deviceInfo.productUUID) && deviceInfo.productUUID==Constant.RGB_UUID) {
            mDisposable.add(Observable.timer(200, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe(aLong -> {
                        //更新参数
                        deviceInfo.meshAddress = meshAddress;
                        String account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                                Constant.DB_NAME_KEY, "dadou");
                        LeUpdateParameters params = Parameters.createUpdateParameters();
                        params.setOldMeshName(mesh.getFactoryName());
                        params.setOldPassword(mesh.getFactoryPassword());
                        params.setNewMeshName(mesh.getName());
                        params.setNewPassword(NetworkFactory.md5(
                                NetworkFactory.md5(mesh.getPassword()) + account).substring(0, 16));
                        params.setUpdateDeviceList(deviceInfo);
                        TelinkLightService.Instance().updateMesh(params);
                    }));
        }
    }

    private boolean checkIsLight(int productUUID) {
        if (productUUID == DeviceType.NORMAL_SWITCH ||
                productUUID == DeviceType.NORMAL_SWITCH2 ||
                productUUID == DeviceType.SCENE_SWITCH ||
                productUUID == DeviceType.SENSOR
                ) {
            return false;
        } else {
            return true;
        }
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

//                Log.d(TAG, "onDeviceStatusChanged: " + deviceInfo1.macAddress + "-----" + deviceInfo1.meshAddress);

                new Thread(() -> RGBDeviceScanningNewActivity.this.mApplication.getMesh().saveOrUpdate(RGBDeviceScanningNewActivity.this)).start();

                if (checkIsLight(deviceInfo1.productUUID) && deviceInfo1.productUUID==Constant.RGB_UUID) {
                    int meshAddress = deviceInfo.meshAddress & 0xFF;
                    DbLight light = this.adapter.get(meshAddress);


                    if (light == null) {
                        light = new DbLight();
                        light.setName(deviceInfo.meshName);
                        light.setMeshAddr(meshAddress);
                        light.textColor = this.getResources().getColor(
                                R.color.black);
                        light.setBelongGroupId(-1L);
                        light.setMacAddr(deviceInfo.macAddress);
                        light.setMeshUUID(deviceInfo.meshUUID);
                        light.setProductUUID(deviceInfo.productUUID);
                        light.setSelected(false);
                        this.adapter.add(light);
                        this.adapter.notifyDataSetChanged();
                    }
                }

                //扫描出灯就设置为非首次进入
                if (isFirtst) {
                    isFirtst = false;
                    SharedPreferencesHelper.putBoolean(RGBDeviceScanningNewActivity.this, SplashActivity.IS_FIRST_LAUNCH, false);
                }
                toolbar.setTitle(getString(R.string.title_scanning_lights_num, adapter.getCount()));

                Log.d("ScanningTest", "update mesh success");
                mRetryCount = 0;
                this.startScan(0);
                break;
            case LightAdapter.STATUS_UPDATE_MESH_FAILURE:
                //加灯失败继续扫描
                if (mRetryCount < MAX_RETRY_COUNT) {
                    mRetryCount++;
                    Log.d("ScanningTest", "update mesh failed , retry count = " + mRetryCount);
                    stopTimer();
                    this.startScan(0);
                } else {
                    Log.d("ScanningTest", "update mesh failed , do not retry");
                }
                break;

            case LightAdapter.STATUS_ERROR_N:
                this.onNError(event);
                break;
            case LightAdapter.STATUS_LOGIN:
                Log.d("ScanningTest", "mConnectTimer = " + mConnectTimer);
                if (mConnectTimer != null && !mConnectTimer.isDisposed()) {
                    Log.d("ScanningTest", " !mConnectTimer.isDisposed() = " + !mConnectTimer.isDisposed());
                    mConnectTimer.dispose();
                    isLoginSuccess = true;
                    //进入分组
                    hideLoadingDialog();
                    startGrouping();
                }
                break;
            case LightAdapter.STATUS_LOGOUT:
                isLoginSuccess = false;
                break;
        }
    }
}
