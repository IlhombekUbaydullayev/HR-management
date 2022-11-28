package com.example.hrmanagement

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity


abstract class ProjectException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorType

    fun toBaseMessage(messageSource: ResourceBundleMessageSource): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(
            BaseMessage(
                errorType().code, messageSource.getMessage(
                    errorType().name, null, LocaleContextHolder.getLocale()
                )
            )
        )
    }
}

class ObjectNotFoundException() : ProjectException() {
    override fun errorType() = ErrorType.OBJECT_NOT_FOUND

}

class EmailException() : ProjectException() {
    override fun errorType() = ErrorType.EMAIL_NOT_FOUND
}

class AlreadyReportedException() : ProjectException() {
    override fun errorType() = ErrorType.ALREADY_REPORTED
}