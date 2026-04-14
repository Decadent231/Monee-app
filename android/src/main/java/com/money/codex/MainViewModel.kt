package com.money.codex

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.money.codex.data.BudgetData
import com.money.codex.data.Category
import com.money.codex.data.CategoryPayload
import com.money.codex.data.CategoryStat
import com.money.codex.data.MoneyRepository
import com.money.codex.data.MonthlyStats
import com.money.codex.data.RecordItem
import com.money.codex.data.RecordPayload
import com.money.codex.data.StatisticsBundle
import com.money.codex.data.TrendData
import com.money.codex.data.UnauthorizedException
import com.money.codex.data.UserInfo
import com.money.codex.data.currentMonthString
import com.money.codex.data.currentYearValue
import com.money.codex.data.todayString
import com.money.codex.ui.theme.AppThemePreset
import kotlinx.coroutines.launch
import java.time.YearMonth

enum class AppTab(val title: String) {
    Dashboard("概览"),
    Records("记账"),
    Statistics("统计"),
    Categories("分类"),
    Settings("设置")
}

enum class AppUiStyle(val label: String) {
    Pulse("灵动卡片"),
    Neo("极简线框"),
    Aurora("流光玻璃"),
    Mono("杂志黑白")
}

enum class MessageTone {
    Success,
    Error,
    Info
}

data class UiMessage(
    val text: String,
    val tone: MessageTone = MessageTone.Info
)

data class DashboardUi(
    val stats: MonthlyStats = MonthlyStats(),
    val budgetRemaining: Double = 0.0,
    val budgetPercent: Double = 0.0,
    val dailyAverageBudget: Double = 0.0,
    val dailyAvailable: Double = 0.0,
    val daysRemaining: Int = 0,
    val recentRecords: List<RecordItem> = emptyList(),
    val expenseStats: List<CategoryStat> = emptyList()
)

data class RecordsUi(
    val list: List<RecordItem> = emptyList(),
    val page: Int = 1,
    val total: Int = 0
)

data class RecordFilters(
    val startDate: String = "",
    val endDate: String = "",
    val type: String = "",
    val categoryId: Int? = null,
    val keyword: String = ""
)

data class StatisticsUi(
    val stats: MonthlyStats = MonthlyStats(),
    val expenseStats: List<CategoryStat> = emptyList(),
    val incomeStats: List<CategoryStat> = emptyList(),
    val trend: TrendData = TrendData(),
    val period: String = "month",
    val month: String = currentMonthString(),
    val year: Int = currentYearValue()
)

data class BudgetUi(
    val budget: BudgetData = BudgetData(),
    val dailyAvailable: Double = 0.0,
    val daysRemaining: Int = 0
)

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: UserInfo? = null,
    val authLoading: Boolean = true,
    val sendingCode: Boolean = false,
    val loginSubmitting: Boolean = false,
    val registerSubmitting: Boolean = false
)

