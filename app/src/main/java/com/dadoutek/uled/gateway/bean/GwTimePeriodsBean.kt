package com.dadoutek.uled.gateway.bean

import android.os.Parcel
import android.os.Parcelable


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 17:50
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
//private  class GwTimePeriodsBean(private val startTime: Int, private val endTime: Int)
open class GwTimePeriodsBean() : Parcelable {
     var index: Int = 0
     var startTime: Int = 0
     var endTime: Int = 0
     var sceneId: Long = 0
     var sceneName: String = ""
    var standingTime: Int = 0

    constructor(parcel: Parcel) : this() {
        index = parcel.readInt()
        startTime = parcel.readInt()
        endTime = parcel.readInt()
        sceneId = parcel.readLong()
        sceneName = parcel.readString()
        standingTime = parcel.readInt()
    }

    constructor(index: Int,startTime: Int, endTime: Int,sceneName:String) : this() {
        this.index = index
        this.startTime = startTime
        this.endTime = endTime
        this.sceneName = sceneName
    }



    override fun toString(): String {
        return "GwTimePeriodsBean(index=$index, startTime=$startTime, endTime=$endTime, sceneId=$sceneId, sceneName='$sceneName', standingTime=$standingTime)"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
        parcel.writeInt(startTime)
        parcel.writeInt(endTime)
        parcel.writeLong(sceneId)
        parcel.writeString(sceneName)
        parcel.writeInt(standingTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GwTimePeriodsBean> {
        override fun createFromParcel(parcel: Parcel): GwTimePeriodsBean {
            return GwTimePeriodsBean(parcel)
        }

        override fun newArray(size: Int): Array<GwTimePeriodsBean?> {
            return arrayOfNulls(size)
        }
    }


}