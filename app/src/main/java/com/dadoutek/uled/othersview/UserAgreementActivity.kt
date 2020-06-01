package com.dadoutek.uled.othersview

import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import kotlinx.android.synthetic.main.toolbar.*

class UserAgreementActivity :BaseActivity(){
    override fun initListener() {

    }

    override fun initData() {

    }

    override fun initView() {
        toolbar.title = getString(R.string.user_agreement)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun setLayoutID(): Int {
       return R.layout.activity_user_agreement
    }

}
