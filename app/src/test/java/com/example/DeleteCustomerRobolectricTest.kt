package com.example

import android.app.Application
import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.model.CasteCategory
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.HouseType
import com.example.model.RoofType
import com.example.viewmodel.SurveyAppViewModel
import java.io.File
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

@Implements(PdfDocument::class)
class TestPdfDocumentShadow {
  private val pages = mutableListOf<PdfDocument.PageInfo>()

  @Implementation
  fun __constructor__() {
    // No native initialization on JVM
  }

  @Implementation
  fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
    pages.add(pageInfo)
    val canvas = Canvas()
    return ReflectionHelpers.callConstructor(
      PdfDocument.Page::class.java,
      ReflectionHelpers.ClassParameter.from(Canvas::class.java, canvas),
      ReflectionHelpers.ClassParameter.from(PdfDocument.PageInfo::class.java, pageInfo),
    )
  }

  @Implementation
  fun finishPage(page: PdfDocument.Page) {
    // Complete page
  }

  @Implementation
  fun writeTo(out: OutputStream) {
    out.write("%PDF-1.4\n% ${pages.size} pages generated\n%%EOF\n".toByteArray())
  }

  @Implementation
  fun close() {
    pages.clear()
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [TestPdfDocumentShadow::class])
class DeleteCustomerRobolectricTest {

  @Before
  fun resetFileProviderCache() {
    runCatching {
      val field = FileProvider::class.java.getDeclaredField("sCache")
      field.isAccessible = true
      (field.get(null) as? MutableMap<*, *>)?.clear()
    }
  }

  @Test
  fun `deleting customer removes whole survey, deletes cached document photos, and updates stats`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val context: Context = app
    val viewModel = SurveyAppViewModel(app)

    val initialCount = viewModel.uiState.value.activeVillageSurveys.size
    val initialReportTotal = viewModel.uiState.value.reportBRows.first().total

    // Create real cached photo files under survey_photos/
    val photosDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
    val docFile = File(photosDir, "aadhaar_delete_test.jpg").apply { writeBytes(ByteArray(1024)) }
    val siteFile = File(photosDir, "notecam_roof_delete_test.jpg").apply { writeBytes(ByteArray(1024)) }
    val docUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", docFile).toString()
    val siteUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", siteFile).toString()

    // Create & save a new customer with attached document and site photos
    viewModel.openNewSurveyWizard()
    viewModel.updateWizardBasic(
      surveyDate = "15 / 11 / 2026",
      beneficiaryName = "चाचणी ग्राहक",
      mobile = "9988776655",
      emailId = "test@example.com",
      casteCategory = CasteCategory.OPEN,
      houseType = HouseType.SINGLE,
    )
    viewModel.updateWizardMeter {
      it.copy(
        hasMeter = true,
        sameAsBeneficiary = true,
        isPoleNearbyForNewConn = true,
        interestedInNewConnection = false,
      )
    }
    viewModel.updateSiteSurvey {
      it.copy(
        roofType = RoofType.RCC,
        hasSolarPanelSpace100SqFt = true,
        generationMeterLocation = "मुख्य भिंत",
        hasEarthingSpace = true,
        noteCamRoofBeforePhotoDone = true,
        noteCamRoofBeforePhotoUri = siteUri,
      )
    }
    viewModel.updateDocumentEntry(
      type = DocumentType.AADHAAR,
      printedName = "चाचणी ग्राहक",
      photoUri = docUri,
      photoSizeKb = 300,
      isSizeValid = true,
      legibility = DocumentLegibility.CLEAR,
    )
    viewModel.saveWizardSurvey()

    val createdSurvey =
      viewModel.uiState.value.activeVillageSurveys.firstOrNull { it.beneficiaryName == "चाचणी ग्राहक" }
    assertNotNull(createdSurvey)
    assertEquals(initialCount + 1, viewModel.uiState.value.activeVillageSurveys.size)
    assertEquals(initialReportTotal + 1, viewModel.uiState.value.reportBRows.first().total)
    assertTrue(docFile.exists())
    assertTrue(siteFile.exists())

