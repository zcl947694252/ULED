//package com.dadoutek.uled.scene;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.graphics.Point;
//import android.graphics.drawable.ColorDrawable;
//import android.os.Bundle;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.widget.DividerItemDecoration;
//import android.support.v7.widget.GridLayoutManager;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.Toolbar;
//import android.util.Log;
//import android.view.Display;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewTreeObserver;
//import android.view.Window;
//import android.view.WindowManager;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ListView;
//import android.widget.SeekBar;
//import android.widget.TextView;
//
//import com.app.hubert.guide.core.Controller;
//import com.blankj.utilcode.util.ToastUtils;
//import com.chad.library.adapter.base.BaseQuickAdapter;
//import com.dadoutek.uled.R;
//import com.dadoutek.uled.group.GroupListAdapter;
//import com.dadoutek.uled.model.Constant;
//import com.dadoutek.uled.model.DbModel.DBUtils;
//import com.dadoutek.uled.model.DbModel.DbGroup;
//import com.dadoutek.uled.model.DbModel.DbScene;
//import com.dadoutek.uled.model.DbModel.DbSceneActions;
//import com.dadoutek.uled.model.ItemColorPreset;
//import com.dadoutek.uled.model.ItemGroup;
//import com.dadoutek.uled.model.Opcode;
//import com.dadoutek.uled.model.Scenes;
//import com.dadoutek.uled.model.SharedPreferencesHelper;
//import com.dadoutek.uled.rgb.ColorSceneSelectDiyRecyclerViewAdapter;
//import com.dadoutek.uled.tellink.TelinkBaseActivity;
//import com.dadoutek.uled.tellink.TelinkLightApplication;
//import com.dadoutek.uled.tellink.TelinkLightService;
//import com.dadoutek.uled.util.DataManager;
//import com.dadoutek.uled.util.GuideUtils;
//import com.dadoutek.uled.util.OtherUtils;
//import com.dadoutek.uled.util.SharedPreferencesUtils;
//import com.dadoutek.uled.util.StringUtils;
//import com.skydoves.colorpickerview.ColorEnvelope;
//import com.skydoves.colorpickerview.ColorPickerView;
//import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;
//
///**
// * Created by hejiajun on 2018/5/2.
// */
//
//public class SetSceneAct extends TelinkBaseActivity {
//    @BindView(R.id.bt_save)
//    Button btSave;
//    @BindView(R.id.edit_name)
//    EditText editName;
//    @BindView(R.id.btn_sure_edit)
//    Button btnSureEdit;
//    @BindView(R.id.scene_group_list_view)
//    RecyclerView sceneGroupListView;
//    @BindView(R.id.bt_add)
//    Button btAdd;
//    @BindView(R.id.toolbar)
//    Toolbar toolbar;
//
//    private boolean isShowChangeDataView =false;
//    private AlertDialog dialog;
//    private Scenes scenes;
//    private DbScene scene;
//    private int currentColor;
//    private List<ItemColorPreset> presetColors;
//    private int currentPosition;
//    private LayoutInflater inflater;
//    private SceneGroupAdapter sceneGroupAdapter;
//
//    private DataManager dataManager;
//    private TelinkLightApplication telinkLightApplication;
//    private ArrayList<DbGroup> groupArrayList = new ArrayList<>();
//    private ArrayList<ItemGroup> itemGroupArrayList = new ArrayList<>();
//    private ArrayList<String> groupNameArrayList = new ArrayList<>();
//    private List<DbGroup> groups = new ArrayList<>();
//    private ArrayList<Integer> groupMeshAddrArrayList = new ArrayList<>();
//    /**
//     * 输入法管理器
//     */
//    private InputMethodManager mInputMethodManager;
//    private boolean guideShowCurrentPage=false;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_scene_set);
//        ButterKnife.bind(this);
//        initType();
//        if(!isShowChangeDataView){
//            editName.setText(DBUtils.INSTANCE.getDefaultNewSceneName());
//            lazyLoad();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//    }
//
//    private void initOnLayoutListener() {
//        final View view = this.getWindow().getDecorView();
//        final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
//        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
////                step3Guide();
//                stepEndGuide();
//            }
//        });
//    }
//
//    public void lazyLoad() {
////        EditText guide1= (EditText)findViewById(R.id.edit_name);
////        Button guide2= (Button)findViewById(R.id.bt_add);
////        GuideUtils.INSTANCE.guideBuilder(this,Constant.TAG_SetSceneAct)
////                .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide1,R.layout.view_guide_simple,getString(R.string.add_scene_guide_1)))
////                .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide2,R.layout.view_guide_simple,getString(R.string.add_scene_guide_2)
////                ))
////                .show();
////
////        if (sceneGroupAdapter.getItemCount() != 0) {
////            TextView guide3= (TextView) sceneGroupAdapter.getViewByPosition(0, R.id.btn_delete);
////            TextView guide4= (TextView) sceneGroupAdapter.getViewByPosition(0, R.id.rgb_view);
////            Button guide5= (Button) findViewById(R.id.bt_save);
////            Builder builder= GuideUtils.INSTANCE.guideBuilder(this,Constant.TAG_SetSceneAct);
////            builder.addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide3,R.layout.view_guide_simple,getString(R.string.add_scene_guide_3)));
////            if(guide4!=null && guide4.getVisibility()==View.VISIBLE){
////                builder.addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide4,R.layout.view_guide_simple,getString(R.string.add_scene_guide_4)));
////            }
////            builder.addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide5,R.layout.view_guide_simple,getString(R.string.add_scene_guide_5)))
////                    .show();
////        }
//        step1Guide();
//    }
//
////    private void stepstartGuide(){
////        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
////        if(guideShowCurrentPage){
////            EditText guide0= (EditText)findViewById(R.id.edit_name);
////            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP8_GUIDE_ADD_SCENE_ADD_GROUP())
////                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide0,R.layout.view_guide_simple_scene_set1,getString(R.string.add_scene_guide_1),
////                            v -> {
////                                step1Guide();
////                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY())).show();
////        }
////    }
//
//    Controller controllerGuide2=null;
//    private void step1Guide(){
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            Button guide1= (Button)findViewById(R.id.bt_add);
//            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP8_GUIDE_ADD_SCENE_ADD_GROUP())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide1,R.layout.view_guide_simple_scene_set1,getString(R.string.add_scene_guide_1),
//                            v -> {
//                                guide1.performClick();
////                                controllerGuide2=step2Guide(lvGp, showList);
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//    }
//
//    private Controller step2Guide(RecyclerView lvGp, List<DbGroup> showList, AlertDialog dialog, GroupListAdapter groupListAdapter){
//        ListView listView =dialog.getListView();
//        dialog.getLayoutInflater();
////        GuideUtils.INSTANCE.guideBuilder(SetSceneAct.this, lvGp, GuideUtils.INSTANCE
////                .getSTEP7_GUIDE_ADD_SCENE())
////                .addGuidePage(
////                        GuideUtils.INSTANCE.addGuidePage(
////                                lvGp.getChildAt(showList.size() - 1), R.layout.view_guide_simple_scene_1, getString(R.string.scene_guide_1),
////                                v -> {
////
////                                }, GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(), SetSceneAct.this))
////                .alwaysShow(true)
////                .show();
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            TextView guide2= findViewById(R.id.toolbarTv);
//            return GuideUtils.INSTANCE.guideBuilder(SetSceneAct.this,lvGp,GuideUtils.INSTANCE.getSTEP9_GUIDE_ADD_SCENE_SELECT_GROUP())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(lvGp.getChildAt(0),R.layout.view_guide_simple_scene_set2,getString(R.string.add_scene_guide_2),
//                            v -> {
//                                ItemGroup itemGroup = new ItemGroup();
//                                itemGroup.brightness = 50;
//                                itemGroup.temperature = 50;
//                                itemGroup.groupAress = showList.get(0).getMeshAddr();
//                                itemGroup.gpName = showList.get(0).getName();
//                                changeDataList(showList.get(0));
//                                sceneGroupAdapter.addData(itemGroup);
//                                dialog.dismiss();
//                                initOnLayoutListener();
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//        return null;
//    }
//
//    private void step3Guide(){
//        if(controllerGuide2!=null){
//            controllerGuide2.remove();
//        }
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            SeekBar guide3= (SeekBar) sceneGroupAdapter.getViewByPosition(0, R.id.sbBrightness);
//             GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP10_GUIDE_ADD_SCENE_CHANGE_BRIGHTNESS())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide3,R.layout.view_guide_simple_scene_set3,getString(R.string.add_scene_guide_3),
//                            v -> {
//                        DbGroup dbGroup=DBUtils.INSTANCE.getGroupByMesh(sceneGroupAdapter.getItem(0).groupAress);
//                        if(OtherUtils.isRGBGroup(dbGroup)){
//                            step5Guide();
//                        }else{
//                            step4Guide();
//                        }
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//    }
//
//    private void step4Guide(){
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            SeekBar guide4= (SeekBar) sceneGroupAdapter.getViewByPosition(0, R.id.sbTemperature);
//            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP11_GUIDE_ADD_SCENE_CHANGE_TEMPERATURE())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide4,R.layout.view_guide_simple_scene_set3,getString(R.string.add_scene_guide_4),
//                            v -> {
//                                stepEndGuide();
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//    }
//
//    private void step5Guide(){
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            TextView guide5= (TextView) sceneGroupAdapter.getViewByPosition(0, R.id.rgb_view);
//            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP12_GUIDE_ADD_SCENE_CHANGE_COLOR())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide5,R.layout.view_guide_simple_scene_set3,getString(R.string.add_scene_guide_5),
//                            v -> {
//                            stepEndGuide();
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//    }
//
//    private void stepEndGuide(){
//        guideShowCurrentPage = !GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false);
//        if(guideShowCurrentPage){
//            Button guide6= (Button) findViewById(R.id.bt_save);
//            GuideUtils.INSTANCE.guideBuilder(this,GuideUtils.INSTANCE.getSTEP13_GUIDE_ADD_SCENE_SAVE())
//                    .addGuidePage(GuideUtils.INSTANCE.addGuidePage(guide6,R.layout.view_guide_simple_scene_set1,getString(R.string.add_scene_guide_6),
//                            v -> {
//                             guide6.performClick();
//                            },GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),this)).show();
//        }
//    }
//
//    private void initType() {
//        Intent intent = getIntent();
//        boolean isChangeScene= (boolean) intent.getExtras().get(Constant.IS_CHANGE_SCENE);
//        if(!isChangeScene){
//            isShowChangeDataView =false;
//            initToolbar();
//            initData();
//            initView();
//        }else{
//            scene = (DbScene) intent.getExtras().get(Constant.CURRENT_SELECT_SCENE);
//            isShowChangeDataView =true;
//            initChangeToolbar();
//            initChangeData();
//            initChangeView();
//            initClick();
//        }
//    }
//
//    private void initClick() {
//        //删除时恢复可添加组标记
//        sceneGroupAdapter.setOnItemChildClickListener((adapter, view, position) -> {
//            switch (view.getId()) {
//                case R.id.btn_delete:
//                    delete(adapter, position);
//                    break;
//                case R.id.rgb_view:
////                    Intent intent=new Intent(this,)
////                    startActivityForResult();
//                    currentPosition = position;
//                    showPickColorDialog();
//                    break;
//            }
//        });
//    }
//
//    private void initChangeView() {
//
//        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
//        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
//        sceneGroupListView.setLayoutManager(layoutmanager);
//
//        this.sceneGroupAdapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList);
//
//        DividerItemDecoration decoration = new DividerItemDecoration(this,
//                DividerItemDecoration
//                        .VERTICAL);
//        decoration.setDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color
//                .divider)));
//        //添加分割线
//        sceneGroupListView.addItemDecoration(decoration);
//        sceneGroupAdapter.bindToRecyclerView(sceneGroupListView);
//
//        inflater = LayoutInflater.from(this);
//        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//
//        editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
//        editName.setText(scene.getName());
//        if (mInputMethodManager.isActive()) {
//            mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
//        }
//        StringUtils.initEditTextFilter(editName);
//    }
//
//    private void initChangeData() {
//        telinkLightApplication = (TelinkLightApplication) this.getApplication();
//        dataManager = new DataManager(this, telinkLightApplication.getMesh().getName(), telinkLightApplication.getMesh().getPassword());
//        groups = DBUtils.INSTANCE.getGroupList();
//        itemGroupArrayList = new ArrayList<>();
//
//        Intent intent = getIntent();
//        scene = (DbScene) intent.getExtras().get(Constant.CURRENT_SELECT_SCENE);
////        scene.__setDaoSession(DaoSessionInstance.getInstance());
//
//        List<DbSceneActions> actions = DBUtils.INSTANCE.getActionsBySceneId(scene.getId());
//
//        boolean includeAll = false;
//
//        for (DbGroup group : groups) {
//            group.selected = false;
//
//            loop1:
//            for (int i = 0; i < actions.size(); i++) {
//                if (group.getMeshAddr() == actions.get(i).getGroupAddr()) {
//                    group.selected = true;
//
//                    ItemGroup itemGroup = new ItemGroup();
//                    itemGroup.brightness = actions.get(i).getBrightness();
//                    itemGroup.temperature = actions.get(i).getColorTemperature();
//                    itemGroup.groupAress = actions.get(i).getGroupAddr();
//                    itemGroup.gpName = group.getName();
//                    itemGroup.color =  actions.get(i).getColor() == 0?
//                            getResources().getColor(R.color.primary) : actions.get(i).getColor();
//                    itemGroupArrayList.add(itemGroup);
//                    groupMeshAddrArrayList.add(group.getMeshAddr());
//
//                    if (group.getMeshAddr() != 0xffff) {
//                        includeAll = false;
//                    } else {
//                        includeAll = true;
//                    }
//                    break loop1;
//                }
//            }
//
//            if (includeAll) {
//                group.selected = true;
//            } else {
//                if (group.getMeshAddr() == 0xffff) {
//                    group.selected = true;
//                }
//            }
//            groupArrayList.add(group);
//        }
//    }
//
//    private void initToolbar() {
//        toolbar.setTitle(R.string.create_scene);
//        toolbar.setNavigationIcon(R.drawable.navigation_back_white);
//        toolbar.setNavigationOnClickListener(v -> finish());
//    }
//
//    private void initChangeToolbar() {
//        toolbar.setTitle(R.string.edit_scene);
//        toolbar.setNavigationIcon(R.drawable.navigation_back_white);
//        toolbar.setNavigationOnClickListener(v -> finish());
//    }
//
//    private void initData() {
//        scenes = new Scenes();
//        telinkLightApplication = (TelinkLightApplication) this.getApplication();
//        dataManager = new DataManager(this, telinkLightApplication.getMesh().getName(), telinkLightApplication.getMesh().getPassword());
//        groups = DBUtils.INSTANCE.getGroupList();
//        itemGroupArrayList = new ArrayList<>();
//
//        for (DbGroup group : groups) {
//            group.selected = false;
//            groupArrayList.add(group);
//            groupNameArrayList.add(group.getName());
//        }
//
//        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
//        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
//        sceneGroupListView.setLayoutManager(layoutmanager);
//
//        this.sceneGroupAdapter = new SceneGroupAdapter(R.layout.scene_group_item, itemGroupArrayList, groupArrayList);
//
//        DividerItemDecoration decoration = new DividerItemDecoration(this,
//                DividerItemDecoration
//                        .VERTICAL);
//        decoration.setDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color
//                .divider)));
//        //添加分割线
//        sceneGroupListView.addItemDecoration(decoration);
//        sceneGroupAdapter.bindToRecyclerView(sceneGroupListView);
//
//
//        //删除时恢复可添加组标记
//        sceneGroupAdapter.setOnItemChildClickListener((adapter, view, position) -> {
//            switch (view.getId()) {
//                case R.id.btn_delete:
//                    delete(adapter, position);
//                    break;
//                case R.id.rgb_view:
////                    Intent intent=new Intent(this,)
////                    startActivityForResult();
//                    currentPosition= position;
//                    showPickColorDialog();
//                    break;
//            }
//        });
//    }
//
//    private void showPickColorDialog() {
//        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pick_color, null);
//        ColorPickerView colorPickerView = view.findViewById(R.id.color_picker);
//        colorPickerView.setColorListener(colorEnvelopeListener);
//        RecyclerView diyColorRecyclerListView=view.findViewById(R.id.diy_color_recycler_list_view);
//        ColorSceneSelectDiyRecyclerViewAdapter colorSelectDiyRecyclerViewAdapter;
//
//        presetColors = (List<ItemColorPreset>) SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR);
//        if (presetColors == null) {
//            presetColors = new ArrayList<>();
//            for (int i = 0; i < 5; i++) {
//                ItemColorPreset itemColorPreset = new ItemColorPreset();
//                itemColorPreset.setBrightness(-1);
//                itemColorPreset.setColor(OtherUtils.getCreateInitColor(i));
//                presetColors.add(itemColorPreset);
//            }
//        }
//
////        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
////        layoutmanager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        diyColorRecyclerListView.setLayoutManager(new GridLayoutManager(this,5));
//        colorSelectDiyRecyclerViewAdapter = new ColorSceneSelectDiyRecyclerViewAdapter(R.layout.dialog_color_select_diy_item, presetColors);
//        colorSelectDiyRecyclerViewAdapter.setOnItemChildClickListener(diyOnItemChildClickListener);
//        colorSelectDiyRecyclerViewAdapter.setOnItemChildLongClickListener(diyOnItemChildLongClickListener);
//        colorSelectDiyRecyclerViewAdapter.bindToRecyclerView(diyColorRecyclerListView);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(R.string.select_color_title);
//        builder.setView(view);
//        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
//
//        });
//        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
//            itemGroupArrayList.get(currentPosition).color=currentColor;
//            sceneGroupAdapter.getViewByPosition(currentPosition,R.id.rgb_view).setBackgroundColor(currentColor);
//            sceneGroupAdapter.notifyItemChanged(currentPosition);
////            lazyLoad();
//        });
//
//        dialog= builder.create();
//        dialog.show();
//    }
//
//    private void hideSelectDialog(){
//        if(dialog!=null){
//            dialog.dismiss();
//        }
//    }
//
//    BaseQuickAdapter.OnItemChildClickListener diyOnItemChildClickListener = new BaseQuickAdapter.OnItemChildClickListener() {
//        @Override
//        public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
//            int color = presetColors.get(position).getColor();
//            currentColor = color;
//            int brightness = presetColors.get(position).getBrightness();
//            int red = (color & 0xff0000) >> 16;
//            int green = (color & 0x00ff00) >> 8;
//            int blue = (color & 0x0000ff);
//            new Thread(() -> {
//                changeColor((byte) red, (byte) green, (byte) blue);
//
////                try {
////                    Thread.sleep(200);
////                    int addr = itemGroupArrayList.get(currentPosition).groupAress;
////                    byte opcode;
////                    byte[] params;
////                    opcode = (byte) Opcode.SET_LUM;
////                    params = new byte[]{(byte) brightness};
////                    itemGroupArrayList.get(currentPosition).brightness=brightness;
////                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
////
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//            }).start();
//
////            hideSelectDialog();
//            itemGroupArrayList.get(currentPosition).color=currentColor;
//            sceneGroupAdapter.getViewByPosition(currentPosition,R.id.rgb_view).setBackgroundColor(currentColor);
//            sceneGroupAdapter.notifyItemChanged(currentPosition);
//        }
//    };
//
//    BaseQuickAdapter.OnItemChildLongClickListener diyOnItemChildLongClickListener = new BaseQuickAdapter.OnItemChildLongClickListener() {
//        @Override
//        public boolean onItemChildLongClick(BaseQuickAdapter adapter, View view, int position) {
//            presetColors.get(position).setColor(currentColor);
////            presetColors.get(position).setBrightness(currentItemGroup.brightness);
//            TextView textView = (TextView) adapter.getViewByPosition(position, R.id.btn_diy_preset);
//            textView.setText("");
//            textView.setBackgroundColor(0xff000000|currentColor);
//            SharedPreferencesHelper.putObject(SetSceneAct.this, Constant.PRESET_COLOR, presetColors);
//            return false;
//        }
//    };
//
//    private ColorEnvelopeListener colorEnvelopeListener = new ColorEnvelopeListener() {
//
//        @Override
//        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
//            int[] argb = envelope.getArgb();
//
//            currentColor = (argb[1]<<16) | (argb[2]<<8) | (argb[3]);
//            if (fromUser) {
//                if (argb[1] == 0 && argb[2] == 0 && argb[3] == 0) {
//                } else {
//                    changeColor((byte) argb[1], (byte) argb[2], (byte) argb[3]);
//                }
//            }
//        }
//    };
//
//    private void changeColor(byte R, byte G, byte B) {
//
//        byte red = R;
//        byte green = G;
//        byte blue = B;
//
//        int addr = itemGroupArrayList.get(currentPosition).groupAress;
//        byte opcode = (byte) 0xE2;
//
//        byte minVal = (byte) 0x50;
//
//        if ((green & 0xff) <= minVal)
//            green = 0;
//        if ((red & 0xff) <= minVal)
//            red = 0;
//        if ((blue & 0xff) <= minVal)
//            blue = 0;
//
//
//        byte[] params = new byte[]{0x04, red, green, blue};
//
//        String logStr = String.format("R = %x, G = %x, B = %x", red, green, blue);
//        Log.d("RGBCOLOR", logStr);
//
//
//        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params);
//    }
//
//    private void delete(BaseQuickAdapter adapter, int position) {
//        if (groupArrayList.size() != 0) {
//            if (adapter.getItemCount() == 1) {
//                for (int k = 0; k < groupArrayList.size(); k++) {
//                    groupArrayList.get(k).selected = false;
//                }
//                adapter.remove(position);
//            } else {
//                for (int k = 0; k < groupArrayList.size(); k++) {
//                    if (groupArrayList.get(k).getName().equals(itemGroupArrayList.get(position).gpName)) {
//                        groupArrayList.get(k).selected = false;
//                        adapter.remove(position);
//                        break;
//                    }
//                }
//            }
//        }
//    }
//
//    private void addNewItem() {
//        for (int j = 0; j < groupArrayList.size(); j++) {
//            if (groupArrayList.get(j).selected == false) {
//                break;
//            } else if (j == groupArrayList.size() - 1) {
//                ToastUtils.showLong(R.string.tip_add_scene);
//                return;
//            }
//        }
//        inflatView();
//    }
//
//    private void inflatView() {
//        AlertDialog.Builder builder;
//        AlertDialog dialog;
//        List<DbGroup> showList = getShowList();
//
//        View bottomView = View.inflate(SetSceneAct.this, R.layout.dialog_list, null);//填充ListView布局
//        RecyclerView lvGp = bottomView.findViewById(R.id.listview_group);//初始化ListView控件
//        Button btnSure = bottomView.findViewById(R.id.btn_sure);
//        btnSure.setVisibility(View.GONE);
//
//        builder = new AlertDialog.Builder(this)
//                .setView(bottomView);
//        if(!GuideUtils.INSTANCE.getCurrentViewIsEnd(this,GuideUtils.INSTANCE.getEND_ADD_SCENE_KEY(),false)){
//            builder.setCancelable(false);
//        }
//        dialog = builder.create();
//
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//
////        List<DbGroup> showList = showList;
//        GroupListAdapter groupListAdapter = new GroupListAdapter(R.layout.item_group, showList);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        lvGp.setLayoutManager(layoutManager);
//        lvGp.setAdapter(groupListAdapter);
//        groupListAdapter.bindToRecyclerView(lvGp);
//
//        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
//            @Override
//            public void onShow(DialogInterface dialogInterface) {
//               step2Guide(lvGp,showList,dialog,groupListAdapter);
//            }
//        });
//
//        dialog.show();
//
//
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        dialog.getWindow().setLayout((int) (size.x * 0.9), WindowManager.LayoutParams.WRAP_CONTENT);
//
//
//        groupListAdapter.setOnItemClickListener((adapter, view, position) -> {
//
//            if(position!=-1){
//                DbGroup item = showList.get(position);
//                if (item.getMeshAddr() == 0xffff) {
//                    ItemGroup itemGroup = new ItemGroup();
//                    itemGroup.brightness = 50;
//                    itemGroup.temperature = 50;
//                    itemGroup.groupAress = showList.get(position).getMeshAddr();
//                    itemGroup.gpName = showList.get(position).getName();
//                    changeData(position, showList);
//                    sceneGroupAdapter.addData(itemGroup);
//                    dialog.dismiss();
//                } else {
//                    btnSure.setVisibility(View.VISIBLE);
//                    if (showList.get(position).checked) {
//                        showList.get(position).checked = false;
//                    } else {
//                        showList.get(position).checked = true;
//                    }
//
//                    if (showList.get(0).getMeshAddr() == 0xffff) {
//                        adapter.remove(0);
//                    }
//
//                    view.setClickable(false);
//                    adapter.notifyItemChanged(position);
//                    view.setClickable(true);
//                }
//            }
//        });
//
//        btnSure.setOnClickListener(v -> {
//            for (int j = 0; j < showList.size(); j++) {
//                if (showList.get(j).checked) {
//                    ItemGroup itemGroup = new ItemGroup();
//                    itemGroup.brightness = 50;
//                    itemGroup.temperature = 50;
//                    itemGroup.groupAress = showList.get(j).getMeshAddr();
//                    itemGroup.gpName = showList.get(j).getName();
//                    changeDataList(showList.get(j));
//                    sceneGroupAdapter.addData(itemGroup);
//                }
//
//                if (j == showList.size() - 1) {
//                    dialog.dismiss();
//                }
//            }
//        });
//
//
//    }
//
//    private void changeData(int position, List<DbGroup> showList) {
//        for (int k = 0; k < groupArrayList.size(); k++) {
//            if (showList.get(position).getMeshAddr() == 0xffff) {
//                groupArrayList.get(k).selected = true;
//            } else {
//                if (groupArrayList.get(k).getMeshAddr() == showList.get(position).getMeshAddr()) {
////                    showList.add(groupArrayList.get(k));
//                    groupArrayList.get(k).selected = true;
//                    for (int i = 0; i < groupArrayList.size(); i++) {
//                        if (groupArrayList.get(i).getMeshAddr() == 0xffff) {
//                            groupArrayList.get(i).selected = true;
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private void changeDataList(DbGroup item) {
//        for (int k = 0; k < groupArrayList.size(); k++) {
//            if (groupArrayList.get(k).getMeshAddr() == item.getMeshAddr()) {
////                    showList.add(groupArrayList.get(k));
//                groupArrayList.get(k).selected = true;
//                for (int i = 0; i < groupArrayList.size(); i++) {
//                    if (groupArrayList.get(i).getMeshAddr() == 0xffff) {
//                        groupArrayList.get(i).selected = true;
//                    }
//                }
//            }
//        }
//    }
//
//    private List<DbGroup> getShowList() {
//        List<DbGroup> showList = new ArrayList<>();
//        for (int k = 0; k < groupArrayList.size(); k++) {
//            if (!groupArrayList.get(k).selected) {
//                groupArrayList.get(k).checked = false;
//                showList.add(groupArrayList.get(k));
//            } else {
//                groupArrayList.get(k).checked = false;
//            }
//        }
//        return showList;
//    }
//
//    private List<DbGroup> getShowListForGuide() {
//        List<DbGroup> showList = new ArrayList<>();
//        int gpId=1;
//        if(groupArrayList.size()>1){
//            gpId=2;
//        }
//        for (int k = gpId-1; k < gpId; k++) {
//            if (!groupArrayList.get(k).selected) {
//                groupArrayList.get(k).checked = false;
//                showList.add(groupArrayList.get(k));
//            } else {
//                groupArrayList.get(k).checked = false;
//            }
//        }
//        return showList;
//    }
//
//    private void initView() {
//        inflater = LayoutInflater.from(this);
//        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//
//        editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
//        if (mInputMethodManager.isActive()) {
//            mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
//        }
//        StringUtils.initEditTextFilter(editName);
//    }
//
//    @OnClick({R.id.bt_save, R.id.edit_name, R.id.btn_sure_edit, R.id.bt_add})
//    public void onViewClicked(View view) {
//        switch (view.getId()) {
//            case R.id.img_header_menu_left:
//                finish();
//                break;
//            case R.id.bt_save:
//                if (checked()) {
//                    if(isShowChangeDataView){
//                        changeDatasave();
//                    }else{
//                        save();
//                    }
//                    setResult(Constant.RESULT_OK);
//                }
//                break;
//            case R.id.edit_name:
//                editName.setFocusable(true);//设置输入框可聚集
//                editName.setFocusableInTouchMode(true);//设置触摸聚焦
//                editName.requestFocus();//请求焦点
//                editName.findFocus();//获取焦点
//                mInputMethodManager.showSoftInput(editName, InputMethodManager.SHOW_FORCED);// 显示输入法
//                break;
//            case R.id.btn_sure_edit:
//                editName.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
//                if (mInputMethodManager.isActive()) {
//                    mInputMethodManager.hideSoftInputFromWindow(editName.getWindowToken(), 0);// 隐藏输入法
//                }
//                break;
//            case R.id.bt_add:
//                addNewItem();
//                break;
//        }
//    }
//
//    private boolean isSave = false;
//
//    private void save() {
//        showLoadingDialog(getString(R.string.saving));
//        new Thread(() -> {
//            String name = editName.getText().toString().trim();
////        List<ItemGroup> itemGroups = adapter.getData();
//            List<ItemGroup> itemGroups = itemGroupArrayList;
//
//            DbScene dbScene = new DbScene();
//            dbScene.setId(getSceneId());
//            dbScene.setName(name);
//            dbScene.setBelongRegionId((long) SharedPreferencesUtils.getCurrentUseRegion());
//            DBUtils.INSTANCE.saveScene(dbScene, false);
//
//            long idAction = dbScene.getId();
//
//            for (int i = 0; i < itemGroups.size(); i++) {
//                DbSceneActions sceneActions = new DbSceneActions();
//                sceneActions.setBelongSceneId(idAction);
//                sceneActions.setBrightness(itemGroups.get(i).brightness);
//                sceneActions.setColor(itemGroups.get(i).color);
//                sceneActions.setColorTemperature(itemGroups.get(i).temperature);
////            if (isSave) {//选择的组里面包含了所有组，用户仍然确定了保存,只保存所有组
////                sceneActions.setGroupAddr(0xFFFF);
////                DBUtils.saveSceneActions(sceneActions);
////                break;
////            } else {
//                sceneActions.setGroupAddr(itemGroups.get(i).groupAress);
//                DBUtils.INSTANCE.saveSceneActions(sceneActions);
////            }
//            }
//
//            try {
//                Thread.sleep(100);
//                addScene(idAction);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                hideLoadingDialog();
//                finish();
//            }
//        }).start();
//    }
//
//    private Long getSceneId() {
//        List<DbScene> list = DBUtils.INSTANCE.getSceneList();
//        List<Integer> idList = new ArrayList<>();
//        for (int i = 0; i < list.size(); i++) {
//            idList.add(list.get(i).getId().intValue());
//        }
//
//        int id = 0;
//        for (int i = 1; i < 17; i++) {
//            if (idList.contains(i)) {
//                Log.d("sceneID", "getSceneId: " + "aaaaa");
//                continue;
//            } else {
//                id = i;
//                Log.d("sceneID", "getSceneId: " + "bbbbb" + id);
//                break;
//            }
//        }
//
//        if (list.size() == 0) {
//            id = 1;
//        }
//
//        return Long.valueOf(id);
//    }
//
//
//    private void addScene(long id) throws InterruptedException {
//        byte opcode = (byte) Opcode.SCENE_ADD_OR_DEL;
//        List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
//        byte[] params;
//
//        for (int i = 0; i < list.size(); i++) {
//            int count = 0;
//            do {
//                count++;
//                Thread.sleep(300);
//                byte temperature = (byte) list.get(i).getColorTemperature();
//                if (temperature > 99)
//                    temperature = 99;
//                byte light = (byte) list.get(i).getBrightness();
//                if (light > 99)
//                    light = 99;
//                int color = list.get(i).getColor();
//                int red = (color & 0xff0000) >> 16;
//                int green = (color & 0x00ff00) >> 8;
//                int blue = (color & 0x0000ff);
//
//                byte minVal = (byte) 0x50;
//                if ((green & 0xff) <= minVal)
//                    green = 0;
//                if ((red & 0xff) <= minVal)
//                    red = 0;
//                if ((blue & 0xff) <= minVal)
//                    blue = 0;
//
//
//                params = new byte[]{0x01, (byte) id, light,
//                        (byte) red, (byte) green, (byte) blue, temperature};
//                    TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
//            } while (count < 3);
//        }
//    }
//
//    private boolean isChange = true;
//
//    private void changeDatasave() {
//        showLoadingDialog(getString(R.string.saving));
//        new Thread(() -> {
//            String name = editName.getText().toString().trim();
//            List<ItemGroup> itemGroups = itemGroupArrayList;
//            List<Integer> nameList = new ArrayList<>();
//
//            scene.setName(name);
//            DBUtils.INSTANCE.updateScene(scene);
//            long idAction = scene.getId();
//
//            DBUtils.INSTANCE.deleteSceneActionsList(DBUtils.INSTANCE.getActionsBySceneId(scene.getId()));
//
//            for (int i = 0; i < itemGroups.size(); i++) {
//                DbSceneActions sceneActions = new DbSceneActions();
//                sceneActions.setBelongSceneId(idAction);
//                sceneActions.setBrightness(itemGroups.get(i).brightness);
//                sceneActions.setColorTemperature(itemGroups.get(i).temperature);
//                sceneActions.setGroupAddr(itemGroups.get(i).groupAress);
//                sceneActions.setColor(itemGroups.get(i).color);
//
//                nameList.add(itemGroups.get(i).groupAress);
//                DBUtils.INSTANCE.saveSceneActions(sceneActions);
//            }
//
//            isChange=compareList(nameList,groupMeshAddrArrayList);
//
//            try {
//                Thread.sleep(100);
//                updateScene(idAction);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                hideLoadingDialog();
//                finish();
//            }
//        }).start();
//    }
//
//    private boolean compareList(List<Integer> actionsList, ArrayList<Integer> actionsList1) {
//        if(actionsList.size()==actionsList1.size()){
//            return !actionsList1.containsAll(actionsList);
//        }else{
//            return true;
//        }
//    }
//
//    private void updateScene(long id) throws InterruptedException {
//        deleteScene(id);
//        byte opcode = (byte) Opcode.SCENE_ADD_OR_DEL;
//        List<DbSceneActions> list = DBUtils.INSTANCE.getActionsBySceneId(id);
//        byte[] params;
//        for (int i = 0; i < list.size(); i++) {
//            Thread.sleep(100);
//            byte temperature = (byte) list.get(i).getColorTemperature();
//            if (temperature > 99)
//                temperature = 99;
//            byte light = (byte) list.get(i).getBrightness();
//            if (light > 99)
//                light = 99;
//            int color = list.get(i).getColor();
//            int red = (color & 0xff0000) >> 16;
//            int green = (color & 0x00ff00) >> 8;
//            int blue = (color & 0x0000ff);
//
//            byte minVal = (byte) 0x50;
//            if ((green & 0xff) <= minVal)
//                green = 0;
//            if ((red & 0xff) <= minVal)
//                red = 0;
//            if ((blue & 0xff) <= minVal)
//                blue = 0;
//
//            String logStr = String.format("R = %x, G = %x, B = %x", red, green, blue);
//            Log.d("RGBCOLOR", logStr);
//            params = new byte[]{0x01, (byte) id, light,
//                    (byte) red, (byte) green, (byte) blue, temperature};
//            TelinkLightService.Instance().sendCommandNoResponse(opcode, list.get(i).getGroupAddr(), params);
//        }
//    }
//
//    private void deleteScene(long id) {
//        if(isChange){
//            byte opcode = Opcode.SCENE_ADD_OR_DEL;
//            byte[] params;
//            params = new byte[]{0x00, (byte) id};
//            try {
//                Thread.sleep(100);
//                TelinkLightService.Instance().sendCommandNoResponse(opcode, 0xFFFF, params);
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private boolean checked() {
//
//        String name = editName.getText().toString().trim();
//        if (StringUtils.compileExChar(name)) {
//            ToastUtils.showLong(R.string.rename_tip_check);
//            return false;
//        }
//
//        if (itemGroupArrayList.size() == 0) {
//            ToastUtils.showLong(R.string.add_scene_gp_tip);
//            return false;
//        }
//        return true;
//    }
//}
