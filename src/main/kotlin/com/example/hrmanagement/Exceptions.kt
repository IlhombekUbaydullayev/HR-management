package com.example.hrmanagement

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity


abstract class ProjectException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorType
    abstract fun errorParams(): Array<Any>?
    fun toBaseMessage(messageSource: ResourceBundleMessageSource): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(
            BaseMessage(
                errorType().code, messageSource.getMessage(
                    errorType().name, errorParams(), LocaleContextHolder.getLocale()
                )
            )
        )
    }
}

class ObjectNotFoundException(private var objName: String, private var identifier: Any) : ProjectException() {
    override fun errorType() = ErrorType.OBJECT_NOT_FOUND
    override fun errorParams(): Array<Any>? {
        return  arrayOf(objName, identifier)
    }
}

class EmailException(private var objName: String?, private var identifier: Any?) : ProjectException() {
    override fun errorType() = ErrorType.EMAIL_NOT_FOUND
    override fun errorParams(): Array<Any>? {
        return arrayOf()
    }
}

class AlreadyReportedException(private var objName: String, private var identifier: Any) : ProjectException() {
    override fun errorType() = ErrorType.ALREADY_REPORTED
    override fun errorParams(): Array<Any>? {
        return arrayOf(objName,identifier)
    }
}