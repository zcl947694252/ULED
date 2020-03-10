package com.dadoutek.uled.gateway;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.gateway.bean.DbGatewayTimeBean;
import com.dadoutek.uled.gateway.util.IndexUtil;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.switches.SelectSceneListActivity;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.qqtheme.framework.picker.DateTimePicker;
import cn.qqtheme.framework.picker.TimePicker;

/**
 * 设置网关时间与场景传递进配config界面
 */
public class GatewayChoseTimeActivity extends TelinkBaseActivity {
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
    private DbGatewayTimeBean gatewayTimeBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_way_chose_time2);
        unbinder = ButterKnife.bind(this);
        initView();
        initData();
        initLisenter();
    }

    private void initLisenter() {
        toolbarCancel.setOnClickListener(v -> finish());
        toolbarConfirm.setOnClickListener(v -> {
            if (scene == null)
                Toast.makeText(getApplicationContext(), getString(R.string.please_select_scene), Toast.LENGTH_SHORT).show();
            else {
                Intent intent = new Intent();
                gatewayTimeBean.setSceneId(scene.getId());
                gatewayTimeBean.setSceneName(scene.getName());
                gatewayTimeBean.setHour(hourTime);
                gatewayTimeBean.setMinute(minuteTime);

                intent.putExtra("data", gatewayTimeBean);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
        timerLy.setOnClickListener(v -> startActivityForResult(new Intent(this, SelectSceneListActivity.class), requestCodes));
    }

    private void initData() {
        toolbarTv.setText(getString(R.string.chose_time));
        timerTitle.setText(getString(R.string.scene_name));
        Intent intent = getIntent();
        Parcelable data = intent.getParcelableExtra("data");
        if (data != null && !TextUtils.isEmpty(data.toString())) {
            gatewayTimeBean = (DbGatewayTimeBean) data;
            timerScene.setText(gatewayTimeBean.getSceneName());
            gatewayTimeBean.setIsNew(false);
            scene = DBUtils.INSTANCE.getSceneByID(gatewayTimeBean.getSceneId());
        } else{
            gatewayTimeBean = new DbGatewayTimeBean(hourTime,minuteTime, true);
            gatewayTimeBean.setIndex(IndexUtil.getNum());
        }
    }

    private void initView() {
        toolbarCancel = findViewById(R.id.toolbar_t_cancel);
        toolbarTv = findViewById(R.id.toolbar_t_center);
        toolbarConfirm = findViewById(R.id.toolbar_t_confim);

        timerTitle = findViewById(R.id.item_gate_way_timer_time);
        timerScene = findViewById(R.id.item_gate_way_timer_scene);
        timerLy = findViewById(R.id.timer_scene_ly);
        wheelPickerLy = findViewById(R.id.wheel_time_container);

        wheelPickerLy.addView(getTimePicker());
    }

    private View getTimePicker() {
        final TimePicker picker = new TimePicker(this);
        picker.setBackgroundColor(this.getResources().getColor(R.color.white));
        picker.setDividerConfig(null);
        picker.setTextColor(this.getResources().getColor(R.color.blue_text));
        picker.setLabel("", "");
        picker.setTextSize(25);
        picker.setOffset(3);
        if (scene != null) {
            String[] split = scene.getTimes().split("-");
            if (split.length == 2)
                picker.setSelectedItem(Integer.getInteger(split[1]), Integer.getInteger(split[0]));
        } else
            picker.setSelectedItem(3, 15);
        picker.setOnWheelListener(new DateTimePicker.OnWheelListener() {
            @Override
            public void onYearWheeled(int index, String year) { }
            @Override
            public void onMonthWheeled(int index, String month) { }
            @Override
            public void onDayWheeled(int index, String day) { }
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
}
