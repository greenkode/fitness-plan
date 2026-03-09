package com.krachtix.commons.process

import com.krachtix.commons.property.SystemPropertyName
import com.krachtix.commons.property.SystemPropertyScope


data class SystemPropertyDto(val id: Int, val name: SystemPropertyName, val scope: SystemPropertyScope, val value: String)