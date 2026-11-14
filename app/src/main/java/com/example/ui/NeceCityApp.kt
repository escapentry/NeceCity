package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AppModePreferences
import com.example.data.NeceCityRepository
import com.example.data.WallpaperRepository
import com.example.data.WallpaperState
import com.example.data.local.LocalNeceCityRepository
import com.example.data.local.NeceCityLocalDatabase
import com.example.display.DisplayAutoScaler
import com.example.display.NeceCityDisplayProfile
import com.example.model.NeceCityTab
import com.example.ui.components.AIOMediaAtmosphericBackground
import com.example.ui.components.AuthScreen
import com.example.ui.components.BackdropThemePickerSheetContent
import com.example.ui.components.CalculatorView
import com.example.ui.components.CalendarView
import com.example.ui.components.ClockView
import com.example.ui.components.NeceCityLoadingSplash
import com.example.ui.components.NotesView
import com.example.ui.components.performGoogleSignOut
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun NeceCityApp() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val modePreferences = remember(context) { AppModePreferences(context.applicationContext) }
  val isOfflineMode by modePreferences.isOfflineModeFlow.collectAsStateWithLifecycle(initialValue = null)
  val hasFirebaseApp = remember(context) { FirebaseApp.getApps(context).isNotEmpty() }
  var currentUser by remember {
    mutableStateOf(if (hasFirebaseApp) runCatching { Firebase.auth.currentUser }.getOrNull() else null)
  }

  DisposableEffect(hasFirebaseApp) {
    if (!hasFirebaseApp) {
      onDispose {}
    } else {
      val listener = FirebaseAuth.AuthStateListener { auth ->
        currentUser = auth.currentUser
      }
      Firebase.auth.addAuthStateListener(listener)
      onDispose {
        Firebase.auth.removeAuthStateListener(listener)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF080B12)),
  ) {
    AIOMediaAtmosphericBackground(wallpaperState = WallpaperState())

    when (isOfflineMode) {
      null -> {
        NeceCityLoadingSplash()
      }
      true -> {
        val offlineId = LocalNeceCityRepository.OFFLINE_USER_ID
        key(offlineId) {
          AuthenticatedNeceCityShell(
            currentUserId = offlineId,
            isOfflineMode = true,
            userDisplayName = null,
            userEmail = null,
            onSwitchToGoogle = {
              coroutineScope.launch { modePreferences.setOfflineMode(false) }
            },
            onSwitchToOffline = {},
          )
        }
      }
      false -> {
        val user = currentUser
        if (user == null) {
          AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onContinueOffline = {
              coroutineScope.launch { modePreferences.setOfflineMode(true) }
            },
          )
        } else {
          val uid = user.uid
          key(uid) {
            AuthenticatedNeceCityShell(
              currentUserId = uid,
              isOfflineMode = false,
              userDisplayName = user.displayName,
              userEmail = user.email,
              onSwitchToGoogle = {},
              onSwitchToOffline = {
                coroutineScope.launch { modePreferences.setOfflineMode(true) }
              },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedNeceCityShell(
  currentUserId: String,
  isOfflineMode: Boolean,
  userDisplayName: String?,
  userEmail: String?,
  onSwitchToGoogle: () -> Unit,
  onSwitchToOffline: () -> Unit,
) {
  val viewModel: NeceCityViewModel = viewModel(
    key = "${if (isOfflineMode) "offline" else "cloud"}_$currentUserId",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val dataSource = if (isOfflineMode) {
          LocalNeceCityRepository(NeceCityLocalDatabase.getInstance(app).localDao())
        } else {
          val databaseId = app.getString(R.string.firestore_database_id)
          NeceCityRepository(FirebaseFirestore.getInstance(databaseId))
        }
        NeceCityViewModel(
          repository = dataSource,
          wallpaperRepository = WallpaperRepository(app),
          alarmScheduler = AlarmScheduler(app),
          currentUserId = currentUserId,
          isOfflineMode = isOfflineMode,
        )
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isAccountDialogOpen by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      viewModel.saveCustomWallpaper(uri)
    }
  }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { /* AlarmManager schedules regardless; notification shows if granted */ }

  val ensureNotificationPermissionIfNeeded = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AIOMediaAtmosphericBackground(wallpaperState = uiState.wallpaperState)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val profile = DisplayAutoScaler.computeProfile(
        availableWidthDp = maxWidth.value,
        availableHeightDp = maxHeight.value,
        density = density.density,
        systemFontScale = density.fontScale,
      )

      Row(modifier = Modifier.fillMaxSize()) {
        if (profile.useNavigationRail) {
          NeceCityNavRail(
            activeTab = uiState.activeTab,
            onSelectTab = viewModel::selectTab,
          )
        }

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          bottomBar = {
            if (!profile.useNavigationRail) {
              NeceCityBottomBar(
                activeTab = uiState.activeTab,
                onSelectTab = viewModel::selectTab,
              )
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        ) { innerPadding ->
          val contentInsets = if (profile.useNavigationRail) {
            WindowInsets.safeDrawing.only(
              WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
            )
          } else {
            WindowInsets.safeDrawing
          }

          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .consumeWindowInsets(innerPadding)
              .windowInsetsPadding(contentInsets)
              .padding(horizontal = profile.horizontalPaddingDp),
          ) {
            NeceCityTopHeader(
              activeTab = uiState.activeTab,
              isOfflineMode = isOfflineMode,
              onOpenThemePicker = viewModel::openWallpaperMenu,
              onOpenAccountMenu = { isAccountDialogOpen = true },
            )

            uiState.statusBanner?.let { banner ->
              NeceCityStatusBanner(
                message = banner,
                onDismiss = viewModel::dismissStatusBanner,
              )
              Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
              contentAlignment = Alignment.TopCenter,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            ) {
              val containerModifier = if (profile.useTwoPaneLayout) {
                Modifier.fillMaxSize()
              } else {
                Modifier.fillMaxSize().widthIn(max = 620.dp)
              }

              Box(modifier = containerModifier) {
                NeceCityTabBody(
                  uiState = uiState,
                  profile = profile,
                  viewModel = viewModel,
                  onEnsureAlarmNotifPermission = ensureNotificationPermissionIfNeeded,
                )
              }
            }
          }
        }
      }
    }

    if (uiState.isWallpaperMenuOpen) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = viewModel::dismissWallpaperMenu,
        sheetState = sheetState,
        containerColor = Color(0xEE0B111E),
      ) {
        BackdropThemePickerSheetContent(
          wallpaperState = uiState.wallpaperState,
          onSelectPreset = viewModel::selectWallpaperPreset,
          onChooseCustomPhoto = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        )
      }
    }

    if (isAccountDialogOpen) {
      AlertDialog(
        onDismissRequest = { isAccountDialogOpen = false },
        title = {
          Text(if (isOfflineMode) "Offline Mode" else "NeceCity Account")
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isOfflineMode) {
              Text(
                text = "You are using NeceCity offline without Google services. Notes, calendar events, alarms, world clocks, and calculation history are saved locally on this device.",
                style = MaterialTheme.typography.bodyMedium,
              )
            } else {
              if (!userDisplayName.isNullOrBlank()) {
                Text(userDisplayName, style = MaterialTheme.typography.titleMedium)
              }
              if (!userEmail.isNullOrBlank()) {
                Text(
                  userEmail,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Your notes, calendar events, alarms, world clocks, and calculation history are synced securely to your account.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedButton(
                onClick = {
                  isAccountDialogOpen = false
                  performGoogleSignOut(
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                    onSignOutComplete = onSwitchToOffline,
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("switch_to_offline_button"),
              ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(id = R.string.switch_to_offline))
              }
            }
          }
        },
        confirmButton = {
          if (isOfflineMode) {
            Button(
              onClick = {
                isAccountDialogOpen = false
                onSwitchToGoogle()
              },
              modifier = Modifier.testTag("switch_to_google_button"),
            ) {
              Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(id = R.string.switch_to_google))
            }
          } else {
            Button(
              onClick = {
                isAccountDialogOpen = false
                performGoogleSignOut(credentialManager, coroutineScope)
              },
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(id = R.string.sign_out))
            }
          }
        },
        dismissButton = {
          TextButton(onClick = { isAccountDialogOpen = false }) { Text("Close") }
        },
      )
    }

    AnimatedVisibility(
      visible = uiState.isInitialLoading,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      NeceCityLoadingSplash()
    }
  }
}

