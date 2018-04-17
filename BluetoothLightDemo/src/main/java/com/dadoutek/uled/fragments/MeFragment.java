package com.dadoutek.uled.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.util.AppUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by hejiajun on 2018/4/16.
 */

public class MeFragment extends Fragment {

    @BindView(R.id.txt_header_title)
    TextView txtHeaderTitle;
    @BindView(R.id.app_version_name)
    TextView appVersionName;
    @BindView(R.id.app_version)
    TextView appVersion;
    Unbinder unbinder;
    private LayoutInflater inflater;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.fragment_me,
                null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
       String versionName= AppUtils.getVersionName(getActivity());
        appVersion.setText(versionName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
