package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ChecklistAnswers
import com.example.model.MaterialAndOfficeDetails
import com.example.model.SurveyCategory
import com.example.ui.components.SegmentedChoiceButton
import com.example.ui.components.SignaturePadCard
import com.example.ui.components.YesNoPillGroup
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.DividerLight
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SurveyPdfGenerator
import com.example.viewmodel.AddEditWizardState

@Composable
fun WizardStep3ReviewAndResult(
  wizard: AddEditWizardState,
  villageNameMr: String,
  onUpdateChecklist: ((ChecklistAnswers) -> ChecklistAnswers) -> Unit,
  onUpdateMaterialAndOffice: ((MaterialAndOfficeDetails) -> MaterialAndOfficeDetails) -> Unit,
  onOverrideCategory: (SurveyCategory?) -> Unit,
  onOverrideReasonChange: (String) -> Unit,
  onRemarkChange: (String) -> Unit,
) {
  val effectiveCategory = wizard.effectiveCategory
  val office = wizard.materialAndOfficeDetails

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // All-Category Yes/No Questions Card + priority rule (4 > 3 > 2 > 1)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "सर्व श्रेणी पडताळणी प्रश्न (होय / नाही)",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "एकापेक्षा जास्त श्रेणी लागू झाल्यास सर्वोच्च श्रेणी (4 > 3 > 2 > 1) ग्राह्य धरली जाईल.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))

        VerificationQuestionRow(
          question = "१. घरकुल व मीटर एकाच व्यक्तीच्या नावावर आहे का?",
          value = wizard.checklist.gharkulAndMeterSamePerson,
          onValueChange = { ans -> onUpdateChecklist { it.copy(gharkulAndMeterSamePerson = ans) } },
          tagPrefix = "chk_same_person",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "१. बँक पासबुक, आधार कार्ड, पॅन कार्ड, लाईट बिल या सर्वांवर नाव समान आहे का?",
          value = wizard.checklist.allDocsHaveSameName,
          onValueChange = { ans -> onUpdateChecklist { it.copy(allDocsHaveSameName = ans) } },
          tagPrefix = "chk_all_same",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "२. आधार कार्डवरील नावामध्ये बदल आवश्यक आहे का?",
          value = wizard.checklist.aadhaarNameChangeNeeded,
          onValueChange = { ans -> onUpdateChecklist { it.copy(aadhaarNameChangeNeeded = ans) } },
          tagPrefix = "chk_aadhaar_chg",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "२. लाईट बिलावरील नावामध्ये बदल आवश्यक आहे का?",
          value = wizard.checklist.lightBillNameChangeNeeded,
          onValueChange = { ans -> onUpdateChecklist { it.copy(lightBillNameChangeNeeded = ans) } },
          tagPrefix = "chk_bill_chg",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "२. पॅन कार्डवरील नावामध्ये बदल आवश्यक आहे का?",
          value = wizard.checklist.panNameChangeNeeded,
          onValueChange = { ans -> onUpdateChecklist { it.copy(panNameChangeNeeded = ans) } },
          tagPrefix = "chk_pan_chg",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "२. बँक पासबुकवरील नावामध्ये बदल आवश्यक आहे का?",
          value = wizard.checklist.passbookNameChangeNeeded,
          onValueChange = { ans -> onUpdateChecklist { it.copy(passbookNameChangeNeeded = ans) } },
          tagPrefix = "chk_passbook_chg",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "३. घरकुल नावावर आहे पण लाईट मीटर कनेक्शन घरातील दुसऱ्या व्यक्तीच्या नावे आहे का?",
          value = wizard.checklist.meterOnFamilyMemberName,
          onValueChange = { ans -> onUpdateChecklist { it.copy(meterOnFamilyMemberName = ans) } },
          tagPrefix = "chk_cat3_meter",
        )
        HorizontalDivider(color = DividerLight)
        VerificationQuestionRow(
          question = "४. घरकुल नावावर आहे पण लाईट कनेक्शन नाही का?",
          value = wizard.checklist.noElectricityConnection,
          onValueChange = { ans -> onUpdateChecklist { it.copy(noElectricityConnection = ans) } },
          tagPrefix = "chk_cat4_noconn",
        )
      }
    }

    // Priority Category Auto-Result Card
    val resultBg =
      if (effectiveCategory == SurveyCategory.CATEGORY_1 && wizard.pendingActions.isEmpty()) SuccessGreenBg
      else BannerIceBlue
    val resultTitleColor =
      if (effectiveCategory == SurveyCategory.CATEGORY_1 && wizard.pendingActions.isEmpty()) SuccessGreenText
      else BrandNavy

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("auto_result_card"),
      shape = RoundedCornerShape(16.dp),
      color = resultBg,
      border = BorderStroke(1.dp, resultTitleColor.copy(alpha = 0.25f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = effectiveCategory.titleMr,
          style = MaterialTheme.typography.titleLarge,
          color = resultTitleColor,
          fontWeight = FontWeight.Bold,
        )
        if (wizard.matchedCategories.size > 1) {
          Text(
            text =
              "लागू झालेल्या श्रेणी: ${wizard.matchedCategories.joinToString(", ") { "श्रेणी ${it.number}" }} → सर्वोच्च प्राधान्य: श्रेणी ${wizard.suggestedCategory.number}",
            style = MaterialTheme.typography.labelMedium,
            color = BrandNavy,
            fontWeight = FontWeight.SemiBold,
          )
        }
        if (wizard.pendingActions.isEmpty()) {
          Text(
            text = "सर्व कागदपत्रे व नावे समान आहेत — अर्ज भरण्यासाठी तयार!",
            style = MaterialTheme.typography.bodyMedium,
            color = SuccessGreenText,
            fontWeight = FontWeight.Medium,
          )
        } else {
          wizard.pendingActions.forEachIndexed { idx, action ->
            val prefix = if (idx == 0) "बाकी: " else "      "
            Text(
              text = "$prefix${action.titleMr}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }

    // ३. साईट सर्व्हे निरीक्षण व साहित्य तपशील (Office Use Only)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "३. साईट सर्व्हे निरीक्षण व साहित्य तपशील",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        MandatoryLabel("कागदपत्रांची स्थिती", isMandatory = true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          SegmentedChoiceButton(
            label = "पूर्ण",
            selected = office.documentsCompleteStatus,
            onClick = { onUpdateMaterialAndOffice { it.copy(documentsCompleteStatus = true) } },
            selectedColor = BrandNavy,
            modifier = Modifier.weight(1f),
            testTag = "docs_status_complete",
          )
          SegmentedChoiceButton(
            label = "अपूर्ण",
            selected = !office.documentsCompleteStatus,
            onClick = { onUpdateMaterialAndOffice { it.copy(documentsCompleteStatus = false) } },
            selectedColor = BrandCrimson,
            modifier = Modifier.weight(1f),
            testTag = "docs_status_incomplete",
          )
        }

        HorizontalDivider(color = DividerLight)
        MandatoryLabel("साहित्य तपशील (Material Details)", isMandatory = true)

        FixedMaterialWithCustomInputRow(
          title = "१६ Sq.mm. Aluminium (Earth Wire)",
          subtitle = "ठरवलेली मर्यादा: 20 मीटर (बदलण्यासाठी + दाबा)",
          currentDisplayValue = office.earthWireMeters.ifBlank { "20 मीटर" },
          defaultValue = "20 मीटर",
          unitLabel = "मीटर",
          inputPlaceholder = "एकूण मीटर संख्या लिहा (उदा. 25)",
          testTagPrefix = "material_earth_wire",
          onSaveCustomValue = { newDigits ->
            onUpdateMaterialAndOffice {
              it.copy(earthWireMeters = "$newDigits मीटर")
            }
          },
          onResetDefault = {
            onUpdateMaterialAndOffice { it.copy(earthWireMeters = "20 मीटर") }
          },
        )

        HorizontalDivider(color = DividerLight)

        FixedMaterialWithCustomInputRow(
          title = "AC 1.5 sq.mm 3 Core केबल",
          subtitle = "ठरवलेली मर्यादा: 10 मीटर (बदलण्यासाठी + दाबा)",
          currentDisplayValue = office.acCableMeters.ifBlank { "10 मीटर" },
          defaultValue = "10 मीटर",
          unitLabel = "मीटर",
          inputPlaceholder = "एकूण मीटर संख्या लिहा (उदा. 15)",
          testTagPrefix = "material_ac_cable",
          onSaveCustomValue = { newDigits ->
            onUpdateMaterialAndOffice {
              it.copy(acCableMeters = "$newDigits मीटर")
            }
          },
          onResetDefault = {
            onUpdateMaterialAndOffice { it.copy(acCableMeters = "10 मीटर") }
          },
        )

        HorizontalDivider(color = DividerLight)

        FixedMaterialWithCustomInputRow(
          title = "पाईप (१ पाईप = ३० मीटर)",
          subtitle = "ठरवलेली मर्यादा: 5 पाईप = 150 मीटर (बदलण्यासाठी + दाबा)",
          currentDisplayValue = SurveyPdfGenerator.formatPipeWithMeters(office.pipeMeters),
          defaultValue = "5 पाईप",
          rawStoredValue = office.pipeMeters.ifBlank { "5 पाईप" },
          unitLabel = "पाईप",
          inputPlaceholder = "एकूण पाईप संख्या लिहा (उदा. 7)",
          testTagPrefix = "material_pipe",
          onSaveCustomValue = { newDigits ->
            onUpdateMaterialAndOffice {
              it.copy(pipeMeters = "$newDigits पाईप")
            }
          },
          onResetDefault = {
            onUpdateMaterialAndOffice { it.copy(pipeMeters = "5 पाईप") }
          },
        )

        Text(
          text = "टीप- वरीलप्रमाणे ठरून दिलेल्या साहित्यामध्ये अतिरिक्त साहित्य लागल्यास त्याचा खर्च ग्राहकांना द्यावा लागेल.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
    }

    // Signatures Card (Customer + Survey Executive Name & Signature)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "सह्या (Signatures)",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        SignaturePadCard(
          title = "ग्राहक / अर्जदाराची सही",
          strokes = office.customerSignatureStrokes,
          onStrokesChange = { s ->
            onUpdateMaterialAndOffice { it.copy(customerSignatureStrokes = s) }
          },
          testTag = "customer_signature_pad",
        )

        HorizontalDivider(color = DividerLight)

        LabeledTextField(
          label = "सर्व्हे करणाऱ्या व्यक्तीचे नाव",
          isMandatory = true,
          value = office.surveyorName,
          placeholder = "सर्व्हे एक्झिक्युटिव्ह नाव",
          onValueChange = { name -> onUpdateMaterialAndOffice { it.copy(surveyorName = name) } },
          testTag = "surveyor_name_input",
        )

        SignaturePadCard(
          title = "सर्व्हे करणाऱ्या व्यक्तीची सही",
          strokes = office.surveyorSignatureStrokes,
          onStrokesChange = { s ->
            onUpdateMaterialAndOffice { it.copy(surveyorSignatureStrokes = s) }
          },
          testTag = "surveyor_signature_pad",
        )
      }
    }

    if (!wizard.validationError.isNullOrBlank()) {
      Text(
        text = wizard.validationError,
        color = BrandCrimson,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
  }
}

