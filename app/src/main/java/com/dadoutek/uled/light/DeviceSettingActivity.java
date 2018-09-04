package com.dadoutek.uled.light;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.communicate.Commander;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.model.HttpModel.DownLoadFileModel;
import com.dadoutek.uled.network.NetworkObserver;
import com.dadoutek.uled.ota.OTAUpdateActivity;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.NetWorkUtils;
import com.dadoutek.uled.util.SharedPreferencesUtils;
import com.dadoutek.uled.util.StringUtils;
import com.laojiang.retrofithttp.weight.downfilesutils.FinalDownFiles;
import com.laojiang.retrofithttp.weight.downfilesutils.action.FinalDownFileResult;
import com.laojiang.retrofithttp.weight.downfilesutils.downfiles.DownInfo;
import com.telink.TelinkApplication;

import org.jetbrains.annotations.NotNull;

public final class DeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private TextView tvOta;
    TextView txtTitle;
    private DeviceSettingFragment settingFragment;
    private String localVersion;

    private DbLight light;
    private int gpAddress;
    private String fromWhere;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;
    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                finish();
            } else if (v == tvOta) {
                gotoUpdateView();
            }
        }
    };

    private void gotoUpdateView() {
        if (checkHaveNetWork()) {
//            //1.获取服务器版本url
//            String serverVersionUrl=getServerVersion();
            transformView();
        } else {
            ToastUtils.showLong(R.string.network_disconect);
        }
    }

    //2.对比服务器和本地版本大小
    private boolean compareServerVersion(String serverVersionUrl) {
        if (!serverVersionUrl.isEmpty() && !localVersion.isEmpty()) {
            int serverVersionNum = Integer.parseInt(StringUtils.versionResolutionURL(serverVersionUrl, 1));
            int localVersionNum = Integer.parseInt(StringUtils.versionResolution(localVersion, 1));
            if (serverVersionNum > localVersionNum) {
                //3.服务器版本是最新弹窗提示优先执行下载（下载成功之后直接跳转）
                download(serverVersionUrl);
//                download("https://cdn.beesmartnet.com/static/soybean/L-2.0.8-L208.bin");
                return true;
            } else {
                //4.本地已经是最新直接跳转升级页面
                transformView();
                return false;
            }
        } else {
            ToastUtils.showLong(R.string.getVsersionFail);
        }
        return false;
    }

    private String getServerVersion() {
        DownLoadFileModel.INSTANCE.getUrl().subscribe(new NetworkObserver<String>() {
            @Override
            public void onNext(String s) {
                compareServerVersion(s);
            }

            @Override
            public void onError(@NotNull Throwable e) {
                super.onError(e);
                ToastUtils.showLong(R.string.get_server_version_fail);
            }
        });
        return "";
    }

    private void transformView() {
        TelinkLightService.Instance().idleMode(true);
        Intent intent = new Intent(DeviceSettingActivity.this, OTAUpdateActivity.class);
        intent.putExtra(Constant.UPDATE_LIGHT, light);
        startActivity(intent);
        finish();
    }

    private boolean checkHaveNetWork() {
        return NetWorkUtils.isNetworkAvalible(this);
    }

    private void getVersion() {
        int dstAdress = 0;
        if (TelinkApplication.getInstance().getConnectDevice() != null) {
            Commander.INSTANCE.getDeviceVersion(light.getMeshAddr(), (s) -> {
                localVersion = s;
                if (txtTitle != null) {
                    txtTitle.setVisibility(View.VISIBLE);
                    txtTitle.setText(localVersion);
                    tvOta.setVisibility(View.VISIBLE);
                }
                return null;
            }, () -> {
                if (txtTitle != null) {
                    txtTitle.setVisibility(View.GONE);
                    tvOta.setVisibility(View.GONE);
                }
                return null;
            });
        } else {
            dstAdress = 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_device_setting);
        initView();
        getVersion();
    }

    private void initView() {
        this.light = (DbLight) this.getIntent().getExtras().get(Constant.LIGHT_ARESS_KEY);
        this.fromWhere = this.getIntent().getStringExtra(Constant.LIGHT_REFRESH_KEY);
        this.gpAddress = this.getIntent().getIntExtra(Constant.GROUP_ARESS_KEY, 0);
        mApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, mApplication.getMesh().getName(), mApplication.getMesh().getPassword());
        txtTitle = (TextView) this
                .findViewById(R.id.txt_header_title);
        txtTitle.setText("");
        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.tvOta = (TextView) this
                .findViewById(R.id.tv_ota);
        this.tvOta.setOnClickListener(this.clickListener);

        this.settingFragment = (DeviceSettingFragment) this
                .getSupportFragmentManager().findFragmentById(
                        R.id.device_setting_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.light = light;
    }

    private void download(String url) {
        String[] downUrl = new String[]{url};
        String localPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/L-2.0.8-L208.bin";
        FinalDownFiles finalDownFiles = new FinalDownFiles(true, this, downUrl[0],localPath
                , new FinalDownFileResult() {
            @Override
            public void onSuccess(DownInfo downInfo) {
                super.onSuccess(downInfo);
                Log.i("成功==", downInfo.toString());
                SharedPreferencesUtils.saveUpdateFilePath(localPath);
                transformView();
            }

            @Override
            public void onCompleted() {
                super.onCompleted();
                Log.i("完成==", "./...");
            }

            @Override
            public void onStart() {
                super.onStart();
                Log.i("开始==", "./...");
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.i("暂停==", "./...");
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.i("结束了一切", "是的没错");
            }

            @Override
            public void onLoading(long readLength, long countLength) {
                super.onLoading(readLength, countLength);
                Log.i("下载过程==", countLength + "");
            }

            @Override
            public void onErroe(String message, int code) {
                super.onErroe(message, code);
                Log.i("错误==", message + "");
                ToastUtils.showLong(R.string.download_pack_fail);
            }
        });
    }
}
