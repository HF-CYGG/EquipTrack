package com.equiptrack.android.ui.department.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton

@Composable
fun AddEditDepartmentDialog(
    department: Department?,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var departmentName by remember { mutableStateOf(department?.name ?: "") }
    var requiresApproval by remember { mutableStateOf(department?.requiresApproval ?: true) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current
    val isEditing = department != null
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEditing) "编辑部门" else "添加部门",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                
                                // Validate and submit
                                val trimmedName = departmentName.trim()
                                if (trimmedName.isBlank()) {
                                    nameError = "请输入部门名称"
                                    return@KeyboardActions
                                }
                                
                                if (trimmedName.length < 2) {
                                    nameError = "部门名称至少需要2个字符"
                                    return@KeyboardActions
                                }
                                
                                if (trimmedName.length > 50) {
                                    nameError = "部门名称不能超过50个字符"
                                    return@KeyboardActions
                                }
                                
                                onConfirm(trimmedName, requiresApproval)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (isEditing) {
                        Text(
                            text = "注意：修改部门名称将同步更新该部门下所有用户的部门信息。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Approval switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { requiresApproval = !requiresApproval }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "借用需要审批", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (requiresApproval) "该部门物资借用需要审批" else "该部门物资可直接借用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = requiresApproval, onCheckedChange = { requiresApproval = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                // Validate form
                                val trimmedName = departmentName.trim()
                                
                                if (trimmedName.isBlank()) {
                                    nameError = "请输入部门名称"
                                    return@AnimatedButton
                                }
                                
                                if (trimmedName.length < 2) {
                                    nameError = "部门名称至少需要2个字符"
                                    return@AnimatedButton
                                }
                                
                                if (trimmedName.length > 50) {
                                    nameError = "部门名称不能超过50个字符"
                                    return@AnimatedButton
                                }
                                
                                onConfirm(trimmedName, requiresApproval)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEditing) "更新" else "添加")
                        }
                    }
                }
            }
        }
    }
}