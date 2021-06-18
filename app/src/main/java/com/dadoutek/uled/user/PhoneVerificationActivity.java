package com.dadoutek.uled.user;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.othersview.RegisterActivity;
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

public class PhoneVerificationActivity extends TelinkBaseActivity {
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
    @BindView(R.id.toolbarTv)
    TextView toolbarTv;

    private String countryCode;
    private String transForm;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private long TIME_INTERVAL = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_phone_verification);
        ButterKnife.bind(this);
        initData();
        initToolbar();
        initView();
    }

    private void initToolbar() {
        toolbarTv.setText(R.string.verification_phone);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setNavigationIcon(R.drawable.icon_return);
    }


    private void initData() {
        Intent intent = getIntent();
        transForm = intent.getStringExtra("fromLogin");
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
                if (Constant.TEST_REGISTER) {
                    testRegister();
                } else {
                    String phoneNum = editPhoneNumber.getEditText().getText().toString().trim();
                    if (StringUtils.isEmpty(phoneNum)) {
                        ToastUtils.showLong(R.string.phone_cannot_be_empty);
                    } else {
                        sendCode(countryCode, phoneNum);
                    }
                }
                break;
            case R.id.btn_verification:
                submitCode(countryCode, editPhoneNumber.getEditText().getText().toString().trim(),
                        editVerification.getEditText().getText().toString().trim());
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

    public void testRegister() {
        tranformView();
    }

    private void timing() {
        mCompositeDisposable.add(Observable.intervalRange(0, TIME_INTERVAL, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Object>() {
                    @Override
                    public void onNext(Object o) {
                        long num = (59 - (Long) o);
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
        Intent intent = null;
        if (transForm.equals("register")) {
            intent = new Intent(PhoneVerificationActivity.this, RegisterActivity.class);
            intent.putExtra("phone", editPhoneNumber.getEditText().getText().toString().trim());
        } else if (transForm.equals("forgetPassword")) {
            intent = new Intent(PhoneVerificationActivity.this, ForgetPassWordActivity.class);
            intent.putExtra("phone", editPhoneNumber.getEditText().getText().toString().trim());
        }
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        //用完回调要注销掉，否则可能会出现内存泄露
        SMSSDK.unregisterAllEventHandler();
    }
}
