package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.InstallationPhotoItem
import com.example.model.InstallationProductRow
import com.example.model.PanelSerialEntry
import com.example.model.SerialVerificationField
import com.example.ui.components.BrandTopBar
import com.example.ui.components.RemoteAwarePhotoPreview
import com.example.ui.components.SignaturePadCard
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
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import com.example.util.ImageCompressionUtil
import com.example.util.LiveLocationOutcome
import com.example.viewmodel.InstallationFormState
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InstallationFormScreen(
  form: InstallationFormState,
  snackbarHostState: SnackbarHostState,
  isSaving: Boolean = false,
  onSetPage: (Int) -> Unit,
  onNextPage: () -> Unit,
  onPreviousPage: () -> Unit,
  onUpdateHeader: (
    consumerNo: String?,
    installationDate: String?,
    consumerName: String?,
    mobileNo: String?,
    systemCapacity: String?,
    systemType: String?,
    villageName: String?,
  ) -> Unit,
  onUpdateProductRow: (
    rowId: String,
    company: String?,
    size: String?,
    capacity: String?,
    qty: String?,
  ) -> Unit,
  onUpdateSignOff: (
    installerName: String?,
    installerSignatureStrokes: List<List<Pair<Float, Float>>>?,
    supervisorName: String?,
    managerName: String?,
    customerSignatureStrokes: List<List<Pair<Float, Float>>>?,
  ) -> Unit,
  onUpdateInverterSerial: (primary: String?, confirm: String?) -> Unit,
  onUpdateMeterSerial: (primary: String?, confirm: String?) -> Unit,
  onUpdatePanelSerial: (panelId: String, primary: String?, confirm: String?) -> Unit,
  onAddPanel: () -> Unit,
  onRemovePanel: (String) -> Unit,
  onAddPhotoSlot: () -> Unit,
  onRemovePhotoSlot: (String) -> Unit,
  onUpdatePhotoCaption: (String, String) -> Unit,
  onUpdatePhotoCapture: (
    photoId: String,
    photoUri: String?,
    photoSizeKb: Int?,
    geoInfo: String,
    isCameraGeoStamped: Boolean,
  ) -> Unit,
  onShowMessage: (String) -> Unit,
  onSubmitCertificate: () -> Unit,
) {
  BackHandler { onPreviousPage() }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Step 1 collapsible states
  var consumerExpanded by remember { mutableStateOf(true) }
  var materialsExpanded by remember { mutableStateOf(true) }
  var signOffExpanded by remember { mutableStateOf(true) }

  // Step 3 NoteCam photo capture states
  var activePhotoId by remember { mutableStateOf<String?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var resolvingPhotoId by remember { mutableStateOf<String?>(null) }
  var showEnableGpsDialog by remember { mutableStateOf(false) }

  fun openDatePicker() {
    val cal = Calendar.getInstance()
    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val formatted = String.format(Locale.ENGLISH, "%02d / %02d / %04d", dayOfMonth, month + 1, year)
        onUpdateHeader(null, formatted, null, null, null, null, null)
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
    ).show()
  }

  fun handleGalleryPhoto(photoId: String, rawUri: Uri) {
    val saved = ImageCompressionUtil.saveDocumentPhoto(context, rawUri, "install_site")
    if (saved != null) {
      onUpdatePhotoCapture(photoId, saved.uriString, saved.sizeKb, "", false)
      onShowMessage("Gallery photo attached (${saved.sizeKb} KB).")
    } else {
      onShowMessage("Failed to load gallery image. Please try again.")
    }
  }

  fun handleCameraPhoto(photoId: String, rawUri: Uri) {
    resolvingPhotoId = photoId
    val fallbackLoc = form.villageName.ifBlank { "Solar Installation Site" }
    scope.launch {
      val outcome =
        withContext(Dispatchers.IO) {
          ImageCompressionUtil.fetchLiveLocationOutcome(context, fallbackLoc)
        }
      when (outcome) {
        is LiveLocationOutcome.LocationDisabled,
        is LiveLocationOutcome.Unavailable -> {
          resolvingPhotoId = null
          showEnableGpsDialog = true
        }
        is LiveLocationOutcome.Success -> {
          val stamped =
            withContext(Dispatchers.IO) {
              ImageCompressionUtil.stampNoteCamGeoLocation(
                context = context,
                sourceUri = rawUri,
                prefix = "install_notecam",
                gpsCoords = outcome.coordsText,
                locationBreadcrumb = outcome.addressText,
                timestampText = outcome.timestampText,
              )
            }
          resolvingPhotoId = null
          if (stamped != null) {
            onUpdatePhotoCapture(photoId, stamped.uriString, stamped.sizeKb, stamped.geoSummary, true)
            onShowMessage("NoteCam photo saved with Live GPS coordinates.")
          } else {
            onShowMessage("Failed to process NoteCam photo.")
          }
        }
      }
    }
  }

  val galleryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      val pid = activePhotoId
      if (uri != null && pid != null) {
        handleGalleryPhoto(pid, uri)
      }
      activePhotoId = null
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val pid = activePhotoId
      val uri = pendingCameraUri
      if (success && pid != null && uri != null) {
        handleCameraPhoto(pid, uri)
      }
      activePhotoId = null
      pendingCameraUri = null
    }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
      val pid = activePhotoId
      val cameraOk =
        results[Manifest.permission.CAMERA] == true ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
          PackageManager.PERMISSION_GRANTED
      val locationOk =
        results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED

      if (!cameraOk) {
        activePhotoId = null
        onShowMessage("Camera permission denied. Please choose Gallery or enable Camera permission.")
      } else if (!locationOk || !ImageCompressionUtil.isDeviceLocationEnabled(context)) {
        activePhotoId = null
        showEnableGpsDialog = true
      } else if (pid != null) {
        val uri = createInstallTempImageUri(context, "install_notecam_raw")
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
      }
    }

  fun triggerNoteCamCapture(photoId: String) {
    activePhotoId = photoId
    val hasCam =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val hasLoc =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    if (!hasCam || !hasLoc) {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.CAMERA,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    } else if (!ImageCompressionUtil.isDeviceLocationEnabled(context)) {
      activePhotoId = null
      showEnableGpsDialog = true
    } else {
      val uri = createInstallTempImageUri(context, "install_notecam_raw")
      pendingCameraUri = uri
      cameraLauncher.launch(uri)
    }
  }

  Scaffold(
    containerColor = SurfaceCanvas,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Column(modifier = Modifier.fillMaxWidth().background(CardWhite)) {
        BrandTopBar(
          title = "Installation Certificate",
          subtitle = "Step ${form.currentPage} of 3 · 1 KW ONGRID",
          showBackButton = true,
          onBackClick = onPreviousPage,
          stepProgress = form.currentPage,
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StepTabPill(
            label = "1. Fill Form",
            selected = form.currentPage == 1,
            onClick = { onSetPage(1) },
            modifier = Modifier.weight(1f).testTag("install_tab_page_1"),
          )
          StepTabPill(
            label = "2. Serial Nos.",
            selected = form.currentPage == 2,
            onClick = { onSetPage(2) },
            modifier = Modifier.weight(1f).testTag("install_tab_page_2"),
          )
          StepTabPill(
            label = "3. NoteCam",
            selected = form.currentPage == 3,
            onClick = { onSetPage(3) },
            modifier = Modifier.weight(1f).testTag("install_tab_page_3"),
          )
        }
        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
      }
    },
    bottomBar = {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardWhite,
        shadowElevation = 10.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
          HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
          if (!form.validationError.isNullOrBlank()) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .background(PendingRoseBg)
                  .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
              Text(
                text = form.validationError,
                style = MaterialTheme.typography.bodySmall,
                color = PendingRoseText,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = onPreviousPage,
              enabled = !isSaving,
              modifier = Modifier.weight(1f).height(50.dp).testTag("install_prev_button"),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, BrandNavy),
            ) {
              Text(
                text = if (form.currentPage == 1) "Cancel" else "Previous",
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
              )
            }

            Button(
              onClick = { if (form.currentPage < 3) onNextPage() else onSubmitCertificate() },
              enabled = !isSaving,
              modifier =
                Modifier.weight(1.45f)
                  .height(50.dp)
                  .testTag(
                    if (form.currentPage < 3) "install_next_button" else "install_submit_button"
                  ),
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (form.currentPage < 3) BrandNavy else BrandCrimson,
                  contentColor = Color.White,
                ),
            ) {
              if (isSaving && form.currentPage == 3) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Uploading & Saving...",
                  fontWeight = FontWeight.Bold,
                )
              } else {
                Text(
                  text =
                    when (form.currentPage) {
                      1 -> "Next: Serial Nos."
                      2 -> "Next: NoteCam Photos"
                      else -> "Submit Certificate"
                    },
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (form.currentPage) {
          // ==================== STEP 1: CLEAN COLLAPSIBLE CERTIFICATE FORM ====================
          1 -> {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, BrandNavy.copy(alpha = 0.25f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.brand_logo),
                    contentDescription = "Reflection Media Logo",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "REFLECTION MEDIA",
                      style = MaterialTheme.typography.titleSmall,
                      color = BrandNavy,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "INSTALLATION / COMPLETION CERTIFICATE FOR SOLAR SYSTEM",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                    )
                  }
                  StatusTagChip(
                    text = "${form.systemCapacity} ${form.systemType}",
                    bgColor = BannerIceBlue,
                    textColor = BrandNavy,
                  )
                }
              }
            }

            // Collapsible Section A: Consumer & System Details
            item {
              CollapsibleFormSection(
                title = "A. Consumer & System Details",
                summary =
                  if (form.consumerName.isNotBlank()) "${form.consumerName} · ${form.consumerNo}"
                  else "Consumer No, Date, Name, Mobile & Capacity",
                expanded = consumerExpanded,
                onToggle = { consumerExpanded = !consumerExpanded },
                testTag = "section_consumer_toggle",
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    EnglishField(
                      label = "Consumer No. *",
                      value = form.consumerNo,
                      onValueChange = { onUpdateHeader(it, null, null, null, null, null, null) },
                      placeholder = "Light bill consumer no.",
                      keyboardType = KeyboardType.Number,
                      modifier = Modifier.weight(1f),
                      testTag = "install_consumer_no_input",
                    )

                    Column(
                      modifier = Modifier.weight(1f),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Text(
                        text = "Installation Date *",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      OutlinedTextField(
                        value = form.installationDate,
                        onValueChange = { onUpdateHeader(null, it, null, null, null, null, null) },
                        modifier = Modifier.fillMaxWidth().testTag("install_date_input"),
                        trailingIcon = {
                          IconButton(onClick = ::openDatePicker) {
                            Icon(
                              imageVector = Icons.Outlined.CalendarToday,
                              contentDescription = "Pick Installation Date",
                              tint = BrandNavy,
                              modifier = Modifier.size(18.dp),
                            )
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = installFieldColors(),
                      )
                    }
                  }

                  EnglishField(
                    label = "Consumer Name *",
                    value = form.consumerName,
                    onValueChange = { onUpdateHeader(null, null, it, null, null, null, null) },
                    placeholder = "Enter full consumer name",
                    capitalization = KeyboardCapitalization.Words,
                    testTag = "install_consumer_name_input",
                  )

                  EnglishField(
                    label = "Mobile No. *",
                    value = form.mobileNo,
                    onValueChange = { onUpdateHeader(null, null, null, it, null, null, null) },
                    placeholder = "10-digit mobile number",
                    keyboardType = KeyboardType.Phone,
                    testTag = "install_mobile_input",
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    EnglishField(
                      label = "System Capacity *",
                      value = form.systemCapacity,
                      onValueChange = { onUpdateHeader(null, null, null, null, it, null, null) },
                      placeholder = "1 KW",
                      modifier = Modifier.weight(1f),
                      testTag = "install_capacity_input",
                    )
                    EnglishField(
                      label = "System Type *",
                      value = form.systemType,
                      onValueChange = { onUpdateHeader(null, null, null, null, null, it, null) },
                      placeholder = "ONGRID",
                      modifier = Modifier.weight(1f),
                      testTag = "install_type_input",
                    )
                  }
                }
              }
            }

            // Collapsible Section B: Product & Material Specifications (2-Column Vertical Grid)
            item {
              CollapsibleFormSection(
                title = "B. Product & Material Table (Items 1 - 14)",
                summary = "${form.products.size} Items · Sample 1 KW * Required, Others Optional",
                expanded = materialsExpanded,
                onToggle = { materialsExpanded = !materialsExpanded },
                testTag = "section_materials_toggle",
              ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  // Table Header Legend Bar
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .background(BannerIceBlue, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Sr. No. · Product Model",
                      style = MaterialTheme.typography.labelMedium,
                      color = BrandNavy,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "* = Required in Sample",
                      style = MaterialTheme.typography.labelSmall,
                      color = BrandCrimson,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  form.products.forEachIndexed { idx, row ->
                    CompactInlineMaterialRow(
                      row = row,
                      onUpdate = { company, size, capacity, qty ->
                        onUpdateProductRow(row.rowId, company, size, capacity, qty)
                      },
                    )
                    if (idx < form.products.lastIndex) {
                      HorizontalDivider(color = DividerLight, thickness = 0.8.dp)
                    }
                  }
                }
              }
            }

            // Collapsible Section C: Verification & Signatures
            item {
              CollapsibleFormSection(
                title = "C. Sign-Off & Office Verification",
                summary =
                  buildString {
                    append("Installer: ${form.installerName.ifBlank { "-" }}")
                    append(
                      if (form.installerSignatureStrokes.isNotEmpty() && form.customerSignatureStrokes.isNotEmpty())
                        " · Both Signatures Ready"
                      else " · Signatures Pending"
                    )
                  },
                expanded = signOffExpanded,
                onToggle = { signOffExpanded = !signOffExpanded },
                testTag = "section_signoff_toggle",
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  EnglishField(
                    label = "Installer Name *",
                    value = form.installerName,
                    onValueChange = { onUpdateSignOff(it, null, null, null, null) },
                    placeholder = "Enter installer full name",
                    capitalization = KeyboardCapitalization.Words,
                    testTag = "install_installer_name_input",
                  )

                  SignaturePadCard(
                    title = "Installer Signature *",
                    strokes = form.installerSignatureStrokes,
                    onStrokesChange = { onUpdateSignOff(null, it, null, null, null) },
                    testTag = "install_installer_signature",
                    clearLabel = "Clear",
                    placeholderText = "Installer signs here with finger",
                  )

                  HorizontalDivider(thickness = 1.dp, color = DividerLight)

                  Text(
                    text = "Office Verification (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    EnglishField(
                      label = "1) Supervisor",
                      value = form.supervisorName,
                      onValueChange = { onUpdateSignOff(null, null, it, null, null) },
                      placeholder = "Supervisor name",
                      capitalization = KeyboardCapitalization.Words,
                      modifier = Modifier.weight(1f),
                      testTag = "install_supervisor_input",
                    )
                    EnglishField(
                      label = "2) Manager",
                      value = form.managerName,
                      onValueChange = { onUpdateSignOff(null, null, null, it, null) },
                      placeholder = "Manager name",
                      capitalization = KeyboardCapitalization.Words,
                      modifier = Modifier.weight(1f),
                      testTag = "install_manager_input",
                    )
                  }

                  HorizontalDivider(thickness = 1.dp, color = DividerLight)

                  SignaturePadCard(
                    title = "Customer Sign and Stamp *",
                    strokes = form.customerSignatureStrokes,
                    onStrokesChange = { onUpdateSignOff(null, null, null, null, it) },
                    testTag = "install_customer_signature",
                    clearLabel = "Clear",
                    placeholderText = "Customer signs here with finger",
                  )

                  Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = BannerIceBlue,
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                      Text(
                        text = "CUSTOMER CARE: 7558723283 / 7283838383 · support@reflectionmedias.in",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandNavy,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "CORPORATE OFFICE: REFLECTION MEDIA, NEAR BUS STAND, NEWASA, DIST - AHILYANAGAR PIN - 414603",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                      )
                    }
                  }
                }
              }
            }
          }

          // ==================== STEP 2: DOUBLE-ENTRY SERIAL NUMBERS ====================
          2 -> {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BannerIceBlue,
                border = BorderStroke(1.dp, BrandNavy.copy(alpha = 0.25f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = BrandNavy,
                    modifier = Modifier.size(26.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Serial Numbers — Double-Entry Verification",
                      style = MaterialTheme.typography.titleSmall,
                      color = BrandNavy,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Enter every serial number twice to prevent typing mistakes. All pairs must match.",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                }
              }
            }

            item {
              DoubleEntrySerialCard(
                title = "Invertor Serial Number *",
                field = form.inverterSerial,
                primaryPlaceholder = "Enter Invertor Serial No.",
                confirmPlaceholder = "Re-enter Invertor Serial No.",
                testTagPrefix = "serial_inverter",
                onPrimaryChange = { onUpdateInverterSerial(it, null) },
                onConfirmChange = { onUpdateInverterSerial(null, it) },
              )
            }

            item {
              DoubleEntrySerialCard(
                title = "Generation Meter Serial Number *",
                field = form.generationMeterSerial,
                primaryPlaceholder = "Enter Generation Meter Serial No.",
                confirmPlaceholder = "Re-enter Generation Meter Serial No.",
                testTagPrefix = "serial_meter",
                onPrimaryChange = { onUpdateMeterSerial(it, null) },
                onConfirmChange = { onUpdateMeterSerial(null, it) },
              )
            }

            item {
              Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Panel Serial Numbers",
                  style = MaterialTheme.typography.titleMedium,
                  color = BrandNavy,
                  fontWeight = FontWeight.Bold,
                )
                StatusTagChip(
                  text = "${form.panelSerials.size} Panels",
                  bgColor = BannerIceBlue,
                  textColor = BrandNavy,
                )
              }
            }

            itemsIndexed(form.panelSerials, key = { _, p -> p.id }) { index, panel: PanelSerialEntry ->
              DoubleEntrySerialCard(
                title = "Panel ${index + 1} Serial Number *",
                field = panel.serial,
                primaryPlaceholder = "Enter Panel ${index + 1} Serial No.",
                confirmPlaceholder = "Re-enter Panel ${index + 1} Serial No.",
                testTagPrefix = "serial_panel_${index + 1}",
                canRemove = form.panelSerials.size > 2 && index >= 2,
                onRemove = { onRemovePanel(panel.id) },
                onPrimaryChange = { onUpdatePanelSerial(panel.id, it, null) },
                onConfirmChange = { onUpdatePanelSerial(panel.id, null, it) },
              )
            }

            item {
              OutlinedButton(
                onClick = onAddPanel,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_panel_serial_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BrandNavy),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite, contentColor = BrandNavy),
              ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Add Panel Serial Number",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          // ==================== STEP 3: AFTER-INSTALLATION NOTECAM GEO IMAGES ====================
          else -> {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BannerIceBlue,
                border = BorderStroke(1.dp, BrandNavy.copy(alpha = 0.25f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = BrandNavy,
                    modifier = Modifier.size(26.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "After Installation NoteCam Geo Images",
                      style = MaterialTheme.typography.titleSmall,
                      color = BrandNavy,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Capture photos using NoteCam Camera for live translucent GPS details, or select Gallery photos unmodified. Photos are optional on submit.",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                }
              }
            }

            itemsIndexed(form.afterInstallationPhotos, key = { _, item -> item.id }) { idx, photoItem ->
              AfterInstallationPhotoCard(
                index = idx,
                item = photoItem,
                canDeleteSlot = form.afterInstallationPhotos.size > 1 && idx >= 1,
                isResolvingLocation = resolvingPhotoId == photoItem.id,
                onCaptionChange = { onUpdatePhotoCaption(photoItem.id, it) },
                onCameraClick = { triggerNoteCamCapture(photoItem.id) },
                onGalleryClick = {
                  activePhotoId = photoItem.id
                  galleryPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                onClearImage = { onUpdatePhotoCapture(photoItem.id, null, null, "", false) },
                onDeleteSlot = { onRemovePhotoSlot(photoItem.id) },
              )
            }

            item {
              OutlinedButton(
                onClick = onAddPhotoSlot,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_more_install_photo_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, BrandNavy),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite, contentColor = BrandNavy),
              ) {
                Icon(imageVector = Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Add More Photo",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showEnableGpsDialog) {
    AlertDialog(
      onDismissRequest = { showEnableGpsDialog = false },
      containerColor = CardWhite,
      icon = {
        Icon(
          imageVector = Icons.Outlined.LocationOn,
          contentDescription = null,
          tint = BrandCrimson,
          modifier = Modifier.size(30.dp),
        )
      },
      title = {
        Text(
          text = "Enable GPS Location",
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = "Live device GPS location is unavailable or turned off. Please enable Location Services for NoteCam Geo capture, or use Gallery to upload an image directly.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showEnableGpsDialog = false
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(48.dp),
        ) {
          Text("Location Settings", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showEnableGpsDialog = false },
          modifier = Modifier.height(48.dp),
        ) {
          Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
        }
      },
    )
  }
}

@Composable
private fun StepTabPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(48.dp),
    shape = RoundedCornerShape(10.dp),
    color = if (selected) BrandNavy else BannerIceBlue,
    border = BorderStroke(1.dp, if (selected) BrandNavy else BorderSubtle),
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else BrandNavy,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CollapsibleFormSection(
  title: String,
  summary: String,
  expanded: Boolean,
  onToggle: () -> Unit,
  testTag: String,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = BrandNavy,
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.fillMaxWidth()) {
          HorizontalDivider(color = DividerLight)
          Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            content()
          }
        }
      }
    }
  }
}

