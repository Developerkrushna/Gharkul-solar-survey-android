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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeleteCustomerRobolectricTest {

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
}
