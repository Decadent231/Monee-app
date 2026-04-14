package com.money.codex.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.money.codex.BudgetUi
import com.money.codex.BudgetAlertPalette
import com.money.codex.BudgetAlertSettingsUi
import com.money.codex.CalendarExpenseDay
import com.money.codex.DashboardUi
import com.money.codex.AppUiStyle
import com.money.codex.AuthUiState
import com.money.codex.RecordFilters
import com.money.codex.RecordsUi
import com.money.codex.StatisticsUi
import com.money.codex.data.AuthSessionStore
import com.money.codex.data.Category
import com.money.codex.data.CategoryStat
import com.money.codex.data.RecordItem
import com.money.codex.data.TrendData
import com.money.codex.data.UserInfo
import com.money.codex.data.todayString
import com.money.codex.ui.theme.AppThemePreset
import com.money.codex.ui.theme.Brand
import com.money.codex.ui.theme.BrandSurface
import com.money.codex.ui.theme.Expense
import com.money.codex.ui.theme.Income
import com.money.codex.ui.theme.TextMuted
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val moneyFmt = DecimalFormat("#,##0.00")
private val expenseIconPresets = listOf("🍽️", "☕", "🛒", "🚕", "⛽", "🏠", "📱", "🎬", "🎮", "💊", "✈️", "🎓")
private val incomeIconPresets = listOf("💰", "💵", "🧧", "🎁", "📈", "💼", "🏆", "💳")

private data class CategoryTemplate(
    val type: String,
    val icon: String,
    val name: String,
    val description: String,
    val sort: Int
)

private val categoryTemplates = listOf(
    CategoryTemplate("expense", "🍽️", "餐饮", "日常吃饭、外卖、聚餐", 10),
    CategoryTemplate("expense", "☕", "咖啡茶饮", "咖啡、奶茶、饮品", 12),
    CategoryTemplate("expense", "🛒", "购物", "日用百货、网购消费", 20),
    CategoryTemplate("expense", "🚕", "出行", "打车、公交、地铁", 30),
    CategoryTemplate("expense", "⛽", "汽车", "加油、停车、保养", 32),
    CategoryTemplate("expense", "🏠", "住房", "房租、水电、物业", 40),
    CategoryTemplate("expense", "📱", "通讯", "话费、流量、宽带", 45),
    CategoryTemplate("expense", "🎬", "娱乐", "电影、游戏、休闲", 50),
    CategoryTemplate("expense", "💊", "医疗", "药品、门诊、体检", 60),
    CategoryTemplate("income", "💼", "工资", "固定工资收入", 10),
    CategoryTemplate("income", "💰", "奖金", "绩效、奖金、分红", 20),
    CategoryTemplate("income", "📈", "投资", "理财、基金、股票收益", 30),
    CategoryTemplate("income", "🎁", "礼金", "红包、礼物折现", 40),
    CategoryTemplate("income", "🧧", "其他收入", "临时或额外收入", 50)
)

private val chartPalette = listOf(
    Color(0xFF4F46E5),
    Color(0xFFF97316),
    Color(0xFFDC2626),
    Color(0xFFEAB308),
    Color(0xFF7C3AED),
    Color(0xFF2563EB),
    Color(0xFFDB2777),
    Color(0xFFEA580C),
    Color(0xFF9333EA),
    Color(0xFF475569)
)

fun money(value: Double): String = "¥${moneyFmt.format(value)}"

private fun budgetAlertColor(percent: Double, settings: BudgetAlertSettingsUi): Color {
    return when {
        percent >= 100.0 -> Color(settings.palette.dangerHex)
        percent >= settings.warningPercent -> Color(settings.palette.warningHex)
        else -> Color(settings.palette.normalHex)
    }
}

private data class UiStyleTokens(
    val pagePadding: Int,
    val cardRadius: Int,
    val accentRadius: Int,
    val cardColor: Color,
    val accentColor: Color,
    val border: BorderStroke? = null
)

@Composable
private fun uiStyleTokens(style: AppUiStyle): UiStyleTokens {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    return when (style) {
        AppUiStyle.Pulse -> UiStyleTokens(
            pagePadding = 14,
            cardRadius = 22,
            accentRadius = 14,
            cardColor = BrandSurface,
            accentColor = Brand
        )
        AppUiStyle.Neo -> UiStyleTokens(
            pagePadding = 16,
            cardRadius = 10,
            accentRadius = 8,
            cardColor = surface,
            accentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, outline)
        )
        AppUiStyle.Aurora -> UiStyleTokens(
            pagePadding = 14,
            cardRadius = 26,
            accentRadius = 16,
            cardColor = BrandSurface.copy(alpha = 0.92f),
            accentColor = Brand.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        )
        AppUiStyle.Mono -> UiStyleTokens(
            pagePadding = 12,
            cardRadius = 6,
            accentRadius = 6,
            cardColor = MaterialTheme.colorScheme.surface,
            accentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )
    }
}

