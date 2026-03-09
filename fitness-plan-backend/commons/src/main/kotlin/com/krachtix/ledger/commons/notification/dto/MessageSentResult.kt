package com.krachtix.commons.notification.dto

import com.krachtix.commons.notification.enumeration.DeliveryStatus


data class MessageSentResult(val status: DeliveryStatus, val response: String? = null, val sentCode: String? = null)