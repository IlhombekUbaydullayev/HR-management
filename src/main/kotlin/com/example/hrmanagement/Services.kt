package com.example.hrmanagement

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

interface AuthService {
    fun login(loginDTO: LoginDTO): ApiResponsess
    fun verifyEmail(email: String, emailCode: String, company: String, verifyDTO: VerifyDTO): ApiResponsess
    fun getVerifyEmail(emailCode: String, email: String): ApiResponsess
}

interface CompanyService {
    fun addCompany(companyDto: CompanyDto): BaseMessage
    fun update(id: Long, companyDtoUpdate: CompanyDtoUpdate): BaseMessage
    fun getAll() : List<CompanyResponseDto>
}

interface UserService {
    fun addUser(userDto: UserDto, request: HttpServletRequest): ApiResponsess
    fun getAll(request: HttpServletRequest):List<UserDtoSec>
    fun getAllTask(request: HttpServletRequest):List<CompanyUserDto>
    fun delete(id: Long): BaseMessage
    fun update(id: Long,dto : CompanyUserUpdateDto): CompanyUserResponseDto
    fun getById(id: Long): UserControlDto
}

interface TaskService {
    fun create(taskCreateDto: TaskCreateDto, request: HttpServletRequest):BaseMessage
    fun getAll():List<TaskResponseDto>
    fun getApi(name: String, description: String, lifetime: String, responsible: String, userId: String, random2: Long): TaskEmailResponseDto
    fun sendTask(id: Long, dto: TaskDto, request: HttpServletRequest): BaseMessage
    fun getById(id: Long): Any
}

interface SalaryService{
    fun create(salary: SalaryDto) : BaseMessage
}
@Service
class AuthServiceImp(
    private var authenticationManager: AuthenticationManager,
    private var userRepository: UserRepository,
    private var passwordEncoder: PasswordEncoder,
    private var jwtProvider: JwtProvider,
    private var companyUserRepository: CompanyUserRepository,
    private var companyRepository: CompanyRepository,
    private var companyRoleRepository: CompanyRoleRepository
) : AuthService, UserDetailsService {

    override fun login(loginDTO: LoginDTO): ApiResponsess {
        return try {
            val authenticate: Authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginDTO.email,
                    loginDTO.password
                )
            )
            val user: User = authenticate.principal as User
            val token: String = jwtProvider.generateToken(user.email, user.systemRoleName!!)
            ApiResponsess(token, true)
        } catch (e: Exception) {
            ApiResponsess("Email not found", false)
        }
    }

    override fun verifyEmail(email: String, emailCode: String, company: String, verifyDTO: VerifyDTO): ApiResponsess {
        userRepository.existsByEmailAndDeletedFalse(email).throwIfFalse { EmailException() }
        val optionalUser = userRepository.findByEmailAndDeletedFalse(email)
        if (optionalUser.isPresent) {
            val user = optionalUser.get()
            if (emailCode == user.fullName && !user.enabled) {
                user.enabled = true
                user.passwords = passwordEncoder.encode(verifyDTO.password)
                val decodedString: String = String(Base64.getDecoder().decode(company))
                companyRepository.existsByName(decodedString).throwIfFalse { ObjectNotFoundException() }
                val comp = companyRepository.findByName(decodedString).get()
                val users = companyRoleRepository.findByWorkspaceIdAndName(comp.id!!,user.systemRoleName!!.name).get()
                userRepository.save(user)

                companyUserRepository.save(CompanyUser(comp, user, users))
                return ApiResponsess("Acount aktivlashtirildi", true)
            }
            return ApiResponsess("Already reported", false)
        }
        return ApiResponsess("Bunday user mavjud emas", false)
    }

    override fun getVerifyEmail(emailCode: String, email: String): ApiResponsess {
        val existsByEmail = userRepository.existsByEmail(email)
        if (existsByEmail) {
            return ApiResponsess("Email prepared registration please enter password", true, null)
        }
        return ApiResponsess("Email not found", false)
    }

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val findByEmail = userRepository.findByEmailAndDeletedFalse(username)
        if (findByEmail.isPresent) {
            return findByEmail.get()
        }
        throw UsernameNotFoundException("$username topilmadi")
