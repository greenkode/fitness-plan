package com.krachtix.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication(scanBasePackages = ["com.krachtix.identity", "com.krachtix.commons"])
class IdentityMsApplication

fun main(args: Array<String>) {
	runApplication<IdentityMsApplication>(*args)
}
