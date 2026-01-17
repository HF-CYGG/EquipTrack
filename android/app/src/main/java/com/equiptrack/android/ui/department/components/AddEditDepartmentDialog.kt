package com.equiptrack.android.ui.department.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.Department

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDepartmentDialog(
    department: Department?,
    availableDepartments: List<Department>,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit
) {
    var departmentName by remember { mutableStateOf(department?.name ?: "") }
    var requiresApproval by remember { mutableStateOf(department?.requiresApproval ?: true) }
    var selectedParentId by remember { mutableStateOf(department?.parentId) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var parentExpanded by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val isEditing = department != null
    
    // Filter out self and circular dependencies (simple check: exclude self)
    val validParents = remember(availableDepartments, department) {
        if (department == null) availableDepartments
        else availableDepartments.filter { it.id != department.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "编辑部门" else "添加部门",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Department name field
                OutlinedTextField(
                    value = departmentName,
                    onValueChange = {
                        departmentName = it
                        nameError = null
                    },
                    label = { Text("部门名称") },
                    placeholder = { Text("请输入部门名称") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                // Parent Department Selection
                ExposedDropdownMenuBox(
                    expanded = parentExpanded,
                    onExpandedChange = { parentExpanded = !parentExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = validParents.find { it.id == selectedParentId }?.name ?: "无 (顶级部门)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("上级部门") },
                        leadingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    ExposedDropdownMenu(
                        expanded = parentExpanded,
                        onDismissRequest = { parentExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("无 (顶级部门)") },
                            onClick = {
                                selectedParentId = null
                                parentExpanded = false
                            }
                        )
                        validParents.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text(parent.name) },
                                onClick = {
                                    selectedParentId = parent.id
                                    parentExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (isEditing) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "注意：修改部门名称将同步更新该部门下所有用户的部门信息。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                // Approval switch
                Surface(
                    onClick = { requiresApproval = !requiresApproval },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "借用审批",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (requiresApproval) "需管理员审批" else "无需审批直接借用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requiresApproval,
                            onCheckedChange = { requiresApproval = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate form
                    val trimmedName = departmentName.trim()
                    
                    if (trimmedName.isBlank()) {
                        nameError = "请输入部门名称"
                        return@Button
                    }
                    
                    if (trimmedName.length < 2) {
                        nameError = "部门名称至少需要2个字符"
                        return@Button
                    }
                    
                    if (trimmedName.length > 50) {
                        nameError = "部门名称不能超过50个字符"
                        return@Button
                    }
                    
                    onConfirm(trimmedName, requiresApproval, selectedParentId)
                }
            ) {
                Text(if (isEditing) "更新" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
