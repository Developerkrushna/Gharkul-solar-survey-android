package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppScreen
import com.example.model.InstallationTab
import com.example.model.LoginRole
import com.example.model.SurveyCategory
import com.example.util.InstallationPdfGenerator
import com.example.viewmodel.SurveyAppViewModel
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [TestPdfDocumentShadow::class])
class SolarInstallationRobolectricTest {

  @Before
  fun resetFileProviderCache() {
    runCatching {
      val field = FileProvider::class.java.getDeclaredField("sCache")
      field.isAccessible = true
      (field.get(null) as? MutableMap<*, *>)?.clear()
    }
  }

  private fun createSamplePhotoUri(context: Context, name: String): String {
    val photoDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
    val file = File(photoDir, name)
    val bmp = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888)
    FileOutputStream(file).use { out ->
      bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    bmp.recycle()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
  }

  @Test
  fun `dual login routes executive to village select and solar installer to english dashboard with tabs`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SurveyAppViewModel(app)

    // Default role is Survey Executive -> VILLAGE_SELECT
    assertEquals(LoginRole.SURVEY_EXECUTIVE, viewModel.uiState.value.loginRole)
    viewModel.submitLogin()
    assertEquals(AppScreen.VILLAGE_SELECT, viewModel.uiState.value.currentScreen)

    // Logout & switch to Solar Installation Login
    viewModel.logout()
    assertEquals(AppScreen.LOGIN, viewModel.uiState.value.currentScreen)

    viewModel.selectLoginRole(LoginRole.SOLAR_INSTALLATION)
    assertEquals("installer@reflectionmedias.in", viewModel.uiState.value.loginEmail)

    // Invalid email shows English error
    viewModel.updateLoginEmail("invalid-email")
    viewModel.submitLogin()
    assertEquals("Please enter a valid email address.", viewModel.uiState.value.loginError)
    assertEquals(AppScreen.LOGIN, viewModel.uiState.value.currentScreen)

    // Valid login routes to INSTALLATION_DASHBOARD on HOME tab with Approved Category 1 queue
    viewModel.updateLoginEmail("installer@reflectionmedias.in")
    viewModel.submitLogin()
    assertEquals(AppScreen.INSTALLATION_DASHBOARD, viewModel.uiState.value.currentScreen)
    assertEquals(InstallationTab.HOME, viewModel.uiState.value.activeInstallationTab)
    assertTrue(viewModel.uiState.value.approvedCategory1Surveys.isNotEmpty())
    assertTrue(
      viewModel.uiState.value.approvedCategory1Surveys.all {
        it.finalCategory == SurveyCategory.CATEGORY_1
      }
    )

