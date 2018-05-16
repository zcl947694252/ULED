package com.dadoutek.uled.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.DbModel.DbSceneActions;
import com.dadoutek.uled.DbModel.DbSceneActionsUtils;
import com.dadoutek.uled.DbModel.DbSceneUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.AddSceneAct;
import com.dadoutek.uled.adapter.SceneAdaper;
import com.dadoutek.uled.intf.AdapterOnClickListner;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.util.DataManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class SceneFragment extends Fragment {

    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.img_header_menu_right)
    ImageView imgHeaderMenuRight;
    @BindView(R.id.scene_list)
    ListView sceneList;
    Unbinder unbinder;
    private LayoutInflater layoutInflater;
    private SceneAdaper adaper;

    private DataManager dataManager;
    private TelinkLightApplication telinkLightApplication;
    //    private List<Scenes> scenesListData;
    private List<DbScene> scenesListData;
    private boolean isDelete = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        layoutInflater = inflater;
        View view = layoutInflater.inflate(R.layout.fragment_scene, null);
        unbinder = ButterKnife.bind(this, view);
        initData();
        initView();
        return view;
    }

    private void initData() {
        telinkLightApplication = (TelinkLightApplication) this.getActivity().getApplication();
        dataManager = new DataManager(getActivity(), telinkLightApplication.getMesh().name, telinkLightApplication.getMesh().password);
        scenesListData = DbSceneUtils.getAllScene();
    }

    private void initView() {
        adaper = new SceneAdaper(scenesListData, getActivity(), isDelete, adapterOnClickListner);
        sceneList.setAdapter(adaper);
    }

    private void refreshData() {
        adaper.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            initData();
            initView();
        }
    }

    @OnClick({R.id.img_header_menu_left, R.id.img_header_menu_right})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                if (isDelete) {
                    isDelete = false;
                } else {
                    isDelete = true;
                }

                adaper.changeState(isDelete);
                refreshData();
                break;
            case R.id.img_header_menu_right:
                Intent intent = new Intent(getActivity(), AddSceneAct.class);
                startActivityForResult(intent, 0);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
            initData();
            initView();
        }
    }

    AdapterOnClickListner adapterOnClickListner = new AdapterOnClickListner() {
        @Override
        public void adapterOnClick(View v, int position) {
            if (v.getId() == R.id.scene_delete) {
//                dataManager.deleteScene(scenesListData.get(position));
                deleteScene(position);
                refreshData();
            } else if (v.getId() == R.id.scene_apply) {
                try {
                    setScene(scenesListData.get(position).getId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void deleteScene(int position) {
        byte opcode = (byte) 0xEE;
        byte[] params;
        long id = scenesListData.get(position).getId();
        List<DbSceneActions> list = DbSceneActionsUtils.searchActionsBySceneId(id);
        params = new byte[]{0x00, (byte) id};
        for (int i = 0; i < list.size(); i++) {
            try {
                Thread.sleep(100);
                TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DbSceneUtils.deleteScene(scenesListData.get(position));
        scenesListData.remove(position);
    }

    private void setScene(long id) throws InterruptedException {
        byte opcode = (byte) 0xEF;
        List<DbSceneActions> list = DbSceneActionsUtils.searchActionsBySceneId(id);
        byte[] params;
        for (int i = 0; i < list.size(); i++) {
            Thread.sleep(100);
            params = new byte[]{(byte) id};
            TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
        }
    }
}