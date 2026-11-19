package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CasteCategory
import com.example.model.ChecklistAnswers
import com.example.model.DocumentComplianceChecks
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.HouseType
import com.example.model.MaterialAndOfficeDetails
import com.example.model.MeterInfo
import com.example.model.SiteSurveyDetails
import com.example.model.SurveyCategory
import com.example.ui.components.BrandTopBar
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.SurfaceCanvas
import com.example.viewmodel.AddEditWizardState

@Composable
fun AddSurveyWizardScreen(
  wizard: AddEditWizardState,
  villageNameMr: String,
  talukaNameMr: String,
  districtNameMr: String,
  snackbarHostState: SnackbarHostState,
  onPreviousStep: () -> Unit,
  onNextStep: () -> Unit,
  onJumpToStep: (Int) -> Unit,
  onSaveSurvey: () -> Unit,
  onUpdateBasic: (
    surveyDate: String?,
    beneficiaryName: String?,
    mobile: String?,
    emailId: String?,
    addressLandmark: String?,
    gharkulId: String?,
    aadhaarLast4: String?,
    casteCategory: CasteCategory?,
    houseType: HouseType?,
    gpsLocation: String?,
  ) -> Unit,
  onCaptureGps: () -> Unit,
  onUpdateMeter: ((MeterInfo) -> MeterInfo) -> Unit,
  onUpdateSiteSurvey: ((SiteSurveyDetails) -> SiteSurveyDetails) -> Unit,
  onUpdateDocument: (
    type: DocumentType,
    printedName: String?,
    photoUri: String?,
    photoSizeKb: Int?,
    isSizeValid: Boolean?,
    legibility: DocumentLegibility?,
    clearPhoto: Boolean,
    queryNote: String?,
  ) -> Unit,
  onUpdateCompliance: ((DocumentComplianceChecks) -> DocumentComplianceChecks) -> Unit,
  onUpdateChecklist: ((ChecklistAnswers) -> ChecklistAnswers) -> Unit,
  onUpdateMaterialAndOffice: ((MaterialAndOfficeDetails) -> MaterialAndOfficeDetails) -> Unit,
  onOverrideCategory: (SurveyCategory?) -> Unit,
  onOverrideReasonChange: (String) -> Unit,
  onRemarkChange: (String) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  BackHandler { onPreviousStep() }

  val (headerTitle, headerSubtitle) =
    when (wizard.currentStep) {
      1 -> (if (wizard.editingSurveyId == null) "नवीन ग्राहक" else "ग्राहक संपादन") to "पायरी 1 / 3 · प्राथमिक व साईट माहिती"
      2 -> "कागदपत्रे" to "पायरी 2 / 3 · कागदपत्रे, टीप व Note Cam"
      else -> "तपासणी आणि निकाल" to "पायरी 3 / 3 · ${wizard.beneficiaryName.ifBlank { "नवीन ग्राहक" }}"
    }

  Scaffold(
    containerColor = SurfaceCanvas,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      BrandTopBar(
        title = headerTitle,
        subtitle = headerSubtitle,
        showBackButton = true,
        onBackClick = onPreviousStep,
        stepProgress = wizard.currentStep,
      )
    },
    bottomBar = {
      Surface(color = CardWhite, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
          HorizontalDivider(color = BorderSubtle)
          if (!wizard.validationError.isNullOrBlank() && wizard.currentStep != 3) {
            Text(
              text = wizard.validationError,
              color = BrandCrimson,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
          }
          Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (wizard.currentStep < 3) {
              Button(
                onClick = onNextStep,
                modifier =
                  Modifier.widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("wizard_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
              ) {
                Text(
                  text = if (wizard.currentStep == 1) "पुढे: कागदपत्रे" else "पुढे: तपासणी",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            } else {
              Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                OutlinedButton(
                  onClick = { onJumpToStep(1) },
                  modifier = Modifier.weight(1f).height(52.dp).testTag("wizard_edit_button"),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.5.dp, BrandNavy),
                ) {
                  Text(
                    text = "संपादित करा",
                    color = BrandNavy,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Button(
                  onClick = onSaveSurvey,
                  modifier = Modifier.weight(1.4f).height(52.dp).testTag("wizard_save_button"),
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
                ) {
                  Text(
                    text = "जतन करा",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().background(SurfaceCanvas).padding(innerPadding).imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
        when (wizard.currentStep) {
          1 ->
            WizardStep1BasicAndMeter(
              wizard = wizard,
              villageNameMr = villageNameMr,
              talukaNameMr = talukaNameMr,
              onUpdateBasic = onUpdateBasic,
              onCaptureGps = onCaptureGps,
              onUpdateMeter = onUpdateMeter,
              onUpdateSiteSurvey = onUpdateSiteSurvey,
            )
          2 ->
            WizardStep2Documents(
              wizard = wizard,
              locationBreadcrumb = listOf(villageNameMr, talukaNameMr, districtNameMr).filter { it.isNotBlank() }.joinToString(" · "),
              onUpdateDocument = onUpdateDocument,
              onUpdateCompliance = onUpdateCompliance,
              onUpdateSiteSurvey = onUpdateSiteSurvey,
              onShowMessage = onShowMessage,
            )
          else ->
            WizardStep3ReviewAndResult(
              wizard = wizard,
              villageNameMr = villageNameMr,
              onUpdateChecklist = onUpdateChecklist,
              onUpdateMaterialAndOffice = onUpdateMaterialAndOffice,
              onOverrideCategory = onOverrideCategory,
              onOverrideReasonChange = onOverrideReasonChange,
              onRemarkChange = onRemarkChange,
            )
        }
      }
    }
  }
}
