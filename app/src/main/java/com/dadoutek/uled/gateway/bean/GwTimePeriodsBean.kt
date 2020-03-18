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
//private  class GwTimePeriodsBean(private val hour: Int, private val minute: Int)
internal open class GwTimePeriodsBean : Parcelable {
     var hour: Int = 0
     var minute: Int = 0
     var sceneId: Long = 0
     var sceneName: String = ""

    constructor(hour: Int, minute: Int, sceneName:String) {
        this.hour = hour
        this.minute = minute
        this.sceneName = sceneName
    }

    protected constructor(`in`: Parcel) {
        hour = `in`.readInt()
        minute = `in`.readInt()
    }



    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(hour)
        dest.writeInt(minute)
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