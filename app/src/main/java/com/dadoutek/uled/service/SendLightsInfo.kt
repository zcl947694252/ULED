package com.dadoutek.uled.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

import com.dadoutek.uled.model.Light
import com.telink.util.Event
import com.telink.util.EventListener

/**
 * Created by hejiajun on 2018/3/22.
 */

class SendLightsInfo : Service(), EventListener<String> {

    internal var light: Light? = null
    private val bundle: Bundle? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        //        bundle=intent.getExtras().getBundle("");
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun performed(event: Event<String>) {

    }
}
