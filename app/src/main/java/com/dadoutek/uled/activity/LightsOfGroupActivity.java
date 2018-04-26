package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.adapter.LightsOfGroupRecyclerViewAdapter;
import com.dadoutek.uled.intf.SwitchButtonOnCheckedChangeListener;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
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

public class LightsOfGroupActivity extends TelinkBaseActivity implements View.OnClickListener, EventListener<String> {
    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.recycler_view_lights)
    RecyclerView recyclerViewLights;

    //组的mesh地址
    private int groupAddress;
    private Group group;
    private DataManager mDataManager;
    private TelinkLightApplication mApplication;
    private List<Integer> lightListAdress;
    private List<Light> lightList;
    private Lights lights = Lights.getInstance();
    private LightsOfGroupRecyclerViewAdapter adapter;
    private int positionCurrent;
    private Light currentLight;
    private static final int UPDATE_LIST = 0;
    private boolean canBeRefresh=true;

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
        initParameter();
    }

    private void initParameter() {
        this.groupAddress = this.getIntent().getIntExtra("groupAddress", 0);
        this.mApplication = (TelinkLightApplication) this.getApplication();
        mDataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);
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
        this.mApplication.removeEventListener(this);
    }

    private void initData() {
        group = mDataManager.getGroup(groupAddress, this);
        lightListAdress = group.containsLightList;
        lightList = new ArrayList<>();
        getLights();
    }

    private void getLights() {

        if(groupAddress==0xFFFF){
            for (int i = 0; i < lightListAdress.size(); i++) {
                for (int j = 0; j < lights.size(); j++) {
                    Light light = lights.get(j);
                    if (light != null) {
                        lightList.add(light);
                    }
                }
            }
        }else{
            if (lightListAdress == null || lightListAdress.size() == 0) {
                ToastUtils.showLong("当前组没有灯！");
                finish();
            }
        }

        for (int i = 0; i < lightListAdress.size(); i++) {
            for (int j = 0; j < lights.size(); j++) {
                Light light = lights.get(j);
                if (light != null && light.meshAddress == lightListAdress.get(i)) {
                    lightList.add(light);
                }
            }
        }
    }

    private void initView() {
        txtHeaderTitle.setText(group.name);
        imgHeaderMenuLeft.setOnClickListener(this);
        recyclerViewLights.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new LightsOfGroupRecyclerViewAdapter(this, lightList, onCheckedChangeListener);
        recyclerViewLights.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
        }
    }

    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case NotificationEvent.ONLINE_STATUS:
                this.onOnlineStatusNotify((NotificationEvent) event);
                break;
            case DeviceEvent.STATUS_CHANGED:
//                this.onDeviceStatusChanged((DeviceEvent) event);
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

    /**
     * 处理{@link NotificationEvent#ONLINE_STATUS}事件
     */
    private synchronized void onOnlineStatusNotify(NotificationEvent event) {

        TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().getId());
        List<OnlineStatusNotificationParser.DeviceNotificationInfo> notificationInfoList;
        //noinspection unchecked
        notificationInfoList = (List<OnlineStatusNotificationParser.DeviceNotificationInfo>) event.parse();

        if (notificationInfoList == null || notificationInfoList.size() <= 0)
            return;

        /*if (this.deviceFragment != null) {
            this.deviceFragment.onNotify(notificationInfoList);
        }*/

        if(canBeRefresh){
            canBeRefresh=false;
        }else{
            return;
        }

        for (OnlineStatusNotificationParser.DeviceNotificationInfo notificationInfo : notificationInfoList) {

            int meshAddress = notificationInfo.meshAddress;
            int brightness = notificationInfo.brightness;

            if (currentLight == null) {
                return;
            }

            currentLight.status = notificationInfo.connectionStatus;

            if (meshAddress == currentLight.meshAddress) {
                currentLight.textColor = this.getResources().getColor(
                        R.color.theme_positive_color);
            } else {
                currentLight.textColor = this.getResources().getColor(
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
                    lightList.set(positionCurrent,currentLight);
                    adapter.notifyItemChanged(positionCurrent);
                    break;
            }
        }
    };

    SwitchButtonOnCheckedChangeListener onCheckedChangeListener = new SwitchButtonOnCheckedChangeListener() {
        @Override
        public void OnCheckedChangeListener(View v, int position) {
            currentLight=lightList.get(position);
            positionCurrent=position;
            byte opcode = (byte) 0xD0;
            if(v.getId()==R.id.img_light){
                canBeRefresh=true;
                if(currentLight.status== ConnectionStatus.OFF){
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight.meshAddress,
                        new byte[]{0x01, 0x00, 0x00});
                }else{
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, currentLight.meshAddress,
                        new byte[]{0x00, 0x00, 0x00});
                }
            }else if(v.getId()==R.id.tv_setting){
                Intent intent=new Intent(LightsOfGroupActivity.this,DeviceSettingActivity.class);
                intent.putExtra(Constant.LIGHT_ARESS_KEY,currentLight.meshAddress);
                intent.putExtra(Constant.LIGHT_REFRESH_KEY,Constant.LIGHT_REFRESH_KEY_OK);
                startActivity(intent);
            }
        }
    };


}
