package com.dadoutek.uled.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.CleanUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.activity.SplashActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbDataChange;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.DbModel.DbRegion;
import com.dadoutek.uled.model.DbModel.DbScene;
import com.dadoutek.uled.model.DbModel.DbSceneActions;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.model.Opcode;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.AppUtils;
import com.dadoutek.uled.util.DBManager;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.telink.bluetooth.light.DeviceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by hejiajun on 2018/4/16.
 */

public class MeFragment extends Fragment {

    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.app_version_name)
    TextView appVersionName;
    @BindView(R.id.app_version)
    TextView appVersion;
    Unbinder unbinder;
    @BindView(R.id.chear_cache)
    Button chearCache;
    @BindView(R.id.update_ite)
    Button updateIte;
    @BindView(R.id.copy_data_base)
    Button copyDataBase;
    @BindView(R.id.exit_login)
    Button exitLogin;
    @BindView(R.id.one_click_backup)
    Button oneClickBackup;
    @BindView(R.id.one_click_reset)
    Button oneClickReset;
    private LayoutInflater inflater;

    private DbDataChange dbDataChange;
    private List<DbDataChange> dbDataChangeList;
    private Dialog loadDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.fragment_me,
                null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        String versionName = AppUtils.getVersionName(getActivity());
        appVersion.setText(versionName);
        //暂时屏蔽
        updateIte.setVisibility(View.GONE);
        if (SharedPreferencesUtils.isDeveloperModel()) {
            copyDataBase.setVisibility(View.VISIBLE);
            chearCache.setVisibility(View.VISIBLE);
        } else {
            copyDataBase.setVisibility(View.GONE);
            chearCache.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            DBManager.getInstance().copyDatabaseToSDCard(activity);
            ToastUtils.showShort(R.string.copy_complete);
        }
    }

    long[] mHints = new long[6];//初始全部为0

    @OnClick({R.id.chear_cache, R.id.update_ite, R.id.copy_data_base, R.id.app_version, R.id.exit_login, R.id.one_click_backup,R.id.one_click_reset})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.chear_cache:
                emptyTheCache();
                break;
            case R.id.update_ite:
                ToastUtils.showShort(R.string.wait_develop);
                break;
            case R.id.copy_data_base:
                verifyStoragePermissions(getActivity());
                break;
            case R.id.app_version:
                developerMode();
                break;
            case R.id.exit_login:
                exitLogin();
                break;
            case R.id.one_click_backup:
                syncDataStep1();
                break;
            case R.id.one_click_reset:
                resetAllLight();
                break;
        }

    }

    private void resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now));
        new Thread(() -> {
            List<DbGroup> list = DBUtils.getGroupList();
            List<DbLight> lightList=new ArrayList<>();
            DeviceInfo deviceInfo=TelinkLightApplication.getInstance().getConnectDevice();
            if(deviceInfo==null){
                ToastUtils.showLong(R.string.disconected);
                return;
            }

            for(int i=0;i<list.size();i++){
                lightList.addAll(DBUtils.getLightByGroupID(list.get(i).getId()));
            }

            int index1=0;
            int index2=0;
            for(int k=0;k<lightList.size();k++){
                if(lightList.get(k).getMeshAddr()==deviceInfo.meshAddress){
                    index2=k;
                    break;
                }
            }
            Collections.swap(lightList,index1,index2);

            for(int j=lightList.size()-1;j>=0;j--){
                if(deviceInfo!=null){
                        try {
                            byte opcode = (byte) Opcode.KICK_OUT;
                            TelinkLightService.Instance().sendCommandNoResponse(opcode, lightList.get(j).getMeshAddr(), null);
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }finally {
                            DBUtils.deleteLight(lightList.get(j));
                            if(j==0){
                                hideLoadingDialog();
                            }
                        }
                }
            }
        }).start();
    }

    public void showLoadingDialog(String content) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.dialogview, null);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);
        TextView tvContent = (TextView) v.findViewById(R.id.tvContent);
        tvContent.setText(content);


//        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
//
//        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
//                R.animator.load_animation);

//        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = new Dialog(getActivity(),
                    R.style.FullHeightDialog);
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog.isShowing()) {
            loadDialog.setCancelable(true);
            loadDialog.setCanceledOnTouchOutside(false);
            loadDialog.setContentView(layout);
            loadDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog.dismiss();
        }
    }

    private void syncDataStep1() {
        dbDataChangeList = DBUtils.getDataChangeAll();
        for (int i = 0; i < dbDataChangeList.size(); i++) {
            getLocalData(dbDataChangeList.get(i).getTableName(),
                    dbDataChangeList.get(i).getChangeId(),
                    dbDataChangeList.get(i).getChangeType());
        }
    }

    private void getLocalData(String tableName, Long changeId, String type) {
        switch (tableName) {
            case "DB_GROUPS":
                DbGroup group = DBUtils.getGroupByID(changeId);
                break;
            case "DB_LIGHT":
                DbLight light = DBUtils.getLightByID(changeId);
                break;
            case "DB_REGION":
                DbRegion region = DBUtils.getRegionByID(changeId);
                break;
            case "DB_SCENE":
                DbScene scene = DBUtils.getSceneByID(changeId);
                break;
            case "DB_SCENE_ACTIONS":
                DbSceneActions actions = DBUtils.getSceneActionsByID(changeId);
                break;
            case "DB_USER":
                DbUser user = DBUtils.getUserByID(changeId);
                break;
        }
    }

//    private void change(String table,){
//
//    }

    private void exitLogin() {
        SharedPreferencesHelper.putBoolean(getActivity(), Constant.IS_LOGIN, false);
        restartApplication();
    }

    private void developerMode() {
        //将mHints数组内的所有元素左移一个位置
        System.arraycopy(mHints, 1, mHints, 0, mHints.length - 1);
        //获得当前系统已经启动的时间
        mHints[mHints.length - 1] = SystemClock.uptimeMillis();
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            ToastUtils.showLong(R.string.developer_mode);
            copyDataBase.setVisibility(View.VISIBLE);
            chearCache.setVisibility(View.VISIBLE);
            SharedPreferencesUtils.setDeveloperModel(true);
        }
    }

    //清空缓存初始化APP
    private void emptyTheCache() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.empty_cache_title))
                .setMessage(getActivity().getString(R.string.empty_cache_tip))
                .setNegativeButton(getActivity().getString(R.string.btn_cancel), (dialog, which) -> {
                })
                .setPositiveButton(getActivity().getString(R.string.btn_sure), (dialog, which) -> {
                    DBUtils.deleteAllData();
                    CleanUtils.cleanInternalSp();
                    CleanUtils.cleanExternalCache();
                    CleanUtils.cleanInternalFiles();
                    CleanUtils.cleanInternalCache();
                    ToastUtils.showShort(R.string.clean_tip);
                    restartApplication();
                })
                .create().show();
    }

    //重启app并杀死原进程
    private void restartApplication() {
        ActivityUtils.startActivity(SplashActivity.class);
        ActivityUtils.finishAllActivitiesExceptNewest();
    }
}
