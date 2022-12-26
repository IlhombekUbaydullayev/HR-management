package com.example.hrmanagement

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    fun login(dto: LoginDTO): ApiResponsess
    fun verifyEmail(email: String, emailCode: String, company: String, dto: VerifyDTO): ApiResponsess
    fun getVerifyEmail(emailCode: String, email: String): ApiResponsess
}

interface CompanyService {
    fun add(dto: CompanyDto): CompanyDto
    fun update(id: Long, dto: CompanyDtoUpdate): CompanyDtoUpdate
    fun getAll(): List<CompanyResponseDto>
    fun getById(id: Long): CompanyResponseDto
    fun delete(id: Long): BaseMessage
}

interface UserService {
    fun add(dto: UserDto, request: HttpServletRequest): ApiResponsess
    fun getAll(request: HttpServletRequest): List<CompanyUserDto>
    fun delete(id: Long): BaseMessage
    fun update(id: Long, dto: CompanyUserUpdateDto): CompanyUserResponseDto
    fun getById(id: Long): List<UserControlDto>
}

interface TaskService {
    fun create(dto: TaskCreateDto, request: HttpServletRequest): TaskCreateDto
    fun getAll(): List<TaskResponseDto>
    fun getApi(
        name: String, description: String, lifetime: String, responsible: String, userId: String, random2: Long
    ): TaskEmailResponseDto

    fun send(id: Long, dto: TaskDto, request: HttpServletRequest): TaskDto
    fun getById(id: Long): TaskResponseDto
    fun update(id: Long, dto: TaskUpdateDto, request: HttpServletRequest): TaskResponseDto
    fun delete(id: Long): BaseMessage
}

interface SalaryService {
    fun create(dto: SalaryDto): SalaryDto
    fun getByApi(
        id: Long,
        request: HttpServletRequest,
        page: Pageable,
        sort: Sort,
        page1: CustomPage
    ): Page<SalaryResponseDto>
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

    override fun login(dto: LoginDTO): ApiResponsess {
        return try {
            val authenticate: Authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    dto.email, dto.password
                )
            )
            val user: User = authenticate.principal as User
            val token: String = jwtProvider.generateToken(user.email, user.systemRoleName!!)
            ApiResponsess(token, true)
        } catch (e: Exception) {
            ApiResponsess("Email not found", false)
        }
    }

    override fun verifyEmail(email: String, emailCode: String, company: String, dto: VerifyDTO): ApiResponsess {
        userRepository.existsByEmailAndDeletedFalse(email).throwIfFalse { EmailException("", "") }
        val optionalUser: User = userRepository.findByEmailAndDeletedFalse(email)!!
        val decodedString = String(Base64.getMimeDecoder().decode(company))
        if (emailCode == optionalUser.fullName && !optionalUser.enabled) {
            optionalUser.enabled = true
            optionalUser.passwords = passwordEncoder.encode(dto.password)
            val comp = companyRepository.findByName(decodedString) ?: throw ObjectNotFoundException(
                "Company",
                decodedString
            )
            val users = companyRoleRepository.findByWorkspaceIdAndName(comp.id!!, optionalUser.systemRoleName!!.name)
            userRepository.save(optionalUser)

            companyUserRepository.save(CompanyUser(comp, optionalUser, users))
            return ApiResponsess("Acount aktivlashtirildi", true)
        }
        return ApiResponsess("$decodedString Already reported", false)
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
        return userRepository.findByEmailAndDeletedFalse(username)
            ?: throw UsernameNotFoundException("$username topilmadi")
    }
}


