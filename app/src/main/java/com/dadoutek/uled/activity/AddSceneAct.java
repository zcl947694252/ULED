package com.dadoutek.uled.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Scene;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.adapter.GroupListAdapter;
import com.dadoutek.uled.adapter.SceneGroupAdapter;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbSceneActions;
import com.dadoutek.uled.model.ItemGroup;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.Scenes;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class AddSceneAct extends TelinkBaseActivity {
    @BindView(R.id.bt_save)
    Button btSave;
    @BindView(R.id.edit_name)
    EditText editName;
    @BindView(R.id.btn_sure_edit)
    Button btnSureEdit;
    @BindView(R.id.scene_group_list_view)
    RecyclerView sceneGroupListView;
    @BindView(R.id.bt_add)
    Button btAdd;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Scenes scenes;
    private LayoutInflater inflater;
    private SceneGroupAdapter adapter;

    private DataManager dataManager;
    private TelinkLightApplication telinkLightApplication;
    private ArrayList<DbGroup> groupArrayList = new ArrayList<>();
    ArrayList<ItemGroup> itemGroupArrayList = new ArrayList<>();
    private ArrayList<String> groupNameArrayList = new ArrayList<>();
    private List<DbGroup> groups = new ArrayList<>();
    /**
     * 输入法管理器
     */
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_set);
        ButterKnife.bind(this);
        initToolbar();
        initData();
        initView();
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.create_scene);
        toolbar.setNavigationIcon(R.drawable.navigation_back);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initData() {
        scenes = new Scenes();
        telinkLightApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, telinkLightApplication.getMesh().name, telinkLightApplication.getMesh().password);
        groups = DBUtils.getGroupList();
        itemGroupArrayList = new ArrayList<>();

        for (DbGroup group : groups) {
//            if (group.containsLightList.size() > 0 || group.getMeshAddr() == 0xffff)
            group.selected = false;
            groupArrayList.add(group);
            groupNameArrayList.add(group.getName());
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        sceneGroupListView.setLayoutManager(layoutmanager);

        this.adapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList);
