package com.dadoutek.uled.group

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.widget.RecyclerGridDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.regex.Pattern


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 11:25
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTAListActivity : TelinkBaseActivity() {
    private var dispose: Disposable? = null
    private var dbGroup: DbGroup? = null
    private var lightList: MutableList<DbLight> = mutableListOf()
    private var curtainList: MutableList<DbCurtain> = mutableListOf()
    private var relayList: MutableList<DbConnector> = mutableListOf()
    private var mapBin = mutableMapOf<String, Int>()
    private var lightAdaper = GroupOTALightAdapter(R.layout.group_ota_item, lightList)
    private var curtainAdaper = GroupOTACurtainAdapter(R.layout.group_ota_item, curtainList)
    private var relayAdaper = GroupOTARelayAdapter(R.layout.group_ota_item, relayList)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_ota_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initData() {
        dbGroup = (intent.getSerializableExtra("group") as DbGroup) ?: DbGroup()

        when (dbGroup!!.deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                template_recycleView.adapter = lightAdaper
                lightAdaper.bindToRecyclerView(template_recycleView)
            }
            DeviceType.SMART_CURTAIN -> {
                template_recycleView.adapter = curtainAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)
            }
            DeviceType.SMART_RELAY -> {
                template_recycleView.adapter = relayAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)
            }
        }
        getBin()

    }

    private fun setRelayData() {
        relayList.clear()
        relayList.addAll(DBUtils.getConnectorByGroupID(dbGroup!!.id))
        relayAdaper.onItemClickListener = onItemClickListener
        relayList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum))
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    else
                        it.isSupportOta = false
                }
            }
        }

        var unsupport = relayList.filter {
            !it.isSupportOta
        }
        relayList.removeAll(unsupport)
        relayList.addAll(unsupport)
        relayAdaper.notifyDataSetChanged()
    }

    private fun setCurtainData() {
        curtainList.clear()
        curtainList.addAll(DBUtils.getCurtainByGroupID(dbGroup!!.id))
        curtainAdaper.onItemClickListener = onItemClickListener
        curtainList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum))
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    else
                        it.isSupportOta = false
                }
            }
        }

        var unsupport = curtainList.filter {
            !it.isSupportOta
        }
        curtainList.removeAll(unsupport)
        curtainList.addAll(unsupport)
        curtainAdaper.notifyDataSetChanged()
    }

    private fun setLightData() {
        lightList.clear()
        lightList.addAll(DBUtils.getLightByGroupID(dbGroup!!.id))
        lightAdaper.onItemClickListener = onItemClickListener
        lightList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum))
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                }
            }
        }

        var unsupport = lightList.filter {
            !it.isSupportOta
        }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
        lightAdaper.notifyDataSetChanged()
    }

    private fun getVersionNum(it: String): StringBuilder {
        var versionNum = StringBuilder()
        val p = Pattern.compile("\\d+")
        val m = p.matcher(it)
        while (m.find()) {
            versionNum.append(m.group())
        }
        return versionNum
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
        toolbarTv.text = getString(R.string.group_ota)
        template_recycleView.layoutManager = GridLayoutManager(this, 2)/*LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)*/
        template_recycleView.addItemDecoration(RecyclerGridDecoration(this, 2))
        getBin()
    }

    private fun getBin() {
        dispose = NetworkFactory.getApi().binList
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    LogUtils.v("zcl获取服务器bin-----------$it-------")
                    mapBin = it
                    updataDevice()

                }, {
                    ToastUtils.showShort(getString(R.string.get_bin_fail))
                    finish()
                })
    }

    private fun updataDevice() {
        when (dbGroup!!.deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> setLightData()
            DeviceType.SMART_CURTAIN -> setCurtainData()
            DeviceType.SMART_RELAY -> setRelayData()
        }
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        when (dbGroup!!.deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val dbLight = lightList[position]
                if (dbLight.isSupportOta)
                    if (TextUtils.isEmpty(dbLight.version)) {
                        showLoadingDialog(getString(R.string.please_wait))
                        connect(meshAddress = dbLight.meshAddr, connectTimeOutTime = 15)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe({
                                    getDeviceVersion(dbLight)
                                }, {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.connect_fail))
                                })
                    } else
                        getFilePath(dbLight.meshAddr, dbLight.macAddr, dbLight.version, DeviceType.LIGHT_NORMAL)
                else
                    ToastUtils.showShort(getString(R.string.dissupport_ota))
            }
            DeviceType.SMART_CURTAIN -> {
            }
            DeviceType.SMART_RELAY -> {
            }
        }
    }

    private fun getDeviceVersion(dbLight: DbLight) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveLight(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getFilePath(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        OtaPrepareUtils.instance().gotoUpdateView(this@GroupOTAListActivity, version, object : OtaPrepareListner {

            override fun downLoadFileStart() {
                showLoadingDialog(getString(R.string.get_update_file))
            }

            override fun startGetVersion() {
                showLoadingDialog(getString(R.string.verification_version))
            }

            override fun getVersionSuccess(s: String) {
                hideLoadingDialog()
            }

            override fun getVersionFail() {
                ToastUtils.showLong(R.string.verification_version_fail)
                hideLoadingDialog()
            }


            override fun downLoadFileSuccess() {
                hideLoadingDialog()
                startOtaAct(meshAddr, macAddr, version, deviceType)
            }

            override fun downLoadFileFail(message: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.download_pack_fail)
            }
        })

    }

    private fun startOtaAct(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        val intent = Intent(this@GroupOTAListActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MES_Add, meshAddr)
        intent.putExtra(Constant.OTA_MAC, macAddr)
        intent.putExtra(Constant.OTA_VERSION, version)
        intent.putExtra(Constant.OTA_TYPE, deviceType)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose?.dispose()
    }
}