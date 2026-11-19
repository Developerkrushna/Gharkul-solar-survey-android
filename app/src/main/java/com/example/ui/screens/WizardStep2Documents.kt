package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.SiteSurveyDetails
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
import com.example.util.LiveLocationOutcome
import com.example.viewmodel.AddEditWizardState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class NoteCamSlot(val prefix: String, val labelMr: String) {
  ROOF("notecam_roof", "सोलर बसवण्याच्या आधीच्या छताचा फोटो"),
  EARTHING("notecam_earthing", "Earthing Point चा फोटो (मालकासोबत)"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStep2Documents(
  wizard: AddEditWizardState,
  locationBreadcrumb: String,
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
  onUpdateSiteSurvey: ((SiteSurveyDetails) -> SiteSurveyDetails) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val requiredList = SurveyRuleEngine.requiredDocumentsFor(wizard.meterInfo, wizard.casteCategory)

  // Regular document capture state
  var activeDocForPhoto by remember { mutableStateOf<DocumentType?>(null) }
  var pendingDocCameraUri by remember { mutableStateOf<Uri?>(null) }

  val scope = rememberCoroutineScope()

  // Note Cam photo capture & live GPS state
  var activeNoteCamSlot by remember { mutableStateOf<NoteCamSlot?>(null) }
  var pendingNoteCamCameraUri by remember { mutableStateOf<Uri?>(null) }
  var resolvingNoteCamSlot by remember { mutableStateOf<NoteCamSlot?>(null) }
  var showEnableGpsDialog by remember { mutableStateOf(false) }

  fun handleSelectedDocImageUri(docType: DocumentType, rawUri: Uri) {
    val saved = ImageCompressionUtil.saveDocumentPhoto(context, rawUri, docType.name)
    if (saved != null) {
      onUpdateDocument(
        docType,
        null,
        saved.uriString,
        saved.sizeKb,
        true,
        DocumentLegibility.CLEAR,
        false,
        null,
      )
      onShowMessage("${docType.labelMr} फोटो जोडला (${saved.sizeKb} KB)")
    } else {
      onShowMessage("फोटो जोडताना त्रुटी आली. कृपया पुन्हा निवडा.")
    }
  }

  // Gallery upload: keeps the original gallery image completely unchanged with no overlay
  fun handleNoteCamGalleryUri(slot: NoteCamSlot, rawUri: Uri) {
    val saved = ImageCompressionUtil.saveDocumentPhoto(context, rawUri, slot.prefix)
    if (saved != null) {
      onUpdateSiteSurvey { site ->
        when (slot) {
          NoteCamSlot.ROOF ->
            site.copy(
              noteCamRoofBeforePhotoDone = true,
              noteCamRoofBeforePhotoUri = saved.uriString,
              noteCamRoofBeforePhotoKb = saved.sizeKb,
              noteCamRoofGeoInfo = "",
            )
          NoteCamSlot.EARTHING ->
            site.copy(
              noteCamEarthingWithOwnerDone = true,
              noteCamEarthingWithOwnerPhotoUri = saved.uriString,
              noteCamEarthingWithOwnerPhotoKb = saved.sizeKb,
              noteCamEarthingGeoInfo = "",
            )
        }
      }
      onShowMessage("${slot.labelMr} गॅलरीतून जोडला")
    } else {
      onShowMessage("Note Cam फोटो निवडताना त्रुटी आली.")
    }
  }

  fun handleNoteCamCameraUri(slot: NoteCamSlot, rawUri: Uri) {
    resolvingNoteCamSlot = slot
    scope.launch {
      val outcome =
        withContext(Dispatchers.IO) {
          ImageCompressionUtil.fetchLiveLocationOutcome(context, locationBreadcrumb)
        }
      when (outcome) {
        is LiveLocationOutcome.LocationDisabled,
        is LiveLocationOutcome.Unavailable -> {
          resolvingNoteCamSlot = null
          showEnableGpsDialog = true
        }
        is LiveLocationOutcome.Success -> {
          val stamped =
            withContext(Dispatchers.IO) {
              ImageCompressionUtil.stampNoteCamGeoLocation(
                context = context,
                sourceUri = rawUri,
                prefix = slot.prefix,
                gpsCoords = outcome.coordsText,
                locationBreadcrumb = outcome.addressText,
                timestampText = outcome.timestampText,
              )
            }
          resolvingNoteCamSlot = null
          if (stamped != null) {
            onUpdateSiteSurvey { site ->
              when (slot) {
                NoteCamSlot.ROOF ->
                  site.copy(
                    noteCamRoofBeforePhotoDone = true,
                    noteCamRoofBeforePhotoUri = stamped.uriString,
                    noteCamRoofBeforePhotoKb = stamped.sizeKb,
                    noteCamRoofGeoInfo = stamped.geoSummary,
                  )
                NoteCamSlot.EARTHING ->
                  site.copy(
                    noteCamEarthingWithOwnerDone = true,
                    noteCamEarthingWithOwnerPhotoUri = stamped.uriString,
                    noteCamEarthingWithOwnerPhotoKb = stamped.sizeKb,
                    noteCamEarthingGeoInfo = stamped.geoSummary,
                  )
              }
            }
            onShowMessage("${slot.labelMr} Live Geo-Location सह जतन केला")
          } else {
            onShowMessage("Note Cam फोटो जतन करताना त्रुटी आली.")
          }
        }
      }
    }
  }

  // Regular document launchers
  val docGalleryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val docType = activeDocForPhoto
      if (uri != null && docType != null) {
        handleSelectedDocImageUri(docType, uri)
      }
      activeDocForPhoto = null
    }

  val docCameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val docType = activeDocForPhoto
      val uri = pendingDocCameraUri
      if (success && docType != null && uri != null) {
        handleSelectedDocImageUri(docType, uri)
      }
      activeDocForPhoto = null
      pendingDocCameraUri = null
    }

  val docCameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val docType = activeDocForPhoto
      if (granted && docType != null) {
        val uri = createTempImageUri(context, docType.name)
        pendingDocCameraUri = uri
        docCameraLauncher.launch(uri)
      } else {
        activeDocForPhoto = null
        onShowMessage("कॅमेरा परवानगी नाकारली; कृपया गॅलरीतून फोटो निवडा.")
      }
    }

  // Note Cam launchers (Camera + Live Location)
  val noteCamGalleryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val slot = activeNoteCamSlot
      if (uri != null && slot != null) {
        handleNoteCamGalleryUri(slot, uri)
      }
      activeNoteCamSlot = null
    }

  val noteCamCameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val slot = activeNoteCamSlot
      val uri = pendingNoteCamCameraUri
      if (success && slot != null && uri != null) {
        handleNoteCamCameraUri(slot, uri)
      }
      activeNoteCamSlot = null
      pendingNoteCamCameraUri = null
    }

  val noteCamPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
      val slot = activeNoteCamSlot
      val cameraGranted =
        results[Manifest.permission.CAMERA] == true ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
          PackageManager.PERMISSION_GRANTED
      val locationGranted =
        results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED

      if (!cameraGranted) {
        activeNoteCamSlot = null
        onShowMessage("Note Cam कॅमेरा परवानगी नाकारली; कृपया गॅलरीतून फोटो अपलोड करा.")
      } else if (!locationGranted || !ImageCompressionUtil.isDeviceLocationEnabled(context)) {
        activeNoteCamSlot = null
        showEnableGpsDialog = true
      } else if (slot != null) {
        val uri = createTempImageUri(context, "${slot.prefix}_raw")
        pendingNoteCamCameraUri = uri
        noteCamCameraLauncher.launch(uri)
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Document header (no KB limit)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = BannerIceBlue,
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
          text = "२. आवश्यक कागदपत्रे तपासणी सूची",
          style = MaterialTheme.typography.titleSmall,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "गॅलरी किंवा कॅमेराद्वारे कागदपत्रांचे फोटो अपलोड करा व कागदपत्राबाबत टीप नोंदवा.",
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

    // महत्त्वाच्या सूचना व तपासणी (Unticked by default)
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
          label = "लाईट बिल घरगुती (Residential) आहे",
          checked = wizard.documentComplianceChecks.lightBillIsResidential,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(lightBillIsResidential = c) } },
          testTag = "compliance_residential_bill",
        )
        ComplianceCheckRow(
          label = "आधार आणि पॅनवरील नाव व जन्मतारीख सारखीच आहे",
          checked = wizard.documentComplianceChecks.panNameAndDobMatchAadhaar,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(panNameAndDobMatchAadhaar = c) } },
          testTag = "compliance_pan_aadhaar_match",
        )
        ComplianceCheckRow(
          label = "राष्ट्रीयकृत बँक खाते आहे (ADCC बँक नाही; खाते क्र. व IFSC स्पष्ट)",
          checked = wizard.documentComplianceChecks.bankIsNationalizedNotAdcc,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(bankIsNationalizedNotAdcc = c) } },
          testTag = "compliance_nationalized_bank",
        )
        ComplianceCheckRow(
          label = "आधार, पॅन, लाईट बिल व बँक खात्याला एकच चालू मोबाईल लिंक आहे",
          checked = wizard.documentComplianceChecks.singleMobileLinkedAllDocs,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(singleMobileLinkedAllDocs = c) } },
          testTag = "compliance_single_mobile",
        )
        ComplianceCheckRow(
          label = "बँक खाते आधारशी लिंक (Aadhaar Seeding) आहे",
          checked = wizard.documentComplianceChecks.aadhaarSeededToBank,
          onCheckedChange = { c -> onUpdateCompliance { it.copy(aadhaarSeededToBank = c) } },
          testTag = "compliance_aadhaar_seeded",
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
          onUpdateDocument(docType, name, null, null, null, null, false, null)
        },
        onCameraOrGalleryClick = {
          activeDocForPhoto = docType
        },
        onToggleMissing = { markMissing ->
          onUpdateDocument(
            docType,
            null,
            null,
            null,
            true,
            if (markMissing) DocumentLegibility.MISSING else DocumentLegibility.CLEAR,
            false,
            null,
          )
        },
        onQueryNoteChange = { note ->
          onUpdateDocument(docType, null, null, null, null, null, false, note)
        },
        onRemovePhoto = {
          onUpdateDocument(docType, null, null, null, true, DocumentLegibility.MISSING, true, null)
        },
      )
    }

    // Note Cam Section at the bottom of Step 2
    Surface(
      modifier = Modifier.fillMaxWidth().testTag("notecam_section_card"),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.5.dp, BrandNavy.copy(alpha = 0.25f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = BrandCrimson,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Note Cam फोटो (साईट Geo-Location सह)",
              style = MaterialTheme.typography.titleMedium,
              color = BrandNavy,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "गॅलरीतून निवडा किंवा कॅमेराद्वारे घ्या (कॅमेरा फोटोवर GPS, गाव/तालुका/जिल्हा व दिनांक-वेळ नोंदवली जाईल).",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
            )
          }
        }

        NoteCamUploadItem(
          slot = NoteCamSlot.ROOF,
          photoUri = wizard.siteSurveyDetails.noteCamRoofBeforePhotoUri,
          geoInfo = wizard.siteSurveyDetails.noteCamRoofGeoInfo,
          isResolvingLocation = resolvingNoteCamSlot == NoteCamSlot.ROOF,
          onCameraClick = {
            activeNoteCamSlot = NoteCamSlot.ROOF
            noteCamPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          },
          onGalleryClick = {
            activeNoteCamSlot = NoteCamSlot.ROOF
            noteCamGalleryPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onRemoveClick = {
            onUpdateSiteSurvey {
              it.copy(
                noteCamRoofBeforePhotoDone = false,
                noteCamRoofBeforePhotoUri = null,
                noteCamRoofBeforePhotoKb = null,
                noteCamRoofGeoInfo = "",
              )
            }
          },
        )

        HorizontalDivider(color = DividerLight)

        NoteCamUploadItem(
          slot = NoteCamSlot.EARTHING,
          photoUri = wizard.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri,
          geoInfo = wizard.siteSurveyDetails.noteCamEarthingGeoInfo,
          isResolvingLocation = resolvingNoteCamSlot == NoteCamSlot.EARTHING,
          onCameraClick = {
            activeNoteCamSlot = NoteCamSlot.EARTHING
            noteCamPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          },
          onGalleryClick = {
            activeNoteCamSlot = NoteCamSlot.EARTHING
            noteCamGalleryPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onRemoveClick = {
            onUpdateSiteSurvey {
              it.copy(
                noteCamEarthingWithOwnerDone = false,
                noteCamEarthingWithOwnerPhotoUri = null,
                noteCamEarthingWithOwnerPhotoKb = null,
                noteCamEarthingGeoInfo = "",
              )
            }
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
  }

  if (showEnableGpsDialog) {
    AlertDialog(
      onDismissRequest = { showEnableGpsDialog = false },
      containerColor = CardWhite,
      title = {
        Text(
          text = "Live GPS / लोकेशन आवश्यक आहे",
          style = MaterialTheme.typography.titleLarge,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = "Note Cam कॅमेरा फोटोवर अचूक Live GPS निर्देशांक व स्थान नोंदवण्यासाठी कृपया डिव्हाइसचे Location (GPS) सुरू करा.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          modifier = Modifier.testTag("gps_enable_prompt_dialog"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showEnableGpsDialog = false
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
          modifier = Modifier.testTag("gps_open_settings_button"),
        ) {
          Text("लोकेशन सुरू करा", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEnableGpsDialog = false }) {
          Text("रद्द करा", color = TextSecondary)
        }
      },
    )
  }

  // Bottom sheet for regular document upload
  val choosingDoc = activeDocForPhoto
  if (choosingDoc != null && pendingDocCameraUri == null) {
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
          text = "कोणत्याही आकाराचा स्पष्ट फोटो कॅमेरा किंवा गॅलरीतून अपलोड करता येईल.",
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
              val uri = createTempImageUri(context, choosingDoc.name)
              pendingDocCameraUri = uri
              docCameraLauncher.launch(uri)
            } else {
              docCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            docGalleryPicker.launch(
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
              null,
            )
            activeDocForPhoto = null
            onShowMessage("${choosingDoc.labelMr} नमुना फोटो जोडला")
          },
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreenText)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "नमुना फोटो जोडा",
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
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(checkedColor = BrandNavy),
      modifier = Modifier.testTag(testTag),
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
  onToggleMissing: (Boolean) -> Unit,
  onQueryNoteChange: (String) -> Unit,
  onRemovePhoto: () -> Unit,
) {
  var isNoteExpanded by remember(docType, entry.queryNote.isNotBlank()) {
    mutableStateOf(entry.queryNote.isNotBlank())
  }
  val isMissing = entry.legibility == DocumentLegibility.MISSING

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
        val sizeKb = entry.photoSizeKb
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(SuccessGreenBg)
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
              text = "फोटो संलग्न",
              style = MaterialTheme.typography.labelMedium,
              color = SuccessGreenText,
              fontWeight = FontWeight.Bold,
            )
            if (sizeKb != null) {
              Text(
                text = "आकार: $sizeKb KB",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
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

      // Note button + उपलब्ध नाही button (स्पष्ट/अस्पष्ट removed)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilterChip(
          selected = isNoteExpanded || entry.queryNote.isNotBlank(),
          onClick = { isNoteExpanded = !isNoteExpanded },
          leadingIcon = {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          label = {
            Text(
              text = if (entry.queryNote.isNotBlank()) "टीप (नोंदलेली)" else "टीप (Note)",
              style = MaterialTheme.typography.labelMedium,
            )
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("doc_note_btn_${docType.name}"),
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandNavy,
              selectedLabelColor = Color.White,
              selectedLeadingIconColor = Color.White,
            ),
        )

        FilterChip(
          selected = isMissing,
          onClick = { onToggleMissing(!isMissing) },
          label = {
            Text(
              text = DocumentLegibility.MISSING.labelMr,
              style = MaterialTheme.typography.labelMedium,
            )
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("doc_missing_btn_${docType.name}"),
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandCrimson,
              selectedLabelColor = Color.White,
            ),
        )
      }

      AnimatedVisibility(visible = isNoteExpanded) {
        OutlinedTextField(
          value = entry.queryNote,
          onValueChange = onQueryNoteChange,
          modifier = Modifier.fillMaxWidth().testTag("doc_note_input_${docType.name}"),
          label = { Text("कागदपत्राबाबत शंका / टीप") },
          placeholder = { Text("उदा. बिलावर पत्ता अपूर्ण आहे किंवा स्पष्ट प्रत हवी", color = TextMuted) },
          minLines = 2,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = BannerIceBlue.copy(alpha = 0.35f),
              unfocusedContainerColor = BannerIceBlue.copy(alpha = 0.25f),
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )
      }
    }
  }
}