//        sceneGroupListView.setAdapter(adapter);
        adapter.bindToRecyclerView(sceneGroupListView);

        //删除时恢复可添加组标记
        adapter.setOnItemChildClickListener((adapter, view, position) -> {
            if (groupArrayList.size() != 0) {
                if (adapter.getItemCount() == 1) {
                    for (int k = 0; k < groupArrayList.size(); k++) {
                        groupArrayList.get(k).selected = false;
                    }
                    adapter.remove(position);
                } else {
                    for (int k = 0; k < groupArrayList.size(); k++) {
                        if (groupArrayList.get(k).getName().equals(itemGroupArrayList.get(position).gpName)) {
                            groupArrayList.get(k).selected = false;
                            adapter.remove(position);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void addNewItem() {
        for (int j = 0; j < groupArrayList.size(); j++) {
            if (groupArrayList.get(j).selected == false) {
                break;
            } else if (j == groupArrayList.size() - 1) {
                ToastUtils.showLong(R.string.tip_add_scene);
                return;
            }
        }
        inflatView();
    }

    private void inflatView() {
        AlertDialog.Builder builder;
        AlertDialog dialog;
        List<DbGroup> showList = getShowList();
        View bottomView = View.inflate(AddSceneAct.this, R.layout.dialog_list, null);//填充ListView布局
        ListView lvGp = (ListView) bottomView.findViewById(R.id.listview_group);//初始化ListView控件
        lvGp.setAdapter(new GroupListAdapter(this, showList));//ListView设置适配器

        builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.group_select)).setView(bottomView);
        dialog = builder.create();

        lvGp.setOnItemClickListener((parent, view, position, id) -> {
            ItemGroup itemGroup = new ItemGroup();
            itemGroup.brightness = 50;
            itemGroup.temperature = 50;
            itemGroup.groupAress = showList.get(position).getMeshAddr();
            itemGroup.gpName = showList.get(position).getName();
            changeData(position, showList);
            adapter.addData(itemGroup);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void changeData(int position, List<DbGroup> showList) {
        for (int k = 0; k < groupArrayList.size(); k++) {
            if (showList.get(position).getMeshAddr() == 0xffff) {
                groupArrayList.get(k).selected = true;
            } else {
                if (groupArrayList.get(k).getMeshAddr() == showList.get(position).getMeshAddr()) {
//                    showList.add(groupArrayList.get(k));
                    groupArrayList.get(k).selected = true;
                    for (int i = 0; i < groupArrayList.size(); i++) {
                        if (groupArrayList.get(i).getMeshAddr() == 0xffff) {
                            groupArrayList.get(i).selected = true;
                        }
                    }
                }
            }
        }
    }

    private List<DbGroup> getShowList() {
        List<DbGroup> showList = new ArrayList<>();
        for (int k = 0; k < groupArrayList.size(); k++) {
            if (!groupArrayList.get(k).selected) {
                showList.add(groupArrayList.get(k));
            }
        }
        return showList;
    }

    private void initView() {
        inflater = LayoutInflater.from(this);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
        if (mInputMethodManager.isActive()) {
            mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
        }
    }

    @OnClick({R.id.bt_save, R.id.edit_name, R.id.btn_sure_edit, R.id.bt_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
            case R.id.bt_save:
                if (checked()) {
                    save();
                    setResult(Constant.RESULT_OK);
                }
                break;
            case R.id.edit_name:
                editName.setFocusable(true);//设置输入框可聚集
                editName.setFocusableInTouchMode(true);//设置触摸聚焦
                editName.requestFocus();//请求焦点
                editName.findFocus();//获取焦点
                mInputMethodManager.showSoftInput(editName, InputMethodManager.SHOW_FORCED);// 显示输入法
                break;
            case R.id.btn_sure_edit:
                editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
                }
                break;
            case R.id.bt_add:
                addNewItem();
                break;
        }
    }

    private boolean isSave = false;

    private void save() {
        showLoadingDialog(getString(R.string.saving));
        new Thread(() -> {
            String name = editName.getText().toString().trim();
//        List<ItemGroup> itemGroups = adapter.getData();
            List<ItemGroup> itemGroups = itemGroupArrayList;

            DbScene dbScene = new DbScene();
            dbScene.setId(getSceneId());
            dbScene.setName(name);
            dbScene.setBelongRegionId((long) SharedPreferencesUtils.getCurrentUseRegion());
            DBUtils.saveScene(dbScene,false);

            long idAction = dbScene.getId();

            for (int i = 0; i < itemGroups.size(); i++) {
                DbSceneActions sceneActions = new DbSceneActions();
                sceneActions.setActionId(idAction);
                sceneActions.setBelongAccount(telinkLightApplication.getMesh().name);
                sceneActions.setBrightness(itemGroups.get(i).brightness);
                sceneActions.setColorTemperature(itemGroups.get(i).temperature);
//            if (isSave) {//选择的组里面包含了所有组，用户仍然确定了保存,只保存所有组
//                sceneActions.setGroupAddr(0xFFFF);
//                DBUtils.saveSceneActions(sceneActions);
//                break;
//            } else {
                sceneActions.setGroupAddr(itemGroups.get(i).groupAress);
                DBUtils.saveSceneActions(sceneActions);
//            }
            }

            try {
                Thread.sleep(100);
                addScene(idAction);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                hideLoadingDialog();
                finish();
            }
        }).start();
    }

    private Long getSceneId() {
        List<DbScene> list=DBUtils.getSceneList();
        List<Integer> idList=new ArrayList<>();
        for(int i=0;i<list.size();i++){
            idList.add(list.get(i).getId().intValue());
        }

        int id= 0;
        for(int i=1;i<17;i++){
            if(idList.contains(i)){
                Log.d("sceneID", "getSceneId: "+"aaaaa");
                continue;
            }else{
                id=i;
                Log.d("sceneID", "getSceneId: "+"bbbbb"+id);
                break;
            }
        }

        if(list.size()==0){
            id=1;
        }

        return Long.valueOf(id);
    }


    private void addScene(long id) throws InterruptedException {
        byte opcode = (byte) Opcode.SCENE_ADD_OR_DEL;
        List<DbSceneActions> list = DBUtils.searchActionsBySceneId(id);
        byte[] params;

        for (int i = 0; i < list.size(); i++) {
            int count=0;
            do{
                count++;
                Thread.sleep(300);
                byte temperature = (byte) list.get(i).getColorTemperature();
                if (temperature > 99)
                    temperature = 99;
                byte light = (byte) list.get(i).getBrightness();
                if (light > 99)
                    light = 99;
                params = new byte[]{0x01, (byte) id, light,
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, temperature};
                TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
            }while(count<3);
        }
    }

    private boolean checked() {

        String name = editName.getText().toString().trim();
        if (StringUtils.compileExChar(name)) {
            ToastUtils.showLong(R.string.rename_tip_check);
            return false;
        }

//        List<ItemGroup> itemGroups = adapter.getData();

//        for (int i = 0; i < itemGroups.size(); i++) {
//            if (itemGroups.get(i).groupAress == 0xFFFF) {
//                showSaveDialog();
//                return false;
//            }
//        }
//        ToastUtils.showLong(R.string.scene_tip);

        if(itemGroupArrayList.size()==0){
            ToastUtils.showLong(R.string.add_scene_gp_tip);
            return false;
        }
        return true;
    }
}
