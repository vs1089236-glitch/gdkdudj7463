package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.HoyoPayRepository
import com.example.localization.AppLanguage
import com.example.localization.HoyoStrings
import com.example.ui.components.AddPayoutMethodDialog
import com.example.ui.components.AddReferralDialog
import com.example.ui.components.LogActivityDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.HoyoPayViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.UiState
import com.example.utils.CsvExportHelper
import com.example.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore

private enum class HoyoTab(val route: String, val icon: ImageVector) {
    HOME("home", Icons.Default.Home),
    ANALYTICS("analytics", Icons.Default.Analytics),
    LEDGER("ledger", Icons.Default.ReceiptLong),
    PAYOUTS("payouts", Icons.Default.AccountBalanceWallet),
    ADMIN("admin", Icons.Default.AdminPanelSettings),
    SETTINGS("settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentUserId: String,
    prefs: AppPreferences,
    prefsRepo: UserPreferencesRepository,
    strings: HoyoStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HoyoPayViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HoyoPayViewModel(
                    repository = HoyoPayRepository(db),
                    currentUserId = currentUserId,
                    pendingReferralCode = prefs.pendingReferralCode,
                    pendingMobileNumber = prefs.pendingMobileNumber,
                    onClearPendingAuthInputs = { prefsRepo.clearPendingAuthInputs() }
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(HoyoTab.HOME) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showAddReferralDialog by remember { mutableStateOf(false) }
    var showAddPayoutMethodDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline = rememberIsOnline(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationsToggle(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.DeviceNotification -> {
                    if (prefs.notificationsEnabled) {
                        NotificationHelper.showStatusNotification(context, event.title, event.body)
                    }
                }
            }
        }
    }

    BackHandler(enabled = currentTab != HoyoTab.HOME) {
        currentTab = HoyoTab.HOME
    }

    val visibleTabs = remember(isAdmin) {
        if (isAdmin) {
            listOf(
                HoyoTab.HOME,
                HoyoTab.ANALYTICS,
                HoyoTab.LEDGER,
                HoyoTab.PAYOUTS,
                HoyoTab.ADMIN,
                HoyoTab.SETTINGS
            )
        } else {
            listOf(
                HoyoTab.HOME,
                HoyoTab.ANALYTICS,
                HoyoTab.LEDGER,
                HoyoTab.PAYOUTS,
                HoyoTab.SETTINGS
            )
        }
    }

    if (currentTab !in visibleTabs) {
        currentTab = HoyoTab.HOME
    }

    fun tabLabel(tab: HoyoTab): String = when (tab) {
        HoyoTab.HOME -> strings.tabHome
        HoyoTab.ANALYTICS -> strings.tabAnalytics
        HoyoTab.LEDGER -> strings.tabLedger
        HoyoTab.PAYOUTS -> strings.tabPayouts
        HoyoTab.ADMIN -> strings.tabAdmin
        HoyoTab.SETTINGS -> strings.tabSettings
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(strings.appName, style = MaterialTheme.typography.headlineSmall) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        TextButton(
                            onClick = {
                                val nextLang = if (prefs.language == AppLanguage.EN) AppLanguage.HI else AppLanguage.EN
                                onLanguageChange(nextLang)
                            },
                            modifier = Modifier.testTag("topbar_language_toggle")
                        ) {
                            Text(if (prefs.language == AppLanguage.EN) "हिन्दी" else "EN")
                        }
                        IconButton(
                            onClick = { showNotificationsDialog = true },
                            modifier = Modifier.testTag("notifications_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (notifications.isNotEmpty()) {
                                        Badge { Text("${notifications.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = strings.notificationsTitle)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        visibleTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        visibleTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab)) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (val state = uiState) {
                            UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { currentTab = HoyoTab.HOME }) {
                                        Text(strings.retry)
                                    }
                                }
                            }
                            is UiState.Success -> {
                                val data = state.data
                                val exportAction: () -> Unit = {
                                    CsvExportHelper.exportLedgerCsv(
                                        context = context,
                                        transactions = data.transactions,
                                        payouts = data.payoutRequests
                                    ).fold(
                                        onSuccess = { intent -> context.startActivity(intent) },
                                        onFailure = { err ->
                                            viewModel.emitSnackbar(err.localizedMessage ?: "Export failed")
                                        }
                                    )
                                }

                                when (currentTab) {
                                    HoyoTab.HOME -> DashboardScreen(
                                        data = data,
                                        strings = strings,
                                        currentUserId = currentUserId,
                                        isOnline = isOnline,
                                        onOpenLogActivity = { showLogDialog = true },
                                        onOpenPayoutsTab = { currentTab = HoyoTab.PAYOUTS },
                                        onExportCsv = exportAction,
                                        onOpenAddReferral = { showAddReferralDialog = true },
                                        onDeletePendingTx = { viewModel.deletePendingTransaction(it) },
                                        onNotify = { viewModel.emitSnackbar(it) }
                                    )
                                    HoyoTab.ANALYTICS -> AnalyticsScreen(
                                        data = data,
                                        strings = strings
                                    )
                                    HoyoTab.LEDGER -> LedgerAndReferralsScreen(
                                        data = data,
                                        strings = strings,
                                        onOpenLogActivity = { showLogDialog = true },
                                        onOpenAddReferral = { showAddReferralDialog = true },
                                        onExportCsv = exportAction,
                                        onDeletePendingTx = { viewModel.deletePendingTransaction(it) }
                                    )
                                    HoyoTab.PAYOUTS -> PayoutsScreen(
                                        data = data,
                                        strings = strings,
                                        isSubmitting = isSubmitting,
                                        onOpenAddMethod = { showAddPayoutMethodDialog = true },
                                        onDeleteMethod = { viewModel.deletePayoutMethod(it) },
                                        onSubmitPayout = { amountStr, method ->
                                            viewModel.requestPayout(
                                                userName = data.profile?.displayName ?: "Hoyo Member",
                                                amountRupeesStr = amountStr,
                                                method = method,
                                                availableBalancePaise = data.summary.availableBalancePaise
                                            )
                                        }
                                    )
                                    HoyoTab.ADMIN -> AdminMonitorScreen(
                                        data = data,
                                        strings = strings,
                                        onVerifyTransaction = { id, status ->
                                            viewModel.adminVerifyTransaction(id, status)
                                        },
                                        onUpdatePayout = { id, status, utr ->
                                            viewModel.adminUpdatePayout(id, status, utr, null)
                                        }
                                    )
                                    HoyoTab.SETTINGS -> SettingsAndSupportScreen(
                                        data = data,
                                        prefs = prefs,
                                        strings = strings,
                                        onSaveProfile = { name, phone, refBy, bio ->
                                            viewModel.updateProfile(name, phone, refBy, bio)
                                        },
                                        onLanguageChange = onLanguageChange,
                                        onThemeChange = onThemeChange,
                                        onBiometricToggle = onBiometricToggle,
                                        onNotificationsToggle = { enable ->
                                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !NotificationHelper.hasNotificationPermission(context)
                                            ) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                onNotificationsToggle(enable)
                                            }
                                        },
                                        onOpenTutorial = onOpenTutorial,
                                        onSignOut = onSignOut
                                    )
                                }

                                if (showLogDialog) {
                                    LogActivityDialog(
                                        strings = strings,
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showLogDialog = false },
                                        onSubmit = { type, title, orderRef, amount, rate, notes ->
                                            viewModel.addTransaction(
                                                userName = data.profile?.displayName ?: "Hoyo Member",
                                                type = type,
                                                title = title,
                                                clientOrOrderRef = orderRef,
                                                amountRupeesStr = amount,
                                                ratePercentStr = rate,
                                                notes = notes
                                            )
                                            showLogDialog = false
                                        }
                                    )
                                }

                                if (showAddReferralDialog) {
                                    AddReferralDialog(
                                        strings = strings,
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showAddReferralDialog = false },
                                        onSubmit = { invitee, contact ->
                                            val code = data.profile?.referralCode ?: "HOYOPAY"
                                            viewModel.addReferralInvite(invitee, contact, code)
                                            showAddReferralDialog = false
                                        }
                                    )
                                }

                                if (showAddPayoutMethodDialog) {
                                    AddPayoutMethodDialog(
                                        strings = strings,
                                        isSubmitting = isSubmitting,
                                        onDismiss = { showAddPayoutMethodDialog = false },
                                        onSubmit = { type, label, holder, upi, bank, last4, ifsc, primary ->
                                            viewModel.addPayoutMethod(
                                                methodType = type,
                                                label = label,
                                                accountHolderName = holder,
                                                upiId = upi,
                                                bankName = bank,
                                                accountLast4 = last4,
                                                ifscCode = ifsc,
                                                primaryMethod = primary
                                            )
                                            showAddPayoutMethodDialog = false
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

    if (showNotificationsDialog) {
        NotificationsDialog(
            strings = strings,
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false }
        )
    }
}

@Composable
private fun rememberIsOnline(context: Context): Boolean {
    val cm = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var isOnline by remember {
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        mutableStateOf(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
    }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }
            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
    return isOnline
}
