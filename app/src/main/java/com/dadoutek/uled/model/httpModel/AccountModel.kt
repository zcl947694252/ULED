package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.dbModel.DbUser
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.SharedPreferencesUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//看起来像存放静态方法的静态类，实际上就是单例模式。
object AccountModel {
    var userPassword: String? = null
    fun login(phone: String, password: String): Observable<DbUser> {
        userPassword = password
        lateinit var account: String

        return NetworkFactory.getApi()
                .getAccount(phone, "dadou")
                .compose(NetworkTransformer())
                .flatMap { response: String ->
                    account = response
                    SharedPreferencesUtils.saveLastUser("$phone-$password-$account")
                    NetworkFactory.getApi()
                            .getsalt(response)
                            .compose(NetworkTransformer())
                }
                .flatMap { response: String ->
                    val md5Pwd = (NetworkFactory.md5(//登录接口
                            NetworkFactory.md5(NetworkFactory.md5(password) + account) + response))
                    NetworkFactory.getApi().login(account, md5Pwd,NetworkFactory.md5(password))
                            .compose(NetworkTransformer())
                }
                .observeOn(Schedulers.io())
                .doOnNext {
                    initDatBase(it)
                    Thread.sleep(2000)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun smsLoginTwo(phone: String): Observable<DbUser> {
        return NetworkFactory.getApi()
                .smsLogin(phone)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    initDatBase(it)
                    Thread.sleep(2000)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, avatar: String, name: String, email: String, introduction: String): Observable<String>? {
        return NetworkFactory.getApi()
                .updateUser(token, avatar, name, email, introduction)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())

                .doOnNext {}
                .observeOn(AndroidSchedulers.mainThread())
    }


    private fun oldDataConvertToNewData(name: String, pwd: String) {
        GlobalScope.launch {
            delay(500L)
            oldDataConvertToNewDataGroup(name, pwd)
            delay(500L)
            oldDataConvertToNewDataLight(name, pwd)
            delay(200L)

            //原有数据转化完成清空本地数据
            SharedPreferencesHelper.putObject(TelinkLightApplication.getApp(),
                    name + pwd + Constants.LIGHTS_KEY, null)
            SharedPreferencesHelper.putObject(TelinkLightApplication.getApp(),
                    name + pwd + Constants.GROUPS_KEY, null)
        }
    }

    private fun oldDataConvertToNewDataLight(name: String, pwd: String) {
        val lights = SharedPreferencesHelper.getObject(TelinkLightApplication.getApp(),
                name + pwd + Constants.LIGHTS_KEY) as? Lights

        if (lights!!.get() != null && lights.get().size > 0) {
            for (item in lights.get()) {
                var light: DbLight = DbLight()
                if (item.belongGroups.size > 0) {
                    light.belongGroupId = DBUtils.getGroupByMeshAddr(item.belongGroups.get(0)).id
                } else {
                    light.belongGroupId = 1
                }
                light.brightness = item.brightness
                light.colorTemperature = item.temperature
                light.meshAddr = item.meshAddress
                light.name = item.name
                light.macAddr = item.macAddress
                light.color = item.color
                if (item.raw != null) {
                    light.meshUUID = item.raw.meshUUID
                    light.productUUID = item.raw.productUUID
                } else {
                    light.meshUUID = 0
                    light.productUUID = 0
                }
                DBUtils.oldToNewSaveLight(light)
            }
        }
    }

    private fun oldDataConvertToNewDataGroup(name: String, pwd: String) {
        val groups = SharedPreferencesHelper.getObject(TelinkLightApplication.getApp(),
                name + pwd + Constants.GROUPS_KEY) as? Groups

        val dbRegion = DBUtils.lastRegion

        if (groups != null) {
            for (item in groups.get()) {
                var dbGroup = DbGroup()
                if (item.meshAddress == 0xffff) {
                    continue
                }
                dbGroup.belongRegionId = dbRegion.id.toInt()
                dbGroup.brightness = item.brightness
                dbGroup.colorTemperature = item.temperature
                dbGroup.meshAddr = item.meshAddress
                dbGroup.name = item.name
                dbGroup.color = item.color
                DBUtils.saveGroup(dbGroup, false)
            }
        }
    }

    fun initDatBase(user: DbUser) {
        //首先保存当前数据库名
        SharedPreferencesHelper.putString(TelinkLightApplication.getApp(), Constants.DB_NAME_KEY, user.account)

        //数据库分库
        DaoSessionInstance.destroySession()
        DaoSessionInstance.getInstance()
        DaoSessionUser.destroySession()
        DaoSessionUser.getInstance()

        if (user.last_authorizer_user_id == null)//不是切换区域就是登录
            user.last_authorizer_user_id = user.id.toString()

        DBUtils.saveUser(user)
        user.password = userPassword

        if (DBUtils.getUserPhone(user.phone) == null) {
            DBUtils.saveUserDao(user)
        }
    }

/*
    private fun setupMesh(account: String) {

        val regionList = DBUtils.regionAll

        //数据库有区域数据直接加载
        if (regionList.size != 0) {
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegionId()
            val dbRegion = DBUtils.lastRegion
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = dbRegion.controlMesh
            mesh.password = dbRegion.controlMeshPwd
            mesh.factoryName = dbRegion.installMesh
            mesh.password = dbRegion.installMeshPwd
//            mesh.saveOrUpdate(TelinkLightApplication.getApp())
            application.setupMesh(mesh)
            return
        }

        //数据库无区域数据（本地拿取，本地没有就给默认，本地有则同步本地到数据库然后清除本地）
        val name = SharedPreferencesHelper.getMeshName(TelinkLightApplication.getApp())
        val pwd = SharedPreferencesHelper.getMeshPassword(TelinkLightApplication.getApp())

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(pwd)) {
            //                Mesh mesh = (Mesh) FileSystem.readAsObject(this, name + "." + pwd);
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = account
            mesh.password = account
            mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
            mesh.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD
//            mesh.saveOrUpdate(TelinkLightApplication.getApp())
            application.setupMesh(mesh)
//            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getApp(), phone)
//            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getApp(), Constant.NEW_MESH_PASSWORD)
            saveToDataBase(mesh.factoryName!!, mesh.factoryPassword!!, mesh.name!!, mesh.password!!)
            SharedPreferencesHelper.putString(TelinkLightApplication.getApp(),
                    Constant.USER_TYPE, Constant.USER_TYPE_NEW)
        } else {
            saveToDataBase(Constant.DEFAULT_MESH_FACTORY_NAME, Constant.DEFAULT_MESH_FACTORY_PASSWORD, name, pwd)
            oldDataConvertToNewData(name, pwd)
            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getApp(), null)
            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getApp(), null)
            SharedPreferencesHelper.putString(TelinkLightApplication.getApp(),
                    Constant.USER_TYPE, Constant.USER_TYPE_OLD)
        }
    }
*/

/*
    private fun saveToDataBase(factoryName: String, factoryPwd: String, mNewMeshName: String, mNewMeshPwd: String) {
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getApp(), Constant.DB_NAME_KEY, "dadou")
        val dbRegio = DbRegion()
        dbRegio.belongAccount = account
        dbRegio.controlMesh = mNewMeshName
        dbRegio.controlMeshPwd = mNewMeshPwd
        dbRegio.installMesh = factoryName
        dbRegio.installMeshPwd = factoryPwd
        DBUtils.saveRegion(dbRegio, false)

        val application = getApplication() as TelinkLightApplication
        val mesh = application.mesh
        mesh.name = dbRegio.controlMesh
       // mesh.password = dbRegio.controlMeshPwd
        mesh.password = account
        mesh.factoryName = dbRegio.installMesh
        mesh.factoryPassword = dbRegio.installMeshPwd
//        mesh.saveOrUpdate(TelinkLightApplication.getApp())
        application.setupMesh(mesh)

//        DBUtils.createAllLightControllerGroup()
    }
*/
}