package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.InMemorySurveyRepository
import com.example.data.LocationRepository
import com.example.data.SurveyRepository
import com.example.domain.SurveyRuleEngine
import com.example.model.AppScreen
import com.example.model.CasteCategory
import com.example.model.ChecklistAnswers
import com.example.model.CustomerSurvey
import com.example.model.District
import com.example.model.DocumentComplianceChecks
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.ExecutiveUser
import com.example.model.HouseType
import com.example.model.MainTab
import com.example.model.MaterialAndOfficeDetails
import com.example.model.MeterInfo
import com.example.model.OverallStatus
import com.example.model.PendingActionItem
import com.example.model.ReportBRow
import com.example.model.SiteSurveyDetails
import com.example.model.SolarCapacity
import com.example.model.SurveyCategory
import com.example.model.Taluka
import com.example.model.Village
import com.example.model.VillageStats
import com.example.util.ImageCompressionUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CustomerStatusFilter(val labelMr: String) {
  ALL("सर्व"),
  READY("तयार"),
  PENDING_CUSTOMER("ग्राहक बाकी"),
  PENDING_COMPANY("कंपनी बाकी"),
}

data class AddEditWizardState(
  val isOpen: Boolean = false,
  val editingSurveyId: String? = null,
  val currentStep: Int = 1, // 1, 2, 3
  val surveyDate: String = defaultTodayDateString(),
  val beneficiaryName: String = "",
  val mobile: String = "",
  val emailId: String = "",
  val addressLandmark: String = "",
  val gharkulId: String = "",
  val aadhaarLast4: String = "",
  val casteCategory: CasteCategory = CasteCategory.OPEN,
  val houseType: HouseType = HouseType.SINGLE,
  val capacity: SolarCapacity = SolarCapacity.KW_1,
  val gpsLocation: String? = null,
  val meterInfo: MeterInfo = MeterInfo(),
  val siteSurveyDetails: SiteSurveyDetails = SiteSurveyDetails(),
  val documentComplianceChecks: DocumentComplianceChecks = DocumentComplianceChecks(),
  val materialAndOfficeDetails: MaterialAndOfficeDetails = MaterialAndOfficeDetails(),
  val documents: Map<DocumentType, DocumentEntry> = defaultDocumentMap(),
  val checklist: ChecklistAnswers = ChecklistAnswers(),
  val isChecklistManuallyEdited: Boolean = false,
  val matchedCategories: List<SurveyCategory> = listOf(SurveyCategory.CATEGORY_1),
  val suggestedCategory: SurveyCategory = SurveyCategory.CATEGORY_1,
  val overrideCategory: SurveyCategory? = null,
  val overrideReason: String = "",
  val executiveRemark: String = "",
  val pendingActions: List<PendingActionItem> = emptyList(),
  val overallStatus: OverallStatus = OverallStatus.READY,
  val duplicateWarnings: List<String> = emptyList(),
  val validationError: String? = null,
) {
  val effectiveCategory: SurveyCategory
    get() = overrideCategory ?: suggestedCategory
}

private fun defaultTodayDateString(): String {
  return SimpleDateFormat("dd / MM / yyyy", Locale.getDefault()).format(Date())
}

private fun defaultDocumentMap(): Map<DocumentType, DocumentEntry> =
  DocumentType.entries.associateWith { DocumentEntry(type = it) }