//    }
//        return userRepository.findByEmail(username!!).orElseThrow { UsernameNotFoundException("$username topilmadi") }
    }
}


@Service
class CompanyServiceImpl(
    private var userRepository: UserRepository,
    private var companyRepository: CompanyRepository,
    private var companyRoleRepository: CompanyRoleRepository,
    private var companyPermissionRepository: CompanyPermissionRepository,
    private var companyUserRepository: CompanyUserRepository,
    private var passwordEncoder: PasswordEncoder,
    private var emails: Emails
) : CompanyService {

    override fun addCompany(companyDto: CompanyDto): BaseMessage {
        companyRepository.existsByName(companyDto.name!!).throwIfTrue { AlreadyReportedException() }
        userRepository.existsByEmailAndDeletedFalse(companyDto.email!!).throwIfTrue { AlreadyReportedException() }
        //Add Company

        val user = User(
            companyDto.full_name!!, companyDto.email!!, "", CompanyRoleName.ROLE_DIRECTOR
        )
        val encodedString: String = Base64.getEncoder().encodeToString(companyDto.name!!.toByteArray())
        emails.sendEmail(user.email, user.fullName,encodedString).throwIfFalse { EmailException() }
        val company : MutableList<Company> = ArrayList()
        company.add(Company(companyDto.name!!, user))
        userRepository.save(user)
        companyRepository.saveAll(company)
        //Add Role

        val director = companyRoleRepository.save(CompanyRole(company, CompanyRoleName.ROLE_DIRECTOR.name, null))
        val hrManager = companyRoleRepository.save(CompanyRole(company, CompanyRoleName.ROLE_HR_MANAGER.name, null))
        val manager = companyRoleRepository.save(CompanyRole(company, CompanyRoleName.ROLE_MANAGER.name, null))
        val users = companyRoleRepository.save(CompanyRole(company, CompanyRoleName.ROLE_USER.name, null))
        //Add permission for role

        val companyPermissionNames = CompanyPermissionName.values()
        val companyPermissions: MutableList<CompanyPermission> = ArrayList()

        for (companyPermissionName in companyPermissionNames) {
            val workspacePermission = CompanyPermission(
                director,
                companyPermissionName
            )
            companyPermissions.add(workspacePermission)
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_HR_MANAGER)) {
                companyPermissions.add(
                    CompanyPermission(
                        hrManager,
                        companyPermissionName
                    )
                )
            }
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_MANAGER)) {
                companyPermissions.add(
                    CompanyPermission(
                        manager,
                        companyPermissionName
                    )
                )
            }
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_USER)) {
                companyPermissions.add(
                    CompanyPermission(
                        users,
                        companyPermissionName
                    )
                )
            }
        }

        companyPermissionRepository.saveAll(companyPermissions)

        //Company User Added
//        companyUserRepository.save(
//            CompanyUser(
//                company,
//                user,
//                director,
//                Timestamp(System.currentTimeMillis()),
//                Timestamp(System.currentTimeMillis())
//            )
//        )
        return BaseMessage.OK
    }

    override fun update(id: Long, companyDtoUpdate: CompanyDtoUpdate): BaseMessage {
        val company = companyRepository.findByIdAndDeletedFalse(id)
        company.isPresent.throwIfFalse { ObjectNotFoundException() }
        val companys = company.get()
        companys.name = companyDtoUpdate.name.let { it!! }
        userRepository.findByIdAndDeletedFalse(companyDtoUpdate.ownerId!!).isPresent.throwIfFalse { ObjectNotFoundException() }
        companys.owner = userRepository.findById(companyDtoUpdate.ownerId!!).get()
        companyRepository.save(companys)
        return BaseMessage.OK
    }

    override fun getAll() = companyRepository.getAllByDeletedFalse().map { CompanyResponseDto.toDto(it) }

}

