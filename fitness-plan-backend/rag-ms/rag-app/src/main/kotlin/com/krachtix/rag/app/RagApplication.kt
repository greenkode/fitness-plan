package com.krachtix.rag.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.krachtix.rag"])
class RagApplication

fun main(args: Array<String>) {
    runApplication<RagApplication>(*args)
}
