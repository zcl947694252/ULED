package com.dadoutek.uled.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dadoutek.uled.R;

/**
 * Created by hejiajun on 2018/4/16.
 */

public class MeFragment extends Fragment{

    private LayoutInflater inflater;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater=inflater;
        View view = inflater.inflate(R.layout.fragment_me,
                null);
        return view;
    }
}