@Composable
private fun NeceCityTopHeader(
  activeTab: NeceCityTab,
  isOfflineMode: Boolean,
  onOpenThemePicker: () -> Unit,
  onOpenAccountMenu: () -> Unit,
) {
  val subtitle = when (activeTab) {
    NeceCityTab.CALCULATOR -> "Calculator"
    NeceCityTab.CALENDAR -> "Calendar"
    NeceCityTab.NOTES -> "Notes"
    NeceCityTab.CLOCK -> "World Clock & Alarms"
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 8.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = stringResource(id = R.string.app_name),
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isOfflineMode) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                RoundedCornerShape(999.dp),
              )
              .testTag("offline_mode_badge"),
          ) {
            Text(
              text = stringResource(id = R.string.offline_badge),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      IconButton(
        onClick = onOpenThemePicker,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
          .testTag("theme_picker_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Palette,
          contentDescription = stringResource(id = R.string.wallpaper_menu_title),
          tint = Color.White,
        )
      }

      IconButton(
        onClick = onOpenAccountMenu,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
          .testTag("account_menu_button"),
      ) {
        Icon(
          imageVector = if (isOfflineMode) Icons.Rounded.CloudOff else Icons.Rounded.AccountCircle,
          contentDescription = if (isOfflineMode) "Offline local mode settings" else "Account and sign out",
          tint = if (isOfflineMode) MaterialTheme.colorScheme.primary else Color.White,
        )
      }
    }
  }
}

