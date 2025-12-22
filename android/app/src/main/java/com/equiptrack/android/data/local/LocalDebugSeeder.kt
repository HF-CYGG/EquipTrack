package com.equiptrack.android.data.local

import com.equiptrack.android.data.local.dao.DepartmentDao
import com.equiptrack.android.data.local.dao.CategoryDao
import com.equiptrack.android.data.local.dao.EquipmentItemDao
import com.equiptrack.android.data.local.dao.BorrowHistoryDao
import com.equiptrack.android.data.local.dao.UserDao
import com.equiptrack.android.data.local.EquipTrackDatabase
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.Category
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.data.model.BorrowHistoryEntry
import com.equiptrack.android.data.model.BorrowStatus
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDebugSeeder @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val database: EquipTrackDatabase,
    private val userDao: UserDao,
    private val departmentDao: DepartmentDao,
    private val categoryDao: CategoryDao,
    private val equipmentItemDao: EquipmentItemDao,
    private val borrowHistoryDao: BorrowHistoryDao
) {
    suspend fun seedIfLocalDebug() = withContext(Dispatchers.IO) {
        if (!settingsRepository.isLocalDebug()) return@withContext

        // Ensure default departments
        val deptId = "dept-default"
        val deptName = "默认部门"
        val existingDept = departmentDao.getDepartmentById(deptId)
        if (existingDept == null) {
            departmentDao.insertDepartment(Department(id = deptId, name = deptName))
        }
        val deptId2 = "dept-math"
        val deptName2 = "教务处"
        if (departmentDao.getDepartmentById(deptId2) == null) {
            departmentDao.insertDepartment(Department(id = deptId2, name = deptName2))
        }
        
        // Add sub-department for hierarchy testing
        val deptId3 = "dept-dev"
        val deptName3 = "研发部"
        if (departmentDao.getDepartmentById(deptId3) == null) {
            departmentDao.insertDepartment(Department(id = deptId3, name = deptName3, parentId = deptId))
        }

        // Seed users if missing
        ensureUser(
            contact = "admin",
            user = User(
                id = "u-admin",
                name = "王管理",
                contact = "admin",
                departmentId = deptId,
                departmentName = deptName,
                role = UserRole.ADMIN,
                status = UserStatus.NORMAL,
                password = "admin"
            )
        )

        ensureUser(
            contact = "advanced",
            user = User(
                id = "u-adv",
                name = "张老师",
                contact = "advanced",
                departmentId = deptId2,
                departmentName = deptName2,
                role = UserRole.ADVANCED_USER,
                status = UserStatus.NORMAL,
                password = "advanced"
            )
        )

        ensureUser(
            contact = "user",
            user = User(
                id = "u-user",
                name = "李四",
                contact = "user",
                departmentId = deptId,
                departmentName = deptName,
                role = UserRole.NORMAL_USER,
                status = UserStatus.NORMAL,
                password = "user"
            )
        )

        ensureUser(
            contact = "furry",
            user = User(
                id = "u-super",
                name = "福瑞",
                contact = "furry",
                departmentId = deptId,
                departmentName = deptName,
                role = UserRole.SUPER_ADMIN,
                status = UserStatus.NORMAL,
                password = "yyh"
            )
        )

        // Seed categories
        ensureCategory("cat-laptop", Category(id = "cat-laptop", name = "笔记本电脑", color = "#2E86C1"))
        ensureCategory("cat-camera", Category(id = "cat-camera", name = "相机设备", color = "#27AE60"))
        ensureCategory("cat-tools", Category(id = "cat-tools", name = "工具器材", color = "#8E44AD"))

        // Seed equipment items
        ensureItem(
            id = "item-laptop-001",
            item = EquipmentItem(
                id = "item-laptop-001",
                name = "联想 ThinkPad X1",
                categoryId = "cat-laptop",
                departmentId = deptId,
                description = "轻薄高性能办公笔记本",
                image = null,
                quantity = 10,
                availableQuantity = 8
            )
        )
        ensureItem(
            id = "item-camera-001",
            item = EquipmentItem(
                id = "item-camera-001",
                name = "索尼 A7M4",
                categoryId = "cat-camera",
                departmentId = deptId2,
                description = "全画幅微单相机",
                image = null,
                quantity = 5,
                availableQuantity = 4
            )
        )
        ensureItem(
            id = "item-tool-001",
            item = EquipmentItem(
                id = "item-tool-001",
                name = "博世电钻",
                categoryId = "cat-tools",
                departmentId = deptId,
                description = "多功能电动工具",
                image = null,
                quantity = 12,
                availableQuantity = 12
            )
        )

        // Seed borrow history examples
        ensureBorrow(
            id = "bh-001",
            history = BorrowHistoryEntry(
                id = "bh-001",
                itemId = "item-laptop-001",
                itemName = "联想 ThinkPad X1",
                departmentId = deptId,
                borrowerName = "李四",
                borrowerContact = "user",
                operatorUserId = "user-001",
                operatorName = "管理员",
                operatorContact = "admin@example.com",
                borrowDate = Date(System.currentTimeMillis() - 7L*24*3600*1000),
                expectedReturnDate = Date(System.currentTimeMillis() + 7L*24*3600*1000),
                returnDate = null,
                status = BorrowStatus.BORROWING,
                forcedReturnBy = null
            )
        )
        ensureBorrow(
            id = "bh-002",
            history = BorrowHistoryEntry(
                id = "bh-002",
                itemId = "item-camera-001",
                itemName = "索尼 A7M4",
                departmentId = deptId2,
                borrowerName = "张老师",
                borrowerContact = "advanced",
                operatorUserId = "user-002",
                operatorName = "高级用户",
                operatorContact = "advanced@example.com",
                borrowDate = Date(System.currentTimeMillis() - 30L*24*3600*1000),
                expectedReturnDate = Date(System.currentTimeMillis() - 3L*24*3600*1000),
                returnDate = Date(System.currentTimeMillis() - 1L*24*3600*1000),
                status = BorrowStatus.OVERDUE_RETURNED,
                forcedReturnBy = "王管理"
            )
        )
    }

    suspend fun resetLocalSeed() = withContext(Dispatchers.IO) {
        clearAllData()
        seedIfLocalDebug()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }

    private suspend fun ensureUser(contact: String, user: User) {
        val existing = userDao.getUserByContact(contact)
        if (existing == null) {
            userDao.insertUser(user)
        } else if (existing.password != user.password) {
            // Keep data fresh for local debug
            userDao.updateUserPassword(existing.id, user.password ?: "")
        }
    }

    private suspend fun ensureCategory(id: String, category: Category) {
        val existing = categoryDao.getCategoryById(id)
        if (existing == null) categoryDao.insertCategory(category)
    }

    private suspend fun ensureItem(id: String, item: EquipmentItem) {
        val existing = equipmentItemDao.getItemById(id)
        if (existing == null) equipmentItemDao.insertItem(item)
    }

    private suspend fun ensureBorrow(id: String, history: BorrowHistoryEntry) {
        val existing = borrowHistoryDao.getHistoryById(id)
        if (existing == null) borrowHistoryDao.insertHistory(history)
    }
}
