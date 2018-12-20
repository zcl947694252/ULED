package com.dadoutek.uled.scene

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSceneActions
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.SelectColorAct
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_new_scene_set.*
import kotlinx.android.synthetic.main.toolbar.*

class NewSceneSetAct : TelinkBaseActivity(), View.OnClickListener {
    private var currentPageIsEdit=false
    private var scene: DbScene? = null
    private var isChangeScene = false
    private var isChange = true
    private var showGroupList: MutableList<ItemGroup>? = null
    private var showCheckListData: MutableList<DbGroup>? = null
    private var sceneGroupAdapter: SceneGroupAdapter? = null
    private var sceneEditListAdapter: SceneEditListAdapter? = null
    private var editSceneName:String?=null
    private val groupMeshAddrArrayList = java.util.ArrayList<Int>()
    private var guideShowCurrentPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_scene_set)
        init()
        if (!isChangeScene) {
            edit_name.setText(DBUtils.getDefaultNewSceneName())
            initOnLayoutListener()
        }
    }

    private fun initOnLayoutListener() {
        val view = this.window.decorView
        val viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    private fun lazyLoad() {
        step0Guide()
    }

    private fun step0Guide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            GuideUtils.guideBuilder(this, GuideUtils.ADDITIONAL_SCENE_GUIDE_KEY_INPUT_NAME)
                    .addGuidePage(GuideUtils.addGuidePage(edit_name, R.layout.view_guide_simple_scene_set0, getString(R.string.add_scene_guide_0),
                            View.OnClickListener {  step1Guide() }, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }

    private fun step1Guide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            val guide1=sceneEditListAdapter!!.getViewByPosition(0,R.id.group_check_state)
            GuideUtils.guideBuilder(this, GuideUtils.STEP8_GUIDE_ADD_SCENE_ADD_GROUP)
                    .addGuidePage(GuideUtils.addGuidePage(guide1!!, R.layout.view_guide_simple, getString(R.string.add_scene_guide_1),
                             View.OnClickListener {
                                changeCheck(0)
                             }, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }

    private fun step2Guide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            val guide1=confirm
            GuideUtils.guideBuilder(this, GuideUtils.STEP9_GUIDE_ADD_SCENE_SURE)
                    .addGuidePage(GuideUtils.addGuidePage(guide1!!, R.layout.view_guide_simple, getString(R.string.add_scene_guide_2),
                            View.OnClickListener {guide1.performClick()}, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }

    private fun stepEndGuide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            val guide6 = confirm
            GuideUtils.guideBuilder(this, GuideUtils.STEP13_GUIDE_ADD_SCENE_SAVE)
                    .addGuidePage(GuideUtils.addGuidePage(guide6, R.layout.view_guide_simple_scene_set1, getString(R.string.add_scene_guide_6),
                            View.OnClickListener {guide6.performClick()}, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }

    private fun init() {
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
                groupMeshAddrArrayList.add(item.getMeshAddr())
            }
        }
        initScene()
    }

    private fun initScene() {
        if (isChangeScene) {
            initChangeView()
            showDataListView()
        } else {
            initCreateView()
            showEditListVew()
        }

        confirm.setOnClickListener(this)
        StringUtils.initEditTextFilter(edit_name)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            if(currentPageIsEdit){
                finish()
            }else{
                showExitSaveDialog()
            }
        }
    }

    private fun showExitSaveDialog() {
        val exitDialogBuilder=AlertDialog.Builder(this)
        exitDialogBuilder.setMessage(getString(R.string.save_scene_tip))
        exitDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
            save()
        }
        exitDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
            finish()
        }

        exitDialogBuilder.create().show()
    }

    private fun initCreateView() {
        toolbar.setTitle(R.string.create_scene)
    }

    private fun initChangeView() {
        toolbar.setTitle(R.string.edit_scene)
        tv_scene_name.text = resources.getString(R.string.scene_name_show,scene!!.name)
        editSceneName = scene!!.name
    }

    internal var onItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when (view.id) {
            R.id.btn_delete -> delete(adapter, position)
            R.id.rgb_view -> changeToColorSelect(position)
        }
    }

    private fun changeToColorSelect(position: Int) {
       val intent=Intent(this,SelectColorAct::class.java)
        intent.putExtra(Constant.GROUPS_KEY, showGroupList!![position])
        startActivityForResult(intent, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            val color=data?.getIntExtra("color",0)
            showGroupList!![requestCode].color= color!!
            sceneGroupAdapter?.notifyItemChanged(requestCode)
        }
    }

    internal var onItemClickListenerCheck: BaseQuickAdapter.OnItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        changeCheck(position)
    }

    fun changeCheck(position: Int) {
        val item = showCheckListData!!.get(position)
        if(item.enableCheck){
            showCheckListData!!.get(position).checked = !item.checked
            changeCheckedViewData()
            sceneEditListAdapter?.notifyDataSetChanged()
        }
        step2Guide()
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
        this.sceneGroupAdapter = SceneGroupAdapter(R.layout.scene_group_item, showGroupList!!)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_group_list_view.addItemDecoration(decoration)
        sceneGroupAdapter?.bindToRecyclerView(recyclerView_group_list_view)

        sceneGroupAdapter?.onItemChildClickListener=onItemChildClickListener
    }

    //显示配置数据页面
    private fun showEditListVew() {
        if(scene!=null){
            edit_name.setText(scene!!.name)
        }
        currentPageIsEdit=true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE
        tv_function1.visibility = View.GONE

        showCheckListData = DBUtils.allGroups
        if(showGroupList!!.size!=0){
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
        }else{
            for(i in showCheckListData!!.indices){
                showCheckListData!![i].enableCheck=true
                showCheckListData!![i].checked=false
            }
        }

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_select_group_list_view.layoutManager = layoutmanager
        this.sceneEditListAdapter = SceneEditListAdapter(R.layout.scene_group_edit_item, showCheckListData!!)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_select_group_list_view.addItemDecoration(decoration)
        sceneEditListAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
        sceneEditListAdapter?.onItemClickListener=onItemClickListenerCheck
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
        if(!currentPageIsEdit){
            saveScene()
        }else{
            saveCurrenEditResult()
            stepEndGuide()
        }
    }

    private fun saveCurrenEditResult() {
        if(checkName()){
            editSceneName=edit_name.text.toString()
            tv_scene_name.text = resources.getString(R.string.scene_name_show,editSceneName)
            val oldResultItemList= ArrayList<ItemGroup>()
            val newResultItemList= ArrayList<ItemGroup>()

            for(i in showCheckListData!!.indices){
                if(showCheckListData!![i].checked){
                    if(showGroupList!!.size==0){
                        val newItemGroup=ItemGroup()
                        newItemGroup.brightness=50
                        newItemGroup.temperature=50
                        newItemGroup.color=R.color.white
                        newItemGroup.checked=true
                        newItemGroup.enableCheck=true
                        newItemGroup.gpName=showCheckListData!![i].name
                        newItemGroup.groupAress=showCheckListData!![i].meshAddr
                        newResultItemList.add(newItemGroup)
                    }else{
                        for(j in showGroupList!!.indices){
                            if(showCheckListData!![i].meshAddr==showGroupList!![j].groupAress){
                                oldResultItemList.add(showGroupList!![j])
                                break
                            }else if(j == showGroupList!!.size-1){
                                val newItemGroup=ItemGroup()
                                newItemGroup.brightness=50
                                newItemGroup.temperature=50
                                newItemGroup.color=R.color.white
                                newItemGroup.checked=true
                                newItemGroup.enableCheck=true
                                newItemGroup.gpName=showCheckListData!![i].name
                                newItemGroup.groupAress=showCheckListData!![i].meshAddr
                                newResultItemList.add(newItemGroup)
                            }
                        }
                    }
                }
            }
            showGroupList?.clear()
            showGroupList?.addAll(oldResultItemList)
            showGroupList?.addAll(newResultItemList)
            showDataListView()
        }
    }

    private fun checkName(): Boolean {
        if(edit_name.text.toString().isEmpty()){
            ToastUtils.showLong(getString(R.string.name_can_not_null))
            return false
        }
        return true
    }

    private fun saveScene(){
        if (checked()) {
            if (isChangeScene) {
                updateOldScene()
            } else {
                saveNewScene()
            }
            setResult(Constant.RESULT_OK)
        }
    }

    private fun saveNewScene() {
        showLoadingDialog(getString(R.string.saving))
        Thread {
            val name = editSceneName
            //        List<ItemGroup> itemGroups = adapter.getData();
            val itemGroups = showGroupList

            val dbScene = DbScene()
            dbScene.id = getSceneId()
            dbScene.name = name
            dbScene.belongRegionId = SharedPreferencesUtils.getCurrentUseRegion()
            DBUtils.saveScene(dbScene, false)

            val idAction = dbScene.id!!

            for (i in itemGroups!!.indices) {
                val sceneActions = DbSceneActions()
                sceneActions.belongSceneId = idAction
                sceneActions.brightness = itemGroups.get(i).brightness
                sceneActions.setColor(itemGroups.get(i).color)
                sceneActions.colorTemperature = itemGroups.get(i).temperature
                //            if (isSave) {//选择的组里面包含了所有组，用户仍然确定了保存,只保存所有组
                //                sceneActions.setGroupAddr(0xFFFF);
                //                DBUtils.saveSceneActions(sceneActions);
                //                break;
                //            } else {
                sceneActions.groupAddr = itemGroups.get(i).groupAress
                DBUtils.saveSceneActions(sceneActions)
                //            }
            }

            try {
                Thread.sleep(100)
                addScene(idAction)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideLoadingDialog()
                finish()
            }
        }.start()
    }

    @Throws(InterruptedException::class)
    private fun addScene(id: Long) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray

        for (i in list.indices) {
            var count = 0
            do {
                count++
                Thread.sleep(300)
                var temperature = list[i].colorTemperature.toByte()
                if (temperature > 99)
                    temperature = 99
                var light = list[i].brightness.toByte()
                if (light > 99)
                    light = 99
                val color = list[i].getColor()
                var red = color and 0xff0000 shr 16
                var green = color and 0x00ff00 shr 8
                var blue = color and 0x0000ff

                val minVal = 0x50.toByte()
                if (green and 0xff <= minVal)
                    green = 0
                if (red and 0xff <= minVal)
                    red = 0
                if (blue and 0xff <= minVal)
                    blue = 0


                params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, list[i].groupAddr, params)
            } while (count < 3)
        }
    }

    private fun getSceneId(): Long {
        val list = DBUtils.sceneList
        val idList = java.util.ArrayList<Int>()
        for (i in list.indices) {
            idList.add(list[i].id!!.toInt())
        }

        var id = 0
        for (i in 1..16) {
            if (idList.contains(i)) {
                Log.d("sceneID", "getSceneId: " + "aaaaa")
                continue
            } else {
                id = i
                Log.d("sceneID", "getSceneId: bbbbb$id")
                break
            }
        }

        if (list.size == 0) {
            id = 1
        }

        return java.lang.Long.valueOf(id.toLong())
    }

    private fun updateOldScene() {
        showLoadingDialog(getString(R.string.saving))
        Thread {
            val name = editSceneName
            val itemGroups = showGroupList
            val nameList = java.util.ArrayList<Int>()

            scene?.setName(name)
            DBUtils.updateScene(scene!!)
            val idAction = scene?.getId()!!

            DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(scene?.getId()!!))

            for (i in itemGroups!!.indices) {
                val sceneActions = DbSceneActions()
                sceneActions.belongSceneId = idAction
                sceneActions.brightness = itemGroups.get(i).brightness
                sceneActions.colorTemperature = itemGroups.get(i).temperature
                sceneActions.groupAddr = itemGroups.get(i).groupAress
                sceneActions.setColor(itemGroups.get(i).color)

                nameList.add(itemGroups.get(i).groupAress)
                DBUtils.saveSceneActions(sceneActions)
            }

            isChange = compareList(nameList, groupMeshAddrArrayList)

            try {
                Thread.sleep(100)
                updateScene(idAction)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideLoadingDialog()
                finish()
            }
        }.start()
    }

    @Throws(InterruptedException::class)
    private fun updateScene(id: Long) {
        deleteScene(id)
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray
        for (i in list.indices) {
            Thread.sleep(100)
            var temperature = list[i].colorTemperature.toByte()
            if (temperature > 99)
                temperature = 99
            var light = list[i].brightness.toByte()
            if (light > 99)
                light = 99
            val color = list[i].getColor()
            var red = color and 0xff0000 shr 16
            var green = color and 0x00ff00 shr 8
            var blue = color and 0x0000ff

            val minVal = 0x50.toByte()
            if (green and 0xff <= minVal)
                green = 0
            if (red and 0xff <= minVal)
                red = 0
            if (blue and 0xff <= minVal)
                blue = 0

            val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
            Log.d("RGBCOLOR", logStr)
            params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature)
            TelinkLightService.Instance().sendCommandNoResponse(opcode, list[i].groupAddr, params)
        }
    }

    private fun deleteScene(id: Long) {
        if (isChange) {
            val opcode = Opcode.SCENE_ADD_OR_DEL
            val params: ByteArray
            params = byteArrayOf(0x00, id.toByte())
            try {
                Thread.sleep(100)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, 0xFFFF, params)
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    private fun compareList(actionsList: List<Int>, actionsList1: java.util.ArrayList<Int>): Boolean {
        return if (actionsList.size == actionsList1.size) {
            !actionsList1.containsAll(actionsList)
        } else {
            true
        }
    }

    private fun checked(): Boolean {
        if (showGroupList!!.size == 0) {
            ToastUtils.showLong(R.string.add_scene_gp_tip)
            return false
        }
        return true
    }
}