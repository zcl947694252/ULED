package com.dadoutek.uled.fragments;

import android.app.Application;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.MainActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.widget.ColorPicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public final class GroupSettingFragment extends Fragment {

    public int groupAddress;
    @BindView(R.id.btn_remove_group)
    Button btnRemoveGroup;
    Unbinder unbinder;

    private SeekBar brightnessBar;
    private SeekBar temperatureBar;
    private ColorPicker colorPicker;
    private TelinkLightApplication mApplication;

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

            int addr = groupAddress;
            byte opcode;
            byte[] params;

            if (view == brightnessBar) {
                opcode = (byte) 0xD2;
                params = new byte[]{(byte) progress};

                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);

            } else if (view == temperatureBar) {

                opcode = (byte) 0xE2;
                params = new byte[]{0x05, (byte) progress};

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

            int addr = groupAddress;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.btn_remove_group)
    public void onViewClicked() {
        DataManager dataManager = new DataManager(getActivity(), mApplication.getMesh().name, mApplication.getMesh().password);
        Groups groups = dataManager.getGroups();
        for (int k = 0; k < groups.size(); k++) {
            if (groupAddress == groups.get(k).meshAddress && groups.get(k).containsLightList != null) {
                groups.get(k).containsLightList.clear();
                dataManager.updateGroup(groups);
//               startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().setResult(Constant.RESULT_OK);
                getActivity().finish();
                break;
            }
        }
    }
}
