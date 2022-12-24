package com.example.hrmanagement

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
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

interface CompanyRepository : BaseRepository<Company> {
    fun existsByName(name: String): Boolean
    fun findByIdAndDeletedFalse(id: Long): Company?
    fun getAllByDeletedFalse(): List<Company>
    fun findByName(name: String): Company?
    fun existsByIdAndDeletedFalse(id: Long): Boolean
}

interface CompanyRoleRepository : BaseRepository<CompanyRole> {
    fun findByWorkspaceIdAndName(workspace_id: Long, name: String): CompanyRole?
}

interface CompanyUserRepository : BaseRepository<CompanyUser> {
    fun findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
        workspace_name: String,
        user_systemRoleName: CompanyRoleName
    ): List<CompanyUser>
    fun findByUserEmail(user_email: String): CompanyUser?
    fun existsByIdAndDeletedFalse(id: Long): Boolean
    fun findByIdAndDeletedFalse(id: Long): CompanyUser?
    fun findByUserEmailAndDeletedFalse(email: String): CompanyUser?
    fun findByUserId(user_id: Long): CompanyUser?
}

interface UserRepository : BaseRepository<User> {
    fun findByEmailAndDeletedFalse(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByEmailAndDeletedFalse(email: String): Boolean
    fun findByIdAndDeletedFalse(id: Long): User?
}

interface TaskRepository : BaseRepository<Task> {
    fun getAllByDeletedFalse(): List<Task>
    fun getByGeneric(generic: Long): Task?
    fun findByGeneric(generic: Long): Task?

    @Query("select t from Task t join t.userId u where u.id = :id")
    fun findAllByUserId(@Param("id") id: Long): List<Task>

    @Query("select t from Task t join t.userId u where u.id = :id")
    fun findByUserId(@Param("id") id: Long): Task?
    fun existsByIdAndDeletedFalse(id: Long): Boolean
}

interface SalaryRepository : BaseRepository<Salary> {
    fun existsByCompanyUserUserId(companyUser_user_id: Long):Boolean
    fun existsByMonthAndCompanyUserUser(createSalary_month: Int, companyUser_user: User): Boolean
    @Query("select s from Salary s join s.companyUser u where u.user.id = :id")
    fun findAllByCompanyUserUserId(@Param("id") id: Long): List<Salary>

    @Query("select s from Salary s where s.companyUser.user.id = :id")
    fun searchByCompanyUserUserOrId(id: Long,pageable: Pageable): Page<Salary>

//    fun findByCreateSalaryBetween(
//        createSalary: Date, createSalary2: Date, pageable: Pageable
//    ) : Page<Salary>

    fun findByCompanyUserUserAndCreateSalaryBetween(
        companyUser_user: User, createSalary: Date, createSalary2: Date, pageable: Pageable
    ) : Page<Salary>

}