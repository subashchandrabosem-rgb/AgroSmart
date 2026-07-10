package com.example.network

import com.example.models.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class OtpRequest(val email: String, val phone: String)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val phone: String, val role: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(val name: String, val email: String, val phone: String, val role: String)

@JsonClass(generateAdapter = true)
data class AuthResponse(val success: Boolean, val message: String, val user: User?, val token: String?)

@JsonClass(generateAdapter = true)
data class BaseResponse(val success: Boolean, val message: String)

@JsonClass(generateAdapter = true)
data class WeatherApiResponse(
    val main: WeatherMain,
    val weather: List<WeatherDescription>,
    val name: String
)

@JsonClass(generateAdapter = true)
data class WeatherMain(val temp: Double, val humidity: Int)

@JsonClass(generateAdapter = true)
data class WeatherDescription(val description: String, val main: String)

interface AgriApiService {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): BaseResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("products/getProducts")
    suspend fun getProducts(): List<Product>

    @POST("products/addProduct")
    suspend fun addProduct(@Body product: Product): BaseResponse

    @POST("products/updateProduct")
    suspend fun updateProduct(@Body product: Product): BaseResponse

    @DELETE("products/deleteProduct")
    suspend fun deleteProduct(@Query("id") id: Int): BaseResponse

    @POST("orders/createOrder")
    suspend fun createOrder(@Body order: Order): BaseResponse

    @GET("orders/getOrders")
    suspend fun getOrders(@Query("userId") userId: Int, @Query("role") role: String): List<Order>

    @POST("orders/updateOrder")
    suspend fun updateOrder(@Query("orderId") orderId: String, @Query("status") status: String): BaseResponse

    @GET("https://api.openweathermap.org/data/2.5/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String = com.example.BuildConfig.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric"
    ): WeatherApiResponse

    companion object {
        private const val BASE_URL = "https://api.agrosmart.com/"

        fun create(): AgriApiService {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AgriApiService::class.java)
        }
    }
}
