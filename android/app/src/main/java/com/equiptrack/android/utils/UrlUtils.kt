package com.equiptrack.android.utils

import android.util.Log
import java.net.URI

object UrlUtils {
    fun resolveImageUrl(baseUrl: String, imagePath: String?): String? {
        if (imagePath.isNullOrEmpty()) return null
        
        // Check if it's a local file URI or content URI (from camera/gallery selection)
        if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
            return imagePath
        }
        
        // Check if it's already a full URL
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath
        }

        // Check if it's a data URI (Base64)
        if (imagePath.startsWith("data:image")) {
            // Remove newlines/returns which might be present if Base64.DEFAULT was used
            return imagePath.replace("\n", "").replace("\r", "")
        }
        
        // It's a relative path (e.g. /uploads/items/...)
        // Ensure baseUrl doesn't end with / and path starts with /
        var cleanBase = baseUrl.trim()
            .replace("127.0.0.1", "10.0.2.2")
            .replace("localhost", "10.0.2.2")
            .trimEnd('/')
        
        // Ensure cleanBase starts with http:// or https://
        if (!cleanBase.startsWith("http://") && !cleanBase.startsWith("https://")) {
            cleanBase = "http://$cleanBase"
        }

        cleanBase = try {
            val uri = URI(cleanBase)
            val host = uri.host ?: return null
            val scheme = uri.scheme ?: "http"
            val port = if (uri.port == -1) 3000 else uri.port
            "$scheme://$host:$port"
        } catch (_: Exception) {
            cleanBase
        }
        
        val cleanPath = if (imagePath.startsWith("/")) imagePath else "/$imagePath"
        
        val fullUrl = "$cleanBase$cleanPath"
        Log.d("UrlUtils", "Resolved image URL: $fullUrl")
        return fullUrl
    }
}
