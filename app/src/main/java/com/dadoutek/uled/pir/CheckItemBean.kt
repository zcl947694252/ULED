package com.dadoutek.uled.pir

import android.os.Parcel
import android.os.Parcelable


/**
 * 创建者     ZCL
 * 创建时间   2020/5/19 17:32
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class CheckItemBean(var id: Long, var name: String, var checked: Boolean,var imgName: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString().toString(),
            parcel.readByte() != 0.toByte(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeByte(if (checked) 1 else 0)
        parcel.writeString(imgName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CheckItemBean> {
        override fun createFromParcel(parcel: Parcel): CheckItemBean {
            return CheckItemBean(parcel)
        }

        override fun newArray(size: Int): Array<CheckItemBean?> {
            return arrayOfNulls(size)
        }
    }

}