@Service
class CompanyServiceImpl(
    private var userRepository: UserRepository,
    private var repository: CompanyRepository,
    private var companyRoleRepository: CompanyRoleRepository,
    private var companyPermissionRepository: CompanyPermissionRepository,
    private var emails: Emails
) : CompanyService {

    override fun add(dto: CompanyDto): CompanyDto {
        repository.existsByName(dto.name!!).throwIfTrue { AlreadyReportedException("Company", dto.name!!) }
        userRepository.existsByEmailAndDeletedFalse(dto.email!!).throwIfTrue {
            AlreadyReportedException(
                "User",
                dto.email!!
            )
        }
        //Add Company

        val user = User(
            dto.full_name!!, dto.email!!, "", CompanyRoleName.ROLE_DIRECTOR
        )
        val encodedString: String = Base64.getEncoder().encodeToString(dto.name!!.toByteArray())
        emails.sendEmail(user.email, user.fullName, encodedString).throwIfFalse { EmailException("", "") }
        val company: MutableList<Company> = ArrayList()
        company.add(Company(dto.name!!, user))
        userRepository.save(user)
        repository.saveAll(company)
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
                director, companyPermissionName
            )
            companyPermissions.add(workspacePermission)
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_HR_MANAGER)) {
                companyPermissions.add(
                    CompanyPermission(
                        hrManager, companyPermissionName
                    )
                )
            }
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_MANAGER)) {
                companyPermissions.add(
                    CompanyPermission(
                        manager, companyPermissionName
                    )
                )
            }
            if (companyPermissionName.workspaceRoleNames.contains(CompanyRoleName.ROLE_USER)) {
                companyPermissions.add(
                    CompanyPermission(
                        users, companyPermissionName
                    )
                )
            }
        }
        companyPermissionRepository.saveAll(companyPermissions)
        return dto
    }

    override fun update(id: Long, dto: CompanyDtoUpdate): CompanyDtoUpdate {
        val company = repository.findByIdAndDeletedFalse(id) ?: throw ObjectNotFoundException("Company", id)
        company.name = dto.name.let { it!! }
        val user = userRepository.findByIdAndDeletedFalse(dto.ownerId!!) ?: throw ObjectNotFoundException(
            "User",
            dto.ownerId!!
        )
        company.owner = user
        repository.save(company)
        return dto
    }

    override fun getAll() = repository.getAllByDeletedFalse().map { CompanyResponseDto.toDto(it) }

    override fun getById(id: Long): CompanyResponseDto {
        repository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException("Company", id) }
        return repository.findById(id).map { CompanyResponseDto.toDto(it) }.get()
    }

    override fun delete(id: Long): BaseMessage {
        repository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException("Company", id) }
        val company = repository.findByIdAndDeletedFalse(id)
        company?.deleted = true
        return BaseMessage.OK
    }

}

