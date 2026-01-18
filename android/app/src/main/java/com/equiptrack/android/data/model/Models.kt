package com.equiptrack.android.data.model

import androidx.compose.runtime.Immutable
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.IgnoredOnParcel
import java.util.Date
import com.google.gson.annotations.SerializedName

/**
 * 部门实体类
 * 对应 departments 表
 */
@Immutable
@Parcelize
@Entity(tableName = "departments")
data class Department(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentId: String? = null,
    val requiresApproval: Boolean = true, // 默认借用该部门物资需要审批
    val order: Int = 0 // 排序权重
) : Parcelable

/**
 * 部门结构更新请求体
 */
@Parcelize
data class DepartmentStructureUpdate(
    val id: String,
    val parentId: String?,
    val order: Int
) : Parcelable

/**
 * 物资分类实体类
 * 对应 categories 表
 */
@Immutable
@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String // 16进制颜色字符串 (如 #FF5733)
) : Parcelable

/**
 * 物资状态枚举
 */
enum class EquipmentStatus(val displayName: String) {
    Available("可借用"),
    Borrowed("已借出")
}

/**
 * 物资实体类
 * 对应 equipment_items 表
 */
@Immutable
@Parcelize
@Entity(
    tableName = "equipment_items",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["departmentId"]),
        Index(value = ["name"])
    ]
)
data class EquipmentItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String,
    val departmentId: String,
    val description: String? = null,
    val image: String? = null, // 缩略图 URL
    val imageFull: String? = null, // 高清图 URL
    val quantity: Int, // 总数量
    val availableQuantity: Int, // 当前可用数量
    val pendingApprovalQuantity: Int = 0, // 待审批的借用数量
    val requiresApproval: Boolean = true, // 借用是否需要审批
    val borrowPhoto: String? = null, // 借用时拍摄的照片 Data URI
    val lastReturnPhoto: String? = null // 上次归还的照片 Data URI
) : Parcelable {
    // 根据可用数量判断状态
    val status: EquipmentStatus
        get() = if (availableQuantity > 0) EquipmentStatus.Available else EquipmentStatus.Borrowed

    @Ignore
    @IgnoredOnParcel
    var borrowHistory: List<BorrowHistoryDto> = emptyList()
}

/**
 * 用户角色枚举
 * 定义了用户的权限等级
 */
enum class UserRole(val displayName: String) {
    @SerializedName("超级管理员")
    SUPER_ADMIN("超级管理员"), // 最高权限：管理所有部门、用户、配置
    @SerializedName("管理员")
    ADMIN("管理员"), // 部门级权限：管理本部门物资、人员审批
    @SerializedName("高级用户")
    ADVANCED_USER("高级用户"), // 可直接借用物资，无需审批（视配置而定），可协助归还
    @SerializedName("普通用户")
    NORMAL_USER("普通用户") // 只能发起借用申请，需审批
}

/**
 * 用户状态枚举
 */
enum class UserStatus(val displayName: String) {
    @SerializedName("active", alternate = ["正常"])
    NORMAL("正常"),
    @SerializedName("banned", alternate = ["封禁"])
    BANNED("封禁")
}

/**
 * 用户实体类
 * 对应 users 表
 */
@Immutable
@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val contact: String, // 用作登录账号 (手机号/邮箱)
    val departmentId: String,
    val departmentName: String? = null,
    val role: UserRole,
    val status: UserStatus,
    val password: String? = null, // 哈希后的密码
    val invitationCode: String? = null, // 注册邀请码
    val avatarUrl: String? = null,
    val banReason: String? = null // 封禁原因
) : Parcelable

/**
 * 注册申请实体类
 * 对应 registration_requests 表 (本地缓存用)
 */
@Immutable
@Parcelize
@Entity(tableName = "registration_requests")
data class RegistrationRequest(
    @PrimaryKey
    val id: String,
    val name: String,
    val contact: String,
    val departmentName: String? = null,
    val password: String? = null,
    val invitationCode: String? = null,
    @SerializedName("createdAt")
    val requestDate: Date,
    @SerializedName("invitedByUserId")
    val invitedBy: String? = null, // 邀请人的 ID
    val departmentId: String? = null,
    val status: String = "pending" // pending, approved, rejected
) : Parcelable

