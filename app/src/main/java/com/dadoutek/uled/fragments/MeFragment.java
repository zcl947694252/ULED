package com.dadoutek.uled.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.blankj.utilcode.util.CleanUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.util.AppUtils;
import com.dadoutek.uled.util.DBManager;

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
    private LayoutInflater inflater;

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

    @OnClick({R.id.chear_cache, R.id.update_ite, R.id.copy_data_base})
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
                    DBManager.getInstance().deleteAllData();
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
        final Intent intent = TelinkLightApplication.getInstance().getPackageManager().
                getLaunchIntentForPackage(TelinkLightApplication.getInstance().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Process.killProcess(Process.myPid());
    }
}
