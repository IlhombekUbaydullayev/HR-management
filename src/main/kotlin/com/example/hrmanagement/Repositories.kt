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
}

interface CompanyRoleRepository : BaseRepository<CompanyRole>

interface CompanyUserRepository : BaseRepository<CompanyUser> {
}

interface UserRepository : BaseRepository<User>{
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String) : Boolean
    fun existsByEmailAndDeletedFalse(email: String) : Boolean
    fun findByIdAndDeletedFalse(id: Long) : Optional<User>
}