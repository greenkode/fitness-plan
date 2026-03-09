package com.krachtix.util

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
object SpringBeanUtil : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(context: ApplicationContext) {
        applicationContext = context
    }

    fun <T : Any> getBean(beanClass: Class<T>): T {
        return applicationContext.getBean(beanClass)
    }

    fun getBean(beanName: String): Any {
        return applicationContext.getBean(beanName)
    }

    fun <T : Any> getBean(beanName: String, beanClass: Class<T>): T {
        return applicationContext.getBean(beanName, beanClass)
    }
}
 
