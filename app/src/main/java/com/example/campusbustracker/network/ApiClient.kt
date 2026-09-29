package com.example.campusbustracker.network

import com.example.campusbustracker.config.AppConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BusTrackerApi {
    @POST("api/scan")
    suspend fun submitScan(@Body request: ScanRequest): Response<ScanResponse>

    @POST("api/checkin-manual")
    suspend fun submitManualCheckin(@Body request: ManualCheckinRequest): Response<ManualCheckinResponse>

    @GET("api/status")
    suspend fun getStatus(): Response<StatusResponse>

    @GET("api/routes")
    suspend fun getRoutes(): Response<RoutesResponse>

    @GET("api/analytics")
    suspend fun getAnalytics(): Response<AnalyticsResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>
}

object ApiClient {
    val api: BusTrackerApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BusTrackerApi::class.java)
    }
}
