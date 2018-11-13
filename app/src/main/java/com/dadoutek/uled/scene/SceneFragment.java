package com.dadoutek.uled.scene;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.hubert.guide.NewbieGuide;
import com.app.hubert.guide.model.GuidePage;
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

    private void initToolBar(View view) {
        setHasOptionsMenu(true);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.SceneSetting);

        ImageView btn_add = toolbar.findViewById(R.id.img_function1);
        ImageView btn_delete = toolbar.findViewById(R.id.img_function2);

        btn_add.setVisibility(View.VISIBLE);
        btn_delete.setVisibility(View.VISIBLE);

        btn_add.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
    }

    public void lazyLoad() {
        NewbieGuide.with(this)
                //.alwaysShow(true)
                .setLabel("scene")
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight((ImageView) toolbar.findViewById(R.id.img_function1))
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener(view -> {
                            TextView tv_Guide = view.findViewById(R.id.show_guide_content);
                            tv_Guide.setText(R.string.scene_guide_1);
                        })
                )
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight((ImageView) toolbar.findViewById(R.id.img_function2))
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener(view -> {
                            TextView tv_Guide = view.findViewById(R.id.show_guide_content);
                            tv_Guide.setText(R.string.scene_guide_2);
                        })
                ).show();

        if (adaper.getItemCount() != 0) {
            TextView tvApply = (TextView) adaper.getViewByPosition(0, R.id.scene_name);
            Button btnUpdate = (Button) adaper.getViewByPosition(0, R.id.scene_edit);
            NewbieGuide.with(this)
                    //.alwaysShow(true)
                    .setLabel("sceneList")
                    .addGuidePage(GuidePage.newInstance()
                            .addHighLight(tvApply)
                            .setLayoutRes(R.layout.view_guide_simple)
                            .setOnLayoutInflatedListener(view -> {
                                TextView tv_Guide = view.findViewById(R.id.show_guide_content);
                                tv_Guide.setText(R.string.scene_guide_3);
                            })
                    )
                    .addGuidePage(GuidePage.newInstance()
                            .addHighLight(btnUpdate)
                            .setLayoutRes(R.layout.view_guide_simple)
                            .setOnLayoutInflatedListener(view -> {
                                TextView tv_Guide = view.findViewById(R.id.show_guide_content);
                                tv_Guide.setText(R.string.scene_guide_4);
                            })
                    ).show();
        }
    }

    private void initOnLayoutListener() {
        final View view = getActivity().getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                lazyLoad();
            }
        });
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
            Intent intent = new Intent(getActivity(), SetSceneAct.class);
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
            initOnLayoutListener();
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
        if (isVisibleToUser) {
//            initData();
//            initView();
            initOnLayoutListener();
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
                        Intent intent = new Intent(getActivity(), SetSceneAct.class);
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
                        Intent intent = new Intent(getActivity(), SetSceneAct.class);
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false);
                        startActivityForResult(intent, 0);
                    }
                }
                break;
        }
    }
}