@Service
class UserServiceImp(
    private var userRepository: UserRepository,
    private var companyRoleRepository: CompanyRoleRepository,
    private var companyUserRepository : CompanyUserRepository,
    private var taskRepository: TaskRepository,
    private var companyRepository: CompanyRepository,
    private var emails: Emails
) : UserService {
    override fun addUser(userDto: UserDto, request: HttpServletRequest): ApiResponsess {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name)) {
            val use = userRepository.findByEmailAndDeletedFalse(request.remoteUser)
            val id = companyUserRepository.findById(use.get().id!!)
            val encodedString: String = Base64.getEncoder().encodeToString(id.get().workspace!!.name.toByteArray())
            if (userDto.systemRoleName?.name == "ROLE_USER") userDto.systemRoleName = CompanyRoleName.ROLE_HR_MANAGER
            val user = User(
                userDto.full_name!!.replace(" ",""), userDto.email, "", userDto.systemRoleName
            )
            userRepository.existsByEmailAndDeletedFalse(userDto.email).throwIfTrue { AlreadyReportedException() }
            emails.sendEmail(userDto.email,userDto.full_name!!.replace(" ",""),encodedString).throwIfFalse { EmailException() }
            userRepository.save(user)
            return ApiResponsess("ROLE_DIRECTOR", true)
        }
        if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name)) {
            val use = userRepository.findByEmailAndDeletedFalse(request.remoteUser)
            val id = companyUserRepository.findById(use.get().id!!)
            val encodedString: String = Base64.getEncoder().encodeToString(id.get().workspace!!.name.toByteArray())
            emails.sendEmail(userDto.email,userDto.full_name!!.replace(" ",""),encodedString).throwIfFalse { EmailException() }
            if (userDto.systemRoleName?.name == "ROLE_DIRECTOR") return ApiResponsess("error message", false)
            val user = User(
                userDto.full_name!!.replace(" ",""), userDto.email, ""
            )
            userRepository.existsByEmailAndDeletedFalse(userDto.email).throwIfTrue { AlreadyReportedException() }
            userRepository.save(user)
            return ApiResponsess("HR_MANAGER", true)
        }
        return ApiResponsess("", false)
    }

    override fun getAll(request: HttpServletRequest): List<UserDtoSec> {
         if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name))
            return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_HR_MANAGER).map { UserDtoSec.toDto(it.user!!) } +
                    companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_USER).map { UserDtoSec.toDto(it.user!!) } +
                    companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_MANAGER).map { UserDtoSec.toDto(it.user!!) }
         else if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name))
             return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_USER).map { UserDtoSec.toDto(it.user!!) } +
                     companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_MANAGER).map { UserDtoSec.toDto(it.user!!) }
        return listOf()
    }

    override fun getAllTask(request: HttpServletRequest): List<CompanyUserDto> {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name))
            return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_HR_MANAGER).map { CompanyUserDto.toDto(it) } +
                    companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_USER).map { CompanyUserDto.toDto(it) } +
                    companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_MANAGER).map { CompanyUserDto.toDto(it) }
        else if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name))
            return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_USER).map { CompanyUserDto.toDto(it) } +
                    companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_MANAGER).map { CompanyUserDto.toDto(it) }
        else if (request.isUserInRole(CompanyRoleName.ROLE_MANAGER.name))
            return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_USER).map { CompanyUserDto.toDto(it) }
        return listOf()
    }

    override fun delete(id: Long): BaseMessage {
        val delete = companyUserRepository.findById(id)
        companyUserRepository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException() }
        delete.isPresent.throwIfFalse { ObjectNotFoundException() }
        delete.get().user!!.deleted = true
        delete.get().deleted = true
        userRepository.save(delete.get().user!!)
        companyUserRepository.save(delete.get())
        return BaseMessage.OK
    }

    override fun update(id: Long, dto: CompanyUserUpdateDto): CompanyUserResponseDto {
        companyUserRepository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException() }
        val update = companyUserRepository.findById(id).get()
        return update.run {
            dto.userName?.let { user?.fullName = it }
            dto.userEmail?.let { user?.email = it }
            dto.companyUserRole?.let { user?.systemRoleName = it }
            dto.userPassword?.let { user?.passwords = it }
            CompanyUserResponseDto.toDto(companyUserRepository.save(update))
        }
    }

    override fun getById(id: Long):UserControlDto  {
        val list : MutableSet<User> = mutableSetOf()
        for (i in taskRepository.findById(id).get().userId!!) {
//            return UserControlDto.toDto(taskRepository.findTaskByUserId(i.id!!)[0])
        }
        return UserControlDto()
    }

}

