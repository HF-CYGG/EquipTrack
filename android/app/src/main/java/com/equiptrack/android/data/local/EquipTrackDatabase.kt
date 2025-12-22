package com.equiptrack.android.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.equiptrack.android.data.local.dao.*
import com.equiptrack.android.data.model.*

@Database(
    entities = [
        Department::class,
        Category::class,
        EquipmentItem::class,
        User::class,
        RegistrationRequest::class,
        BorrowHistoryEntry::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EquipTrackDatabase : RoomDatabase() {
    
    abstract fun departmentDao(): DepartmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun equipmentItemDao(): EquipmentItemDao
    abstract fun userDao(): UserDao
    abstract fun registrationRequestDao(): RegistrationRequestDao
    abstract fun borrowHistoryDao(): BorrowHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: EquipTrackDatabase? = null
        
        fun getDatabase(context: Context): EquipTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EquipTrackDatabase::class.java,
                    "equiptrack_database_v2"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
