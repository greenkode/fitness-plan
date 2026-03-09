package com.krachtix.config

import com.krachtix.commons.search.DefaultBaseRepository
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.Optional
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class JpaConfiguration(
    private val tenantResolver: MerchantTenantResolver,
    @Value("\${hibernate.search.backend.type:lucene}")
    private val hibernateSearchBackendType: String,
    @Value("\${hibernate.search.backend.directory.root:./indexes}")
    private val hibernateSearchIndexPath: String,
    @Value("\${hibernate.search.backend.hosts:}")
    private val opensearchHosts: String,
    @Value("\${hibernate.search.backend.protocol:https}")
    private val opensearchProtocol: String,
    @Value("\${hibernate.search.backend.username:}")
    private val opensearchUsername: String,
    @Value("\${hibernate.search.backend.password:}")
    private val opensearchPassword: String
) {

    private fun createVendorAdapter(): HibernateJpaVendorAdapter {
        return HibernateJpaVendorAdapter().apply {
            setShowSql(false)
            setGenerateDdl(false)
        }
    }

    @Primary
    @DependsOn("flywayCoreMigrator")
    @Bean(name = ["mainEntityManagerFactory"])
    fun mainEntityManagerFactory(
        @Qualifier("coreDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val properties = hashMapOf<String, Any>(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "validate",
            "hibernate.show_sql" to false,
            "hibernate.format_sql" to false,
            "hibernate.use_sql_comments" to false,
            "hibernate.physical_naming_strategy" to "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
            "hibernate.implicit_naming_strategy" to "org.springframework.boot.hibernate.SpringImplicitNamingStrategy",
            "hibernate.search.backend.type" to hibernateSearchBackendType,
            AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER to tenantResolver,
        )

        if (hibernateSearchBackendType == "lucene") {
            properties["hibernate.search.backend.directory.root"] = hibernateSearchIndexPath
        } else if (hibernateSearchBackendType == "elasticsearch" && opensearchHosts.isNotBlank()) {
            properties["hibernate.search.backend.hosts"] = opensearchHosts
            properties["hibernate.search.backend.protocol"] = opensearchProtocol
            if (opensearchUsername.isNotBlank()) {
                properties["hibernate.search.backend.username"] = opensearchUsername
                properties["hibernate.search.backend.password"] = opensearchPassword
            }
        }

        return LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            setPackagesToScan(
                "com.krachtix.campaign",
                "com.krachtix.messaging",
                "com.krachtix.notification",
                "com.krachtix.process",
                "com.krachtix.scheduler",
                "com.krachtix.user",
                "com.krachtix.vault",
                "org.springframework.modulith.events.jpa"
            )
            setPersistenceUnitName("main")
            jpaVendorAdapter = createVendorAdapter()
            setJpaPropertyMap(properties)
        }
    }

    @Primary
    @Bean(name = ["mainTransactionManager"])
    fun mainTransactionManager(
        @Qualifier("mainEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    @Bean(name = ["readReplicaEntityManagerFactory"])
    fun readReplicaEntityManagerFactory(
        @Qualifier("coreReadReplicaDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val properties = hashMapOf<String, Any>(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "none",
            "hibernate.show_sql" to false,
            "hibernate.format_sql" to false,
            "hibernate.use_sql_comments" to false,
            "hibernate.physical_naming_strategy" to "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
            "hibernate.implicit_naming_strategy" to "org.springframework.boot.hibernate.SpringImplicitNamingStrategy",
        )

        return LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            setPackagesToScan("com.krachtix")
            setPersistenceUnitName("readReplica")
            jpaVendorAdapter = createVendorAdapter()
            setJpaPropertyMap(properties)
        }
    }

    @Bean(name = ["readReplicaTransactionManager"])
    fun readReplicaTransactionManager(
        @Qualifier("readReplicaEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = [
        "com.krachtix.campaign",
        "com.krachtix.messaging",
        "com.krachtix.notification",
        "com.krachtix.process",
        "com.krachtix.scheduler",
        "com.krachtix.user",
        "com.krachtix.vault"
    ],
    entityManagerFactoryRef = "mainEntityManagerFactory",
    transactionManagerRef = "mainTransactionManager",
    repositoryBaseClass = DefaultBaseRepository::class,
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.REGEX, pattern = [".*\\.statistics\\..*"])
    ]
)
class MainRepositoryConfiguration

@Configuration
@EnableJpaRepositories(
    basePackages = [
        "com.krachtix.campaign.statistics",
        "com.krachtix.messaging.statistics",
        "com.krachtix.notification.statistics",
        "com.krachtix.process.statistics",
        "com.krachtix.scheduler.statistics",
        "com.krachtix.user.statistics",
        "com.krachtix.vault.statistics"
    ],
    entityManagerFactoryRef = "readReplicaEntityManagerFactory",
    transactionManagerRef = "readReplicaTransactionManager",
    repositoryBaseClass = DefaultBaseRepository::class
)
class ReadReplicaRepositoryConfiguration

@Component
internal class SpringSecurityAuditorAware : AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map { it.authentication }
            .filter { it?.isAuthenticated == true }
            .map { it?.name }
    }
}
