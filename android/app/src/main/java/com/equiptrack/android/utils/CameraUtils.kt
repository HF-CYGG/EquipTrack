package com.equiptrack.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CameraUtils {
    
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private const val PHOTO_EXTENSION = ".jpg"
    
    /**
     * Create a file for saving the captured image
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName = "EQUIPTRACK_${timeStamp}$PHOTO_EXTENSION"
        return File(context.getExternalFilesDir(null), imageFileName)
    }
    
    /**
     * Take a photo using ImageCapture
     */
    fun takePhoto(
        imageCapture: ImageCapture,
        outputFile: File,
        context: Context,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onImageCaptured(Uri.fromFile(outputFile))
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
    
    /**
     * Convert image file to Base64 string (Data URI format)
     */
    fun imageFileToBase64(file: File, maxWidth: Int = 800, maxHeight: Int = 600): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            "data:image/jpeg;base64,${Base64.encodeToString(byteArray, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convert Uri to Base64 string (Data URI format)
     */
    fun imageUriToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            "data:image/jpeg;base64,${Base64.encodeToString(byteArray, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Resize bitmap to fit within the specified dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight, 1.0f)
        
        if (scale >= 1.0f) {
            return bitmap
        }
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
    }
    
    /**
     * Convert Base64 string to Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val base64Data = if (base64String.startsWith("data:image")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete temporary image file
     */
    fun deleteImageFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Clean up old temporary image files
     */
    fun cleanupOldImages(context: Context, maxAgeMillis: Long = 24 * 60 * 60 * 1000) {
        try {
            val directory = context.getExternalFilesDir(null)
            directory?.listFiles()?.forEach { file ->
                if (file.name.startsWith("EQUIPTRACK_") && 
                    file.name.endsWith(PHOTO_EXTENSION) &&
                    System.currentTimeMillis() - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}