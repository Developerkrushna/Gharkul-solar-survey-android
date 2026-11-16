package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.domain.SurveyRuleEngine
import com.example.model.DocumentComplianceChecks
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.NameMatchResult
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
import com.example.util.ImageCompressionUtil
import com.example.viewmodel.AddEditWizardState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStep2Documents(
  wizard: AddEditWizardState,
  onUpdateDocument: (
    type: DocumentType,
    printedName: String?,
    photoUri: String?,
    photoSizeKb: Int?,
    isSizeValid: Boolean?,
    legibility: DocumentLegibility?,
    clearPhoto: Boolean,
  ) -> Unit,
  onUpdateCompliance: ((DocumentComplianceChecks) -> DocumentComplianceChecks) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val requiredList = SurveyRuleEngine.requiredDocumentsFor(wizard.meterInfo, wizard.casteCategory)

  var activeDocForPhoto by remember { mutableStateOf<DocumentType?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  fun handleSelectedImageUri(docType: DocumentType, rawUri: Uri) {
    val compressed = ImageCompressionUtil.compressToTargetRange(context, rawUri, docType.name)
    if (compressed != null) {
      onUpdateDocument(
        docType,
        null,
        compressed.uriString,
        compressed.sizeKb,
        compressed.isWithinTargetRange,
        DocumentLegibility.CLEAR,
        false,
      )
      if (compressed.isWithinTargetRange) {
        onShowMessage("${docType.labelMr} फोटो जोडला (${compressed.sizeKb} KB · 200-500 KB वैध)")
      } else {
        onShowMessage("${docType.labelMr} फोटो आकार ${compressed.sizeKb} KB आहे (200-500 KB दरम्यान हवा).")
      }
    } else {
      onShowMessage("फोटो प्रक्रिया करताना त्रुटी आली. कृपया पुन्हा निवडा.")
    }
  }

  val galleryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val docType = activeDocForPhoto
      if (uri != null && docType != null) {
        handleSelectedImageUri(docType, uri)
      }
      activeDocForPhoto = null
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val docType = activeDocForPhoto
      val uri = pendingCameraUri
      if (success && docType != null && uri != null) {
        handleSelectedImageUri(docType, uri)
      }
      activeDocForPhoto = null
      pendingCameraUri = null
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val docType = activeDocForPhoto
      if (granted && docType != null) {
        val uri = createTempImageUri(context, docType)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
      } else {
        activeDocForPhoto = null
        onShowMessage("कॅमेरा परवानगी नाकारली; कृपया गॅलरीतून फोटो निवडा.")
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 200-500 KB Callout + Mandatory Star Legend
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = BannerIceBlue,
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
          text = "२. आवश्यक कागदपत्रे तपासणी सूची (200 KB ते 500 KB)",
          style = MaterialTheme.typography.titleSmall,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "प्रत्येक कागदपत्राच्या फोटोचा आकार 200 KB ते 500 KB दरम्यान असणे अनिवार्य आहे.",
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text =
            buildAnnotatedString {
              withStyle(SpanStyle(color = BrandCrimson, fontWeight = FontWeight.Bold)) { append("* ") }
              withStyle(SpanStyle(color = TextSecondary)) { append("म्हणजे अनिवार्य कागदपत्र.") }
            },
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // Important PDF Page 2 Rules + Compliance Checks
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = WarningAmberBg,
      border = BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.25f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = WarningAmberText,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "महत्त्वाच्या सूचना व तपासणी",
            style = MaterialTheme.typography.titleSmall,
            color = WarningAmberText,
            fontWeight = FontWeight.Bold,
          )
        }

        ComplianceCheckRow(
          label = "लाईट बिल घरगुती (Residential) आहे (कमर्शियल/इंडस्ट्रियल नाही)",
          checked = wizard.documentComplianceChecks.lightBillIsResidential,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(lightBillIsResidential = c) } },
        )
        ComplianceCheckRow(
          label = "आधार आणि पॅनवरील नाव व जन्मतारीख सारखीच आहे",
          checked = wizard.documentComplianceChecks.panNameAndDobMatchAadhaar,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(panNameAndDobMatchAadhaar = c) } },
        )
        ComplianceCheckRow(
          label = "राष्ट्रीयकृत बँक खाते आहे (ADCC बँक नाही; खाते क्र. व IFSC स्पष्ट)",
          checked = wizard.documentComplianceChecks.bankIsNationalizedNotAdcc,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(bankIsNationalizedNotAdcc = c) } },
        )
        ComplianceCheckRow(
          label = "आधार, पॅन, लाईट बिल व बँक खात्याला एकच चालू मोबाईल लिंक आहे",
          checked = wizard.documentComplianceChecks.singleMobileLinkedAllDocs,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(singleMobileLinkedAllDocs = c) } },
        )
        ComplianceCheckRow(
          label = "बँक खाते आधारशी लिंक (Aadhaar Seeding) आहे",
          checked = wizard.documentComplianceChecks.aadhaarSeededToBank,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(aadhaarSeededToBank = c) } },
        )
      }
    }

    requiredList.forEach { docType ->
      val entry = wizard.documents[docType] ?: DocumentEntry(docType)
      val isMandatory =
        docType.isRequiredCore ||
          docType == DocumentType.CASTE_CERTIFICATE ||
          !wizard.meterInfo.sameAsBeneficiary
      DocumentCaptureCard(
        docType = docType,
        entry = entry,
        isRequired = isMandatory,
        onPrintedNameChange = { name ->
          onUpdateDocument(docType, name, null, null, null, null, false)
        },
        onCameraOrGalleryClick = {
          activeDocForPhoto = docType
        },
        onLegibilityChange = { legibility ->
          onUpdateDocument(docType, null, null, null, null, legibility, false)
        },
        onRemovePhoto = {
          onUpdateDocument(docType, null, null, null, true, DocumentLegibility.MISSING, true)
        },
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
  }

  val choosingDoc = activeDocForPhoto
  if (choosingDoc != null && pendingCameraUri == null) {
    ModalBottomSheet(
      onDismissRequest = { activeDocForPhoto = null },
      containerColor = CardWhite,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "${choosingDoc.labelMr} फोटो जोडा",
          style = MaterialTheme.typography.titleLarge,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "निवडलेला फोटो आपोआप 200 KB ते 500 KB दरम्यान कॉम्प्रेस केला जाईल.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
        )

        Surface(
          modifier = Modifier.fillMaxWidth().testTag("pick_from_camera"),
          shape = RoundedCornerShape(14.dp),
          color = BannerIceBlue,
          onClick = {
            val hasPermission =
              ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
              val uri = createTempImageUri(context, choosingDoc)
              pendingCameraUri = uri
              cameraLauncher.launch(uri)
            } else {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
          },
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "कॅमेराने फोटो काढा",
              style = MaterialTheme.typography.titleMedium,
              color = BrandNavy,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Surface(
          modifier = Modifier.fillMaxWidth().testTag("pick_from_gallery"),
          shape = RoundedCornerShape(14.dp),
          color = CardWhite,
          border = BorderStroke(1.dp, BorderSubtle),
          onClick = {
            galleryPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "गॅलरीतून निवडा",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Surface(
          modifier = Modifier.fillMaxWidth().testTag("simulate_clear_capture"),
          shape = RoundedCornerShape(14.dp),
          color = SuccessGreenBg,
          onClick = {
            onUpdateDocument(
              choosingDoc,
              null,
              "captured://sample_${choosingDoc.name}",
              320,
              true,
              DocumentLegibility.CLEAR,
              false,
            )
            activeDocForPhoto = null
            onShowMessage("${choosingDoc.labelMr} नमुना स्पष्ट फोटो जोडला (320 KB)")
          },
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreenText)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "नमुना स्पष्ट फोटो जोडा (320 KB)",
              style = MaterialTheme.typography.titleSmall,
              color = SuccessGreenText,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun ComplianceCheckRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(checkedColor = BrandNavy),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = TextPrimary,
    )
  }
}

@Composable
private fun DocumentCaptureCard(
  docType: DocumentType,
  entry: DocumentEntry,
  isRequired: Boolean,
  onPrintedNameChange: (String) -> Unit,
  onCameraOrGalleryClick: () -> Unit,
  onLegibilityChange: (DocumentLegibility) -> Unit,
  onRemovePhoto: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("doc_card_${docType.name}"),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              buildAnnotatedString {
                if (isRequired) {
                  withStyle(SpanStyle(color = BrandCrimson, fontWeight = FontWeight.Bold)) { append("* ") }
                }
                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                  append(docType.labelMr)
                }
              },
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = docType.subtitleMr,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
          )
        }

        if (entry.printedName.isNotBlank()) {
          val (bg, fg) =
            when (entry.matchResult) {
              NameMatchResult.MATCH -> SuccessGreenBg to SuccessGreenText
              NameMatchResult.CLOSE_MATCH -> WarningAmberBg to WarningAmberText
              NameMatchResult.MISMATCH -> PendingRoseBg to PendingRoseText
              NameMatchResult.NOT_ENTERED -> BannerIceBlue to TextMuted
            }
          Spacer(modifier = Modifier.width(6.dp))
          StatusTagChip(text = entry.matchResult.labelMr, bgColor = bg, textColor = fg)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = entry.printedName,
          onValueChange = onPrintedNameChange,
          modifier = Modifier.weight(1f).testTag("doc_name_${docType.name}"),
          placeholder = { Text("कागदपत्रावरील नाव लिहा", color = TextMuted) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardWhite,
              unfocusedContainerColor = CardWhite,
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )

        Surface(
          modifier = Modifier.size(54.dp).testTag("doc_camera_${docType.name}"),
          shape = RoundedCornerShape(12.dp),
          color = if (entry.photoUri != null) SuccessGreenBg else BannerIceBlue,
          border =
            BorderStroke(
              1.dp,
              if (entry.photoUri != null) SuccessGreenText.copy(alpha = 0.3f) else BorderSubtle,
            ),
          onClick = onCameraOrGalleryClick,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.CameraAlt,
              contentDescription = "${docType.labelMr} फोटो घ्या",
              tint = if (entry.photoUri != null) SuccessGreenText else BrandNavy,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }

      if (entry.photoUri != null) {
        val sizeKb = entry.photoSizeKb ?: 320
        val valid = sizeKb in ImageCompressionUtil.MIN_TARGET_KB..ImageCompressionUtil.MAX_TARGET_KB
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (valid) SuccessGreenBg else PendingRoseBg)
              .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (!entry.photoUri.startsWith("captured://")) {
            AsyncImage(
              model = entry.photoUri,
              contentDescription = "${docType.labelMr} थंबनेल",
              modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(10.dp))
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (valid) "फोटो संलग्न (वैध आकार)" else "आकार 200-500 KB मर्यादेबाहेर!",
              style = MaterialTheme.typography.labelMedium,
              color = if (valid) SuccessGreenText else PendingRoseText,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "आकार: $sizeKb KB (मर्यादा: 200–500 KB)",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
          }
          IconButton(onClick = onRemovePhoto) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = "फोटो काढा",
              tint = BrandCrimson,
            )
          }
        }
      }

      HorizontalDivider(color = DividerLight)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DocumentLegibility.entries.forEach { legibility ->
          val selected = entry.legibility == legibility
          val selectedBg =
            when (legibility) {
              DocumentLegibility.CLEAR -> BrandNavy
              DocumentLegibility.UNCLEAR -> WarningAmberText
              DocumentLegibility.MISSING -> BrandCrimson
            }
          FilterChip(
            selected = selected,
            onClick = { onLegibilityChange(legibility) },
            label = { Text(text = legibility.labelMr, style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.weight(1f).testTag("doc_leg_${docType.name}_${legibility.name}"),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = selectedBg,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }
    }
  }
}

private fun createTempImageUri(context: Context, docType: DocumentType): Uri {
  val dir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
  val file = File(dir, "${docType.name.lowercase()}_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
