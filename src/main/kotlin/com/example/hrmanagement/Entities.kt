package com.example.hrmanagement

import lombok.EqualsAndHashCode
import lombok.Value
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.ManyToAny
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
abstract class AbsLongEntity : AbsMainEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var deleted: Boolean? = false
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbsMainEntity {

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private val createdAt: Timestamp? = null

    @UpdateTimestamp
    private val updatedAt: Timestamp? = null

    @JoinColumn(updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private val createdBy: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    private val updatedBy: User? = null
}

@Entity(name = "users")
@EqualsAndHashCode(callSuper = true)
data class User(
    var fullName: String,
    @field:Column(unique = true, nullable = false)
    var email: String,
    var passwords: String = "",
    @Enumerated(EnumType.STRING)
    var systemRoleName: CompanyRoleName? = CompanyRoleName.ROLE_USER,
    var enabled: Boolean = false,
    private val accountNonExpired: Boolean = true,
    private val accountNonLocked: Boolean = true,
    private val credentialsNonExpired: Boolean = true,
    var emailCode: String? = null
) :
    AbsLongEntity(), UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority?> {
        val simpleGrantedAuthority = SimpleGrantedAuthority(systemRoleName?.name)
        return Collections.singletonList(simpleGrantedAuthority)
    }

    override fun getPassword(): String {
        return this.passwords
    }

    override fun getUsername(): String {
        return this.email
    }

    override fun isAccountNonExpired(): Boolean {
        return this.accountNonExpired
    }

    override fun isAccountNonLocked(): Boolean {
        return this.accountNonLocked
    }

    override fun isCredentialsNonExpired(): Boolean {
        return this.credentialsNonExpired
    }

    override fun isEnabled(): Boolean {
        return this.enabled
    }
}


@EqualsAndHashCode(callSuper = true)
@Entity
data class Company(
    @field:Column(nullable = false) var name: String,
    @field:ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE])
    var owner: User? = null,
) : AbsLongEntity()

@EqualsAndHashCode(callSuper = true)
@Entity
data class CompanyPermission(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private val workspaceRole: CompanyRole? = null,
    @Enumerated(EnumType.STRING)
    private val permission: CompanyPermissionName? = null
) : AbsLongEntity()

@EqualsAndHashCode(callSuper = true)
@Entity
data class CompanyRole(
    @ManyToMany(fetch = FetchType.LAZY)
    val workspace: List<Company>? = null,
    @Column(nullable = false)
    val name: String? = null,
    @Enumerated(EnumType.STRING)
    val extendsRole: CompanyRoleName? = null
) : AbsLongEntity()

@EqualsAndHashCode(callSuper = true)
@Entity
data class CompanyUser(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val workspace: Company? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    var workspaceRole: CompanyRole? = null
) : AbsLongEntity()

@Entity
data class Task(
    var name: String,
    var comment: String,
    var lifeTime: Date? = null,
    @Enumerated(EnumType.STRING) var status: ProjectStatus = ProjectStatus.TODO,
    @ManyToOne
    var responsible: User? = null,
    @ManyToMany
    var userId: Set<User>? = null,
    @field:Column(nullable = false, unique = true)
    var generic: Long,
) : AbsLongEntity()

@Entity
data class Controller(
    var enter_time: Date,
    var exit_time: Date,
    var status: Boolean,
    @OneToOne(fetch = FetchType.LAZY)
    var companyUser: CompanyUser? = null
) : AbsLongEntity()

@Entity
data class Salary(
    var salary: Double? = null,
    @ManyToOne
    var companyUser: CompanyUser,
    val createSalary: Date? = null,
    var endDate: Date? = null,
    val month: Int? = null
) : AbsLongEntity()