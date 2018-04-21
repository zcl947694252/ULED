package com.dadoutek.uled.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.util.DataManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by hejiajun on 2018/4/20.
 */

public class RenameActivity extends TelinkBaseActivity {

    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.edit_rename)
    EditText editRename;
    @BindView(R.id.btn_sure)
    Button btnSure;
    private int groupAddress;

    private TelinkLightApplication mApplication;
    private String newName;
    private DataManager dataManager;
    private Groups groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);
        ButterKnife.bind(this);
        mApplication = (TelinkLightApplication) this.getApplication();
        this.groupAddress = this.getIntent().getIntExtra("groupAddress", 0);
        dataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);
        groups = dataManager.getGroups();
    }

    @OnClick({R.id.img_header_menu_left, R.id.edit_rename, R.id.btn_sure})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_header_menu_left:
                finish();
                break;
            case R.id.edit_rename:
                break;
            case R.id.btn_sure:
                checkAndSave();
                break;
        }
    }

    private void checkAndSave() {
        newName = editRename.getText().toString().trim();
        if (checkName()) {
            saveName();
        }
    }

    private void saveName() {
        for (
                int k = 0; k < groups.size(); k++)

        {
            if (groupAddress == groups.get(k).meshAddress) {
                groups.get(k).name = newName;
                dataManager.updateGroup(groups);
                Toast.makeText(RenameActivity.this, R.string.successfully_modified, Toast.LENGTH_LONG).show();
                finish();
                break;
            }
        }
    }

    private boolean checkName() {
        if(compileExChar(newName)){
            return false;
        }

        if(dataManager.checkRepeat(groups,this,newName)){
            return false;
        }
        return true;
    }

    private boolean compileExChar(String str) {

        String limitEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";

        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);

        if (m.find()) {
            Toast.makeText(RenameActivity.this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
