package com.dadoutek.uled.WindowCurtains

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import kotlinx.android.synthetic.main.activity_window_curtains.*

class WindowCurtainsActivity : TelinkBaseActivity() ,View.OnClickListener{

    private var showList: List<DbCurtain>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initView()
    }

    private fun initView() {
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        down.setOnClickListener(this)
        up.setOnClickListener(this)
        accelerate.setOnClickListener(this)
        slow_down.setOnClickListener(this)
        pause.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
       when(v?.id){
           R.id.open->openWindow()
           R.id.off->offWindow()
           R.id.down->downWindow()
           R.id.up->upWindow()
           R.id.accelerate->accelerateWindow()
           R.id.slow_down->slow_downWindow()
       }
    }

    private fun slow_downWindow() {
        val params = byteArrayOf(0x08, 0x01)
        val opcode = 0xDD.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }

    private fun accelerateWindow() {
        val params = byteArrayOf(0x08, 0x01)
        val opcode = 0xDD.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }

    private fun upWindow() {
        val params = byteArrayOf(0x08, 0x01)
        val opcode = 0xDD.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }

    private fun downWindow() {
        val params = byteArrayOf(0x08, 0x01)
        val opcode = 0xDD.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }

    private fun offWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0A, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = 0xF2.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }

    private fun openWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0C, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = 0xF2.toByte()
        TelinkLightService.Instance().sendCommandNoResponse(opcode,0xFFFFF,params)
    }
}

