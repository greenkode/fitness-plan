package com.krachtix.util

import org.apache.commons.lang3.StringUtils

fun hasAtLeastOneValue(vararg strings: String?): Boolean {
    return strings.any { StringUtils.isNotEmpty(it) }
}
