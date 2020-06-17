package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.GridView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.curtains.WindowCurtainsActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.event.NotificationEvent
import com.telink.util.Event
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.activity_device_grouping.*
import java.lang.Thread.sleep
import java.util.*


class CurtainGroupingActivity : TelinkBaseActivity(), EventListener<String> {

    private var inflater: LayoutInflater? = null
    private var adapter: DeviceGroupingAdapter? = null
    private var groupsInit: MutableList<DbGroup>? = null

    private var curtain: DbCurtain? = null
    private var gpAdress: Int = 0

    private var isStartGroup = false
    private val itemClickListener = OnItemClickListener { parent, view, position, id ->
        val group = adapter!!.getItem(position)
        if (group != null) {
            if (TelinkApplication.getInstance().connectDevice == null) {
                ToastUtils.showLong(R.string.group_fail)
            } else {
                if(!isStartGroup){
                    isStartGroup=true
                    showLoadingDialog(getString(R.string.grouping))
                     Thread{
                         val sceneIds = getRelatedSceneIds(group.meshAddr)
                         for(i in 0..1){
                             deletePreGroup(curtain!!.meshAddr)
                             sleep(100)
                         }

                         for(i in 0..1){
                             deleteAllSceneByLightAddr(curtain!!.meshAddr)
                             sleep(100)
                         }

                         for(i in 0..1){
                             allocDeviceGroup(group)
                             sleep(100)
                         }

                         for (sceneId in sceneIds) {
                             val action = DBUtils.getActionBySceneId(sceneId, group.meshAddr)
                             if (action != null) {
                                 for(i in 0..1){
                                     Commander.addScene(sceneId, curtain!!.meshAddr, action.color)
                                     sleep(100)
                                 }
                             }
                         }
                         group.deviceType=curtain!!.productUUID.toLong()

                         DBUtils.updateGroup(group)
                         DBUtils.updateCurtain(curtain!!)
                         runOnUiThread {
                             hideLoadingDialog()
                             ActivityUtils.finishActivity(WindowCurtainsActivity::class.java)
                             finish()
                         }
                     }.start()
                }
            }
        }
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                UPDATE -> adapter!!.notifyDataSetChanged()
            }
        }
    }

    private var mApplication: TelinkLightApplication? = null


    private fun getRelatedSceneIds(groupAddress: Int): List<Long> {
        val sceneIds = ArrayList<Long>()
        val dbSceneList = DBUtils.sceneList
        sceneLoop@ for (dbScene in dbSceneList) {
            val dbActions = DBUtils.getActionsBySceneId(dbScene.id)
            for (action in dbActions) {
                if (groupAddress == action.groupAddr || 0xffff == action.groupAddr) {
                    sceneIds.add(dbScene.id)
                    continue@sceneLoop
                }
            }
        }
        return sceneIds
    }


    /**
     * 删除指定灯里的所有场景
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    /**
     * 删除指定灯的之前的分组
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deletePreGroup(lightMeshAddr: Int) {
        if (DBUtils.getGroupByID(curtain!!.belongGroupId!!) != null) {
            val groupAddress = DBUtils.getGroupByID(curtain!!.belongGroupId!!)?.meshAddr
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x00, (groupAddress!! and 0xFF).toByte(), //0x00表示删除组
                    (groupAddress shr 8 and 0xFF).toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        this.mApplication = this.application as TelinkLightApplication
        this.mApplication!!.addEventListener(NotificationEvent.GET_GROUP, this)
        //this.mApplication.addEventListener(NotificationEvent.GET_SCENE, this);

        this.setContentView(R.layout.activity_device_grouping)

        initData()
        getDeviceGroup()
        initView()

        //        getScene();
    }

    private fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.activity_device_grouping)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        this.inflater = this.layoutInflater
        list_groups!!.onItemClickListener = this.itemClickListener
        adapter = DeviceGroupingAdapter(groupsInit!!, this)
        list_groups!!.adapter = adapter
    }

    private fun initData() {
        this.curtain = this.intent.extras?.get("curtain") as DbCurtain
        this.gpAdress = this.intent.getIntExtra("gpAddress", 0)

        groupsInit = ArrayList()

        val list = DBUtils.groupList
        filter(list)
//        groupsInit = DBUtils.groupList
    }

    private fun filter(list: MutableList<DbGroup>) {
        groupsInit?.clear()
        for (i in list.indices) {

            if (curtain?.productUUID == DeviceType.SMART_CURTAIN){
                if (OtherUtils.isCurtain(list[i])) {
                    groupsInit?.add(list[i])
                    list[i].checked = list[i].id == this.curtain?.belongGroupId
                }
            }

            if(OtherUtils.isDefaultGroup(list[i])){
                groupsInit?.add(list[i])
            }

        }
    }

    private fun getScene() {
        val opcode = 0xc0.toByte()
        val dstAddress = curtain!!.meshAddr
        val params = byteArrayOf(0x10, 0x00)

        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddress, params)
        TelinkLightService.Instance()?.updateNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.mApplication!!.removeEventListener(this)
    }

    private fun getDeviceGroup() {
        val opcode = 0xDD.toByte()
        val dstAddress = curtain!!.meshAddr
        val params = byteArrayOf(0x08, 0x01)

        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddress, params)
        TelinkLightService.Instance()?.updateNotification()
    }

    private fun allocDeviceGroup(group: DbGroup) {

        val groupAddress = group.meshAddr
        val dstAddress = curtain!!.meshAddr
        val opcode = 0xD7.toByte()
        val params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
        params[0] = 0x01
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddress, params)
        curtain!!.belongGroupId = group.id
    }

    override fun performed(event: Event<String>) {
        if (event.type === NotificationEvent.GET_GROUP) {
            val e = event as NotificationEvent
            val info = e.args

            val srcAddress = info.src and 0xFF
            val params = info.params

            if (srcAddress != curtain!!.meshAddr)
                return

            val count = this.adapter!!.count

            var group: DbGroup?

            for (i in 0 until count) {
                group = this.adapter!!.getItem(i)

                if (group != null)
                    group.checked = false
            }

            var groupAddress: Int
            val len = params.size

            for (j in 0 until len) {

                groupAddress = params[j].toInt()

                if (groupAddress == 0x00 || groupAddress == 0xFF)
                    break

                groupAddress = groupAddress or 0x8000

                group = this.adapter!![groupAddress]

                if (group != null) {
                    group.checked = true
                }
            }

            mHandler.obtainMessage(UPDATE).sendToTarget()
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
         //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(this@CurtainGroupingActivity)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        refreshView()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun refreshView() {
        val list = DBUtils.groupList
        filter(list)

        adapter = DeviceGroupingAdapter(groupsInit!!, this)
        list_groups!!.adapter = this.adapter
        adapter!!.notifyDataSetChanged()
    }


    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this@CurtainGroupingActivity)
        builder.setTitle(R.string.group_not_change_tip)
        builder.setPositiveButton(android.R.string.ok) { dialog, which -> finish() }
        builder.setNegativeButton(R.string.btn_cancel) { dialog, which -> }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()

            R.id.menu_install -> popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0) /*addNewGroup()*/
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_fragment, menu)
        return true
    }

    companion object {

        private val UPDATE = 1
    }
}
