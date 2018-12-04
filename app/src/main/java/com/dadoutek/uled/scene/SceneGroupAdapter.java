package com.dadoutek.uled.scene;

import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.ItemGroup;
import com.dadoutek.uled.util.OtherUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class SceneGroupAdapter extends BaseQuickAdapter<ItemGroup, BaseViewHolder> implements SeekBar.OnSeekBarChangeListener {

    public SceneGroupAdapter(int layoutResId, List<ItemGroup> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ItemGroup item) {
        int position = helper.getLayoutPosition();
        SeekBar sbBrightness = helper.getView(R.id.sb_brightness);
        SeekBar sBtemperature = helper.getView(R.id.sb_temperature);

        helper.setText(R.id.name_gp, item.gpName);
        helper.setBackgroundColor(R.id.rgb_view, item.color==0?TelinkLightApplication.
                getInstance().getResources().getColor(R.color.primary):(0xff000000|item.color));
        helper.setProgress(R.id.sb_brightness, item.brightness);
        helper.setProgress(R.id.sb_temperature, item.temperature);
        helper.setText(R.id.tv_brightness, sbBrightness.getProgress() + "%");
        helper.setText(R.id.tv_temperature, sBtemperature.getProgress() + "%");
        if(OtherUtils.isRGBGroup(DBUtils.INSTANCE.getGroupByMesh(item.groupAress))){
            helper.setGone(R.id.scene_rgb_layout, true);
            helper.setGone(R.id.sb_temperature_layout, false);
        }else{
            helper.setGone(R.id.scene_rgb_layout, false);
            helper.setGone(R.id.sb_temperature_layout, true);
        }

        sbBrightness.setTag(position);
        sBtemperature.setTag(position);
        sbBrightness.setOnSeekBarChangeListener(this);
        sBtemperature.setOnSeekBarChangeListener(this);
        helper.addOnClickListener(R.id.btn_delete);
        helper.addOnClickListener(R.id.rgb_view);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int position = (int) seekBar.getTag();
            int address = getData().get(position).groupAress;
            byte opcode;
            byte[] params;
            if (seekBar.getId() == R.id.sb_brightness) {
                TextView tvBrightness = (TextView) getViewByPosition(position, R.id.tv_brightness);
                if (tvBrightness != null) {
                    tvBrightness.setText(progress + "%");
                    opcode = (byte) 0xD2;
                    params = new byte[]{(byte) progress};
                    new Thread(
                            () ->
                            TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params)).start();
                }
            } else if (seekBar.getId() == R.id.sb_temperature) {
                TextView tvTemperature = (TextView) getViewByPosition(position, R.id.tv_temperature);
                if (tvTemperature != null) {
                    tvTemperature.setText(progress + "%");
                    opcode = (byte) 0xE2;
                    params = new byte[]{0x05, (byte) progress};
                    new Thread(() -> TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params)).start();
                }
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int pos = (Integer) seekBar.getTag();
        ItemGroup itemGroup = getData().get(pos);
        if (seekBar.getId() == R.id.sb_brightness) {
            ((TextView) Objects.requireNonNull(getViewByPosition(pos, R.id.tv_brightness))).setText(seekBar.getProgress() + "%");
            itemGroup.brightness=seekBar.getProgress();
        } else if (seekBar.getId() == R.id.sb_temperature) {
            ((TextView) Objects.requireNonNull(getViewByPosition(pos, R.id.tv_temperature))).setText(seekBar.getProgress() + "%");
            itemGroup.temperature=seekBar.getProgress();
        }
        notifyItemChanged((Integer) seekBar.getTag());
    }



}
