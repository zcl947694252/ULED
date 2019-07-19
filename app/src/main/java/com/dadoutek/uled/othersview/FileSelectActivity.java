package com.dadoutek.uled.othersview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Administrator on 2016/10/8.
 */
public class FileSelectActivity extends TelinkBaseActivity {
    private ListView lv_file;
    private TextView tv_parent;
    private TextView tv_cur_name; // 当前目录名称
    private FileListAdapter mAdapter;
    private List<File> mFiles = new ArrayList<>();
    private File mCurrentDir;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);
        initView();
        update();
    }

    private void initView() {
        lv_file =  findViewById(R.id.lv_file);
        tv_cur_name =  findViewById(R.id.tv_cur_name);
        tv_parent =  findViewById(R.id.tv_parent);
        tv_parent.setOnClickListener(v -> {
            mCurrentDir = mCurrentDir.getParentFile();
            update();
        });

        mCurrentDir = Environment.getExternalStorageDirectory();
//        mCurrentDir = getFilesDir();
        mAdapter = new FileListAdapter(this);
        lv_file.setAdapter(mAdapter);
        lv_file.setOnItemClickListener((parent, view, position, id) -> {
            if (mFiles.get(position).isDirectory()) {
                mCurrentDir = mFiles.get(position);
                update();
            } else {
                Intent intent = new Intent();
                intent.putExtra("path", mFiles.get(position).getAbsolutePath());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }


    private void update() {
        if (mCurrentDir.getParentFile() != null) {
            tv_parent.setVisibility(View.VISIBLE);
        } else {
            tv_parent.setVisibility(View.INVISIBLE);
        }
        tv_cur_name.setText(mCurrentDir.toString());
        File[] files = mCurrentDir.listFiles();
        if (files == null) {
            mFiles.clear();
        } else {
            mFiles = new ArrayList<>(Arrays.asList(files));

            // 排序
            Collections.sort(mFiles, (o1, o2) -> {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            });
        }
        mAdapter.setData(mFiles);
    }
}
