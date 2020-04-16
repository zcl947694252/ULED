package com.dadoutek.uled.region


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DBUtils.lastUser
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.model.HttpModel.RegionModel.lookAndMakeRegionQR
import com.dadoutek.uled.model.HttpModel.RegionModel.lookAuthorizeCode
import com.dadoutek.uled.model.HttpModel.RegionModel.lookTransferCode
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.network.bean.TransferRegionBean
import com.dadoutek.uled.region.adapter.AreaAuthorizeItemAdapter
import com.dadoutek.uled.region.adapter.AreaItemAdapter
import com.dadoutek.uled.region.bean.ParseCodeBean
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.region.bean.ShareCodeBean
import com.dadoutek.uled.region.bean.TransferBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.textColor
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/12 10:51
 * 描述	      ${不论区域怎么切换永远不变的是user的ID}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${
 */
class NetworkActivity : BaseActivity(), View.OnClickListener {
    private var transferRegionCode: String = ""
    private var transferCode: String = ""
    private var authorizeCode: String = ""
    private var disposableEnsure: Disposable? = null
    private var disposableRequest: Disposable? = null
    private lateinit var mRxPermission: RxPermissions

    private var TAG = "zcl-NetworkActivity"
    private var isRefresh: Boolean = false
    private val REQUEST_CODE: Int = 1000
    private val REQUEST_CODE_CARMER: Int = 100
    private var isAuthorizeRegionAll: Boolean = false
    private var isMeRegionAll: Boolean = false

    var list: MutableList<RegionBean>? = null
    private var listAll: MutableList<RegionBean>? = null
    private var subMeList: MutableList<RegionBean>? = null

    var listAuthorize: MutableList<RegionAuthorizeBean>? = null
    var listAuthorizeAll: MutableList<RegionAuthorizeBean>? = null
    var subAuthorizeList: MutableList<RegionAuthorizeBean>? = null

    private var popAdd: PopupWindow? = null
    private var viewAdd: View? = null
    var adapter: AreaItemAdapter? = null
    var adapterAuthorize: AreaAuthorizeItemAdapter? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var regionBean: RegionBean? = null
    var regionBeanAuthorize: RegionAuthorizeBean? = null
    var mCompositeDisposable = CompositeDisposable()
    var mBuild: AlertDialog.Builder? = null
    var isShowType = 3 //1自己的区域  2接收区域  3 全部区域
    var mExpire: Long = 0
    /**
     * 0没有 1账户移交码  2区域授权码  3 区域移交码
     */
    var qrCodeType = 0


    private var isAddRegion = true
    override fun setLayoutID(): Int {
        return R.layout.activity_network
    }

    override fun initView() {
        list = ArrayList()
        listAuthorize = ArrayList()
        mRxPermission = RxPermissions(this)
        initToolBar()
        makePop()
        initListener()
        getQrInfo()//查看提交吗信息如果存在授权码就直接提示它去取消授权码

        mBuild = AlertDialog.Builder(this@NetworkActivity)
    }

    private fun getQrInfo() {
        val disposable = RegionModel.lookTransferCodeState().subscribe(object : NetworkObserver<TransferBean?>() {
            override fun onNext(it: TransferBean) {
                val isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
                transfer_account_tv.text = if (isNewQr) getString(R.string.transfer_accounts) else getString(R.string.to_receive)
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(e.message)
            }
        })
    }

    private fun initToolBar() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.drawable.icon_scanning)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initListener() {
        img_function1.setOnClickListener {
            isAddRegion = true
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_region_name)
            popAdd?.let { it1 -> showPop(it1, Gravity.CENTER) }
        }

        image_bluetooth.setOnClickListener {
            openScan()
        }
        disposableEnsure = RxView.clicks(image_bluetooth)
                .compose(mRxPermission.ensure(Manifest.permission.CAMERA))
                .subscribe({
                    if (it) {
                        openScan()
                    } else {
                        disposableRequest = mRxPermission.request(Manifest.permission.CAMERA).subscribe({ t ->
                            if (t)
                                openScan()
                            else
                                ToastUtils.showLong(getString(R.string.permission_denfied))
                        }, { t -> ToastUtils.showLong(t.localizedMessage) })
                    }
                }, { t -> ToastUtils.showLong(t.localizedMessage) })

        transfer_account.setOnClickListener {
            SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                override fun start() {
                }

                override fun complete() {
                    lookAndMakeTransferCode()
                }

                override fun error(msg: String?) {
                    ToastUtils.showLong(getString(R.string.make_code_fail))
                }
            })
        }
    }

    private fun addRegion(text: Editable) {
        val dbRegion = DbRegion()
        dbRegion.installMesh = Constant.PIR_SWITCH_MESH_NAME
        dbRegion.installMeshPwd = "123"
        dbRegion.name = text.toString()
        if (isAddRegion) {
            if (listAll == null || listAll!!.size <= 0) {
                dbRegion.id = 1
            } else {
                for (k in listAll!!.indices) {
                    if (k == 0)
                        dbRegion.id = listAll!![k].id + 1
                    else if (listAll!![k].id + 1 > dbRegion.id)
                        dbRegion.id = listAll!![k].id + 1
                }
            }
        } else {
            dbRegion.id = regionBean?.id
        }

        val disposable = RegionModel.addRegions(lastUser!!.token, dbRegion, dbRegion.id)!!.subscribe(object : NetworkObserver<Any?>() {
            override fun onNext(t: Any) {
                 initData()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(e.message)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        hideLoadingDialog()
        showLoadingDialog(getString(R.string.get_data_please_wait))
        if (lastUser != null) {
            region_account_num.text = lastUser!!.phone
//            LogUtils.e(TAG, "zcl******isShowType****$isShowType" + "user${lastUser.toString()}")
            when (isShowType) {
                1 -> RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
                    override fun onNext(it: MutableList<RegionBean>) {
                        setMeData(it)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        ToastUtils.showLong(e.message)
                        hideLoadingDialog()
                    }
                })
                2 -> RegionModel.getAuthorizerList()?.subscribe(object : NetworkObserver<MutableList<RegionAuthorizeBean>?>() {
                    override fun onNext(t: MutableList<RegionAuthorizeBean>) {
                        setAuthorizeData(t)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        ToastUtils.showLong(e.message)
                        hideLoadingDialog()
                    }
                })
                3 -> {
                    RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
                        override fun onNext(it: MutableList<RegionBean>) {
                            setMeData(it)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            ToastUtils.showLong(e.message)
                            hideLoadingDialog()
                        }
                    })
                    RegionModel.getAuthorizerList()?.subscribe(object : NetworkObserver<MutableList<RegionAuthorizeBean>?>() {
                        override fun onNext(t: MutableList<RegionAuthorizeBean>) {
                            setAuthorizeData(t)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            ToastUtils.showLong(e.message)
                            hideLoadingDialog()
                        }
                    })
                }
            }
        }
    }

    /**
     * 赋值我的区域数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setMeData(it: MutableList<RegionBean>) {
        hideLoadingDialog()
        //lastUser.lastGenMeshAddr =it[0].lastGenMeshAddr
        listAll = it
        region_me_net_num.text = getString(R.string.me_net_num, it.size)
        if (it.size > 3) {
            region_me_more.visibility = View.VISIBLE
            subMeList = it.subList(0, 2)
        } else {
            subMeList = it
            region_me_more.visibility = View.GONE
        }

        list = if (!isMeRegionAll) subMeList else listAll

        region_me_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(R.layout.item_area_net, list!!, lastUser)
//        LogUtils.e("zcl_NetworkActivity", "zcl***设置adapter***$lastUser")
        adapter!!.setOnItemChildClickListener { _, view, position ->
            isShowType = 1
            setClickAction(position, view, list!!)
        }
        region_me_recycleview.adapter = adapter

        region_me_more.setOnClickListener {
            isMeRegionAll = !isMeRegionAll
            setMoreArr(isMeRegionAll, region_me_more_tv, region_me_more_arr)
            isShowType = 1
            initData()
        }
    }

    /**
     * 查看更多状态变化
     */
    private fun setMoreArr(isAll: Boolean, more_tv: TextView, more_arr: ImageView) {
        if (isAll) {
            more_arr.setImageResource(R.mipmap.icon_on)
            more_tv.text = getString(R.string.pick_up)
        } else {
            more_tv.text = getString(R.string.see_more)
            more_arr.setImageResource(R.drawable.icon_under)
        }
    }

    /**
     * 赋值接受区域数据
     */
    @SuppressLint("StringFormatMatches")
    private fun setAuthorizeData(it: MutableList<RegionAuthorizeBean>) {
        hideLoadingDialog()
        listAuthorizeAll = it
        region_authorize_net_num.text = getString(R.string.received_net_num, it.size)

        if (it.size > 3) {
            region_authorize_more.visibility = View.VISIBLE
            subAuthorizeList = it.subList(0, 2)
        } else {
            subAuthorizeList = it
            region_authorize_more.visibility = View.GONE
        }
        listAuthorize = if (!isAuthorizeRegionAll) subAuthorizeList else listAuthorizeAll

        Log.e(TAG, "zcl******authorize**${listAuthorize!!.size}")

        region_authorize_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapterAuthorize = AreaAuthorizeItemAdapter(R.layout.item_area_net, listAuthorize!!, lastUser)
        adapterAuthorize!!.setOnItemChildClickListener { _, view, position ->
            isShowType = 2
            setClickActionAuthorize(position, view, listAuthorize!!)
        }
        region_authorize_recycleview.adapter = adapterAuthorize

        region_authorize_more.setOnClickListener {
            isAuthorizeRegionAll = !isAuthorizeRegionAll
            setMoreArr(isAuthorizeRegionAll, region_authorize_more_tv, region_authorize_more_arr)
            isShowType = 2
            initData()
        }
    }

    private fun setClickActionAuthorize(position: Int, view: View?, list: MutableList<RegionAuthorizeBean>) {
        regionBeanAuthorize = list[position]
        when (view?.id) {
            R.id.item_area_state -> {
                val lastUser = lastUser!!
                //不是使用中的
                if (lastUser.last_authorizer_user_id != regionBeanAuthorize!!.authorizer_id.toString() || regionBeanAuthorize!!.id.toString() != lastUser.last_region_id)
                    setChangeDialog(null)

            }
            R.id.item_area_more -> setPopDataAuthorize(position, list)
        }
    }

    private fun setPopDataAuthorize(position: Int, list: MutableList<RegionAuthorizeBean>) {
        if (lastUser!!.last_authorizer_user_id == regionBeanAuthorize!!.authorizer_id.toString() && regionBeanAuthorize!!.id.toString() == lastUser!!.last_region_id)
            return

        isShowType = 2
        regionBeanAuthorize = list[position]

        if (regionBeanAuthorize != null) {

            view?.let {
                it.findViewById<TextView>(R.id.pop_net_name).text = regionBeanAuthorize!!.name
                it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + regionBeanAuthorize!!.count_all
                //授权区域只有解绑和使用功能
                it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = false
                it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)

                //分享
                it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.mipmap.icon_share)
                it.findViewById<ImageView>(R.id.pop_share_net).isClickable = false
                //修改不能用
                it.findViewById<ImageView>(R.id.pop_update_net).setImageResource(R.mipmap.icon_modify)
                it.findViewById<ImageView>(R.id.pop_update_net).isClickable = false

                it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + regionBeanAuthorize?.phone

                if (lastUser!!.last_authorizer_user_id == regionBeanAuthorize!!.authorizer_id.toString()
                        && lastUser!!.last_region_id == regionBeanAuthorize!!.id.toString()) {
                    it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = false
                    it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.drawable.icon_untied)
                    it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
                } else {
                    it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = true
                    it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.drawable.icon_untied_b)
                    it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
                }
            }
            view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.GONE
            view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.VISIBLE
            showPop(pop!!, Gravity.BOTTOM)
        }
    }

    private fun setChangeDialog(t: ParseCodeBean? = null) {
        var message = getString(R.string.change_region)
        if (t != null) {
            message += t.switchToRegionName
            //-1 表示是区域的移交码，0是账号的移交码，1是区域的授权码
            val type = t.type
            if (type == 1) {
                isShowType = 2
                regionBeanAuthorize = RegionAuthorizeBean()
                regionBeanAuthorize?.id = t.switchToRegionId
                regionBeanAuthorize?.authorizer_id = t.switchToAuthorizerUserId
            } else {
                isShowType = 1//代表自己的区域
                regionBean = RegionBean()
                regionBean?.id = t.switchToRegionId.toLong()
                regionBean?.authorizer_id = t.switchToAuthorizerUserId
            }
        }

        mBuild?.let {
            it.setMessage(message)
            it.setNegativeButton(getString(R.string.btn_sure)) { dialog, _ ->
                changeRegion()
                dialog.dismiss()
            }
            it.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            it.setCancelable(true)
            it.show()
        }
    }

    /**
     * 点击Item事件
     */
    private fun setClickAction(position: Int, view: View, list: MutableList<RegionBean>) {
        regionBean = list[position]
        when (view.id) {
            R.id.item_area_state -> {
                //不是使用中的
                if (lastUser!!.last_authorizer_user_id != lastUser!!.id.toString() || regionBean!!.id.toString() != lastUser!!.last_region_id) {
                    setChangeDialog(null)
                }
            }
            R.id.item_area_more -> setPopData(position, list)
        }
    }

    /**
     * 弹框各个view监听
     */
    override fun onClick(v: View?) {
       // DBUtils.deleteAllSensorAndSwitch() 取消对开关和传感器的控制
        when (v!!.id) {
            R.id.pop_view -> PopUtil.dismiss(pop)
            R.id.pop_unbind_net_ly -> {
                //解绑网络
                when (isShowType) {
                    1 -> {
                        val intent = Intent(this@NetworkActivity, UnbindMeNetActivity::class.java)
                        intent.putExtra(Constant.SHARE_PERSON, regionBean)
                        startActivity(intent)
                        PopUtil.dismiss(pop)
                    }
                    2 -> showUnbindDialog()
                }
            }
            R.id.pop_qr_cancel -> {//取消弹框
                if (qrCodeType == 1)
                    RegionModel.removeTransferCode()!!.subscribe(object : NetworkObserver<String?>() {
                        override fun onNext(t: String) {
                            setCancel()
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            ToastUtils.showLong(e.message)
                        }
                    })
                PopUtil.dismiss(pop)
            }
            R.id.pop_user_net -> {
                //上传数据到服务器
                changeRegion()
            }
            R.id.pop_delete_net -> {
                if (isShowType == 1 && regionBean?.count_all ?: 0 > 0)
                    ToastUtils.showLong(getString(R.string.delete_region_tip))
                else
                    RegionModel.removeRegion(regionBean!!.id)!!.subscribe(object : NetworkObserver<String?>() {
                        override fun onNext(t: String) {

                            LogUtils.e("zcl====删除区域$t----删除信息$regionBean")
                            PopUtil.dismiss(pop)
                            regionBean?.let {itr->
                                val dbRegion = DbRegion()
                                dbRegion.installMeshPwd = itr.installMeshPwd
                                dbRegion.controlMeshPwd = itr.controlMeshPwd
                                dbRegion.belongAccount = itr.belongAccount
                                dbRegion.controlMesh = itr.controlMesh
                                dbRegion.installMesh = itr.installMesh
                                dbRegion.name = itr.name
                                dbRegion.id = itr.id
                                DBUtils.deleteRegion(dbRegion)
                            }
                            initData()
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            ToastUtils.showLong(e.message)
                        }
                    })
            }
            R.id.pop_transfer_region -> {
                SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                    override fun start() {}

                    override fun complete() {
                        //生成移交单个区域 刷新二维码
                        lookAndMakeTransferRegionCode()
                    }

                    override fun error(msg: String?) {
                        ToastUtils.showLong(getString(R.string.make_code_fail))
                    }
                })
            }
            R.id.pop_share_net -> {
                SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                    override fun start() {}

                    override fun complete() {
                        //生成区域授权码
                        lookAndMakeAuthorCode()
                    }

                    override fun error(msg: String?) {
                        ToastUtils.showLong(getString(R.string.make_code_fail))
                    }
                })

            }
            R.id.pop_update_net -> {
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_new_region_name)
                PopUtil.dismiss(pop)
                isAddRegion = false
                popAdd?.let { showPop(it, Gravity.CENTER) }
            }
            R.id.pop_qr_undo -> {//取消二维码
                if (isRefresh) {//过期要生成
                    //刷新二维码
                    when (qrCodeType) {
                        1 -> SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                            override fun start() {
                            }

                            override fun complete() {
                                lookAndMakeTransferCode()
                            }

                            override fun error(msg: String?) {
                                ToastUtils.showLong(getString(R.string.make_code_fail))
                            }
                        })
                        2 -> SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
                            override fun start() {}

                            override fun complete() {
                                //生成区域授权码
                                lookAndMakeAuthorCode()
                            }

                            override fun error(msg: String?) {
                                ToastUtils.showLong(getString(R.string.make_code_fail))
                            }
                        })
                        3 -> PopUtil.dismiss(pop)
                    }
                } else {
                    //是倒计时状态 撤销二维码
                    when (qrCodeType) {
                        1 -> RegionModel.removeTransferCode()!!.subscribe {
                            //设置二维码失效状态 倒计时颜色状态
                            LogUtils.e("zcl取消移交码成功")
                            transfer_account_tv.text = getString(R.string.transfer_accounts)
                            setCancel()
                        }
                        2 -> RegionModel.removeAuthorizationCode(regionBean!!.id, regionBean!!.code_info!!.type)!!.subscribe(object : NetworkObserver<String?>() {
                            override fun onNext(t: String) {
                                LogUtils.e("zcl取消网络授权成功id" + regionBean!!.id + "=======type" + regionBean!!.code_info!!.type)
                                setCancel()
                                setCreatShareCodeState()
                            }

                            override fun onError(e: Throwable) {
                                super.onError(e)
                                ToastUtils.showLong(e.message)
                            }
                        })
                        3 -> /*RegionModel.removeQrCode(transferRegionCode)
                                ?.subscribe({
                                    PopUtil.dismiss(pop)
                                    ToastUtils.showShort(getString(R.string.QR_canceled))
                                }, { ToastUtils.showShort(it.message) })*/
                            RegionModel.removeTransferRegionCode(regionBean!!.id)
                                        ?.subscribe(object : NetworkObserver<Response<TransferRegionBean>?>() {
                                            override fun onNext(t: Response<TransferRegionBean>) {
                                                PopUtil.dismiss(pop)
                                                ToastUtils.showShort(getString(R.string.QR_canceled))
                                            }

                                            override fun onError(e: Throwable) {
                                                super.onError(e)
                                                ToastUtils.showShort(e.message)
                                            }
                                        })
                    }

                }
            }
        }
    }

    /**
     * 生成区域移交码
     */
    private fun lookAndMakeTransferRegionCode() {
        val disposable = lookAndMakeRegionQR(regionBean!!.id)?.subscribe(object : NetworkObserver<TransferRegionBean?>() {
            override fun onNext(it: TransferRegionBean) {
                    mExpire = it.expire.toLong()
                    regionBean!!.code_info!!.type = it.type
                    transferRegionCode = it.code
                    //1-移交 2 授权 3区域移交
                    qrCodeType = 3
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.text = getString(R.string.transfer_region_code)
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.visibility = View.VISIBLE
                    setQR(it.code)
                    val expire = it.expire.toLong()
                    transfer_account_tv.text = getString(R.string.transfer_accounts)
                    downTimer(expire)
                    view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE

            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(e.message)
            }
        })
    }

    /**
     * 生成区域授权码
     */
    private fun lookAndMakeAuthorCode() {
        val disposable = lookAuthorizeCode(regionBean!!.id).subscribe(object : NetworkObserver<ShareCodeBean?>() {
            override fun onNext(it: ShareCodeBean) {
                    mExpire = it.expire.toLong()
                    regionBean!!.code_info!!.type = it.type
                    authorizeCode = it.code
                    authorizeCode
                    qrCodeType = 2
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.text = getString(R.string.authorization_warm)
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.visibility = View.VISIBLE
                    setQR(authorizeCode)
                    val expire = it.expire.toLong()
                    transfer_account_tv.text = getString(R.string.transfer_accounts)
                    downTimer(expire)
                    view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE

            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(e.message)
            }
        })
    }

    /**
     * 生成账户移交码
     */
    private fun lookAndMakeTransferCode() {
        val disposable = lookTransferCode().subscribe(object : NetworkObserver<TransferBean?>() {
            override fun onNext(it: TransferBean) {
                    //0没有 1账户移交码  2账户授权码  3 区域移交码
                    qrCodeType = 1
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.text = getString(R.string.region_warm)
                    view?.findViewById<TextView>(R.id.pop_qr_area_name)?.visibility = View.VISIBLE
                    transferCode = it.code
                    setQR(transferCode)

                    downTimer(it.expire.toLong())
                    view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
                    view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.GONE
                    showPop(pop!!, Gravity.BOTTOM)
                    transfer_account_tv.text = getString(R.string.to_receive)
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(e.message)
            }
        })
    }


    private fun setCancel() {
        view?.let {
            it?.findViewById<TextView>(R.id.pop_qr_area_name)?.visibility = View.GONE
            //设置二维码失效状态 倒计时颜色状态
            it.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.QR_expired)
            it.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.red)

            //设置刷新二维码变为二维码已撤销
            it.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.black_three)
            it.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_canceled)
            it.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = false
            it.findViewById<ImageView>(R.id.pop_qr_img)?.setImageResource(R.drawable.icon_revoked)
            it.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.GONE
            it.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.share_network)

            it.findViewById<View>(R.id.view19)?.visibility = View.GONE
            mCompositeDisposable.clear()
        }
    }

    private fun changeRegion() {
        when (isShowType) {
            1 -> if (lastUser!!.last_authorizer_user_id != lastUser!!.id.toString() || regionBean!!.id.toString() != lastUser!!.last_region_id) {//切换的不是正在使用的区域
                checkNetworkAndSyncRegion(this)
            }
            2 -> if (lastUser!!.last_authorizer_user_id != regionBeanAuthorize!!.authorizer_id.toString() || regionBeanAuthorize!!.id.toString() != lastUser!!.last_region_id) {//不是正在的区域
                checkNetworkAndSyncRegion(this)
            }
        }
    }


    private fun setQR(it: String) {
        var mBitmap = CodeUtils.createImage(it, ConvertUtils.dp2px(231f), ConvertUtils.dp2px(231f), null)
        view?.findViewById<ImageView>(R.id.pop_qr_img)?.setImageBitmap(mBitmap)
    }

    @SuppressLint("SetTextI18n")
    private fun downTimer(expire: Long) {
        mCompositeDisposable.clear()
        if (expire > 0) {
            isRefresh = false//有时间就不是刷新状态
            view?.findViewById<ImageView>(R.id.pop_share_net)?.setImageResource(R.drawable.icon_code)
            view?.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.see_qr)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.black_three)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.text_red)
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_cancel)
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = true
            view?.findViewById<View>(R.id.view19)?.visibility = View.VISIBLE

            mCompositeDisposable.add(Observable
                    //从0开始发射11个数字为：0-10依次输出，延时0s执行，每1s发射一次。 (0, 11, 0, 1, TimeUnit.SECONDS
                    //从1开始发送ex个数字 1-ex  从0开始发射11个数字为：0-10依次输出
                    .intervalRange(1, expire, 0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        mExpire = expire - it
                        if (mExpire == 0L) {
                            isRefresh = true //倒计时结束 二维码过期就是刷新状态
                            when (qrCodeType) {
                                1 -> {
                                    setRefreshState()
                                    transfer_account_tv.text = getString(R.string.transfer_accounts)
                                }
                                2 -> setShareAndTimerAndCancelState()
                                3 -> {
                                    PopUtil.dismiss(pop)
                                    ToastUtils.showLong(getString(R.string.region_transfer_code_invalid))
                                }
                            }
                        } else {
                            val s = mExpire % 60
                            val m = mExpire / 60 % 60
                            val h = mExpire / 60 / 60
                            view?.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.cancel_timer) + "$h:$m:$s"
                        }
                    })
        }
    }

    private fun setShareAndTimerAndCancelState() {
        //设置分享网络状态
        setCreatShareCodeState()
        setRefreshState()
    }

    /**
     * 设置二维码失效状态 倒计时颜色状态
     */
    private fun setRefreshState() {
        //设置二维码失效状态 倒计时颜色状态
        view?.findViewById<ImageView>(R.id.pop_qr_img)?.setImageResource(R.drawable.icon_invalid)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.QR_expired)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.red)
        //设置取消二维码变为刷新状态
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.black_three)
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.refresh_qr_code)
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = true
    }

    private fun setCreatShareCodeState() {
        view?.findViewById<ImageView>(R.id.pop_share_net)?.setImageResource(R.drawable.icon_share)
        view?.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.share_network)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.black_three)
        isRefresh = true //二维码撤销 需要刷新
    }

    /**
     * 创建pop并添加按钮监听
     */
    @SuppressLint("SetTextI18n")
    private fun makePop() {
        view = View.inflate(this, R.layout.popwindown_region, null)
        viewAdd = View.inflate(this, R.layout.popwindown_add_region, null)

        pop = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pop?.setOnDismissListener {
            isShowType = 3
            initData()
        }
        //添加区域pop
        popAdd = PopupWindow(viewAdd, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setPopSetting(pop!!)
        setPopSetting(popAdd!!)

        view?.let {
            it.findViewById<TextView>(R.id.pop_qr_area_user).text = getString(R.string.cur_network_owner) + lastUser?.phone

            it.findViewById<ImageView>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_delete_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_qr_cancel).setOnClickListener(this)
            it.findViewById<LinearLayout>(R.id.pop_qr_ly).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_qr_undo).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_transfer_region).setOnClickListener(this)
        }
        viewAdd?.let {
            var name = it.findViewById<EditText>(R.id.pop_region_name)
            it.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                if (com.blankj.utilcode.util.StringUtils.isTrimEmpty(name.text.toString())) {
                    ToastUtils.showLong(getString(R.string.please_input_region_name))
                    return@setOnClickListener
                }
                isShowType = 1
                addRegion(name.text)
                PopUtil.dismiss(popAdd)
            }
            it.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                PopUtil.dismiss(popAdd)
            }
        }
    }

    private fun setPopSetting(pop: PopupWindow) {
        pop.isOutsideTouchable = true
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    /**
     * 设置指定pop内容并弹框
     */
    @SuppressLint("SetTextI18n")
    private fun setPopData(position: Int, list: MutableList<RegionBean>) {
        isShowType = 1
        regionBean = list[position]
        Log.e(TAG, "zcl******" + regionBean.toString())
        if (regionBean != null){
            view?.let { itView
                ->
                authorCodeUsefulOrUnuseful(itView)

                itView.findViewById<TextView>(R.id.pop_net_name).text = regionBean!!.name
                // itView.findViewById<TextView>(R.id.pop_qr_area_name).text = regionBean!!.name
                itView.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + regionBean!!.count_all
                //使用中不能删除 2
                val isUseingRegionId = regionBean!!.id.toString() == lastUser?.last_region_id.toString()
                val isLastUserID = regionBean!!.authorizer_id.toString() == lastUser?.last_authorizer_user_id.toString()
                val isFristRegionBean = regionBean!!.id.toInt() == 1


                itView.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + lastUser!!.phone

                LogUtils.e("zcl", "zcl***tankuang***$isUseingRegionId-----------$isLastUserID")

                if (isUseingRegionId && isLastUserID) {// 2 300551
                    itView.findViewById<ImageView>(R.id.pop_delete_net).isClickable = false
                    itView.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
                    itView.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.mipmap.icon_delete)
                } else {
                    itView.findViewById<ImageView>(R.id.pop_delete_net).isClickable = !isFristRegionBean
                    if (isFristRegionBean) //如果是第一个或者是授权区域不允许删除
                        itView.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)
                    else
                        itView.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete_bb)
                    itView.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
                }

                if (regionBean!!.ref_users?.size!! <= 0) {//没有授权不能进入解绑
                    itView.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = false
                    itView.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.drawable.icon_untied)
                } else {
                    itView.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = true
                    itView.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.drawable.icon_untied_b)
                }

                itView.findViewById<ImageView>(R.id.pop_update_net).isClickable = true
                itView.findViewById<ImageView>(R.id.pop_update_net).setImageResource(R.drawable.icon_modify)

                mExpire = regionBean!!.code_info!!.expire.toLong()
                LogUtils.e(TAG, "zcl****regionBean**${regionBean.toString()}")
                itView.findViewById<ImageView>(R.id.pop_share_net).isClickable = true
                view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.GONE
                view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.VISIBLE

                trsansferCodeUsefulOrUnuseful(itView)
            }
        }
    }

    private fun trsansferCodeUsefulOrUnuseful(itView: View) {
         RegionModel.lookTransforRegionCode(regionBean!!.id)
                ?.subscribe(object : NetworkObserver<TransferRegionBean?>() {
                    override fun onNext(it: TransferRegionBean) {
                        var isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
                        if (!isNewQr){
                            itView.findViewById<ImageView>(R.id.pop_transfer_region).setImageResource(R.drawable.icon_code)
                            itView.findViewById<TextView>(R.id.pop_transfer_region_tv)?.text = getString(R.string.see_qr)
                        } else {
                            itView.findViewById<ImageView>(R.id.pop_transfer_region).setImageResource(R.drawable.icon_single)
                            itView.findViewById<TextView>(R.id.pop_transfer_region_tv)?.text = getString(R.string.transfer_region)
                        }
                    }

                    override fun onError(it: Throwable) {
                        super.onError(it)
                        itView.findViewById<ImageView>(R.id.pop_transfer_region).setImageResource(R.drawable.icon_single)
                        itView.findViewById<TextView>(R.id.pop_transfer_region_tv)?.text = getString(R.string.transfer_region)
                    }
                })
    }


    /**
     * 查看收授权qr是否过期
     */
    private fun authorCodeUsefulOrUnuseful(itView: View) {
         RegionModel.lookAuthorCodeState(regionBean!!.id)?.subscribe(object : NetworkObserver<ShareCodeBean?>() {
             override fun onNext(it: ShareCodeBean) {
                     LogUtils.e("zcl_network-------授权码信息是否可用-------$it")
                     mExpire = it.expire.toLong()

                     isRefresh = mExpire <= 0 //过期或者撤销需要刷新

                     if (mExpire > 0) {
                         itView.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.drawable.icon_code)
                         itView.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.see_qr)
                     } else {
                         itView.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.drawable.icon_share)
                         itView.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.share_network)
                     }
                     showPop(pop!!, Gravity.BOTTOM)

             }

             override fun onError(it: Throwable) {
                 super.onError(it)
                 ToastUtils.showLong(it.message)
                 showPop(pop!!, Gravity.BOTTOM)
             }
         })
    }

    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    private fun checkNetworkAndSyncRegion(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
                    }.create().show()
        } else {
            val lastUser = lastUser
            lastUser?.let {
                //如果是自己的区域切换到其他区域 那么就要上传自己数据 否则不上传
                if (it.id.toString() == it.last_authorizer_user_id) {
                    SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
                } else {
                    downLoadDataAndChangeDbUser()
                }
            }

        }
    }

    /**
     * 上传回调
     */
    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(this@NetworkActivity.getString(R.string.start_change_region))
        }

        override fun complete() {
            ToastUtils.showLong(this@NetworkActivity.getString(R.string.success_change_region))
            Log.e(TAG, "zcl******上传数据成功")
            downLoadDataAndChangeDbUser()
            hideLoadingDialog()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(getString(R.string.error_change_region))
            hideLoadingDialog()
        }
    }

    private fun downLoadDataAndChangeDbUser() {
        TelinkLightService.Instance()?.disconnect()
        TelinkLightService.Instance()?.idleMode(true)

        //创建数据库
        var lastUser = lastUser

        lastUser?.let {
            //更新user
            when (isShowType) {
                1 -> {//自己的区域
                    it.last_region_id = regionBean?.id.toString()
                    it.last_authorizer_user_id = regionBean?.authorizer_id.toString()
                }
                2 -> {//授权的区域
                    it.last_region_id = regionBeanAuthorize?.id.toString()
                    it.last_authorizer_user_id = regionBeanAuthorize?.authorizer_id.toString()
                }
            }

            DBUtils.deleteAllData()
            //创建数据库
            AccountModel.initDatBase(it)
            //更新last—region-id
            DBUtils.saveUser(it)
            //下拉数据
            SyncDataPutOrGetUtils.syncGetDataStart(it, syncCallbackGets)

            view?.findViewById<ImageView>(R.id.pop_delete_net)?.isClickable = false
            view?.findViewById<ImageView>(R.id.pop_delete_net)?.setImageResource(R.drawable.icon_delete)
            isShowType = 3
        }
    }

    private fun showUnbindDialog() {
        val builder = android.support.v7.app.AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.warm_unbind_authorize_config, regionBeanAuthorize!!.name))
        builder.setNegativeButton(getString(R.string.btn_sure)) { dialog, _ ->
            //解除授权
            //authorizer_id授权用户id  rid区域id
            RegionModel.dropAuthorizeRegion(regionBeanAuthorize!!.authorizer_id, regionBeanAuthorize!!.id)
                    ?.subscribe(object : NetworkObserver<String?>() {
                        override fun onNext(t: String) {
                                isShowType = 2
                                initData()
                                ToastUtils.showLong(getString(R.string.unbundling_success))
                                PopUtil.dismiss(pop)

                        }

                        override fun onError(it: Throwable) {
                            super.onError(it)
                            ToastUtils.showLong(it.message)
                        }
                    })
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    var syncCallbackGets: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {
            Log.e(TAG, "zcl******下拉数据成功")
            view?.findViewById<ImageView>(R.id.pop_user_net)?.setImageResource(R.drawable.icon_use_blue)
            initData()
        }

        override fun error(msg: String) {}
    }

    private fun showPop(pop: PopupWindow, gravity: Int) {
        if (!pop.isShowing && !this.isFinishing)
            pop.showAtLocation(window.decorView, gravity, 0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
        PopUtil.dismiss(popAdd)
        disposableEnsure?.dispose()
        disposableRequest?.dispose()
        mCompositeDisposable.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE -> {  //处理扫描结果
                if (null != data) {
                    var bundle: Bundle? = data.extras ?: return
                    if (bundle!!.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                        var result = bundle.getString(CodeUtils.RESULT_STRING)
                        Log.e(TAG, "zcl***parse_result***$result")
                        postParseCode(result)
                    } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                        Toast.makeText(this, getString(R.string.fail_parse_qr), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun postParseCode(result: String?) {
        result?.let { it1 ->
            RegionModel.parseQRCode(it1, NetworkFactory.md5(lastUser!!.password))?.subscribe(object : NetworkObserver<ParseCodeBean>() {
                override fun onNext(t: ParseCodeBean) {
                    initData()
                    setChangeDialog(t)
                    ToastUtils.showLong(getString(R.string.scan_success))
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CARMER) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                var intent = Intent(this@NetworkActivity, CaptureActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE)
            } else {
                ToastUtils.showLong(getString(R.string.fail_parse_qr))
            }
        }
    }

    private fun openScan() {
        var intent = Intent(this@NetworkActivity, CaptureActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()
        isShowType = 3
                //initData()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun notifyWSData(type: Int, rid: Int) {
        PopUtil.dismiss(pop)
        PopUtil.dismiss(popAdd)
        isShowType = 3

        when (type) {
            -1, 0, 2 -> { //移交的区域已被接收 账号已被接收 解除了区域%1$s的授权
                if (lastUser?.last_region_id?.toInt()== rid || rid == 1) {//如果正在使用或者是区域一则退出
                    //DBUtils.deleteAllData()//删除数据
                    restartApplication()
                } else {//否则只进行刷新
                    initData()
                }
            }

            1 -> {//分享的区域已被接收
                RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
                    override fun onNext(it: MutableList<RegionBean>) {
                        setMeData(it)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        ToastUtils.showLong(e.message)
                        hideLoadingDialog()
                    }
                })
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        hideLoadingDialog()
        if (qrCodeType == 3)
            RegionModel.removeTransferRegionCode(regionBean!!.id)?.subscribe(object : NetworkObserver<Response<TransferRegionBean>?>() {
                override fun onNext(t: Response<TransferRegionBean>) {

                }
            })
    }

}
