package com.dadoutek.uled.stomp.model

import java.io.Serializable

data class QrCodeTopicMsg(var code: String,
                          var ref_user_phone: String,
                          var region_name: String,
                          var rid: Int,
                          var type: Int) :Serializable

