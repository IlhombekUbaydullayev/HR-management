package com.example.hrmanagement

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.sql.Timestamp
import java.util.*
import javax.persistence.EntityManager


@NoRepositoryBean
interface BaseRepository<T : AbsLongEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T>
class BaseRepositoryImpl<T : AbsLongEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

}

interface CompanyPermissionRepository : BaseRepository<CompanyPermission>

interface CompanyRepository : BaseRepository<Company>{
    fun existsByName(name: String):Boolean
    fun findByIdAndDeletedFalse(id: Long) : Optional<Company>
    fun getAllByDeletedFalse(): List<Company>
    fun findByName(name: String):Optional<Company>
}

interface CompanyRoleRepository : BaseRepository<CompanyRole> {
    fun findByWorkspaceIdAndName(workspace_id: Long, name: String):Optional<CompanyRole>
}

interface CompanyUserRepository : BaseRepository<CompanyUser> {
//    fun findAllByWorkspaceNameAndDeletedFalse(workspace_name: String) : List<CompanyUser>
    fun findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
    workspace_name: String,
    user_systemRoleName: CompanyRoleName
) : List<CompanyUser>
    fun findByUserEmail(user_email: String) : Optional<CompanyUser>
    fun existsByIdAndDeletedFalse(id: Long):Boolean
    fun findByIdAndDeletedFalse(id: Long):Optional<CompanyUser>
    fun findByUserEmailAndDeletedFalse(email: String):Optional<CompanyUser>
    fun findByUserId(user_id: Long):Optional<CompanyUser>
}

interface UserRepository : BaseRepository<User>{
    fun findByEmailAndDeletedFalse(email: String): Optional<User>
    fun existsByEmail(email: String) : Boolean
    fun existsByEmailAndDeletedFalse(email: String) : Boolean
    fun findByIdAndDeletedFalse(id: Long) : Optional<User>
}

interface TaskRepository : BaseRepository<Task>{
    fun getAllByDeletedFalse():List<Task>
    fun getByGeneric(generic: Long):Optional<Task>
    fun findByGeneric(generic: Long):Optional<Task>
    fun findTasksByUserId(userId: MutableSet<User>):List<Task>
}

interface SalaryRepository : BaseRepository<Salary>{
    fun findByMonthAndCompanyUserUserOrderById(createSalary_month: Int, companyUser_user: User):Optional<Salary>
}