data class ReminderCapabilityUi(
    val exactAlarmSupported: Boolean = true
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MoneyRepository(application.applicationContext)

    var currentTab by mutableStateOf(AppTab.Dashboard)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var categories by mutableStateOf(emptyList<Category>())
        private set

    var dashboard by mutableStateOf(DashboardUi())
        private set

    var records by mutableStateOf(RecordsUi())
        private set

    var recordFilters by mutableStateOf(RecordFilters())
        private set

    var statistics by mutableStateOf(StatisticsUi())
        private set

    var budget by mutableStateOf(BudgetUi())
        private set

    var currentMonth by mutableStateOf(currentMonthString())
        private set

    var selectedTheme by mutableStateOf(AppThemePreset.Midnight)
        private set

    var selectedUiStyle by mutableStateOf(AppUiStyle.Aurora)
        private set

    var reminderEnabled by mutableStateOf(false)
        private set

    var reminderHour by mutableStateOf(21)
        private set

    var reminderMinute by mutableStateOf(0)
        private set

    var statisticsPeriod by mutableStateOf("month")
        private set

    var statisticsMonth by mutableStateOf(currentMonthString())
        private set

    var statisticsYear by mutableStateOf(currentYearValue())
        private set

    var addRecordDialogVisible by mutableStateOf(false)
        private set

    var toastMessage by mutableStateOf<UiMessage?>(null)
        private set

    var editingRecord by mutableStateOf<RecordItem?>(null)
        private set

    var authState by mutableStateOf(AuthUiState())
        private set

    var reminderCapability by mutableStateOf(ReminderCapabilityUi())
        private set

    init {
        bootstrap()
    }

    private fun bootstrap() {
        authState = authState.copy(
            currentUser = repository.cachedUser(),
            isAuthenticated = repository.hasSession(),
            authLoading = true
        )
        if (!repository.hasSession()) {
            authState = authState.copy(authLoading = false)
            return
        }
        viewModelScope.launch {
            runCatching { repository.fetchCurrentUser() }
                .onSuccess { user ->
                    authState = authState.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        authLoading = false
                    )
                    refreshAll()
                }
                .onFailure { handleFailure(it, fallback = { authState = AuthUiState(authLoading = false) }) }
        }
    }

    fun selectTab(tab: AppTab) {
        currentTab = tab
    }

    fun showAddRecordDialog(show: Boolean) {
        addRecordDialogVisible = show
    }

    fun setTheme(preset: AppThemePreset) {
        selectedTheme = preset
    }

    fun setUiStyle(style: AppUiStyle) {
        selectedUiStyle = style
    }

    fun loadReminderSettings(context: Context) {
        val settings = ReminderScheduler.loadSettings(context)
        reminderEnabled = settings.enabled
        reminderHour = settings.hour
        reminderMinute = settings.minute
        ReminderScheduler.ensureNotificationChannel(context)
        reminderCapability = ReminderCapabilityUi(
            exactAlarmSupported = ReminderScheduler.canScheduleExactAlarms(context)
        )
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        reminderEnabled = enabled
        ReminderScheduler.saveSettings(context, enabled, reminderHour, reminderMinute)
        if (enabled) {
            val scheduled = ReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
            reminderCapability = ReminderCapabilityUi(
                exactAlarmSupported = ReminderScheduler.canScheduleExactAlarms(context)
            )
            toastMessage = if (scheduled) {
                UiMessage("定时提醒已开启", MessageTone.Success)
            } else {
                UiMessage("系统未授予精确提醒权限，请先完成授权", MessageTone.Error)
            }
        } else {
            ReminderScheduler.cancelDailyReminder(context)
            toastMessage = UiMessage("定时提醒已关闭", MessageTone.Info)
        }
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
        ReminderScheduler.saveSettings(context, reminderEnabled, hour, minute)
        if (reminderEnabled) {
            val scheduled = ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            reminderCapability = ReminderCapabilityUi(
                exactAlarmSupported = ReminderScheduler.canScheduleExactAlarms(context)
            )
            toastMessage = if (scheduled) {
                UiMessage("提醒时间已更新", MessageTone.Success)
            } else {
                UiMessage("系统未授予精确提醒权限，请先完成授权", MessageTone.Error)
            }
        }
    }

    fun refreshReminderCapability(context: Context) {
        reminderCapability = ReminderCapabilityUi(
            exactAlarmSupported = ReminderScheduler.canScheduleExactAlarms(context)
        )
    }

    fun clearToast() {
        toastMessage = null
    }

    fun openEditRecord(record: RecordItem) {
        editingRecord = record
    }

    fun closeEditRecord() {
        editingRecord = null
    }

    fun sendRegisterCode(email: String) {
        viewModelScope.launch {
            authState = authState.copy(sendingCode = true)
            runCatching { repository.sendRegisterCode(email) }
                .onSuccess { toastMessage = UiMessage("验证码已发送，请检查邮箱", MessageTone.Success) }
                .onFailure { handleFailure(it) }
            authState = authState.copy(sendingCode = false)
        }
    }

    fun register(nickname: String, email: String, code: String, password: String) {
        viewModelScope.launch {
            authState = authState.copy(registerSubmitting = true)
            runCatching { repository.register(nickname, email, code, password) }
                .onSuccess { toastMessage = UiMessage("注册成功，请登录", MessageTone.Success) }
                .onFailure { handleFailure(it) }
            authState = authState.copy(registerSubmitting = false)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authState = authState.copy(loginSubmitting = true)
            runCatching {
                repository.login(email, password)
                repository.fetchCurrentUser()
            }.onSuccess { user ->
                authState = authState.copy(
                    isAuthenticated = true,
                    currentUser = user,
                    authLoading = false,
                    loginSubmitting = false
                )
                toastMessage = UiMessage("登录成功", MessageTone.Success)
                refreshAll()
            }.onFailure {
                authState = authState.copy(loginSubmitting = false)
                handleFailure(it)
            }
        }
    }

    fun logout() {
        repository.logout()
        authState = AuthUiState(authLoading = false)
        categories = emptyList()
        dashboard = DashboardUi()
        records = RecordsUi()
        statistics = StatisticsUi()
        budget = BudgetUi()
        addRecordDialogVisible = false
        editingRecord = null
        currentTab = AppTab.Dashboard
        toastMessage = UiMessage("已退出登录", MessageTone.Info)
    }

    fun refreshAll() {
        if (!authState.isAuthenticated) return
        viewModelScope.launch {
            isLoading = true
            runCatching {
                categories = repository.loadCategories()

                val dashboardData = repository.loadDashboard(currentMonth)
                val percent = if (dashboardData.budget.budget <= 0.0) 0.0
                else (dashboardData.budget.spent / dashboardData.budget.budget) * 100
                dashboard = DashboardUi(
                    stats = dashboardData.stats,
                    budgetRemaining = dashboardData.budget.remaining,
                    budgetPercent = percent.coerceIn(0.0, 100.0),
                    dailyAverageBudget = dashboardData.dailyBudget.dailyAvailable,
                    dailyAvailable = dashboardData.dailyBudget.dailyAvailable,
                    daysRemaining = dashboardData.dailyBudget.daysRemaining,
                    recentRecords = dashboardData.records,
                    expenseStats = dashboardData.categoryStats
                )

                val recordsData = repository.loadRecords(
                    page = 1,
                    size = 20,
                    startDate = recordFilters.startDate.ifBlank { null },
                    endDate = recordFilters.endDate.ifBlank { null },
                    type = recordFilters.type.ifBlank { null },
                    categoryId = recordFilters.categoryId,
                    keyword = recordFilters.keyword.ifBlank { null }
                )
                records = RecordsUi(recordsData.records, 1, recordsData.total)

                val statisticsData: StatisticsBundle = repository.loadStatistics(
                    period = statisticsPeriod,
                    month = statisticsMonth,
                    year = statisticsYear
                )
                statistics = StatisticsUi(
                    statisticsData.stats,
                    statisticsData.expenseStats,
                    statisticsData.incomeStats,
                    statisticsData.trend,
                    statisticsPeriod,
                    statisticsMonth,
                    statisticsYear
                )

                val (budgetData, dailyData) = repository.loadBudget(currentMonth)
                budget = BudgetUi(budgetData, dailyData.dailyAvailable, dailyData.daysRemaining)
            }.onFailure { handleFailure(it) }
            isLoading = false
        }
    }

    fun loadDashboard() {
        if (!authState.isAuthenticated) return
        viewModelScope.launch {
            runCatching {
                val data = repository.loadDashboard(currentMonth)
                val percent = if (data.budget.budget <= 0.0) 0.0 else (data.budget.spent / data.budget.budget) * 100
                dashboard = DashboardUi(
                    stats = data.stats,
                    budgetRemaining = data.budget.remaining,
                    budgetPercent = percent.coerceIn(0.0, 100.0),
                    dailyAverageBudget = data.dailyBudget.dailyAvailable,
                    dailyAvailable = data.dailyBudget.dailyAvailable,
                    daysRemaining = data.dailyBudget.daysRemaining,
                    recentRecords = data.records,
                    expenseStats = data.categoryStats
                )
            }.onFailure { handleFailure(it) }
        }
    }

    fun loadRecords(page: Int = 1) {
        if (!authState.isAuthenticated) return
        viewModelScope.launch {
            runCatching {
                val data = repository.loadRecords(
                    page = page,
                    size = 20,
                    startDate = recordFilters.startDate.ifBlank { null },
                    endDate = recordFilters.endDate.ifBlank { null },
                    type = recordFilters.type.ifBlank { null },
                    categoryId = recordFilters.categoryId,
                    keyword = recordFilters.keyword.ifBlank { null }
                )
                records = RecordsUi(data.records, page, data.total)
            }.onFailure { handleFailure(it) }
        }
    }

    fun updateRecordFilters(
        startDate: String = recordFilters.startDate,
        endDate: String = recordFilters.endDate,
        type: String = recordFilters.type,
        categoryId: Int? = recordFilters.categoryId,
        keyword: String = recordFilters.keyword
    ) {
        recordFilters = RecordFilters(startDate, endDate, type, categoryId, keyword)
    }

    fun resetRecordFilters() {
        recordFilters = RecordFilters()
        loadRecords(1)
    }

    fun searchRecords() {
        loadRecords(1)
    }

    fun loadStatistics() {
        if (!authState.isAuthenticated) return
        viewModelScope.launch {
            runCatching {
                val data: StatisticsBundle = repository.loadStatistics(
                    period = statisticsPeriod,
                    month = statisticsMonth,
                    year = statisticsYear
                )
                statistics = StatisticsUi(
                    data.stats,
                    data.expenseStats,
                    data.incomeStats,
                    data.trend,
                    statisticsPeriod,
                    statisticsMonth,
                    statisticsYear
                )
            }.onFailure { handleFailure(it) }
        }
    }

    fun changeStatisticsPeriod(period: String) {
        statisticsPeriod = period
        loadStatistics()
    }

    fun changeStatisticsMonth(month: String) {
        statisticsMonth = month
        loadStatistics()
    }

    fun changeStatisticsYear(year: Int) {
        statisticsYear = year
        loadStatistics()
    }

    fun loadBudget() {
        if (!authState.isAuthenticated) return
        viewModelScope.launch {
            runCatching {
                val (budgetData, dailyData) = repository.loadBudget(currentMonth)
                budget = BudgetUi(budgetData, dailyData.dailyAvailable, dailyData.daysRemaining)
            }.onFailure { handleFailure(it) }
        }
    }

    fun addRecord(
        type: String,
        categoryId: Int,
        amount: Double,
        remark: String,
        date: String = todayString()
    ) {
        viewModelScope.launch {
            if (amount <= 0.0 || categoryId <= 0) {
                toastMessage = UiMessage("请先填写正确的金额与分类", MessageTone.Error)
                return@launch
            }
            runCatching {
                repository.addRecord(
                    RecordPayload(
                        date = date,
                        type = type,
                        categoryId = categoryId,
                        amount = amount,
                        remark = remark
                    )
                )
            }.onSuccess {
                toastMessage = UiMessage("记录已添加到你的账本", MessageTone.Success)
                addRecordDialogVisible = false
                loadDashboard()
                loadRecords()
                loadStatistics()
                loadBudget()
            }.onFailure { handleFailure(it) }
        }
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteRecord(id) }
                .onSuccess {
                    toastMessage = UiMessage("记录已删除", MessageTone.Success)
                    if (editingRecord?.id == id) {
                        editingRecord = null
                    }
                    loadDashboard()
                    loadRecords(records.page)
                    loadStatistics()
                    loadBudget()
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun updateRecord(
        id: Int,
        type: String,
        categoryId: Int,
        amount: Double,
        remark: String,
        date: String
    ) {
        viewModelScope.launch {
            if (amount <= 0.0 || categoryId <= 0) {
                toastMessage = UiMessage("请先填写正确的金额与分类", MessageTone.Error)
                return@launch
            }
            runCatching {
                repository.updateRecord(
                    id,
                    RecordPayload(
                        date = date,
                        type = type,
                        categoryId = categoryId,
                        amount = amount,
                        remark = remark
                    )
                )
            }.onSuccess {
                toastMessage = UiMessage("记录已更新", MessageTone.Success)
                editingRecord = null
                loadDashboard()
                loadRecords(records.page)
                loadStatistics()
                loadBudget()
            }.onFailure { handleFailure(it) }
        }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            runCatching { repository.setBudget(amount, currentMonth) }
                .onSuccess {
                    toastMessage = UiMessage("预算设置成功", MessageTone.Success)
                    loadDashboard()
                    loadBudget()
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun addCategory(
        type: String,
        icon: String,
        name: String,
        description: String,
        sort: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addCategory(CategoryPayload(type, icon, name, description, sort))
            }.onSuccess {
                toastMessage = UiMessage("分类添加成功", MessageTone.Success)
                refreshAll()
            }.onFailure { handleFailure(it) }
        }
    }

    fun updateCategory(
        id: Int,
        type: String,
        icon: String,
        name: String,
        description: String,
        sort: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateCategory(id, CategoryPayload(type, icon, name, description, sort))
            }.onSuccess {
                toastMessage = UiMessage("分类更新成功", MessageTone.Success)
                refreshAll()
            }.onFailure { handleFailure(it) }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteCategory(id) }
                .onSuccess {
                    toastMessage = UiMessage("分类已删除", MessageTone.Success)
                    refreshAll()
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun updateProfile(nickname: String) {
        viewModelScope.launch {
            if (nickname.isBlank()) {
                toastMessage = UiMessage("昵称不能为空", MessageTone.Error)
                return@launch
            }
            runCatching { repository.updateProfile(nickname) }
                .onSuccess { user ->
                    authState = authState.copy(currentUser = user, isAuthenticated = true)
                    toastMessage = UiMessage("昵称已更新", MessageTone.Success)
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            if (oldPassword.isBlank() || newPassword.isBlank()) {
                toastMessage = UiMessage("请完整填写旧密码和新密码", MessageTone.Error)
                return@launch
            }
            runCatching { repository.changePassword(oldPassword, newPassword) }
                .onSuccess {
                    toastMessage = UiMessage("密码修改成功，请牢记新密码", MessageTone.Success)
                }
                .onFailure { handleFailure(it) }
        }
    }

    private fun handleFailure(throwable: Throwable, fallback: (() -> Unit)? = null) {
        if (throwable is UnauthorizedException) {
            repository.logout()
            authState = AuthUiState(authLoading = false)
            fallback?.invoke()
            toastMessage = UiMessage(throwable.message ?: "登录状态已失效，请重新登录", MessageTone.Error)
            return
        }
        fallback?.invoke()
        toastMessage = UiMessage(throwable.message ?: "操作失败", MessageTone.Error)
    }
}
