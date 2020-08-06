package com.dadoutek.uled.othersview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.blankj.utilcode.util.LogUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tools.CharacterParserUtil;
import com.dadoutek.uled.tools.CountryComparator;
import com.dadoutek.uled.tools.CountrySortAdapter;
import com.dadoutek.uled.tools.CountrySortModel;
import com.dadoutek.uled.tools.GetCountryNameSort;
import com.dadoutek.uled.tools.SideBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 类简要描述
 * 类详细描述
 */
public class CountryActivity extends Activity {
    private String TAG = "CountryActivity";
    private List<CountrySortModel> mAllCountryList;
    private EditText country_edt_search;
    private ListView country_lv_countryList;
    private ImageView country_iv_clearText;
    private CountrySortAdapter adapter;
    private SideBar sideBar;
    private TextView dialog;
    private CountryComparator pinyinComparator;
    private GetCountryNameSort countryChangeUtil;
    private CharacterParserUtil characterParserUtil;
    private Toolbar toolbar;
    private TextView toolbarTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.coogame_country);

        initView();
        setListener();
        getCountryList();
    }

    /**
     * 初始化界面
     */
    private void initView() {
        country_edt_search = findViewById(R.id.country_et_search);
        country_lv_countryList = findViewById(R.id.country_lv_list);
        country_iv_clearText = findViewById(R.id.country_iv_cleartext);

        sideBar = findViewById(R.id.country_sidebar);

        toolbarTv = findViewById(R.id.toolbarTv);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.icon_return);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbarTv.setText(R.string.select_country_area);

        mAllCountryList = new ArrayList<>();
        pinyinComparator = new CountryComparator();
        countryChangeUtil = new GetCountryNameSort();
        characterParserUtil = new CharacterParserUtil();

        // 将联系人进行排序，按照A~Z的顺序
        Collections.sort(mAllCountryList, pinyinComparator);
        adapter = new CountrySortAdapter(this, mAllCountryList);
        country_lv_countryList.setAdapter(adapter);
    }

    /****
     * 添加监听
     */
    private void setListener() {
        country_iv_clearText.setOnClickListener(v -> {
            country_edt_search.setText("");
            Collections.sort(mAllCountryList, pinyinComparator);
            adapter.updateListView(mAllCountryList);
        });

        country_edt_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                String searchContent = country_edt_search.getText().toString();
                if (searchContent.equals("")) {
                    country_iv_clearText.setVisibility(View.INVISIBLE);
                } else {
                    country_iv_clearText.setVisibility(View.VISIBLE);
                }

                if (searchContent.length() > 0) {
                    // 按照输入内容进行匹配
                    ArrayList<CountrySortModel> fileterList =
                            (ArrayList<CountrySortModel>) countryChangeUtil
                            .search(searchContent, mAllCountryList);

                    adapter.updateListView(fileterList);
                } else {
                    adapter.updateListView(mAllCountryList);
                }
                country_lv_countryList.setSelection(0);
            }
        });

        // 右侧sideBar监听
        sideBar.setOnTouchingLetterChangedListener(s -> {
            // 该字母首次出现的位置
            int position = adapter.getPositionForSection(s.charAt(0));
            if (position != -1) {
                country_lv_countryList.setSelection(position);
            }
        });

        country_lv_countryList.setOnItemClickListener((adapterView, view, position, arg3) -> {
            String countryName;
            String countryNumber;
            String searchContent = country_edt_search.getText().toString();

            if (searchContent.length() > 0) { // 按照输入内容进行匹配
                ArrayList<CountrySortModel> fileterList =
                        (ArrayList<CountrySortModel>) countryChangeUtil
                        .search(searchContent, mAllCountryList);
                countryName = fileterList.get(position).countryName;
                countryNumber = fileterList.get(position).countryNumber;
            } else {// 点击后返回
                countryName = mAllCountryList.get(position).countryName;
                countryNumber = mAllCountryList.get(position).countryNumber;
            }

            Intent intent = new Intent(CountryActivity.this, MainActivity.class);
            intent.putExtra("countryName", countryName);
            intent.putExtra("countryNumber", countryNumber);
            setResult(RESULT_OK, intent);
            LogUtils.e(TAG, "countryName: + " + countryName + "countryNumber: " + countryNumber);
            finish();
        });
    }

    /**
     * 获取国家列表
     */
    private void getCountryList() {
        String[] countryList = getResources().getStringArray(R.array.country_code_list);

        for (int i = 0, length = countryList.length; i < length; i++) {
            String[] country = countryList[i].split("\\*");

            String countryName = country[0];
            String countryNumber = country[1];
            String countrySortKey = characterParserUtil.getSelling(countryName);
            CountrySortModel countrySortModel = new CountrySortModel(countryName, countryNumber,
                    countrySortKey);
            String sortLetter = countryChangeUtil.getSortLetterBySortKey(countrySortKey);
            if (sortLetter == null) {
                sortLetter = countryChangeUtil.getSortLetterBySortKey(countryName);
            }

            countrySortModel.sortLetters = sortLetter;
            mAllCountryList.add(countrySortModel);
        }

        Collections.sort(mAllCountryList, pinyinComparator);
        adapter.updateListView(mAllCountryList);
        Log.e(TAG, "changdu" + mAllCountryList.size());
    }
}
