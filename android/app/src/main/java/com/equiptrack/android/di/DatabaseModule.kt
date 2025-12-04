package com.equiptrack.android.di

import android.content.Context
import androidx.room.Room
import com.equiptrack.android.data.local.EquipTrackDatabase
import com.equiptrack.android.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideEquipTrackDatabase(
        @ApplicationContext context: Context
    ): EquipTrackDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            EquipTrackDatabase::class.java,
            "equiptrack_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideDepartmentDao(database: EquipTrackDatabase): DepartmentDao {
        return database.departmentDao()
    }
    
    @Provides
    fun provideCategoryDao(database: EquipTrackDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideEquipmentItemDao(database: EquipTrackDatabase): EquipmentItemDao {
        return database.equipmentItemDao()
    }
    
    @Provides
    fun provideUserDao(database: EquipTrackDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideRegistrationRequestDao(database: EquipTrackDatabase): RegistrationRequestDao {
        return database.registrationRequestDao()
    }
    
    @Provides
    fun provideBorrowHistoryDao(database: EquipTrackDatabase): BorrowHistoryDao {
        return database.borrowHistoryDao()
    }
}