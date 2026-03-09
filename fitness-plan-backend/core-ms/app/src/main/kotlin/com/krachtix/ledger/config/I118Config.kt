package com.krachtix.config

import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@Configuration
class I118Config {

    private val log = LoggerFactory.getLogger(I118Config::class.java)

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        val baseNames = mutableSetOf<String>()

        // Dynamically scan for all messages-*.properties files on the classpath
        val resolver = PathMatchingResourcePatternResolver()
        try {
            val resources = resolver.getResources("classpath*:messages-*.properties")
            log.info("Found ${resources.size} message resource files")

            resources.forEach { resource ->
                resource.filename?.let { filename ->
                    val baseName = filename.removeSuffix(".properties")
                    // Remove locale suffix if present (e.g., messages-commons_en -> messages-commons)
                    val baseNameWithoutLocale = baseName.replace(Regex("_[a-z]{2}(_[A-Z]{2})?$"), "")
                    baseNames.add("classpath:$baseNameWithoutLocale")
                    log.info("Added message basename: classpath:$baseNameWithoutLocale from $filename")
                }
            }
        } catch (e: Exception) {
            log.warn("Failed to scan for message files: ${e.message}")
        }

        // Fallback: explicitly add known message basenames if scan found nothing
        if (baseNames.isEmpty()) {
            log.warn("No message files found via scanning, adding fallback basenames")
            baseNames.add("classpath:messages-commons")
        }

        messageSource.setBasenames(*baseNames.toTypedArray())
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setFallbackToSystemLocale(false)
        messageSource.setUseCodeAsDefaultMessage(true)

        log.info("Configured MessageSource with basenames: $baseNames")
        return messageSource
    }

    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = AcceptHeaderLocaleResolver()
        resolver.setDefaultLocale(Locale.ENGLISH)
        resolver.supportedLocales = listOf(Locale.ENGLISH, Locale.US, Locale.UK)
        return resolver
    }
}
