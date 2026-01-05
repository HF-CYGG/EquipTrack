package com.equiptrack.android.utils

import com.equiptrack.android.data.model.Department

fun getDepartmentPath(deptId: String?, departments: List<Department>): String {
    if (deptId == null) return ""
    val path = mutableListOf<String>()
    var currentId: String? = deptId
    // Prevent infinite loops if there's a circular reference (though UI prevents creating them)
    val visited = mutableSetOf<String>()
    
    while (currentId != null && !visited.contains(currentId)) {
        visited.add(currentId)
        val dept = departments.find { it.id == currentId }
        if (dept != null) {
            path.add(0, dept.name)
            currentId = dept.parentId
        } else {
            break
        }
    }
    return if (path.isEmpty()) "" else path.joinToString(" > ")
}