data class SurveyAppUiState(
  val currentScreen: AppScreen = AppScreen.LOGIN,
  val activeTab: MainTab = MainTab.HOME,
  val executive: ExecutiveUser =
    ExecutiveUser(
      "रमेश कुलकर्णी",
      "रमेश",
      "ramesh.k@gharkulsolar.in",
      "9823011223",
      "फील्ड एक्झिक्युटिव्ह",
    ),
  val loginEmail: String = "ramesh.k@gharkulsolar.in",
  val loginPassword: String = "123456",
  val loginError: String? = null,
  val districts: List<District> = emptyList(),
  val selectedDistrict: District? = null,
  val selectedTaluka: Taluka? = null,
  val selectedVillage: Village? = null,
  val homeSearchQuery: String = "",
  val listSearchQuery: String = "",
  val selectedCategoryFilter: SurveyCategory? = null,
  val selectedStatusFilter: CustomerStatusFilter = CustomerStatusFilter.ALL,
  val allSurveys: List<CustomerSurvey> = emptyList(),
  val activeVillageSurveys: List<CustomerSurvey> = emptyList(),
  val filteredListSurveys: List<CustomerSurvey> = emptyList(),
  val villageStats: VillageStats = VillageStats(0, 0, 0, 0),
  val reportBRows: List<ReportBRow> = emptyList(),
  val selectedDetailSurvey: CustomerSurvey? = null,
  val pendingDeleteSurvey: CustomerSurvey? = null,
  val wizard: AddEditWizardState = AddEditWizardState(),
  val snackbarMessage: String? = null,
)

class SurveyAppViewModel(application: Application) : AndroidViewModel(application) {

  private val locationRepository = LocationRepository(application.applicationContext)
  private val surveyRepository: SurveyRepository = InMemorySurveyRepository()

  private val _uiState = MutableStateFlow(SurveyAppUiState())
  val uiState: StateFlow<SurveyAppUiState> = _uiState.asStateFlow()

  init {
    val districts = locationRepository.getDistricts()
    val defaultDistrict = districts.firstOrNull()
    val defaultTaluka =
      defaultDistrict?.talukas?.find { it.id == "nagar" } ?: defaultDistrict?.talukas?.firstOrNull()
    val defaultVillage =
      defaultTaluka?.villages?.find { it.id == "ng_19" } ?: defaultTaluka?.villages?.firstOrNull()

    _uiState.update {
      it.copy(
        executive = surveyRepository.getExecutive(),
        districts = districts,
        selectedDistrict = defaultDistrict,
        selectedTaluka = defaultTaluka,
        selectedVillage = defaultVillage,
        allSurveys = surveyRepository.surveys.value,
      )
    }
    recomputeDerivedCollections()
  }

  fun updateLoginEmail(email: String) {
    _uiState.update { it.copy(loginEmail = email, loginError = null) }
  }

  fun updateLoginPassword(password: String) {
    _uiState.update { it.copy(loginPassword = password, loginError = null) }
  }

  fun submitLogin() {
    val state = _uiState.value
    if (state.loginEmail.isBlank() || !state.loginEmail.contains("@")) {
      _uiState.update { it.copy(loginError = "कृपया वैध ईमेल पत्ता प्रविष्ट करा.") }
      return
    }
    if (state.loginPassword.length < 4) {
      _uiState.update { it.copy(loginError = "कृपया किमान ४ अंकी पासवर्ड टाका.") }
      return
    }
    _uiState.update { it.copy(loginError = null, currentScreen = AppScreen.VILLAGE_SELECT) }
  }

