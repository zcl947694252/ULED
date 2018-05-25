package com.dadoutek.uled.model.HttpModel

import android.text.TextUtils
import android.util.Log
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.util.NetworkUtils
import com.mob.tools.utils.DeviceHelper.getApplication
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

//看起来像存放静态方法的静态类，实际上就是单例模式。
object AccountModel {
    fun login(phone: String, password: String, channel: String): Observable<DbUser> {
        lateinit var account: String
        return NetworkUtils.getApi()
                .getAccount(phone, channel)
                .compose(NetworkTransformer())
                .flatMap { response: String ->
                    account = response
                    NetworkUtils.getApi()
                            .getsalt(response)
                            .compose(NetworkTransformer())

                }
                .flatMap { response: String ->
                    val salt = response
                    val md5Pwd = NetworkUtils.md5(
                            NetworkUtils.md5(
                                    NetworkUtils.md5(password) + account) + salt)
                    NetworkUtils.getApi().login(account, md5Pwd)
                            .compose(NetworkTransformer())
                }
                .observeOn(Schedulers.io())
                .doOnNext {
                    setIsLogin(true)
                    initDatBase(it)
                    setupMesh(phone)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun oldDataConvertToNewData(name: String, pwd: String) {
        launch(CommonPool) {
            delay(500L)
            oldDataConvertToNewDataGroup(name,pwd)
            delay(500L)
            oldDataConvertToNewDataLight(name,pwd)
            delay(200L)

            //原有数据转化完成清空本地数据
            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                    name+pwd+Constant.LIGHTS_KEY,null)
            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                    name+pwd+Constant.GROUPS_KEY,null)
        }
    }

    private fun oldDataConvertToNewDataLight(name: String, pwd: String) {
        val lights= SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                name+pwd+Constant.LIGHTS_KEY) as? Lights

        if(lights!!.get()!=null&&lights.get().size>0){
            for(item in lights.get()){
                var light : DbLight = DbLight()
                if(item.belongGroups.size>0){
                    light.belongGroupId = DBUtils.getGroupByMesh(item.belongGroups.get(0)).id
                }
                light.brightness=item.brightness
                light.colorTemperature=item.temperature
                light.meshAddr=item.meshAddress
                light.name=item.name
                light.macAddr=item.macAddress
                if (item.raw != null) {
                    light.meshUUID = item.raw.meshUUID
                    light.productUUID = item.raw.productUUID
                } else {
                    light.meshUUID = 0
                    light.productUUID = 0
                }
                DBUtils.saveLight(light)
            }
        }
    }

    private fun oldDataConvertToNewDataGroup(name: String, pwd: String) {
       val groups= SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
               name+pwd+Constant.GROUPS_KEY) as? Groups

        val dbRegion=DBUtils.getLastRegion()

       if(groups!=null){
          for(item in groups.get()){
              var dbGroup : DbGroup = DbGroup()
              if(item.meshAddress==0xffff){
                  continue
              }
              dbGroup.belongRegionId = dbRegion.id.toInt()
              dbGroup.brightness=item.brightness
              dbGroup.colorTemperature=item.temperature
              dbGroup.meshAddr=item.meshAddress
              dbGroup.name=item.name
              DBUtils.saveGroup(dbGroup)
          }
       }
    }

    private fun initDatBase(user: DbUser) {
        //首先保存当前数据库名
        SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, user.account)

        //数据库分库
        DaoSessionInstance.destroySession()
        DaoSessionInstance.getInstance()

        //从云端用户表同步数据到本地
//        dbUser = user
        DBUtils.saveUser(user)
        DBUtils.createAllLightControllerGroup()
    }

    private fun setIsLogin(isLogin: Boolean) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, isLogin)
    }

    private fun setupMesh(phone: String) {

        val regionList = DBUtils.getRegionAll()

        //数据库有区域数据直接加载
        if(regionList.size!=0){
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegion()
            val dbRegion=DBUtils.getLastRegion()
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = dbRegion.controlMesh
            mesh.password = dbRegion.controlMeshPwd
            mesh.factoryName = dbRegion.installMesh
            mesh.password = dbRegion.installMeshPwd
            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
            return
        }

        //数据库无区域数据（本地拿取，本地没有就给默认，本地有则同步本地到数据库然后清除本地）
        val name = SharedPreferencesHelper.getMeshName(TelinkLightApplication.getInstance())
        val pwd = SharedPreferencesHelper.getMeshPassword(TelinkLightApplication.getInstance())

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(pwd)) {
            //                Mesh mesh = (Mesh) FileSystem.readAsObject(this, name + "." + pwd);
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = phone
            mesh.password = Constant.NEW_MESH_PASSWORD
            mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
            mesh.password = Constant.DEFAULT_MESH_FACTORY_PASSWORD
            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
//            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getInstance(), phone)
//            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getInstance(), Constant.NEW_MESH_PASSWORD)
            saveToDataBase(mesh.factoryName, mesh.password, mesh.name, mesh.password)
        }else{
            saveToDataBase(Constant.DEFAULT_MESH_FACTORY_NAME, Constant.DEFAULT_MESH_FACTORY_PASSWORD,name, pwd)
            oldDataConvertToNewData(name,pwd)
            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getInstance(),null)
            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getInstance(),null)
        }
    }

    private fun saveToDataBase(factoryName: String, factoryPwd: String, mNewMeshName: String, mNewMeshPwd: String) {
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, "dadou")
        val dbRegio = DbRegion()
        dbRegio.belongAccount = account
        dbRegio.controlMesh = mNewMeshName
        dbRegio.controlMeshPwd = mNewMeshPwd
        dbRegio.installMesh = factoryName
        dbRegio.installMeshPwd = factoryPwd
        DBUtils.saveRegion(dbRegio)

        val application = getApplication() as TelinkLightApplication
        val mesh = application.mesh
        mesh.name = dbRegio.controlMesh
        mesh.password = dbRegio.controlMeshPwd
        mesh.factoryName = dbRegio.installMesh
        mesh.password = dbRegio.installMeshPwd
        mesh.saveOrUpdate(TelinkLightApplication.getInstance())
        application.setupMesh(mesh)
    }


}