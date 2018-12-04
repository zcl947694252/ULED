package com.dadoutek.uled.scene

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_new_scene_set.*
import kotlinx.android.synthetic.main.toolbar.*

class NewSceneSetAct : TelinkBaseActivity(), View.OnClickListener {
    private var currentPageIsEdit=false
    private var scene: DbScene? = null
    private var isChangeScene = false
    private var showGroupList: MutableList<ItemGroup>? = null
    private var showCheckListData: MutableList<DbGroup>? = null
    private var sceneGroupAdapter: SceneGroupAdapter? = null
    private var sceneEditListAdapter: SceneEditListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_scene_set)
        initData()
        initView()
    }

    private fun initData() {
        val intent = intent
        isChangeScene = intent.extras!!.get(Constant.IS_CHANGE_SCENE) as Boolean
        showGroupList = ArrayList()
        if (isChangeScene) {
            scene = intent.extras!!.get(Constant.CURRENT_SELECT_SCENE) as DbScene
            val actions = DBUtils.getActionsBySceneId(scene!!.id)
            for (i in actions.indices) {
                val item = DBUtils.getGroupByMesh(actions[i].groupAddr)
                val itemGroup = ItemGroup()
                itemGroup.gpName=item.name
                itemGroup.enableCheck = false
                itemGroup.groupAress=actions[i].groupAddr
                itemGroup.brightness =actions[i].brightness
                itemGroup.temperature = actions[i].colorTemperature
                itemGroup.color=actions[i].color
                item.checked = true
                showGroupList!!.add(itemGroup)
            }
        }
    }

    private fun initView() {
        if (isChangeScene) {
            initChangeData()
            initChangeView()
            showDataListView()
        } else {
            initCreateData()
            initCreateView()
            showEditListVew()
        }

        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initCreateData() {

    }

    private fun initChangeData() {

    }

    private fun initCreateView() {
        toolbar.setTitle(R.string.create_scene)
    }

    private fun initChangeView() {
        toolbar.setTitle(R.string.edit_scene)
        tv_scene_name.text = resources.getString(R.string.scene_name_show,scene!!.name)
    }

    internal var onItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when (view.id) {
            R.id.btn_delete -> delete(adapter, position)
            R.id.rgb_view -> {
//                currentPosition = position
//                showPickColorDialog()
            }
        }
    }

    internal var onItemChildClickListenerCheck: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when (view.id) {
            R.id.group_check_state -> {
                val item = showCheckListData!!.get(position)
                if(item.enableCheck){
                    showCheckListData!!.get(position).checked = !item.checked
                    changeCheckedViewData()
                    sceneEditListAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun delete(adapter: BaseQuickAdapter<*, *>, position: Int) {
        adapter.remove(position)
    }

    //显示数据列表页面
    private fun showDataListView() {
        currentPageIsEdit=false
        data_view_layout.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE
        tv_function1.visibility = View.VISIBLE
        tv_function1.text = getString(R.string.edit)
        tv_function1.setOnClickListener(this)

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_group_list_view.layoutManager = layoutmanager
        this.sceneGroupAdapter = SceneGroupAdapter(R.layout.scene_group_item, showGroupList)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_group_list_view.addItemDecoration(decoration)
        sceneGroupAdapter?.bindToRecyclerView(recyclerView_group_list_view)

        sceneGroupAdapter?.onItemChildClickListener=onItemChildClickListener
    }

    //显示配置数据页面
    private fun showEditListVew() {
        currentPageIsEdit=true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE
        tv_function1.visibility = View.GONE

        showCheckListData = DBUtils.allGroups
        for(i in showCheckListData!!.indices){
            for(j in showGroupList!!.indices){
                if(showCheckListData!![i].meshAddr == showGroupList!![j].groupAress){
                    showCheckListData!![i].checked= true
                    break
                }else if(j==showGroupList!!.size-1 && showCheckListData!![i].meshAddr != showGroupList!![j].groupAress){
                    showCheckListData!![i].checked= false
                }
            }
        }
        changeCheckedViewData()

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_select_group_list_view.layoutManager = layoutmanager
        this.sceneEditListAdapter = SceneEditListAdapter(R.layout.scene_group_edit_item, showCheckListData!!)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_select_group_list_view.addItemDecoration(decoration)
        sceneEditListAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
        sceneEditListAdapter?.onItemChildClickListener=onItemChildClickListenerCheck
    }

    private fun changeCheckedViewData(){
        var isAllCanCheck=true
        for(i in showCheckListData!!.indices){
            if(showCheckListData!![0].meshAddr==0xffff && showCheckListData!![0].checked){
                showCheckListData!![0].enableCheck=true
                if(showCheckListData!!.size>1 && i>0){
                    showCheckListData!![i].enableCheck=false
                }
            }else{
                showCheckListData!![0].enableCheck=false
                if(showCheckListData!!.size>1 && i>0){
                    showCheckListData!![i].enableCheck=true
                }

                if(i>0 && showCheckListData!![i].checked){
                    isAllCanCheck=false
                }
            }
        }
        if(isAllCanCheck){
            showCheckListData!![0].enableCheck=true
        }
    }

    private fun changeEditView() {
        showEditListVew()
    }

    override fun onClick(v: View?) {
       when(v?.id){
           R.id.tv_function1->changeEditView()
           R.id.confirm->save()
       }
    }

    private fun save() {
        if(currentPageIsEdit){
            saveScene()
        }else{
            saveCurrenEditResult()
        }
    }

    private fun saveCurrenEditResult() {
        val checkedItemList= ArrayList<Int>()
        val resultItemList= ArrayList<ItemGroup>()

       for(i in showCheckListData!!.indices){
          if(showCheckListData!![i].checked){
              for(i in showGroupList!!.indices){
                  checkedItemList.add(showGroupList!![i].groupAress)
              }
          }
       }
    }

    private fun saveScene(){

    }
}