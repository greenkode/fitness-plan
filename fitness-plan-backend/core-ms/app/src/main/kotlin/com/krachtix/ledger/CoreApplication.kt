package com.krachtix

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.TimeZone


@EnableAsync
@EnableResilientMethods
@SpringBootApplication(scanBasePackages = ["com.krachtix"])
@EnableTransactionManagement
@EnableConfigurationProperties
@ConfigurationPropertiesScan
class CoreApplication

fun main(args: Array<String>) {

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<CoreApplication>(*args)
}
