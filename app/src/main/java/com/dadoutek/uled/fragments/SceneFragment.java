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

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.SceneSetAct;
import com.dadoutek.uled.adapter.SceneAdaper;
import com.dadoutek.uled.intf.AdapterOnClickListner;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Scenes;
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
    private List<Scenes> scenesListData;
    private boolean isDelete=false;

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
        telinkLightApplication= (TelinkLightApplication) this.getActivity().getApplication();
        dataManager=new DataManager(getActivity(),telinkLightApplication.getMesh().name,telinkLightApplication.getMesh().password);
        scenesListData=dataManager.getScenesList();
    }

    private void initView() {
        adaper= new SceneAdaper(scenesListData,getActivity(),isDelete,adapterOnClickListner);
        sceneList.setAdapter(adaper);
    }

    private void refreshData(){
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

    @OnClick({R.id.img_header_menu_left, R.id.img_header_menu_right})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                if(isDelete){
                    isDelete=false;
                }else {
                    isDelete=true;
                }

                adaper.changeState(isDelete);
                refreshData();
                break;
            case R.id.img_header_menu_right:
                Intent intent=new Intent(getActivity(), SceneSetAct.class);
                startActivityForResult(intent,0);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0&&resultCode== Constant.RESULT_OK){
            initData();
            initView();
        }
    }

    AdapterOnClickListner adapterOnClickListner=new AdapterOnClickListner() {
        @Override
        public void adapterOnClick(View v, int position) {
            if(v.getId()==R.id.scene_delete){
                dataManager.deleteScene(scenesListData.get(position));
                scenesListData.remove(position);
                refreshData();
            }else if(v.getId()==R.id.scene_apply){
                try {
                    setScene(position);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void setScene(int position) throws InterruptedException {
        byte opcodeBn;
        byte[] paramsBn;
        byte opcodeTT;
        byte[] paramsTT;
        Scenes scenes=scenesListData.get(position);
        opcodeBn = (byte) 0xD2;
        opcodeTT = (byte) 0xE2;
        paramsBn = new byte[]{(byte) scenes.brightness};
        paramsTT = new byte[]{0x05, (byte) scenes.temperature};

        List<Integer> groupsAddressList=scenes.groupsAddressList;

        if(groupsAddressList.size()>0){
            for(int i=0;i<groupsAddressList.size();i++){
                Thread.sleep(100);
                TelinkLightService.Instance().sendCommandNoResponse(opcodeBn, groupsAddressList.get(i), paramsBn);
                Thread.sleep(100);
                TelinkLightService.Instance().sendCommandNoResponse(opcodeTT, groupsAddressList.get(i), paramsTT);
            }
        }
    }
}
