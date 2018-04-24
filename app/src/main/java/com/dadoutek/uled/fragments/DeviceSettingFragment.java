package com.dadoutek.uled.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.RenameLightActivity;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.widget.ColorPicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public final class DeviceSettingFragment extends Fragment implements View.OnClickListener {

    public final static String TAG = DeviceSettingFragment.class.getSimpleName();

    public int meshAddress;
    @BindView(R.id.tv_brightness)
    TextView tvBrightness;
    @BindView(R.id.tv_temperature)
    TextView tvTemperature;
    Unbinder unbinder;
    @BindView(R.id.btn_rename)
    Button btnRename;

    private SeekBar brightnessBar;
    private SeekBar temperatureBar;
    private ColorPicker colorPicker;
    private Button remove;
    private AlertDialog dialog;
    Light light;

    private OnSeekBarChangeListener barChangeListener = new OnSeekBarChangeListener() {

        private long preTime;
        private int delayTime = 100;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            this.onValueChange(seekBar, seekBar.getProgress(), true);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.preTime = System.currentTimeMillis();
            this.onValueChange(seekBar, seekBar.getProgress(), true);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

           /* if (progress % 5 != 0)
                return;

            long currentTime = System.currentTimeMillis();

            if ((currentTime - this.preTime) >= this.delayTime) {
                this.preTime = currentTime;*/
            this.onValueChange(seekBar, progress, false);
            //}
        }

        private void onValueChange(View view, int progress, boolean immediate) {

            int addr = meshAddress;
            byte opcode;
            byte[] params;
            if (view == brightnessBar) {
//                progress += 5;
//                Log.d(TAG, "onValueChange: "+progress);
                tvBrightness.setText(getString(R.string.device_setting_brightness, progress + ""));
                opcode = (byte) 0xD2;
                params = new byte[]{(byte) progress};

                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);

            } else if (view == temperatureBar) {

                opcode = (byte) 0xE2;
                params = new byte[]{0x05, (byte) progress};
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

            int addr = meshAddress;
            byte opcode = (byte) 0xE2;
            byte[] params = new byte[]{0x04, red, green, blue};

            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
        }
    };
    private TelinkLightApplication mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mApp = (TelinkLightApplication) this.getActivity().getApplication();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_device_setting, null);

        this.brightnessBar = (SeekBar) view.findViewById(R.id.sb_brightness);
        this.temperatureBar = (SeekBar) view.findViewById(R.id.sb_temperature);

        brightnessBar.setMax(100);
        temperatureBar.setMax(100);

        this.brightnessBar.setOnSeekBarChangeListener(this.barChangeListener);
        this.temperatureBar.setOnSeekBarChangeListener(this.barChangeListener);

        this.colorPicker = (ColorPicker) view.findViewById(R.id.color_picker);
        this.colorPicker.setOnColorChangeListener(this.colorChangedListener);

        this.remove = (Button) view.findViewById(R.id.btn_remove);
        this.remove.setOnClickListener(this);

        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        light = Lights.getInstance().getByMeshAddress(meshAddress);
        brightnessBar.setProgress(light.brightness);
        tvBrightness.setText(getString(R.string.device_setting_brightness, light.brightness + ""));
        temperatureBar.setProgress(light.temperature);
        tvTemperature.setText(getString(R.string.device_setting_temperature, light.temperature + ""));
    }

    @Override
    public void onClick(View v) {
        if (v == this.remove) {
            byte opcode = (byte) 0xE3;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, meshAddress, null);
            Lights.getInstance().remove(Lights.getInstance().getByMeshAddress(meshAddress));
            if (TelinkLightApplication.getApp().getMesh().removeDeviceByMeshAddress(meshAddress)) {
                TelinkLightApplication.getApp().getMesh().saveOrUpdate(getActivity());
            }

//            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.btn_rename)
    public void onViewClicked() {
        Intent intent=new Intent(getActivity(), RenameLightActivity.class);
        intent.putExtra("lightAddress",meshAddress);
        startActivityForResult(intent,0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
