package com.example.hrmanagement

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition
@SpringBootApplication
class HrManagementApplication

fun main(args: Array<String>) {
    runApplication<HrManagementApplication>(*args)
}
