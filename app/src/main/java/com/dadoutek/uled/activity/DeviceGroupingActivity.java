package com.dadoutek.uled.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.StringUtils;
import com.telink.bluetooth.event.NotificationEvent;
import com.telink.bluetooth.light.NotificationInfo;
import com.telink.util.Event;
import com.telink.util.EventListener;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;


public final class DeviceGroupingActivity extends TelinkBaseActivity implements EventListener {

    private final static int UPDATE = 1;

    private LayoutInflater inflater;
    private GroupListAdapter adapter;
    private List<DbGroup> groupsInit;

    private DbLight light;
    private int gpAdress;

    private Button btnAdd;

    private OnClickListener clickListener = v -> finish();

    private GridView listView;
    private OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            DbGroup group = adapter.getItem(position);
            if (group.checked) {
                ToastUtils.showLong(R.string.tip_selected_group);
                finish();
            } else {
                deletePreGroup();
                allocDeviceGroup(group);
                saveInfo();
            }
        }
    };

    private void deletePreGroup() {
        int groupAddress = DBUtils.getGroupByID(light.getBelongGroupId()).getMeshAddr();
        int dstAddress = light.getMeshAddr();
        byte opcode = (byte) 0xD7;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};
        params[0] = 0x00;
        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
    }

    private void saveInfo() {
        DBUtils.updateLight(light);
        finish();
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

        this.light = (DbLight) this.getIntent().getExtras().get("light");
        this.gpAdress = this.getIntent().getIntExtra("gpAddress", 0);

        this.inflater = this.getLayoutInflater();
        this.adapter = new GroupListAdapter();
        btnAdd=findViewById(R.id.add_group_new);

        ImageView backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        backView.setOnClickListener(this.clickListener);

        this.initData();

        listView = (GridView) this.findViewById(R.id.list_groups);
        listView.setOnItemClickListener(this.itemClickListener);
        listView.setAdapter(this.adapter);
        btnAdd.setOnClickListener(v -> addNewGroup());

        this.getDeviceGroup();
    }

    private void initData() {
        groupsInit = DBUtils.getGroupList();
        this.adapter.notifyDataSetChanged();

//        for(int i=0;i<groupsInit.size();i++){
//            if(groupsInit.get(i).getMeshAddr()==gpAdress){
//                groupsInit.get(i).checked=true;
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mApplication.removeEventListener(this);
    }

    private void getDeviceGroup() {
        byte opcode = (byte) 0xDD;
        int dstAddress = light.getMeshAddr();
        byte[] params = new byte[]{0x08, 0x01};

        TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
        TelinkLightService.Instance().updateNotification();
    }

    private void allocDeviceGroup(DbGroup group) {

        int groupAddress = group.getMeshAddr();
        int dstAddress = light.getMeshAddr();
        byte opcode = (byte) 0xD7;
        byte[] params = new byte[]{0x01, (byte) (groupAddress & 0xFF),
                (byte) (groupAddress >> 8 & 0xFF)};

//        if (!group.checked) {
            params[0] = 0x01;
            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
            light.setBelongGroupId(group.getId());
//
//        } else {
//            params[0] = 0x00;
//            TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params);
//        }
    }

    @Override
    public void performed(Event event) {

        if (event.getType() == NotificationEvent.GET_GROUP) {
            NotificationEvent e = (NotificationEvent) event;
            NotificationInfo info = e.getArgs();

            int srcAddress = info.src & 0xFF;
            byte[] params = info.params;

            if (srcAddress != light.getMeshAddr())
                return;

            int count = this.adapter.getCount();

            DbGroup group;

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

    private void addNewGroup() {
            final EditText textGp = new EditText(this);
            new AlertDialog.Builder(DeviceGroupingActivity.this)
                    .setTitle(R.string.create_new_group)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(textGp)

                    .setPositiveButton(getString(R.string.btn_sure), (dialog, which) -> {
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.getText().toString().trim())) {
                            ToastUtils.showShort(getString(R.string.rename_tip_check));
                        } else {
                            //往DB里添加组数据
                            DBUtils.addNewGroup(textGp.getText().toString().trim(), groupsInit, this);
                            refreshView();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> {
                        dialog.dismiss();
                    }).show();
    }

    private void refreshView() {
        groupsInit = DBUtils.getGroupList();

        for(int k=0;k<groupsInit.size();k++){
            if(k==groupsInit.size()-1){
                groupsInit.get(k).checked=true;
                deletePreGroup();
                allocDeviceGroup(groupsInit.get(k));
                light.setBelongGroupId(groupsInit.get(k).getId());
                DBUtils.updateLight(light);
            }else{
                groupsInit.get(k).checked=false;
            }
        }

        adapter=new GroupListAdapter();
        listView.setAdapter(this.adapter);
        adapter.notifyDataSetInvalidated();
    }

    private static class GroupItemHolder {
        public TextView name;
    }

    private final class GroupListAdapter extends BaseAdapter {

        public GroupListAdapter() {
        }

        @Override
        public int getCount() {
            return groupsInit.size();
        }

        @Override
        public DbGroup getItem(int position) {
            return groupsInit.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public DbGroup get(int addr) {
            for (int j = 0; j < groupsInit.size(); j++) {
                if (addr == groupsInit.get(j).getMeshAddr()) {
                    return groupsInit.get(j);
                }
            }
            return null;
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

            DbGroup group = this.getItem(position);

            if (group != null) {
                holder.name.setText(group.getName());

                Activity mContext = DeviceGroupingActivity.this;
                if (group.checked) {
                    ColorStateList color = mContext.getResources()
                            .getColorStateList(R.color.primary);
                    holder.name.setTextColor(color);
                } else {
                    ColorStateList color = mContext.getResources()
                            .getColorStateList(R.color.black);
                    holder.name.setTextColor(color);
                }

            }

            if (position == 0) {
                AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(1, 1);
                convertView.setLayoutParams(layoutParams);
            }

            return convertView;
        }
    }
}
