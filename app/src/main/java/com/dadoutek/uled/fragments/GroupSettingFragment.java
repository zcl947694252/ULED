package com.dadoutek.uled.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.RenameActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.widget.ColorPicker;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public final class GroupSettingFragment extends Fragment {

    @BindView(R.id.btn_remove_group)
    Button btnRemoveGroup;
    @BindView(R.id.btn_rename)
    Button btnRename;
    Unbinder unbinder;
    @BindView(R.id.tv_brightness)
    TextView tvBrightness;
    @BindView(R.id.tv_temperature)
    TextView tvTemperature;

    private SeekBar brightnessBar;
    private SeekBar temperatureBar;
    private ColorPicker colorPicker;
    private TelinkLightApplication mApplication;
    public DbGroup group;
    private DataManager dataManager;

    private OnSeekBarChangeListener barChangeListener = new OnSeekBarChangeListener() {

        private long preTime;
        private int delayTime = 100;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            this.onValueChange(seekBar, seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.preTime = System.currentTimeMillis();
            this.onValueChange(seekBar, seekBar.getProgress());
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            if (progress % 5 != 0)
                return;

            long currentTime = System.currentTimeMillis();

            if ((currentTime - this.preTime) < this.delayTime) {
                this.preTime = currentTime;
                return;
            }

            this.onValueChange(seekBar, progress);
        }

        private void onValueChange(View view, int progress) {

            int addr = group.getMeshAddr();
            byte opcode;
            byte[] params;

            if (view == brightnessBar) {
                opcode = (byte) 0xD2;
                params = new byte[]{(byte) progress};
                group.setBrightness(progress);
                DBUtils.updateGroup(group);
                tvBrightness.setText(getString(R.string.device_setting_brightness, progress + ""));
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);

            } else if (view == temperatureBar) {

                opcode = (byte) 0xE2;
                params = new byte[]{0x05, (byte) progress};
                group.setColorTemperature(progress);
                DBUtils.updateGroup(group);
                tvTemperature.setText(getString(R.string.device_setting_temperature, progress + ""));
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
            }
        }
    };
    private ColorPicker.OnColorChangeListener colorChangedListener = new ColorPicker.OnColorChangeListener() {

        private long preTime;
        private int delayTime = 100;

        @Override
        public void onStartTrackingTouch(ColorPicker view) {
            this.preTime = System.currentTimeMillis();
            this.changeColor(view.getColor());
        }

        @Override
        public void onStopTrackingTouch(ColorPicker view) {
            this.changeColor(view.getColor());
        }

        @Override
        public void onColorChanged(ColorPicker view, int color) {

            long currentTime = System.currentTimeMillis();

            if ((currentTime - this.preTime) >= this.delayTime) {
                this.preTime = currentTime;
                this.changeColor(color);
            }
        }

        private void changeColor(int color) {

            byte red = (byte) (color >> 16 & 0xFF);
            byte green = (byte) (color >> 8 & 0xFF);
            byte blue = (byte) (color & 0xFF);

            int addr = group.getMeshAddr();
            byte opcode = (byte) 0xE2;
            byte[] params = new byte[]{0x04, red, green, blue};

            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplication = (TelinkLightApplication) getActivity().getApplication();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_group_setting,
                null);

        this.brightnessBar = (SeekBar) view.findViewById(R.id.sb_brightness);
        this.temperatureBar = (SeekBar) view.findViewById(R.id.sb_temperature);

        this.brightnessBar.setOnSeekBarChangeListener(this.barChangeListener);
        this.temperatureBar.setOnSeekBarChangeListener(this.barChangeListener);

        this.colorPicker = (ColorPicker) view.findViewById(R.id.color_picker);
        this.colorPicker.setOnColorChangeListener(this.colorChangedListener);

        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private void checkGroupIsSystemGroup() {
        if (group.getMeshAddr() == 0xFFFF) {
            btnRemoveGroup.setVisibility(View.GONE);
            btnRename.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkGroupIsSystemGroup();
        brightnessBar.setProgress(group.getBrightness());
        tvBrightness.setText(getString(R.string.device_setting_brightness, group.getBrightness() + ""));
        temperatureBar.setProgress(group.getColorTemperature());
        tvTemperature.setText(getString(R.string.device_setting_temperature, group.getColorTemperature() + ""));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.btn_remove_group, R.id.btn_rename})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_remove_group:
                new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setMessage(R.string.delete_group_confirm)
                        .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                            DBUtils.deleteGroup(group);
                            getActivity().setResult(Constant.RESULT_OK);
                            getActivity().finish();
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                break;
            case R.id.btn_rename:
                renameGp();
                break;
        }
    }

    private void renameGp() {
        Intent intent = new Intent(getActivity(), RenameActivity.class);
        intent.putExtra("group", group);
        startActivity(intent);
        getActivity().finish();
    }


}
