package com.dadoutek.uled.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.util.DataManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private int lightAddress;

    private TelinkLightApplication mApplication;
    private String newName;
    private DataManager dataManager;
    private Lights lights = Lights.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);
        ButterKnife.bind(this);
        txtHeaderTitle.setText(R.string.rename_light);
        mApplication = (TelinkLightApplication) this.getApplication();
        this.lightAddress = this.getIntent().getIntExtra("lightAddress", 0);
        dataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);
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
        for (
                int k = 0; k < lights.size(); k++)

        {
            if (lightAddress == lights.get(k).meshAddress) {
                lights.get(k).name = newName;
                dataManager.updateLights(lights);
                Toast.makeText(RenameLightActivity.this, R.string.successfully_modified, Toast.LENGTH_LONG).show();
                finish();
                break;
            }
        }
    }

    private boolean checkName() {
        if (compileExChar(newName)) {
            Toast.makeText(RenameLightActivity.this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (dataManager.checkRepeat(lights, this, newName)) {
            return false;
        }
        return true;
    }
}
