package com.example.hrmanagement

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.function.RequestPredicates.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("api/v1/auth")
class AuthController(
    private var authService: AuthService
) {
    @PostMapping("login")
    fun login(@Valid @RequestBody loginDTO: LoginDTO): HttpEntity<Any> {
        val login = authService.login(loginDTO)
        return ResponseEntity.status(if (login.success) 200 else 409).body(login)
    }

    @GetMapping("verifyEmail")
    fun verifyEmail(@RequestParam emailCode: String, @RequestParam email: String): HttpEntity<Any> {
        val apiResponse: ApiResponsess = authService.getVerifyEmail(emailCode, email)
        return ResponseEntity.status(if (apiResponse.success) 200 else 409).body(apiResponse)
    }

    @PostMapping("verifyEmail")
    fun verifyEmail(
        @RequestParam email: String,
        @RequestParam emailCode: String,
        @RequestBody verifyDTO: VerifyDTO
    ): HttpEntity<Any> {
        val apiResponse: ApiResponsess = authService.verifyEmail(email, emailCode, verifyDTO)
        return ResponseEntity.status(if (apiResponse.success) 200 else 409).body(apiResponse)
    }
}

@RestController
@RequestMapping("api/v1/auth")
class CompanyController(
    private var companyService: CompanyService
) {
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = CompanyDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @PostMapping("company")
    fun addCompany(@Validated @RequestBody companyDto: CompanyDto) = companyService.addCompany(companyDto)
    @PutMapping("company/{id}")
    fun update(@PathVariable id : Long,@RequestBody companyDtoUpdate: CompanyDtoUpdate) = companyService.update(id,companyDtoUpdate)

    @GetMapping("getAll")
    fun getAll() = companyService.getAll()
}

@RestController
@RequestMapping("api/v1/auth")
class UserController(
    private var userService: UserService
){
    @Operation(summary = "Create", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @PostMapping("workers")
    fun create(@RequestBody userDto: UserDto,request : HttpServletRequest): HttpEntity<Any> {
        val userAdd = userService.addUser(userDto,request)
        return ResponseEntity.status(if (userAdd.success) 200 else 409).body(userAdd)
    }
}