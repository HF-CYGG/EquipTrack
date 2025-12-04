package com.equiptrack.android.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.rememberToastState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val toastState = rememberToastState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var departmentExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Observe signup result
    LaunchedEffect(Unit) {
        viewModel.signupResult.collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showSuccessDialog = true
                }
                is NetworkResult.Error -> {
                    toastState.showError(result.message ?: "注册失败")
                }
                is NetworkResult.Loading -> {
                    // Loading is handled in the UI state
                }
            }
        }
    }

    if (showSuccessDialog) {
        Dialog(
            onDismissRequest = {
                showSuccessDialog = false
                onSignupSuccess()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showSuccessDialog = false
                            onSignupSuccess()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "提交成功",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "您的注册申请已提交，请等待管理员审核。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            AnimatedTextButton(
                                onClick = {
                                    showSuccessDialog = false
                                    onSignupSuccess()
                                }
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("注册申请") },
                    navigationIcon = {
                        AnimatedIconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Form card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "填写注册信息",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Text(
                                text = "请填写以下信息提交注册申请，管理员审核通过后即可使用系统。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Name field
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = {
                                    viewModel.updateName(it)
                                    viewModel.clearErrors()
                                },
                                label = { Text("姓名") },
                                placeholder = { Text("请输入真实姓名") },
                                isError = uiState.nameError != null,
                                supportingText = uiState.nameError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        
                        // Contact field
                        OutlinedTextField(
                            value = uiState.contact,
                            onValueChange = {
                                viewModel.updateContact(it)
                                viewModel.clearErrors()
                            },
                            label = { Text("联系方式") },
                            placeholder = { Text("请输入手机号或邮箱") },
                            isError = uiState.contactError != null,
                            supportingText = uiState.contactError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Department name field (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = departmentExpanded,
                            onExpandedChange = { departmentExpanded = !departmentExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.departmentName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("部门名称") },
                                placeholder = { Text("请选择所属部门") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                                isError = uiState.departmentNameError != null,
                                supportingText = uiState.departmentNameError?.let { { Text(it) } },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = departmentExpanded,
                                onDismissRequest = { departmentExpanded = false }
                            ) {
                                if (departments.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("加载中或无可用部门") },
                                        onClick = { departmentExpanded = false }
                                    )
                                } else {
                                    departments.forEach { department ->
                                        DropdownMenuItem(
                                            text = { Text(department.name) },
                                            onClick = {
                                                viewModel.updateDepartmentName(department.name)
                                                viewModel.clearErrors()
                                                departmentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Password field
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = {
                                viewModel.updatePassword(it)
                                viewModel.clearErrors()
                            },
                            label = { Text("密码") },
                            placeholder = { Text("请输入密码（至少6位）") },
                            isError = uiState.passwordError != null,
                            supportingText = uiState.passwordError?.let { { Text(it) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                AnimatedIconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Confirm password field
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = {
                                viewModel.updateConfirmPassword(it)
                                viewModel.clearErrors()
                            },
                            label = { Text("确认密码") },
                            placeholder = { Text("请再次输入密码") },
                            isError = uiState.confirmPasswordError != null,
                            supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                AnimatedIconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Invitation code field
                        OutlinedTextField(
                            value = uiState.invitationCode,
                            onValueChange = {
                                viewModel.updateInvitationCode(it)
                                viewModel.clearErrors()
                            },
                            label = { Text("邀请码") },
                            placeholder = { Text("请输入邀请码") },
                            isError = uiState.invitationCodeError != null,
                            supportingText = uiState.invitationCodeError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.signup()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Submit button
                        AnimatedButton(
                            onClick = { viewModel.signup() },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("提交申请")
                        }
                    }
                }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    ToastMessage(
        toastData = toastState.currentToast,
        onDismiss = { toastState.dismiss() },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
    )
    }
}
}