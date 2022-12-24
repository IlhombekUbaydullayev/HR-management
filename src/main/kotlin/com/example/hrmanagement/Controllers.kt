package com.example.hrmanagement

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.function.RequestPredicates.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private var service: AuthService
) {
    @PostMapping("login")
    fun login(@Valid @RequestBody dto: LoginDTO): HttpEntity<Any> {
        val login = service.login(dto)
        return ResponseEntity.status(if (login.success) 200 else 409).body(login)
    }

    @GetMapping("verifyEmail")
    fun verifyEmail(@RequestParam emailCode: String, @RequestParam email: String,response: HttpServletResponse): String {
        return "login"
    }

    @PostMapping("verifyEmail")
    fun verifyEmail(
        @RequestParam email: String,
        @RequestParam emailCode: String,
        @RequestParam company : String,
        @RequestBody dto: VerifyDTO
    ): HttpEntity<Any> {
        val apiResponse: ApiResponsess = service.verifyEmail(email, emailCode,company,dto)
        return ResponseEntity.status(if (apiResponse.success) 200 else 409).body(apiResponse)
    }
}

@RestController
@RequestMapping("/api/v1/auth")
class CompanyController(
    private var service: CompanyService
) {
    @Operation(summary = "Create", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = CompanyDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @PostMapping("company")
    fun add(@Validated @RequestBody dto: CompanyDto) = service.add(dto)

    @Operation(summary = "Update", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = CompanyDtoUpdate::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @PutMapping("company/{id}")
    fun update(@PathVariable id : Long,@RequestBody companyDtoUpdate: CompanyDtoUpdate) = service.update(id,companyDtoUpdate)

    @Operation(summary = "Get all", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = CompanyResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @GetMapping("getAll")
    fun getAll() = service.getAll()

    @Operation(summary = "Get by id", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = CompanyResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @GetMapping("get/{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @Operation(summary = "Delete", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a company does not exist"),
        ]
    )
    @DeleteMapping("delete/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

}

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private var service: UserService
){
    @Operation(summary = "Create", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a user does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @PostMapping("workers")
    fun create(@RequestBody dto: UserDto,request : HttpServletRequest): HttpEntity<Any> {
        val userAdd = service.add(dto,request)
        return ResponseEntity.status(if (userAdd.success) 200 else 409).body(userAdd)
    }

    @Operation(summary = "Get all task users", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = UserResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a user does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER')")
    @GetMapping("getAll/tasks")
    fun getAll(request: HttpServletRequest) = service.getAll(request)

    @Operation(summary = "Delete",security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a user does not exist",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @Operation(summary = "Update",security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a user does not exist",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @PutMapping("{id}")
    fun update(@PathVariable id: Long,@RequestBody dto : CompanyUserUpdateDto) = service.update(id,dto)

    @Operation(summary = "Get by id",security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a user does not exist",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)


}

@RestController
@RequestMapping("/api/v1/task")
class TaskController(
    private var service: TaskService
){

    @Operation(summary = "Create",security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER')")
    @PostMapping("create")
    fun create(@Validated @RequestBody dto: TaskCreateDto,request : HttpServletRequest) = service.create(dto,request)

    @Operation(summary = "Get all", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = UserResponseDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist"),
        ]
    )

    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER','ROLE_USER')")
    @GetMapping("getAll")
    fun getAll() = service.getAll()

    @GetMapping("get/api")
    fun getApi(
        @RequestParam name : String,
        @RequestParam description : String,
        @RequestParam lifetime : String,
        @RequestParam responsible : String,
        @RequestParam user_id : String,
        @RequestParam random2 : Long
    ) = service.getApi(name,description,lifetime,responsible,user_id,random2)

    @Operation(summary = "Send Task", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist"),
        ]
    )

    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER','ROLE_USER')")
    @PostMapping("send/{id}")
    fun send(
        @PathVariable id : Long,@RequestBody dto : TaskDto,request: HttpServletRequest
    ) = service.send(id,dto,request)


    @Operation(summary = "Task getbyId", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER','ROLE_USER')")
    @GetMapping("{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @Operation(summary = "Update", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR','ROLE_MANAGER')")
    @PutMapping("update/{id}")
    fun update(
        @PathVariable id : Long,@RequestBody dto : TaskUpdateDto,request: HttpServletRequest
    ) = service.update(id,dto,request)

    @Operation(summary = "Update", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = BaseMessage::class))]),
            ApiResponse(responseCode = "400", description = "Such a task does not exist"),
        ]
    )
    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @DeleteMapping("delete/{id}")
    fun delete(@PathVariable id : Long) = service.delete(id)
}

@RestController
@RequestMapping("api/v1/salary")
class SalaryController(
    private var service: SalaryService
){

    @Operation(summary = "Create salary", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = SalaryDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a salary does not exist"),
        ]
    )

    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @PostMapping("create")
    fun create(@RequestBody dto: SalaryDto) = service.create(dto)

    @Operation(summary = "get salary", security = [SecurityRequirement(name = "Bearer Authentication")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful Operation",content = [Content(mediaType = "application/json", schema = Schema(implementation = SalaryDto::class))]),
            ApiResponse(responseCode = "400", description = "Such a salary does not exist"),
        ]
    )

    @PreAuthorize("hasAnyRole('ROLE_HR_MANAGER','ROLE_DIRECTOR')")
    @GetMapping("getBy/{id}")
    fun getByApi(@PathVariable id: Long,request: HttpServletRequest,page : CustomPage) = service.getByApi(id,request,PageRequest.of(page.page,page.size),page.sort,page)

}
