package com.dadoutek.uled.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.AddMeshActivity;
import com.dadoutek.uled.activity.DeviceScanningActivity;
import com.dadoutek.uled.activity.DeviceSettingActivity;
import com.dadoutek.uled.activity.LogInfoActivity;
import com.dadoutek.uled.activity.OTAUpdateActivity;
import com.dadoutek.uled.activity.OnlineStatusTestActivity;
import com.dadoutek.uled.activity.UserAllActivity;
import com.dadoutek.uled.model.Lights;
import com.telink.bluetooth.light.ConnectionStatus;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Light;

import java.util.List;

public final class DeviceListFragment extends Fragment {

    private static final String TAG = DeviceListFragment.class.getSimpleName();
    private static final int UPDATE = 1;
    private LayoutInflater inflater;
    private DeviceListAdapter adapter;

    private Button backView;
    private ImageView editView;
    private Button btnAllOn;
    private Button btnAllOff;
    private Button btnOta;

    private Activity mContext;

    private EditText txtSendInterval;
    private EditText txtSendNumbers;
    private TextView txtNotifyCount;
    private TextView log;

    // interval on off test
    private EditText et_adr, et_interval;
    private Button btn_start_test;
    private Handler mIntervalHandler;
    private boolean testStarted;
    private long interval;
    private int address;
    private boolean onOff = false;
    private TextView tv_test_count;
    private int testCount;


    private Button btn_online_status;



    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (v == btnAllOn) {
                byte opcode = (byte) 0xD0;
                int address = 0xFFFF;
                byte[] params = new byte[]{0x01, 0x00, 0x00};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params);
            } else if (v == btnAllOff) {
                byte opcode = (byte) 0xD0;
                int address = 0xFFFF;
                byte[] params = new byte[]{0x00, 0x00, 0x00};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params);
            } else if (v == backView) {
                Intent intent = new Intent(mContext, AddMeshActivity.class);
                startActivity(intent);

            } else if (v == editView) {
                Intent intent = new Intent(mContext, DeviceScanningActivity.class);
//                Intent intent = new Intent(mContext, DeviceBatchScanningActivity.class);
                startActivity(intent);
            } else if (v == btnOta) {
//                Intent intent = new Intent(mContext, OtaDeviceListActivity.class);
                List<Light> lights = Lights.getInstance().get();
                for (Light light : lights) {
                    if (light.status != ConnectionStatus.OFFLINE) {
//                        Intent intent = new Intent(mContext, OtaActivity.class);
//                        intent.putExtra("meshAddress", light.meshAddress);
                        Intent intent = new Intent(mContext, OTAUpdateActivity.class);
                        startActivity(intent);
                        return;
                    }
                }
                Toast.makeText(getActivity(), "No Device Online!", Toast.LENGTH_SHORT).show();
            } else if (v == log) {
                startActivity(new Intent(getActivity(), LogInfoActivity.class));
            } else if (v.getId() == R.id.userAll) {
                startActivity(new Intent(getActivity(), UserAllActivity.class));
            } else if (v ==  btn_start_test){
                if (!testStarted){
                    startIntervalTest();
                }else {
                    stopIntervalTest();
                }
            }else if (v == btn_online_status){
                startActivity(new Intent(getActivity(), OnlineStatusTestActivity.class));
            }
        }
    };



    private void startIntervalTest(){
        try {
            interval = Long.parseLong(et_interval.getText().toString().trim());
            address = Integer.parseInt(et_adr.getText().toString(), 16);
            testStarted = true;
            testCount = 0;
            btn_start_test.setText("stop");
            tv_test_count.setText(testCount + "");
            mIntervalHandler.removeCallbacksAndMessages(null);
            mIntervalHandler.post(intervalTask);

        }catch (Exception e){
            Toast.makeText(mContext, "input error", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopIntervalTest(){
        testStarted = false;
        btn_start_test.setText("start");
        mIntervalHandler.removeCallbacksAndMessages(null);
    }

    private Runnable intervalTask = new Runnable() {
        @Override
        public void run() {
            if (!testStarted)return;
            if (onOff) {
                byte opcode = (byte) 0xD0;
//                int address = 0xFFFF;
                byte[] params = new byte[]{0x01, 0x00, 0x00};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params);
            } else {
                byte opcode = (byte) 0xD0;
//                int address = 0xFFFF;
                byte[] params = new byte[]{0x00, 0x00, 0x00};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, address,
                        params);
            }
            testCount++;
            tv_test_count.setText(testCount + "");
            onOff = !onOff;
            mIntervalHandler.removeCallbacks(this);
            mIntervalHandler.postDelayed(this, interval);
        }
    };

    private OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            Light light = adapter.getItem(position);

            if (light.status == ConnectionStatus.OFFLINE)
                return;

            int dstAddr = light.meshAddress;

            byte opcode = (byte) 0xD0;

            if (light.status == ConnectionStatus.OFF) {

//                if (TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
//                        new byte[]{0x01, 0x00, 0x00})) {
//                TelinkLightService.Instance().sendCommandNoResponse((byte) 0xFE, 0xFFFF,
//                        new byte[]{0x07, 0x00, 0x0B, 0x00, 0x0F, 0x00, 0x17, 0x1E, 0x00, 0x00});
//                TelinkLightService.Instance().sendCommandNoResponse((byte) 0xFE, 0xFFFF,
//                        new byte[]{0x07, 0x00, 0x0B, 0x00, 0x0F, 0x00, 0x17, 0x1E, 0x00, 0x00});
                TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
                        new byte[]{0x01, 0x00, 0x00});
//
//                TelinkLightService.Instance().sendCommandNoResponse((byte) 0xFE, 0xFFFF,
//                        new byte[]{0x07, 0x00, 0x0B, 0x00, 0x0F, 0x00, 0x17, 0x1E, 0x00, 0x00});
//                TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
//                        new byte[]{0x00, 0x00, 0x00});
//                TelinkLightService.Instance().updateNotification();
                /*light.status = ConnectionStatus.ON;
                light.icon = R.drawable.icon_light_on;
                notifyDataSetChanged();*/
//                }
            } else if (light.status == ConnectionStatus.ON) {

//                if (TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
//                        new byte[]{0x00, 0x00, 0x00})) {

//                TelinkLightService.Instance().sendCommandNoResponse((byte) 0xFE, 0xFFFF,
//                        new byte[]{0x07, 0x00, 0x0B, 0x00, 0x0F, 0x00, 0x17, 0x1E, 0x00, 0x00});
                TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
                        new byte[]{0x00, 0x00, 0x00});
