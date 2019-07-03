package com.dadoutek.uled.user

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.dadoutek.uled.R

class AgainEnterPasswordActivity : AppCompatActivity() {

    private var phone: String? = null

    private var password: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_again_enter_password)
    }
}
