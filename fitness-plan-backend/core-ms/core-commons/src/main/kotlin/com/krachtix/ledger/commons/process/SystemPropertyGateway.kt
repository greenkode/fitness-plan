package com.krachtix.commons.process

import com.krachtix.commons.property.SystemPropertyName
import com.krachtix.commons.property.SystemPropertyScope

interface SystemPropertyGateway {

    fun findByNameAndScope(name: SystemPropertyName, scope: SystemPropertyScope): SystemPropertyDto?
    fun update(name: SystemPropertyName, scope: SystemPropertyScope, value: String)
}