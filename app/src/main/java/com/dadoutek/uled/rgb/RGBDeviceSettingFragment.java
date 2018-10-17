package com.dadoutek.uled.rgb;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dadoutek.uled.R;
import com.dadoutek.uled.light.RenameLightActivity;
import com.dadoutek.uled.group.LightGroupingActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.ItemColorPreset;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.network.NetworkFactory;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.DataManager;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.telink.bluetooth.light.DeviceInfo;
import com.telink.bluetooth.light.LeAutoConnectParameters;
import com.telink.bluetooth.light.LeRefreshNotifyParameters;
import com.telink.bluetooth.light.LightAdapter;
import com.telink.bluetooth.light.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public final class RGBDeviceSettingFragment extends Fragment {

    public final static String TAG = RGBDeviceSettingFragment.class.getSimpleName();

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
    @BindView(R.id.update_group)
    Button updateGroup;
    @BindView(R.id.btn_remove)
    Button btnRemove;
    @BindView(R.id.color_r)
    TextView colorR;
    @BindView(R.id.color_g)
    TextView colorG;
    @BindView(R.id.color_b)
    TextView colorB;
    @BindView(R.id.diy_color_recycler_list_view)
    RecyclerView diyColorRecyclerListView;
    @BindView(R.id.scroll_view)
    ScrollView scrollView;
    Unbinder unbinder1;

    private SeekBar brightnessBar;
    private SeekBar temperatureBar;
    private ColorPickerView colorPicker;
    private Button remove;
    private AlertDialog dialog;
    private List<ItemColorPreset> presetColors;
    private ColorSelectDiyRecyclerViewAdapter colorSelectDiyRecyclerViewAdapter;

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
                DBUtils.INSTANCE.updateLight(light);
            } else if (view == temperatureBar) {

                opcode = (byte) Opcode.SET_TEMPERATURE;
                params = new byte[]{0x05, (byte) progress};
                tvTemperature.setText(getString(R.string.device_setting_temperature, progress + ""));

                light.setColorTemperature(progress);
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
                DBUtils.INSTANCE.updateLight(light);
            }
        }
    };


    private TelinkLightApplication mApp;
    private DataManager manager;
    private DeviceInfo mConnectDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mApp = (TelinkLightApplication) this.getActivity().getApplication();
        manager = new DataManager(mApp, mApp.getMesh().getName(), mApp.getMesh().getPassword());
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
        View view = inflater.inflate(R.layout.fragment_rgb_device_setting, null);
        View newView = getView(view);
        unbinder1 = ButterKnife.bind(this, newView);
        return newView;
    }

    private View getView(View view) {
        this.brightnessBar = (SeekBar) view.findViewById(R.id.sb_brightness);
        this.temperatureBar = (SeekBar) view.findViewById(R.id.sb_temperature);

        brightnessBar.setMax(100);
        temperatureBar.setMax(100);

        this.colorPicker = (ColorPickerView) view.findViewById(R.id.color_picker);
        this.colorPicker.setColorListener(colorEnvelopeListener);

        unbinder = ButterKnife.bind(this, view);


        mConnectDevice = TelinkLightApplication.getInstance().getConnectDevice();

        presetColors = (List<ItemColorPreset>) SharedPreferencesHelper.getObject(getActivity(), Constant.LIGHT_PRESET_COLOR);
        if (presetColors == null) {
            presetColors = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ItemColorPreset itemColorPreset = new ItemColorPreset();
                presetColors.add(itemColorPreset);
            }
        }
        LinearLayoutManager layoutmanager = new LinearLayoutManager(getActivity());
        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        diyColorRecyclerListView.setLayoutManager(layoutmanager);
        colorSelectDiyRecyclerViewAdapter = new ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildClickListener(diyOnItemChildClickListener);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildLongClickListener(diyOnItemChildLongClickListener);
        colorSelectDiyRecyclerViewAdapter.bindToRecyclerView(diyColorRecyclerListView);
        return view;
    }

    BaseQuickAdapter.OnItemChildClickListener diyOnItemChildClickListener = new BaseQuickAdapter.OnItemChildClickListener() {
        @Override
        public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
            int color = presetColors.get(position).getColor();
            int brightness = presetColors.get(position).getBrightness();
            int red = (color & 0xff0000) >> 16;
            int green = (color & 0x00ff00) >> 8;
            int blue = (color & 0x0000ff);
            new Thread(() -> {
                changeColor((byte) red, (byte) green, (byte) blue);

                try {
                    Thread.sleep(200);
                    int addr = light.getMeshAddr();
                    byte opcode;
                    byte[] params;
                    opcode = (byte) Opcode.SET_LUM;
                    params = new byte[]{(byte) brightness};
                    light.setBrightness(brightness);
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
                    DBUtils.INSTANCE.updateLight(light);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            brightnessBar.setProgress(brightness);
            scrollView.setBackgroundColor(color);
            colorR.setText(red + "");
            colorG.setText(green + "");
            colorB.setText(blue + "");
        }
    };

    BaseQuickAdapter.OnItemChildLongClickListener diyOnItemChildLongClickListener = new BaseQuickAdapter.OnItemChildLongClickListener() {
        @Override
        public boolean onItemChildLongClick(BaseQuickAdapter adapter, View view, int position) {
            presetColors.get(position).setColor(light.getColor());
            presetColors.get(position).setBrightness(light.getBrightness());
            TextView textView = (TextView) adapter.getViewByPosition(position, R.id.btn_diy_preset);
            textView.setText(light.getBrightness() + "%");
            textView.setBackgroundColor(light.getColor());
            SharedPreferencesHelper.putObject(getActivity(), Constant.LIGHT_PRESET_COLOR, presetColors);
            return false;
        }
    };

    int count =0;
    private ColorEnvelopeListener colorEnvelopeListener = new ColorEnvelopeListener() {

        @Override
        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
            int[] argb = envelope.getArgb();

            colorR.setText(argb[1] + "");
            colorG.setText(argb[2] + "");
            colorB.setText(argb[3] + "");

            int color = Color.argb(255, argb[1], argb[2], argb[3]);
            if(fromUser){
                scrollView.setBackgroundColor(color);
                light.setColor(color);
                if(argb[1]==0 && argb[2]==0 && argb[3]==0){
                }else{
                    changeColor((byte) argb[1], (byte) argb[2], (byte) argb[3]);
                }
            }
        }
    };

    private void changeColor(byte R, byte G, byte B) {

        byte red = R;
        byte green = G;
        byte blue = B;

        int addr = light.getMeshAddr();
        byte opcode = (byte) 0xE2;

        byte minVal = (byte) 0x50;

        if ((green & 0xff) <= minVal)
            green = 0;
        if ((red & 0xff) <= minVal)
            red = 0;
        if ((blue & 0xff) <= minVal)
            blue = 0;


        byte[] params = new byte[]{0x04, red, green, blue};

        String logStr = String.format("R = %x, G = %x, B = %x", red, green, blue);
        Log.d("RGBCOLOR", logStr);

        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (fromWhere != null && !fromWhere.isEmpty() && gpAddress == 0xffff) {
////            remove.setVisibility(View.GONE);
//            btnRename.setVisibility(View.GONE);
//        }
        btnRename.setVisibility(View.GONE);
        brightnessBar.setProgress(light.getBrightness());
        tvBrightness.setText(getString(R.string.device_setting_brightness, light.getBrightness() + ""));
        temperatureBar.setProgress(light.getColorTemperature());
        tvTemperature.setText(getString(R.string.device_setting_temperature, light.getColorTemperature() + ""));

//        sendInitCmd(light.getBrightness(),light.getColorTemperature());

        this.brightnessBar.setOnSeekBarChangeListener(this.barChangeListener);
        this.temperatureBar.setOnSeekBarChangeListener(this.barChangeListener);
    }

    private void sendInitCmd(int brightness, int colorTemperature) {
        int addr = light.getMeshAddr();
        byte opcode;
        byte[] params;
//                progress += 5;
//                Log.d(TAG, "onValueChange: "+progress);
        opcode = (byte) Opcode.SET_LUM;
        params = new byte[]{(byte) brightness};
        light.setBrightness(brightness);
        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            opcode = (byte) Opcode.SET_TEMPERATURE;
            params = new byte[]{0x05, (byte) colorTemperature};
            light.setColorTemperature(colorTemperature);
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
        }
    }

    public void remove() {
        new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {

                    if (TelinkLightService.Instance().getAdapter().mLightCtrl.getCurrentLight().isConnected()) {
                        byte opcode = (byte) Opcode.KICK_OUT;
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, light.getMeshAddr(), null);
                        DBUtils.INSTANCE.deleteLight(light);
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

                if (TextUtils.isEmpty(mesh.getName()) || TextUtils.isEmpty(mesh.getPassword())) {
                    TelinkLightService.Instance().idleMode(true);
                    return;
                }

                String account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.DB_NAME_KEY, "dadou");

                //自动重连参数
                LeAutoConnectParameters connectParams = Parameters.createAutoConnectParameters();
                connectParams.setMeshName(mesh.getName());
                if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance()
                        , Constant.USER_TYPE, Constant.USER_TYPE_OLD).equals(Constant.USER_TYPE_NEW)) {
                    connectParams.setPassword(NetworkFactory.md5(
                            NetworkFactory.md5(mesh.getPassword()) + account));
                } else {
                    connectParams.setPassword(mesh.getPassword());
                }
                connectParams.autoEnableNotification(true);

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh.isOtaProcessing()) {
                    connectParams.setConnectMac(mesh.getOtaDevice().mac);
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

    @OnClick({R.id.update_group, R.id.btn_remove})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.update_group:
                updateGroup();
                break;
            case R.id.btn_remove:
                remove();
                break;
        }
    }

    private void updateGroup() {
        Intent intent = new Intent(getActivity(),
                LightGroupingActivity.class);
        intent.putExtra("light", light);
        intent.putExtra("gpAddress", gpAddress);
        startActivity(intent);
        getActivity().finish();
    }
}
