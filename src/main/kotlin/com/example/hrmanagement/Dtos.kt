package com.example.hrmanagement

import com.fasterxml.jackson.annotation.*
import io.swagger.v3.oas.annotations.media.Schema
import java.sql.Timestamp
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
    var id: Long,
    var name: String,
    var owner: UserDto
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
        fun toDto(u: User) = u.run {
            UserDto(id!!,fullName,email,systemRoleName)
        }
    }
}

data class UserDtoSec(
    var id : Long,
    var full_name : String?,
    @get:Email
    @Column(unique = true, nullable = false)
    var email : String,
    var systemRoleName: CompanyRoleName? = CompanyRoleName.ROLE_USER,
    var companyUserId: Long? = null
)
{
    companion object {
        fun toDto(u:User) = u.run {
            UserDtoSec(id!!,fullName,email,systemRoleName)
        }
    }
}

data class CompanyUserDto(
    var companyUserId: Long?,
    var name : String,
    var user : UserDtoSec
)
{
    companion object{
        fun toDto(c:CompanyUser) = c.run {
            CompanyUserDto(id,workspace!!.name,UserDtoSec.toDto(user!!))
        }
    }
}

data class VerifyDTO (
    @field:NotNull(message = "password must not be empty")
    val password: String
)

data class TaskCreateDto(
    @field:NotNull(message = "name must not be empty")
    @field:Size(min = 3)
    var name: String,
    var description: String?,
    @field:NotNull(message = "date must not be empty")
    var lifetime: Date,
    var userId : Set<Long>,
    var status: ProjectStatus = ProjectStatus.TODO,
)

data class UserResponseDto(
    var id : Long,
    var fullName : String,
    var email : String,
    var systemRoleName: CompanyRoleName?
)
{
    companion object {
        fun toDto(u:User) = u.run {
            UserResponseDto(id!!,fullName,email,systemRoleName)
        }
    }
}

 class UserControlDto{
    var task: TaskResponseDto? = null
    var salary: SalaryResponseDto? = null
    constructor(t : TaskResponseDto){
        this.task = t
    }

    constructor(s : SalaryResponseDto){
        this.salary = s
    }
    companion object{
        fun toDto(j: Task) = j.run {
            UserControlDto(TaskResponseDto.doDto(j))
        }

        fun doDto(s:Salary) = s.run {
            UserControlDto(SalaryResponseDto.toDto(this))
        }
    }
}

data class TaskResponseDto(
    var id: Long,
    var name: String,
    var comment: String,
    var lifeTime: Date? = null,
    var user_id: Set<UserDtoSec>? = null,
    var status: ProjectStatus = ProjectStatus.TODO,
    var responsible: UserDto? = null
)
{
    companion object{
        fun toDto(t:Task) = t.run {
            TaskResponseDto(id!!,name,comment,lifeTime, userId!!.map { UserDtoSec.toDto(it) }.toSet(),status,
                responsible?.let { UserDto.toDto(it) })
        }

        fun doDto(t:Task) = t.run {
            TaskResponseDto(id!!,name,comment,lifeTime,null,status,null)
        }
    }
}

data class TaskEmailResponseDto(
    var id: Long,
    var name: String,
    var description: String,
    var lifetime: String,
    var responsible: String,
    var userId: String,
    var random2: Long
)

data class TaskDto(
    var localeDate : String,
    var projectStatus : ProjectStatus
)

data class CompanyUserUpdateDto(
    var userName : String? = null,
    var userEmail : String? = null,
    var userPassword : String? = null,
    var companyUserRole : CompanyRoleName? = null
)

data class CompanyUserResponseDto(
    var id : Long? = null,
    var companyName : String? = null,
    var companyUserRole : CompanyRoleName? = CompanyRoleName.ROLE_USER,
    var userName : String? = null,
    var userEmail : String? = null,
    var userPassword : String? = null
)
{
    companion object{
        fun toDto(c : CompanyUser) = c.run {
            CompanyUserResponseDto(id,workspace?.name,user?.systemRoleName,user?.fullName,user?.email,user?.password)
        }
    }
}

data class SalaryDto(
    var salary : Double,
    var userId : Long,
    var createDate : Date
)

data class SalaryResponseDto(
    var salary : Double? = null,
    var createDate : Date? = null
)
{
    companion object{
        fun toDto(s: Salary) = s.run {
            SalaryResponseDto(salary,createSalary)
        }
    }
}
