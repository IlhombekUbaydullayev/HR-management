package com.example.hrmanagement

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
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
    fun findByName(name: String):Optional<CompanyRole>
}

interface CompanyUserRepository : BaseRepository<CompanyUser> {
//    fun findAllByWorkspaceNameAndDeletedFalse(workspace_name: String) : List<CompanyUser>
    fun findAllByWorkspaceNameAndDeletedFalseAndUserSystemRoleName(
    workspace_name: String,
    user_systemRoleName: CompanyRoleName
) : List<CompanyUser>
    fun findByUserEmail(user_email: String) : Optional<CompanyUser>
}

interface UserRepository : BaseRepository<User>{
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String) : Boolean
    fun existsByEmailAndDeletedFalse(email: String) : Boolean
    fun findByIdAndDeletedFalse(id: Long) : Optional<User>
}

interface TaskRepository : BaseRepository<Task>{
    fun getAllByDeletedFalse():List<Task>
}