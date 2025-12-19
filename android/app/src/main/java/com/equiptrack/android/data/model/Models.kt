package com.equiptrack.android.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Date
import com.google.gson.annotations.SerializedName

@Parcelize
@Entity(tableName = "departments")
data class Department(
    @PrimaryKey
    val id: String,
    val name: String
) : Parcelable

@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String // hex color string
) : Parcelable

// Equipment status enum
enum class EquipmentStatus(val displayName: String) {
    Available("可借用"),
    Borrowed("已借出")
}

@Parcelize
@Entity(tableName = "equipment_items")
data class EquipmentItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String,
    val departmentId: String,
    val description: String,
    val image: String? = null,
    val imageFull: String? = null,
    val quantity: Int,
    val availableQuantity: Int,
    val borrowPhoto: String? = null, // Data URI for the photo taken on borrow
    val lastReturnPhoto: String? = null // Data URI for the photo
) : Parcelable {
    val status: EquipmentStatus
        get() = if (availableQuantity > 0) EquipmentStatus.Available else EquipmentStatus.Borrowed
}

// User roles enum
enum class UserRole(val displayName: String) {
    @SerializedName("超级管理员")
    SUPER_ADMIN("超级管理员"),
    @SerializedName("管理员")
    ADMIN("管理员"),
    @SerializedName("高级用户")
    ADVANCED_USER("高级用户"),
    @SerializedName("普通用户")
    NORMAL_USER("普通用户")
}

// User status enum
enum class UserStatus(val displayName: String) {
    @SerializedName("active", alternate = ["正常"])
    NORMAL("正常"),
    @SerializedName("banned", alternate = ["封禁"])
    BANNED("封禁")
}

@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val contact: String, // Used as email/login id
    val departmentId: String,
    val departmentName: String? = null,
    val role: UserRole,
    val status: UserStatus,
    val password: String? = null,
    val invitationCode: String? = null,
    val avatarUrl: String? = null // 新增头像字段（圆形裁剪后上传）
) : Parcelable

@Parcelize
@Entity(tableName = "registration_requests")
data class RegistrationRequest(
    @PrimaryKey
    val id: String,
    val name: String,
    val contact: String,
    val departmentName: String? = null,
    val password: String? = null,
    val invitationCode: String,
    @SerializedName("createdAt")
    val requestDate: Date,
    @SerializedName("invitedByUserId")
    val invitedBy: String, // ID of the user who's code was used
    val departmentId: String? = null,
    val status: String = "pending"
) : Parcelable

// Borrow status enum
enum class BorrowStatus(val displayName: String) {
    @SerializedName("已归还")
    RETURNED("已归还"),
    @SerializedName("逾期归还")
    OVERDUE_RETURNED("逾期归还"),
    @SerializedName("借用中")
    BORROWING("借用中"),
    @SerializedName("逾期未归还")
    OVERDUE_NOT_RETURNED("逾期未归还")
}

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
    val operatorUserId: String, // ID of the user who performed the borrow operation
    val operatorName: String, // Name of the user who performed the borrow operation
    val operatorContact: String, // Contact of the user who performed the borrow operation
    val borrowDate: Date,
    val expectedReturnDate: Date,
    val returnDate: Date? = null,
    val status: BorrowStatus,
    val forcedReturnBy: String? = null, // Name of the admin/advanced user who forced the return
    val photo: String? = null, // Add photo field to match server response if needed, or just to be safe
    val returnPhoto: String? = null // Return proof photo
) : Parcelable

// Data classes for API requests and responses
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
    val photo: String? = null, // Data URI format, optional
    val quantity: Int = 1 // Number of items to borrow
) : Parcelable

@Parcelize
data class Borrower(
    val name: String,
    val phone: String
) : Parcelable

@Parcelize
data class ReturnRequest(
    val photo: String, // Data URI format
    val isForced: Boolean = false,
    val adminName: String? = null
) : Parcelable

@Parcelize
data class ApiResponse<T>(
    val success: Boolean,
    val data: @RawValue T? = null,
    val message: String? = null
) : Parcelable
