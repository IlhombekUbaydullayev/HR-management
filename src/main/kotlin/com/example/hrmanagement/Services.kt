package com.example.hrmanagement

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.ArrayList
import javax.mail.internet.MimeMessage
import javax.servlet.http.HttpServletRequest

interface AuthService {
    fun login(loginDTO: LoginDTO): ApiResponsess
    fun verifyEmail(email: String, emailCode: String, verifyDTO: VerifyDTO): ApiResponsess
    fun getVerifyEmail(emailCode: String, email: String): ApiResponsess
}

interface CompanyService {
    fun addCompany(companyDto: CompanyDto): BaseMessage
    fun update(id: Long, companyDtoUpdate: CompanyDtoUpdate): BaseMessage
    fun getAll() : List<CompanyResponseDto>
}

interface UserService {
    fun addUser(userDto: UserDto, request: HttpServletRequest): ApiResponsess
}

@Service
class AuthServiceImp(
    private var authenticationManager: AuthenticationManager,
    private var userRepository: UserRepository,
    private var passwordEncoder: PasswordEncoder,
    private var jwtProvider: JwtProvider
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

    override fun verifyEmail(email: String, emailCode: String, verifyDTO: VerifyDTO): ApiResponsess {
        userRepository.existsByEmailAndDeletedFalse(email).throwIfFalse { EmailException() }
        val optionalUser = userRepository.findByEmail(email)
        if (optionalUser.isPresent) {
            val user = optionalUser.get()
            if (emailCode == user.fullName) {
                user.enabled = true
                user.passwords = passwordEncoder.encode(verifyDTO.password)
                userRepository.save(user)
                return ApiResponsess("Acount aktivlashtirildi", true)
            }
            return ApiResponsess("Kod xato", false)
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
    private var companyUserRepository: CompanyUserRepository
) : CompanyService {

    @Autowired
    lateinit var javaMailSender: JavaMailSender

    override fun addCompany(companyDto: CompanyDto): BaseMessage {
        companyRepository.existsByName(companyDto.name!!).throwIfTrue { AlreadyReportedException() }
        userRepository.existsByEmailAndDeletedFalse(companyDto.email!!).throwIfTrue { AlreadyReportedException() }
        //Add Company

        val user = User(
            companyDto.full_name!!, companyDto.email!!, "", CompanyRoleName.ROLE_DIRECTOR
        )
        sendEmail(user.email, user.fullName).throwIfFalse { EmailException() }
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
        companyUserRepository.save(
            CompanyUser(
                company,
                user,
                director,
                Timestamp(System.currentTimeMillis()),
                Timestamp(System.currentTimeMillis())
            )
        )
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


    fun sendEmail(sendingEmail: String, emailCode: String): Boolean {
        try {
            val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true)
            mimeMessageHelper.setFrom("ubaydullaevilhombek681@gmail.com")
            mimeMessageHelper.setTo(sendingEmail)
            mimeMessageHelper.setSubject("Accountni Tasdiqlash")
            mimeMessageHelper.setText("<a href='http://localhost:8090/api/v1/auth/verifyEmail?emailCode=$emailCode&email=$sendingEmail'>Tasdiqlang</a>")
            javaMailSender.send(mimeMessage)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

@Service
class UserServiceImp(
    private var userRepository: UserRepository,
    private var companyRoleRepository: CompanyRoleRepository,
    private var javaMailSender: JavaMailSender,
    private var companyUserRepository : CompanyUserRepository
) : UserService {
    override fun addUser(userDto: UserDto, request: HttpServletRequest): ApiResponsess {
        if (request.isUserInRole(CompanyRoleName.ROLE_DIRECTOR.name)) {
            sendEmail(userDto.email,userDto.full_name!!).throwIfFalse { EmailException() }
            if (userDto.systemRoleName?.name == "ROLE_USER") userDto.systemRoleName = CompanyRoleName.ROLE_HR_MANAGER
            val user = User(
                userDto.full_name!!, userDto.email, "", userDto.systemRoleName
            )
            userRepository.existsByEmailAndDeletedFalse(userDto.email).throwIfTrue { AlreadyReportedException() }
            userRepository.save(user)
            return ApiResponsess("ROLE_DIRECTOR", true)
        }
        if (request.isUserInRole(CompanyRoleName.ROLE_HR_MANAGER.name)) {
            sendEmail(userDto.email,userDto.full_name!!).throwIfFalse { EmailException() }
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

    fun sendEmail(sendingEmail: String, emailCode: String): Boolean {
        try {
            val mimeMessage: MimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, true)
            mimeMessageHelper.setFrom("ubaydullaevilhombek681@gmail.com")
            mimeMessageHelper.setTo(sendingEmail)
            mimeMessageHelper.setSubject("Accountni Tasdiqlash")
            mimeMessageHelper.setText("<a href='http://localhost:8090/api/v1/auth/verifyEmail?emailCode=$emailCode&email=$sendingEmail'>Tasdiqlang</a>")
            javaMailSender.send(mimeMessage)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}