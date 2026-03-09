package com.krachtix.notification.dto

import java.io.Serializable

data class NotificationRecipient(val address: String, val name: String) : Serializable
