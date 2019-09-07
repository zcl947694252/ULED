package com.dadoutek.uled.communicate

import com.blankj.utilcode.util.LogUtils
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class CommanderTest()
{
    @Test
    fun test() {
        val oldVersion = "3.4.0"
        val newVersion = "9"

//        val ret = newVersion > oldVersion
        assert(newVersion < oldVersion)

    }
}