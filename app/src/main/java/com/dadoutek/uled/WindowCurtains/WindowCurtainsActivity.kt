package com.dadoutek.uled.WindowCurtains

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.ehorizontalselectedview.EHorizontalSelectedView
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import kotlinx.android.synthetic.main.activity_window_curtains.*

class WindowCurtainsActivity : TelinkBaseActivity() ,View.OnClickListener{

    private var showList: List<DbCurtain>? = null

    private var curtain:DbCurtain?=null

    private var ctAdress:Int?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initView()
    }

    private fun initView() {
//        this.curtain = this.intent.extras!!.get(Constant.CURTAINS_ARESS_KEY) as DbCurtain
        this.ctAdress = this.intent.getIntExtra(Constant.CURTAINS_ARESS_KEY, 0)
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        pause.setOnClickListener(this)
        commutation.setOnClickListener(this)
        setSpeed()
    }

    private fun setSpeed() {
        val horizontalSelectedView = findViewById<EHorizontalSelectedView>(R.id.hsv)
        val objects = ArrayList<String>()
        objects.add("0")
        objects.add("1")
        objects.add("2")
        objects.add("3")
        objects.add("4")
        horizontalSelectedView.setData(objects)
        horizontalSelectedView.setSeeSize(5)
        horizontalSelectedView.setOtherTextSize(25F)
        horizontalSelectedView.setSelectTextSize(25F)

        horizontalSelectedView.setOnRollingListener { _, s ->
            if (s == "1") {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
            } else if (s == "2") {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
            }else if(s=="3"){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
            }else if(s=="4"){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 4, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
            }
        }
    }

    override fun onClick(v: View?) {
       when(v?.id){
           R.id.open->openWindow()
           R.id.off->offWindow()
           R.id.pause->pauseWindow()
           R.id.commutation->electricCommutation()
       }
    }

    private fun electricCommutation() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x01, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun threeLevel() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun secondLevel() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun onceLevel() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun pauseWindow() {
       val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0B, 0x00, Opcode.CURTAIN_PACK_END)
       val opcode = Opcode.CURTAIN_ON_OFF
       TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun offWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0C, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun openWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0A, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }
}

