package com.dadoutek.uled.device;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dadoutek.uled.R;
import com.dadoutek.uled.light.LightsOfGroupRecyclerViewAdapter;
import com.dadoutek.uled.model.dbModel.DbLight;

import java.util.ArrayList;
import java.util.List;

public class NewDeviceAdapter extends BaseExpandableListAdapter {

    private Context mContext;

    /**
     * 每个分组的名字的集合
     */
    private List<String> deviceTypeList;

    /**
     * 所有分组的所有子项的 GridView 数据集合
     */
    private ArrayList<ArrayList<DbLight>> allDeviceList;

    private RecyclerView recyclerView;

    private LightsOfGroupRecyclerViewAdapter adapter = null;

    public NewDeviceAdapter(Context context, List<String> deviceTypeList,
                            ArrayList<ArrayList<DbLight>> lightList) {
        mContext = context;
        this.deviceTypeList = deviceTypeList;
        this.allDeviceList = lightList;
    }

    @Override
    public int getGroupCount() {
        return deviceTypeList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return allDeviceList.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return deviceTypeList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return allDeviceList.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if (null == convertView) {
            convertView = View.inflate(mContext, R.layout.item_device_type, null);
        }
        ImageView deviceTypeEnterImg = (ImageView) convertView.findViewById(R.id.deviceTypeEnterImg);
        TextView deviceTypeName = (TextView) convertView.findViewById(R.id.deviceTypeName);

        if (isExpanded) {
            deviceTypeEnterImg.setImageResource(R.drawable.enter_right);
        } else {
            deviceTypeEnterImg.setImageResource(R.drawable.enter_bottom);
        }
        // 设置分组组名
        deviceTypeName.setText(deviceTypeList.get(groupPosition));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = View.inflate(mContext, R.layout.item_type_list, null);
        }
        // 因为 convertView 的布局就是一个 GridView，
        // 所以可以向下转型为 GridView
        recyclerView = (RecyclerView) convertView;
        // 创建 GridView 适配器
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        adapter = new LightsOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, allDeviceList.get(groupPosition));
        adapter.setOnItemChildClickListener(childClickListener);
        adapter.bindToRecyclerView(recyclerView);
        for (int i = 0; i< allDeviceList.get(groupPosition).size(); i++) {
            allDeviceList.get(groupPosition).get(i).updateIcon();
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    BaseQuickAdapter.OnItemChildClickListener childClickListener = (adapter, view, position) -> {

    };
}
