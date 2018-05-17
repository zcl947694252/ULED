package com.dadoutek.uled.adapter;

import android.support.annotation.NonNull;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.ItemGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class SceneGroupAdapter extends BaseQuickAdapter implements SeekBar.OnSeekBarChangeListener {
    private ArrayList<Group> groupArrayList;

    private List<ItemGroup> data;
    TextView tvBrightness;
    TextView tvTemperature;
    ItemGroup itemGroup;

    public SceneGroupAdapter(int layoutResId, List<ItemGroup> data, ArrayList<Group> groupArrayList) {
        super(layoutResId, data);
        this.data = data;
        this.groupArrayList = groupArrayList;
    }

    @Override
    protected void convert(BaseViewHolder helper, Object item) {
        int position = helper.getPosition();
        SeekBar sbBrightness = helper.getView(R.id.sb_brightness);
        SeekBar sBtemperature = helper.getView(R.id.sb_temperature);

        tvBrightness = helper.getView(R.id.tv_brightness);
        tvTemperature = helper.getView(R.id.tv_temperature);
        itemGroup = (ItemGroup) item;
        helper.setText(R.id.name_gp, itemGroup.gpName);

        sbBrightness.setTag(position);
        sBtemperature.setTag(position);
        sbBrightness.setOnSeekBarChangeListener(this);
        sBtemperature.setOnSeekBarChangeListener(this);
        helper.addOnClickListener(R.id.btn_delete);

//        sbBrightness.setProgress(50);
//        sBtemperature.setProgress(50);
        tvBrightness.setText(sbBrightness.getProgress() + "%");
        tvTemperature.setText(sBtemperature.getProgress() + "%");
//        helper.setText(R.id.tv_brightness,sbBrightness.getProgress()+"");
//        helper.setText(R.id.tv_temperature,sBtemperature.getProgress()+"");
    }

    @Override
    public void addData(@NonNull Object data) {
        super.addData(data);
    }

    @Override
    public void remove(int position) {
        super.remove(position);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int position = (int) seekBar.getTag();
        int address = data.get(position).groupAress;
        byte opcode;
        byte[] params;
        if (seekBar.getId() == R.id.sb_brightness) {
            tvBrightness = (TextView) getViewByPosition(position, R.id.tv_brightness);
            tvBrightness.setText(progress + "%");
            opcode = (byte) 0xD2;
            params = new byte[]{(byte) progress};
            TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params);
        } else if (seekBar.getId() == R.id.sb_temperature) {
            tvTemperature = (TextView) getViewByPosition(position, R.id.tv_temperature);
            tvTemperature.setText(progress + "%");
            opcode = (byte) 0xE2;
            params = new byte[]{0x05, (byte) progress};
            TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.sb_brightness) {
            tvBrightness.setText(seekBar.getProgress() + "%");
            itemGroup.brightness = seekBar.getProgress();
        } else if (seekBar.getId() == R.id.sb_temperature) {
            tvTemperature.setText(seekBar.getProgress() + "%");
            itemGroup.temperature = seekBar.getProgress();
        }
    }
}
