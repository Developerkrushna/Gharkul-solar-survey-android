package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SolarPower
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CustomerSurvey
import com.example.model.ExecutiveUser
import com.example.model.InstallationQueueFilter
import com.example.model.InstallationReport
import com.example.model.InstallationTab
import com.example.ui.components.BrandTopBar
import com.example.ui.components.CircularSyncTopBarButton
import com.example.ui.components.InstallationBottomBar
import com.example.ui.components.StatusTagChip
import com.example.ui.components.SyncErrorRetryBanner
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import com.example.util.InstallationPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationDashboardScreen(
  activeTab: InstallationTab,
  executive: ExecutiveUser,
  approvedCat1Surveys: List<CustomerSurvey>,
  installationReports: List<InstallationReport>,
  searchQuery: String,
  selectedFilter: InstallationQueueFilter,
  snackbarHostState: SnackbarHostState,
  isCloudConfigured: Boolean = false,
  isSyncing: Boolean = false,
  pendingSyncCount: Int = 0,
  syncErrorMessage: String? = null,
  onSyncClick: () -> Unit = {},
  onSelectTab: (InstallationTab) -> Unit,
  onSearchChange: (String) -> Unit,
  onFilterSelect: (InstallationQueueFilter) -> Unit,
  onStartFromSurvey: (CustomerSurvey) -> Unit,
  onStartManual: () -> Unit,
  onEditReport: (InstallationReport) -> Unit,
  onShowMessage: (String) -> Unit,
  onLogoutClick: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var shareTargetReport by remember { mutableStateOf<InstallationReport?>(null) }

  if (activeTab != InstallationTab.HOME) {
    BackHandler { onSelectTab(InstallationTab.HOME) }
  }

  val reportBySurveyId =
    installationReports.filter { it.linkedSurveyId != null }.associateBy { it.linkedSurveyId!! }
  val pendingSurveysAll = approvedCat1Surveys.filterNot { reportBySurveyId.containsKey(it.id) }
  val pendingCount = pendingSurveysAll.size
  val totalCompletedCount = installationReports.size

  val q = searchQuery.trim()
  val filteredPendingSurveys =
    pendingSurveysAll.filter { survey ->
      q.isBlank() ||
        survey.beneficiaryName.contains(q, ignoreCase = true) ||
        survey.meterInfo.consumerNumber.contains(q, ignoreCase = true) ||
        survey.mobile.contains(q, ignoreCase = true) ||
        survey.villageNameMr.contains(q, ignoreCase = true) ||
        survey.talukaNameMr.contains(q, ignoreCase = true) ||
        survey.districtNameMr.contains(q, ignoreCase = true)
    }

  val filteredReports =
    installationReports.filter { report ->
      q.isBlank() ||
        report.consumerName.contains(q, ignoreCase = true) ||
        report.consumerNo.contains(q, ignoreCase = true) ||
        report.mobileNo.contains(q, ignoreCase = true) ||
        report.villageName.contains(q, ignoreCase = true)
    }

  fun launchPdfShare(report: InstallationReport, includeImages: Boolean) {
    scope.launch {
      onShowMessage("इन्स्टॉलेशन सर्टिफिकेट PDF तयार होत आहे...")
      val pdfUri =
        withContext(Dispatchers.IO) {
          InstallationPdfGenerator.generateCertificatePdf(context, report, includeImages)
        }
      if (pdfUri != null) {
        val shareIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(
              Intent.EXTRA_SUBJECT,
              "Installation Certificate - ${report.consumerName} (${report.consumerNo})",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
        context.startActivity(Intent.createChooser(shareIntent, "Share Installation Certificate PDF"))
      } else {
        onShowMessage("PDF तयार करताना त्रुटी आली. कृपया पुन्हा प्रयत्न करा.")
      }
    }
  }

  Scaffold(
    containerColor = SurfaceCanvas,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      BrandTopBar(
        title =
          if (activeTab == InstallationTab.HOME) "सोलर इन्स्टॉलेशन पोर्टल"
          else "इन्स्टॉलर प्रोफाइल",
        subtitle = "Reflection Media · ${executive.nameMr}",
        trailingAction = {
          CircularSyncTopBarButton(
            isSyncing = isSyncing,
            pendingSyncCount = pendingSyncCount,
            onClick = onSyncClick,
            testTag = "installation_sync_button",
            contentDescription = "डेटा सिंक व रिफ्रेश करा",
          )
        },
      )
    },
    bottomBar = {
      InstallationBottomBar(
        activeTab = activeTab,
        onSelectTab = onSelectTab,
        onAddManualClick = onStartManual,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (activeTab == InstallationTab.PROFILE) {
        InstallationProfileContent(
          executive = executive,
          pendingCount = pendingCount,
          completedCount = totalCompletedCount,
          onLogoutClick = onLogoutClick,
        )
      } else {
        PullToRefreshBox(
          isRefreshing = isSyncing,
          onRefresh = { if (isCloudConfigured) onSyncClick() },
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (!syncErrorMessage.isNullOrBlank()) {
              item {
                SyncErrorRetryBanner(
                  errorMessage = syncErrorMessage,
                  isSyncing = isSyncing,
                  onRetry = onSyncClick,
                )
              }
            }

            // Two-tab Marathi switcher: 1. इन्स्टॉलेशन बाकी ग्राहक | 2. माझे पूर्ण इन्स्टॉलेशन
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, BorderSubtle),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(6.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  InstallationHomeTabButton(
                    label = InstallationQueueFilter.PENDING.labelMr,
                    count = pendingCount,
                    selected = selectedFilter == InstallationQueueFilter.PENDING,
                    accentBg = WarningAmberBg,
                    accentText = WarningAmberText,
                    onClick = { onFilterSelect(InstallationQueueFilter.PENDING) },
                    modifier = Modifier.weight(1f).testTag("install_tab_pending"),
                  )
                  InstallationHomeTabButton(
                    label = InstallationQueueFilter.COMPLETED.labelMr,
                    count = totalCompletedCount,
                    selected = selectedFilter == InstallationQueueFilter.COMPLETED,
                    accentBg = SuccessGreenBg,
                    accentText = SuccessGreenText,
                    onClick = { onFilterSelect(InstallationQueueFilter.COMPLETED) },
                    modifier = Modifier.weight(1f).testTag("install_tab_completed"),
                  )
                }
              }
            }

            item {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().testTag("installation_search_input"),
                placeholder = { Text("नाव, गाव, ग्राहक क्र. किंवा मोबाईल शोधा", color = TextMuted) },
                leadingIcon = {
                  Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = BrandNavy,
                    unfocusedBorderColor = BorderSubtle,
                  ),
              )
            }

            if (selectedFilter == InstallationQueueFilter.PENDING) {
              if (filteredPendingSurveys.isNotEmpty()) {
                item {
                  Text(
                    text = "इन्स्टॉलेशन बाकी ग्राहक (${filteredPendingSurveys.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                  )
                }
                items(filteredPendingSurveys, key = { it.id }) { survey ->
                  PendingInstallationSurveyCard(
                    survey = survey,
                    onFillFormClick = { onStartFromSurvey(survey) },
                  )
                }
              } else {
                item {
                  EmptyInstallationQueueCard(
                    title = "इन्स्टॉलेशन बाकी ग्राहक नाहीत",
                    subtitle =
                      "ऑफिस अ‍ॅडमिनने श्रेणी १ (Category 1) ग्राहकांचा सरकारी फॉर्म पूर्ण केल्यावर ते येथे दिसतील. नवीन यादीसाठी खाली ओढा (Pull to Refresh).",
                  )
                }
              }
            } else {
              if (filteredReports.isNotEmpty()) {
                item {
                  Text(
                    text = "माझे पूर्ण इन्स्टॉलेशन (${filteredReports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                  )
                }
                items(filteredReports, key = { it.id }) { report ->
                  CompletedInstallationCard(
                    report = report,
                    onEditClick = { onEditReport(report) },
                    onSharePdfClick = { shareTargetReport = report },
                  )
                }
              } else {
                item {
                  EmptyInstallationQueueCard(
                    title = "अद्याप पूर्ण इन्स्टॉलेशन नोंद नाही",
                    subtitle =
                      "'इन्स्टॉलेशन बाकी ग्राहक' टॅबमधून ग्राहक निवडा किंवा खालील '+' बटणाने नवीन इन्स्टॉलेशन सर्टिफिकेट भरा.",
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  val targetToShare = shareTargetReport
  if (targetToShare != null) {
    ShareInstallationPdfOptionDialog(
      report = targetToShare,
      onDismiss = { shareTargetReport = null },
      onShareWithImages = {
        shareTargetReport = null
        launchPdfShare(targetToShare, includeImages = true)
      },
      onShareWithoutImages = {
        shareTargetReport = null
        launchPdfShare(targetToShare, includeImages = false)
      },
    )
  }
}

@Composable
private fun InstallationHomeTabButton(
  label: String,
  count: Int,
  selected: Boolean,
  accentBg: Color,
  accentText: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(56.dp),
    shape = RoundedCornerShape(10.dp),
    color = if (selected) BrandNavy else BannerIceBlue.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, if (selected) BrandNavy else Color.Transparent),
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) Color.White else BrandNavy,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        Box(
          modifier =
            Modifier
              .background(
                color = if (selected) Color.White.copy(alpha = 0.2f) else accentBg,
                shape = RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else accentText,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyInstallationQueueCard(
  title: String,
  subtitle: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.SolarPower,
        contentDescription = null,
        tint = BrandNavy,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun ShareInstallationPdfOptionDialog(
  report: InstallationReport,
  onDismiss: () -> Unit,
  onShareWithImages: () -> Unit,
  onShareWithoutImages: () -> Unit,
  isPostSave: Boolean = false,
) {
  val attachedCount = report.afterInstallationPhotos.count { !it.photoUri.isNullOrBlank() }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardWhite,
    icon = {
      Icon(
        imageVector = if (isPostSave) Icons.Outlined.CheckCircle else Icons.Outlined.PictureAsPdf,
        contentDescription = null,
        tint = if (isPostSave) SuccessGreenText else BrandNavy,
        modifier = Modifier.size(34.dp),
      )
    },
    title = {
      Text(
        text =
          if (isPostSave) "Certificate Saved — Share PDF"
          else "Share Installation Certificate PDF",
        style = MaterialTheme.typography.titleLarge,
        color = BrandNavy,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Choose how you want to export the Completion Certificate for ${report.consumerName} (${report.consumerNo}):",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )

        Button(
          onClick = onShareWithImages,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pdf_share_with_images_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson, contentColor = Color.White),
        ) {
          Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "With Images ($attachedCount Photos)",
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = onShareWithoutImages,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pdf_share_without_images_btn"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BrandNavy),
        ) {
          Icon(imageVector = Icons.Outlined.Description, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Without Images (2-Page Certificate)",
            color = BrandNavy,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text(if (isPostSave) "Done" else "Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
      }
    },
  )
}

@Composable
private fun InstallationProfileContent(
  executive: ExecutiveUser,
  pendingCount: Int,
  completedCount: Int,
  onLogoutClick: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(BannerIceBlue),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Person,
              contentDescription = "इन्स्टॉलर प्रोफाइल",
              tint = BrandNavy,
              modifier = Modifier.size(30.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = executive.nameMr,
              style = MaterialTheme.typography.titleLarge,
              color = BrandNavy,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = executive.roleMr,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Text(
              text = "${executive.email} · ${executive.mobile}",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        InstallKpiCard(
          label = "इन्स्टॉलेशन बाकी",
          value = pendingCount.toString(),
          bgColor = WarningAmberBg,
          accentColor = WarningAmberText,
          modifier = Modifier.weight(1f),
        )
        InstallKpiCard(
          label = "माझे पूर्ण रिपोर्ट",
          value = completedCount.toString(),
          bgColor = SuccessGreenBg,
          accentColor = SuccessGreenText,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BannerIceBlue,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Outlined.Business,
            contentDescription = "Corporate Office",
            tint = BrandNavy,
            modifier = Modifier.padding(top = 2.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "REFLECTION MEDIA — CORPORATE OFFICE",
              style = MaterialTheme.typography.labelMedium,
              color = BrandNavy,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Near Bus Stand, Newasa, Dist - Ahilyanagar, PIN - 414603",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
            )
            Text(
              text = "Customer Care: 7558723283 / 7283838383 · support@reflectionmedias.in",
              style = MaterialTheme.typography.bodySmall,
              color = BrandNavy,
            )
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onLogoutClick,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("installation_logout_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandCrimson),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandCrimson),
      ) {
        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "लॉगआउट करा",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun InstallKpiCard(
  label: String,
  value: String,
  bgColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(
        modifier =
          Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = BrandNavy,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun PendingInstallationSurveyCard(
  survey: CustomerSurvey,
  onFillFormClick: () -> Unit,
) {
  val locationLine =
    listOf(survey.villageNameMr, survey.talukaNameMr, survey.districtNameMr)
      .filter { it.isNotBlank() }
      .joinToString(" · ")

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("pending_install_card_${survey.id}"),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = survey.beneficiaryName,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
          )
          if (locationLine.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = locationLine,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        StatusTagChip(
          text = if (survey.govFormCompleted) "फॉर्म पूर्ण" else "श्रेणी १ · तयार",
          bgColor = if (survey.govFormCompleted) SuccessGreenBg else WarningAmberBg,
          textColor = if (survey.govFormCompleted) SuccessGreenText else WarningAmberText,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "मोबाईल: ${survey.mobile}",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
        )
        Text(
          text = "ग्राहक क्र.: ${survey.meterInfo.consumerNumber.ifBlank { "-" }}",
          style = MaterialTheme.typography.bodySmall,
          color = BrandNavy,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Button(
        onClick = onFillFormClick,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_install_btn_${survey.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
      ) {
        Icon(imageVector = Icons.Outlined.SolarPower, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("इन्स्टॉलेशन फॉर्म भरा", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun CompletedInstallationCard(
  report: InstallationReport,
  onEditClick: () -> Unit,
  onSharePdfClick: () -> Unit,
) {
  val photoCount = report.afterInstallationPhotos.count { !it.photoUri.isNullOrBlank() }

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("completed_install_card_${report.id}"),
    shape = RoundedCornerShape(16.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, SuccessGreenText.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = report.consumerName,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              listOfNotNull(
                report.villageName.takeIf { it.isNotBlank() },
                "Consumer No: ${report.consumerNo}",
                report.installationDate.takeIf { it.isNotBlank() },
              ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
        StatusTagChip(
          text = "पूर्ण",
          bgColor = SuccessGreenBg,
          textColor = SuccessGreenText,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${report.systemCapacity} ${report.systemType} · ${report.panelSerials.size} Panels · $photoCount Photos",
          style = MaterialTheme.typography.bodySmall,
          color = BrandNavy,
          fontWeight = FontWeight.SemiBold,
        )
        Icon(
          imageVector = Icons.Outlined.CheckCircle,
          contentDescription = "Verified",
          tint = SuccessGreenText,
          modifier = Modifier.size(18.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onEditClick,
          modifier = Modifier.weight(1f).height(48.dp).testTag("edit_install_btn_${report.id}"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, BrandNavy),
        ) {
          Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("संपादित करा", color = BrandNavy, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onSharePdfClick,
          modifier = Modifier.weight(1.3f).height(48.dp).testTag("share_install_pdf_${report.id}"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson, contentColor = Color.White),
        ) {
          Icon(imageVector = Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("PDF शेअर करा", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
