package com.dadoutek.uled.gateway;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.gateway.bean.GwTagBean;
import com.dadoutek.uled.gateway.bean.GwTasksBean;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.HttpModel.GwModel;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.network.GwGattBody;
import com.dadoutek.uled.network.NetworkObserver;
import com.dadoutek.uled.switches.SelectSceneListActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.TmtUtils;
import com.telink.TelinkApplication;
import com.telink.bluetooth.event.DeviceEvent;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.util.Event;
import com.telink.util.EventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.qqtheme.framework.picker.DateTimePicker;
import cn.qqtheme.framework.picker.TimePicker;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * 设置网关时间与场景传递进配config界面
 * task任务
 */
public class GwChoseTimeActivity extends TelinkBaseActivity implements EventListener<String> {
    private TextView toolbarTv;
    private Unbinder unbinder;
    private TextView toolbarCancel;
    private TextView timerTitle;
    private TextView toolbarConfirm;
    private TextView timerScene;
    private RelativeLayout timerLy;
    private LinearLayout wheelPickerLy;
    private int requestCodes = 1000;
    private int hourTime = 03;
    private int minuteTime = 15;
    private DbScene scene;
    private GwTasksBean tasksBean;
    ArrayList<GwTasksBean> data = new ArrayList();
    private Disposable disposableTimer;
    private int sendCount = 0;
    private byte[] labHeadPar;
    private GwTagBean gwTagBean;
    private byte opcodeHead;
    private Disposable disposableHeadTimer;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_way_chose_time2);
        unbinder = ButterKnife.bind(this);
        initView();
        initData();
        initLisenter();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initLisenter() {
        toolbarCancel.setOnClickListener(v -> finish());
        toolbarConfirm.setOnClickListener(v -> {
            if (scene == null)
                Toast.makeText(getApplicationContext(), getString(R.string.please_select_scene),
                        Toast.LENGTH_SHORT).show();
            else {
                if (isTimeHave()) {//如果已有该时间
                    if (tasksBean.getStartHour() == hourTime && tasksBean.getStartMins() == minuteTime && !tasksBean.isCreateNew()) {//并且是当前的task的时间 返回结果
                        setForResult();
                    } else {
                        TmtUtils.midToastLong(this, getString(R.string.have_time_task));
                    }
                } else {//没有此时间task返回结果
                    setForResult();
                }
            }
        });
        timerLy.setOnClickListener(v -> startActivityForResult(new Intent(this,
                SelectSceneListActivity.class), requestCodes));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setForResult() {
        tasksBean.setSceneId(scene.getId());
        tasksBean.setSenceName(scene.getName());
        tasksBean.setStartHour(hourTime);
        tasksBean.setStartMins(minuteTime);
        sendLabelHeadParams();
    }

    /**
     * 发送标签保存命令
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void sendLabelHeadParams() {
        showLoadingDialog(getString(R.string.please_wait));
        if (disposableTimer != null) disposableTimer.dispose();
        disposableHeadTimer = Observable.timer(1500, TimeUnit.MILLISECONDS).subscribe(aLong -> {
            TmtUtils.midToastLong(this, getString(R.string.config_gate_way_fail));
            return;
        });
        int meshAddress = gwTagBean.getMeshAddr();
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode
        // .GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);


         opcodeHead = Opcode.CONFIG_GW_TIMER_LABLE_HEAD;

        if (gwTagBean != null) if (TelinkLightApplication.Companion.getApp().getOffLine()) {

            byte[] labHeadPar = new byte[]{0x11, 0x11, 0x11, 0, 0, 0, 0, opcodeHead, 0x11, 0x02,
                    (byte) (gwTagBean.getTagId() & 0xff), (byte) gwTagBean.getStatus(),
                    (byte) gwTagBean.getWeek(), 0, (byte) month, (byte) day, 0, 0};// status 开1
            // 关0 tag的外部现实

            Base64.Encoder encoder = Base64.getEncoder();
            String s = encoder.encodeToString(labHeadPar);
            GwGattBody gattBody = new GwGattBody();
            gattBody.setData(s);
            gattBody.setMacAddr(gwTagBean.getMacAddr());
            gattBody.setTagHead(1);
            sendToServer(gattBody);
        } else {
            byte[] labHeadPar = new byte[]{(byte) (gwTagBean.getTagId() & 0xff),
                    (byte) gwTagBean.getStatus(), (byte) gwTagBean.getWeek(), 0, (byte) month,
                    (byte) day, 0, 0};
            TelinkLightService.Instance().sendCommandResponse(opcodeHead, meshAddress, labHeadPar
                    , "1");
        }
        sendLabelHeadParams();
    }


    /**
     * 定时场景标签头下发,时间段时间下发
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendTime(GwTasksBean tasks) {
        if (disposableTimer != null) disposableTimer.dispose();
        disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS).subscribe(aLong -> {
            sendCount++;
            if (sendCount < 3) {
                sendTime(tasks);
            } else {
                ToastUtils.showLong(getString(R.string.config_gate_way_fail));
            }
        });

        //sendLabelHeadParams()
        if (gwTagBean != null && gwTagBean.isTimer()) {//定时场景标签头下发,时间段时间下发 挪移至时间段内部发送 定时场景时间下发
            if (!TelinkLightApplication.Companion.getApp().isConnectGwBle()) {
                labHeadPar = new byte[]{0x11, 0x11, 0x11, 0, 0, 0, 0,
                        Opcode.CONFIG_GW_TIMER_LABLE_TIME, 0x11, 0x02,
                        (byte) (gwTagBean.getTagId() & 0xff), (byte) (tasks.getIndex() & 0xff),
                        (byte) (tasks.getStartHour() & 0xff),
                        (byte) (tasks.getStartMins() & 0xff), (byte) (tasks.getSceneId() & 0xff),
                        0, 0, 0};

                Base64.Encoder encoder = Base64.getEncoder();
                String s = encoder.encodeToString(labHeadPar);
                GwGattBody gattBody = new GwGattBody();
                gattBody.setData(s);
                gattBody.setMacAddr(gwTagBean.getMacAddr());
                sendToServer(gattBody);
            } else {
                byte[] params = new byte[]{(byte) (gwTagBean.getTagId() & 0xff),
                        (byte) (tasks.getIndex() & 0xff), (byte) (tasks.getStartHour() & 0xff),
                        (byte) (tasks.getStartMins() & 0xff), (byte) (tasks.getSceneId() & 0xff),
                        0, 0, 0};
                TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_TIMER_LABLE_TIME, tasks.getGwMeshAddr(), params, "1");
            }
        } else {//时间段场景下发 时间段场景时间下发 挪移至时间段内部发送
        }
    }

    private void sendToServer(GwGattBody gattBody) {
        GwModel.INSTANCE.sendToGatt(gattBody).subscribe(new NetworkObserver<String>() {
            @Override
            public void onNext(String s) {
                LogUtils.v("zcl---发送服务器返回----------" + s);
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                LogUtils.e("zcl-------发送服务器返回-----------$" + e.getMessage());
            }
        });

    }


    private Boolean isTimeHave() {
        Boolean isHave = false;
        if (data != null) for (int i = 0; i < data.size(); i++) {
            GwTasksBean tag = data.get(i);
            if (tag.getStartHour() == hourTime && tag.getStartMins() == minuteTime) {
                isHave = true;
            } else {
                isHave = false;
            }
        }
        return isHave;
    }

    private void initData() {
        gwTagBean = TelinkLightApplication.Companion.getApp().getCurrentGwTagBean();
        toolbarTv.setText(getString(R.string.chose_time));
        timerTitle.setText(getString(R.string.scene_name));//底部item 的title
        Intent intent = getIntent();
        Parcelable dataParcelable = intent.getParcelableExtra("data");
        tasksBean = (GwTasksBean) dataParcelable;
        if (tasksBean != null) {
            data = TelinkLightApplication.Companion.getApp().getListTask();
            if (!tasksBean.isCreateNew()) {//不是新的赋值旧的数据
                hourTime = tasksBean.getStartHour();
                minuteTime = tasksBean.getStartMins();
                timerScene.setText(tasksBean.getSenceName());
                scene = DBUtils.INSTANCE.getSceneByID(tasksBean.getSceneId());
            }
        } else {
            TmtUtils.midToastLong(this, getString(R.string.invalid_data));
            finish();
        }
        wheelPickerLy.addView(getTimePicker());
    }

    private void initView() {
        toolbarCancel = findViewById(R.id.toolbar_t_cancel);
        toolbarTv = findViewById(R.id.toolbar_t_center);
        toolbarConfirm = findViewById(R.id.toolbar_t_confim);

        //时间选择器底部item
        timerTitle = findViewById(R.id.item_gw_timer_title);//条目头部
        timerScene = findViewById(R.id.item_gw_timer_scene);//条目尾部
        timerLy = findViewById(R.id.timer_scene_ly);
        wheelPickerLy = findViewById(R.id.wheel_time_container);

        TelinkApplication application = TelinkLightApplication.getInstance();
        application.addEventListener(DeviceEvent.STATUS_CHANGED, this);
    }

    private View getTimePicker() {
        final TimePicker picker = new TimePicker(this);

        picker.setBackgroundColor(this.getResources().getColor(R.color.white));
        picker.setDividerConfig(null);
        picker.setTextColor(this.getResources().getColor(R.color.blue_text));
        picker.setLabel("", "");
        picker.setTextSize(25);
        picker.setOffset(3);
        picker.setSelectedItem(hourTime, minuteTime);
        picker.setOnWheelListener(new DateTimePicker.OnWheelListener() {
            @Override
            public void onYearWheeled(int index, String year) {
            }

            @Override
            public void onMonthWheeled(int index, String month) {
            }

            @Override
            public void onDayWheeled(int index, String day) {
            }

            @Override
            public void onHourWheeled(int index, String hour) {
                hourTime = Integer.parseInt(hour);
            }

            @Override
            public void onMinuteWheeled(int index, String minute) {
                minuteTime = Integer.parseInt(minute);
            }
        });
        return picker.getContentView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //获取场景返回值
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            Parcelable par = data.getParcelableExtra("data");
            scene = (DbScene) par;
            LogUtils.v("zcl获取场景信息scene" + scene.toString());
            timerScene.setText(scene.getName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void performed(Event<String> event) {
        switch (event.getType()) {
            case DeviceEvent.STATUS_CHANGED:
                onDeviceEvent((DeviceEvent) event);
                break;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onDeviceEvent(DeviceEvent event) {
        DeviceInfo deviceInfo = event.getArgs();
        switch (deviceInfo.status) {
            case LightAdapter.STATUS_SET_GW_COMPLETED:
                LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo"+deviceInfo);
                switch (deviceInfo.gwVoipState) {
                    case Constant.GW_CONFIG_TIMER_LABEL_VOIP://定时标签头下发
                        if (disposableHeadTimer != null) disposableHeadTimer.dispose();
                            sendTime(tasksBean);//下发task
                        break;
                    case Constant.GW_CONFIG_TIMER_TASK_VOIP://下发成功返回数据给配置也保存更新
                        Intent intent = new Intent();
                        intent.putExtra("data", tasksBean);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                        hideLoadingDialog();
                        break;
                }
                break;
        }
    }
}
