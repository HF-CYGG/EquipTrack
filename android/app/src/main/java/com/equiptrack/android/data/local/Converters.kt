package com.equiptrack.android.data.local

import androidx.room.TypeConverter
import com.equiptrack.android.data.model.BorrowStatus
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus
import java.util.Date

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.displayName
    }
    
    @TypeConverter
    fun toUserRole(roleString: String): UserRole {
        return UserRole.values().find { it.displayName == roleString } ?: UserRole.NORMAL_USER
    }
    
    @TypeConverter
    fun fromUserStatus(status: UserStatus): String {
        return status.displayName
    }
    
    @TypeConverter
    fun toUserStatus(statusString: String): UserStatus {
        return UserStatus.values().find { it.displayName == statusString } ?: UserStatus.NORMAL
    }
    
    @TypeConverter
    fun fromBorrowStatus(status: BorrowStatus): String {
        return status.displayName
    }
    
    @TypeConverter
    fun toBorrowStatus(statusString: String): BorrowStatus {
        return BorrowStatus.values().find { it.displayName == statusString } ?: BorrowStatus.BORROWING
    }
}