package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.model.Response;
import com.dadoutek.uled.intf.NetworkFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dadoutek.uled.intf.NetworkFactory.md5;

/**
 * Created by hejiajun on 2018/5/16.
 */

public class RegisterActivity extends TelinkBaseActivity {
    @BindView(R.id.edit_user_password)
    TextInputLayout editUserPassword;
    @BindView(R.id.register_completed)
    Button registerCompleted;
    @BindView(R.id.edit_user_phone)
    TextInputLayout editUserName;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private String userName;
    private String userPassWord;
    private String MD5PassWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        initView();


    }

    private void initView() {
        initToolbar();
        String phone = getIntent().getStringExtra("phone");
        if (!phone.isEmpty()) {
            if (editUserName.getEditText() != null)
                editUserName.getEditText().setText(phone);
        }
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.register_title_name);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.register_completed)
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.register_completed:
                if (checkIsOK()) {
                    register();
                }
                break;
        }
    }

    private void register() {
        showLoadingDialog(getString(R.string.registing));
        MD5PassWord = md5(userPassWord);
        NetworkFactory.getApi()
                .register(userName, MD5PassWord, userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    Observer<Response<DbUser>> observer = new Observer<Response<DbUser>>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(Response<DbUser> dbUserResponse) {
            hideLoadingDialog();
            if (dbUserResponse.getErrorCode() == 0) {
                Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(RegisterActivity.this, "ErrorCode = " + dbUserResponse.getErrorCode(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable e) {
            hideLoadingDialog();
            Toast.makeText(RegisterActivity.this, "onError:" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onComplete() {

        }
    };

    private boolean checkIsOK() {
        userName = editUserName.getEditText().getText().toString().trim();
        userPassWord = editUserPassword.getEditText().getText().toString().trim();

        if (compileExChar(userName)) {
            ToastUtils.showLong(R.string.phone_input_error);
            return false;
        } else if (compileExChar(userName) || compileExChar(userPassWord)) {
            ToastUtils.showLong(R.string.tip_register_input_error);
            return false;
        } else {
            return true;
        }
    }
}
