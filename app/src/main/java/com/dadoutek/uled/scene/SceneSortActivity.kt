package com.dadoutek.uled.scene

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.switches.SceneRecycleGridAdapter
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import kotlin.collections.ArrayList

class SceneSortActivity : BaseActivity() {

    private var recyclerView: RecyclerView? = null
    private var scenesListData: MutableList<DbScene> = ArrayList()
    private var adapter: SceneRecycleGridAdapter? = null

    override fun initListener() {

    }

    override fun initView() {
        recyclerView = findViewById(R.id.rv_order_scene)
        recyclerView?.layoutManager = GridLayoutManager(this,2)
        scenesListData = DBUtils.sceneList // 获取场景的数据
        scenesListData.sortBy { it.index }
        image_bluetooth.visibility = View.GONE
        order_scene.visibility = View.GONE
        toolbarTv.text = getString(R.string.sort)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            // 在finish之前更新数据库的index属性 发现无用，还是删除所有数据库在进行保存
            for ((i, dbscene) in scenesListData.withIndex()) {
//                DBUtils.getSceneByID(dbscene.id) // chown
                DBUtils.deleteScene(dbscene)
            }
            for ((i, dbscene) in scenesListData.withIndex()) {
                dbscene.index = i
                DBUtils.saveScene(dbscene,false)
            }
            setResult(Activity.RESULT_OK)
            LogUtils.v("chown ====--====== $scenesListData")
            finish()
        }
        adapter = SceneRecycleGridAdapter(R.layout.template_device_type_item, scenesListData)
        adapter?.bindToRecyclerView(recyclerView)
        adapter?.notifyDataSetChanged()
        helper.attachToRecyclerView(recyclerView)
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_scene_order
    }

    override fun initData() {
    }

    var helper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            var dragFrlg = 0
            dragFrlg = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            return makeMovementFlags(dragFrlg, 0)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(scenesListData,i,i+1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(scenesListData,i,i-1)
                }
            }
            adapter?.notifyItemMoved(fromPosition,toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        }

    })
}