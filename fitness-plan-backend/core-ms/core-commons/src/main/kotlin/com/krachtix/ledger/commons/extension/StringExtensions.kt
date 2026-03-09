package com.krachtix.extension

fun String.removeLineBreaks(): String {
    return this.replace(Regex("\\n|\\r\\n"), "")
}