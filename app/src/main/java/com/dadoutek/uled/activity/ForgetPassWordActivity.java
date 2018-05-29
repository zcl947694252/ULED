package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.util.LogUtils;
import com.dadoutek.uled.intf.NetworkFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by hejiajun on 2018/5/18.
 */

public class ForgetPassWordActivity extends TelinkBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.edit_new_password)
    TextInputLayout editNewPassword;
    @BindView(R.id.edit_new_password_sure)
    TextInputLayout editNewPasswordSure;
    @BindView(R.id.btn_sure)
    Button btnSure;

    private String phone;
    private DbUser dbUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        Intent intent=getIntent();
        phone=intent.getStringExtra("phone");
        dbUser=new DbUser();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.update_password);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.btn_sure)
    public void onViewClicked() {
        if(checkPassWordOk()){
            getAccount();
        }
    }

    private void getAccount() {
        showLoadingDialog(getString(R.string.updating_password));
        Map<String, String> map = new HashMap<>();
        map.put("phone", phone);
        map.put("channel", dbUser.getChannel());
        NetworkFactory.getApi()
                .getAccount(phone, dbUser.getChannel())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerAccount);
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
                updatePassword();
            } else {
                ToastUtils.showLong(R.string.get_account_fail);
            }
        }

        @Override
        public void onError(Throwable e) {
            hideLodingDialog();
            Toast.makeText(ForgetPassWordActivity.this, "onError:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onComplete() {
        }
    };

    Observer<Response<DbUser>> observerUpdatePassword = new Observer<Response<DbUser>>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(Response<DbUser> stringResponse) {
            hideLodingDialog();
            if (stringResponse.getErrorCode() == 0) {
                LogUtils.d("logging" + stringResponse.getErrorCode() + "更改成功");
                ToastUtils.showLong(R.string.tip_update_password_success);
                finish();
            } else {
                ToastUtils.showLong(R.string.tip_update_password_fail);
            }
        }

        @Override
        public void onError(Throwable e) {
            hideLodingDialog();
            Toast.makeText(ForgetPassWordActivity.this, "onError:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onComplete() {
        }
    };

    private void updatePassword() {
        NetworkFactory.getApi()
                .putPassword(dbUser.getAccount(),md5(editNewPassword.getEditText().getText().toString().trim()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observerUpdatePassword);
    }

    private boolean checkPassWordOk() {
        String password1=editNewPassword.getEditText().getText().toString().trim();
        String password2=editNewPasswordSure.getEditText().getText().toString().trim();

        if(compileExChar(password1)||compileExChar(password2)){
            ToastUtils.showLong(R.string.password_input_error);
            return false;
        }

        if(!password1.equals(password2)){
            ToastUtils.showLong(R.string.different_input);
            return false;
        }

        return true;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
