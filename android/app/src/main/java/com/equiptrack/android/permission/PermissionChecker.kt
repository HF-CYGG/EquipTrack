package com.equiptrack.android.permission

import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole

/**
 * Unified permission checker combining role matrix and department scope rules.
 */
object PermissionChecker {

    /**
     * Check if user has specific permission.
     * If targetDepartmentId is provided, checks if the user can operate on that specific department.
     */
    fun hasPermission(
        user: User?,
        permission: PermissionType,
        targetDepartmentId: String? = null
    ): Boolean {
        if (user == null) return false

        val role = user.role
        val basePermissions = RolePermissionsMatrix.roleToPermissions[role] ?: emptySet()
        
        // First check if the role generally has this permission
        if (permission !in basePermissions) {
             return false
        }

        // If a target department is specified, we need to check scope
        if (targetDepartmentId != null) {
            // Super Admin can operate on any department
            if (role == UserRole.SUPER_ADMIN) {
                return true
            }
            
            // For others (Admin, Advanced), they can only operate on their own department
            // This applies to management and viewing history permissions
            return when (permission) {
                PermissionType.VIEW_REGISTRATION_APPROVALS,
                PermissionType.VIEW_BORROW_APPROVALS,
                PermissionType.VIEW_USER_MANAGEMENT,
                PermissionType.VIEW_DEPARTMENT_MANAGEMENT,
                PermissionType.MANAGE_EQUIPMENT_ITEMS,
                PermissionType.VIEW_DEPARTMENT_HISTORY -> {
                    user.departmentId == targetDepartmentId
                }
                else -> true // Other permissions (like VIEW_OWN_HISTORY) might not be dept-bound in this way
            }
        }

        return true
    }
}
