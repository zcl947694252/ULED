package com.dadoutek.uled.stomp.model

import java.io.Serializable

data class QrCodeTopicMsg(val ref_user_phone: String, val type: Int, val account: String) :Serializable