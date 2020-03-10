package com.dadoutek.uled.gateway.bean

import android.os.Parcel
import android.os.Parcelable


/**
 * 创建者     ZCL
 * 创建时间   2020/3/9 14:24
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class WeekBean(var week: String,var pos: Int,var checked:Boolean = false) :Parcelable{
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(week)
        parcel.writeInt(pos)
        parcel.writeByte(if (checked) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WeekBean> {
        override fun createFromParcel(parcel: Parcel): WeekBean {
            return WeekBean(parcel)
        }

        override fun newArray(size: Int): Array<WeekBean?> {
            return arrayOfNulls(size)
        }
    }
}