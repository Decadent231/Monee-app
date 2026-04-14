package com.money.codex.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

private const val GATEWAY_BASE_URL = "http://101.201.235.13:8080/"
private const val MONEE_BASE_URL = "${GATEWAY_BASE_URL}monee/"
private const val USER_BASE_URL = "${GATEWAY_BASE_URL}user/"

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AuthSessionStore.token
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

interface MoneyApiService {
    @GET("api/categories")
    suspend fun categories(): ApiResponse<List<Category>>

    @POST("api/categories")
    suspend fun addCategory(@Body payload: CategoryPayload): ApiResponse<Any>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body payload: CategoryPayload): ApiResponse<Any>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/records")
    suspend fun records(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("type") type: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("keyword") keyword: String? = null
    ): ApiResponse<RecordsPage>

    @POST("api/records")
    suspend fun addRecord(@Body payload: RecordPayload): ApiResponse<Any>

    @PUT("api/records/{id}")
    suspend fun updateRecord(@Path("id") id: Int, @Body payload: RecordPayload): ApiResponse<Any>

    @DELETE("api/records/{id}")
    suspend fun deleteRecord(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/statistics/monthly")
    suspend fun monthlyStats(@Query("month") month: String): ApiResponse<MonthlyStats>

    @GET("api/statistics/yearly")
    suspend fun yearlyStats(@Query("year") year: Int): ApiResponse<YearlyStats>

    @GET("api/statistics/category")
    suspend fun categoryStats(
        @Query("month") month: String,
        @Query("type") type: String
    ): ApiResponse<List<CategoryStat>>

    @GET("api/statistics/category/yearly")
    suspend fun yearlyCategoryStats(
        @Query("year") year: Int,
        @Query("type") type: String
    ): ApiResponse<List<CategoryStat>>

    @GET("api/statistics/trend")
    suspend fun trend(@Query("month") month: String): ApiResponse<TrendData>

    @GET("api/statistics/trend/yearly")
    suspend fun yearlyTrend(@Query("year") year: Int): ApiResponse<TrendData>

    @GET("api/budget")
    suspend fun budget(@Query("month") month: String): ApiResponse<BudgetData>

    @GET("api/budget/daily-available")
    suspend fun dailyBudget(@Query("month") month: String): ApiResponse<DailyBudgetData>

    @POST("api/budget")
    suspend fun setBudget(@Body payload: BudgetPayload): ApiResponse<Any>
}

interface UserApiService {
    @POST("auth/send-code")
    suspend fun sendCode(@Body payload: SendCodeRequest): ApiResponse<Void>

    @POST("auth/register")
    suspend fun register(@Body payload: RegisterRequest): ApiResponse<Void>

    @POST("auth/login")
    suspend fun login(@Body payload: LoginRequest): ApiResponse<LoginData>

    @GET("users/me")
    suspend fun currentUser(): ApiResponse<UserInfo>
}

object ApiFactory {
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(logger)
            .build()
    }

    private val plainClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val moneyService: MoneyApiService by lazy {
        retrofit(MONEE_BASE_URL, authClient).create(MoneyApiService::class.java)
    }

    val userService: UserApiService by lazy {
        retrofit(USER_BASE_URL, plainClient).create(UserApiService::class.java)
    }

    val userAuthedService: UserApiService by lazy {
        retrofit(USER_BASE_URL, authClient).create(UserApiService::class.java)
    }
}