@Composable
private fun CompactInlineMaterialRow(
  row: InstallationProductRow,
  onUpdate: (company: String?, size: String?, capacity: String?, qty: String?) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(if (row.isSubRow) Color(0xFFFAFBFD) else CardWhite)
        .padding(vertical = 10.dp, horizontal = 4.dp)
        .testTag("product_row_${row.rowId}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (row.srNoDisplay.isNotBlank()) {
          Box(
            modifier =
              Modifier
                .background(BrandNavy, RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = row.srNoDisplay,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = row.productModel,
          style = MaterialTheme.typography.bodyMedium,
          color = if (row.isSubRow) TextSecondary else BrandNavy,
          fontWeight = if (row.isSubRow) FontWeight.SemiBold else FontWeight.Bold,
        )
      }
      if (row.isQtyRequired) {
        StatusTagChip(
          text = "* Required",
          bgColor = BannerIceBlue,
          textColor = BrandNavy,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CompactInlineInput(
        label = "Company",
        value = row.company,
        onValueChange = { onUpdate(it, null, null, null) },
        modifier = Modifier.weight(1f),
        testTag = "product_${row.rowId}_company",
      )
      CompactInlineInput(
        label = "Size",
        value = row.size,
        onValueChange = { onUpdate(null, it, null, null) },
        modifier = Modifier.weight(1f),
        testTag = "product_${row.rowId}_size",
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CompactInlineInput(
        label = "Capacity",
        value = row.capacity,
        onValueChange = { onUpdate(null, null, it, null) },
        modifier = Modifier.weight(1f),
        testTag = "product_${row.rowId}_capacity",
      )
      CompactInlineInput(
        label = if (row.isQtyRequired) "QTY *" else "QTY",
        value = row.qty,
        onValueChange = { onUpdate(null, null, null, it) },
        modifier = Modifier.weight(1f),
        testTag = "product_${row.rowId}_qty",
      )
    }
  }
}

@Composable
private fun CompactInlineInput(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "",
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
    singleLine = true,
    shape = RoundedCornerShape(10.dp),
    textStyle = MaterialTheme.typography.bodyMedium,
    modifier = modifier.then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    colors = installFieldColors(),
  )
}

@Composable
private fun AfterInstallationPhotoCard(
  index: Int,
  item: InstallationPhotoItem,
  canDeleteSlot: Boolean,
  isResolvingLocation: Boolean,
  onCaptionChange: (String) -> Unit,
  onCameraClick: () -> Unit,
  onGalleryClick: () -> Unit,
  onClearImage: () -> Unit,
  onDeleteSlot: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("install_photo_card_${index + 1}"),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Photo ${index + 1}",
          style = MaterialTheme.typography.titleSmall,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StatusTagChip(
            text = if (item.photoUri != null) "Attached" else "Optional",
            bgColor = if (item.photoUri != null) SuccessGreenBg else WarningAmberBg,
            textColor = if (item.photoUri != null) SuccessGreenText else WarningAmberText,
          )
          if (canDeleteSlot) {
            IconButton(
              onClick = onDeleteSlot,
              modifier = Modifier.size(48.dp).testTag("remove_photo_slot_${index + 1}"),
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Remove Photo Slot",
                tint = BrandCrimson,
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = item.caption,
        onValueChange = onCaptionChange,
        label = { Text("Photo Description / Label") },
        placeholder = { Text("e.g. Solar Panels, Inverter, Earthing Pit", color = TextMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("install_photo_caption_${index + 1}"),
        colors = installFieldColors(),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onCameraClick,
          enabled = !isResolvingLocation,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BrandNavy),
          modifier = Modifier.weight(1f).height(48.dp).testTag("install_photo_camera_${index + 1}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = BrandNavy,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("NoteCam (Geo)", color = BrandNavy, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onGalleryClick,
          enabled = !isResolvingLocation,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.weight(1f).height(48.dp).testTag("install_photo_gallery_${index + 1}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Gallery", color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
            text = "Fetching live GPS location...",
            style = MaterialTheme.typography.bodySmall,
            color = BrandNavy,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      if (item.photoUri != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(BannerIceBlue)
              .testTag("install_photo_preview_${index + 1}"),
        ) {
          RemoteAwarePhotoPreview(
            photoUri = item.photoUri,
            contentDescription = item.caption,
            modifier = Modifier.fillMaxSize(),
            compact = false,
            isMarathi = false,
          )

          IconButton(
            onClick = onClearImage,
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(8.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.45f)),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = "Clear Image",
              tint = Color.White,
            )
          }

          if (item.isCameraGeoStamped && item.geoInfo.isNotBlank()) {
            Box(
              modifier =
                Modifier.align(Alignment.BottomEnd)
                  .padding(8.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color.Black.copy(alpha = 0.45f))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("install_photo_geo_overlay_${index + 1}"),
            ) {
              Text(
                text = item.geoInfo,
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
}

@Composable
private fun DoubleEntrySerialCard(
  title: String,
  field: SerialVerificationField,
  primaryPlaceholder: String,
  confirmPlaceholder: String,
  testTagPrefix: String,
  canRemove: Boolean = false,
  onRemove: () -> Unit = {},
  onPrimaryChange: (String) -> Unit,
  onConfirmChange: (String) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border =
      BorderStroke(
        width = if (field.hasMismatch || field.isMatch) 1.5.dp else 1.dp,
        color =
          when {
            field.hasMismatch -> BrandCrimson
            field.isMatch -> SuccessGreenText
            else -> BorderSubtle
          },
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        if (canRemove) {
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(48.dp).testTag("${testTagPrefix}_remove"),
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
              contentDescription = "Remove Panel",
              tint = BrandCrimson,
            )
          }
        }
      }

      OutlinedTextField(
        value = field.primaryValue,
        onValueChange = onPrimaryChange,
        label = { Text("1st Entry") },
        placeholder = { Text(primaryPlaceholder, color = TextMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth().testTag("${testTagPrefix}_primary"),
        colors = installFieldColors(),
      )

      OutlinedTextField(
        value = field.confirmValue,
        onValueChange = onConfirmChange,
        label = { Text("2nd Entry (Confirm)") },
        placeholder = { Text(confirmPlaceholder, color = TextMuted) },
        singleLine = true,
        isError = field.hasMismatch,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth().testTag("${testTagPrefix}_confirm"),
        colors = installFieldColors(),
      )

      if (field.isMatch) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier =
            Modifier.fillMaxWidth()
              .background(SuccessGreenBg, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = SuccessGreenText,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Matched & Verified",
            style = MaterialTheme.typography.labelSmall,
            color = SuccessGreenText,
            fontWeight = FontWeight.Bold,
          )
        }
      } else if (field.hasMismatch) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier =
            Modifier.fillMaxWidth()
              .background(PendingRoseBg, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = PendingRoseText,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Serial numbers do not match. Re-check both entries.",
            style = MaterialTheme.typography.labelSmall,
            color = PendingRoseText,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun EnglishField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
  testTag: String = "",
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
        Modifier.fillMaxWidth()
          .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
      placeholder = { Text(placeholder, color = TextMuted) },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
      colors = installFieldColors(),
    )
  }
}

@Composable
private fun installFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardWhite,
    unfocusedContainerColor = CardWhite,
    focusedBorderColor = BrandNavy,
    unfocusedBorderColor = BorderSubtle,
  )

private fun createInstallTempImageUri(context: Context, prefix: String): Uri {
  val dir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
  val file = File(dir, "${prefix.lowercase()}_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
