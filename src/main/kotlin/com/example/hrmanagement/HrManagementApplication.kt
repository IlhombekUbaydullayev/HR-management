package com.example.hrmanagement

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
//    tags = [
//        Tag(name="widget", description="Widget operations."),
//        Tag(name="gasket", description="Operations related to gaskets")
//    ],
//    info = Info(title="Example API",
//        version = "1.0.1",
//        contact = Contact(
//            name = "Example API Support",
//            url = "http://exampleurl.com/contact",
//            email = "techsupport@example.com"),
//        license = License(
//            name = "Apache 2.0",
//            url = "https://www.apache.org/licenses/LICENSE-2.0.html"))
)
@SpringBootApplication
class HrManagementApplication

fun main(args: Array<String>) {
    runApplication<HrManagementApplication>(*args)
}
