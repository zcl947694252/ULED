package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.DbModel.DBUtils;
import com.dadoutek.uled.DbModel.DbUser;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.intf.NetworkObserver;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DaoSessionInstance;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.util.LogUtils;
import com.dadoutek.uled.util.NetworkUtils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.dadoutek.uled.util.NetworkUtils.md5;

/**
 * Created by hejiajun on 2018/5/15.
 */

public class LoginActivity extends TelinkBaseActivity {
    //    @BindView(R.id.img_header_menu_left)
//    ImageView imgHeaderMenuLeft;
//    @BindView(R.id.txt_header_title)
//    TextView txtHeaderTitle;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.edit_user_password)
    TextInputLayout editUserPassword;
    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.edit_user_phone_or_email)
    TextInputLayout editUserPhoneOrEmail;
    @BindView(R.id.btn_register)
    Button btnRegister;
    @BindView(R.id.forget_password)
    TextView forgetPassword;

    private DbUser dbUser;
    private String salt = "";
    private String MD5Password;
    private String phone;
    private String editPassWord;
    private boolean isFirstLauch;
    private static final int REQ_MESH_SETTING = 0x01;
    public static final String IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        initData();
        initView();
    }

    private void initData() {
        dbUser = new DbUser();
        Intent intent = getIntent();
        isFirstLauch = intent.getBooleanExtra(IS_FIRST_LAUNCH, true);
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.user_login_title);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initView() {
        initToolbar();
//        txtHeaderTitle.setText(R.string.user_login_title);
        if (SharedPreferencesHelper.getBoolean(LoginActivity.this, Constant.IS_LOGIN, false)) {
            TransformView();
        }
    }

    @OnClick({R.id.btn_login, R.id.btn_register, R.id.forget_password})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_register:
                Intent intent = new Intent(LoginActivity.this, PhoneVerificationActivity.class);
                intent.putExtra("fromLogin", "register");
                startActivity(intent);
                break;
            case R.id.forget_password:
                forgetPassword();
                break;
        }
    }

    private void forgetPassword() {
        Intent intent = new Intent(LoginActivity.this, PhoneVerificationActivity.class);
        intent.putExtra("fromLogin", "forgetPassword");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ActivityUtils.finishAllActivities(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void login() {
        showLoadingDialog(getString(R.string.logging_tip));
        phone = editUserPhoneOrEmail.getEditText().getText().toString().trim();
        editPassWord = editUserPassword.getEditText().getText().toString().trim();

        Map<String, String> map = new HashMap<>();
        map.put("phone", phone);
        map.put("channel", dbUser.getChannel());

        //首先调用getAccount接口
        NetworkUtils.getAccountApi()
                .getAccount(map)
                .observeOn(Schedulers.io())
                .flatMap((Function<Response<String>, ObservableSource<Response<String>>>)
                        stringResponse -> {
                            dbUser.setAccount(stringResponse.getT());
                            //然后调用getSalt接口
                            return NetworkUtils.getSaltApi()
                                    .getsalt(dbUser.getAccount());
                        })
                .flatMap((Function<Response<String>, ObservableSource<Response<DbUser>>>)
                        stringResponse -> {
                            salt = stringResponse.getT();
                            MD5Password = md5(md5(md5(editPassWord) + dbUser.getAccount()) + salt);
                            //最后调用登录接口
                            return NetworkUtils.getloginApi()
                                    .login(dbUser.getAccount(), MD5Password);
                        })

                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NetworkObserver<DbUser>(){
                    @Override
                    public void onNext(@NotNull Response<DbUser> t) {
                        super.onNext(t);
                        hideLodingDialog();
                        LogUtils.d("logging" + t.getErrorCode() + "登录成功");
                        ToastUtils.showLong(R.string.login_success);
                        SharedPreferencesHelper.putBoolean(LoginActivity.this, Constant.IS_LOGIN, true);
                        setupMesh();
                        initDatBase(t.getT());
                        TransformView();
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        hideLodingDialog();
                    }
                });
    }

    Observer<Response<DbUser>> observerLogin = new Observer<Response<DbUser>>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(Response<DbUser> stringResponse) {
            hideLodingDialog();
            if (stringResponse.getErrorCode() == 0) {
                LogUtils.d("logging" + stringResponse.getErrorCode() + "登录成功");
                ToastUtils.showLong(R.string.login_success);
                SharedPreferencesHelper.putBoolean(LoginActivity.this, Constant.IS_LOGIN, true);
                setupMesh();
                initDatBase(stringResponse.getT());
                TransformView();
            } else {
                ToastUtils.showLong(R.string.login_fail);
            }
        }

        @Override
        public void onError(Throwable e) {
            hideLodingDialog();
            Toast.makeText(LoginActivity.this, "onError:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onComplete() {
        }
    };

    private void initDatBase(DbUser user) {
        //首先保存当前数据库名
        SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, user.getAccount());

        //数据库分库
        DaoSessionInstance.destroySession();
        DaoSessionInstance.getInstance();

        //从云端用户表同步数据到本地
        dbUser = user;
        DBUtils.saveUser(dbUser);
        DBUtils.createAllLightControllerGroup(this);
    }

    private void setupMesh() {
        String name = SharedPreferencesHelper.getMeshName(this);
        String pwd = SharedPreferencesHelper.getMeshPassword(this);

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(pwd)) {
//                Mesh mesh = (Mesh) FileSystem.readAsObject(this, name + "." + pwd);
            TelinkLightApplication application = (TelinkLightApplication) getApplication();
            Mesh mesh = application.getMesh();
            mesh.name = phone;
            mesh.password = Constant.NEW_MESH_PASSWORD;
            mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME;
            mesh.password = Constant.DEFAULT_MESH_FACTORY_PASSWORD;
            mesh.saveOrUpdate(this);
            application.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, phone);
            SharedPreferencesHelper.saveMeshPassword(this, Constant.NEW_MESH_PASSWORD);
        }
    }

    private void TransformView() {
//        if (isFirstLauch) {
//            startActivityForResult(new Intent(this, AddMeshActivity.class), REQ_MESH_SETTING);
//        } else {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //退出MeshSetting后进入DeviceScanning
        if (requestCode == REQ_MESH_SETTING) {
            gotoDeviceScanning();
        }
    }

    /**
     * 进入引导流程，也就是进入DeviceActivity。
     */
    private void gotoDeviceScanning() {
        //首次进入APP才进入引导流程
        Intent intent = new Intent(LoginActivity.this, DeviceScanningNewActivity.class);
        intent.putExtra("isInit", true);
        startActivity(intent);
        finish();
    }

}
