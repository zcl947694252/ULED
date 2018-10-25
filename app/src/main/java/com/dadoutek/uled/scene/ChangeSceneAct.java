package com.dadoutek.uled.scene;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ProgressBar;
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
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.rgb.ColorSelectDiyRecyclerViewAdapter;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.DataManager;
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

public class ChangeSceneAct extends TelinkBaseActivity {
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
    @BindView(R.id.progressbar)
    ProgressBar progressbar;

    private DbScene scene;
    private LayoutInflater inflater;
    private SceneGroupAdapter sceneGroupAdapter;

    private int currentColor;
    private List<ItemColorPreset> presetColors;
    private int currentPosition;

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
        initClick();
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.edit_scene);
        toolbar.setNavigationIcon(R.drawable.navigation_back_white);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initClick() {
        //删除时恢复可添加组标记
        sceneGroupAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            switch (view.getId()) {
                case R.id.btn_delete:
                    delete(adapter, position);
                    break;
                case R.id.btn_rgb:
//                    Intent intent=new Intent(this,)
//                    startActivityForResult();
                    currentPosition = position;
                    showPickColorDialog();
                    break;
            }
        });
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

    private void showPickColorDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pick_color, null);
        ColorPickerView colorPickerView = view.findViewById(R.id.color_picker);
        colorPickerView.setColorListener(colorEnvelopeListener);
        RecyclerView diyColorRecyclerListView = view.findViewById(R.id.diy_color_recycler_list_view);
        ColorSelectDiyRecyclerViewAdapter colorSelectDiyRecyclerViewAdapter;

        presetColors = (List<ItemColorPreset>) SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR);
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
        colorSelectDiyRecyclerViewAdapter = new ColorSelectDiyRecyclerViewAdapter(R.layout.dialog_color_select_diy_item, presetColors);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildClickListener(diyOnItemChildClickListener);
        colorSelectDiyRecyclerViewAdapter.setOnItemChildLongClickListener(diyOnItemChildLongClickListener);
        colorSelectDiyRecyclerViewAdapter.bindToRecyclerView(diyColorRecyclerListView);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_color_title);
        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {

        });
        builder.setPositiveButton(R.string.btn_sure, (dialog, which) -> {
            itemGroupArrayList.get(currentPosition).color = currentColor;
            sceneGroupAdapter.getViewByPosition(currentPosition, R.id.btn_rgb).setBackgroundColor(currentColor);
            sceneGroupAdapter.notifyItemChanged(currentPosition);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    BaseQuickAdapter.OnItemChildClickListener diyOnItemChildClickListener = new BaseQuickAdapter.OnItemChildClickListener() {
        @Override
        public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
            int color = presetColors.get(position).getColor();
            currentColor = color;
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
                    itemGroupArrayList.get(currentPosition).brightness = brightness;
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
            SharedPreferencesHelper.putObject(ChangeSceneAct.this, Constant.PRESET_COLOR, presetColors);
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

    private void initData() {
        telinkLightApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, telinkLightApplication.getMesh().getName(), telinkLightApplication.getMesh().getPassword());
        groups = DBUtils.INSTANCE.getGroupList();
        itemGroupArrayList = new ArrayList<>();

        Intent intent = getIntent();
        scene = (DbScene) intent.getExtras().get(Constant.CURRENT_SELECT_SCENE);
//        scene.__setDaoSession(DaoSessionInstance.getInstance());

        List<DbSceneActions> actions = DBUtils.INSTANCE.getActionsBySceneId(scene.getId());

        boolean includeAll = false;

        for (DbGroup group : groups) {
//            if (group.containsLightList.size() > 0 || group.getMeshAddr() == 0xffff)
//            DbGroup group=new DbGroup();
//            group.setBelongRegionId(group1.getBelongRegionId());
//            group.setBrightness(group1.getBrightness());
//            group.setColorTemperature(group1.getColorTemperature());
//            group.setMeshAddr(group1.getMeshAddr());
//            group.setName(group1.getName());

            group.selected = false;

            loop1:
            for (int i = 0; i < actions.size(); i++) {
                if (group.getMeshAddr() == actions.get(i).getGroupAddr()) {
                    group.selected = true;

                    ItemGroup itemGroup = new ItemGroup();
                    itemGroup.brightness = actions.get(i).getBrightness();
                    itemGroup.temperature = actions.get(i).getColorTemperature();
                    itemGroup.groupAress = actions.get(i).getGroupAddr();
                    itemGroup.gpName = group.getName();
                    itemGroup.color = Integer.parseInt(actions.get(i).getColor() == null ?
                            getResources().getColor(R.color.primary) + "" : actions.get(i).getColor());
                    itemGroupArrayList.add(itemGroup);

                    if (group.getMeshAddr() != 0xffff) {
                        includeAll = false;
                    } else {
                        includeAll = true;
                    }
                    break loop1;
                }
            }

            if (includeAll) {
                group.selected = true;
            } else {
                if (group.getMeshAddr() == 0xffff) {
                    group.selected = true;
                }
            }
            groupArrayList.add(group);
            groupNameArrayList.add(group.getName());
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
        ArrayList<DbGroup> selectGroupList = new ArrayList<>();

        View bottomView = View.inflate(ChangeSceneAct.this, R.layout.dialog_list, null);//填充ListView布局
        RecyclerView lvGp = (RecyclerView) bottomView.findViewById(R.id.listview_group);//初始化ListView控件
        Button btnSure = bottomView.findViewById(R.id.btn_sure);
        btnSure.setVisibility(View.GONE);

        GroupListAdapter groupListAdapter;
        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        lvGp.setLayoutManager(layoutmanager);
        groupListAdapter = new GroupListAdapter(R.layout.item_group, showList);
        groupListAdapter.bindToRecyclerView(lvGp);

        builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.group_select)).setView(bottomView);
        dialog = builder.create();

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

        dialog.show();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dialog.getWindow().setLayout((int) (size.x * 0.9), WindowManager.LayoutParams.WRAP_CONTENT);
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

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        sceneGroupListView.setLayoutManager(layoutmanager);

        this.sceneGroupAdapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList);
//        sceneGroupListView.setAdapter(adapter);
        sceneGroupAdapter.bindToRecyclerView(sceneGroupListView);

        inflater = LayoutInflater.from(this);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
        editName.setText(scene.getName());
        if (mInputMethodManager.isActive()) {
            mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
        }
        StringUtils.initEditTextFilter(editName);
    }

    @OnClick({R.id.bt_save, R.id.edit_name, R.id.btn_sure_edit, R.id.bt_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
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
            List<ItemGroup> itemGroups = itemGroupArrayList;

            scene.setName(name);
            DBUtils.INSTANCE.updateScene(scene);
            long idAction = scene.getId();
            DBUtils.INSTANCE.deleteSceneActionsList(DBUtils.INSTANCE.getActionsBySceneId(scene.getId()));

            for (int i = 0; i < itemGroups.size(); i++) {
                DbSceneActions sceneActions = new DbSceneActions();
                sceneActions.setBelongSceneId(idAction);
                sceneActions.setBrightness(itemGroups.get(i).brightness);
                sceneActions.setColorTemperature(itemGroups.get(i).temperature);
                sceneActions.setGroupAddr(itemGroups.get(i).groupAress);
                sceneActions.setColor(itemGroups.get(i).color+"");
                DBUtils.INSTANCE.saveSceneActions(sceneActions);
            }
            try {
                Thread.sleep(100);
                updateScene(idAction);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                hideLoadingDialog();
                finish();
            }
        }).start();
    }

    private void updateScene(long id) throws InterruptedException {
        deleteScene(id);
        byte opcode = (byte) Opcode.SCENE_ADD_OR_DEL;
        List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
        byte[] params;
        for (int i = 0; i < list.size(); i++) {
            Thread.sleep(100);
            byte temperature = (byte) list.get(i).getColorTemperature();
            if (temperature > 99)
                temperature = 99;
            byte light = (byte) list.get(i).getBrightness();
            if (light > 99)
                light = 99;
            int color = Integer.parseInt(list.get(i).getColor());
            int red = (color & 0xff0000) >> 16;
            int green = (color & 0x00ff00) >> 8;
            int blue = (color & 0x0000ff);

            params = new byte[]{0x01, (byte) id, light,
                    (byte) red, (byte) green, (byte) blue, temperature};
            TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
        }
    }

    private void deleteScene(long id) {
        byte opcode = Opcode.SCENE_ADD_OR_DEL;
        byte[] params;
        params = new byte[]{0x00, (byte) id};
        try {
            Thread.sleep(100);
            TelinkLightService.Instance().sendCommandNoResponse(opcode, 0xFFFF, params);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checked() {

        String name = editName.getText().toString().trim();
        if (StringUtils.compileExChar(name)) {
            ToastUtils.showLong(R.string.rename_tip_check);
            return false;
        }

//        List<ItemGroup> itemGroups = adapter.getData();
//
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