@Service
class UserServiceImp(
    private var userRepository: UserRepository,
    private var companyUserRepository: CompanyUserRepository,
    private var taskRepository: TaskRepository,
    private var salaryRepository: SalaryRepository,
    private var emails: Emails
) : UserService {
    override fun add(dto: UserDto, request: HttpServletRequest): ApiResponsess {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name)) {
            val use = userRepository.findByEmailAndDeletedFalse(request.remoteUser)
            val id = companyUserRepository.findByIdAndDeletedFalse(use?.id!!)
                ?: throw ObjectNotFoundException("Company User", use.id!!)
            val encodedString: String = Base64.getEncoder().encodeToString(id.workspace!!.name.toByteArray())
            if (dto.systemRoleName?.name == "ROLE_USER") dto.systemRoleName = CompanyRoleName.ROLE_HR_MANAGER
            val user = User(
                dto.full_name!!.replace(" ", ""), dto.email, "", dto.systemRoleName
            )
            userRepository.existsByEmailAndDeletedFalse(dto.email)
                .throwIfTrue { AlreadyReportedException("User", dto.email) }
            emails.sendEmail(dto.email, dto.full_name!!.replace(" ", ""), encodedString)
                .throwIfFalse { EmailException("", "") }
            userRepository.save(user)
            return ApiResponsess("ROLE_DIRECTOR", true)
        }
        if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name)) {
            val use = userRepository.findByEmailAndDeletedFalse(request.remoteUser)
            val id = companyUserRepository.findByIdAndDeletedFalse(use?.id!!)
                ?: throw ObjectNotFoundException("Company User", use.id!!)
            val encodedString: String = Base64.getEncoder().encodeToString(id.workspace?.name?.toByteArray())
            emails.sendEmail(dto.email, dto.full_name!!.replace(" ", ""), encodedString)
                .throwIfFalse { EmailException("", "") }
            if (dto.systemRoleName?.name == "ROLE_DIRECTOR") return ApiResponsess("error message", false)
            val user = User(
                dto.full_name!!.replace(" ", ""), dto.email, ""
            )
            userRepository.existsByEmailAndDeletedFalse(dto.email)
                .throwIfTrue { AlreadyReportedException("User", dto.email) }
            userRepository.save(user)
            return ApiResponsess("HR_MANAGER", true)
        }
        return ApiResponsess("", false)
    }

    override fun getAll(request: HttpServletRequest): List<CompanyUserDto> {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name)) return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name,
            CompanyRoleName.ROLE_HR_MANAGER
        )
            .map { CompanyUserDto.toDto(it) } + companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name, CompanyRoleName.ROLE_USER
        )
            .map { CompanyUserDto.toDto(it) } + companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name,
            CompanyRoleName.ROLE_MANAGER
        ).map { CompanyUserDto.toDto(it) }
        else if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name)) return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name, CompanyRoleName.ROLE_USER
        )
            .map { CompanyUserDto.toDto(it) } + companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name,
            CompanyRoleName.ROLE_MANAGER
        ).map { CompanyUserDto.toDto(it) }
        else if (request.isUserInRole(CompanyRoleName.ROLE_MANAGER.name)) return companyUserRepository.findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
            companyUserRepository.findByUserEmail(request.remoteUser)?.workspace!!.name, CompanyRoleName.ROLE_USER
        ).map { CompanyUserDto.toDto(it) }
        return listOf()
    }

    override fun delete(id: Long): BaseMessage {
        val delete =
            companyUserRepository.findByIdAndDeletedFalse(id) ?: throw ObjectNotFoundException("Company User", id)
        delete.user!!.deleted = true
        delete.deleted = true
        userRepository.save(delete.user!!)
        companyUserRepository.save(delete)
        return BaseMessage.OK
    }

    override fun update(id: Long, dto: CompanyUserUpdateDto): CompanyUserResponseDto {
        val update =
            companyUserRepository.findByIdAndDeletedFalse(id) ?: throw ObjectNotFoundException("Company User", id)
        return update.run {
            dto.userName?.let { user?.fullName = it }
            dto.userEmail?.let { user?.email = it }
            dto.companyUserRole?.let { user?.systemRoleName = it }
            dto.userPassword?.let { user?.passwords = it }
            CompanyUserResponseDto.toDto(companyUserRepository.save(update))
        }
    }

    override fun getById(id: Long): List<UserControlDto> {
        return taskRepository.findAllByUserId(id)
            .map { UserControlDto.toDto(it) } + salaryRepository.findAllByCompanyUserUserId(id)
            .map { UserControlDto.doDto(it) }
    }

}