@Composable
private fun FixedMaterialWithCustomInputRow(
  title: String,
  subtitle: String,
  currentDisplayValue: String,
  defaultValue: String,
  unitLabel: String,
  inputPlaceholder: String,
  testTagPrefix: String,
  onSaveCustomValue: (String) -> Unit,
  onResetDefault: () -> Unit,
  rawStoredValue: String = currentDisplayValue,
) {
  var isEditingCustom by remember { mutableStateOf(false) }
  val storedDigits = rawStoredValue.filter { it.isDigit() }
  val defaultDigits = defaultValue.filter { it.isDigit() }
  val isCustomized = storedDigits.isNotBlank() && storedDigits != defaultDigits
  var customDigitsInput by remember(rawStoredValue, isEditingCustom) {
    mutableStateOf(storedDigits.ifBlank { defaultDigits })
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Question on top
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
      )
    }

    // Answer directly BELOW the question + '+' custom trigger
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isCustomized) BannerIceBlue else SurfaceCanvas,
        border = BorderStroke(1.dp, if (isCustomized) BrandNavy else BorderSubtle),
      ) {
        Text(
          text = currentDisplayValue,
          style = MaterialTheme.typography.labelLarge,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).testTag("${testTagPrefix}_value"),
        )
      }

      OutlinedButton(
        onClick = { isEditingCustom = !isEditingCustom },
        modifier = Modifier.height(48.dp).testTag("${testTagPrefix}_plus_btn"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BrandNavy),
        contentPadding = PaddingValues(horizontal = 12.dp),
      ) {
        Icon(
          imageVector = if (isEditingCustom) Icons.Outlined.Close else Icons.Filled.Add,
          contentDescription = if (isEditingCustom) "बंद करा" else "कस्टम मोजमाप जोडा",
          tint = BrandNavy,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isEditingCustom) "बंद" else "कस्टम",
          color = BrandNavy,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      if (isCustomized) {
        TextButton(
          onClick = {
            customDigitsInput = defaultDigits
            onResetDefault()
            isEditingCustom = false
          },
          modifier = Modifier.height(48.dp).testTag("${testTagPrefix}_reset_btn"),
        ) {
          Icon(
            imageVector = Icons.Outlined.RestartAlt,
            contentDescription = "डीफॉल्ट",
            tint = BrandCrimson,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("रीसेट", color = BrandCrimson, fontWeight = FontWeight.Bold)
        }
      }
    }

    AnimatedVisibility(visible = isEditingCustom) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = customDigitsInput,
          onValueChange = { typed ->
            customDigitsInput = typed.filter { it.isDigit() }.take(4)
          },
          modifier = Modifier.weight(1f).testTag("${testTagPrefix}_input"),
          label = { Text("एकूण $unitLabel संख्या") },
          placeholder = { Text(inputPlaceholder, color = TextMuted) },
          suffix = { Text(unitLabel, color = TextSecondary, fontWeight = FontWeight.SemiBold) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )

        androidx.compose.material3.Button(
          onClick = {
            val numeric = customDigitsInput.toIntOrNull()
            if (numeric != null && numeric > 0) {
              onSaveCustomValue(numeric.toString())
              isEditingCustom = false
            }
          },
          enabled = (customDigitsInput.toIntOrNull() ?: 0) > 0,
          modifier = Modifier.height(52.dp).testTag("${testTagPrefix}_add_btn"),
          shape = RoundedCornerShape(12.dp),
          colors =
            androidx.compose.material3.ButtonDefaults.buttonColors(
              containerColor = BrandNavy,
              contentColor = androidx.compose.ui.graphics.Color.White,
            ),
        ) {
          Text("जोडा", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun VerificationQuestionRow(
  question: String,
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  tagPrefix: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = question,
      style = MaterialTheme.typography.bodyMedium,
      color = TextPrimary,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.weight(1f),
    )
    YesNoPillGroup(
      value = value,
      onValueChange = onValueChange,
      testTagPrefix = tagPrefix,
    )
  }
}
