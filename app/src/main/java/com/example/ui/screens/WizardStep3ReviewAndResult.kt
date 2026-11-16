package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SurveyRuleEngine
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AddEditWizardState

@OptIn(ExperimentalLayoutApi::class)
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
  val context = LocalContext.current
  val effectiveCategory = wizard.effectiveCategory

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // All-Category Yes/No Questions Card matching image.png + priority rule
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

    // ३. साईट सर्व्हे निरीक्षण व शेरा (Office Use Only) & Material Details from PDF Page 2
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
          text = "३. साईट सर्व्हे निरीक्षण व शेरा (Office Use Only)",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        MandatoryLabel("कागदपत्रांची स्थिती", isMandatory = true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          SegmentedChoiceButton(
            label = "पूर्ण",
            selected = wizard.materialAndOfficeDetails.documentsCompleteStatus,
            onClick = { onUpdateMaterialAndOffice { it.copy(documentsCompleteStatus = true) } },
            selectedColor = BrandNavy,
            modifier = Modifier.weight(1f),
            testTag = "docs_status_complete",
          )
          SegmentedChoiceButton(
            label = "अपूर्ण",
            selected = !wizard.materialAndOfficeDetails.documentsCompleteStatus,
            onClick = { onUpdateMaterialAndOffice { it.copy(documentsCompleteStatus = false) } },
            selectedColor = BrandCrimson,
            modifier = Modifier.weight(1f),
            testTag = "docs_status_incomplete",
          )
        }

        HorizontalDivider(color = DividerLight)
        MandatoryLabel("साहित्य तपशील (Material Details)", isMandatory = true)

        LabeledTextField(
          label = "१६ Sq.mm. Aluminium (Earth Wire)",
          value = wizard.materialAndOfficeDetails.earthWireMeters,
          placeholder = "20 मीटर",
          onValueChange = { v -> onUpdateMaterialAndOffice { it.copy(earthWireMeters = v) } },
          testTag = "material_earth_wire",
        )
        LabeledTextField(
          label = "AC 1.5 sq.mm 3 Core केबल",
          value = wizard.materialAndOfficeDetails.acCableMeters,
          placeholder = "10 मीटर",
          onValueChange = { v -> onUpdateMaterialAndOffice { it.copy(acCableMeters = v) } },
          testTag = "material_ac_cable",
        )
        LabeledTextField(
          label = "पाईप (Pipe)",
          value = wizard.materialAndOfficeDetails.pipeMeters,
          placeholder = "मीटर / तपशील लिहा",
          onValueChange = { v -> onUpdateMaterialAndOffice { it.copy(pipeMeters = v) } },
          testTag = "material_pipe",
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
          strokes = wizard.materialAndOfficeDetails.customerSignatureStrokes,
          onStrokesChange = { s ->
            onUpdateMaterialAndOffice { it.copy(customerSignatureStrokes = s) }
          },
          testTag = "customer_signature_pad",
        )

        HorizontalDivider(color = DividerLight)

        LabeledTextField(
          label = "सर्व्हे करणाऱ्या व्यक्तीचे नाव",
          isMandatory = true,
          value = wizard.materialAndOfficeDetails.surveyorName,
          placeholder = "सर्व्हे एक्झिक्युटिव्ह नाव",
          onValueChange = { name -> onUpdateMaterialAndOffice { it.copy(surveyorName = name) } },
          testTag = "surveyor_name_input",
        )

        SignaturePadCard(
          title = "सर्व्हे करणाऱ्या व्यक्तीची सही",
          strokes = wizard.materialAndOfficeDetails.surveyorSignatureStrokes,
          onStrokesChange = { s ->
            onUpdateMaterialAndOffice { it.copy(surveyorSignatureStrokes = s) }
          },
          testTag = "surveyor_signature_pad",
        )
      }
    }

    // Category Override + Executive Remark
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "श्रेणी बदलायची असल्यास निवडा (पर्यायी)",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SurveyCategory.entries.forEach { cat ->
            val isSelected = effectiveCategory == cat
            FilterChip(
              selected = isSelected,
              onClick = {
                if (cat == wizard.suggestedCategory) onOverrideCategory(null)
                else onOverrideCategory(cat)
              },
              label = { Text("श्रेणी ${cat.number}") },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandNavy,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }

        if (wizard.overrideCategory != null && wizard.overrideCategory != wizard.suggestedCategory) {
          OutlinedTextField(
            value = wizard.overrideReason,
            onValueChange = onOverrideReasonChange,
            modifier = Modifier.fillMaxWidth().testTag("override_reason_input"),
            label = { Text("श्रेणी बदलण्याचे कारण *") },
            placeholder = { Text("सुचवलेली श्रेणी बदलण्याचे कारण लिहा", color = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandNavy,
                unfocusedBorderColor = BorderSubtle,
              ),
          )
        }

        OutlinedTextField(
          value = wizard.executiveRemark,
          onValueChange = onRemarkChange,
          modifier = Modifier.fillMaxWidth().testTag("executive_remark_input"),
          label = { Text("एक्झिक्युटिव्ह शेरा (पर्यायी)") },
          placeholder = { Text("ग्राहक किंवा कागदपत्रांबद्दल विशेष नोंद", color = TextMuted) },
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )
      }
    }

    OutlinedButton(
      onClick = {
        val message =
          SurveyRuleEngine.buildWhatsAppPendingMessage(
            beneficiaryName = wizard.beneficiaryName.ifBlank { "ग्राहक" },
            villageName = villageNameMr,
            category = effectiveCategory,
            pendingActions = wizard.pendingActions,
          )
        val intent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
          }
        context.startActivity(Intent.createChooser(intent, "ग्राहकाला माहिती पाठवा"))
      },
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("share_pending_message_button"),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, BrandNavy),
    ) {
      Icon(
        imageVector = Icons.Outlined.Share,
        contentDescription = null,
        tint = BrandNavy,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "ग्राहकाला बाकी कागदपत्रांचा मेसेज पाठवा (WhatsApp/SMS)",
        color = BrandNavy,
        style = MaterialTheme.typography.labelLarge,
      )
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
