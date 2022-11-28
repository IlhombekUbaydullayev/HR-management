package com.example.hrmanagement

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;



//@Configuration
//@OpenAPIDefinition(info = Info(title = "My API", version = "v1"))
//@io.swagger.v3.oas.annotations.security.SecurityScheme(
//    name = "bearerAuth",
//    type = SecuritySchemeType.HTTP,
//    bearerFormat = "JWT",
//    scheme = "bearer"
//)
//class OpenApiConfig(
//) {
//    @Bean
//    fun customOpenAPI(): OpenAPI {
//        val securitySchemeName = "bearerAuth"
//        val apiTitle = "My Api"
//        return OpenAPI()
//            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
//            .components(
//                Components()
//                        .addSecuritySchemes(
//                        securitySchemeName,
//                        SecurityScheme()
//                            .name(securitySchemeName)
//                            .type(SecurityScheme.Type.HTTP)
//                            .scheme("bearer")
//                            .bearerFormat("JWT")
//                    )
//            )
//            .info(io.swagger.v3.oas.models.info.Info().title(apiTitle).version("v1"))
//    }
//}

