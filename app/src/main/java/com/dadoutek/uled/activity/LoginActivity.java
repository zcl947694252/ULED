package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.DbModel.DbUser;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.model.Cmd;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.LogUtils;
import com.dadoutek.uled.util.NetworkUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dadoutek.uled.util.NetworkUtils.md5;

/**
 * Created by hejiajun on 2018/5/15.
 */

public class LoginActivity extends TelinkBaseActivity {
    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
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

    private final MyHandler mHandler = new MyHandler(this);
    private DbUser dbUser;
    private String salt = "";
    private String MD5Password;
    private String editUserName;
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

    private void initView() {
        txtHeaderTitle.setText(R.string.user_login_title);
        if (SharedPreferencesHelper.getBoolean(LoginActivity.this, Constant.IS_LOGIN, false)) {
            TransformView();
        }
    }

    @OnClick({R.id.img_header_menu_left, R.id.btn_login, R.id.btn_register, R.id.forget_password})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
            case R.id.btn_login:
                creatMessage(Cmd.GETACCOUNT, 0);
                break;
            case R.id.btn_register:
                Intent intent=new Intent(LoginActivity.this, PhoneVerificationActivity.class);
                intent.putExtra("fromLogin","register");
                startActivity(intent);
                break;
            case R.id.forget_password:
                forgetPassword();
                break;
        }
    }

    private void forgetPassword() {
        Intent intent=new Intent(LoginActivity.this, PhoneVerificationActivity.class);
        intent.putExtra("fromLogin","forgetPassword");
        startActivity(intent);
    }

    private void creatMessage(int what, int arg) {
        Message message = new Message();
        message.what = what;
        message.arg1 = arg;
        mHandler.sendMessage(message);
    }

    private class MyHandler extends Handler {
        //防止内存溢出
        private final WeakReference<LoginActivity> mWeakActivity;

        private MyHandler(LoginActivity mWeakActivity) {
            this.mWeakActivity = new WeakReference<>(mWeakActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LoginActivity activity = mWeakActivity.get();
            switch (msg.what) {
                case Cmd.GETACCOUNT:
                    activity.getAccount();
                    break;
                case Cmd.GETSALT:
                    activity.getSalt();
                    break;
                case Cmd.STLOGIN:
                    activity.login();
                    break;
            }
        }
    }

    private void getAccount() {
        showLoadingDialog(getString(R.string.logging_tip));
        editUserName = editUserPhoneOrEmail.getEditText().getText().toString().trim();
        editPassWord = editUserPassword.getEditText().getText().toString().trim();

        Map<String, String> map = new HashMap<>();
        map.put("phone", editUserName);
        map.put("channel", dbUser.getChannel());
        NetworkUtils.getAccountApi()
                .getAccount(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerAccount);
    }

    private void getSalt() {
        NetworkUtils.getSaltApi()
                .getsalt(dbUser.getAccount())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerSalt);
    }

    private void login() {
        NetworkUtils.getloginApi()
                .login(dbUser.getAccount(), MD5Password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerLogin);
    }

    Observer<Response<String>> observerAccount = new Observer<Response<String>>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(Response<String> stringResponse) {
            if (stringResponse.getErrorCode() == 0) {
                LogUtils.d("logging" + stringResponse.getErrorCode() + "获取成功account");
                dbUser.setAccount(stringResponse.getT());
                creatMessage(Cmd.GETSALT, 0);
            } else {
                ToastUtils.showLong(R.string.name_or_password_error);
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

    Observer<Response<String>> observerSalt = new Observer<Response<String>>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(Response<String> stringResponse) {
            if (stringResponse.getErrorCode() == 0) {
                LogUtils.d("logging" + stringResponse.getErrorCode() + "获取成功salt");
                salt = stringResponse.getT();
                MD5Password = md5(md5(md5(editPassWord) + dbUser.getAccount()) + salt);
                creatMessage(Cmd.STLOGIN, 0);
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

    private void TransformView() {
        if (isFirstLauch) {
            startActivityForResult(new Intent(this, AddMeshActivity.class), REQ_MESH_SETTING);
        } else {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
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
        Intent intent = new Intent(LoginActivity.this, DeviceScanningActivity.class);
        intent.putExtra("isInit", true);
        startActivity(intent);
        finish();
    }

}
