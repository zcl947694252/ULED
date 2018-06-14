package com.dadoutek.uled.model.HttpModel

import android.text.TextUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
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
        return NetworkFactory.getApi()
                .getAccount(phone, channel)
                .compose(NetworkTransformer())
                .flatMap { response: String ->
                    account = response
                    NetworkFactory.getApi()
                            .getsalt(response)
                            .compose(NetworkTransformer())

                }
                .flatMap { response: String ->
                    val salt = response
                    val md5Pwd = NetworkFactory.md5(
                            NetworkFactory.md5(
                                    NetworkFactory.md5(password) + account) + salt)
                    NetworkFactory.getApi().login(account, md5Pwd)
                            .compose(NetworkTransformer())
                }
                .observeOn(Schedulers.io())
                .doOnNext {
                    initDatBase(it)
                    syncIsNewUser(it.token,it.account,it)
                    Thread.sleep(2000)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(token: String, avatar: String, name: String, email: String, introduction: String): Observable<String>? {
        return NetworkFactory.getApi()
                .updateUser(token, avatar, name, email, introduction)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    private fun oldDataConvertToNewData(name: String, pwd: String) {
        launch(CommonPool) {
            delay(500L)
            oldDataConvertToNewDataGroup(name, pwd)
            delay(500L)
            oldDataConvertToNewDataLight(name, pwd)
            delay(200L)

            //原有数据转化完成清空本地数据
            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                    name + pwd + Constant.LIGHTS_KEY, null)
            SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                    name + pwd + Constant.GROUPS_KEY, null)
        }
    }

    private fun oldDataConvertToNewDataLight(name: String, pwd: String) {
        val lights = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                name + pwd + Constant.LIGHTS_KEY) as? Lights

        if (lights!!.get() != null && lights.get().size > 0) {
            for (item in lights.get()) {
                var light: DbLight = DbLight()
                if (item.belongGroups.size > 0) {
                    light.belongGroupId = DBUtils.getGroupByMesh(item.belongGroups.get(0)).id
                } else {
                    light.belongGroupId = 1
                }
                light.brightness = item.brightness
                light.colorTemperature = item.temperature
                light.meshAddr = item.meshAddress
                light.name = item.name
                light.macAddr = item.macAddress
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
        val groups = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                name + pwd + Constant.GROUPS_KEY) as? Groups

        val dbRegion = DBUtils.getLastRegion()

        if (groups != null) {
            for (item in groups.get()) {
                var dbGroup: DbGroup = DbGroup()
                if (item.meshAddress == 0xffff) {
                    continue
                }
                dbGroup.belongRegionId = dbRegion.id.toInt()
                dbGroup.brightness = item.brightness
                dbGroup.colorTemperature = item.temperature
                dbGroup.meshAddr = item.meshAddress
                dbGroup.name = item.name
                DBUtils.saveGroup(dbGroup, false)
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
    }

    private fun setIsLogin(isLogin: Boolean) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, isLogin)
    }

    private fun setupMesh(account: String) {

        val regionList = DBUtils.getRegionAll()

        //数据库有区域数据直接加载
        if (regionList.size != 0) {
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegion()
            val dbRegion = DBUtils.getLastRegion()
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = dbRegion.controlMesh
            mesh.password = dbRegion.controlMeshPwd
            mesh.factoryName = dbRegion.installMesh
            mesh.password = dbRegion.installMeshPwd
//            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
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
            mesh.name = account
            mesh.password = account
            mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
            mesh.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD
//            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
//            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getInstance(), phone)
//            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getInstance(), Constant.NEW_MESH_PASSWORD)
            saveToDataBase(mesh.factoryName, mesh.password, mesh.name, mesh.password)
            SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                    Constant.USER_TYPE, Constant.USER_TYPE_NEW)
        } else {
            saveToDataBase(Constant.DEFAULT_MESH_FACTORY_NAME, Constant.DEFAULT_MESH_FACTORY_PASSWORD, name, pwd)
            oldDataConvertToNewData(name, pwd)
            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getInstance(), null)
            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getInstance(), null)
            SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                    Constant.USER_TYPE, Constant.USER_TYPE_OLD)
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
        DBUtils.saveRegion(dbRegio, false)

        val application = getApplication() as TelinkLightApplication
        val mesh = application.mesh
        mesh.name = dbRegio.controlMesh
        mesh.password = dbRegio.controlMeshPwd
        mesh.factoryName = dbRegio.installMesh
        mesh.password = dbRegio.installMeshPwd
//        mesh.saveOrUpdate(TelinkLightApplication.getInstance())
        application.setupMesh(mesh)

//        DBUtils.createAllLightControllerGroup()
    }


    private fun syncIsNewUser(token: String,account: String,user: DbUser){
        setIsLogin(true)
        setupMesh(account)

//        NetworkFactory.getApi()
//                .getRegionList(token)
//                .compose(NetworkTransformer())
//                .observeOn(Schedulers.io())
//                .doOnNext {
//                }
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(object : NetworkObserver<List<DbRegion>>() {
//                    override fun onNext(t: List<DbRegion>) {
//                        //非首次在当前手机登录或者是首次注册新用户在此手机登录加载数据，如果是老用户更换设备登录需在登录成功后拉取服务器数据
//                        if (t.size == 0 || SharedPreferencesUtils.getCurrentUserList().contains(account)) {
//                                setIsLogin(true)
//                                setupMesh(account)
//                        }
//                    }
//
//                    override fun onError(e: Throwable) {
//                        super.onError(e)
//                    }
//                })
    }
}