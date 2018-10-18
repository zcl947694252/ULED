package com.dadoutek.uled.scene;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dadoutek.uled.R;
import com.dadoutek.uled.group.GroupListAdapter;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbSceneActions;
import com.dadoutek.uled.model.ItemColorPreset;
import com.dadoutek.uled.model.ItemGroup;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.Scenes;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.rgb.ColorSelectDiyRecyclerViewAdapter;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.StringUtils;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

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
    private int currentColor;
    private List<ItemColorPreset> presetColors;
    private int currentPosition;
    private LayoutInflater inflater;
    private SceneGroupAdapter sceneGroupAdapter;

    private DataManager dataManager;
    private TelinkLightApplication telinkLightApplication;
    private ArrayList<DbGroup> groupArrayList = new ArrayList<>();
    private ArrayList<ItemGroup> itemGroupArrayList = new ArrayList<>();
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
        toolbar.setNavigationIcon(R.drawable.navigation_back_white);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initData() {
        scenes = new Scenes();
        telinkLightApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, telinkLightApplication.getMesh().getName(), telinkLightApplication.getMesh().getPassword());
        groups = DBUtils.INSTANCE.getGroupList();
        itemGroupArrayList = new ArrayList<>();

        for (DbGroup group : groups) {
            group.selected = false;
            groupArrayList.add(group);
            groupNameArrayList.add(group.getName());
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        sceneGroupListView.setLayoutManager(layoutmanager);

        this.sceneGroupAdapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList);