@Service
class TaskServiceImp(
    private var repository: TaskRepository,
    private var companyUserRepository: CompanyUserRepository,
    private var emails: Emails
) : TaskService {
    override fun create(dto: TaskCreateDto, request: HttpServletRequest): TaskCreateDto {
        dto.apply {
            val set = HashSet<User>()
            val random2 = Random().nextLong(10000) + 1
            dto.userId.forEach {
                set.add(
                    companyUserRepository.findByIdAndDeletedFalse(it)?.user
                        ?: throw ObjectNotFoundException("Company User", it)
                )
                val compUser = companyUserRepository.findByIdAndDeletedFalse(it)?.user ?: throw ObjectNotFoundException(
                    "Company User",
                    it
                )
                emails.sendEmailTask(
                    name,
                    description!!,
                    "${lifetime.year}:${lifetime.month}:${lifetime.day}",
                    companyUserRepository.findByUserEmailAndDeletedFalse(request.remoteUser)?.user!!.email,
                    compUser.email,
                    random2
                )
            }
            val compUser = companyUserRepository.findByUserEmailAndDeletedFalse(request.remoteUser)
                ?: throw ObjectNotFoundException("Company User", request.remoteUser)
            repository.save(
                Task(
                    name,
                    description!!,
                    lifetime,
                    status,
                    compUser.user,
                    set,
                    random2
                )
            )
        }
        return dto
    }

    override fun getAll(): List<TaskResponseDto> =
        repository.getAllByDeletedFalse().map { TaskResponseDto.toDto(it) }

    override fun getApi(
        name: String, description: String, lifetime: String, responsible: String, userId: String, random2: Long
    ): TaskEmailResponseDto {
        val task = repository.getByGeneric(random2) ?: throw ObjectNotFoundException("Task", random2)
        return TaskEmailResponseDto(
            task.id!!, name, description, lifetime, responsible, userId, random2
        )
    }

    override fun send(id: Long, dto: TaskDto, request: HttpServletRequest): TaskDto {
        val task = repository.findByGeneric(id) ?: throw ObjectNotFoundException("Task", id)
        if (dto.projectStatus == ProjectStatus.DONE) {
            task.status = ProjectStatus.DONE
            emails.sendTask(task.responsible!!.email, task.name, request.remoteUser)
                .throwIfFalse { EmailException("", "") }
            repository.save(task)
            return dto
        }
        return dto
    }

    override fun getById(id: Long): TaskResponseDto {
        repository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException("Task", id) }
        return repository.findById(id).map { TaskResponseDto.toDto(it) }.get()
    }

    override fun update(id: Long, dto: TaskUpdateDto, request: HttpServletRequest): TaskResponseDto {
        repository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException("Task", id) }
        val update = repository.findById(id).get()
        return update.run {
            dto.name?.let { name = it }
            dto.comment?.let { comment = it }
            dto.status?.let { status = it }
            TaskResponseDto.doDto(repository.save(update))
        }
    }

    override fun delete(id: Long): BaseMessage {
        repository.existsByIdAndDeletedFalse(id).throwIfFalse { ObjectNotFoundException("Task", id) }
        val delete = repository.findById(id).get()
        delete.deleted = true
        repository.save(delete)
        return BaseMessage.OK
    }

}

@Service
class SalaryServiceImp(
    private var repository: SalaryRepository, private var companyUserRepository: CompanyUserRepository
) : SalaryService {
    override fun create(dto: SalaryDto): SalaryDto {
        val user =
            companyUserRepository.findByUserId(dto.userId) ?: throw ObjectNotFoundException("Company User", dto.userId)
        repository.existsByMonthAndCompanyUserUser(
            dto.createDate.month, user.user!!
        ).throwIfTrue { AlreadyReportedException("already", "") }
        repository.save(Salary(dto.salary, user, dto.createDate, null, dto.createDate.month))
        return dto
    }

    override fun getByApi(
        id: Long,
        request: HttpServletRequest,
        page: Pageable,
        sort: Sort,
        page1: CustomPage
    ): Page<SalaryResponseDto> {
        return when (sort) {
            Sort.ID -> {
                repository.existsByCompanyUserUserId(id).throwIfFalse { ObjectNotFoundException("Company User", id) }
                repository.searchByCompanyUserUserOrId(id, page).map { SalaryResponseDto.toDto(it) }
            }

            Sort.START_DATE -> {
                repository.findByCompanyUserUserAndCreateSalaryBetween(
                    companyUserRepository.findByUserId(id)?.user!!,
                    page1.startDate,
                    page1.endDate,
                    page
                ).map { SalaryResponseDto.toDto(it) }
            }

            else -> {
                throw ObjectNotFoundException("Salary", sort)
            }
        }

    }
}