//                TelinkLightService.Instance().updateNotification();
//                TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
//                        new byte[]{0x01, 0x00, 0x00});
                /*light.status = ConnectionStatus.OFF;
                light.icon = R.drawable.icon_light_off;
                notifyDataSetChanged();*/
//                }
            }
        }
    };

    private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {

            Intent intent = new Intent(getActivity(),
                    DeviceSettingActivity.class);
            Light light = adapter.getItem(position);
            intent.putExtra("meshAddress", light.meshAddress);
            startActivity(intent);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this.getActivity();
        this.adapter = new DeviceListAdapter();
        mIntervalHandler = new Handler();
        onOff = false;
        testStarted = false;
        testCount = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIntervalHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.inflater = inflater;

        View view = inflater.inflate(R.layout.fragment_device_list, null);

        GridView listView = (GridView) view.findViewById(R.id.list_devices);

        listView.setOnItemClickListener(this.itemClickListener);
        listView.setOnItemLongClickListener(this.itemLongClickListener);
        listView.setAdapter(this.adapter);

        this.backView = (Button) view.findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.editView = (ImageView) view
                .findViewById(R.id.img_header_menu_right);
        this.editView.setOnClickListener(this.clickListener);

        this.btnAllOn = (Button) view.findViewById(R.id.btn_on);
        this.btnAllOn.setOnClickListener(this.clickListener);

        this.btnAllOff = (Button) view.findViewById(R.id.btn_off);
        this.btnAllOff.setOnClickListener(this.clickListener);

        this.btnOta = (Button) view.findViewById(R.id.btn_ota);
        this.btnOta.setOnClickListener(this.clickListener);

        this.txtSendInterval = (EditText) view.findViewById(R.id.sendInterval);
        this.txtSendNumbers = (EditText) view.findViewById(R.id.sendNumbers);
        this.txtNotifyCount = (TextView) view.findViewById(R.id.notifyCount);
        this.log = (TextView) view.findViewById(R.id.log);
        this.log.setOnClickListener(this.clickListener);
        view.findViewById(R.id.userAll).setOnClickListener(this.clickListener);

        et_adr = (EditText) view.findViewById(R.id.et_adr);
        et_interval = (EditText) view.findViewById(R.id.et_interval);
        btn_start_test = (Button) view.findViewById(R.id.btn_start_test);
        btn_start_test.setOnClickListener(this.clickListener);
        btn_online_status = (Button) view.findViewById(R.id.online_status);
        btn_online_status.setOnClickListener(this.clickListener);

        tv_test_count = (TextView) view.findViewById(R.id.tv_test_count);
        return view;
    }

    public void addDevice(Light light) {
        this.adapter.add(light);
    }

    public Light getDevice(int meshAddress) {
        return this.adapter.get(meshAddress);
    }

    public void notifyDataSetChanged() {
        if (this.adapter != null)
            this.adapter.notifyDataSetChanged();
    }


    private static void hidSoftInput(Context context, IBinder token) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(token, 0);
        } catch (Exception e) {
        }
    }

    private static class DeviceItemHolder {
        public ImageView statusIcon;
        public TextView txtName;
    }

    final class DeviceListAdapter extends BaseAdapter {

        public DeviceListAdapter() {

        }

        @Override
        public int getCount() {
            return Lights.getInstance().size();
        }

        @Override
        public Light getItem(int position) {
            return Lights.getInstance().get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            DeviceItemHolder holder;

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.device_item, null);

                ImageView statusIcon = (ImageView) convertView
                        .findViewById(R.id.img_icon);
                TextView txtName = (TextView) convertView
                        .findViewById(R.id.txt_name);

                holder = new DeviceItemHolder();

                holder.statusIcon = statusIcon;
                holder.txtName = txtName;

                convertView.setTag(holder);
            } else {
                holder = (DeviceItemHolder) convertView.getTag();
            }

            Light light = this.getItem(position);

            holder.txtName.setText(light.getLabel());
            holder.txtName.setTextColor(light.textColor);
            holder.statusIcon.setImageResource(light.icon);

            Log.d("dadouLog",light.getLabel()+"-----"+light.textColor+"-----"+light.icon);
            return convertView;
        }

        public void add(Light light) {
            Lights.getInstance().add(light);
        }

        public Light get(int meshAddress) {
            return Lights.getInstance().getByMeshAddress(meshAddress);
        }
    }

}
