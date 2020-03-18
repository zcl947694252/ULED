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
import com.dadoutek.uled.gateway.bean.GatewayTasksBean;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.switches.SelectSceneListActivity;
import com.dadoutek.uled.util.TmtUtils;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.qqtheme.framework.picker.DateTimePicker;
import cn.qqtheme.framework.picker.TimePicker;

/**
 * 设置网关时间与场景传递进配config界面
 * task任务
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
    private GatewayTasksBean tasksBean;
    ArrayList<Parcelable> data;

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
                if (isTimeHave()) {//如果已有该时间
                    if (tasksBean.getStartHour() == hourTime && tasksBean.getStartMins() == minuteTime) {//并且是当前的task的时间 返回结果
                        setForResult();
                    } else {
                        TmtUtils.midToastLong(this, getString(R.string.have_time_task));
                    }
                } else {//没有此时间task返回结果
                    setForResult();
                }
            }
        });
        timerLy.setOnClickListener(v -> startActivityForResult(new Intent(this, SelectSceneListActivity.class), requestCodes));
    }

    private void setForResult() {
        Intent intent = new Intent();
        tasksBean.setSceneId(scene.getId());
        tasksBean.setSenceName(scene.getName());
        tasksBean.setStartHour(hourTime);
        tasksBean.setStartMins(minuteTime);
        intent.putExtra("data", tasksBean);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private Boolean isTimeHave() {
        Boolean isHave = false;
        if (data!=null)
        for (int i = 0; i < data.size(); i++) {
            GatewayTasksBean tag = (GatewayTasksBean) data.get(i);
            if (tag.getStartHour() == hourTime && tag.getStartMins() == minuteTime) {
                isHave = true;
            } else {
                isHave = false;
            }
        }
        return isHave;
    }

    private void initData() {
        toolbarTv.setText(getString(R.string.chose_time));
        timerTitle.setText(getString(R.string.scene_name));//底部item 的title
        Intent intent = getIntent();
        data = intent.getParcelableArrayListExtra("data");

        if (data != null && !TextUtils.isEmpty(data.toString())) {//编辑老的task
            GatewayTasksBean tagBean = (GatewayTasksBean) data.get(0);
            int pos = tagBean.getSelectPos();//拿到点击pos
            tasksBean = (GatewayTasksBean) data.get(pos);
            hourTime = tasksBean.getStartHour();
            minuteTime = tasksBean.getStartMins();
            timerScene.setText(tasksBean.getSenceName());
            tasksBean.setCreateNew(false);
            scene = DBUtils.INSTANCE.getSceneByID(tasksBean.getSceneId());
        } else {//新创建task 获取传过来的index值
           int index =  intent.getIntExtra("index",0);
            tasksBean = new GatewayTasksBean(index);
            tasksBean.setCreateNew(true);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {//获取场景返回值
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
