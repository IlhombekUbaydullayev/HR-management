package com.example.hrmanagement

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.*
import java.util.function.Consumer

@ControllerAdvice
class GlobalExceptionHandler(private val messageSource: ResourceBundleMessageSource) : ResponseEntityExceptionHandler() {
    @ExceptionHandler(value = [ProjectException::class])
    fun handleHrException(e: ProjectException)=e.toBaseMessage(messageSource)


    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {

        val body: MutableMap<String, Any> = LinkedHashMap()
        body["code"] = status.value()
        val errors: MutableList<String?> = LinkedList()
        ex.bindingResult.fieldErrors.forEach(Consumer { error: FieldError ->
            errors.add(
                error.defaultMessage
            )
        })
        body["error"] = errors
        return ResponseEntity(body, headers, status)
    }


}


