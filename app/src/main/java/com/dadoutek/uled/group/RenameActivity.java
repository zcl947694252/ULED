package com.dadoutek.uled.group;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.StringUtils;

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
        StringUtils.initEditTextFilter(editRename);
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
        DBUtils.INSTANCE.updateGroup(group);
        finish();

    }

    private boolean checkName() {
        if (compileExChar(newName)) {
            Toast.makeText(RenameActivity.this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void doWhichOperation(int actionId) {
       switch (actionId) {
           case EditorInfo.IME_ACTION_DONE:
                 case EditorInfo.IME_ACTION_GO:
                 case EditorInfo.IME_ACTION_NEXT:
                   Log.d("MainActivity", "IME_ACTION_NEXT");
                   break;
                 case EditorInfo.IME_ACTION_NONE:
                   Log.d("MainActivity", "IME_ACTION_NONE");
                   break;
                 case EditorInfo.IME_ACTION_PREVIOUS:
                   Log.d("MainActivity", "IME_ACTION_PREVIOUS");
                   break;
                 case EditorInfo.IME_ACTION_SEARCH:
                   Log.d("MainActivity", "IME_ACTION_SEARCH");
                   break;
                 case EditorInfo.IME_ACTION_SEND:
                   Log.d("MainActivity", "IME_ACTION_SEND");
                   break;
                 case EditorInfo.IME_ACTION_UNSPECIFIED:
                   Log.d("MainActivity", "IME_ACTION_UNSPECIFIED");
                   break;
                 default:
                   break;
               }
         }


}