@Composable
private fun NeceCityTabBody(
  uiState: NeceCityUiState,
  profile: NeceCityDisplayProfile,
  viewModel: NeceCityViewModel,
  onEnsureAlarmNotifPermission: () -> Unit,
) {
  when (uiState.activeTab) {
    NeceCityTab.CALCULATOR -> CalculatorView(
      expression = uiState.calcExpression,
      livePreview = uiState.calcPreview,
      errorText = uiState.calcError,
      history = uiState.calcHistory,
      profile = profile,
      onAppendToken = viewModel::appendCalcToken,
      onToggleParenthesis = viewModel::toggleCalcParenthesis,
      onToggleSign = viewModel::toggleCalcSign,
      onBackspace = viewModel::backspaceCalc,
      onClearAll = viewModel::clearCalcAll,
      onEvaluate = viewModel::evaluateCalc,
      onUseHistory = viewModel::useCalcHistory,
      onDeleteHistory = viewModel::deleteCalcHistory,
      isOfflineMode = uiState.isOfflineMode,
    )

    NeceCityTab.CALENDAR -> CalendarView(
      displayedYear = uiState.displayedYear,
      displayedMonth = uiState.displayedMonth,
      selectedDateIso = uiState.selectedDateIso,
      allEvents = uiState.events,
      profile = profile,
      onPrevMonth = viewModel::prevMonth,
      onNextMonth = viewModel::nextMonth,
      onJumpToday = viewModel::jumpToday,
      onSelectDate = viewModel::selectDate,
      onSaveEvent = viewModel::saveCalendarEvent,
      onDeleteEvent = viewModel::deleteCalendarEvent,
    )

    NeceCityTab.NOTES -> NotesView(
      notes = uiState.notes,
      searchQuery = uiState.notesSearchQuery,
      selectedColorFilter = uiState.notesColorFilter,
      profile = profile,
      isOfflineMode = uiState.isOfflineMode,
      onSearchQueryChange = viewModel::updateNotesSearch,
      onSelectColorFilter = viewModel::selectNotesColorFilter,
      onSaveNote = viewModel::saveNote,
      onTogglePin = viewModel::toggleNotePin,
      onDeleteNote = viewModel::deleteNote,
    )

    NeceCityTab.CLOCK -> ClockView(
      activeSection = uiState.clockSection,
      use24Hour = uiState.use24HourClock,
      worldClocks = uiState.worldClocks,
      alarms = uiState.alarms,
      profile = profile,
      onSelectSection = viewModel::selectClockSection,
      onToggle24Hour = viewModel::toggle24HourClock,
      onAddWorldClock = viewModel::addWorldClock,
      onDeleteWorldClock = viewModel::deleteWorldClock,
      onSaveAlarm = { id, h, m, label, enabled, days, vib ->
        if (enabled) onEnsureAlarmNotifPermission()
        viewModel.saveAlarm(id, h, m, label, enabled, days, vib)
      },
      onToggleAlarm = { id, enabled ->
        if (enabled) onEnsureAlarmNotifPermission()
        viewModel.toggleAlarm(id, enabled)
      },
      onDeleteAlarm = viewModel::deleteAlarm,
    )
  }
}

