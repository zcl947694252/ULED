package com.dadoutek.uled.user;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.model.Constants;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by hejiajun on 2018/5/18.
 */

public class ManagerVerificationActivity extends TelinkBaseActivity {
    @BindView(R.id.ccp)
    CountryCodePicker ccp;
    @BindView(R.id.edit_phone_number)
    TextInputLayout editPhoneNumber;
    @BindView(R.id.btn_send_verification)
    Button btnSendVerification;
    @BindView(R.id.edit_verification)
    TextInputLayout editVerification;
    @BindView(R.id.btn_verification)
    Button btnVerification;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private String phone;
    private String countryCode;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private long TIME_INTERVAL = 60;
    private String function;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_phone_verification);
        ButterKnife.bind(this);
        initToolbar();
        initData();
        initView();
        PopupWindow  p = makeP();
    }

    private static PopupWindow makeP() {
        return null;

    }


    private void initData() {
        phone = DBUtils.INSTANCE.getLastUser().getPhone();
        editPhoneNumber.getEditText().setText(phone);

        Intent intent = getIntent();
        function = intent.getExtras().getString(Constants.ME_FUNCTION);
    }

    private void initToolbar() {
        toolbar.setTitle(R.string.manager_verification);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initView() {
        countryCode = ccp.getSelectedCountryCode();
        ccp.setOnCountryChangeListener(() -> countryCode = ccp.getSelectedCountryCode());
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


    @OnClick({R.id.btn_send_verification, R.id.btn_verification})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
            case R.id.btn_send_verification:
                sendCode(countryCode, editPhoneNumber.getEditText().getText().toString().trim());
                break;
            case R.id.btn_verification:
                if(editPhoneNumber.getEditText().getText().toString().trim().equals(phone)){
                    submitCode(countryCode, editPhoneNumber.getEditText().getText().toString().trim(),
                            editVerification.getEditText().getText().toString().trim());
                }else {
                    ToastUtils.showLong(R.string.manager_phone_error);
                }
                break;
        }
    }

    // 请求验证码，其中country表示国家代码，如“86”；phone表示手机号码，如“13800138000”
    public void sendCode(String country, String phone) {
        timing();
        // 注册一个事件回调，用于处理发送验证码操作的结果
        SMSSDK.registerEventHandler(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // TODO 处理成功得到验证码的结果
                    // 请注意，此时只是完成了发送验证码的请求，验证码短信还需要几秒钟之后才送达
                    ToastUtils.showLong(R.string.send_message_success);
                } else {
                    // TODO 处理错误的结果
                    ToastUtils.showLong(R.string.send_message_fail);
                }

            }
        });
        // 触发操作
        SMSSDK.getVerificationCode(country, phone);
    }

    // 提交验证码，其中的code表示验证码，如“1357”
    public void submitCode(String country, String phone, String code) {
        // 注册一个事件回调，用于处理提交验证码操作的结果
        SMSSDK.registerEventHandler(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // TODO 处理验证成功的结果
                    tranformView();
                } else {
                    // TODO 处理错误的结果
                    ToastUtils.showLong(R.string.verification_code_error);
                }

            }
        });
        // 触发操作
        SMSSDK.submitVerificationCode(country, phone, code);
    }

    private void timing() {
        mCompositeDisposable.add(Observable.intervalRange(0, TIME_INTERVAL, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Object>() {
                    @Override
                    public void onNext(Object o) {
                        long num = (59 - Long.valueOf((Long) o));
                        if (num == 0) {
                            btnSendVerification.setText(getResources().getString(R.string.send_verification));
                            btnSendVerification.setBackgroundColor(getResources().getColor(R.color.primary));
                            btnSendVerification.setClickable(true);
                        } else {
                            btnSendVerification.setText(getString(R.string.regetCount, num));
                            btnSendVerification.setBackgroundColor(getResources().getColor(R.color.gray));
                            btnSendVerification.setClickable(false);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }));
    }

    private void tranformView() {
        ToastUtils.showLong(R.string.successful_verification);
        Intent intent = new Intent();
        intent.putExtra(Constants.ME_FUNCTION, function);
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        //用完回调要注销掉，否则可能会出现内存泄露
        SMSSDK.unregisterAllEventHandler();
    }

    ;
}
