package com.krachtix.identity.config

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.krachtix.identity"])
@EntityScan(basePackages = ["com.krachtix.identity"])
class AppConfig