package com.dadoutek.uled.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.LightsOfGroupActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.activity.GroupSettingActivity;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.DataManager;

import java.util.ArrayList;
import java.util.List;

public final class GroupListFragment extends Fragment {

    private LayoutInflater inflater;
    private GroupListAdapter adapter;

    private Activity mContext;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;


    private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {

//            Group group = adapter.getItem(position);
//
//            Intent intent = new Intent(mContext, GroupSettingActivity.class);
//            intent.putExtra("groupAddress", group.meshAddress);
//
//            startActivityForResult(intent, 0);

            return true;
        }
    };
    private GridView gridView;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
            this.initData();
            this.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.mContext = this.getActivity();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("dadougg", "onResume: ");
        this.initData();
        this.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.inflater = inflater;

        View view = inflater.inflate(R.layout.fragment_group_list, null);

        gridView = (GridView) view.findViewById(R.id.list_groups);
        gridView.setOnItemLongClickListener(this.itemLongClickListener);

        this.initData();
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden) {
            this.initData();
        }
    }

    private void initData() {
        this.mApplication = (TelinkLightApplication) getActivity().getApplication();
        dataManager = new DataManager(getActivity(), mApplication.getMesh().name, mApplication.getMesh().password);
        this.adapter = new GroupListAdapter(dataManager.getGroups());
        gridView.setAdapter(this.adapter);

    }

    public void notifyDataSetChanged() {
        this.adapter.notifyDataSetChanged();
    }

    private static class GroupItemHolder {
        public TextView txtName;
        public TextView btnOn;
        public TextView btnOff;
        public TextView btnSet;
    }

    final class GroupListAdapter extends BaseAdapter implements
            OnClickListener, OnLongClickListener {
        ArrayList<Group> groupArrayList = new ArrayList<>();

        public GroupListAdapter(Groups groups) {
            List<Group> groupList = groups.get();
            Mesh mesh = mApplication.getMesh();
            DataManager dataManager = new DataManager(getActivity(), mesh.name, mesh.password);
            groupArrayList.add(dataManager.createAllLightControllerGroup());
            for (Group group : groupList) {
                if (group.containsLightList.size() > 0)
                    groupArrayList.add(group);
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return groupArrayList.size();
        }

        @Override
        public Group getItem(int position) {
            return groupArrayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            GroupItemHolder holder;

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.group_item, null);

                TextView txtName = (TextView) convertView
                        .findViewById(R.id.txt_name);
                txtName.setOnClickListener(this);

                TextView btnOn = (TextView) convertView.findViewById(R.id.btn_on);
                btnOn.setOnClickListener(this);

                TextView btnOff = (TextView) convertView.findViewById(R.id.btn_off);
                btnOff.setOnClickListener(this);

                TextView btnSet = (TextView) convertView.findViewById(R.id.btn_set);
                btnSet.setOnClickListener(this);

                holder = new GroupItemHolder();

                holder.txtName = txtName;
                holder.btnOn = btnOn;
                holder.btnOff = btnOff;
                holder.btnSet=btnSet;

                convertView.setTag(holder);

            } else {
                holder = (GroupItemHolder) convertView.getTag();
            }

            Group group = this.getItem(position);

            if (group != null) {
                if (group.textColor == null)
                    group.textColor = mContext.getResources()
                            .getColorStateList(R.color.black);

                holder.txtName.setText(group.name);
                holder.txtName.setTextColor(group.textColor);
                holder.txtName.setTag(group.meshAddress);
                holder.btnOn.setTag(group.meshAddress);
                holder.btnOff.setTag(group.meshAddress);
                holder.btnSet.setTag(group.meshAddress);
            }

            return convertView;
        }

        @Override
        public void onClick(View view) {

            int clickId = view.getId();
            int meshAddress = (int) view.getTag();

            byte opcode = (byte) 0xD0;
            int dstAddr = meshAddress;
            Intent intent;

            if(!dataManager.getConnectState(getActivity())){
                return;
            }

            switch (clickId){
                case R.id.btn_on:
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
                            new byte[]{0x01, 0x00, 0x00});
                    break;
                case R.id.btn_off:
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddr,
                            new byte[]{0x00, 0x00, 0x00});
                    break;
                case R.id.btn_set:
                    intent = new Intent(mContext, GroupSettingActivity.class);
                    intent.putExtra("groupAddress", meshAddress);
                    startActivityForResult(intent, 0);
                    break;
                case R.id.txt_name:
                        intent = new Intent(mContext, LightsOfGroupActivity.class);
                        intent.putExtra("groupAddress", meshAddress);
                        startActivity(intent);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {

            return false;
        }
    }
}
