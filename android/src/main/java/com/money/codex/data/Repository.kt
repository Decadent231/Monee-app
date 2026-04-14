package com.money.codex.data

import android.content.Context
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class UnauthorizedException(message: String = "登录状态已失效，请重新登录") : RuntimeException(message)

class MoneyRepository(
    context: Context,
    private val moneyApi: MoneyApiService = ApiFactory.moneyService,
    private val userApi: UserApiService = ApiFactory.userService,
    private val userAuthedApi: UserApiService = ApiFactory.userAuthedService
) {

    init {
        AuthSessionStore.init(context)
    }

    private fun <T> unwrap(response: ApiResponse<T>): T? {
        if (response.code == 200) {
            return response.data
        }
        throw IllegalStateException(response.message ?: "接口调用失败")
    }

    private fun mapException(t: Throwable): Throwable {
        return when (t) {
            is HttpException -> {
                if (t.code() == 401 || t.code() == 403) {
                    UnauthorizedException()
                } else {
                    IllegalStateException("请求失败(${t.code()})")
                }
            }

            is IOException -> IllegalStateException("网络连接失败，请检查服务器或网络")
            else -> t
        }
    }

    private suspend fun <T> callApi(block: suspend () -> T): T {
        return try {
            block()
        } catch (t: Throwable) {
            throw mapException(t)
        }
    }

    fun hasSession(): Boolean = !AuthSessionStore.token.isNullOrBlank()

    fun cachedUser(): UserInfo? = AuthSessionStore.currentUser()

    suspend fun sendRegisterCode(email: String) {
        callApi {
            unwrap(userApi.sendCode(SendCodeRequest(email)))
            Unit
        }
    }

    suspend fun register(nickname: String, email: String, code: String, password: String) {
        callApi {
            unwrap(userApi.register(RegisterRequest(email, password, code, nickname)))
            Unit
        }
    }

    suspend fun login(email: String, password: String): UserInfo {
        return callApi {
            val data = unwrap(userApi.login(LoginRequest(email, password)))
                ?: throw IllegalStateException("登录结果为空")
            AuthSessionStore.saveSession(data.token, data.userInfo)
            AuthSessionStore.saveRememberedCredentials(email.trim(), password)
            data.userInfo
        }
    }

    suspend fun fetchCurrentUser(): UserInfo {
        return callApi {
            val data = unwrap(userAuthedApi.currentUser())
                ?: throw IllegalStateException("用户信息为空")
            AuthSessionStore.saveSession(AuthSessionStore.token.orEmpty(), data)
            data
        }
    }

    suspend fun updateProfile(nickname: String): UserInfo {
        return callApi {
            val data = unwrap(userAuthedApi.updateProfile(UpdateProfileRequest(nickname.trim())))
                ?: throw IllegalStateException("更新后的用户信息为空")
            AuthSessionStore.saveSession(AuthSessionStore.token.orEmpty(), data)
            data
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        callApi {
            unwrap(userAuthedApi.changePassword(ChangePasswordRequest(oldPassword, newPassword)))
            Unit
        }
    }

    fun logout() {
        AuthSessionStore.clearSession()
    }

    suspend fun loadCategories(): List<Category> = callApi {
        unwrap(moneyApi.categories()).orEmpty()
    }

    suspend fun addCategory(payload: CategoryPayload): Boolean = callApi {
        unwrap(moneyApi.addCategory(payload))
        true
    }

    suspend fun updateCategory(id: Int, payload: CategoryPayload): Boolean = callApi {
        unwrap(moneyApi.updateCategory(id, payload))
        true
    }

    suspend fun deleteCategory(id: Int): Boolean = callApi {
        unwrap(moneyApi.deleteCategory(id))
        true
    }

    suspend fun loadDashboard(month: String): DashboardBundle {
        val monthlyStats = callApi { unwrap(moneyApi.monthlyStats(month)) ?: MonthlyStats() }
        val budget = callApi { unwrap(moneyApi.budget(month)) ?: BudgetData() }
        val dailyBudget = callApi { unwrap(moneyApi.dailyBudget(month)) ?: DailyBudgetData() }
        val records = callApi { unwrap(moneyApi.records(page = 1, size = 100))?.records.orEmpty() }
        val expenseStats = callApi { unwrap(moneyApi.categoryStats(month, "expense")).orEmpty() }
        return DashboardBundle(monthlyStats, budget, dailyBudget, records, expenseStats)
    }

    suspend fun loadRecords(
        page: Int = 1,
        size: Int = 20,
        startDate: String? = null,
        endDate: String? = null,
        type: String? = null,
        categoryId: Int? = null,
        keyword: String? = null
    ): RecordsPage = callApi {
        unwrap(moneyApi.records(page, size, startDate, endDate, type, categoryId, keyword)) ?: RecordsPage()
    }

    suspend fun addRecord(payload: RecordPayload): Boolean = callApi {
        unwrap(moneyApi.addRecord(payload))
        true
    }

    suspend fun updateRecord(id: Int, payload: RecordPayload): Boolean = callApi {
        unwrap(moneyApi.updateRecord(id, payload))
        true
    }

    suspend fun deleteRecord(id: Int): Boolean = callApi {
        unwrap(moneyApi.deleteRecord(id))
        true
    }

    suspend fun loadStatistics(
        period: String,
        month: String,
        year: Int
    ): StatisticsBundle {
        return if (period == "year") {
            val yearly = callApi { unwrap(moneyApi.yearlyStats(year)) ?: YearlyStats() }
            val stats = MonthlyStats(
                expense = yearly.expense,
                income = yearly.income,
                balance = yearly.balance
            )
            val expense = callApi { unwrap(moneyApi.yearlyCategoryStats(year, "expense")).orEmpty() }
            val income = callApi { unwrap(moneyApi.yearlyCategoryStats(year, "income")).orEmpty() }
            val trend = callApi { unwrap(moneyApi.yearlyTrend(year)) ?: TrendData() }
            StatisticsBundle(stats, expense, income, trend)
        } else {
            val stats = callApi { unwrap(moneyApi.monthlyStats(month)) ?: MonthlyStats() }
            val expense = callApi { unwrap(moneyApi.categoryStats(month, "expense")).orEmpty() }
            val income = callApi { unwrap(moneyApi.categoryStats(month, "income")).orEmpty() }
            val trend = callApi { unwrap(moneyApi.trend(month)) ?: TrendData() }
            StatisticsBundle(stats, expense, income, trend)
        }
    }

    suspend fun loadBudget(month: String): Pair<BudgetData, DailyBudgetData> {
        val budget = callApi { unwrap(moneyApi.budget(month)) ?: BudgetData() }
        val daily = callApi { unwrap(moneyApi.dailyBudget(month)) ?: DailyBudgetData() }
        return budget to daily
    }

    suspend fun setBudget(amount: Double, month: String): Boolean = callApi {
        unwrap(moneyApi.setBudget(BudgetPayload(amount, month)))
        true
    }
}

data class DashboardBundle(
    val stats: MonthlyStats,
    val budget: BudgetData,
    val dailyBudget: DailyBudgetData,
    val records: List<RecordItem>,
    val categoryStats: List<CategoryStat>
)

data class StatisticsBundle(
    val stats: MonthlyStats,
    val expenseStats: List<CategoryStat>,
    val incomeStats: List<CategoryStat>,
    val trend: TrendData
)

fun currentMonthString(): String = YearMonth.now().toString()
fun currentYearValue(): Int = Year.now().value
fun todayString(): String = LocalDate.now().toString()