  fun logout() {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.LOGIN,
        activeTab = MainTab.HOME,
        selectedDetailSurvey = null,
        wizard = AddEditWizardState(),
      )
    }
  }

  fun selectDistrict(district: District) {
    val firstTaluka = district.talukas.firstOrNull()
    val firstVillage = firstTaluka?.villages?.firstOrNull()
    _uiState.update {
      it.copy(
        selectedDistrict = district,
        selectedTaluka = firstTaluka,
        selectedVillage = firstVillage,
      )
    }
    recomputeDerivedCollections()
  }

  fun selectTaluka(taluka: Taluka) {
    val firstVillage = taluka.villages.firstOrNull()
    _uiState.update { it.copy(selectedTaluka = taluka, selectedVillage = firstVillage) }
    recomputeDerivedCollections()
  }

  fun selectVillage(village: Village) {
    _uiState.update { it.copy(selectedVillage = village) }
    recomputeDerivedCollections()
  }

  fun addAndSelectCustomVillage(villageName: String) {
    val district = _uiState.value.selectedDistrict ?: return
    val taluka = _uiState.value.selectedTaluka ?: return
    if (villageName.isBlank()) return
    val created = locationRepository.addCustomVillage(district.id, taluka.id, villageName)
    val refreshedDistricts = locationRepository.getDistricts()
    val refreshedDistrict = refreshedDistricts.find { it.id == district.id }
    val refreshedTaluka = refreshedDistrict?.talukas?.find { it.id == taluka.id }
    _uiState.update {
      it.copy(
        districts = refreshedDistricts,
        selectedDistrict = refreshedDistrict,
        selectedTaluka = refreshedTaluka,
        selectedVillage = created,
      )
    }
    recomputeDerivedCollections()
  }

  fun proceedToDashboard() {
    if (_uiState.value.selectedVillage == null) return
    _uiState.update { it.copy(currentScreen = AppScreen.MAIN_APP, activeTab = MainTab.HOME) }
    recomputeDerivedCollections()
  }

  fun openVillageSelectorFromDashboard() {
    _uiState.update { it.copy(currentScreen = AppScreen.VILLAGE_SELECT) }
  }

  fun navigateBackFromVillageSelector() {
    _uiState.update { it.copy(currentScreen = AppScreen.MAIN_APP) }
  }

  fun selectTab(tab: MainTab) {
    _uiState.update { it.copy(activeTab = tab, selectedDetailSurvey = null) }
  }

  fun updateHomeSearch(query: String) {
    _uiState.update { it.copy(homeSearchQuery = query) }
    recomputeDerivedCollections()
  }

  fun updateListSearch(query: String) {
    _uiState.update { it.copy(listSearchQuery = query) }
    recomputeDerivedCollections()
  }

  fun selectCategoryFilter(category: SurveyCategory?) {
    _uiState.update {
      val next = if (it.selectedCategoryFilter == category) null else category
      it.copy(selectedCategoryFilter = next)
    }
    recomputeDerivedCollections()
  }

  fun selectStatusFilter(filter: CustomerStatusFilter) {
    _uiState.update { it.copy(selectedStatusFilter = filter) }
    recomputeDerivedCollections()
  }

  fun openCustomerDetail(survey: CustomerSurvey) {
    _uiState.update { it.copy(selectedDetailSurvey = survey) }
  }

  fun closeCustomerDetail() {
    _uiState.update { it.copy(selectedDetailSurvey = null) }
  }

  fun requestDeleteCustomer(survey: CustomerSurvey) {
    _uiState.update { it.copy(pendingDeleteSurvey = survey) }
  }

  fun dismissDeleteDialog() {
    _uiState.update { it.copy(pendingDeleteSurvey = null) }
  }

  fun confirmDeleteCustomer() {
    val target = _uiState.value.pendingDeleteSurvey ?: return
    val photoUris = buildList {
      target.documents.values.forEach { add(it.photoUri) }
      add(target.siteSurveyDetails.noteCamRoofBeforePhotoUri)
      add(target.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri)
    }
    ImageCompressionUtil.deleteCachedPhotos(getApplication(), photoUris)
    surveyRepository.deleteSurvey(target.id)

    _uiState.update { state ->
      state.copy(
        allSurveys = surveyRepository.surveys.value,
        pendingDeleteSurvey = null,
        selectedDetailSurvey =
          if (state.selectedDetailSurvey?.id == target.id) null else state.selectedDetailSurvey,
        wizard =
          if (state.wizard.isOpen && state.wizard.editingSurveyId == target.id) AddEditWizardState()
          else state.wizard,
        snackbarMessage = "${target.beneficiaryName} यांची सर्व माहिती व कागदपत्रे कायमची हटवली.",
      )
    }
    recomputeDerivedCollections()
  }

  fun openNewSurveyWizard() {
    val execName = _uiState.value.executive.nameMr
    _uiState.update {
      it.copy(
        selectedDetailSurvey = null,
        wizard =
          AddEditWizardState(
            isOpen = true,
            currentStep = 1,
            materialAndOfficeDetails = MaterialAndOfficeDetails(surveyorName = execName),
          ),
      )
    }
    recalculateWizardRules()
  }

  fun openEditSurveyWizard(survey: CustomerSurvey) {
    val filledDocs = defaultDocumentMap().toMutableMap()
    survey.documents.forEach { (k, v) -> filledDocs[k] = v }
    _uiState.update {
      it.copy(
        selectedDetailSurvey = null,
        wizard =
          AddEditWizardState(
            isOpen = true,
            editingSurveyId = survey.id,
            currentStep = 1,
            surveyDate = survey.surveyDate,
            beneficiaryName = survey.beneficiaryName,
            mobile = survey.mobile,
            emailId = survey.emailId,
            addressLandmark = survey.addressLandmark,
            gharkulId = survey.gharkulId,
            aadhaarLast4 = survey.aadhaarLast4,
            casteCategory = survey.casteCategory,
            houseType = survey.houseType,
            capacity = survey.capacity,
            gpsLocation = survey.gpsLocation,
            meterInfo = survey.meterInfo,
            siteSurveyDetails = survey.siteSurveyDetails,
            documentComplianceChecks = survey.documentComplianceChecks,
            materialAndOfficeDetails = survey.materialAndOfficeDetails,
            documents = filledDocs,
            checklist = survey.checklistAnswers,
            isChecklistManuallyEdited = true,
            matchedCategories = survey.matchedCategories,
            suggestedCategory = survey.suggestedCategory,
            overrideCategory = if (survey.finalCategory != survey.suggestedCategory) survey.finalCategory else null,
            overrideReason = survey.overrideReason,
            executiveRemark = survey.executiveRemark,
            pendingActions = survey.pendingActions,
            overallStatus = survey.overallStatus,
          ),
      )
    }
    recalculateWizardRules()
  }

  fun closeWizard() {
    _uiState.update { it.copy(wizard = AddEditWizardState()) }
  }

  fun setWizardStep(step: Int) {
    _uiState.update {
      it.copy(wizard = it.wizard.copy(currentStep = step.coerceIn(1, 3), validationError = null))
    }
  }

  fun nextWizardStep() {
    val w = _uiState.value.wizard
    if (w.currentStep == 1) {
      val error = validateStep1(w)
      if (error != null) {
        _uiState.update { it.copy(wizard = w.copy(validationError = error)) }
        return
      }
      _uiState.update { it.copy(wizard = w.copy(currentStep = 2, validationError = null)) }
      recalculateWizardRules()
    } else if (w.currentStep == 2) {
      val invalidSizeDoc =
        w.documents.values.firstOrNull { entry ->
          entry.photoUri != null &&
            entry.photoSizeKb != null &&
            entry.photoSizeKb !in ImageCompressionUtil.MIN_TARGET_KB..ImageCompressionUtil.MAX_TARGET_KB
        }
      if (invalidSizeDoc != null) {
        _uiState.update {
          it.copy(
            wizard =
              w.copy(
                validationError =
                  "${invalidSizeDoc.type.labelMr} फोटोचा आकार ${invalidSizeDoc.photoSizeKb} KB आहे. प्रत्येक फोटो 200 ते 500 KB दरम्यान असणे आवश्यक आहे."
              )
          )
        }
        return
      }
      _uiState.update { it.copy(wizard = w.copy(currentStep = 3, validationError = null)) }
      recalculateWizardRules()
    }
  }

  private fun validateStep1(w: AddEditWizardState): String? {
    if (w.surveyDate.isBlank()) return "कृपया सर्व्हे दिनांक निवडा (* अनिवार्य)."
    if (w.beneficiaryName.isBlank()) return "कृपया घरकुलावरील नाव (मालकाचे नाव) टाका (* अनिवार्य)."
    if (w.mobile.filter { it.isDigit() }.length < 10) return "कृपया १० अंकी मोबाईल नंबर टाका (* अनिवार्य)."
    if (w.emailId.isBlank() || !w.emailId.contains("@")) return "कृपया वैध ई-मेल आयडी टाका (* अनिवार्य)."
    if (w.meterInfo.isPoleNearbyForNewConn == null) return "कृपया 'नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?' निवडा (* अनिवार्य)."
    if (w.meterInfo.interestedInNewConnection == null) return "कृपया 'नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?' निवडा (* अनिवार्य)."
    if (w.meterInfo.hasMeter && !w.meterInfo.sameAsBeneficiary && w.meterInfo.meterHolderName.isBlank()) {
      return "कृपया मीटर धारकाचे नाव टाका (* अनिवार्य)."
    }
    if (w.siteSurveyDetails.roofType == null) return "कृपया छताचा प्रकार निवडा (* अनिवार्य)."
    if (w.siteSurveyDetails.hasSolarPanelSpace100SqFt == null) {
      return "कृपया 'सोलर पॅनेलसाठी जागा उपलब्ध आहे का?' निवडा (* अनिवार्य)."
    }
    if (w.siteSurveyDetails.generationMeterLocation.isBlank()) {
      return "कृपया जनरेशन मीटर बसवण्याची जागा लिहा (* अनिवार्य)."
    }
    if (w.siteSurveyDetails.hasEarthingSpace == null) {
      return "कृपया 'अर्थिंगसाठी जागा उपलब्ध आहे का?' निवडा (* अनिवार्य)."
    }
    return null
  }

  fun previousWizardStep() {
    val w = _uiState.value.wizard
    if (w.currentStep > 1) {
      _uiState.update { it.copy(wizard = w.copy(currentStep = w.currentStep - 1, validationError = null)) }
    } else {
      closeWizard()
    }
  }

  fun updateWizardBasic(
    surveyDate: String? = null,
    beneficiaryName: String? = null,
    mobile: String? = null,
    emailId: String? = null,
    addressLandmark: String? = null,
    gharkulId: String? = null,
    aadhaarLast4: String? = null,
    casteCategory: CasteCategory? = null,
    houseType: HouseType? = null,
    gpsLocation: String? = null,
  ) {
    _uiState.update { state ->
      val cur = state.wizard
      state.copy(
        wizard =
          cur.copy(
            surveyDate = surveyDate ?: cur.surveyDate,
            beneficiaryName = beneficiaryName ?: cur.beneficiaryName,
            mobile = mobile ?: cur.mobile,
            emailId = emailId ?: cur.emailId,
            addressLandmark = addressLandmark ?: cur.addressLandmark,
            gharkulId = gharkulId ?: cur.gharkulId,
            aadhaarLast4 = aadhaarLast4 ?: cur.aadhaarLast4,
            casteCategory = casteCategory ?: cur.casteCategory,
            houseType = houseType ?: cur.houseType,
            gpsLocation = gpsLocation ?: cur.gpsLocation,
            validationError = null,
          )
      )
    }
    recalculateWizardRules()
  }

  fun captureSampleGpsForSelectedDistrict() {
    val isSambhajinagar = _uiState.value.selectedDistrict?.id == "sambhajinagar"
    val coords = if (isSambhajinagar) "19.8762° N, 75.3433° E" else "19.0948° N, 74.7480° E"
    updateWizardBasic(gpsLocation = coords)
  }

  fun updateWizardMeter(transform: (MeterInfo) -> MeterInfo) {
    _uiState.update { state ->
      state.copy(
        wizard =
          state.wizard.copy(
            meterInfo = transform(state.wizard.meterInfo),
            validationError = null,
          )
      )
    }
    recalculateWizardRules()
  }

  fun updateSiteSurvey(transform: (SiteSurveyDetails) -> SiteSurveyDetails) {
    _uiState.update { state ->
      state.copy(
        wizard =
          state.wizard.copy(
            siteSurveyDetails = transform(state.wizard.siteSurveyDetails),
            validationError = null,
          )
      )
    }
  }

  fun updateDocumentCompliance(transform: (DocumentComplianceChecks) -> DocumentComplianceChecks) {
    _uiState.update { state ->
      state.copy(
        wizard =
          state.wizard.copy(
            documentComplianceChecks = transform(state.wizard.documentComplianceChecks),
          )
      )
    }
  }

  fun updateMaterialAndOffice(transform: (MaterialAndOfficeDetails) -> MaterialAndOfficeDetails) {
    _uiState.update { state ->
      state.copy(
        wizard =
          state.wizard.copy(
            materialAndOfficeDetails = transform(state.wizard.materialAndOfficeDetails),
            validationError = null,
          )
      )
    }
  }

  fun updateDocumentEntry(
    type: DocumentType,
    printedName: String? = null,
    photoUri: String? = null,
    photoSizeKb: Int? = null,
    isSizeValid: Boolean? = null,
    legibility: DocumentLegibility? = null,
    clearPhoto: Boolean = false,
  ) {
    _uiState.update { state ->
      val cur = state.wizard
      val oldEntry = cur.documents[type] ?: DocumentEntry(type)
      val nextPrintedName = printedName ?: oldEntry.printedName
      val nextPhotoUri = if (clearPhoto) null else (photoUri ?: oldEntry.photoUri)
      val nextPhotoKb = if (clearPhoto) null else (photoSizeKb ?: oldEntry.photoSizeKb)
      val nextSizeValid =
        when {
          clearPhoto -> true
          isSizeValid != null -> isSizeValid
          nextPhotoKb != null -> nextPhotoKb in ImageCompressionUtil.MIN_TARGET_KB..ImageCompressionUtil.MAX_TARGET_KB
          else -> oldEntry.isSizeValid
        }
      val nextLegibility =
        when {
          legibility != null -> legibility
          photoUri != null && oldEntry.legibility == DocumentLegibility.MISSING -> DocumentLegibility.CLEAR
          clearPhoto -> DocumentLegibility.MISSING
          else -> oldEntry.legibility
        }
      val refName =
        cur.documents[DocumentType.AADHAAR]?.printedName?.ifBlank { cur.beneficiaryName }
          ?: cur.beneficiaryName
      val matchRes =
        SurveyRuleEngine.matchName(
          referenceName = if (type == DocumentType.AADHAAR) cur.beneficiaryName else refName,
          documentName = nextPrintedName,
        )
      val updatedDocs =
        cur.documents.toMutableMap().apply {
          put(
            type,
            oldEntry.copy(
              printedName = nextPrintedName,
              photoUri = nextPhotoUri,
              photoSizeKb = nextPhotoKb,
              isSizeValid = nextSizeValid,
              legibility = nextLegibility,
              matchResult = matchRes,
            ),
          )
        }
      state.copy(wizard = cur.copy(documents = updatedDocs, isChecklistManuallyEdited = false, validationError = null))
    }
    recalculateWizardRules()
  }

  fun updateChecklistAnswer(transform: (ChecklistAnswers) -> ChecklistAnswers) {
    _uiState.update { state ->
      state.copy(
        wizard =
          state.wizard.copy(
            checklist = transform(state.wizard.checklist),
            isChecklistManuallyEdited = true,
          )
      )
    }
    recalculateWizardRules()
  }

  fun setOverrideCategory(category: SurveyCategory?) {
    _uiState.update { state ->
      state.copy(wizard = state.wizard.copy(overrideCategory = category, validationError = null))
    }
    recalculateWizardRules()
  }

  fun updateOverrideReason(reason: String) {
    _uiState.update { state ->
      state.copy(wizard = state.wizard.copy(overrideReason = reason, validationError = null))
    }
  }

  fun updateExecutiveRemark(remark: String) {
    _uiState.update { state ->
      state.copy(wizard = state.wizard.copy(executiveRemark = remark))
    }
  }

  fun saveWizardSurvey() {
    val state = _uiState.value
    val w = state.wizard
    val district = state.selectedDistrict ?: return
    val taluka = state.selectedTaluka ?: return
    val village = state.selectedVillage ?: return

    if (w.overrideCategory != null && w.overrideCategory != w.suggestedCategory && w.overrideReason.isBlank()) {
      _uiState.update {
        it.copy(wizard = w.copy(validationError = "श्रेणी बदलल्यास कारण लिहिणे आवश्यक आहे."))
      }
      return
    }
    if (w.materialAndOfficeDetails.surveyorName.isBlank()) {
      _uiState.update {
        it.copy(wizard = w.copy(validationError = "कृपया सर्व्हे करणाऱ्या व्यक्तीचे नाव लिहा."))
      }
      return
    }

    val finalCategory = w.effectiveCategory
    val pending =
      SurveyRuleEngine.computePendingActions(
        category = finalCategory,
        meterInfo = w.meterInfo,
        casteCategory = w.casteCategory,
        checklist = w.checklist,
        documents = w.documents,
      )
    val overall = SurveyRuleEngine.computeOverallStatus(finalCategory, pending)

    val saved =
      CustomerSurvey(
        id = w.editingSurveyId ?: "cust_${System.currentTimeMillis()}",
        villageId = village.id,
        villageNameMr = village.nameMr,
        talukaNameMr = taluka.nameMr,
        districtNameMr = district.nameMr,
        surveyDate = w.surveyDate.trim(),
        beneficiaryName = w.beneficiaryName.trim(),
        mobile = w.mobile.trim(),
        emailId = w.emailId.trim(),
        addressLandmark = w.addressLandmark.trim(),
        gharkulId = w.gharkulId.trim(),
        aadhaarLast4 = w.aadhaarLast4.trim(),
        casteCategory = w.casteCategory,
        houseType = w.houseType,
        capacity = SolarCapacity.KW_1,
        gpsLocation = w.gpsLocation,
        meterInfo = w.meterInfo,
        siteSurveyDetails = w.siteSurveyDetails,
        documentComplianceChecks = w.documentComplianceChecks,
        materialAndOfficeDetails = w.materialAndOfficeDetails,
        documents = w.documents,
        checklistAnswers = w.checklist,
        matchedCategories = w.matchedCategories,
        suggestedCategory = w.suggestedCategory,
        finalCategory = finalCategory,
        overrideReason = w.overrideReason.trim(),
        overallStatus = overall,
        pendingActions = pending,
        executiveRemark = w.executiveRemark.trim(),
        surveyDateLabel = "आज",
        isSurveyedToday = true,
      )

    surveyRepository.saveSurvey(saved)
    _uiState.update {
      it.copy(
        allSurveys = surveyRepository.surveys.value,
        wizard = AddEditWizardState(),
        snackbarMessage = "${saved.beneficiaryName} यांचा सर्व्हे (${finalCategory.shortTagMr}) जतन झाला.",
      )
    }
    recomputeDerivedCollections()
  }

  fun showMessage(message: String) {
    _uiState.update { it.copy(snackbarMessage = message) }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  private fun recalculateWizardRules() {
    _uiState.update { state ->
      val w = state.wizard
      val refName =
        w.documents[DocumentType.AADHAAR]?.printedName?.ifBlank { w.beneficiaryName }
          ?: w.beneficiaryName
      val syncedDocs =
        w.documents.mapValues { (type, entry) ->
          val targetRef = if (type == DocumentType.AADHAAR) w.beneficiaryName else refName
          entry.copy(matchResult = SurveyRuleEngine.matchName(targetRef, entry.printedName))
        }
      val checklist =
        if (w.isChecklistManuallyEdited) w.checklist
        else SurveyRuleEngine.deriveChecklist(w.beneficiaryName, w.meterInfo, syncedDocs)
      val (suggested, matched) = SurveyRuleEngine.suggestCategoryWithPriority(checklist)
      val effective = w.overrideCategory ?: suggested
      val pending =
        SurveyRuleEngine.computePendingActions(
          category = effective,
          meterInfo = w.meterInfo,
          casteCategory = w.casteCategory,
          checklist = checklist,
          documents = syncedDocs,
        )
      val status = SurveyRuleEngine.computeOverallStatus(effective, pending)
      val duplicates =
        SurveyRuleEngine.findDuplicates(
          existingSurveys = state.allSurveys,
          editingId = w.editingSurveyId,
          aadhaarLast4 = w.aadhaarLast4,
          gharkulId = w.gharkulId,
          consumerNo = w.meterInfo.consumerNumber,
        )
      state.copy(
        wizard =
          w.copy(
            documents = syncedDocs,
            checklist = checklist,
            matchedCategories = matched,
            suggestedCategory = suggested,
            pendingActions = pending,
            overallStatus = status,
            duplicateWarnings = duplicates,
            materialAndOfficeDetails =
              w.materialAndOfficeDetails.copy(
                documentsCompleteStatus = pending.isEmpty(),
              ),
          )
      )
    }
  }

  private fun recomputeDerivedCollections() {
    _uiState.update { state ->
      val villageId = state.selectedVillage?.id
      val villageSurveys =
        if (villageId == null) state.allSurveys
        else state.allSurveys.filter { it.villageId == villageId }

      val homeFiltered =
        if (state.homeSearchQuery.isBlank()) villageSurveys
        else {
          val q = state.homeSearchQuery.trim()
          villageSurveys.filter {
            it.beneficiaryName.contains(q, ignoreCase = true) ||
              it.mobile.contains(q, ignoreCase = true) ||
              it.gharkulId.contains(q, ignoreCase = true)
          }
        }

      val listFiltered =
        villageSurveys.filter { s ->
          val matchesQuery =
            state.listSearchQuery.isBlank() ||
              s.beneficiaryName.contains(state.listSearchQuery.trim(), ignoreCase = true) ||
              s.mobile.contains(state.listSearchQuery.trim(), ignoreCase = true) ||
              s.gharkulId.contains(state.listSearchQuery.trim(), ignoreCase = true) ||
              s.meterInfo.consumerNumber.contains(state.listSearchQuery.trim(), ignoreCase = true)
          val matchesCategory =
            state.selectedCategoryFilter == null || s.finalCategory == state.selectedCategoryFilter
          val matchesStatus =
            when (state.selectedStatusFilter) {
              CustomerStatusFilter.ALL -> true
              CustomerStatusFilter.READY -> s.overallStatus == OverallStatus.READY
              CustomerStatusFilter.PENDING_CUSTOMER -> s.overallStatus == OverallStatus.PENDING_CUSTOMER
              CustomerStatusFilter.PENDING_COMPANY -> s.overallStatus == OverallStatus.PENDING_COMPANY
            }
          matchesQuery && matchesCategory && matchesStatus
        }

      val readyCount = villageSurveys.count { it.overallStatus == OverallStatus.READY }
      val pendingCount = villageSurveys.count { it.overallStatus != OverallStatus.READY }
      val todayCount = villageSurveys.count { it.isSurveyedToday }

      val stats =
        VillageStats(
          surveyedToday = todayCount,
          totalCount = villageSurveys.size,
          readyCount = readyCount,
          pendingCount = pendingCount,
        )

      val reportRows = buildReportBRows(villageSurveys)

      state.copy(
        activeVillageSurveys = homeFiltered,
        filteredListSurveys = listFiltered,
        villageStats = stats,
        reportBRows = reportRows,
      )
    }
  }

  private fun buildReportBRows(surveys: List<CustomerSurvey>): List<ReportBRow> {
    fun row(srNo: String, label: String, subset: List<CustomerSurvey>) =
      ReportBRow(
        srNo = srNo,
        labelMr = label,
        total = subset.size,
        openCount = subset.count { it.casteCategory == CasteCategory.OPEN },
        scCount = subset.count { it.casteCategory == CasteCategory.SC },
        stCount = subset.count { it.casteCategory == CasteCategory.ST },
      )
    return listOf(
      row("एकूण", "एकूण साईट सर्व्हे", surveys),
      row("1", "घरकुल व मीटर एकाच नावावर (तयार)", surveys.filter { it.finalCategory == SurveyCategory.CATEGORY_1 }),
      row("2", "नावामध्ये बदल (कागदपत्रे)", surveys.filter { it.finalCategory == SurveyCategory.CATEGORY_2 }),
      row("3", "मीटर दुसऱ्याच्या नावावर", surveys.filter { it.finalCategory == SurveyCategory.CATEGORY_3 }),
      row("4", "लाईट कनेक्शन नाही", surveys.filter { it.finalCategory == SurveyCategory.CATEGORY_4 }),
    )
  }
}
