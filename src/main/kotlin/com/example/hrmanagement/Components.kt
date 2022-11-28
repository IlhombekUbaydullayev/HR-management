package com.example.hrmanagement

import org.springframework.context.annotation.Bean
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import java.util.*


@Component
class Components {
    @Bean
    fun messageResourceBundleMessageSource(): ResourceBundleMessageSource? {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasename("errors")
        messageSource.setCacheSeconds(3600)
        messageSource.setDefaultLocale(Locale.US)
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }
}