    // Bottom bar switching between Profile and Home
    viewModel.selectInstallationTab(InstallationTab.PROFILE)
    assertEquals(InstallationTab.PROFILE, viewModel.uiState.value.activeInstallationTab)
    viewModel.selectInstallationTab(InstallationTab.HOME)
    assertEquals(InstallationTab.HOME, viewModel.uiState.value.activeInstallationTab)
  }

  @Test
  fun `3-step installation form enforces step validation, captures dynamic photos, and generates pdf with or without images`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val context: Context = app
    val viewModel = SurveyAppViewModel(app)

    viewModel.selectLoginRole(LoginRole.SOLAR_INSTALLATION)
    viewModel.submitLogin()

    // Start from an approved Category 1 survey
    val approvedCustomer = viewModel.uiState.value.approvedCategory1Surveys.first()
    viewModel.startInstallationFromSurvey(approvedCustomer)
    assertEquals(AppScreen.INSTALLATION_FORM, viewModel.uiState.value.currentScreen)

    val initialForm = viewModel.uiState.value.installationForm
    assertEquals(1, initialForm.currentPage)
    assertEquals(approvedCustomer.beneficiaryName, initialForm.consumerName)
    assertEquals(approvedCustomer.meterInfo.consumerNumber, initialForm.consumerNo)
    assertEquals(approvedCustomer.mobile, initialForm.mobileNo)
    assertEquals("1 KW", initialForm.systemCapacity)
    assertEquals("ONGRID", initialForm.systemType)
    assertEquals(23, initialForm.products.size)
    assertEquals(2, initialForm.panelSerials.size)
    assertEquals(1, initialForm.afterInstallationPhotos.size)

    // Sample PDF defaults: 12 mentioned QTY items marked required; other fields empty
    val panelRow = initialForm.products.first { it.rowId == "1" }
    assertTrue(panelRow.isQtyRequired)
    assertEquals("02", panelRow.qty)
    assertEquals("", panelRow.company)
    assertEquals("", panelRow.size)
    assertEquals("", panelRow.capacity)
    assertEquals("20 mtr", initialForm.products.first { it.rowId == "2A" }.qty)
    assertEquals("10 mtr", initialForm.products.first { it.rowId == "2B" }.qty)
    assertEquals("08", initialForm.products.first { it.rowId == "7A" }.qty)
    val optionalRow = initialForm.products.first { it.rowId == "14" }
    assertEquals(false, optionalRow.isQtyRequired)
    assertEquals("", optionalRow.qty)

    // Clearing a required sample QTY blocks Step 1 Next
    viewModel.updateInstallationProductRow(rowId = "7A", company = null, size = null, capacity = null, qty = "")
    viewModel.nextInstallationFormPage()
    assertEquals(1, viewModel.uiState.value.installationForm.currentPage)
    assertEquals(
      "Please enter QTY for A) PVC PIPE.",
      viewModel.uiState.value.installationForm.validationError,
    )
    viewModel.updateInstallationProductRow(rowId = "7A", company = null, size = null, capacity = null, qty = "08")

    // Customize product row 1 (PANEL'S)
    viewModel.updateInstallationProductRow(
      rowId = "1",
      company = "Waaree Energies",
      size = "Standard",
      capacity = "550W",
      qty = "02",
    )
    assertEquals(
      "Waaree Energies",
      viewModel.uiState.value.installationForm.products.first { it.rowId == "1" }.company,
    )

    // Step 1 requires signatures before advancing to Step 2
    viewModel.nextInstallationFormPage()
    assertEquals(1, viewModel.uiState.value.installationForm.currentPage)
    assertTrue(
      viewModel.uiState.value.installationForm.validationError?.contains("Signature") == true
    )

    // Complete signatures & verifier names -> advances cleanly to Step 2
    viewModel.updateInstallationSignOff(
      installerName = "Vikram Kulkarni",
      installerSignatureStrokes = listOf(listOf(10f to 10f, 80f to 40f)),
      accountantName = "S. Joshi",
      managerName = "A. Deshmukh",
      supervisorName = "R. Patil",
      customerSignatureStrokes = listOf(listOf(15f to 20f, 95f to 45f)),
    )
    viewModel.nextInstallationFormPage()
    assertEquals(2, viewModel.uiState.value.installationForm.currentPage)
    assertNull(viewModel.uiState.value.installationForm.validationError)

    // Add 3rd panel dynamically -> now 3 panels requiring double entry
    viewModel.addPanelSerialRow()
    val panels = viewModel.uiState.value.installationForm.panelSerials
    assertEquals(3, panels.size)

    // Fill Invertor & Meter & Panels 1-3, intentionally mismatching Panel 3
    viewModel.updateInverterSerial("INV-2026-001", "INV-2026-001")
    viewModel.updateGenerationMeterSerial("GM-884422", "GM-884422")
    viewModel.updatePanelSerial(panels[0].id, "PNL-1001", "PNL-1001")
    viewModel.updatePanelSerial(panels[1].id, "PNL-1002", "PNL-1002")
    viewModel.updatePanelSerial(panels[2].id, "PNL-1003", "PNL-9999")

    assertTrue(viewModel.uiState.value.installationForm.inverterSerial.isMatch)
    assertTrue(viewModel.uiState.value.installationForm.panelSerials[2].serial.hasMismatch)

    // Attempt Step 2 -> Step 3 blocked by Panel 3 mismatch
    viewModel.nextInstallationFormPage()
    assertEquals(2, viewModel.uiState.value.installationForm.currentPage)
    assertEquals(
      "Panel 3 Serial Number entries do not match. Please verify both fields.",
      viewModel.uiState.value.installationForm.validationError,
    )

    // Fix Panel 3 confirmation -> advances cleanly to Step 3 (After Installation NoteCam Images)
    viewModel.updatePanelSerial(panels[2].id, confirm = "PNL-1003")
    viewModel.nextInstallationFormPage()
    assertEquals(3, viewModel.uiState.value.installationForm.currentPage)
    assertNull(viewModel.uiState.value.installationForm.validationError)

    // Cannot remove the only default photo slot
    val firstPhotoId = viewModel.uiState.value.installationForm.afterInstallationPhotos.first().id
    viewModel.removeInstallationPhotoSlot(firstPhotoId)
    assertEquals(1, viewModel.uiState.value.installationForm.afterInstallationPhotos.size)

    // Add 2nd photo slot dynamically and attach sample photos (1 GPS NoteCam, 1 Gallery)
    viewModel.addInstallationPhotoSlot()
    val photos = viewModel.uiState.value.installationForm.afterInstallationPhotos
    assertEquals(2, photos.size)

    val uri1 = createSamplePhotoUri(context, "site_after_1.jpg")
    val uri2 = createSamplePhotoUri(context, "site_after_2.jpg")
    viewModel.updateInstallationPhotoCaption(photos[0].id, "Rooftop Panel Array & Inverter")
    viewModel.updateInstallationPhotoCapture(
      photoId = photos[0].id,
      photoUri = uri1,
      photoSizeKb = 240,
      geoInfo = "19.87620° N, 75.34330° E · Newasa",
      isCameraGeoStamped = true,
    )
    viewModel.updateInstallationPhotoCaption(photos[1].id, "Generation Meter & Earthing Pit")
    viewModel.updateInstallationPhotoCapture(
      photoId = photos[1].id,
      photoUri = uri2,
      photoSizeKb = 195,
      geoInfo = "",
      isCameraGeoStamped = false,
    )

    // Submit form -> saves report & navigates back to HOME dashboard
    val saved = viewModel.saveInstallationReport()
    assertNotNull(saved)
    assertEquals(AppScreen.INSTALLATION_DASHBOARD, viewModel.uiState.value.currentScreen)
    assertEquals(InstallationTab.HOME, viewModel.uiState.value.activeInstallationTab)
    assertEquals(1, viewModel.uiState.value.installationReports.size)
    assertEquals("S. Joshi", saved!!.accountantName)
    assertEquals("PNL-1003", saved.panelSerials[2].serial.trimmedPrimary)
    assertEquals(2, saved.afterInstallationPhotos.count { !it.photoUri.isNullOrBlank() })

    // Generate PDF WITHOUT images -> 2 pages (Certificate + Serial Numbers)
    val withoutImagesUri = InstallationPdfGenerator.generateCertificatePdf(context, saved, includeImages = false)
    assertNotNull(withoutImagesUri)
    val withoutImagesBytes = context.contentResolver.openInputStream(withoutImagesUri!!)?.use { it.readBytes() }
    assertNotNull(withoutImagesBytes)
    val withoutImagesText = String(withoutImagesBytes!!)
    assertTrue(withoutImagesText.startsWith("%PDF-"))
    assertTrue(withoutImagesText.contains("2 pages generated"))

    // Generate PDF WITH images -> 3 pages (2 certificate pages + 1 photo page holding 2 photo cards)
    val withImagesUri = InstallationPdfGenerator.generateCertificatePdf(context, saved, includeImages = true)
    assertNotNull(withImagesUri)
    val withImagesBytes = context.contentResolver.openInputStream(withImagesUri!!)?.use { it.readBytes() }
    assertNotNull(withImagesBytes)
    val withImagesText = String(withImagesBytes!!)
    assertTrue(withImagesText.startsWith("%PDF-"))
    assertTrue(withImagesText.contains("3 pages generated"))
  }

  @Test
  fun `manual installation entry creates standalone certificate with optional step 3 photos`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SurveyAppViewModel(app)

    viewModel.selectLoginRole(LoginRole.SOLAR_INSTALLATION)
    viewModel.submitLogin()
    viewModel.startManualInstallationForm()

    assertNull(viewModel.uiState.value.installationForm.linkedSurveyId)
    viewModel.updateInstallationHeader(
      consumerNo = "445566778899",
      consumerName = "Mahesh Shinde",
      mobileNo = "9876543210",
    )
    viewModel.updateInstallationSignOff(
      installerSignatureStrokes = listOf(listOf(5f to 5f, 40f to 35f)),
      customerSignatureStrokes = listOf(listOf(10f to 15f, 55f to 40f)),
    )
    viewModel.nextInstallationFormPage()
    assertEquals(2, viewModel.uiState.value.installationForm.currentPage)

    val panels = viewModel.uiState.value.installationForm.panelSerials
    viewModel.updateInverterSerial("INV-M-10", "INV-M-10")
    viewModel.updateGenerationMeterSerial("GM-M-20", "GM-M-20")
    viewModel.updatePanelSerial(panels[0].id, "P-M-1", "P-M-1")
    viewModel.updatePanelSerial(panels[1].id, "P-M-2", "P-M-2")

    viewModel.nextInstallationFormPage()
    assertEquals(3, viewModel.uiState.value.installationForm.currentPage)

    // Save with no attached photos on Step 3 (photos optional)
    val saved = viewModel.saveInstallationReport()
    assertNotNull(saved)
    assertNull(saved!!.linkedSurveyId)
    assertEquals("Mahesh Shinde", saved.consumerName)
    assertEquals(1, viewModel.uiState.value.installationReports.size)
  }
}
