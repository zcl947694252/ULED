package com.dadoutek.uled.group;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class AddGroupActivity extends TelinkBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);


    }
}