@Composable
private fun NeceCityStatusBanner(
  message: String,
  onDismiss: () -> Unit,
) {
  Surface(
    color = Color(0xB8182238),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
      }
    }
  }
}

private data class NavItemSpec(
  val tab: NeceCityTab,
  val labelRes: Int,
  val icon: ImageVector,
  val testTag: String,
)

private val navItems = listOf(
  NavItemSpec(NeceCityTab.CALCULATOR, R.string.nav_calculator, Icons.Rounded.Calculate, "nav_calculator"),
  NavItemSpec(NeceCityTab.CALENDAR, R.string.nav_calendar, Icons.Rounded.CalendarMonth, "nav_calendar"),
  NavItemSpec(NeceCityTab.NOTES, R.string.nav_notes, Icons.Rounded.EditNote, "nav_notes"),
  NavItemSpec(NeceCityTab.CLOCK, R.string.nav_clock, Icons.Rounded.Schedule, "nav_clock"),
)

@Composable
private fun NeceCityBottomBar(
  activeTab: NeceCityTab,
  onSelectTab: (NeceCityTab) -> Unit,
) {
  NavigationBar(
    containerColor = Color(0xCC0A101D),
    contentColor = Color.White,
    windowInsets = WindowInsets.safeDrawing.only(
      WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    ),
  ) {
    navItems.forEach { item ->
      val selected = activeTab == item.tab
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectTab(item.tab) },
        icon = { Icon(item.icon, contentDescription = stringResource(id = item.labelRes)) },
        label = { Text(stringResource(id = item.labelRes), maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = Color.White.copy(alpha = 0.62f),
          unselectedTextColor = Color.White.copy(alpha = 0.62f),
          indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ),
        modifier = Modifier.testTag(item.testTag),
      )
    }
  }
}

@Composable
private fun NeceCityNavRail(
  activeTab: NeceCityTab,
  onSelectTab: (NeceCityTab) -> Unit,
) {
  NavigationRail(
    containerColor = Color(0xB8090E1A),
    contentColor = Color.White,
    windowInsets = WindowInsets.safeDrawing.only(
      WindowInsetsSides.Vertical + WindowInsetsSides.Start
    ),
    modifier = Modifier.fillMaxHeight(),
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    navItems.forEach { item ->
      val selected = activeTab == item.tab
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(item.tab) },
        icon = { Icon(item.icon, contentDescription = stringResource(id = item.labelRes)) },
        label = { Text(stringResource(id = item.labelRes), maxLines = 1) },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = Color.White.copy(alpha = 0.62f),
          unselectedTextColor = Color.White.copy(alpha = 0.62f),
          indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
        modifier = Modifier.testTag(item.testTag),
      )
    }
  }
}
