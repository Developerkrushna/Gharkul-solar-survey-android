package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SurveyRuleEngine
import com.example.model.CustomerSurvey
import com.example.model.DocumentLegibility
import com.example.model.NameMatchResult
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StatusTagChip
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PendingRoseBg
import com.example.ui.theme.PendingRoseText
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailSheet(
  customer: CustomerSurvey,
  onDismiss: () -> Unit,
  onEditClick: (CustomerSurvey) -> Unit,
  onDeleteClick: (CustomerSurvey) -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CardWhite,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = customer.beneficiaryName,
            style = MaterialTheme.typography.headlineSmall,
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${customer.villageNameMr} · ${customer.talukaNameMr} · ${customer.districtNameMr}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
          )
        }
        StatusPillBadge(status = customer.overallStatus)
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BannerIceBlue,
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = customer.finalCategory.titleMr,
            style = MaterialTheme.typography.titleSmall,
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "प्रवर्ग: ${customer.casteCategory.labelMr}  |  घर: ${customer.houseType.labelMr}  |  क्षमता: ${customer.capacity.labelMr}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
          if (customer.matchedCategories.size > 1) {
            Text(
              text = "लागू झालेल्या श्रेणी: ${customer.matchedCategories.joinToString(", ") { "श्रेणी ${it.number}" }} (प्राधान्य: 4 > 3 > 2 > 1)",
              style = MaterialTheme.typography.bodySmall,
              color = BrandNavy,
              fontWeight = FontWeight.SemiBold,
            )
          }
          if (customer.overrideReason.isNotBlank()) {
            Text(
              text = "श्रेणी बदल कारण: ${customer.overrideReason}",
              style = MaterialTheme.typography.bodySmall,
              color = WarningAmberText,
            )
          }
        }
      }

      // Personal & Meter summary
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (customer.surveyDate.isNotBlank()) DetailLine("सर्व्हे दिनांक", customer.surveyDate)
          DetailLine("मोबाईल", customer.mobile)
          if (customer.emailId.isNotBlank()) DetailLine("ई-मेल आयडी", customer.emailId)
          if (customer.addressLandmark.isNotBlank()) DetailLine("पत्ता", customer.addressLandmark)
          if (customer.gharkulId.isNotBlank()) DetailLine("घरकुल आयडी", customer.gharkulId)
          if (customer.aadhaarLast4.isNotBlank()) DetailLine("आधार क्र.", "XXXX-XXXX-${customer.aadhaarLast4}")
          if (!customer.gpsLocation.isNullOrBlank()) DetailLine("GPS लोकेशन", customer.gpsLocation)
          HorizontalDivider(color = DividerLight, modifier = Modifier.padding(vertical = 4.dp))
          DetailLine("लाईट मीटर", if (customer.meterInfo.hasMeter) "उपलब्ध" else "उपलब्ध नाही")
          if (customer.meterInfo.hasMeter) {
            if (customer.meterInfo.consumerNumber.isNotBlank()) {
              DetailLine("ग्राहक क्र.", customer.meterInfo.consumerNumber)
            }
            DetailLine(
              "मीटर धारक",
              if (customer.meterInfo.sameAsBeneficiary) "लाभार्थी स्वतः"
              else "${customer.meterInfo.meterHolderName} (${customer.meterInfo.relationship} · ${if (customer.meterInfo.isHolderAlive) "हयात" else "मृत"})",
            )
          }
          customer.meterInfo.isPoleNearbyForNewConn?.let {
            DetailLine("पोल जवळ आहे का?", if (it) "होय" else "नाही")
          }
          customer.meterInfo.interestedInNewConnection?.let {
            DetailLine("नवीन कनेक्शन इच्छुक?", if (it) "होय" else "नाही")
          }
        }
      }

      // 1. Site Survey Details (PDF Page 1-2)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        val site = customer.siteSurveyDetails
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "साईट सर्व्हे तपशील",
            style = MaterialTheme.typography.titleSmall,
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
          site.roofType?.let { DetailLine("छताचा प्रकार", it.labelMr) }
          site.hasSolarPanelSpace100SqFt?.let {
            DetailLine("पॅनेलसाठी १०० चौ.फूट जागा", if (it) "होय" else "नाही")
          }
          if (site.generationMeterLocation.isNotBlank()) {
            DetailLine("जनरेशन मीटर जागा", site.generationMeterLocation)
          }
          site.hasEarthingSpace?.let {
            DetailLine("अर्थिंगसाठी जागा", if (it) "होय" else "नाही")
          }
          DetailLine(
            "Note Cam फोटो (छत / अर्थिंग)",
            "${if (site.noteCamRoofBeforePhotoDone) "छत ✓" else "छत ✗"} · ${if (site.noteCamEarthingWithOwnerDone) "अर्थिंग ✓" else "अर्थिंग ✗"}",
          )
        }
      }

      // Document checklist status
      Text(
        text = "कागदपत्रांची स्थिती",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
      )
      val requiredDocs = SurveyRuleEngine.requiredDocumentsFor(customer.meterInfo, customer.casteCategory)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          requiredDocs.forEachIndexed { idx, docType ->
            val entry = customer.documents[docType]
            val printed = entry?.printedName.orEmpty()
            if (idx > 0) HorizontalDivider(color = DividerLight)
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = docType.labelMr,
                  style = MaterialTheme.typography.titleSmall,
                  color = TextPrimary,
                )
                val sizeLabel = entry?.photoSizeKb?.let { " · ${it} KB" }.orEmpty()
                Text(
                  text = (if (printed.isNotBlank()) "नाव: $printed" else "नाव प्रविष्ट नाही") + sizeLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted,
                )
              }
              val leg = entry?.legibility ?: DocumentLegibility.MISSING
              val match = entry?.matchResult ?: NameMatchResult.NOT_ENTERED
              val (bg, fg) =
                when {
                  leg == DocumentLegibility.CLEAR && (match == NameMatchResult.MATCH || printed.isBlank()) ->
                    SuccessGreenBg to SuccessGreenText
                  leg == DocumentLegibility.UNCLEAR || match == NameMatchResult.CLOSE_MATCH ->
                    WarningAmberBg to WarningAmberText
                  else -> PendingRoseBg to PendingRoseText
                }
              val chipText =
                if (leg != DocumentLegibility.CLEAR) leg.labelMr
                else if (entry?.printedName.isNullOrBlank()) leg.labelMr
                else match.labelMr
              StatusTagChip(text = chipText, bgColor = bg, textColor = fg)
            }
          }
        }
      }

      // 3. Office Material & Signatures summary
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        val office = customer.materialAndOfficeDetails
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "साहित्य तपशील व स्वाक्षरी",
            style = MaterialTheme.typography.titleSmall,
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
          DetailLine("कागदपत्रे स्थिती", if (office.documentsCompleteStatus) "पूर्ण" else "अपूर्ण")
          if (office.earthWireMeters.isNotBlank()) DetailLine("अर्थ वायर", office.earthWireMeters)
          if (office.acCableMeters.isNotBlank()) DetailLine("AC केबल", office.acCableMeters)
          if (office.pipeMeters.isNotBlank()) DetailLine("पाईप", office.pipeMeters)
          DetailLine("सर्व्हे अधिकारी", office.surveyorName.ifBlank { "-" })
          DetailLine(
            "डिजिटल स्वाक्षरी",
            "ग्राहक: ${if (office.customerSignatureStrokes.isNotEmpty()) "होय" else "नाही"} · अधिकारी: ${if (office.surveyorSignatureStrokes.isNotEmpty()) "होय" else "नाही"}",
          )
        }
      }

      if (customer.pendingActions.isNotEmpty()) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = PendingRoseBg,
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "बाकी कार्यवाही",
              style = MaterialTheme.typography.titleSmall,
              color = PendingRoseText,
              fontWeight = FontWeight.Bold,
            )
            customer.pendingActions.forEach { action ->
              Text(
                text = "• ${action.titleMr}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
              )
            }
          }
        }
      }

      if (customer.executiveRemark.isNotBlank()) {
        Text(
          text = "एक्झिक्युटिव्ह शेरा: ${customer.executiveRemark}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            val msg =
              SurveyRuleEngine.buildWhatsAppPendingMessage(
                beneficiaryName = customer.beneficiaryName,
                villageName = customer.villageNameMr,
                category = customer.finalCategory,
                pendingActions = customer.pendingActions,
              )
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
              }
            context.startActivity(Intent.createChooser(intent, "मेसेज पाठवा"))
          },
          modifier = Modifier.weight(1f).height(50.dp).testTag("detail_share_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, BrandNavy),
        ) {
          Icon(Icons.Outlined.Share, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("मेसेज पाठवा", color = BrandNavy, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = { onEditClick(customer) },
          modifier = Modifier.weight(1.2f).height(50.dp).testTag("detail_edit_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("संपादित करा", fontWeight = FontWeight.Bold)
        }
      }

      OutlinedButton(
        onClick = { onDeleteClick(customer) },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("detail_delete_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandCrimson),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = PendingRoseBg.copy(alpha = 0.45f),
            contentColor = BrandCrimson,
          ),
      ) {
        Icon(
          imageVector = Icons.Outlined.DeleteOutline,
          contentDescription = null,
          tint = BrandCrimson,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("ग्राहक हटवा (संपूर्ण माहिती व कागदपत्रे)", color = BrandCrimson, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun DetailLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
