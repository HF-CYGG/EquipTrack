package com.equiptrack.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.utils.getDepartmentPath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentFilter(
    departments: List<Department>,
    selectedDepartmentId: String?,
    onDepartmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDepartment = departments.find { it.id == selectedDepartmentId }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        FilterChip(
            selected = selectedDepartmentId != null,
            onClick = { expanded = true },
            label = { Text(if (selectedDepartment != null) getDepartmentPath(selectedDepartment.id, departments) else "所有部门") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (selectedDepartmentId != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("所有部门") },
                onClick = {
                    onDepartmentSelected(null)
                    expanded = false
                }
            )
            departments.forEach { department ->
                DropdownMenuItem(
                    text = { Text(getDepartmentPath(department.id, departments)) },
                    onClick = {
                        onDepartmentSelected(department.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
