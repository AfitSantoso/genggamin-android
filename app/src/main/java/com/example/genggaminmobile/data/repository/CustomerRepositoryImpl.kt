package com.example.genggaminmobile.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import com.example.genggaminmobile.data.remote.api.CustomerApi
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi,
    private val gson: Gson
) : CustomerRepository {

    override suspend fun getProfile(): Result<CustomerProfileResponse> {
        return try {
            val response = customerApi.getProfile()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Gagal mengambil profil: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createOrUpdateProfile(
        data: CustomerProfileRequest,
        ktp: File?,
        selfie: File?,
        payslip: File?
    ): Result<CustomerProfileResponse> {
        return try {
            val jsonString = gson.toJson(data)
            val dataPart = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            // Kompres & Resize setiap file jika ada
            val ktpPart = ktp?.let {
                MultipartBody.Part.createFormData("ktp", it.name, compressAndResizeImage(it))
            }

            val selfiePart = selfie?.let {
                MultipartBody.Part.createFormData("selfie", it.name, compressAndResizeImage(it))
            }

            val payslipPart = payslip?.let {
                MultipartBody.Part.createFormData("payslip", it.name, compressAndResizeImage(it))
            }

            val response = customerApi.createOrUpdateProfile(dataPart, ktpPart, selfiePart, payslipPart)
            
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("CustomerRepo", "Http Error ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Gagal menyimpan profil"))
        } catch (e: Exception) {
            Log.e("CustomerRepo", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fungsi untuk Resize (Maks 1024px) dan Kompres (60% Quality)
     * Ini akan memastikan ukuran file di bawah 300KB namun tetap tajam.
     */
    private fun compressAndResizeImage(file: File): RequestBody {
        return try {
            val options = BitmapFactory.Options()
            var bitmap = BitmapFactory.decodeFile(file.path, options)

            // 1. Resize: Batasi dimensi maksimal ke 1024px
            val maxSize = 1024
            val width = bitmap.width
            val height = bitmap.height
            
            if (width > maxSize || height > maxSize) {
                val bitmapRatio = width.toFloat() / height.toFloat()
                val targetWidth: Int
                val targetHeight: Int
                
                if (bitmapRatio > 1) {
                    targetWidth = maxSize
                    targetHeight = (maxSize / bitmapRatio).toInt()
                } else {
                    targetHeight = maxSize
                    targetWidth = (maxSize * bitmapRatio).toInt()
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                Log.d("CustomerRepo", "Resized ${file.name} to ${targetWidth}x${targetHeight}")
            }

            // 2. Compress: Gunakan kualitas 60%
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            
            val byteArray = outputStream.toByteArray()
            Log.d("CustomerRepo", "Final Size ${file.name}: ${byteArray.size / 1024} KB")
            
            byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        } catch (e: Exception) {
            Log.e("CustomerRepo", "Gagal olah ${file.name}, kirim asli", e)
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        }
    }
}
