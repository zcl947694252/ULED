package com.dadoutek.uled.light;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by hejiajun on 2018/4/20.
 */

public class RenameLightActivity extends TelinkBaseActivity {

    @BindView(R.id.img_header_menu_left)
    ImageView imgHeaderMenuLeft;
    @BindView(R.id.edit_rename)
    EditText editRename;
    @BindView(R.id.btn_sure)
    Button btnSure;
    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    private DbLight light;

    private TelinkLightApplication mApplication;
    private String newName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);
        ButterKnife.bind(this);
        txtHeaderTitle.setText(R.string.rename_light);
        mApplication = (TelinkLightApplication) this.getApplication();
        this.light = (DbLight) this.getIntent().getExtras().get("light");
//        Lights lights1=dataManager.getLights();
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
        light.setName(newName);
        DBUtils.updateLight(light);
        finish();
    }

    private boolean checkName() {
        if (compileExChar(newName)) {
            Toast.makeText(RenameLightActivity.this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show();
            return false;
        }

        List<DbLight> lights=DBUtils.getAllLight();
        for(int k=0;k<lights.size();k++){
            if(lights.get(k).getName().equals(newName)){
                ToastUtils.showLong(R.string.tip_used_name);
                return false;
            }
        }
        return true;
    }
}
