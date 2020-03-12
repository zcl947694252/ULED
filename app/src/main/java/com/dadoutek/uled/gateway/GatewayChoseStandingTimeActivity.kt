package com.dadoutek.uled.gateway

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_chose_standing_time.*
import kotlinx.android.synthetic.main.template_top_three.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/11 15:33
 * 描述 选择停留时间
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayChoseStandingTimeActivity : TelinkBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chose_standing_time)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        toolbar_t_cancel.setOnClickListener { finish() }
        toolbar_t_confim.setOnClickListener { finish() }
    }

    private fun initData() {
        gate_way_standing_time_ly.addView(getNumPicker())
    }

    private fun getNumPicker(): View? {
        val numberPicker = NumberPicker(this)
        numberPicker.dividerDrawable = null
        numberPicker.maxValue = 50
        numberPicker.minValue = 1
        numberPicker.setFormatter { getString(R.string.minute) }
        numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            LogUtils.v("zcl----------$oldVal----------$newVal")
        }
        return numberPicker.rootView
    }
    //{
    //        final TimePicker picker = new TimePicker(this);
    //        picker.setBackgroundColor(this.getResources().getColor(R.color.white));
    //        picker.setDividerConfig(null);
    //        picker.setTextColor(this.getResources().getColor(R.color.blue_text));
    //        picker.setLabel("", "");
    //        picker.setTextSize(25);
    //        picker.setOffset(3);
    //        if (scene != null) {
    //            String[] split = scene.getTimes().split("-");
    //            if (split.length == 2)
    //                picker.setSelectedItem(Integer.getInteger(split[1]), Integer.getInteger(split[0]));
    //        } else
    //            picker.setSelectedItem(3, 15);
    //        picker.setOnWheelListener(new DateTimePicker.OnWheelListener() {
    //            @Override
    //            public void onYearWheeled(int index, String year) { }
    //            @Override
    //            public void onMonthWheeled(int index, String month) { }
    //            @Override
    //            public void onDayWheeled(int index, String day) { }
    //            @Override
    //            public void onHourWheeled(int index, String hour) {
    //                hourTime = Integer.parseInt(hour);
    //            }
    //            @Override
    //            public void onMinuteWheeled(int index, String minute) {
    //                minuteTime = Integer.parseInt(minute);
    //            }
    //        });
    //
    //        return picker.getContentView();
    //    }

    private fun initView() {
        toolbar_t_center.text = getString(R.string.standing_time)
    }
}