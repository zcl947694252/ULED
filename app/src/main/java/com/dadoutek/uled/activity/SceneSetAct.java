package com.dadoutek.uled.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.DbModel.DbSceneActions;
import com.dadoutek.uled.DbModel.DbSceneActionsUtils;
import com.dadoutek.uled.DbModel.DbSceneUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.adapter.SceneGroupAdapter;
import com.dadoutek.uled.dao.DbSceneDao;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.ItemGroup;
import com.dadoutek.uled.model.Scenes;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class SceneSetAct extends TelinkBaseActivity {
    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
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

    private int brightness = 0;
    private int temperature = 0;
    private Groups groups;
    private Scenes scenes;
    private LayoutInflater inflater;
    private SceneGroupAdapter adapter;

    private DataManager dataManager;
    private TelinkLightApplication telinkLightApplication;
    private ArrayList<Group> groupArrayList = new ArrayList<>();
    private ArrayList<ItemGroup> itemGroupArrayList = new ArrayList<>();
    private ArrayList<String> groupNameArrayList = new ArrayList<>();
    private List<Integer> groupsAddressList = new ArrayList<>();
    /**
     * 输入法管理器
     */
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_set);
        ButterKnife.bind(this);
        initData();
        initView();
    }

    private void initData() {
        scenes = new Scenes();
        telinkLightApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, telinkLightApplication.getMesh().name, telinkLightApplication.getMesh().password);
        groups = dataManager.getGroups();
        itemGroupArrayList = new ArrayList<>();
        groupArrayList.add(dataManager.createAllLightControllerGroup());
        groupNameArrayList.add(groupArrayList.get(0).name);
        List<Group> groupList = groups.get();
        for (Group group : groupList) {
            if (group.containsLightList.size() > 0)
                group.checked = false;
            groupArrayList.add(group);
            groupNameArrayList.add(group.name);
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        sceneGroupListView.setLayoutManager(layoutmanager);

        addNewItem();

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, groupNameArrayList);
        this.adapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList,arrayAdapter);
//        sceneGroupListView.setAdapter(adapter);
        adapter.bindToRecyclerView(sceneGroupListView);
    }

    private void addNewItem() {
        ItemGroup itemGroup = new ItemGroup();
        itemGroup.brightness = 0;
        itemGroup.temperature = 0;
        itemGroup.groupPosition = 0;

        if (itemGroupArrayList.size() == 0) {
            itemGroupArrayList.add(itemGroup);
        } else {
            adapter.addData(itemGroup);
//            itemGroupArrayList.add(itemGroup);
        }
    }

    private void initView() {
        inflater = LayoutInflater.from(this);
        txtHeaderTitle.setText(R.string.creat_scene);
//        sceneGroupListView.set(this);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
        if (mInputMethodManager.isActive()) {
            mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
        }
    }

    @OnClick({R.id.img_header_menu_left, R.id.bt_save, R.id.edit_name, R.id.btn_sure_edit,R.id.bt_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
            case R.id.bt_save:
                if (checked()) {
                    save();
                    setResult(Constant.RESULT_OK);
                    finish();
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

    private void save() {
        String name=editName.getText().toString().trim();
        List<ItemGroup> itemGroups=adapter.getData();
        DbScene dbScene=new DbScene();
        dbScene.setName(name);
        dbScene.setBelongAccount(Constant.TESTACCOUNT);
        DbSceneUtils.save(dbScene);

        long idAction=dbScene.getId();

        for(int i=0;i<itemGroups.size();i++){
            DbSceneActions sceneActions=new DbSceneActions();
            sceneActions.setActionId(idAction);
            sceneActions.setBelongAccount(Constant.TESTACCOUNT);
            sceneActions.setBrightness(itemGroups.get(i).brightness);
            sceneActions.setColorTemperature(itemGroups.get(i).temperature);
            sceneActions.setGroupAddr(groupArrayList.get(itemGroups.get(i).groupPosition).meshAddress);
            DbSceneActionsUtils.save(sceneActions);
        }

        try {
            Thread.sleep(100);
            addScene(idAction);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addScene(long id) throws InterruptedException {
        byte opcode=(byte) 0xEE;
        List<DbSceneActions> list= DbSceneActionsUtils.searchActionsBySceneId(id);
        byte[] params;
        for(int i=0;i<list.size();i++){
            Thread.sleep(100);
            params = new byte[]{0x01,(byte) id,(byte) list.get(i).getBrightness(),
                    (byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte)list.get(i).getColorTemperature()};
            TelinkLightService.Instance().sendCommandNoResponse(opcode,list.get(i).getGroupAddr(),params);
        }
    }

    private boolean checked() {

        String name = editName.getText().toString().trim();
        if (StringUtils.compileExChar(name)) {
            ToastUtils.showLong(R.string.rename_tip_check);
            return false;
        }

//        ToastUtils.showLong(R.string.scene_tip);
        return true;
    }
}
