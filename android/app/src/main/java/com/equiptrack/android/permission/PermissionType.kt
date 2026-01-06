package com.equiptrack.android.permission

import com.equiptrack.android.data.model.UserRole

/**
 * System permission type definitions based on user requirements.
 */
enum class PermissionType {
    // Global/Super Admin Permissions
    MANAGE_ALL_DEPARTMENTS, // Super admin only

    // Management Permissions (Admin & Super Admin)
    VIEW_REGISTRATION_APPROVALS,
    VIEW_BORROW_APPROVALS,
    VIEW_USER_MANAGEMENT,
    VIEW_DEPARTMENT_MANAGEMENT,

    // Operational Permissions (Advanced User, Admin, Super Admin)
    MANAGE_EQUIPMENT_ITEMS, // Add/Edit/Delete items
    VIEW_DEPARTMENT_HISTORY, // See borrow history of department members

    // Basic Permissions (All Users)
    BORROW_ITEMS,
    VIEW_OWN_HISTORY
}

/**
 * Mapping of roles to permissions.
 */
object RolePermissionsMatrix {
    val roleToPermissions: Map<UserRole, Set<PermissionType>> = mapOf(
        UserRole.SUPER_ADMIN to setOf(
            PermissionType.MANAGE_ALL_DEPARTMENTS,
            PermissionType.VIEW_REGISTRATION_APPROVALS,
            PermissionType.VIEW_BORROW_APPROVALS,
            PermissionType.VIEW_USER_MANAGEMENT,
            PermissionType.VIEW_DEPARTMENT_MANAGEMENT,
            PermissionType.MANAGE_EQUIPMENT_ITEMS,
            PermissionType.VIEW_DEPARTMENT_HISTORY,
            PermissionType.BORROW_ITEMS,
            PermissionType.VIEW_OWN_HISTORY
        ),
        UserRole.ADMIN to setOf(
            PermissionType.VIEW_REGISTRATION_APPROVALS,
            PermissionType.VIEW_BORROW_APPROVALS,
            PermissionType.VIEW_USER_MANAGEMENT,
            PermissionType.VIEW_DEPARTMENT_MANAGEMENT,
            PermissionType.MANAGE_EQUIPMENT_ITEMS,
            PermissionType.VIEW_DEPARTMENT_HISTORY,
            PermissionType.BORROW_ITEMS,
            PermissionType.VIEW_OWN_HISTORY
        ),
        UserRole.ADVANCED_USER to setOf(
            PermissionType.VIEW_REGISTRATION_APPROVALS,
            PermissionType.VIEW_BORROW_APPROVALS,
            PermissionType.MANAGE_EQUIPMENT_ITEMS,
            PermissionType.VIEW_DEPARTMENT_HISTORY,
            PermissionType.BORROW_ITEMS,
            PermissionType.VIEW_OWN_HISTORY
        ),
        UserRole.NORMAL_USER to setOf(
            PermissionType.BORROW_ITEMS,
            PermissionType.VIEW_OWN_HISTORY
        )
    )
}
