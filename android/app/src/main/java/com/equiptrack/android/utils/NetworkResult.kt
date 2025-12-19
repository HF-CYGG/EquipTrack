package com.equiptrack.android.utils

import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : NetworkResult<T>(data)
    class Error<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)
    class Loading<T> : NetworkResult<T>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("响应体为空")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            var errorMessage = "请求失败: ${response.code()} ${response.message()}"
            
            if (!errorBody.isNullOrEmpty()) {
                try {
                    val json = org.json.JSONObject(errorBody)
                    if (json.has("message")) {
                        errorMessage = json.getString("message")
                    }
                } catch (e: Exception) {
                    // 尝试直接使用 errorBody 如果它不是 JSON
                    // errorMessage = errorBody 
                    // 但通常 errorBody 可能是 HTML (nginx error page)，所以还是保守一点只解析 JSON
                }
            }
            if (response.code() == 401) {
                errorMessage = "登录已过期，请重新登录"
            }
            NetworkResult.Error(errorMessage)
        }
    } catch (e: Exception) {
        val msg = when (e) {
            is ConnectException -> "无法连接到服务器，请检查网络或服务器配置"
            is SocketTimeoutException -> "连接超时，请检查网络或服务器配置"
            is UnknownHostException -> "无法解析服务器地址，请检查服务器配置"
            else -> "网络错误: ${e.message}"
        }
        NetworkResult.Error(msg)
    }
}
