package com.money.codex.data

data class ApiResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
)

data class Category(
    val id: Int,
    val type: String,
    val icon: String,
    val name: String,
    val description: String? = null,
    val sort: Int? = null
)

data class RecordItem(
    val id: Int,
    val date: String,
    val type: String,
    val categoryId: Int,
    val categoryName: String,
    val amount: Double,
    val remark: String? = null
)

data class RecordsPage(
    val records: List<RecordItem> = emptyList(),
    val total: Int = 0
)

data class MonthlyStats(
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val balance: Double = 0.0
)

data class BudgetData(
    val budget: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0
)

data class DailyBudgetData(
    val dailyAvailable: Double = 0.0,
    val daysRemaining: Int = 0
)

data class CategoryStat(
    val categoryId: Int,
    val categoryName: String,
    val amount: Double,
    val count: Int = 0
)

data class TrendData(
    val dates: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val expenses: List<Double> = emptyList(),
    val incomes: List<Double> = emptyList(),
    val unit: String? = null
)

data class TopExpenseCategory(
    val categoryName: String,
    val amount: Double
)

data class YearlyStats(
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val balance: Double = 0.0,
    val topExpenseCategory: TopExpenseCategory? = null
)

data class RecordPayload(
    val date: String,
    val type: String,
    val categoryId: Int,
    val amount: Double,
    val remark: String = ""
)

data class BudgetPayload(
    val amount: Double,
    val month: String
)

data class CategoryPayload(
    val type: String,
    val icon: String,
    val name: String,
    val description: String = "",
    val sort: Int = 99
)

data class SendCodeRequest(
    val email: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val code: String,
    val nickname: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserInfo(
    val id: Long,
    val email: String,
    val nickname: String
)

data class LoginData(
    val token: String,
    val userInfo: UserInfo
)