/**
 * 借还状态枚举
 */
enum class BorrowStatus(val displayName: String) {
    @SerializedName("已归还")
    RETURNED("已归还"),
    @SerializedName("逾期归还")
    OVERDUE_RETURNED("逾期归还"),
    @SerializedName("借用中")
    BORROWING("借用中"),
    @SerializedName("逾期未归还")
    OVERDUE_NOT_RETURNED("逾期未归还"),
    @SerializedName("pending")
    PENDING("审核中"),
    @SerializedName("approved")
    APPROVED("已批准"),
    @SerializedName("rejected")
    REJECTED("已拒绝")
}

/**
 * 借还历史记录实体类
 * 对应 borrow_history 表 (本地缓存用)
 */
@Immutable
@Parcelize
@Entity(tableName = "borrow_history")
data class BorrowHistoryEntry(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemName: String,
    val departmentId: String,
    val borrowerName: String,
    val borrowerContact: String,
    val operatorUserId: String? = null, // 操作人 ID
    val operatorName: String? = null, // 操作人姓名
    val operatorContact: String? = null, // 操作人联系方式
    val borrowDate: Date,
    val expectedReturnDate: Date,
    val returnDate: Date? = null,
    val status: BorrowStatus,
    val forcedReturnBy: String? = null, // 强制归还的管理员姓名
    val photo: String? = null, // 借用照片 URL
    val returnPhoto: String? = null, // 归还照片 URL
    val note: String? = null, // 备注
    val remark: String? = null // 审批备注
) : Parcelable

// ----------------------------------------------------------------
// API 请求与响应数据类 (DTOs)
// ----------------------------------------------------------------

@Parcelize
data class LoginRequest(
    val contact: String,
    val password: String
) : Parcelable

@Parcelize
data class LoginResponse(
    val user: User,
    val token: String
) : Parcelable

@Parcelize
data class SignupRequest(
    val name: String,
    val contact: String,
    val departmentName: String,
    val password: String,
    val invitationCode: String
) : Parcelable

@Parcelize
data class BorrowRequest(
    val borrower: Borrower,
    val expectedReturnDate: Date,
    val photo: String? = null, // Data URI 格式
    val quantity: Int = 1, // 借用数量
    val note: String? = null
) : Parcelable

@Parcelize
data class Borrower(
    val name: String,
    val phone: String,
    val id: String? = null
) : Parcelable

@Parcelize
data class ReturnRequest(
    val photo: String, // Data URI 格式
    val isForced: Boolean = false, // 是否为管理员强制归还
    val adminName: String? = null // 强制归还时的管理员姓名
) : Parcelable

@Immutable
@Parcelize
data class BorrowHistoryDto(
    val id: String,
    val itemId: String,
    val borrower: Borrower?,
    val operator: Borrower? = null,
    val borrowDate: Date,
    val expectedReturnDate: Date,
    val returnDate: Date? = null,
    val status: BorrowStatus,
    val forcedReturnBy: String? = null,
    val photo: String? = null,
    val returnPhoto: String? = null
) : Parcelable

@Immutable
@Parcelize
data class BorrowRequestEntry(
    val id: String,
    val itemId: String,
    val itemDepartmentId: String,
    val itemName: String? = null,
    val itemImage: String? = null,
    val borrower: Borrower?,
    val applicant: Borrower?,
    val expectedReturnDate: Date,
    val photo: String? = null,
    val quantity: Int,
    val note: String? = null,
    val status: String,
    val remark: String? = null,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("reviewedAt")
    val reviewedAt: Date? = null,
    val reviewer: Borrower? = null
) : Parcelable

@Parcelize
data class BorrowRequestCreateRequest(
    val itemId: String,
    val borrower: Borrower,
    val expectedReturnDate: Date,
    val photo: String? = null,
    val quantity: Int = 1,
    val note: String? = null
) : Parcelable

@Parcelize
data class BorrowReviewActionRequest(
    val remark: String? = null
) : Parcelable

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

/**
 * 应用版本信息
 */
@Immutable
@Parcelize
data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val updateContent: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val releaseDate: String,
    val updateType: String = "normal" // urgent, major, feature, normal, patch
) : Parcelable
