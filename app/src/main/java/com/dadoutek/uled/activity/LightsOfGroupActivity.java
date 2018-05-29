package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.adapter.LightsOfGroupRecyclerViewAdapter;
import com.dadoutek.uled.intf.SwitchButtonOnCheckedChangeListener;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.util.DataManager;
import com.telink.bluetooth.TelinkLog;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.event.ErrorReportEvent;
import com.telink.bluetooth.event.MeshEvent;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.event.ServiceEvent;
import com.telink.bluetooth.light.ConnectionStatus;
import com.telink.bluetooth.light.ErrorReportInfo;
import com.telink.bluetooth.light.OnlineStatusNotificationParser;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by hejiajun on 2018/4/24.
 */

public class LightsOfGroupActivity extends TelinkBaseActivity implements EventListener<String> {
    @BindView(R.id.recycler_view_lights)
    RecyclerView recyclerViewLights;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private DbGroup group;
    private DataManager mDataManager;
    private TelinkLightApplication mApplication;
    private List<Integer> lightListAdress;
    private List<DbLight> lightList;
    private LightsOfGroupRecyclerViewAdapter adapter;
    private int positionCurrent;
    private DbLight currentLight;
    private static final int UPDATE_LIST = 0;
    private boolean canBeRefresh = true;

    @Override
    protected void onStart() {
        super.onStart();
        // 监听各种事件
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this);
        this.mApplication.addEventListener(NotificationEvent.ONLINE_STATUS, this);
        this.mApplication.addEventListener(NotificationEvent.GET_ALARM, this);
        this.mApplication.addEventListener(NotificationEvent.GET_DEVICE_STATE, this);
        this.mApplication.addEventListener(ServiceEvent.SERVICE_CONNECTED, this);
        this.mApplication.addEventListener(MeshEvent.OFFLINE, this);

        this.mApplication.addEventListener(ErrorReportEvent.ERROR_REPORT, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights_of_group);
        ButterKnife.bind(this);
        initToolbar();
        initParameter();
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.group_setting_header);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initParameter() {
        this.group = (DbGroup) this.getIntent().getExtras().get("group");
        this.mApplication = (TelinkLightApplication) this.getApplication();
        mDataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
        initView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mApplication.removeEventListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        this.mApplication.removeEventListener(this);
        canBeRefresh = false;
        this.mHandler.removeCallbacksAndMessages(null);
    }

    private void initData() {
        lightList=new ArrayList<>();
        if(group.getMeshAddr()==0xffff){
//            lightList = DBUtils.getAllLight();
            List<DbGroup> list=DBUtils.getGroupList();
            for(int j=0;j<list.size();j++){
                lightList.addAll(DBUtils.getLightByGroupID(list.get(j).getId()));
            }
        }else{
            lightList = DBUtils.getLightByGroupID(group.getId());
        }
    }


    private void initView() {
        toolbar.setTitle(group.getName());
        recyclerViewLights.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new LightsOfGroupRecyclerViewAdapter(this, lightList, onCheckedChangeListener);
        recyclerViewLights.setAdapter(adapter);
    }


    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case NotificationEvent.ONLINE_STATUS:
                this.onOnlineStatusNotify((NotificationEvent) event);
                break;
            case DeviceEvent.STATUS_CHANGED:
                this.onDeviceStatusChanged((DeviceEvent) event);
                break;
            case MeshEvent.OFFLINE:
//                this.onMeshOffline((MeshEvent) event);
                break;
            case ServiceEvent.SERVICE_CONNECTED:
//                this.onServiceConnected((ServiceEvent) event);
                break;
            case ServiceEvent.SERVICE_DISCONNECTED:
//                this.onServiceDisconnected((ServiceEvent) event);
                break;
            case NotificationEvent.GET_DEVICE_STATE:
//                onNotificationEvent((NotificationEvent) event);
                break;

            case ErrorReportEvent.ERROR_REPORT:
                ErrorReportInfo info = ((ErrorReportEvent) event).getArgs();
                TelinkLog.d("MainActivity#performed#ERROR_REPORT: " + " stateCode-" + info.stateCode
                        + " errorCode-" + info.errorCode
                        + " deviceId-" + info.deviceId);
                break;
        }
    }

    private void onDeviceStatusChanged(DeviceEvent event) {
    }

    /**
     * 处理{@link NotificationEvent#ONLINE_STATUS}事件
     */
    private synchronized void onOnlineStatusNotify(NotificationEvent event) {

        if (canBeRefresh) {
            canBeRefresh = false;
        } else {
            return;
        }

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

            if (currentLight == null) {
                return;
            }

            currentLight.status = notificationInfo.connectionStatus;

            if (currentLight.getMeshAddr() ==
                    TelinkLightApplication.getInstance().getConnectDevice().meshAddress) {
                currentLight.textColor = getResources().getColor(
                        R.color.primary);
            } else {
                currentLight.textColor = getResources().getColor(
                        R.color.black);
            }

            currentLight.updateIcon();
        }

        mHandler.obtainMessage(UPDATE_LIST).sendToTarget();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_LIST:
                    if (lightList.size() > 0 && positionCurrent < lightList.size()) {
                        lightList.set(positionCurrent, currentLight);
                        adapter.notifyItemChanged(positionCurrent);
                    }
                    break;
            }
        }
    };

    SwitchButtonOnCheckedChangeListener onCheckedChangeListener = new SwitchButtonOnCheckedChangeListener() {
        @Override
        public void OnCheckedChangeListener(View v, int position) {
            currentLight = lightList.get(position);
            positionCurrent = position;
            byte opcode = (byte) 0xD0;
            if (v.getId() == R.id.img_light) {
                canBeRefresh = true;
                if (currentLight.status == ConnectionStatus.OFF) {
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight.getMeshAddr(),
                            new byte[]{0x01, 0x00, 0x00});
                } else {
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight.getMeshAddr(),
                            new byte[]{0x00, 0x00, 0x00});
                }
            } else if (v.getId() == R.id.tv_setting) {
                Intent intent = new Intent(LightsOfGroupActivity.this, DeviceSettingActivity.class);
                intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight);
                intent.putExtra(Constant.GROUP_ARESS_KEY, group.getMeshAddr());
                intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK);
                startActivity(intent);
            }
        }
    };


}