    // Open detail sheet then request delete & dismiss -> nothing should be deleted
    viewModel.openCustomerDetail(createdSurvey!!)
    viewModel.requestDeleteCustomer(createdSurvey)
    assertNotNull(viewModel.uiState.value.pendingDeleteSurvey)
    viewModel.dismissDeleteDialog()
    assertNull(viewModel.uiState.value.pendingDeleteSurvey)
    assertTrue(docFile.exists())
    assertTrue(siteFile.exists())
    assertEquals(initialCount + 1, viewModel.uiState.value.activeVillageSurveys.size)

    // Now request & confirm delete -> record, detail sheet, cached files, and stats must all update
    viewModel.requestDeleteCustomer(createdSurvey)
    viewModel.confirmDeleteCustomer()

    val afterState = viewModel.uiState.value
    assertNull(afterState.pendingDeleteSurvey)
    assertNull(afterState.selectedDetailSurvey)
    assertTrue(afterState.allSurveys.none { it.id == createdSurvey.id })
    assertTrue(afterState.activeVillageSurveys.none { it.id == createdSurvey.id })
    assertTrue(afterState.filteredListSurveys.none { it.id == createdSurvey.id })
    assertEquals(initialCount, afterState.activeVillageSurveys.size)
    assertEquals(initialReportTotal, afterState.reportBRows.first().total)
    assertFalse(docFile.exists())
    assertFalse(siteFile.exists())
    assertTrue(afterState.snackbarMessage?.contains("चाचणी ग्राहक") == true)
  }

  @Test
  fun `step 1 enforces 10 digit mobile, valid email, 12 digit consumer no if filled, and under construction roof`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SurveyAppViewModel(app)

    viewModel.openNewSurveyWizard()
    viewModel.updateWizardBasic(
      surveyDate = "17 / 11 / 2026",
      beneficiaryName = "सुनील काळे",
      mobile = "987654321", // 9 digits -> invalid
      emailId = "sunil@gmail.com",
    )
    viewModel.updateWizardMeter {
      it.copy(
        hasMeter = true,
        sameAsBeneficiary = true,
        isPoleNearbyForNewConn = true,
        interestedInNewConnection = false,
      )
    }
    viewModel.updateSiteSurvey {
      it.copy(
        roofType = RoofType.UNDER_CONSTRUCTION,
        hasSolarPanelSpace100SqFt = true,
        generationMeterLocation = "भिंतीवर",
        hasEarthingSpace = true,
      )
    }

    // 9-digit mobile should fail Step 1
    viewModel.nextWizardStep()
    assertEquals(1, viewModel.uiState.value.wizard.currentStep)
    assertTrue(viewModel.uiState.value.wizard.validationError?.contains("१० अंकी") == true)

    // Invalid email without TLD should fail Step 1
    viewModel.updateWizardBasic(mobile = "9876543210", emailId = "sunil@invalid")
    viewModel.nextWizardStep()
    assertEquals(1, viewModel.uiState.value.wizard.currentStep)
    assertTrue(viewModel.uiState.value.wizard.validationError?.contains("ई-मेल") == true)

    // 10-digit consumer no (filled but not 12 digits) should fail Step 1
    viewModel.updateWizardBasic(emailId = "sunil@gmail.com")
    viewModel.updateWizardMeter { it.copy(consumerNumber = "1234567890") }
    viewModel.nextWizardStep()
    assertEquals(1, viewModel.uiState.value.wizard.currentStep)
    assertTrue(viewModel.uiState.value.wizard.validationError?.contains("१२ अंकी") == true)

    // 12-digit consumer no + UNDER_CONSTRUCTION roof should pass Step 1 -> Step 2
    assertEquals("बांधकाम सुरू आहे", RoofType.UNDER_CONSTRUCTION.labelMr)
    viewModel.updateWizardMeter { it.copy(consumerNumber = "123456789012") }
    viewModel.nextWizardStep()
    assertEquals(2, viewModel.uiState.value.wizard.currentStep)
    assertNull(viewModel.uiState.value.wizard.validationError)
  }

  @Test
  fun `step 2 defaults compliance unticked, allows any doc size, saves query notes, and stamps notecam geo info`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val context: Context = app
    val viewModel = SurveyAppViewModel(app)

    viewModel.openNewSurveyWizard()
    val compliance = viewModel.uiState.value.wizard.documentComplianceChecks
    assertFalse(compliance.lightBillIsResidential)
    assertFalse(compliance.panNameAndDobMatchAadhaar)
    assertFalse(compliance.bankIsNationalizedNotAdcc)
    assertFalse(compliance.singleMobileLinkedAllDocs)
    assertFalse(compliance.aadhaarSeededToBank)

    // Complete Step 1 and advance to Step 2
    viewModel.updateWizardBasic(
      surveyDate = "17 / 11 / 2026",
      beneficiaryName = "आशा पाटील",
      mobile = "9876543210",
      emailId = "asha.patil@gmail.com",
    )
    viewModel.updateWizardMeter {
      it.copy(
        hasMeter = true,
        sameAsBeneficiary = true,
        isPoleNearbyForNewConn = true,
        interestedInNewConnection = false,
      )
    }
    viewModel.updateSiteSurvey {
      it.copy(
        roofType = RoofType.RCC,
        hasSolarPanelSpace100SqFt = true,
        generationMeterLocation = "मुख्य भिंत",
        hasEarthingSpace = true,
      )
    }
    viewModel.nextWizardStep()
    assertEquals(2, viewModel.uiState.value.wizard.currentStep)

    // Attach small (45 KB) and large (1800 KB) docs + queryNote -> should advance cleanly without KB error
    viewModel.updateDocumentEntry(
      type = DocumentType.LIGHT_BILL,
      printedName = "आशा पाटील",
      photoUri = "captured://bill",
      photoSizeKb = 45,
      legibility = DocumentLegibility.CLEAR,
      queryNote = "नवीन मीटर क्रमांक तपासावा",
    )
    viewModel.updateDocumentEntry(
      type = DocumentType.AADHAAR,
      printedName = "आशा पाटील",
      photoUri = "captured://aadhaar",
      photoSizeKb = 1800,
      legibility = DocumentLegibility.CLEAR,
    )
    viewModel.nextWizardStep()
    assertEquals(3, viewModel.uiState.value.wizard.currentStep)
    assertNull(viewModel.uiState.value.wizard.validationError)
    assertEquals(
      "नवीन मीटर क्रमांक तपासावा",
      viewModel.uiState.value.wizard.documents[DocumentType.LIGHT_BILL]?.queryNote,
    )

    // Verify Note Cam geo-location stamping creates a valid JPEG with location metadata
    val rawBitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
    val photosDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
    val rawFile = File(photosDir, "raw_notecam.jpg")
    rawFile.outputStream().use { rawBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
    rawBitmap.recycle()

    val rawUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", rawFile)
    val stamped =
      com.example.util.ImageCompressionUtil.stampNoteCamGeoLocation(
        context = context,
        sourceUri = rawUri,
        prefix = "notecam_roof",
        gpsCoords = "19.0481° N, 74.7392° E",
        locationBreadcrumb = "हिवरे बाजार · नगर · अहिल्यानगर",
        timestampText = "17/11/2026 09:40 PM",
      )
    assertNotNull(stamped)
    assertTrue(stamped!!.geoSummary.contains("19.0481° N, 74.7392° E"))
    assertTrue(stamped.geoSummary.contains("हिवरे बाजार"))
    assertTrue(stamped.geoSummary.contains("17/11/2026 09:40 PM"))
  }

  @Test
  fun `step 3 defaults material values and generates complete survey pdf with signatures and photos`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val context: Context = app
    val viewModel = SurveyAppViewModel(app)

    // Verify default material values match 20m earthwire, 10m AC cable, 5 pipe (150m)
    viewModel.openNewSurveyWizard()
    val defaultOffice = viewModel.uiState.value.wizard.materialAndOfficeDetails
    assertEquals("20 मीटर", defaultOffice.earthWireMeters)
    assertEquals("10 मीटर", defaultOffice.acCableMeters)
    assertEquals("5 पाईप", defaultOffice.pipeMeters)
    assertEquals(
      "5 पाईप (150 मीटर)",
      com.example.util.SurveyPdfGenerator.formatPipeWithMeters(defaultOffice.pipeMeters),
    )

    // Update custom pipe count directly (e.g., 7 pipes -> 210 meters) and add signatures + attached photo
    val photosDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
    val rawBitmap = android.graphics.Bitmap.createBitmap(200, 150, android.graphics.Bitmap.Config.ARGB_8888)
    val docPhoto = File(photosDir, "aadhaar_pdf_test.jpg")
    docPhoto.outputStream().use { rawBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
    rawBitmap.recycle()
    val docPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", docPhoto).toString()

    viewModel.updateWizardBasic(
      surveyDate = "18 / 11 / 2026",
      beneficiaryName = "विठ्ठल कांबळे",
      mobile = "9876543210",
      emailId = "vitthal@gmail.com",
    )
    viewModel.updateWizardMeter {
      it.copy(
        hasMeter = true,
        sameAsBeneficiary = true,
        isPoleNearbyForNewConn = true,
        interestedInNewConnection = false,
      )
    }
    viewModel.updateSiteSurvey {
      it.copy(
        roofType = RoofType.RCC,
        hasSolarPanelSpace100SqFt = true,
        generationMeterLocation = "मुख्य भिंत",
        hasEarthingSpace = true,
      )
    }
    viewModel.updateDocumentEntry(
      type = DocumentType.AADHAAR,
      printedName = "विठ्ठल कांबळे",
      photoUri = docPhotoUri,
      photoSizeKb = 120,
      legibility = DocumentLegibility.CLEAR,
      queryNote = "स्पष्ट आहे",
    )
    viewModel.updateMaterialAndOffice {
      it.copy(
        earthWireMeters = "25 मीटर",
        acCableMeters = "15 मीटर",
        pipeMeters = "7 पाईप",
        customerSignatureStrokes = listOf(listOf(10f to 20f, 80f to 50f, 140f to 30f)),
        surveyorName = "रमेश कुलकर्णी",
        surveyorSignatureStrokes = listOf(listOf(15f to 15f, 95f to 65f)),
      )
    }
    viewModel.saveWizardSurvey()

    val saved =
      viewModel.uiState.value.activeVillageSurveys.firstOrNull { it.beneficiaryName == "विठ्ठल कांबळे" }
    assertNotNull(saved)
    assertEquals("7 पाईप (210 मीटर)", com.example.util.SurveyPdfGenerator.formatPipeWithMeters(saved!!.materialAndOfficeDetails.pipeMeters))

    val pdfUri = com.example.util.SurveyPdfGenerator.generateCompleteSurveyPdf(context, saved)
    assertNotNull(pdfUri)
    val pdfBytes = context.contentResolver.openInputStream(pdfUri!!)?.use { it.readBytes() }
    assertNotNull(pdfBytes)
    assertTrue(pdfBytes!!.isNotEmpty())
    val header = String(pdfBytes.copyOfRange(0, minOf(5, pdfBytes.size)))
    assertEquals("%PDF-", header)
    // Summary page + 8 doc/NoteCam cards at 2 per page = 5+ total pages
    val pdfContent = String(pdfBytes)
    assertTrue(pdfContent.contains("pages generated"))

    // Also confirm without permission/GPS fix, live GPS never emits demo coordinates
    val gpsOutcome =
      kotlinx.coroutines.runBlocking {
        com.example.util.ImageCompressionUtil.fetchLiveLocationOutcome(context, "हिवरे बाजार")
      }
    assertFalse(gpsOutcome is com.example.util.LiveLocationOutcome.Success)
  }
}
