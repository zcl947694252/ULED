package com.dadoutek.uled.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemDragListener;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.AddMeshActivity;
import com.dadoutek.uled.activity.DeviceScanningNewActivity;
import com.dadoutek.uled.activity.GroupSettingActivity;
import com.dadoutek.uled.activity.LightsOfGroupActivity;
import com.dadoutek.uled.activity.ScanningSwitchActivity;
import com.dadoutek.uled.adapter.GroupListRecycleViewAdapter;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public final class GroupListFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    private LayoutInflater inflater;
    private GroupListRecycleViewAdapter adapter;

    private Activity mContext;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;
    private List<DbGroup> gpList;
    private TelinkLightApplication application;
    private Toolbar toolbar;

    //    private GridView gridView;
    private RecyclerView recyclerView;
    List<DbGroup> showList;


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
        setHasOptionsMenu(true);

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

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.group_list_header);
        toolbar.inflateMenu(R.menu.men_group);
        toolbar.setOnMenuItemClickListener(this);
        if (SharedPreferencesUtils.isDeveloperModel()) {
            toolbar.getMenu().findItem(R.id.menu_setting).setVisible(false);
        } else {
            toolbar.getMenu().findItem(R.id.menu_setting).setVisible(false);
        }

        setHasOptionsMenu(true);

        recyclerView = view.findViewById(R.id.list_groups);

        this.initData();
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser) {

        }
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
        gpList = DBUtils.getGroupList();

        showList = new ArrayList<>();

        List<DbGroup> dbOldGroupList = (List<DbGroup>) SharedPreferencesHelper.
                getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA);

        //如果有调整过顺序取本地数据，否则取数据库数据
        if (dbOldGroupList != null && dbOldGroupList.size() > 0) {
            showList = dbOldGroupList;
        } else {
            showList = gpList;
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(getActivity());
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutmanager);
        this.adapter = new GroupListRecycleViewAdapter(R.layout.group_item, showList);
        adapter.setOnItemChildClickListener(onItemChildClickListener);
        adapter.bindToRecyclerView(recyclerView);
//        adapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN);
        setMove();

        application = (TelinkLightApplication) getActivity().getApplication();
        dataManager = new DataManager(TelinkLightApplication.getInstance(),
                application.getMesh().name, application.getMesh().password);
    }

    private void setMove() {
        OnItemDragListener onItemDragListener = new OnItemDragListener() {
            @Override
            public void onItemDragStart(RecyclerView.ViewHolder viewHolder, int pos) {
            }

            @Override
            public void onItemDragMoving(RecyclerView.ViewHolder source, int from,
                                         RecyclerView.ViewHolder target, int to) {

            }

            @Override
            public void onItemDragEnd(RecyclerView.ViewHolder viewHolder, int pos) {
//                viewHolder.getItemId();
                List<DbGroup> list = adapter.getData();
                SharedPreferencesHelper.putObject(getActivity(), Constant.OLD_INDEX_DATA, list);
            }
        };

        ItemDragAndSwipeCallback itemDragAndSwipeCallback = new ItemDragAndSwipeCallback(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemDragAndSwipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        adapter.enableDragItem(itemTouchHelper, R.id.txt_name, true);
        adapter.setOnItemDragListener(onItemDragListener);
    }


    BaseQuickAdapter.OnItemChildClickListener onItemChildClickListener = (adapter, view, position) -> {

        DbGroup group = showList.get(position);

        byte opcode = (byte) 0xD0;
        int dstAddr = group.getMeshAddr();
        Intent intent;

        if (!dataManager.getConnectState(getActivity())) {
            return;
        }

        switch (view.getId()) {
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
                intent.putExtra("group", group);
                startActivityForResult(intent, 0);
                break;
            case R.id.txt_name:
                intent = new Intent(mContext, LightsOfGroupActivity.class);
                intent.putExtra("group", group);
                startActivity(intent);
                break;
        }
    };

    public void notifyDataSetChanged() {
        this.adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setting:
                Intent intent = new Intent(mContext, AddMeshActivity.class);
                startActivity(intent);
                break;

            case R.id.menu_install:
                showPopupMenu(toolbar.findViewById(R.id.menu_install));
                break;
        }
        return false;
    }

    private void showPopupMenu(View view) {
        // 这里的view代表popupMenu需要依附的view
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_select_device_type, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.popup_install_light:
                    startActivity(new Intent(mContext, DeviceScanningNewActivity.class));
                    break;
                case R.id.popup_install_switch:
                    startActivity(new Intent(mContext, ScanningSwitchActivity.class));
                    break;
            }
            return true;
        });

    }
}
