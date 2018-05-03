package com.dadoutek.uled.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.adapter.SceneGroupAdapter;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
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

public class SceneSetAct extends TelinkBaseActivity{
    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.tv_brightness)
    TextView tvBrightness;
    @BindView(R.id.sb_brightness)
    SeekBar sbBrightness;
    @BindView(R.id.tv_temperature)
    TextView tvTemperature;
    @BindView(R.id.sb_temperature)
    SeekBar sbTemperature;
    @BindView(R.id.bt_save)
    Button btSave;
    @BindView(R.id.edit_name)
    EditText editName;
    @BindView(R.id.btn_sure_edit)
    Button btnSureEdit;
    @BindView(R.id.scene_group_list_view)
    RecyclerView sceneGroupListView;

    private int brightness = 0;
    private int temperature = 0;
    private Groups groups;
    private Scenes scenes;
    private LayoutInflater inflater;
    private SceneGroupAdapter adapter;

    private DataManager dataManager;
    private TelinkLightApplication telinkLightApplication;
    private ArrayList<Group> groupArrayList = new ArrayList<>();
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
        groupArrayList.add(dataManager.createAllLightControllerGroup());
        List<Group> groupList = groups.get();
        for (Group group : groupList) {
            if (group.containsLightList.size() > 0)
                group.checked = false;
            groupArrayList.add(group);
        }

        LinearLayoutManager layoutmanager = new LinearLayoutManager(this);
        layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        sceneGroupListView.setLayoutManager(layoutmanager);
        this.adapter = new SceneGroupAdapter(R.layout.scene_group_item,groupArrayList);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                boolean checked = groupArrayList.get(position).checked;
                if (checked) {
                    groupArrayList.get(position).checked = false;
                } else {
                    groupArrayList.get(position).checked = true;
                }

                adapter.notifyDataSetChanged();
            }
        });
        sceneGroupListView.setAdapter(adapter);
    }

    private void initView() {
        inflater = LayoutInflater.from(this);
        tvBrightness.setText(getString(R.string.device_setting_brightness, 0 + ""));
        tvTemperature.setText(getString(R.string.device_setting_temperature, 0 + ""));
        txtHeaderTitle.setText(R.string.creat_scene);
        sbBrightness.setOnSeekBarChangeListener(barChangeListener);
        sbTemperature.setOnSeekBarChangeListener(barChangeListener);
//        sceneGroupListView.set(this);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private SeekBar.OnSeekBarChangeListener barChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private long preTime;
        private int delayTime = 100;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            this.onValueChange(seekBar, seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            this.preTime = System.currentTimeMillis();
            this.onValueChange(seekBar, seekBar.getProgress());
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            if (progress % 5 != 0)
                return;

            long currentTime = System.currentTimeMillis();

            if ((currentTime - this.preTime) < this.delayTime) {
                this.preTime = currentTime;
                return;
            }

            this.onValueChange(seekBar, progress);
        }

        private void onValueChange(View view, int progress) {
            if (view == sbBrightness) {
                brightness = progress;
                tvBrightness.setText(getString(R.string.device_setting_brightness, progress + ""));
            } else if (view == sbTemperature) {
                temperature = progress;
                tvTemperature.setText(getString(R.string.device_setting_temperature, progress + ""));
            }
        }
    };

//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
////        Group group=adapter.getItem(position);
//        boolean checked = groupArrayList.get(position).checked;
//        if (checked) {
//            groupArrayList.get(position).checked = false;
//        } else {
//            groupArrayList.get(position).checked = true;
//        }
//
//        adapter.notifyDataSetChanged();
//    }

//    private static class GroupItemHolder {
//        public TextView txtName;
//        public ImageView rightBt;
//    }

//    final class GroupListAdapter extends BaseAdapter {
//        ArrayList<Group> groupArrayList = new ArrayList<>();
//
//        public GroupListAdapter(ArrayList<Group> groupArrayList) {
//            this.groupArrayList = groupArrayList;
//        }
//
//        @Override
//        public boolean isEnabled(int position) {
//            return false;
//        }
//
//        @Override
//        public int getCount() {
//            return groupArrayList.size();
//        }
//
//        @Override
//        public Group getItem(int position) {
//            return groupArrayList.get(position);
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//
//            GroupItemHolder holder;
//
//            if (convertView == null) {
//
//                convertView = inflater.inflate(R.layout.scene_group_item, null);
//
//                TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
//
//                ImageView rightBt = (ImageView) convertView.findViewById(R.id.right_bt);
//
//                holder = new GroupItemHolder();
//
//                holder.txtName = txtName;
//                holder.rightBt = rightBt;
//
//                convertView.setTag(holder);
//
//            } else {
//                holder = (GroupItemHolder) convertView.getTag();
//            }
//
//            Group group = this.getItem(position);
//
//            if (group != null) {
//                if (group.textColor == null)
//                    group.textColor = getResources()
//                            .getColorStateList(R.color.black);
//
//                holder.txtName.setText(group.name);
//                holder.txtName.setTextColor(group.textColor);
//                holder.txtName.setTag(group.meshAddress);
//                if (group.checked) {
//                    holder.rightBt.setVisibility(View.VISIBLE);
//                } else {
//                    holder.rightBt.setVisibility(View.INVISIBLE);
//                }
//                holder.rightBt.setTag(group.meshAddress);
//            }
//
//            return convertView;
//        }
//    }

    @OnClick({R.id.img_header_menu_left, R.id.bt_save, R.id.edit_name, R.id.btn_sure_edit})
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
        }
    }

    private void save() {
        scenes.temperature = temperature;
        scenes.brightness = brightness;
        scenes.sceneName=editName.getText().toString().trim();

        for (int k = 0; k < groupArrayList.size(); k++) {
            if (groupArrayList.get(k).checked) {
                groupsAddressList.add(groupArrayList.get(k).meshAddress);
            }
        }

        scenes.groupsAddressList = this.groupsAddressList;
        dataManager.addScene(scenes);
    }

    private boolean checked() {

        String name=editName.getText().toString().trim();
        if(StringUtils.compileExChar(name)){
            ToastUtils.showLong(R.string.rename_tip_check);
            return false;
        }

        for (int j = 0; j < groupArrayList.size(); j++) {
            if (groupArrayList.get(j).checked) {
                return true;
            }
        }

        ToastUtils.showLong(R.string.scene_tip);
        return false;
    }
}
