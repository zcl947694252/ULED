package com.dadoutek.uled.scene;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.hubert.guide.core.Builder;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbSceneActions;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.othersview.BaseFragment;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.GuideUtils;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class SceneFragment extends BaseFragment implements
        Toolbar.OnMenuItemClickListener, View.OnClickListener {

    private static final int SCENE_MAX_COUNT = 16;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    Unbinder unbinder;
    private LayoutInflater layoutInflater;
    private SceneRecycleListAdapter adaper;

    private Toolbar toolbar;
    TextView toolbarTitle;

    private TelinkLightApplication telinkLightApplication;
    //    private List<Scenes> scenesListData;
    private List<DbScene> scenesListData;
    private boolean isDelete = false;
    Builder builder = null;
    private boolean guideShowCurrentPage=false;

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
        initToolBar(view);
        initData();
        initView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void initToolBar(View view) {
        setHasOptionsMenu(true);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.SceneTitle);

        ImageView btn_add = toolbar.findViewById(R.id.img_function1);
        ImageView btn_delete = toolbar.findViewById(R.id.img_function2);

        btn_add.setVisibility(View.VISIBLE);
        btn_delete.setVisibility(View.VISIBLE);

        btn_add.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
    }

//    public void initLoad(){
//        ImageView guide1= (ImageView) toolbar.findViewById(R.id.img_function1);
//        ImageView guide2= (ImageView) toolbar.findViewById(R.id.img_function2);
//        Builder builder = GuideUtils.guideBuilder(this,Constant.TAG_SceneFragment)
//                .addGuidePage(GuideUtils.addGuidePage(guide1,R.layout.view_guide_simple,getString(R.string.scene_guide_1)))
//                .addGuidePage(GuideUtils.addGuidePage(guide2,R.layout.view_guide_simple,getString(R.string.scene_guide_2)));
//        builder.setOnGuideChangedListener(new OnGuideChangedListener() {
//            @Override
//            public void onShowed(Controller controller) {
//
//            }
//
//            @Override
//            public void onRemoved(Controller controller) {
//                initOnLayoutListener(1);
////                lazyLoad(1);
//            }
//        });
//        builder.show();
//    }

    public void lazyLoad(int showTypeView) {
        step1Guide();
//        ImageView guide1= (ImageView) toolbar.findViewById(R.id.img_function1);
//        ImageView guide2= (ImageView) toolbar.findViewById(R.id.img_function2);
//                builder.addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide1,R.layout.view_guide_simple_scene_set1,getString(R.string.scene_guide_1)))
//                .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide2,R.layout.view_guide_simple_scene_set1,getString(R.string.scene_guide_2)));
//        boolean isOldUser=SharedPreferencesHelper.getBoolean(getActivity(),"Older",false);
//        if (adaper.getItemCount() != 0 && showTypeView==1) {
//            TextView guide3= (TextView) adaper.getViewByPosition(0, R.id.scene_name);
//            Button guide4= (Button) adaper.getViewByPosition(0, R.id.scene_edit);
//
//            int size= adaper.getItemCount();
//            if(guide3==null){
//                for(int i=0;i<size;i++){
//                    if(adaper.getViewByPosition(i,R.id.txt_name)!=null){
//                        guide3= (TextView) adaper.getViewByPosition(i+1, R.id.scene_name);
//                        guide4= (Button) adaper.getViewByPosition(i+1, R.id.scene_edit);
//                        break;
//                    }
//                }
//            }
//
//            builder.addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide3,R.layout.view_guide_simple_scene_set2,getString(R.string.scene_guide_3)))
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide4,R.layout.view_guide_simple_scene_set2,getString(R.string.scene_guide_4)));
//            SharedPreferencesHelper.putBoolean(getActivity(),"Older",true);
//            builder.show();
//        }else if(adaper.getItemCount()==1  && showTypeView==0 && !isOldUser){
//            TextView guide3= (TextView) adaper.getViewByPosition(0, R.id.scene_name);
//            Button guide4= (Button) adaper.getViewByPosition(0, R.id.scene_edit);
//            GuideUtils.INSTANCE.guideBuilder(this,Constant.TAG_SceneFragment1)
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide3,R.layout.view_guide_simple_scene_set2,getString(R.string.scene_guide_3)))
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide4,R.layout.view_guide_simple_scene_set2,getString(R.string.scene_guide_4)))
//                    .show();
//        }
    }

    private void step1Guide(){
        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(getActivity(),GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
        if(guideShowCurrentPage){
            GuideUtils.INSTANCE.resetSceneGuide(getActivity());
            ImageView guide1= (ImageView) toolbar.findViewById(R.id.img_function1);
            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP7_GUIDE_ADD_SCENE())
                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide1,R.layout.view_guide_simple_scene_1,getString(R.string.scene_guide_1),
                            v -> {
                              guide1.performClick();
                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),getActivity())).show();
        }
    }

    private void stepEndGuide(){
        if(getActivity()!=null){
            final View view = getActivity().getWindow().getDecorView();
            final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    TextView guide2= (TextView) adaper.getViewByPosition(0, R.id.scene_name);
                    GuideUtils.INSTANCE.guideBuilder(SceneFragment.this,GuideUtils.INSTANCE.getSTEP14_GUIDE_APPLY_SCENE())
                            .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide2,R.layout.view_guide_simple_scene_2,getString(R.string.apply_scene),
                                    v -> {
                                        guide2.performClick();
                                        GuideUtils.INSTANCE.changeCurrentViewIsEnd(getActivity(),GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),true);
                                    },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),getActivity())).show();
                }
            });
        }
    }

    private void initOnLayoutListener(int showTypeView) {
            if(getActivity()!=null){
                final View view = getActivity().getWindow().getDecorView();
                final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        lazyLoad(showTypeView);
                    }
                });
            }
