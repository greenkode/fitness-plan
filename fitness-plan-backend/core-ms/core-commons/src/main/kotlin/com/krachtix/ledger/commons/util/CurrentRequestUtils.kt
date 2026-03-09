package com.krachtix.commons.util

import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.RequestContextAttributeName
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.RequestContextHolder

class CurrentRequestUtils {

    companion object {
        fun getChannel() = RequestContextHolder.getRequestAttributes()
            ?.getAttribute(RequestContextAttributeName.CHANNEL.name, SCOPE_REQUEST)?.let { it as ProcessChannel }
            ?: ProcessChannel.SYSTEM
    }
}