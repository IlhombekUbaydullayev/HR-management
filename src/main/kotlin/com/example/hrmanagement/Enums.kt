package com.example.hrmanagement

enum class CompanyRoleName {
    ROLE_DIRECTOR,
    ROLE_USER,
    ROLE_MANAGER,
    ROLE_HR_MANAGER
}

enum class CompanyPermissionName(
    var names: String,
    val description: String,
    val workspaceRoleNames: List<CompanyRoleName>
) {
    CAN_ADD_HR_MANAGER(
        "Add/Remove/Update/FindById",
        "Allows the company to add managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR)
    ),
    CAN_REMOVE_HR_MANAGER(
        "CAN_REMOVE_MANAGER",
        "Allows the company to remove managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR)
    ),
    CAN_EDIT_HR_MANAGER(
        "CAN_EDIT_MANAGER",
        "Allows the company to edit managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR)
    ),
    CAN_ADD_MANAGER(
        "Add/Remove/Update/FindById",
        "Allows the company to add managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR)
    ),
    CAN_REMOVE_MANAGER(
        "CAN_REMOVE_MANAGER",
        "Allows the company to remove managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR, CompanyRoleName.ROLE_HR_MANAGER)
    ),
    CAN_EDIT_MANAGER(
        "CAN_EDIT_MANAGER",
        "Allows the company to edit managers",
        listOf(CompanyRoleName.ROLE_DIRECTOR, CompanyRoleName.ROLE_HR_MANAGER)
    ),
    CAN_ADD_USER(
        "Add/Remove/Update/FindById",
        "Allows the company to add user",
        listOf(CompanyRoleName.ROLE_DIRECTOR, CompanyRoleName.ROLE_HR_MANAGER)
    ),
    CAN_REMOVE_USER(
        "CAN_REMOVE_MANAGER",
        "Allows the company to remove user",
        listOf(CompanyRoleName.ROLE_DIRECTOR, CompanyRoleName.ROLE_HR_MANAGER)
    ),
    CAN_EDIT_USER(
        "CAN_EDIT_MANAGER",
        "Allows the company to edit user",
        listOf(CompanyRoleName.ROLE_DIRECTOR, CompanyRoleName.ROLE_HR_MANAGER)
    ),
}

enum class ErrorType(val code: Int) {
    BASE_EXCEPTION(100),
    OBJECT_NOT_FOUND(404),
    EMAIL_NOT_FOUND(404),
    ALREADY_REPORTED(208)
}

enum class ProjectStatus{
    TODO,
    DOING,
    DONE,
}

