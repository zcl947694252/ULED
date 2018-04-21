package com.dadoutek.uled.activity;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.util.DataManager;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.NotificationInfo;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.model.Groups;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.ArrayList;
import java.util.List;

public final class DeviceGroupingActivity extends TelinkBaseActivity implements EventListener {

    private final static int UPDATE = 1;

    private LayoutInflater inflater;
    private GroupListAdapter adapter;
    private Groups groupsInit;

    private int meshAddress;
    private DataManager mDataManager;

    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            finish();
        }
    };
    private OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            Group group = adapter.getItem(position);
            deleteLightFromOldGroup();
            allocDeviceGroup(group);
            saveInfo(position);
        }
    };

    private void deleteLightFromOldGroup() {
//        Light light = Lights.getInstance().getByMeshAddress(meshAddress);
        for (Group group :
                Groups.getInstance().get()) {
            for (int i = 0; i < group.containsLightList.size(); i++) {
                if (group.containsLightList.get(i) == meshAddress) {
                    group.containsLightList.remove(i);
                }
            }
        }

    }

    private void saveInfo(int position) {
        Groups groups = Groups.getInstance();
        if (groups.get(position).containsLightList == null) {
            groups.get(position).containsLightList = new ArrayList<>();
        }
        if (groups.get(position).containsLightList.size() == 0) {
            groups.get(position).containsLightList.add(meshAddress);
        }
        if (!groups.get(position).containsLightList.contains(meshAddress)) {
            groups.get(position).containsLightList.add(meshAddress);
        }

        mDataManager.updateGroup(groups);
        finish();
//        Lights lights=DataManager.getLights();
//        for(int i=0;){
//
//        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case UPDATE:
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private TelinkLightApplication mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mApplication = (TelinkLightApplication) this.getApplication();
        this.mApplication.addEventListener(NotificationEvent.GET_GROUP, this);


        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_device_grouping);

        this.meshAddress = this.getIntent().getIntExtra("meshAddress", 0);

        this.inflater = this.getLayoutInflater();
        this.adapter = new GroupListAdapter();

        ImageView backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        backView.setOnClickListener(this.clickListener);

        GridView listView = (GridView) this.findViewById(R.id.list_groups);
        listView.setOnItemClickListener(this.itemClickListener);
        listView.setAdapter(this.adapter);


        this.initData();
        this.getDeviceGroup();
    }

    private void initData() {
        Groups.getInstance().clear();
        mDataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);

        groupsInit = mDataManager.getGroups();

        for (int i = 0; i < groupsInit.size(); i++) {
            Groups.getInstance().add(groupsInit.get(i));
        }

        this.adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mApplication.removeEventListener(this);
    }

    private void getDeviceGroup() {
        byte opcode = (byte) 0xDD;
        int dstAddress = this.meshAddress;
        byte[] params = new byte[]{0x08, 0x01};

        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
        TelinkLightService.Instance().updateNotification();
    }

    private void allocDeviceGroup(Group group) {

        int groupAddress = group.meshAddress;
        int dstAddress = this.meshAddress;
        byte opcode = (byte) 0xD7;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};

        if (!group.checked) {
            params[0] = 0x01;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
            group.containsLightList.add(meshAddress);

        } else {
            params[0] = 0x00;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
        }
    }

    @Override
    public void performed(Event event) {

        if (event.getType() == NotificationEvent.GET_GROUP) {
            NotificationEvent e = (NotificationEvent) event;
            NotificationInfo info = e.getArgs();

            int srcAddress = info.src & 0xFF;
            byte[] params = info.params;

            if (srcAddress != this.meshAddress)
                return;

            int count = this.adapter.getCount();

            Group group;

            for (int i = 0; i < count; i++) {
                group = this.adapter.getItem(i);

                if (group != null)
                    group.checked = false;
            }

            int groupAddress;
            int len = params.length;

            for (int j = 0; j < len; j++) {

                groupAddress = params[j];

                if (groupAddress == 0x00 || groupAddress == 0xFF)
                    break;

                groupAddress = groupAddress | 0x8000;

                group = this.adapter.get(groupAddress);

                if (group != null) {
                    group.checked = true;
                }
            }

            mHandler.obtainMessage(UPDATE).sendToTarget();
        }
    }

    private static class GroupItemHolder {
        public TextView name;
    }

    private final class GroupListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Groups.getInstance().size();
        }

        @Override
        public Group getItem(int position) {
            return Groups.getInstance().get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public Group get(int addr) {
            return Groups.getInstance().getByMeshAddress(addr);
        }

        @Override
        @Deprecated
        public View getView(int position, View convertView, ViewGroup parent) {

            GroupItemHolder holder;

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.grouping_item, null);

                TextView txtName = (TextView) convertView
                        .findViewById(R.id.txt_name);

                holder = new GroupItemHolder();
                holder.name = txtName;

                convertView.setTag(holder);

            } else {
                holder = (GroupItemHolder) convertView.getTag();
            }

            Group group = this.getItem(position);

            if (group != null) {
                holder.name.setText(group.name);

                Activity mContext = DeviceGroupingActivity.this;
                if (group.checked) {
                    ColorStateList color = mContext.getResources()
                            .getColorStateList(R.color.theme_positive_color);
                    holder.name.setTextColor(color);
                } else {
                    ColorStateList color = mContext.getResources()
                            .getColorStateList(R.color.black);
                    holder.name.setTextColor(color);
                }

            }

            return convertView;
        }
    }
}
