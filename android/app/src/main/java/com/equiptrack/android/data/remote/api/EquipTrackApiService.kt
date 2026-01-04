package com.equiptrack.android.data.remote.api

import com.equiptrack.android.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface EquipTrackApiService {
    
    // Authentication endpoints
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("api/signup")
    suspend fun signup(@Body request: SignupRequest): Response<ApiResponse<String>>
    
    @POST("api/notifications/register")
    suspend fun registerDeviceToken(@Body body: Map<String, String>): Response<ApiResponse<Boolean>>
    
    // System
    @GET("api/system/android-version")
    suspend fun getAppVersion(): Response<AppVersion>

    // Public Data
    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Query("type") type: String = "item"
    ): Response<Map<String, String>>

    // Department endpoints
    @GET("api/departments")
    suspend fun getDepartments(): Response<List<Department>>
    
    @POST("api/departments")
    suspend fun createDepartment(@Body department: Department): Response<Department>
    
    @PUT("api/departments/{id}")
    suspend fun updateDepartment(
        @Path("id") id: String,
        @Body department: Department
    ): Response<Department>
    
    @DELETE("api/departments/{id}")
    suspend fun deleteDepartment(@Path("id") id: String): Response<ApiResponse<String>>
    
    // Category endpoints
    @GET("api/categories")
    suspend fun getCategories(): Response<List<Category>>
    
    @POST("api/categories")
    suspend fun createCategory(@Body category: Category): Response<Category>
    
    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<ApiResponse<String>>
    
    // Equipment Item endpoints
    @GET("api/items")
    suspend fun getItems(
        @Query("userRole") userRole: String,
        @Query("departmentId") departmentId: String? = null,
        @Query("allAvailable") allAvailable: Boolean? = null
    ): Response<List<EquipmentItem>>
    
    @GET("api/items/{id}")
    suspend fun getItemById(@Path("id") id: String): Response<EquipmentItem>
    
    @POST("api/items")
    suspend fun createItem(@Body item: EquipmentItem): Response<EquipmentItem>
    
    @PUT("api/items/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Body item: EquipmentItem
    ): Response<EquipmentItem>
    
    @DELETE("api/items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<ApiResponse<String>>
    
    @POST("api/items/{id}/borrow")
    suspend fun borrowItem(
        @Path("id") id: String,
        @Body request: BorrowRequest
    ): Response<EquipmentItem>
    
    @POST("api/items/{itemId}/return/{historyEntryId}")
    suspend fun returnItem(
        @Path("itemId") itemId: String,
        @Path("historyEntryId") historyEntryId: String,
        @Body request: ReturnRequest
    ): Response<EquipmentItem>
    
    // User endpoints
    @GET("api/users")
    suspend fun getUsers(
        @Query("userRole") userRole: String,
        @Query("departmentId") departmentId: String? = null
    ): Response<List<User>>
    
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<User>
    
    @POST("api/users")
    suspend fun createUser(@Body user: User): Response<User>
    
    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body user: User
    ): Response<User>
    
    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<User>
    
    // Registration approval endpoints
    @GET("api/approvals")
    suspend fun getRegistrationRequests(
        @Query("userId") userId: String,
        @Query("userRole") userRole: String,
        @Query("departmentId") departmentId: String
    ): Response<List<RegistrationRequest>>
    
    @POST("api/approvals/{requestId}")
    suspend fun approveRegistration(@Path("requestId") requestId: String): Response<User>
    
    @DELETE("api/approvals/{requestId}")
    suspend fun rejectRegistration(@Path("requestId") requestId: String): Response<ApiResponse<String>>
    
    // History endpoints
    @GET("api/history")
    suspend fun getBorrowHistory(
        @Query("userRole") userRole: String,
        @Query("departmentId") departmentId: String? = null
    ): Response<List<BorrowHistoryEntry>>

    // Borrow request approval endpoints
    @POST("api/borrow-requests")
    suspend fun createBorrowRequest(
        @Body request: BorrowRequestCreateRequest
    ): Response<BorrowRequestEntry>

    @GET("api/borrow-requests/mine")
    suspend fun getMyBorrowRequests(): Response<List<BorrowRequestEntry>>

    // Review endpoints
    @GET("api/borrow-requests/review")
    suspend fun getBorrowReviewRequests(
        @Query("status") status: String? = null
    ): Response<List<BorrowRequestEntry>>

    @POST("api/borrow-requests/{id}/approve")
    suspend fun approveBorrowRequest(
        @Path("id") requestId: String,
        @Body request: BorrowReviewActionRequest
    ): Response<BorrowRequestEntry>

    @POST("api/borrow-requests/{id}/reject")
    suspend fun rejectBorrowRequest(
        @Path("id") requestId: String,
        @Body request: BorrowReviewActionRequest
    ): Response<BorrowRequestEntry>
}
