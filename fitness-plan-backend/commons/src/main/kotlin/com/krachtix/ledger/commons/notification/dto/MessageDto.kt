package com.krachtix.commons.notification.dto

import com.krachtix.commons.notification.enumeration.DeliveryStatus
import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority
import com.krachtix.commons.notification.enumeration.TemplateName

import java.time.Instant
import java.util.Locale

data class MessageDto(val channel: MessageChannel,
                      val template: TemplateName,
                      val deliveryStatus: DeliveryStatus,
                      val integrator: String,
                      val priority: MessagePriority,
                      val clientIdentifier: String,
                      val locale: Locale,
                      val status: String?,
                      val deliveryDate: Instant?,)