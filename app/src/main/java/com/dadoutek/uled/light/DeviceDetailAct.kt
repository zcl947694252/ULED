package com.dadoutek.uled.light

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.usb.UsbDevice.getDeviceName
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import butterknife.ButterKnife
import com.dadoutek.uled.R
import com.dadoutek.uled.R.string.grouping
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.scene.SceneRecycleListAdapter
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.telink.util.Event
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.activity_config_light_light.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.inputMethodManager
import java.util.ArrayList

/**
 * Created by hejiajun on 2018/4/24.
 */

class DeviceDetailAct : TelinkBaseActivity(), EventListener<String> {

    private var type: Int? = null

    private var lightsData: ArrayList<DbLight>? = null

    private var inflater: LayoutInflater? = null

    private var recyclerView: RecyclerView? = null

    private var adaper: DeviceDetailListAdapter? = null

    override fun performed(event: Event<String>?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initDate()
        initView()
    }

    private fun initView() {
        val layoutmanager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.recycleView)
        recyclerView!!.layoutManager = GridLayoutManager(this,3)
        val decoration = DividerItemDecoration(this!!,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this!!, R.color
                .divider)))
        recyclerView!!.addItemDecoration(decoration)
        //添加Item变化动画
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adaper = DeviceDetailListAdapter(R.layout.device_detail_adapter, lightsData)
        adaper!!.bindToRecyclerView(recyclerView)

        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.details)
    }

    private fun initDate() {
        when (type) {
            Constant.INSTALL_NORMAL_LIGHT -> {
                lightsData = DBUtils.getAllNormalLight()
            }
            Constant.INSTALL_RGB_LIGHT -> {
                lightsData=DBUtils.getAllRGBLight()
            }
        }
    }
}

