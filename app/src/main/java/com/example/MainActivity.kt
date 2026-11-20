package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.InstallationReport
import com.example.model.MainTab
import com.example.ui.components.BrandTopBar
import com.example.ui.components.CircularSyncTopBarButton
import com.example.ui.components.DeleteCustomerConfirmationDialog
import com.example.ui.components.GharkulBottomBar
import com.example.ui.screens.AddSurveyWizardScreen
import com.example.ui.screens.CustomerDetailSheet
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InstallationDashboardScreen
import com.example.ui.screens.InstallationFormScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VillageSelectionScreen
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.GharkulSurveyTheme
import com.example.ui.theme.SurfaceCanvas
import com.example.util.InstallationPdfGenerator
import com.example.viewmodel.SurveyAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      GharkulSurveyTheme {
        GharkulSurveyApp()
      }
    }
  }
}

@Composable
fun GharkulSurveyApp(viewModel: SurveyAppViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  var justSavedReport by remember { mutableStateOf<InstallationReport?>(null) }

  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.onAppForegroundResume()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val villageBreadcrumb =
    listOfNotNull(
      uiState.selectedVillage?.nameMr,
      uiState.selectedTaluka?.nameMr,
      uiState.selectedDistrict?.nameMr,
    ).joinToString(" · ")

  fun shareInstallationPdfAsync(target: InstallationReport, includeImages: Boolean) {
    scope.launch {
      viewModel.showMessage("Preparing Installation Certificate PDF...")
      val uri =
        withContext(Dispatchers.IO) {
          InstallationPdfGenerator.generateCertificatePdf(context, target, includeImages)
        }
      if (uri != null) {
        val shareIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
              Intent.EXTRA_SUBJECT,
              "Installation Certificate - ${target.consumerName} (${target.consumerNo})",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
        context.startActivity(
          Intent.createChooser(shareIntent, "Share Installation Certificate PDF")
        )
      } else {
        viewModel.showMessage("Could not generate PDF. Please try again.")
      }
    }
  }

  if (uiState.isRestoringSession) {
    Surface(
      modifier = Modifier.fillMaxSize().testTag("session_restore_splash"),
      color = SurfaceCanvas,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = BrandNavy,
          modifier = Modifier.size(72.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "घरकुल",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "घरकुल सोलर सर्व्हे",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = BrandNavy,
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
          color = BrandNavy,
          strokeWidth = 3.dp,
          modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "सत्र सुरू करत आहे...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  } else if (uiState.wizard.isOpen) {
    AddSurveyWizardScreen(
      wizard = uiState.wizard,
      villageNameMr = uiState.selectedVillage?.nameMr.orEmpty(),
      talukaNameMr = uiState.selectedTaluka?.nameMr.orEmpty(),
      districtNameMr = uiState.selectedDistrict?.nameMr.orEmpty(),
      snackbarHostState = snackbarHostState,
      isSaving = uiState.isSavingSurvey,
      onPreviousStep = viewModel::previousWizardStep,
      onNextStep = viewModel::nextWizardStep,
      onJumpToStep = viewModel::setWizardStep,
      onSaveSurvey = viewModel::saveWizardSurvey,
      onUpdateBasic = viewModel::updateWizardBasic,
      onCaptureGps = viewModel::captureSampleGpsForSelectedDistrict,
      onUpdateMeter = viewModel::updateWizardMeter,
      onUpdateSiteSurvey = viewModel::updateSiteSurvey,
      onUpdateDocument = viewModel::updateDocumentEntry,
      onUpdateCompliance = viewModel::updateDocumentCompliance,
      onUpdateChecklist = viewModel::updateChecklistAnswer,
      onUpdateMaterialAndOffice = viewModel::updateMaterialAndOffice,
      onOverrideCategory = viewModel::setOverrideCategory,
      onOverrideReasonChange = viewModel::updateOverrideReason,
      onRemarkChange = viewModel::updateExecutiveRemark,
      onShowMessage = viewModel::showMessage,
    )
  } else {
    when (uiState.currentScreen) {
      AppScreen.LOGIN -> {
        LoginScreen(
          selectedRole = uiState.loginRole,
          email = uiState.loginEmail,
          password = uiState.loginPassword,
          errorMessage = uiState.loginError,
          isAuthenticating = uiState.isAuthenticating,
          isCloudConfigured = uiState.isCloudConfigured,
          onSelectRole = viewModel::selectLoginRole,
          onEmailChange = viewModel::updateLoginEmail,
          onPasswordChange = viewModel::updateLoginPassword,
          onLoginClick = viewModel::submitLogin,
        )
      }

      AppScreen.VILLAGE_SELECT -> {
        val exec = uiState.executive
        val assignedAreaBanner =
          if (
            exec.assignedDistrict.isNotBlank() ||
              exec.assignedTaluka.isNotBlank() ||
              exec.assignedVillage.isNotBlank()
          ) {
            val parts =
              listOf(exec.assignedDistrict, exec.assignedTaluka, exec.assignedVillage)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (exec.assignedVillage.isBlank() || exec.assignedVillage.trim() == "सर्व गाव") {
              "नेमलेला परिसर: $parts (तुम्ही कोणतेही गाव निवडू शकता)"
            } else {
              "नेमलेला परिसर: $parts"
            }
          } else null
        VillageSelectionScreen(
          executiveFirstName = uiState.executive.firstNameMr,
          districts = uiState.districts,
          selectedDistrict = uiState.selectedDistrict,
          selectedTaluka = uiState.selectedTaluka,
          selectedVillage = uiState.selectedVillage,
          villageStats = uiState.villageStats,
          assignedAreaBanner = assignedAreaBanner,
          onSelectDistrict = viewModel::selectDistrict,
          onSelectTaluka = viewModel::selectTaluka,
          onSelectVillage = viewModel::selectVillage,
          onAddCustomVillage = viewModel::addAndSelectCustomVillage,
          onProceedClick = viewModel::proceedToDashboard,
          onBackToDashboard = viewModel::navigateBackFromVillageSelector,
        )
      }

      AppScreen.MAIN_APP -> {
        if (uiState.activeTab != MainTab.HOME) {
          BackHandler { viewModel.selectTab(MainTab.HOME) }
        }

        Scaffold(
          containerColor = SurfaceCanvas,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            val topTitle =
              when (uiState.activeTab) {
                MainTab.HOME -> "नमस्कार, ${uiState.executive.firstNameMr}"
                MainTab.CUSTOMERS -> "ग्राहक यादी"
                MainTab.PROFILE -> "प्रोफाइल आणि सारांश"
              }
            BrandTopBar(
              title = topTitle,
              subtitle = villageBreadcrumb,
              trailingAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularSyncTopBarButton(
                    isSyncing = uiState.isSyncing,
                    pendingSyncCount = uiState.pendingSyncCount,
                    onClick = viewModel::refreshCloudData,
                    testTag = "survey_sync_button",
                    contentDescription = "डेटा सिंक व रिफ्रेश करा",
                  )
                  Surface(
                    modifier = Modifier.testTag("header_change_village_chip"),
                    shape = RoundedCornerShape(10.dp),
                    color = BannerIceBlue,
                    border = BorderStroke(1.dp, BorderSubtle),
                    onClick = viewModel::openVillageSelectorFromDashboard,
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "गाव बदला",
                        tint = BrandNavy,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "गाव",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandNavy,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              },
            )
          },
          bottomBar = {
            GharkulBottomBar(
              activeTab = uiState.activeTab,
              onSelectTab = viewModel::selectTab,
              onAddNewClick = viewModel::openNewSurveyWizard,
            )
          },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState.activeTab) {
              MainTab.HOME ->
                HomeDashboardScreen(
                  stats = uiState.villageStats,
                  searchQuery = uiState.homeSearchQuery,
                  recentSurveys = uiState.activeVillageSurveys,
                  isCloudConfigured = uiState.isCloudConfigured,
                  isSyncing = uiState.isSyncing,
                  syncErrorMessage = uiState.syncErrorMessage,
                  onRefresh = viewModel::refreshCloudData,
                  onSearchChange = viewModel::updateHomeSearch,
                  onCustomerClick = viewModel::openCustomerDetail,
                )

              MainTab.CUSTOMERS ->
                CustomerListScreen(
                  searchQuery = uiState.listSearchQuery,
                  selectedCategory = uiState.selectedCategoryFilter,
                  selectedStatus = uiState.selectedStatusFilter,
                  surveys = uiState.filteredListSurveys,
                  isCloudConfigured = uiState.isCloudConfigured,
                  isSyncing = uiState.isSyncing,
                  syncErrorMessage = uiState.syncErrorMessage,
                  onRefresh = viewModel::refreshCloudData,
                  onSearchChange = viewModel::updateListSearch,
                  onCategorySelect = viewModel::selectCategoryFilter,
                  onStatusSelect = viewModel::selectStatusFilter,
                  onCustomerClick = viewModel::openCustomerDetail,
                  onDeleteClick = viewModel::requestDeleteCustomer,
                )

              MainTab.PROFILE ->
                ProfileScreen(
                  executive = uiState.executive,
                  villageBreadcrumb = villageBreadcrumb,
                  onChangeVillageClick = viewModel::openVillageSelectorFromDashboard,
                  onLogoutClick = viewModel::logout,
                )
            }
          }
        }

        val detailSurvey = uiState.selectedDetailSurvey
        if (detailSurvey != null) {
          CustomerDetailSheet(
            customer = detailSurvey,
            onDismiss = viewModel::closeCustomerDetail,
            onEditClick = viewModel::openEditSurveyWizard,
            onDeleteClick = viewModel::requestDeleteCustomer,
          )
        }

        val pendingDelete = uiState.pendingDeleteSurvey
        if (pendingDelete != null) {
          DeleteCustomerConfirmationDialog(
            beneficiaryName = pendingDelete.beneficiaryName,
            onConfirm = viewModel::confirmDeleteCustomer,
            onDismiss = viewModel::dismissDeleteDialog,
          )
        }
      }

      AppScreen.INSTALLATION_DASHBOARD -> {
        InstallationDashboardScreen(
          activeTab = uiState.activeInstallationTab,
          executive = uiState.installationExecutive,
          approvedCat1Surveys = uiState.approvedCategory1Surveys,
          installationReports = uiState.installationReports,
          searchQuery = uiState.installationSearchQuery,
          selectedFilter = uiState.installationFilter,
          snackbarHostState = snackbarHostState,
          isCloudConfigured = uiState.isCloudConfigured,
          isSyncing = uiState.isSyncing,
          pendingSyncCount = uiState.pendingSyncCount,
          syncErrorMessage = uiState.syncErrorMessage,
          onSyncClick = viewModel::refreshCloudData,
          onSelectTab = viewModel::selectInstallationTab,
          onSearchChange = viewModel::updateInstallationSearch,
          onFilterSelect = viewModel::selectInstallationFilter,
          onStartFromSurvey = viewModel::startInstallationFromSurvey,
          onStartManual = viewModel::startManualInstallationForm,
          onEditReport = viewModel::openEditInstallationReport,
          onShowMessage = viewModel::showMessage,
          onLogoutClick = viewModel::logout,
        )

        val savedDialogReport = justSavedReport
        if (savedDialogReport != null) {
          com.example.ui.screens.ShareInstallationPdfOptionDialog(
            report = savedDialogReport,
            isPostSave = true,
            onDismiss = { justSavedReport = null },
            onShareWithImages = {
              val target = savedDialogReport
              justSavedReport = null
              shareInstallationPdfAsync(target, includeImages = true)
            },
            onShareWithoutImages = {
              val target = savedDialogReport
              justSavedReport = null
              shareInstallationPdfAsync(target, includeImages = false)
            },
          )
        }
      }

      AppScreen.INSTALLATION_FORM -> {
        InstallationFormScreen(
          form = uiState.installationForm,
          snackbarHostState = snackbarHostState,
          isSaving = uiState.isSavingInstallation,
          onSetPage = viewModel::setInstallationFormPage,
          onNextPage = viewModel::nextInstallationFormPage,
          onPreviousPage = viewModel::previousInstallationFormPage,
          onUpdateHeader = viewModel::updateInstallationHeader,
          onUpdateProductRow = viewModel::updateInstallationProductRow,
          onUpdateSignOff = viewModel::updateInstallationSignOff,
          onUpdateInverterSerial = viewModel::updateInverterSerial,
          onUpdateMeterSerial = viewModel::updateGenerationMeterSerial,
          onUpdatePanelSerial = viewModel::updatePanelSerial,
          onAddPanel = viewModel::addPanelSerialRow,
          onRemovePanel = viewModel::removePanelSerialRow,
          onAddPhotoSlot = viewModel::addInstallationPhotoSlot,
          onRemovePhotoSlot = viewModel::removeInstallationPhotoSlot,
          onUpdatePhotoCaption = viewModel::updateInstallationPhotoCaption,
          onUpdatePhotoCapture = viewModel::updateInstallationPhotoCapture,
          onShowMessage = viewModel::showMessage,
          onSubmitCertificate = {
            viewModel.saveInstallationReport(onSaved = { justSavedReport = it })
          },
        )
      }
    }
  }
}
