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
}

interface TaskService {
    fun create(taskCreateDto: TaskCreateDto, request: HttpServletRequest):BaseMessage
    fun getAll():List<TaskResponseDto>
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
        val optionalUser = userRepository.findByEmail(email)
        if (optionalUser.isPresent) {
            val user = optionalUser.get()
            if (emailCode == user.fullName && !user.enabled) {
                user.enabled = true
                user.passwords = passwordEncoder.encode(verifyDTO.password)
                val decodedString: String = String(Base64.getDecoder().decode(company))
                companyRepository.existsByName(decodedString).throwIfFalse { ObjectNotFoundException() }
                val comp = companyRepository.findByName(decodedString).get()
                val users = companyRoleRepository.findById(user.id!!).get()
                userRepository.save(user)
                companyUserRepository.save(CompanyUser(comp,user,users))
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
        val findByEmail = userRepository.findByEmail(username)
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
        val company = Company(
            companyDto.name!!,
            user
        )
        userRepository.save(user)
        companyRepository.save(company)

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
    private var passwordEncoder: PasswordEncoder,
    private var companyRepository: CompanyRepository,
    private var emails: Emails
) : UserService {
    override fun addUser(userDto: UserDto, request: HttpServletRequest): ApiResponsess {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name)) {
            val use = userRepository.findByEmail(request.remoteUser)
            val id = companyUserRepository.findById(use.get().id!!)
            val encodedString: String = Base64.getEncoder().encodeToString(id.get().workspace!!.name.toByteArray())
            emails.sendEmail(userDto.email,userDto.full_name!!,encodedString).throwIfFalse { EmailException() }
            if (userDto.systemRoleName?.name == "ROLE_USER") userDto.systemRoleName = CompanyRoleName.ROLE_HR_MANAGER
            val user = User(
                userDto.full_name!!, userDto.email, "", userDto.systemRoleName
            )
            userRepository.existsByEmailAndDeletedFalse(userDto.email).throwIfTrue { AlreadyReportedException() }
            userRepository.save(user)
            return ApiResponsess("ROLE_DIRECTOR", true)
        }
        if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name)) {
//            val encodedString: String = Base64.getEncoder().encodeToString(id.get().workspace!!.name.toByteArray())
            emails.sendEmail(userDto.email,userDto.full_name!!,companyUserRepository.findById(userRepository.findByEmail(request.remoteUser).get().id!!).get().workspace!!.name).throwIfFalse { EmailException() }
            if (userDto.systemRoleName?.name == "ROLE_DIRECTOR") return ApiResponsess("error message", false)
            val user = User(
                userDto.full_name!!, userDto.email, ""
            )
            userRepository.existsByEmailAndDeletedFalse(userDto.email).throwIfTrue { AlreadyReportedException() }
            userRepository.save(user)
            return ApiResponsess("HR_MANAGER", true)
        }
        return ApiResponsess("", false)
    }

    override fun getAll(request: HttpServletRequest): List<UserDtoSec> {
         if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name))
            return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(companyUserRepository.findByUserEmail(request.remoteUser).get().workspace!!.name,CompanyRoleName.ROLE_HR_MANAGER).map { UserDtoSec.toDto(it.user!!) }
        return listOf()
    }

}

@Service
class TaskServiceImp(
    private var taskRepository: TaskRepository,
    private var userRepository: UserRepository
): TaskService{
    override fun create(taskCreateDto: TaskCreateDto, request: HttpServletRequest): BaseMessage {
        taskCreateDto.apply {
            val set = HashSet<User>()
            taskCreateDto.userId.forEach {
                taskRepository.existsById(it).throwIfFalse { ObjectNotFoundException() }
                userRepository.existsById(it).throwIfFalse { ObjectNotFoundException() }
                set.add(userRepository.findByIdAndDeletedFalse(it).get())
            }
            taskRepository.save(Task(name,description!!,lifetime,status,userRepository.findByEmail(request.remoteUser).get(),set))
        }
        return BaseMessage.OK
    }

    override fun getAll(): List<TaskResponseDto> = taskRepository.getAllByDeletedFalse().map { TaskResponseDto.toDto(it) }

}