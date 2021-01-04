package com.dadoutek.uled.router.bean

import java.io.Serializable


/**
 * 创建者     ZCL
 * 创建时间   2020/9/9 11:32
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
 class RouteSceneBean(
    val cmd: Int,
    val finish: Boolean,
    val msg: String,
    val scene: Scene,
    val ser_id: String,
    val status: Int
):Serializable{
    override fun toString(): String {
        return "RouteSceneBean(cmd=$cmd, finish=$finish, msg='$msg', scene=$scene, ser_id='$ser_id', status=$status)"
    }
}

data class Scene(
    val actions: List<Action>,
    val belongRegionId: Int,
    val id: Int,
    val imgName: String,
    val index: Int,
    val name: String,
    val uid: Int
):Serializable{
    override fun toString(): String {
        return "Scene(actions=$actions, belongRegionId=$belongRegionId, id=$id, imgName='$imgName', index=$index, name='$name', uid=$uid)"
    }
}

data class Action(
    val belongSceneId: Int,
    val brightness: Int,
    val circleFour: Int,
    val circleOne: Int,
    val circleThree: Int,
    val circleTwo: Int,
    val color: Int,
    val colorTemperature: Int,
    val deviceType: Int,
    val groupAddr: Int,
    val id: Int,
    val isOn: Boolean
):Serializable{
    override fun toString(): String {
        return "Action(belongSceneId=$belongSceneId, brightness=$brightness, circleFour=$circleFour, circleOne=$circleOne, circleThree=$circleThree, circleTwo=$circleTwo, color=$color, colorTemperature=$colorTemperature, deviceType=$deviceType, groupAddr=$groupAddr, id=$id, isOn=$isOn)"
    }
}