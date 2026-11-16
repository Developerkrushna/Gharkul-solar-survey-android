package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.MainTab
import com.example.ui.components.BrandTopBar
import com.example.ui.components.DeleteCustomerConfirmationDialog
import com.example.ui.components.GharkulBottomBar
import com.example.ui.screens.AddSurveyWizardScreen
import com.example.ui.screens.CustomerDetailSheet
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VillageSelectionScreen
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.GharkulSurveyTheme
import com.example.ui.theme.SurfaceCanvas
import com.example.viewmodel.SurveyAppViewModel

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

  if (uiState.wizard.isOpen) {
    AddSurveyWizardScreen(
      wizard = uiState.wizard,
      villageNameMr = uiState.selectedVillage?.nameMr.orEmpty(),
      talukaNameMr = uiState.selectedTaluka?.nameMr.orEmpty(),
      snackbarHostState = snackbarHostState,
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
          email = uiState.loginEmail,
          password = uiState.loginPassword,
          errorMessage = uiState.loginError,
          onEmailChange = viewModel::updateLoginEmail,
          onPasswordChange = viewModel::updateLoginPassword,
          onLoginClick = viewModel::submitLogin,
        )
      }

      AppScreen.VILLAGE_SELECT -> {
        VillageSelectionScreen(
          executiveFirstName = uiState.executive.firstNameMr,
          districts = uiState.districts,
          selectedDistrict = uiState.selectedDistrict,
          selectedTaluka = uiState.selectedTaluka,
          selectedVillage = uiState.selectedVillage,
          villageStats = uiState.villageStats,
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
                  onSearchChange = viewModel::updateHomeSearch,
                  onCustomerClick = viewModel::openCustomerDetail,
                )

              MainTab.CUSTOMERS ->
                CustomerListScreen(
                  searchQuery = uiState.listSearchQuery,
                  selectedCategory = uiState.selectedCategoryFilter,
                  selectedStatus = uiState.selectedStatusFilter,
                  surveys = uiState.filteredListSurveys,
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
                  reportBRows = uiState.reportBRows,
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
    }
  }
}
