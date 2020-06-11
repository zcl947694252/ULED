package com.dadoutek.uled.group

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.util.OtaPrepareUtils
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 11:25
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTAListActivity : TelinkBaseActivity() {
    private var dbGroup: DbGroup? = null
    private var lightList: MutableList<DbLight> = mutableListOf()
    private var curtainList: MutableList<DbCurtain> = mutableListOf()
    private var relayList: MutableList<DbConnector> = mutableListOf()
    private var lightAdaper = GroupOTALightAdapter(R.layout.device_detail_item, lightList)
    private var curtainAdaper = GroupOTACurtainAdapter(R.layout.device_detail_item, curtainList)
    private var relayAdaper = GroupOTARelayAdapter(R.layout.device_detail_item, relayList)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_ota_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
    }

    private fun initData() {
        dbGroup = (intent.getSerializableExtra("group") as DbGroup) ?: DbGroup()
        when (dbGroup!!.deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                template_recycleView.adapter = lightAdaper
                lightAdaper.bindToRecyclerView(template_recycleView)

                lightList.clear()
                lightList.addAll(DBUtils.getLightByGroupID(dbGroup!!.id))
                lightAdaper.onItemClickListener = onItemClickListener
                lightList.forEach {
                    if (it.version == null)
                        it.isSupportOta = false
                    else
                        it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(it.version)
                }

                var unsupport = lightList.filter {
                    !it.isSupportOta
                }
                lightList.removeAll(unsupport)
                lightList.addAll(unsupport)
                lightAdaper.notifyDataSetChanged()
            }
            DeviceType.SMART_CURTAIN -> {
                template_recycleView.adapter = curtainAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)

                curtainList.clear()
                curtainList.addAll(DBUtils.getCurtainByGroupID(dbGroup!!.id))
                curtainAdaper.onItemClickListener = onItemClickListener
                curtainList.forEach {
                    if (it.version == null)
                        it.isSupportOta = false
                    else
                        it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(it.version)
                }

                var unsupport = curtainList.filter {
                    !it.isSupportOta
                }
                curtainList.removeAll(unsupport)
                curtainList.addAll(unsupport)
                curtainAdaper.notifyDataSetChanged()
            }
            DeviceType.SMART_RELAY -> {
                template_recycleView.adapter = relayAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)

                relayList.clear()
                relayList.addAll(DBUtils.getConnectorByGroupID(dbGroup!!.id))
                relayAdaper.onItemClickListener = onItemClickListener
                relayList.forEach {
                    if (it.version == null)
                        it.isSupportOta = false
                    else
                        it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(it.version)
                }

                var unsupport = relayList.filter {
                    !it.isSupportOta
                }
                relayList.removeAll(unsupport)
                relayList.addAll(unsupport)
                relayAdaper.notifyDataSetChanged()
            }
        }

    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
        toolbarTv.text = getString(R.string.group_ota)
        template_recycleView.layoutManager = GridLayoutManager(this, 2)/*LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)*/
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        when (dbGroup!!.deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val dbLight = lightList[position]
                if (dbLight.isSupportOta)
                    startOtaAct(dbLight.meshAddr, dbLight.macAddr, dbLight.version, DeviceType.LIGHT_NORMAL)
                else
                    ToastUtils.showShort(getString(R.string.dissupport_ota))
            }
            DeviceType.SMART_CURTAIN -> {
            }
            DeviceType.SMART_RELAY -> {
            }
        }
    }

    private fun startOtaAct(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        val intent = Intent(this@GroupOTAListActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MES_Add, meshAddr)
        intent.putExtra(Constant.OTA_MAC, macAddr)
        intent.putExtra(Constant.OTA_VERSION, version)
        intent.putExtra(Constant.OTA_TYPE, deviceType)
        startActivity(intent)
    }
}