@Composable
private fun NoteCamUploadItem(
  slot: NoteCamSlot,
  photoUri: String?,
  geoInfo: String,
  isResolvingLocation: Boolean,
  onCameraClick: () -> Unit,
  onGalleryClick: () -> Unit,
  onRemoveClick: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("${slot.prefix}_card"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = slot.labelMr,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f).padding(end = 8.dp),
      )
      StatusTagChip(
        text = if (photoUri != null) "फोटो जोडला" else "प्रलंबित",
        bgColor = if (photoUri != null) SuccessGreenBg else WarningAmberBg,
        textColor = if (photoUri != null) SuccessGreenText else WarningAmberText,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onCameraClick,
        enabled = !isResolvingLocation,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BrandNavy),
        modifier = Modifier.weight(1f).height(48.dp).testTag("${slot.prefix}_camera"),
      ) {
        Icon(
          imageVector = Icons.Outlined.CameraAlt,
          contentDescription = null,
          tint = BrandNavy,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("कॅमेरा (Geo)", color = BrandNavy, fontWeight = FontWeight.SemiBold)
      }

      OutlinedButton(
        onClick = onGalleryClick,
        enabled = !isResolvingLocation,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.weight(1f).height(48.dp).testTag("${slot.prefix}_gallery"),
      ) {
        Icon(
          imageVector = Icons.Outlined.PhotoLibrary,
          contentDescription = null,
          tint = TextPrimary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("गॅलरी", color = TextPrimary, fontWeight = FontWeight.SemiBold)
      }
    }

    if (isResolvingLocation) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BannerIceBlue)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(color = BrandNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Live GPS व स्थान मिळवत आहे...",
          style = MaterialTheme.typography.bodySmall,
          color = BrandNavy,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    if (photoUri != null) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BannerIceBlue)
            .testTag("${slot.prefix}_preview_box"),
      ) {
        AsyncImage(
          model = photoUri,
          contentDescription = slot.labelMr,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )

        IconButton(
          onClick = onRemoveClick,
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(8.dp)
              .size(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(Color.Black.copy(alpha = 0.45f)),
        ) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Note Cam फोटो काढा",
            tint = Color.White,
          )
        }

        // Translucent bottom-right geo-location overlay ONLY when Camera live geo-info exists
        if (geoInfo.isNotBlank()) {
          Box(
            modifier =
              Modifier.align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("${slot.prefix}_geo_overlay"),
          ) {
            Text(
              text = geoInfo,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.92f),
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
  }
}

private fun createTempImageUri(context: Context, prefix: String): Uri {
  val dir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
  val file = File(dir, "${prefix.lowercase()}_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
