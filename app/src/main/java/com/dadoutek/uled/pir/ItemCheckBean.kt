package com.dadoutek.uled.pir

import android.os.Parcel
import android.os.Parcelable

class ItemCheckBean(var title : String, var checked: Boolean):Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString().toString(),
            parcel.readByte() != 0.toByte()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeByte(if (checked) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemCheckBean> {
        override fun createFromParcel(parcel: Parcel): ItemCheckBean {
            return ItemCheckBean(parcel)
        }

        override fun newArray(size: Int): Array<ItemCheckBean?> {
            return arrayOfNulls(size)
        }
    }

}