//        }
    }

    private void initData() {
        telinkLightApplication = (TelinkLightApplication) this.getActivity().getApplication();
        scenesListData = DBUtils.INSTANCE.getSceneList();
    }

    private void initView() {

        LinearLayoutManager layoutmanager = new LinearLayoutManager(getActivity());
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutmanager);
        //添加分割线
        DividerItemDecoration decoration = new DividerItemDecoration(getActivity(),
                DividerItemDecoration
                        .VERTICAL);
        decoration.setDrawable(new ColorDrawable(ContextCompat.getColor(getActivity(), R.color
                .divider)));
        recyclerView.addItemDecoration(decoration);
        //添加Item变化动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adaper = new SceneRecycleListAdapter(R.layout.item_scene, scenesListData, isDelete);
        adaper.setOnItemClickListener(onItemClickListener);
        adaper.setOnItemChildClickListener(onItemChildClickListener);
        adaper.bindToRecyclerView(recyclerView);
    }

    BaseQuickAdapter.OnItemClickListener onItemClickListener = (adapter, view, position) -> {
        setScene(scenesListData.get(position).getId());
    };

    BaseQuickAdapter.OnItemChildClickListener onItemChildClickListener = (adapter, view, position) -> {
        if (view.getId() == R.id.scene_delete) {
//                dataManager.deleteScene(scenesListData.get(position));
            deleteScene(position);
            adapter.notifyItemRemoved(position);

//            refreshData();
        } else if (view.getId() == R.id.scene_edit) {
//                setScene(scenesListData.get(position).getId());
            DbScene scene = scenesListData.get(position);
            Intent intent = new Intent(getActivity(), NewSceneSetAct.class);
            intent.putExtra(Constant.CURRENT_SELECT_SCENE, scene);
            intent.putExtra(Constant.IS_CHANGE_SCENE, true);
            startActivityForResult(intent, 0);
        }
    };

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
            initData();
            initView();
            stepEndGuide();
        }
    }

    synchronized
    private void deleteScene(int position) {
        byte opcode = Opcode.SCENE_ADD_OR_DEL;
        byte[] params;
        if (scenesListData.size() > 0) {
            long id = scenesListData.get(position).getId();
            List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
            params = new byte[]{0x00, (byte) id};
            new Thread(() -> {
                TelinkLightService.Instance().sendCommandNoResponse(opcode, 0xFFFF, params);
            }).start();
            DBUtils.INSTANCE.deleteSceneActionsList(list);
            DBUtils.INSTANCE.deleteScene(scenesListData.get(position));
            scenesListData.remove(position);
        }
    }

    private void setScene(long id) {
        byte opcode = Opcode.SCENE_LOAD;
        List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
        new Thread(() -> {
            byte[] params;
            params = new byte[]{(byte) id};
            TelinkLightService.Instance().sendCommandNoResponse(opcode, 0xFFFF, params);
        }).start();

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        builder = GuideUtils.INSTANCE.guideBuilder(this,Constant.TAG_SceneFragment);
        if (isVisibleToUser) {
//            initData();
//            initView();
//            showLoadingDialog("ss");
            initOnLayoutListener(1);
        }else{
            if(getActivity()!=null){
//                initOnLayoutListener(2);
            }
        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                if (isDelete) {
                    isDelete = false;
                } else {
                    isDelete = true;
                }

                adaper.changeState(isDelete);
                refreshData();
                break;
            case R.id.menu_install:
                if (!SharedPreferencesUtils.getConnectState(getActivity())) {
//                    return;
                } else {
                    if (scenesListData.size() >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip);
                    } else {
                        Intent intent = new Intent(getActivity(), NewSceneSetAct.class);
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false);
                        startActivityForResult(intent, 0);
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_function2:
                if (isDelete) {
                    isDelete = false;
                } else {
                    isDelete = true;
                }

                adaper.changeState(isDelete);
                refreshData();
                break;
            case R.id.img_function1:
                if (!SharedPreferencesUtils.getConnectState(getActivity())) {
//                    return;
                } else {
                    if (scenesListData.size() >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip);
                    } else {
                        if(TelinkLightApplication.getInstance().getConnectDevice()==null){
                            ToastUtils.showLong(R.string.device_not_connected);
                        }else{
                            Intent intent = new Intent(getActivity(), NewSceneSetAct.class);
                            intent.putExtra(Constant.IS_CHANGE_SCENE, false);
                            startActivityForResult(intent, 0);
                        }
                    }
                }
                break;
        }
    }
}
