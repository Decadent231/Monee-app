package com.money.codex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.money.codex.ui.screens.AddRecordDialog
import com.money.codex.ui.screens.AuthScreen
import com.money.codex.ui.screens.CategoriesScreen
import com.money.codex.ui.screens.DashboardScreen
import com.money.codex.ui.screens.EditRecordDialog
import com.money.codex.ui.screens.RecordsScreen
import com.money.codex.ui.screens.SettingsScreen
import com.money.codex.ui.screens.StatisticsScreen
import com.money.codex.ui.theme.AppBackground
import com.money.codex.ui.theme.Brand
import com.money.codex.ui.theme.BrandSurface
import com.money.codex.ui.theme.Expense
import com.money.codex.ui.theme.Income
import com.money.codex.ui.theme.MoneyTheme

class MainActivity : ComponentActivity() {
    private var externalAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalAction = intent?.action
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            MoneyTheme(themePreset = vm.selectedTheme) {
                MoneyApp(
                    vm = vm,
                    externalAction = externalAction,
                    onExternalActionConsumed = { externalAction = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalAction = intent.action
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
private fun MoneyApp(
    vm: MainViewModel,
    externalAction: String?,
    onExternalActionConsumed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarTone by remember { mutableStateOf(MessageTone.Info) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        vm.loadReminderSettings(context)
    }

    LaunchedEffect(vm.toastMessage) {
        vm.toastMessage?.let { message ->
            snackbarTone = message.tone
            snackbarHostState.showSnackbar(message.text)
            vm.clearToast()
        }
    }

    LaunchedEffect(externalAction, vm.authState.isAuthenticated) {
        if (externalAction == ReminderScheduler.ACTION_QUICK_ADD && vm.authState.isAuthenticated) {
            vm.openQuickAdd()
            onExternalActionConsumed()
        }
    }

    if (vm.authState.authLoading) {
        LaunchingScreen()
        return
    }

    if (!vm.authState.isAuthenticated) {
        Scaffold(
            containerColor = AppBackground,
            snackbarHost = {
                AppSnackbarHost(
                    hostState = snackbarHostState,
                    tone = snackbarTone
                )
            }
        ) { padding ->
            AuthScreen(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                selectedTheme = vm.selectedTheme,
                authState = vm.authState,
                rememberPasswordEnabled = vm.rememberPasswordEnabled,
                onRememberPasswordChange = vm::updateRememberPasswordEnabled,
                onLogin = vm::login,
                onRegister = vm::register,
                onSendCode = vm::sendRegisterCode
            )
        }
        return
    }

    if (vm.addRecordDialogVisible) {
        AddRecordDialog(
            categories = vm.categories,
            onDismiss = { vm.showAddRecordDialog(false) },
            onSubmit = { type, categoryId, amount, remark, date ->
                vm.addRecord(type, categoryId, amount, remark, date)
            }
        )
    }

    vm.editingRecord?.let { record ->
        EditRecordDialog(
            record = record,
            categories = vm.categories,
            onDismiss = { vm.closeEditRecord() },
            onDelete = { vm.deleteRecord(record.id) },
            onUpdate = { type, categoryId, amount, remark, date ->
                vm.updateRecord(record.id, type, categoryId, amount, remark, date)
            }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = {
            AppSnackbarHost(
                hostState = snackbarHostState,
                tone = snackbarTone
            )
        },
        bottomBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                ) {
                    listOf(
                        AppTab.Dashboard to Pair(Icons.Default.Home, "概览"),
                        AppTab.Records to Pair(Icons.AutoMirrored.Filled.List, "记账"),
                        AppTab.Statistics to Pair(Icons.Default.BarChart, "统计"),
                        AppTab.Categories to Pair(Icons.Default.Apps, "分类"),
                        AppTab.Settings to Pair(Icons.Default.Settings, "设置")
                    ).forEach { (tab, iconLabel) ->
                        NavigationBarItem(
                            selected = vm.currentTab == tab,
                            onClick = { vm.selectTab(tab) },
                            icon = { Icon(iconLabel.first, contentDescription = iconLabel.second) },
                            label = { Text(iconLabel.second) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (vm.currentTab != AppTab.Settings) {
                FloatingActionButton(
                    onClick = { vm.showAddRecordDialog(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(58.dp)
                        .width(58.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                        )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
        val pullRefreshState = rememberPullRefreshState(
            refreshing = vm.isLoading,
            onRefresh = { vm.refreshAll() }
        )
        Box(modifier = contentModifier.pullRefresh(pullRefreshState)) {
            when (vm.currentTab) {
                AppTab.Dashboard -> Box(Modifier.fillMaxSize()) {
                    DashboardScreen(
                        ui = vm.dashboard,
                        uiStyle = vm.selectedUiStyle,
                        currentUserName = vm.authState.currentUser?.nickname ?: "Yee",
                        budgetAlertSettings = vm.budgetAlertSettings
                    ) { vm.openEditRecord(it) }
                }

                AppTab.Records -> Box(Modifier.fillMaxSize()) {
                    RecordsScreen(
                        ui = vm.records,
                        uiStyle = vm.selectedUiStyle,
                        filters = vm.recordFilters,
                        categories = vm.categories,
                        onFiltersChange = { startDate, endDate, type, categoryId, keyword ->
                            vm.updateRecordFilters(startDate, endDate, type, categoryId, keyword)
                        },
                        onSearch = { vm.searchRecords() },
                        onReset = { vm.resetRecordFilters() },
                        onDelete = { vm.deleteRecord(it) },
                        onEdit = { vm.openEditRecord(it) }
                    )
                }

                AppTab.Statistics -> Box(Modifier.fillMaxSize()) {
                    StatisticsScreen(
                        ui = vm.statistics,
                        uiStyle = vm.selectedUiStyle,
                        onPeriodChange = { vm.changeStatisticsPeriod(it) },
                        onMonthChange = { vm.changeStatisticsMonth(it) },
                        onYearChange = { vm.changeStatisticsYear(it) }
                    )
                }

                AppTab.Categories -> Box(Modifier.fillMaxSize()) {
                    CategoriesScreen(
                        categories = vm.categories,
                        uiStyle = vm.selectedUiStyle,
                        onAdd = { type, icon, name, description, sort ->
                            vm.addCategory(type, icon, name, description, sort)
                        },
                        onUpdate = { id, type, icon, name, description, sort ->
                            vm.updateCategory(id, type, icon, name, description, sort)
                        },
                        onDelete = { id ->
                            vm.deleteCategory(id)
                        }
                    )
                }

                AppTab.Settings -> Box(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        ui = vm.budget,
                        selectedUiStyle = vm.selectedUiStyle,
                        selectedTheme = vm.selectedTheme,
                        currentUser = vm.authState.currentUser,
                        exactAlarmSupported = vm.reminderCapability.exactAlarmSupported,
                        reminderEnabled = vm.reminderEnabled,
                        reminderHour = vm.reminderHour,
                        reminderMinute = vm.reminderMinute,
                        budgetAlertSettings = vm.budgetAlertSettings,
                        onUiStyleChange = { vm.setUiStyle(it) },
                        onThemeChange = { vm.setTheme(it) },
                        onSetBudget = { vm.setBudget(it) },
                        onBudgetAlertChange = vm::updateBudgetAlertSettings,
                        onReminderEnabledChange = { enabled ->
                            if (
                                enabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            vm.setReminderEnabled(context, enabled)
                        },
                        onReminderTimeChange = { hour, minute -> vm.setReminderTime(context, hour, minute) },
                        onOpenExactAlarmSettings = {
                            ReminderScheduler.exactAlarmSettingsIntent(context)?.let(context::startActivity)
                        },
                        onRefreshReminderCapability = { vm.refreshReminderCapability(context) },
                        onUpdateProfile = vm::updateProfile,
                        onChangePassword = vm::changePassword,
                        onLogout = vm::logout
                    )
                }
            }
            PullRefreshIndicator(
                refreshing = vm.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun LaunchingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        BrandSurface.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(132.dp)
                .height(132.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(36.dp))
                .background(Brand.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(160.dp)
                .height(160.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(48.dp))
                .background(Income.copy(alpha = 0.10f))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(vertical = 48.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = "Monee",
                        color = Brand,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "让每一笔记录，\n都更有秩序。",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "正在同步你的个人账本、预算与分类设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(72.dp)
                        .height(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                        .background(Brand.copy(alpha = 0.9f))
                )
            }
        }
    }
}

@Composable
private fun AppSnackbarHost(
    hostState: SnackbarHostState,
    tone: MessageTone
) {
    val background = when (tone) {
        MessageTone.Success -> Income.copy(alpha = 0.92f)
        MessageTone.Error -> Expense.copy(alpha = 0.92f)
        MessageTone.Info -> Brand.copy(alpha = 0.92f)
    }
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = background,
            contentColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            actionColor = Color.White
        )
    }
}
