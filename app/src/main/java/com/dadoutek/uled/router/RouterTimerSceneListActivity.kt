package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.ViewGroup
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.RouterTimerSceneBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.util.DensityUtil
import com.yanzhenjie.recyclerview.SwipeMenu
import com.yanzhenjie.recyclerview.SwipeMenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_batch_group_four.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import org.greenrobot.greendao.DbUtils
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/10/27 18:12
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterTimerSceneListActivity : TelinkBaseActivity() {
    //操作后当前开启的状态
    private var currentStatus: Int = 0
    private var currentPostion: Int = 0
    private var list = mutableListOf<RouterTimerSceneBean>()
    val adapter = RouterTimerSceneItemAdapter(R.layout.event_item, list)
    private val function: (leftMenu: SwipeMenu, rightMenu: SwipeMenu, position: Int) -> Unit = { _, rightMenu, _ ->
        val menuItem = SwipeMenuItem(this@RouterTimerSceneListActivity)// 创建菜单
        menuItem.height = ViewGroup.LayoutParams.MATCH_PARENT
        menuItem.weight = DensityUtil.dip2px(this, 500f)
        menuItem.textSize = 20
        menuItem.setBackgroundColor(getColor(R.color.red))
        menuItem.setText(R.string.delete)
        rightMenu.addMenuItem(menuItem)//添加进右侧菜单
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router_timer_scene_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        add_group_btn.setOnClickListener {
            startActivity(Intent(this@RouterTimerSceneListActivity, AddTimerSceneActivity::class.java))
        }
        adapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.item_event_switch) {
                currentPostion = position
                routerChangeTimerSceneStatus()
            } else {
                val intent = Intent(this@RouterTimerSceneListActivity, AddTimerSceneActivity::class.java)
                intent.putExtra("timerScene", list[position])
                startActivity(intent)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerChangeTimerSceneStatus() {
        val timerSceneBean = list[currentPostion]
        //state	int	1关联场景存在， 0关联场景已删除  status	int	开关状态，1开 0关
        currentStatus = if (timerSceneBean.status == 1) 0 else 1
        RouterModel.routeTimerSceneStatus(timerSceneBean.id, currentStatus, "swStatus")?.subscribe({
            LogUtils.v("zcl-----------路由请求更改状态$currentStatus")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                if (currentStatus == 1)
                                    ToastUtils.showShort(getString(R.string.timer_scene_open_faile))
                                else
                                    ToastUtils.showShort(getString(R.string.timer_scene_close_faile))
                                adapter.notifyDataSetChanged()
                            }
                }
                90030 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterChangeTimerSceneStatus(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由swStatus通知-------$cmdBean")
        if (cmdBean.ser_id == "swStatus") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            when (cmdBean.status) {
                0 -> list[currentPostion].status = currentStatus
                else -> ToastUtils.showShort(getString(R.string.timer_scene_close_faile))
            }
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {// LogUtils.v("zcl-----------获取路由定时列表-------$it")

        RouterModel.routeTimerSceneList()?.subscribe({
            list.clear()
            list.addAll(it)
            adapter.notifyDataSetChanged()
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.timer_scene)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        add_group_btn_tv.text = getString(R.string.add_timer_scene)

        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        swipe_recycleView.setSwipeMenuCreator(function)   // 设置监听器。
        swipe_recycleView.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            currentPostion = adapterPosition
            routerDelTimerScene()
        }
        swipe_recycleView.adapter = adapter
    }

    @SuppressLint("CheckResult")
    private fun routerDelTimerScene() {
        RouterModel.routeDelTimerScene(list[currentPostion].id, "delTimerScene")?.subscribe({
            LogUtils.v("zcl-----------路由请求删除定时场景$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                             .subscribeOn(Schedulers.io())
                                             .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.delete_timerscne_fail))
                            }
                }
                90030 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterDelTimerScene(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由delTimerScene通知-------$cmdBean")
        if (cmdBean.ser_id == "delTimerScene") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            when (cmdBean.status) {
                0 -> {
                    list.removeAt(currentPostion)
                    adapter.notifyDataSetChanged()
                }
                else -> ToastUtils.showShort(getString(R.string.delete_timerscne_fail))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initData()
    }
}
