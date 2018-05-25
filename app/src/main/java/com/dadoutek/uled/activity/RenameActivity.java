package com.dadoutek.uled.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;

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

    private TelinkLightApplication mApplication;
    private String newName;
    private DbGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);
        ButterKnife.bind(this);
        mApplication = (TelinkLightApplication) this.getApplication();
        this.group = (DbGroup) this.getIntent().getExtras().get("group");
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
        group.setName(newName);
        DBUtils.updateGroup(group);
        finish();
    }

    private boolean checkName() {
        if (compileExChar(newName)) {
            Toast.makeText(RenameActivity.this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show();
            return false;
        }

//        if (dataManager.checkRepeat(groups, this, newName)) {
//            return false;
//        }
        return true;
    }

}