@Service
class TaskServiceImp(
    private var taskRepository: TaskRepository,
    private var userRepository: UserRepository,
    private var companyUserRepository: CompanyUserRepository,
    private var emails: Emails
): TaskService{
    override fun create(taskCreateDto: TaskCreateDto, request: HttpServletRequest): BaseMessage {
        taskCreateDto.apply {
            val set = HashSet<User>()
            val random2 = Random().nextLong(10000) + 1
            taskCreateDto.userId.forEach {
//                taskRepository.existsById(it).throwIfTrue { ObjectNotFoundException() }
                companyUserRepository.existsById(it).throwIfFalse { ObjectNotFoundException() }
                set.add(companyUserRepository.findByIdAndDeletedFalse(it).get().user!!)
                emails.sendEmailTask(name,description!!,"${lifetime.year}:${lifetime.month}:${lifetime.day}",
                    companyUserRepository.findByUserEmailAndDeletedFalse(request.remoteUser).get().user!!.email,companyUserRepository.findById(it).get().user!!.email,random2)
            }
            taskRepository.save(Task(name,description!!,lifetime,status,companyUserRepository.findByUserEmailAndDeletedFalse(request.remoteUser).get().user,set,random2))
        }
        return BaseMessage.OK
    }

    override fun getAll(): List<TaskResponseDto> = taskRepository.getAllByDeletedFalse().map { TaskResponseDto.toDto(it) }

    override fun getApi(
        name: String,
        description: String,
        lifetime: String,
        responsible: String,
        userId: String,
        random2: Long
    ): TaskEmailResponseDto {
        return TaskEmailResponseDto(taskRepository.getByGeneric(random2).get().id!!,name,description,lifetime,responsible,userId,random2)
    }

    override fun sendTask(id: Long, dto: TaskDto, request: HttpServletRequest): BaseMessage {
        val task = taskRepository.findByGeneric(id).get()
        if (dto.projectStatus == ProjectStatus.DONE) {
            task.status = ProjectStatus.DONE
            emails.sendTask(task.responsible!!.email,task.name,request.remoteUser).throwIfFalse { EmailException() }
            taskRepository.save(task)
            return BaseMessage.OK
        }
        return BaseMessage.Not_Found
    }

    override fun getById(id: Long): TaskResponseDto {
        return taskRepository.findByGeneric(id).map { TaskResponseDto.toDto(it) }.get()
    }

}

@Service
class SalaryServiceImp(
    private var salaryRepository: SalaryRepository,
    private var companyUserRepository: CompanyUserRepository
) : SalaryService{
    override fun create(salary: SalaryDto): BaseMessage {
        val user = companyUserRepository.findByUserId(salary.userId)
        user.isPresent.throwIfFalse { ObjectNotFoundException() }
        salaryRepository.findByMonthAndCompanyUserUserOrderById(salary.createDate.month,user.get().user!!).isPresent.throwIfTrue { AlreadyReportedException() }
        salaryRepository.save(Salary(salary.salary,user.get(),salary.createDate,salary.createDate.month))
        return BaseMessage.OK
    }

}