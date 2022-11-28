package com.example.hrmanagement

import com.fasterxml.jackson.annotation.*
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*
import javax.persistence.Column
import javax.validation.constraints.*


data class ApiResponsess(
    var message : String,
    var success : Boolean,
    var objects: Any? = null
)

data class AuthDto(
    var name : String
)

@Schema(description = "This is company class")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CompanyDto(
    @Column(nullable = false)
    @get:Schema(description = "First line of the company.", example = "Samsung")
    @field:NotNull(message = "name must not be empty")
    @get:Size(min = 3, max = 20)
    var name : String?,
    @Column(nullable = false)
    @get:Schema(description = "Line company director fullName.", example = "John Doe")
    @field:NotNull(message = "fullName must not be empty")
    var full_name : String?,
    @get:Email
    @get:Schema(description = "Line company director email.", example = "johndoe63@gmail.com")
    @field:NotNull(message = "email must not be empty")
    @Column(unique = true, nullable = false)
    var email : String?
)
{
    companion object{
        fun toDto(c:Company) = c.run {
            CompanyDto(name,"","")
        }
    }
}

data class CompanyDtoUpdate(
    var name : String? = null,
    var ownerId : Long? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CompanyResponseDto(
    var id : Long,
    var name : String,
    var owner : UserDto
){
    companion object{
        fun toDto(c : Company) = c.run {
            CompanyResponseDto(id!!,name,UserDto.toDto(owner!!))
        }
    }
}
data class LoginDTO (
    @field:NotNull
    val email: String,
    @field:NotNull
    val password: String,
)

data class UserDto(
    var id : Long,
    var full_name : String?,
    @get:Email
    @Column(unique = true, nullable = false)
    var email : String,
    var systemRoleName: CompanyRoleName? = CompanyRoleName.ROLE_USER
)
{
    companion object{
        fun toDto(u : User) = u.run {
            UserDto(id!!,fullName,email,systemRoleName)
        }
    }
}

data class VerifyDTO (
    @field:NotNull
    val password: String
)