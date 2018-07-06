package com.dadoutek.uled.light;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.othersview.MainActivity;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EmptyAddActivity extends TelinkBaseActivity {


    @BindView(R.id.tip)
    TextView tip;
    @BindView(R.id.img_add)
    ImageView imgAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_add);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.tip, R.id.img_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tip:
//                developerMode();
                break;
            case R.id.img_add:
                Intent intent=new Intent(EmptyAddActivity.this,DeviceScanningNewActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    long[] mHints = new long[6];//初始全部为0
    private void developerMode() {
        //将mHints数组内的所有元素左移一个位置
        System.arraycopy(mHints, 1, mHints, 0, mHints.length - 1);
        //获得当前系统已经启动的时间
        mHints[mHints.length - 1] = SystemClock.uptimeMillis();
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000){
            ToastUtils.showLong(R.string.developer_mode);
            SharedPreferencesUtils.setDeveloperModel(true);
            Intent intent=new Intent(EmptyAddActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }
}
