package com.dadoutek.uled.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.RenameLightActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.widget.ColorPicker;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeAutoConnectParameters;
import com.telink.bluetooth.light.LeRefreshNotifyParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.Parameters;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public final class DeviceSettingFragment extends Fragment implements View.OnClickListener {

    public final static String TAG = DeviceSettingFragment.class.getSimpleName();

    public DbLight light;
    public int gpAddress;
    public String fromWhere;
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

    private OnSeekBarChangeListener barChangeListener = new OnSeekBarChangeListener() {

        private long preTime;
        private int delayTime = 20;

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

//            if (progress % 5 != 0)
//                return;

//            long currentTime = System.currentTimeMillis();
//
//            if ((currentTime - this.preTime) >= this.delayTime) {
//                this.preTime = currentTime;
//            }

            this.onValueChange(seekBar, progress, false);
        }

        private void onValueChange(View view, int progress, boolean immediate) {

            int addr = light.getMeshAddr();
            byte opcode;
            byte[] params;
            if (view == brightnessBar) {
//                progress += 5;
//                Log.d(TAG, "onValueChange: "+progress);
                tvBrightness.setText(getString(R.string.device_setting_brightness, progress + ""));
                opcode = (byte) Opcode.SET_LUM;
                params = new byte[]{(byte) progress};

                light.setBrightness(progress);
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
                DBUtils.updateLight(light);
            } else if (view == temperatureBar) {

                opcode = (byte) Opcode.SET_TEMPERATURE;
                params = new byte[]{0x05, (byte) progress};
                tvTemperature.setText(getString(R.string.device_setting_temperature, progress + ""));

                light.setColorTemperature(progress);
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
                DBUtils.updateLight(light);
            }
        }
    };

    private ColorPicker.OnColorChangeListener colorChangedListener = new ColorPicker.OnColorChangeListener() {

        private long preTime;
        private int delayTime = 20;

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

//            if ((currentTime - this.preTime) >= this.delayTime) {
//                this.preTime = currentTime;
//                this.changeColor(color);
//            }
        }

        private void changeColor(int color) {

            byte red = (byte) (color >> 16 & 0xFF);
            byte green = (byte) (color >> 8 & 0xFF);
            byte blue = (byte) (color & 0xFF);

            int addr = light.getMeshAddr();
            byte opcode = Opcode.SET_TEMPERATURE;
            byte[] params = new byte[]{0x04, red, green, blue};

            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
        }
    };
    private TelinkLightApplication mApp;
    private DataManager manager;
    private DeviceInfo mConnectDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mApp = (TelinkLightApplication) this.getActivity().getApplication();
        manager = new DataManager(mApp, mApp.getMesh().name, mApp.getMesh().password);
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


        mConnectDevice = TelinkLightApplication.getInstance().getConnectDevice();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fromWhere != null && !fromWhere.isEmpty() && gpAddress == 0xffff) {
//            remove.setVisibility(View.GONE);
            btnRename.setVisibility(View.GONE);
        }
        brightnessBar.setProgress(light.getBrightness());
        tvBrightness.setText(getString(R.string.device_setting_brightness, light.getBrightness() + ""));
        temperatureBar.setProgress(light.getColorTemperature());
        tvTemperature.setText(getString(R.string.device_setting_temperature, light.getColorTemperature() + ""));
    }

    @Override
    public void onClick(View v) {
        if (v == this.remove) {
            new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setMessage(R.string.delete_light_confirm)
                    .setPositiveButton(R.string.btn_ok, (dialog, which) -> {

                        if (TelinkLightService.Instance().getAdapter().mLightCtrl.getCurrentLight().isConnected()) {
                            byte opcode = (byte) Opcode.KICK_OUT;
                            TelinkLightService.Instance().sendCommandNoResponse(opcode, light.getMeshAddr(), null);
                            DBUtils.deleteLight(light);
                            if (TelinkLightApplication.getApp().getMesh().removeDeviceByMeshAddress(light.getMeshAddr())) {
                                TelinkLightApplication.getApp().getMesh().saveOrUpdate(getActivity());
                            }
                            if (mConnectDevice != null) {
                                Log.d(getActivity().getClass().getSimpleName(), "mConnectDevice.meshAddress = " + mConnectDevice.meshAddress);
                                Log.d(getActivity().getClass().getSimpleName(), "light.getMeshAddr() = " + light.getMeshAddr());
                                if (light.getMeshAddr() == mConnectDevice.meshAddress) {
                                    getActivity().setResult(Activity.RESULT_OK, new Intent().putExtra("data", true));
                                }
                            }
                            getActivity().finish();


                        } else {
                            ToastUtils.showLong("当前处于未连接状态，重连中。。。");
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();

//            if (gpAddress != 0) {
//                Group group = manager.getGroup(gpAddress, getActivity());
//                group.containsLightList.remove((Integer) light.getMeshAddr());
//                manager.updateGroup(group, getActivity());
//            }
//            getActivity().finish();
        }
    }

    /**
     * 自动重连
     */
    private void autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().getMode() != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting));
                SharedPreferencesHelper.putBoolean(getActivity(), Constant.CONNECT_STATE_SUCCESS_KEY, false);

                if (this.mApp.isEmptyMesh())
                    return;

//                Lights.getInstance().clear();
                this.mApp.refreshLights();

                Mesh mesh = this.mApp.getMesh();

                if (TextUtils.isEmpty(mesh.name) || TextUtils.isEmpty(mesh.password)) {
                    TelinkLightService.Instance().idleMode(true);
                    return;
                }

                //自动重连参数
                LeAutoConnectParameters connectParams = Parameters.createAutoConnectParameters();
                connectParams.setMeshName(mesh.name);
                connectParams.setPassword(mesh.password);
                connectParams.autoEnableNotification(true);

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh.isOtaProcessing()) {
                    connectParams.setConnectMac(mesh.otaDevice.mac);
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams);
            }

            //刷新Notify参数
            LeRefreshNotifyParameters refreshNotifyParams = Parameters.createRefreshNotifyParameters();
            refreshNotifyParams.setRefreshRepeatCount(2);
            refreshNotifyParams.setRefreshInterval(2000);
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.btn_rename)
    public void onViewClicked() {
        Intent intent = new Intent(getActivity(), RenameLightActivity.class);
        intent.putExtra("light", light);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