private fun weekdayZh(date: String): String {
    val day = runCatching { LocalDate.parse(date).dayOfWeek }.getOrNull() ?: return ""
    return when (day.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
}

private fun defaultCategoryId(categories: List<Category>, type: String): Int {
    val typed = categories.filter { it.type == type }
    val preferred = if (type == "expense") {
        typed.firstOrNull { it.name.contains("餐饮") }
    } else {
        null
    }
    return (preferred ?: typed.firstOrNull())?.id ?: 0
}

@Composable
fun DashboardScreen(
    ui: DashboardUi,
    uiStyle: AppUiStyle,
    currentUserName: String,
    budgetAlertSettings: BudgetAlertSettingsUi,
    onEditRecord: (RecordItem) -> Unit
) {
    val s = uiStyleTokens(uiStyle)
    val progressColor = budgetAlertColor(ui.budgetPercent, budgetAlertSettings)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.pagePadding.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(s.cardRadius.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    BrandSurface.copy(alpha = 0.96f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    BrandSurface.copy(alpha = 0.9f)
                                )
                            )
                        )
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Monee",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "概览 · $currentUserName",
                                    color = Color.White.copy(alpha = 0.64f),
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "结余 ${money(ui.stats.balance)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "本月支出",
                                color = Color.White.copy(alpha = 0.74f),
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = money(ui.stats.expense),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 46.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "每日可支配 ${money(ui.dailyAverageBudget)}",
                                color = Color.White.copy(alpha = 0.66f),
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("收入", color = Color(0xFFA7F3D0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        money(ui.stats.income),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("支出", color = Color(0xFFFDA4AF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        money(ui.stats.expense),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "剩余预算:${money(ui.budgetRemaining)} / ${ui.daysRemaining}天",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "日均预算 ${money(ui.dailyAvailable)}",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (ui.budgetPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = progressColor,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }
        item { SectionTitle("最近记录", uiStyle) }
        val recentRecords = ui.recentRecords.sortedWith(
            compareByDescending<RecordItem> { it.date }.thenByDescending { it.id }
        )
        if (recentRecords.isEmpty()) {
            item { EmptyTip("暂无记录，开始记账吧") }
        } else {
            val grouped = recentRecords.groupBy { it.date }
            grouped.forEach { (date, records) ->
                val dayExpense = records.filter { it.type == "expense" }.sumOf { it.amount }
                val dayIncome = records.filter { it.type == "income" }.sumOf { it.amount }
                val dayNet = dayIncome - dayExpense
                val dayNetText = if (dayNet < 0) {
                    "支出 -${money(kotlin.math.abs(dayNet))}"
                } else {
                    "收入 +${money(dayNet)}"
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$date ${weekdayZh(date)}",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = dayNetText,
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                items(records) { record ->
                    RecordRow(
                        record = record,
                        showDelete = false,
                        showEdit = true,
                        onDelete = {},
                        onEdit = { onEditRecord(record) }
                    )
                }
            }
        }

    }
}

@Composable
fun RecordsScreen(
    ui: RecordsUi,
    uiStyle: AppUiStyle,
    filters: RecordFilters,
    categories: List<Category>,
    onFiltersChange: (startDate: String, endDate: String, type: String, categoryId: Int?, keyword: String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (RecordItem) -> Unit
) {
    val s = uiStyleTokens(uiStyle)
    val context = LocalContext.current
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteRecord by remember { mutableStateOf<RecordItem?>(null) }
    val hasAdvancedFilters = filters.type.isNotBlank() || filters.categoryId != null || filters.keyword.isNotBlank()
    val availableCategories = if (filters.type.isBlank()) {
        categories
    } else {
        categories.filter { it.type == filters.type }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = s.pagePadding.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionTitle("记账记录", uiStyle)
        Card(
            shape = RoundedCornerShape((s.cardRadius - 4).coerceAtLeast(8).dp),
            colors = CardDefaults.cardColors(containerColor = s.cardColor),
            border = s.border
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filters.startDate.isBlank()) "开始日期" else filters.startDate,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .background(BrandSurface, RoundedCornerShape(10.dp))
                            .clickable {
                                val current = filters.startDate.ifBlank { todayString() }
                                showDatePicker(context, current) {
                                    onFiltersChange(it, filters.endDate, filters.type, filters.categoryId, filters.keyword)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    Text("~", color = TextMuted)
                    Text(
                        text = if (filters.endDate.isBlank()) "结束日期" else filters.endDate,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .background(BrandSurface, RoundedCornerShape(10.dp))
                            .clickable {
                                val current = filters.endDate.ifBlank { todayString() }
                                showDatePicker(context, current) {
                                    onFiltersChange(filters.startDate, it, filters.type, filters.categoryId, filters.keyword)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSearch,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("搜索") }
                    Text(
                        text = if (advancedExpanded) "收起筛选" else if (hasAdvancedFilters) "筛选(已选)" else "展开筛选",
                        color = if (hasAdvancedFilters) Brand else TextMuted,
                        modifier = Modifier
                            .background(BrandSurface, RoundedCornerShape(10.dp))
                            .clickable { advancedExpanded = !advancedExpanded }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "重置",
                        color = TextMuted,
                        modifier = Modifier
                            .background(BrandSurface, RoundedCornerShape(10.dp))
                            .clickable {
                                advancedExpanded = false
                                onReset()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("" to "全部", "expense" to "支出", "income" to "收入").forEach { (value, label) ->
                                Text(
                                    text = label,
                                    color = if (filters.type == value) Color.White else Color(0xFF334155),
                                    modifier = Modifier
                                        .background(
                                            if (filters.type == value) Brand else BrandSurface,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onFiltersChange(
                                                filters.startDate,
                                                filters.endDate,
                                                value,
                                                null,
                                                filters.keyword
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Text(
                                    text = "全部分类",
                                    color = if (filters.categoryId == null) Color.White else Color(0xFF334155),
                                    modifier = Modifier
                                        .background(
                                            if (filters.categoryId == null) Brand else BrandSurface,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onFiltersChange(
                                                filters.startDate,
                                                filters.endDate,
                                                filters.type,
                                                null,
                                                filters.keyword
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            items(availableCategories) { category ->
                                Text(
                                    text = "${category.icon} ${category.name}",
                                    color = if (filters.categoryId == category.id) Color.White else Color(0xFF334155),
                                    modifier = Modifier
                                        .background(
                                            if (filters.categoryId == category.id) Brand else BrandSurface,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onFiltersChange(
                                                filters.startDate,
                                                filters.endDate,
                                                filters.type,
                                                category.id,
                                                filters.keyword
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = filters.keyword,
                            onValueChange = {
                                onFiltersChange(
                                    filters.startDate,
                                    filters.endDate,
                                    filters.type,
                                    filters.categoryId,
                                    it
                                )
                            },
                            label = { Text("关键词（备注）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (ui.list.isEmpty()) {
            EmptyTip("暂无记录")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val grouped = ui.list
                .sortedWith(compareByDescending<RecordItem> { it.date }.thenByDescending { it.id })
                .groupBy { it.date }
            grouped.forEach { (date, records) ->
                item {
                    Text(
                        text = "$date ${weekdayZh(date)}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }
                items(records) { record ->
                    RecordRow(
                        record = record,
                        showDelete = true,
                        showEdit = true,
                        onDelete = { pendingDeleteRecord = record },
                        onEdit = { onEdit(record) }
                    )
                }
            }
        }
    }

    pendingDeleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            title = { Text("确认删除") },
            text = { Text("确定删除这条记录吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(record.id)
                        pendingDeleteRecord = null
                    }
                ) { Text("删除", color = Expense) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) { Text("取消") }
            }
        )
    }
}

@Composable
fun StatisticsScreen(
    ui: StatisticsUi,
    uiStyle: AppUiStyle,
    onPeriodChange: (String) -> Unit,
    onMonthChange: (String) -> Unit,
    onYearChange: (Int) -> Unit
) {
    val s = uiStyleTokens(uiStyle)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.pagePadding.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("month" to "按月", "year" to "按年").forEach { (value, label) ->
                    Button(
                        onClick = { onPeriodChange(value) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ui.period == value) Brand else BrandSurface,
                            contentColor = if (ui.period == value) Color.White else Color(0xFF334155)
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }

        item {
            if (ui.period == "month") {
                val current = runCatching { YearMonth.parse(ui.month) }.getOrElse { YearMonth.now() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onMonthChange(current.minusMonths(1).toString()) }) { Text("上月") }
                    Text(ui.month, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { onMonthChange(current.plusMonths(1).toString()) }) { Text("下月") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onYearChange(ui.year - 1) }) { Text("上一年") }
                    Text("${ui.year}", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { onYearChange(ui.year + 1) }) { Text("下一年") }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("总支出", money(ui.stats.expense), Expense, Modifier.weight(1f), uiStyle)
                SummaryCard("总收入", money(ui.stats.income), Income, Modifier.weight(1f), uiStyle)
            }
        }
        item {
            SummaryCard("结余", money(ui.stats.balance), Brand, Modifier.fillMaxWidth(), uiStyle)
        }
        item {
            PieChartCard(
                title = "支出分类饼图",
                centerTitle = "总支出",
                stats = ui.expenseStats,
                emptyTip = "暂无支出分类数据"
            )
        }
        item {
            PieChartCard(
                title = "收入分类饼图",
                centerTitle = "总收入",
                stats = ui.incomeStats,
                emptyTip = "暂无收入分类数据"
            )
        }
        item {
            PieChartCard(
                title = "收支对比饼图",
                centerTitle = "总收支",
                stats = listOf(
                    CategoryStat(-1, "支出", ui.stats.expense, 0),
                    CategoryStat(-2, "收入", ui.stats.income, 0)
                ),
                emptyTip = "暂无收支数据"
            )
        }
        if (ui.period == "month") {
            item {
                ExpenseCalendarCard(
                    month = ui.month,
                    calendarExpenses = ui.calendarExpenses,
                    uiStyle = uiStyle
                )
            }
        }
        item { TrendBarChartCard(if (ui.period == "month") "日度收支趋势柱状图" else "年度收支趋势柱状图", ui.trend) }
    }
}

@Composable
private fun ExpenseCalendarCard(
    month: String,
    calendarExpenses: List<CalendarExpenseDay>,
    uiStyle: AppUiStyle
) {
    val s = uiStyleTokens(uiStyle)
    val currentMonth = runCatching { YearMonth.parse(month) }.getOrElse { YearMonth.now() }
    val expenseMap = calendarExpenses.associateBy { it.date }
    val maxAmount = calendarExpenses.maxOfOrNull { it.amount } ?: 0.0
    val firstDate = currentMonth.atDay(1)
    val leadingSlots = (firstDate.dayOfWeek.value % 7)
    val totalDays = currentMonth.lengthOfMonth()
    val weekTitles = listOf("日", "一", "二", "三", "四", "五", "六")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(s.cardRadius.dp),
        colors = CardDefaults.cardColors(containerColor = s.cardColor),
        border = s.border
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("消费日历", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("深色块代表当天支出更高，点开统计时可以快速发现高消费日期。", color = TextMuted, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekTitles.forEach { title ->
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val cells = buildList {
                    repeat(leadingSlots) { add(null) }
                    repeat(totalDays) { day ->
                        add(currentMonth.atDay(day + 1))
                    }
                }
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(7) { index ->
                            val date = week.getOrNull(index)
                            val expense = date?.let { expenseMap[it.toString()] }
                            val intensity = when {
                                expense == null || maxAmount <= 0.0 -> 0f
                                else -> (expense.amount / maxAmount).coerceIn(0.0, 1.0).toFloat()
                            }
                            val bg = if (date == null) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                            } else {
                                Brand.copy(alpha = 0.10f + intensity * 0.45f)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = if (expense != null) 0.18f else 0.08f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = date?.dayOfMonth?.toString().orEmpty(),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when {
                                        expense == null -> ""
                                        expense.amount >= 1000 -> "¥${expense.amount.toInt()}"
                                        else -> "¥${moneyFmt.format(expense.amount)}"
                                    },
                                    color = if (expense != null) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    fontSize = 10.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CategoriesScreen(
    categories: List<Category>,
    uiStyle: AppUiStyle,
    onAdd: (type: String, icon: String, name: String, description: String, sort: Int) -> Unit,
    onUpdate: (id: Int, type: String, icon: String, name: String, description: String, sort: Int) -> Unit,
    onDelete: (id: Int) -> Unit
) {
    val s = uiStyleTokens(uiStyle)
    var editing by remember { mutableStateOf<Category?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        CategoryEditorDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSubmit = { type, icon, name, description, sort ->
                onAdd(type, icon, name, description, sort)
                showAdd = false
            }
        )
    }
    editing?.let { item ->
        CategoryEditorDialog(
            initial = item,
            onDismiss = { editing = null },
            onSubmit = { type, icon, name, description, sort ->
                onUpdate(item.id, type, icon, name, description, sort)
                editing = null
            }
        )
    }

    val sorted = categories.sortedWith(
        compareBy<Category> { it.type }.thenBy { it.sort ?: Int.MAX_VALUE }.thenBy { it.id }
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = s.pagePadding.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "分类管理",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) { Text("新增分类") }
        }
        if (sorted.isEmpty()) {
            EmptyTip("暂无分类")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sorted) { item ->
                Card(
                    shape = RoundedCornerShape((s.cardRadius - 4).coerceAtLeast(8).dp),
                    colors = CardDefaults.cardColors(containerColor = s.cardColor),
                    border = s.border
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${item.icon}  ${item.name}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${if (item.type == "expense") "支出" else "收入"} · sort=${item.sort ?: 99}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "编辑",
                                color = Brand,
                                modifier = Modifier
                                    .background(BrandSurface, RoundedCornerShape(8.dp))
                                    .clickable { editing = item }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Text(
                                "删除",
                                color = Expense,
                                modifier = Modifier
                                    .background(Expense.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { onDelete(item.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CategoryEditorDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSubmit: (type: String, icon: String, name: String, description: String, sort: Int) -> Unit
) {
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: "expense") }
    var icon by remember(initial?.id) { mutableStateOf(initial?.icon ?: "🍽️") }
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var sort by remember(initial?.id) { mutableStateOf((initial?.sort ?: 99).toString()) }

    val presets = if (type == "expense") expenseIconPresets else incomeIconPresets
    val templates = categoryTemplates.filter { it.type == type }
    LaunchedEffect(type) {
        if (!presets.contains(icon)) {
            icon = presets.firstOrNull() ?: icon
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (initial == null) "新增分类" else "编辑分类", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense" to "支出", "income" to "收入").forEach { (value, label) ->
                        Button(
                            onClick = { type = value },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == value) Brand else BrandSurface,
                                contentColor = if (type == value) Color.White else Color(0xFF334155)
                            )
                        ) { Text(label) }
                    }
                }

                Text("快捷模板", color = TextMuted, fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    templates.forEach { template ->
                        Text(
                            text = "${template.icon} ${template.name}",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .background(BrandSurface, RoundedCornerShape(12.dp))
                                .clickable {
                                    icon = template.icon
                                    name = template.name
                                    description = template.description
                                    sort = template.sort.toString()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 5
                ) {
                    presets.forEach { item ->
                        Text(
                            text = item,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .background(
                                    if (icon == item) Brand.copy(alpha = 0.14f) else BrandSurface,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { icon = item }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sort,
                    onValueChange = { sort = it.filter { c -> c.isDigit() } },
                    label = { Text("排序值 sort") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("排序值越小排在越前面", fontSize = 12.sp, color = TextMuted)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSubmit(type, icon, name.trim(), description.trim(), sort.toIntOrNull() ?: 99)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand)
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsScreen(
    ui: BudgetUi,
    selectedUiStyle: AppUiStyle,
    selectedTheme: AppThemePreset,
    currentUser: UserInfo?,
    exactAlarmSupported: Boolean,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    budgetAlertSettings: BudgetAlertSettingsUi,
    onUiStyleChange: (AppUiStyle) -> Unit,
    onThemeChange: (AppThemePreset) -> Unit,
    onSetBudget: (Double) -> Unit,
    onBudgetAlertChange: (Int, BudgetAlertPalette) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onRefreshReminderCapability: () -> Unit,
    onUpdateProfile: (String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val s = uiStyleTokens(selectedUiStyle)
    val budgetAccent = budgetAlertColor(ui.usagePercent, budgetAlertSettings)
    var showDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDialog) {
        SetBudgetDialog(
            onDismiss = { showDialog = false },
            onSave = {
                onSetBudget(it)
                showDialog = false
            }
        )
    }
    if (showProfileDialog) {
        ProfileEditorDialog(
            currentNickname = currentUser?.nickname.orEmpty(),
            onDismiss = { showProfileDialog = false },
            onSave = {
                onUpdateProfile(it)
                showProfileDialog = false
            }
        )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { oldPassword, newPassword ->
                onChangePassword(oldPassword, newPassword)
                showPasswordDialog = false
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = s.pagePadding.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("设置", selectedUiStyle)
        currentUser?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(s.cardRadius.dp),
                colors = CardDefaults.cardColors(containerColor = s.cardColor),
                border = s.border
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前账号", fontWeight = FontWeight.SemiBold)
                    Text(user.nickname, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(user.email, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("修改昵称")
                        }
                        OutlinedButton(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("修改密码")
                        }
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Expense.copy(alpha = 0.9f))
                    ) {
                        Text("退出登录")
                    }
                }
            }
        }
        SummaryCard("月度预算", money(ui.budget.budget), Brand, Modifier.fillMaxWidth(), selectedUiStyle)
        SummaryCard("已支出", money(ui.budget.spent), Expense, Modifier.fillMaxWidth(), selectedUiStyle)
        SummaryCard("预算剩余", money(ui.budget.remaining), budgetAccent, Modifier.fillMaxWidth(), selectedUiStyle)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(s.cardRadius.dp),
            colors = CardDefaults.cardColors(containerColor = s.cardColor),
            border = s.border
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("每日可用 ${money(ui.dailyAvailable)}", fontWeight = FontWeight.SemiBold)
                Text("剩余 ${ui.daysRemaining} 天", color = TextMuted)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(s.cardRadius.dp),
            colors = CardDefaults.cardColors(containerColor = s.cardColor),
            border = s.border
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("预算预警色阶", fontWeight = FontWeight.SemiBold)
                Text(
                    "达到 ${budgetAlertSettings.warningPercent}% 后切换预警色，超过 100% 使用危险色。",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                Text("预警阈值 ${budgetAlertSettings.warningPercent}%", color = budgetAccent, fontWeight = FontWeight.Bold)
                Slider(
                    value = budgetAlertSettings.warningPercent.toFloat(),
                    onValueChange = {
                        onBudgetAlertChange(it.toInt(), budgetAlertSettings.palette)
                    },
                    valueRange = 50f..100f
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        BudgetAlertPalette.Sunset,
                        BudgetAlertPalette.Ocean,
                        BudgetAlertPalette.Rose,
                        BudgetAlertPalette.Plum
                    ).chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { palette ->
                                val active = budgetAlertSettings.palette == palette
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (active) Color(palette.warningHex).copy(alpha = 0.16f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                        )
                                        .border(
                                            1.dp,
                                            if (active) Color(palette.warningHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onBudgetAlertChange(budgetAlertSettings.warningPercent, palette) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(palette.normalHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(palette.warningHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(palette.dangerHex))
                                        )
                                    }
                                    Text(
                                        text = palette.label,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand)
        ) {
            Text("设置预算")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(s.cardRadius.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = s.border
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("界面套装", fontWeight = FontWeight.SemiBold)
                Text("会明显改变卡片圆角、边框、留白和整体质感", color = TextMuted, fontSize = 13.sp)
                val styles = listOf(
                    AppUiStyle.Pulse to "柔和圆角 · 轻盈层次",
                    AppUiStyle.Neo to "利落边框 · 信息清晰",
                    AppUiStyle.Aurora to "玻璃流光 · 更偏氛围",
                    AppUiStyle.Mono to "杂志感 · 黑白对比"
                )
                styles.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (style, desc) ->
                            val active = selectedUiStyle == style
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape((s.accentRadius + 2).dp))
                                    .background(if (active) Brand.copy(alpha = 0.16f) else BrandSurface)
                                    .border(
                                        1.dp,
                                        if (active) Brand.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        RoundedCornerShape((s.accentRadius + 2).dp)
                                    )
                                    .clickable { onUiStyleChange(style) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    color = if (active) Brand else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(desc, color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("定时提醒", fontWeight = FontWeight.SemiBold)
                        Text("每天提醒记账", color = TextMuted, fontSize = 13.sp)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onReminderEnabledChange(it) }
                    )
                }
                if (!exactAlarmSupported) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("系统未授予精确闹钟权限", fontWeight = FontWeight.SemiBold)
                            Text(
                                "这会导致应用不常驻后台时，提醒可能延迟或不触发。开启后系统可在后台静默唤醒广播，无需在概览界面保留任务。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenExactAlarmSettings,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("开启精确提醒")
                                }
                                OutlinedButton(
                                    onClick = onRefreshReminderCapability,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("刷新状态")
                                }
                            }
                        }
                    }
                } else {
                    Text("已具备后台精确提醒能力", color = Income, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onReminderTimeChange(hour, minute) },
                            reminderHour,
                            reminderMinute,
                            true
                        ).show()
                    },
                    enabled = reminderEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("提醒时间  ${String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)}")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("主题外观", fontWeight = FontWeight.SemiBold)
                Text("每套主题会同步改变品牌主色、背景和强调色", color = TextMuted, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("明亮系", color = TextMuted, fontSize = 12.sp)
                    Text("暗黑系", color = TextMuted, fontSize = 12.sp)
                }
                val lightThemes = listOf(
                    AppThemePreset.Ocean,
                    AppThemePreset.Mint,
                    AppThemePreset.Sunset,
                    AppThemePreset.Graphite
                )
                val darkThemes = listOf(
                    AppThemePreset.Midnight,
                    AppThemePreset.Obsidian,
                    AppThemePreset.Nocturne,
                    AppThemePreset.Carbon
                )
                repeat(4) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val left = lightThemes[index]
                        val leftActive = selectedTheme == left
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (leftActive) Brand.copy(alpha = 0.16f) else BrandSurface)
                                .border(
                                    1.dp,
                                    if (leftActive) Brand.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onThemeChange(left) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = left.label,
                                color = if (leftActive) Brand else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Brand, RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Income, RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Expense, RoundedCornerShape(10.dp))
                                )
                            }
                        }

                        val right = darkThemes[index]
                        val rightActive = selectedTheme == right
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (rightActive) Brand.copy(alpha = 0.16f) else BrandSurface)
                                .border(
                                    1.dp,
                                    if (rightActive) Brand.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onThemeChange(right) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = right.label,
                                color = if (rightActive) Brand else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Brand, RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Income, RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Expense, RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ProfileEditorDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nickname by remember(currentNickname) { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("新的昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(nickname.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("旧密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(oldPassword, newPassword) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    selectedTheme: AppThemePreset,
    authState: AuthUiState,
    rememberPasswordEnabled: Boolean,
    onRememberPasswordChange: (Boolean) -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (nickname: String, email: String, code: String, password: String) -> Unit,
    onSendCode: (email: String) -> Unit
) {
    var mode by rememberSaveable { mutableStateOf("login") }
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var registerNickname by rememberSaveable { mutableStateOf("") }
    var registerEmail by rememberSaveable { mutableStateOf("") }
    var registerCode by rememberSaveable { mutableStateOf("") }
    var registerPassword by rememberSaveable { mutableStateOf("") }

    val accent = Brand
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)

    LaunchedEffect(Unit) {
        if (rememberPasswordEnabled) {
            loginEmail = AuthSessionStore.rememberedEmail()
            loginPassword = AuthSessionStore.rememberedPassword()
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        BrandSurface.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                Brand.copy(alpha = 0.92f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Money Cloud", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
                        Text("先登录，再进入你的专属账本。", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        Text(
                            "安卓端已切换到 Gateway + User + Monee 微服务接口。",
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("login" to "登录", "register" to "注册").forEach { (value, label) ->
                    val active = mode == value
                    Text(
                        text = label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) accent else surface)
                            .border(
                                1.dp,
                                if (active) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { mode = value }
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (mode == "login") {
                        Text("邮箱登录", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("登录后所有账目接口都会自动附带你的 Token。", color = TextMuted, fontSize = 13.sp)
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it.trim() },
                            label = { Text("邮箱") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("记住密码", fontWeight = FontWeight.SemiBold)
                                Text("下次打开应用时自动填充账号与密码", color = TextMuted, fontSize = 12.sp)
                            }
                            Switch(
                                checked = rememberPasswordEnabled,
                                onCheckedChange = { onRememberPasswordChange(it) }
                            )
                        }
                        Button(
                            onClick = { onLogin(loginEmail, loginPassword) },
                            enabled = !authState.loginSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text(if (authState.loginSubmitting) "登录中..." else "立即登录")
                        }
                    } else {
                        Text("注册账号", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("注册完成后直接回到登录页即可进入记账系统。", color = TextMuted, fontSize = 13.sp)
                        OutlinedTextField(
                            value = registerNickname,
                            onValueChange = { registerNickname = it },
                            label = { Text("昵称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = { registerEmail = it.trim() },
                            label = { Text("邮箱") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = registerCode,
                                onValueChange = { registerCode = it },
                                label = { Text("验证码") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = { onSendCode(registerEmail) },
                                enabled = !authState.sendingCode,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(if (authState.sendingCode) "发送中" else "发送验证码")
                            }
                        }
                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = { registerPassword = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                onRegister(registerNickname, registerEmail, registerCode, registerPassword)
                            },
                            enabled = !authState.registerSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text(if (authState.registerSubmitting) "提交中..." else "完成注册")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, uiStyle: AppUiStyle) {
    val bg = when (uiStyle) {
        AppUiStyle.Aurora -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            )
        )
        AppUiStyle.Mono -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )
        )
        else -> Brush.horizontalGradient(listOf(BrandSurface, BrandSurface))
    }
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    uiStyle: AppUiStyle = AppUiStyle.Pulse
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
        }
    }
}

@Composable
private fun MiniMetricCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(title, color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatRow(title: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PieChartCard(
    title: String,
    centerTitle: String,
    stats: List<CategoryStat>,
    emptyTip: String
) {
    val validStats = stats.filter { it.amount > 0 }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (validStats.isEmpty()) {
                EmptyTip(emptyTip)
            } else {
                val total = validStats.sumOf { it.amount }
                val chartColors = chartPalette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(188.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier.matchParentSize()
                        ) {
                            var start = -90f
                            validStats.forEachIndexed { index, stat ->
                                val sweep = (stat.amount / total * 360f).toFloat()
                                drawArc(
                                    color = chartColors[index % chartColors.size],
                                    startAngle = start,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 34f)
                                )
                                start += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(centerTitle, color = TextMuted, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                money(total),
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for ((index, stat) in validStats.withIndex()) {
                            val percent = stat.amount / total * 100
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(chartColors[index % chartColors.size], RoundedCornerShape(12.dp))
                                    )
                                    Text(stat.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                                Text(
                                    "${money(stat.amount)} · ${"%.1f".format(percent)}%",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }                
            }
        }
    }
}

@Composable
private fun TrendBarChartCard(title: String, trend: TrendData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            val labels = if (trend.labels.isNotEmpty()) trend.labels else trend.dates.map { it.takeLast(5) }
            val expenses = trend.expenses
            val incomes = trend.incomes
            val maxValue = (expenses + incomes).maxOrNull()?.takeIf { it > 0 } ?: 1.0

            if (labels.isEmpty() || expenses.isEmpty() || incomes.isEmpty()) {
                EmptyTip("暂无趋势数据")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(labels.size) { index ->
                        val expense = expenses.getOrElse(index) { 0.0 }
                        val income = incomes.getOrElse(index) { 0.0 }
                        val expenseH = (expense / maxValue * 90).toFloat().coerceAtLeast(2f)
                        val incomeH = (income / maxValue * 90).toFloat().coerceAtLeast(2f)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.height(100.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = expenseH.dp)
                                        .background(Expense.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = incomeH.dp)
                                        .background(Income.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(labels[index], fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("■ 支出", color = Expense, fontSize = 12.sp)
                    Text("■ 收入", color = Income, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: RecordItem,
    showDelete: Boolean,
    showEdit: Boolean,
    onDelete: (Int) -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        BrandSurface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        BrandSurface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(enabled = showEdit, onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("${record.categoryName} · ${record.date}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (!record.remark.isNullOrBlank()) {
                Text(record.remark, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        Text(
            text = (if (record.type == "expense") "-" else "+") + money(record.amount),
            color = if (record.type == "expense") Expense else Income,
            fontWeight = FontWeight.Bold
        )
        if (showDelete) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = "删除",
                color = Expense,
                modifier = Modifier
                    .background(Expense.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .clickable { onDelete(record.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptyTip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
            .padding(vertical = 24.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AddRecordDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSubmit: (type: String, categoryId: Int, amount: Double, remark: String, date: String) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("expense") }
    var categoryId by remember(categories) { mutableStateOf(defaultCategoryId(categories, "expense")) }
    var date by remember { mutableStateOf(todayString()) }
    var amountExpression by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    val currentCategories = categories.filter { it.type == type }
    val amountValue = evaluateAmountExpression(amountExpression)
    val amountDisplay = if (amountValue <= 0.0) "0" else moneyFmt.format(amountValue)

    LaunchedEffect(type, categories) {
        val valid = categories.any { it.id == categoryId && it.type == type }
        if (!valid) {
            categoryId = defaultCategoryId(categories, type)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("记一笔", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "关闭",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = "expense"
                            categoryId = defaultCategoryId(categories, "expense")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "expense") Expense else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            contentColor = if (type == "expense") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("支出") }
                    Button(
                        onClick = {
                            type = "income"
                            categoryId = defaultCategoryId(categories, "income")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") Income else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            contentColor = if (type == "income") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("收入") }
                }

                if (currentCategories.isEmpty()) {
                    Text("暂无可选分类", color = TextMuted, fontSize = 13.sp)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4
                    ) {
                        currentCategories.take(8).forEach { c ->
                            Text(
                                text = "${c.icon} ${c.name}",
                                color = if (categoryId == c.id) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .background(
                                        if (categoryId == c.id) Brand else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { categoryId = c.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("输入备注") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("金额", color = TextMuted)
                    Text("¥$amountDisplay", color = Color(0xFFFF5C89), fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = date,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .background(BrandSurface, RoundedCornerShape(12.dp))
                            .clickable {
                                showDatePicker(context, date) {
                                    date = it
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    Text(
                        text = "今天",
                        color = Color.White,
                        modifier = Modifier
                            .background(Brand, RoundedCornerShape(12.dp))
                            .clickable { date = todayString() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        listOf("1", "2", "3", "删"),
                        listOf("4", "5", "6", "+"),
                        listOf("7", "8", "9", "-"),
                        listOf(".", "0", "00", "完成")
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                KeyPadButton(
                                    text = key,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (key) {
                                            "删" -> {
                                                if (amountExpression.isNotEmpty()) {
                                                    amountExpression = amountExpression.dropLast(1)
                                                }
                                            }
                                            "+" -> amountExpression = appendOperator(amountExpression, '+')
                                            "-" -> amountExpression = appendOperator(amountExpression, '-')
                                            "." -> amountExpression = appendDot(amountExpression)
                                            "完成" -> {
                                                onSubmit(type, categoryId, amountValue, remark, date)
                                            }
                                            "00" -> amountExpression = appendDigit(amountExpression, "00")
                                            else -> amountExpression = appendDigit(amountExpression, key)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditRecordDialog(
    record: RecordItem,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (type: String, categoryId: Int, amount: Double, remark: String, date: String) -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var type by remember(record.id) { mutableStateOf(record.type) }
    var categoryId by remember(record.id) { mutableStateOf(record.categoryId) }
    var date by remember(record.id) { mutableStateOf(record.date) }
    var remark by remember(record.id) { mutableStateOf(record.remark.orEmpty()) }
    var amountExpression by remember(record.id) { mutableStateOf(moneyFmt.format(record.amount)) }

    val currentCategories = categories.filter { it.type == type }
    val amountValue = evaluateAmountExpression(amountExpression)
    val amountDisplay = if (amountValue <= 0.0) "0.00" else moneyFmt.format(amountValue)

    LaunchedEffect(type, categories) {
        val valid = categories.any { it.id == categoryId && it.type == type }
        if (!valid) {
            categoryId = defaultCategoryId(categories, type)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("编辑记录", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = "expense"
                            categoryId = defaultCategoryId(categories, "expense")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "expense") Expense else BrandSurface,
                            contentColor = if (type == "expense") Color.White else Color(0xFF1E293B)
                        )
                    ) { Text("支出") }
                    Button(
                        onClick = {
                            type = "income"
                            categoryId = defaultCategoryId(categories, "income")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") Income else BrandSurface,
                            contentColor = if (type == "income") Color.White else Color(0xFF1E293B)
                        )
                    ) { Text("收入") }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentCategories) { c ->
                        Text(
                            text = "${c.icon} ${c.name}",
                            color = if (categoryId == c.id) Color.White else Color(0xFF334155),
                            modifier = Modifier
                                .background(
                                    if (categoryId == c.id) Brand else BrandSurface,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { categoryId = c.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("金额", color = TextMuted)
                    Text("¥$amountDisplay", color = Brand, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = date,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .background(BrandSurface, RoundedCornerShape(12.dp))
                            .clickable { showDatePicker(context, date) { date = it } }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    Text(
                        text = "今天",
                        color = Color.White,
                        modifier = Modifier
                            .background(Brand, RoundedCornerShape(12.dp))
                            .clickable { date = todayString() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        listOf("1", "2", "3", "删"),
                        listOf("4", "5", "6", "+"),
                        listOf("7", "8", "9", "-"),
                        listOf(".", "0", "00", "完成")
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                KeyPadButton(
                                    text = key,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (key) {
                                            "删" -> if (amountExpression.isNotEmpty()) {
                                                amountExpression = amountExpression.dropLast(1)
                                            }
                                            "+" -> amountExpression = appendOperator(amountExpression, '+')
                                            "-" -> amountExpression = appendOperator(amountExpression, '-')
                                            "." -> amountExpression = appendDot(amountExpression)
                                            "完成" -> onUpdate(type, categoryId, amountValue, remark, date)
                                            "00" -> amountExpression = appendDigit(amountExpression, "00")
                                            else -> amountExpression = appendDigit(amountExpression, key)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Expense)
                    ) { Text("删除", color = Color.White) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandSurface, contentColor = Color(0xFF1E293B))
                    ) { Text("取消") }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定删除这条记录吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) { Text("删除", color = Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun KeyPadButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (background, contentColor) = when (text) {
        "完成" -> Brand to Color.White
        "删", "+", "-", "00" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) to MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f) to MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
    }
}

private fun appendDigit(expression: String, key: String): String {
    if (expression.length >= 18) return expression
    return expression + key
}

private fun appendOperator(expression: String, operator: Char): String {
    if (expression.isEmpty()) return expression
    val last = expression.last()
    return if (last == '+' || last == '-') {
        expression.dropLast(1) + operator
    } else {
        expression + operator
    }
}

private fun appendDot(expression: String): String {
    if (expression.length >= 18) return expression
    if (expression.isEmpty() || expression.last() == '+' || expression.last() == '-') return expression + "0."
    val segment = expression.takeLastWhile { it != '+' && it != '-' }
    if (segment.contains('.')) return expression
    return expression + "."
}

private fun evaluateAmountExpression(expression: String): Double {
    val clean = expression.trim().trimEnd('+', '-')
    if (clean.isEmpty()) return 0.0

    var total = 0.0
    var current = StringBuilder()
    var sign = 1.0

    clean.forEachIndexed { index, c ->
        when {
            c.isDigit() || c == '.' -> current.append(c)
            c == '+' || c == '-' -> {
                val number = current.toString().toDoubleOrNull() ?: 0.0
                total += sign * number
                current = StringBuilder()
                sign = if (c == '+') 1.0 else -1.0
            }
            index == clean.lastIndex -> {
                // no-op, only to keep exhaustive when with index available
            }
        }
    }

    if (current.isNotEmpty()) {
        val number = current.toString().toDoubleOrNull() ?: 0.0
        total += sign * number
    }
    return total
}

private fun showDatePicker(context: Context, currentDate: String, onSelect: (String) -> Unit) {
    val parts = currentDate.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 1

    DatePickerDialog(
        context,
        { _, y, m, d ->
            onSelect(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        },
        year,
        month,
        day
    ).show()
}

@Composable
fun SetBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置预算") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("月度预算金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount.toDoubleOrNull() ?: 0.0) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