//        sceneGroupListView.setAdapter(adapter);
        sceneGroupAdapter.bindToRecyclerView(sceneGroupListView);

        //删除时恢复可添加组标记
        sceneGroupAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            switch (view.getId()) {
                case R.id.btn_delete:
                    delete(adapter, position);
                    break;
                case R.id.btn_rgb:
//                    Intent intent=new Intent(this,)
//                    startActivityForResult();
                    currentPosition= position;
                    showPickColorDialog();
                    break;
            }
        });
    }

    private void showPickColorDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pick_color, null);
        ColorPickerView colorPickerView = view.findViewById(R.id.color_picker);
        colorPickerView.setColorListener(colorEnvelopeListener);
        RecyclerView diyColorRecyclerListView=view.findViewById(R.id.diy_color_recycler_list_view);
        ColorSelectDiyRecyclerViewAdapter colorSelectDiyRecyclerViewAdapter;

        presetColors = (List<ItemColorPreset>) SharedPreferencesHelper.getObject(this, Constant.LIGHT_PRESET_COLOR);
        if (presetColors == null) {
            presetColors = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ItemColorPreset itemColorPreset = new ItemColorPreset();
                presetColors.add(itemColorPreset);
            }
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
        diyColorRecyclerListView.setLayoutManager(layoutmanager);
        colorSelectDiyRecyclerViewAdapter = new ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildClickListener(diyOnItemChildClickListener);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildLongClickListener(diyOnItemChildLongClickListener);
        colorSelectDiyRecyclerViewAdapter.bindToRecyclerView(diyColorRecyclerListView);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_color_title);
        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

        });
        builder.setPositiveButton(R.string.btn_sure, (dialog, which) -> {
            sceneGroupAdapter.getViewByPosition(currentPosition,R.id.btn_rgb).setBackgroundColor(currentColor);
            sceneGroupAdapter.notifyItemChanged(currentPosition);
        });

        AlertDialog dialog=builder.create();
        dialog.show();
    }

    BaseQuickAdapter.OnItemChildClickListener diyOnItemChildClickListener = new BaseQuickAdapter.OnItemChildClickListener() {
        @Override
        public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
            int color = presetColors.get(position).getColor();
            int brightness = presetColors.get(position).getBrightness();
            int red = (color & 0xff0000) >> 16;
            int green = (color & 0x00ff00) >> 8;
            int blue = (color & 0x0000ff);
            new Thread(() -> {
                changeColor((byte) red, (byte) green, (byte) blue);

                try {
                    Thread.sleep(200);
                    int addr = itemGroupArrayList.get(currentPosition).groupAress;
                    byte opcode;
                    byte[] params;
                    opcode = (byte) Opcode.SET_LUM;
                    params = new byte[]{(byte) brightness};
                    itemGroupArrayList.get(currentPosition).brightness=brightness;
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    };

    BaseQuickAdapter.OnItemChildLongClickListener diyOnItemChildLongClickListener = new BaseQuickAdapter.OnItemChildLongClickListener() {
        @Override
        public boolean onItemChildLongClick(BaseQuickAdapter adapter, View view, int position) {
            presetColors.get(position).setColor(currentColor);
//            presetColors.get(position).setBrightness(currentItemGroup.brightness);
            TextView textView = (TextView) adapter.getViewByPosition(position, R.id.btn_diy_preset);
            textView.setText("");
            textView.setBackgroundColor(currentColor);
            SharedPreferencesHelper.putObject(AddSceneAct.this, Constant.LIGHT_PRESET_COLOR, presetColors);
            return false;
        }
    };

    private ColorEnvelopeListener colorEnvelopeListener = new ColorEnvelopeListener() {

        @Override
        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
            int[] argb = envelope.getArgb();

            currentColor = Color.argb(255, argb[1], argb[2], argb[3]);
            if (fromUser) {
                if (argb[1] == 0 && argb[2] == 0 && argb[3] == 0) {
                } else {
                    changeColor((byte) argb[1], (byte) argb[2], (byte) argb[3]);
                }
            }
        }
    };

    private void changeColor(byte R, byte G, byte B) {

        byte red = R;
        byte green = G;
        byte blue = B;

        int addr = itemGroupArrayList.get(currentPosition).groupAress;
        byte opcode = (byte) 0xE2;

        byte minVal = (byte) 0x50;

        if ((green & 0xff) <= minVal)
            green = 0;
        if ((red & 0xff) <= minVal)
            red = 0;
        if ((blue & 0xff) <= minVal)
            blue = 0;


        byte[] params = new byte[]{0x04, red, green, blue};

        String logStr = String.format("R = %x, G = %x, B = %x", red, green, blue);
        Log.d("RGBCOLOR", logStr);

        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
    }

    private void delete(BaseQuickAdapter adapter, int position) {
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
        RecyclerView lvGp = bottomView.findViewById(R.id.listview_group);//初始化ListView控件
        Button btnSure = bottomView.findViewById(R.id.btn_sure);
        btnSure.setVisibility(View.GONE);

        builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.group_select))
                .setView(bottomView);
        dialog = builder.create();


        GroupListAdapter groupListAdapter = new GroupListAdapter(R.layout.item_group, showList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        lvGp.setLayoutManager(layoutManager);
        lvGp.setAdapter(groupListAdapter);
        groupListAdapter.bindToRecyclerView(lvGp);

        dialog.show();


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dialog.getWindow().setLayout((int) (size.x * 0.9), WindowManager.LayoutParams.WRAP_CONTENT);


        groupListAdapter.setOnItemClickListener((adapter, view, position) -> {

            DbGroup item = showList.get(position);
            if (item.getMeshAddr() == 0xffff) {
                ItemGroup itemGroup = new ItemGroup();
                itemGroup.brightness = 50;
                itemGroup.temperature = 50;
                itemGroup.groupAress = showList.get(position).getMeshAddr();
                itemGroup.gpName = showList.get(position).getName();
                changeData(position, showList);
                sceneGroupAdapter.addData(itemGroup);
                dialog.dismiss();
            } else {
                btnSure.setVisibility(View.VISIBLE);
                if (showList.get(position).checked) {
                    showList.get(position).checked = false;
                } else {
                    showList.get(position).checked = true;
                }

                if (showList.get(0).getMeshAddr() == 0xffff) {
                    adapter.remove(0);
                }
                adapter.notifyItemChanged(position);
            }
        });

        btnSure.setOnClickListener(v -> {
            for (int j = 0; j < showList.size(); j++) {
                if (showList.get(j).checked) {
                    ItemGroup itemGroup = new ItemGroup();
                    itemGroup.brightness = 50;
                    itemGroup.temperature = 50;
                    itemGroup.groupAress = showList.get(j).getMeshAddr();
                    itemGroup.gpName = showList.get(j).getName();
                    changeDataList(showList.get(j));
                    sceneGroupAdapter.addData(itemGroup);
                }

                if (j == showList.size() - 1) {
                    dialog.dismiss();
                }
            }
        });


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

    private void changeDataList(DbGroup item) {
        for (int k = 0; k < groupArrayList.size(); k++) {
            if (groupArrayList.get(k).getMeshAddr() == item.getMeshAddr()) {
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

    private List<DbGroup> getShowList() {
        List<DbGroup> showList = new ArrayList<>();
        for (int k = 0; k < groupArrayList.size(); k++) {
            if (!groupArrayList.get(k).selected) {
                groupArrayList.get(k).checked = false;
                showList.add(groupArrayList.get(k));
            } else {
                groupArrayList.get(k).checked = false;
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
        StringUtils.initEditTextFilter(editName);
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
            DBUtils.INSTANCE.saveScene(dbScene, false);

            long idAction = dbScene.getId();

            for (int i = 0; i < itemGroups.size(); i++) {
                DbSceneActions sceneActions = new DbSceneActions();
                sceneActions.setBelongSceneId(idAction);
                sceneActions.setBrightness(itemGroups.get(i).brightness);
                sceneActions.setColorTemperature(itemGroups.get(i).temperature);
//            if (isSave) {//选择的组里面包含了所有组，用户仍然确定了保存,只保存所有组
//                sceneActions.setGroupAddr(0xFFFF);
//                DBUtils.saveSceneActions(sceneActions);
//                break;
//            } else {
                sceneActions.setGroupAddr(itemGroups.get(i).groupAress);
                DBUtils.INSTANCE.saveSceneActions(sceneActions);
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
        List<DbScene> list = DBUtils.INSTANCE.getSceneList();
        List<Integer> idList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            idList.add(list.get(i).getId().intValue());
        }

        int id = 0;
        for (int i = 1; i < 17; i++) {
            if (idList.contains(i)) {
                Log.d("sceneID", "getSceneId: " + "aaaaa");
                continue;
            } else {
                id = i;
                Log.d("sceneID", "getSceneId: " + "bbbbb" + id);
                break;
            }
        }

        if (list.size() == 0) {
            id = 1;
        }

        return Long.valueOf(id);
    }


    private void addScene(long id) throws InterruptedException {
        byte opcode = (byte) Opcode.SCENE_ADD_OR_DEL;
        List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
        byte[] params;

        for (int i = 0; i < list.size(); i++) {
            int count = 0;
            do {
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
            } while (count < 3);
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

        if (itemGroupArrayList.size() == 0) {
            ToastUtils.showLong(R.string.add_scene_gp_tip);
            return false;
        }
        return true;
    }
}
