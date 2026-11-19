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
import com.example.model.InstallationPhotoItem
import com.example.model.InstallationProductRow
import com.example.model.InstallationQueueFilter
import com.example.model.InstallationReport
import com.example.model.InstallationTab
import com.example.model.LoginRole
import com.example.model.MainTab
import com.example.model.MaterialAndOfficeDetails
import com.example.model.MeterInfo
import com.example.model.OverallStatus
import com.example.model.PanelSerialEntry
import com.example.model.PendingActionItem
import com.example.model.ReportBRow
import com.example.model.SerialVerificationField
import com.example.model.SiteSurveyDetails
import com.example.model.SolarCapacity
import com.example.model.SurveyCategory
import com.example.model.Taluka
import com.example.model.Village
import com.example.model.VillageStats
import com.example.model.defaultInstallationDate
import com.example.model.defaultInstallationPhotos
import com.example.model.defaultInstallationProductRows
import com.example.model.defaultPanelSerialEntries
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

data class InstallationFormState(
  val editingReportId: String? = null,
  val linkedSurveyId: String? = null,
  val villageName: String = "",
  val currentPage: Int = 1, // 1 = Fill Form, 2 = Serial Numbers, 3 = After Installation NoteCam Geo Images
  val consumerNo: String = "",
  val installationDate: String = defaultInstallationDate(),
  val consumerName: String = "",
  val mobileNo: String = "",
  val systemCapacity: String = "1 KW",
  val systemType: String = "ONGRID",
  val products: List<InstallationProductRow> = defaultInstallationProductRows(),
  val installerName: String = "Vikram Kulkarni",
  val installerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
  val accountantName: String = "",
  val managerName: String = "",
  val supervisorName: String = "",
  val customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
  val inverterSerial: SerialVerificationField = SerialVerificationField(),
  val generationMeterSerial: SerialVerificationField = SerialVerificationField(),
  val panelSerials: List<PanelSerialEntry> = defaultPanelSerialEntries(),
  val afterInstallationPhotos: List<InstallationPhotoItem> = defaultInstallationPhotos(),
  val validationError: String? = null,
)

data class SurveyAppUiState(
  val loginRole: LoginRole = LoginRole.SURVEY_EXECUTIVE,
  val currentScreen: AppScreen = AppScreen.LOGIN,
  val activeTab: MainTab = MainTab.HOME,
  val activeInstallationTab: InstallationTab = InstallationTab.HOME,
  val executive: ExecutiveUser =
    ExecutiveUser(
      "रमेश कुलकर्णी",
      "रमेश",
      "ramesh.k@gharkulsolar.in",
      "9823011223",
      "फील्ड एक्झिक्युटिव्ह",
    ),
  val installationExecutive: ExecutiveUser =
    ExecutiveUser(
      "Vikram Kulkarni",
      "Vikram",
      "installer@reflectionmedias.in",
      "91300028122",
      "Solar Installation Executive",
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
  val approvedCategory1Surveys: List<CustomerSurvey> = emptyList(),
  val installationReports: List<InstallationReport> = emptyList(),
  val installationSearchQuery: String = "",
  val installationFilter: InstallationQueueFilter = InstallationQueueFilter.ALL,
  val installationForm: InstallationFormState = InstallationFormState(),
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
        installationExecutive = surveyRepository.getInstallationExecutive(),
        districts = districts,
        selectedDistrict = defaultDistrict,
        selectedTaluka = defaultTaluka,
        selectedVillage = defaultVillage,
        allSurveys = surveyRepository.surveys.value,
        installationReports = surveyRepository.installationReports.value,
      )
    }
    recomputeDerivedCollections()
  }

  fun selectLoginRole(role: LoginRole) {
    _uiState.update { state ->
      if (state.loginRole == role) return@update state
      val nextEmail =
        when (role) {
          LoginRole.SURVEY_EXECUTIVE -> state.executive.email
          LoginRole.SOLAR_INSTALLATION -> state.installationExecutive.email
        }
      state.copy(
        loginRole = role,
        loginEmail = nextEmail,
        loginPassword = if (state.loginPassword.isBlank()) "123456" else state.loginPassword,
        loginError = null,
      )
    }
  }

  fun updateLoginEmail(email: String) {
    _uiState.update { it.copy(loginEmail = email, loginError = null) }
  }

  fun updateLoginPassword(password: String) {
    _uiState.update { it.copy(loginPassword = password, loginError = null) }
  }

  fun submitLogin() {
    val state = _uiState.value
    val isInstallation = state.loginRole == LoginRole.SOLAR_INSTALLATION
    if (state.loginEmail.isBlank() || !state.loginEmail.contains("@")) {
      _uiState.update {
        it.copy(
          loginError =
            if (isInstallation) "Please enter a valid email address."
            else "कृपया वैध ईमेल पत्ता प्रविष्ट करा."
        )
      }
      return
    }
    if (state.loginPassword.length < 4) {
      _uiState.update {
        it.copy(
          loginError =
            if (isInstallation) "Please enter at least a 4-character password."
            else "कृपया किमान ४ अंकी पासवर्ड टाका."
        )
      }
      return
    }
    val nextScreen =
      if (isInstallation) AppScreen.INSTALLATION_DASHBOARD else AppScreen.VILLAGE_SELECT
    _uiState.update {
      it.copy(
        loginError = null,
        currentScreen = nextScreen,
        activeInstallationTab = InstallationTab.HOME,
      )
    }
  }

  fun logout() {
    _uiState.update { state ->
      val defaultEmail =
        if (state.loginRole == LoginRole.SOLAR_INSTALLATION) state.installationExecutive.email
        else state.executive.email
      state.copy(
        currentScreen = AppScreen.LOGIN,
        activeTab = MainTab.HOME,
        activeInstallationTab = InstallationTab.HOME,
        loginEmail = defaultEmail,
        loginError = null,
        selectedDetailSurvey = null,
        wizard = AddEditWizardState(),
        installationForm = InstallationFormState(),
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
      _uiState.update { it.copy(wizard = w.copy(currentStep = 3, validationError = null)) }
      recalculateWizardRules()
    }
  }

  private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  private fun validateStep1(w: AddEditWizardState): String? {
    if (w.surveyDate.isBlank()) return "कृपया सर्व्हे दिनांक निवडा (* अनिवार्य)."
    if (w.beneficiaryName.isBlank()) return "कृपया घरकुलावरील नाव (मालकाचे नाव) टाका (* अनिवार्य)."
    val mobileDigits = w.mobile.filter { it.isDigit() }
    if (mobileDigits.length != 10) return "कृपया बरोबर १० अंकी मोबाईल नंबर टाका (* अनिवार्य)."
    val trimmedEmail = w.emailId.trim()
    if (trimmedEmail.isBlank() || !emailRegex.matches(trimmedEmail)) {
      return "कृपया वैध ई-मेल आयडी टाका (उदा. name@gmail.com) (* अनिवार्य)."
    }
    if (w.meterInfo.isPoleNearbyForNewConn == null) return "कृपया 'नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?' निवडा (* अनिवार्य)."
    if (w.meterInfo.interestedInNewConnection == null) return "कृपया 'नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?' निवडा (* अनिवार्य)."
    if (w.meterInfo.hasMeter) {
      val consumerDigits = w.meterInfo.consumerNumber.trim()
      if (consumerDigits.isNotEmpty() && consumerDigits.length != 12) {
        return "ग्राहक क्रमांक भरल्यास बरोबर १२ अंकी असणे आवश्यक आहे."
      }
      if (!w.meterInfo.sameAsBeneficiary && w.meterInfo.meterHolderName.isBlank()) {
        return "कृपया मीटर धारकाचे नाव टाका (* अनिवार्य)."
      }
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
    queryNote: String? = null,
  ) {
    _uiState.update { state ->
      val cur = state.wizard
      val oldEntry = cur.documents[type] ?: DocumentEntry(type)
      val nextPrintedName = printedName ?: oldEntry.printedName
      val nextPhotoUri = if (clearPhoto) null else (photoUri ?: oldEntry.photoUri)
      val nextPhotoKb = if (clearPhoto) null else (photoSizeKb ?: oldEntry.photoSizeKb)
      val nextSizeValid = isSizeValid ?: true
      val nextLegibility =
        when {
          legibility != null -> legibility
          photoUri != null -> DocumentLegibility.CLEAR
          clearPhoto -> DocumentLegibility.MISSING
          else -> oldEntry.legibility
        }
      val nextQueryNote = queryNote ?: oldEntry.queryNote
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
              queryNote = nextQueryNote,
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

    if (w.materialAndOfficeDetails.surveyorName.isBlank()) {
      _uiState.update {
        it.copy(wizard = w.copy(validationError = "कृपया सर्व्हे करणाऱ्या व्यक्तीचे नाव लिहा."))
      }
      return
    }

    val finalCategory = w.suggestedCategory
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
      val approvedCat1 =
        state.allSurveys.filter {
          it.finalCategory == SurveyCategory.CATEGORY_1 && it.overallStatus == OverallStatus.READY
        }

      state.copy(
        activeVillageSurveys = homeFiltered,
        filteredListSurveys = listFiltered,
        villageStats = stats,
        reportBRows = reportRows,
        approvedCategory1Surveys = approvedCat1,
        installationReports = surveyRepository.installationReports.value,
      )
    }
  }

  // ---- Solar Installation Workflow Actions (English Only) ----

  fun selectInstallationTab(tab: InstallationTab) {
    _uiState.update { it.copy(activeInstallationTab = tab) }
  }

  fun updateInstallationSearch(query: String) {
    _uiState.update { it.copy(installationSearchQuery = query) }
  }

  fun selectInstallationFilter(filter: InstallationQueueFilter) {
    _uiState.update { it.copy(installationFilter = filter) }
  }

  fun startInstallationFromSurvey(survey: CustomerSurvey) {
    val existing = _uiState.value.installationReports.firstOrNull { it.linkedSurveyId == survey.id }
    if (existing != null) {
      openEditInstallationReport(existing)
      return
    }
    val installerName = _uiState.value.installationExecutive.nameMr
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.INSTALLATION_FORM,
        installationForm =
          InstallationFormState(
            linkedSurveyId = survey.id,
            villageName = survey.villageNameMr,
            currentPage = 1,
            consumerNo = survey.meterInfo.consumerNumber,
            installationDate = defaultInstallationDate(),
            consumerName = survey.beneficiaryName,
            mobileNo = survey.mobile,
            systemCapacity = "1 KW",
            systemType = "ONGRID",
            products = defaultInstallationProductRows(),
            installerName = installerName,
            panelSerials = defaultPanelSerialEntries(),
            afterInstallationPhotos = defaultInstallationPhotos(),
          ),
      )
    }
  }

  fun startManualInstallationForm() {
    val installerName = _uiState.value.installationExecutive.nameMr
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.INSTALLATION_FORM,
        installationForm =
          InstallationFormState(
            linkedSurveyId = null,
            villageName = "Manual Entry",
            currentPage = 1,
            installationDate = defaultInstallationDate(),
            systemCapacity = "1 KW",
            systemType = "ONGRID",
            products = defaultInstallationProductRows(),
            installerName = installerName,
            panelSerials = defaultPanelSerialEntries(),
            afterInstallationPhotos = defaultInstallationPhotos(),
          ),
      )
    }
  }

  fun openEditInstallationReport(report: InstallationReport) {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.INSTALLATION_FORM,
        installationForm =
          InstallationFormState(
            editingReportId = report.id,
            linkedSurveyId = report.linkedSurveyId,
            villageName = report.villageName,
            currentPage = 1,
            consumerNo = report.consumerNo,
            installationDate = report.installationDate,
            consumerName = report.consumerName,
            mobileNo = report.mobileNo,
            systemCapacity = report.systemCapacity,
            systemType = report.systemType,
            products = report.products,
            installerName = report.installerName,
            installerSignatureStrokes = report.installerSignatureStrokes,
            accountantName = report.accountantName,
            managerName = report.managerName,
            supervisorName = report.supervisorName,
            customerSignatureStrokes = report.customerSignatureStrokes,
            inverterSerial = report.inverterSerial,
            generationMeterSerial = report.generationMeterSerial,
            panelSerials = report.panelSerials.ifEmpty { defaultPanelSerialEntries() },
            afterInstallationPhotos = report.afterInstallationPhotos.ifEmpty { defaultInstallationPhotos() },
          ),
      )
    }
  }

  fun closeInstallationForm() {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.INSTALLATION_DASHBOARD,
        installationForm = InstallationFormState(),
      )
    }
  }

  fun setInstallationFormPage(page: Int) {
    _uiState.update {
      it.copy(
        installationForm = it.installationForm.copy(
          currentPage = page.coerceIn(1, 3),
          validationError = null,
        )
      )
    }
  }

  fun nextInstallationFormPage() {
    val form = _uiState.value.installationForm
    when (form.currentPage) {
      1 -> {
        val part1Error = validateInstallationPart1(form)
        if (part1Error != null) {
          _uiState.update { it.copy(installationForm = form.copy(validationError = part1Error)) }
          return
        }
        _uiState.update {
          it.copy(installationForm = form.copy(currentPage = 2, validationError = null))
        }
      }
      2 -> {
        val part2Error = validateInstallationPart2(form)
        if (part2Error != null) {
          _uiState.update { it.copy(installationForm = form.copy(validationError = part2Error)) }
          return
        }
        _uiState.update {
          it.copy(installationForm = form.copy(currentPage = 3, validationError = null))
        }
      }
    }
  }

  fun previousInstallationFormPage() {
    val form = _uiState.value.installationForm
    if (form.currentPage > 1) {
      _uiState.update {
        it.copy(installationForm = form.copy(currentPage = form.currentPage - 1, validationError = null))
      }
    } else {
      closeInstallationForm()
    }
  }

  fun updateInstallationHeader(
    consumerNo: String? = null,
    installationDate: String? = null,
    consumerName: String? = null,
    mobileNo: String? = null,
    systemCapacity: String? = null,
    systemType: String? = null,
    villageName: String? = null,
  ) {
    _uiState.update { state ->
      val f = state.installationForm
      state.copy(
        installationForm =
          f.copy(
            consumerNo = consumerNo ?: f.consumerNo,
            installationDate = installationDate ?: f.installationDate,
            consumerName = consumerName ?: f.consumerName,
            mobileNo = mobileNo ?: f.mobileNo,
            systemCapacity = systemCapacity ?: f.systemCapacity,
            systemType = systemType ?: f.systemType,
            villageName = villageName ?: f.villageName,
            validationError = null,
          )
      )
    }
  }

  fun updateInstallationProductRow(
    rowId: String,
    company: String? = null,
    size: String? = null,
    capacity: String? = null,
    qty: String? = null,
  ) {
    _uiState.update { state ->
      val f = state.installationForm
      val updatedProducts =
        f.products.map { row ->
          if (row.rowId == rowId) {
            row.copy(
              company = company ?: row.company,
              size = size ?: row.size,
              capacity = capacity ?: row.capacity,
              qty = qty ?: row.qty,
            )
          } else {
            row
          }
        }
      state.copy(
        installationForm = f.copy(products = updatedProducts, validationError = null)
      )
    }
  }

  fun updateInstallationSignOff(
    installerName: String? = null,
    installerSignatureStrokes: List<List<Pair<Float, Float>>>? = null,
    accountantName: String? = null,
    managerName: String? = null,
    supervisorName: String? = null,
    customerSignatureStrokes: List<List<Pair<Float, Float>>>? = null,
  ) {
    _uiState.update { state ->
      val f = state.installationForm
      state.copy(
        installationForm =
          f.copy(
            installerName = installerName ?: f.installerName,
            installerSignatureStrokes = installerSignatureStrokes ?: f.installerSignatureStrokes,
            accountantName = accountantName ?: f.accountantName,
            managerName = managerName ?: f.managerName,
            supervisorName = supervisorName ?: f.supervisorName,
            customerSignatureStrokes = customerSignatureStrokes ?: f.customerSignatureStrokes,
            validationError = null,
          )
      )
    }
  }

  fun updateInverterSerial(primary: String? = null, confirm: String? = null) {
    _uiState.update { state ->
      val f = state.installationForm
      val cur = f.inverterSerial
      state.copy(
        installationForm =
          f.copy(
            inverterSerial =
              cur.copy(
                primaryValue = primary ?: cur.primaryValue,
                confirmValue = confirm ?: cur.confirmValue,
              ),
            validationError = null,
          )
      )
    }
  }

  fun updateGenerationMeterSerial(primary: String? = null, confirm: String? = null) {
    _uiState.update { state ->
      val f = state.installationForm
      val cur = f.generationMeterSerial
      state.copy(
        installationForm =
          f.copy(
            generationMeterSerial =
              cur.copy(
                primaryValue = primary ?: cur.primaryValue,
                confirmValue = confirm ?: cur.confirmValue,
              ),
            validationError = null,
          )
      )
    }
  }

  fun updatePanelSerial(panelId: String, primary: String? = null, confirm: String? = null) {
    _uiState.update { state ->
      val f = state.installationForm
      val updated =
        f.panelSerials.map { panel ->
          if (panel.id == panelId) {
            panel.copy(
              serial =
                panel.serial.copy(
                  primaryValue = primary ?: panel.serial.primaryValue,
                  confirmValue = confirm ?: panel.serial.confirmValue,
                )
            )
          } else {
            panel
          }
        }
      state.copy(installationForm = f.copy(panelSerials = updated, validationError = null))
    }
  }

  fun addPanelSerialRow() {
    _uiState.update { state ->
      val f = state.installationForm
      val nextIndex = f.panelSerials.size + 1
      val newEntry = PanelSerialEntry(id = "panel_${nextIndex}_${System.currentTimeMillis()}")
      state.copy(
        installationForm =
          f.copy(
            panelSerials = f.panelSerials + newEntry,
            validationError = null,
          )
      )
    }
  }

  fun removePanelSerialRow(panelId: String) {
    _uiState.update { state ->
      val f = state.installationForm
      if (f.panelSerials.size <= 2) return@update state
      state.copy(
        installationForm =
          f.copy(
            panelSerials = f.panelSerials.filterNot { it.id == panelId },
            validationError = null,
          )
      )
    }
  }

  private fun validateInstallationPart1(form: InstallationFormState): String? {
    if (form.consumerNo.isBlank()) {
      return "Please enter Consumer No. (Light Bill No)."
    }
    if (form.installationDate.isBlank()) {
      return "Please enter or select the Installation Date."
    }
    if (form.consumerName.isBlank()) {
      return "Please enter the Consumer Name."
    }
    val digits = form.mobileNo.filter { it.isDigit() }
    if (digits.length != 10) {
      return "Please enter a valid 10-digit Mobile No."
    }
    if (form.systemCapacity.isBlank() || form.systemType.isBlank()) {
      return "Please enter System Capacity and System Type."
    }
    val missingRequiredQty = form.products.firstOrNull { it.isQtyRequired && it.qty.isBlank() }
    if (missingRequiredQty != null) {
      return "Please enter QTY for ${missingRequiredQty.productModel}."
    }
    if (form.installerName.isBlank()) {
      return "Please enter the Installer Name."
    }
    if (form.installerSignatureStrokes.isEmpty()) {
      return "Please capture the Installer Signature."
    }
    if (form.customerSignatureStrokes.isEmpty()) {
      return "Please capture the Customer Sign and Stamp."
    }
    return null
  }

  private fun validateInstallationPart2(form: InstallationFormState): String? {
    if (!form.inverterSerial.isFilled) {
      return "Please enter and re-enter the Invertor Serial Number."
    }
    if (!form.inverterSerial.isMatch) {
      return "Invertor Serial Number entries do not match. Please verify both fields."
    }
    if (!form.generationMeterSerial.isFilled) {
      return "Please enter and re-enter the Generation Meter Serial Number."
    }
    if (!form.generationMeterSerial.isMatch) {
      return "Generation Meter Serial Number entries do not match. Please verify both fields."
    }
    if (form.panelSerials.size < 2) {
      return "At least 2 Panel Serial Numbers are required."
    }
    form.panelSerials.forEachIndexed { idx, panel ->
      val label = "Panel ${idx + 1} Serial Number"
      if (!panel.serial.isFilled) {
        return "Please enter and re-enter $label."
      }
      if (!panel.serial.isMatch) {
        return "$label entries do not match. Please verify both fields."
      }
    }
    return null
  }

  fun addInstallationPhotoSlot() {
    _uiState.update { state ->
      val f = state.installationForm
      val nextIdx = f.afterInstallationPhotos.size + 1
      val newSlot =
        InstallationPhotoItem(
          id = "site_photo_${nextIdx}_${System.currentTimeMillis()}",
          caption = "After Installation Photo $nextIdx",
        )
      state.copy(
        installationForm =
          f.copy(
            afterInstallationPhotos = f.afterInstallationPhotos + newSlot,
            validationError = null,
          )
      )
    }
  }

  fun removeInstallationPhotoSlot(photoId: String) {
    _uiState.update { state ->
      val f = state.installationForm
      if (f.afterInstallationPhotos.size <= 1) return@update state
      state.copy(
        installationForm =
          f.copy(
            afterInstallationPhotos = f.afterInstallationPhotos.filterNot { it.id == photoId },
            validationError = null,
          )
      )
    }
  }

  fun updateInstallationPhotoCaption(photoId: String, caption: String) {
    _uiState.update { state ->
      val f = state.installationForm
      val updated =
        f.afterInstallationPhotos.map { item ->
          if (item.id == photoId) item.copy(caption = caption) else item
        }
      state.copy(installationForm = f.copy(afterInstallationPhotos = updated))
    }
  }

  fun updateInstallationPhotoCapture(
    photoId: String,
    photoUri: String?,
    photoSizeKb: Int?,
    geoInfo: String,
    isCameraGeoStamped: Boolean,
  ) {
    _uiState.update { state ->
      val f = state.installationForm
      val updated =
        f.afterInstallationPhotos.map { item ->
          if (item.id == photoId) {
            item.copy(
              photoUri = photoUri,
              photoSizeKb = photoSizeKb,
              geoInfo = geoInfo,
              isCameraGeoStamped = isCameraGeoStamped,
            )
          } else {
            item
          }
        }
      state.copy(installationForm = f.copy(afterInstallationPhotos = updated))
    }
  }

  fun saveInstallationReport(): InstallationReport? {
    val form = _uiState.value.installationForm
    val part1Error = validateInstallationPart1(form)
    if (part1Error != null) {
      _uiState.update {
        it.copy(installationForm = form.copy(currentPage = 1, validationError = part1Error))
      }
      return null
    }
    val part2Error = validateInstallationPart2(form)
    if (part2Error != null) {
      _uiState.update {
        it.copy(installationForm = form.copy(currentPage = 2, validationError = part2Error))
      }
      return null
    }

    val report =
      InstallationReport(
        id = form.editingReportId ?: "inst_${System.currentTimeMillis()}",
        linkedSurveyId = form.linkedSurveyId,
        villageName = form.villageName.trim(),
        consumerNo = form.consumerNo.trim(),
        installationDate = form.installationDate.trim(),
        consumerName = form.consumerName.trim(),
        mobileNo = form.mobileNo.filter { it.isDigit() },
        systemCapacity = form.systemCapacity.trim(),
        systemType = form.systemType.trim(),
        products = form.products,
        installerName = form.installerName.trim(),
        installerSignatureStrokes = form.installerSignatureStrokes,
        accountantName = form.accountantName.trim(),
        managerName = form.managerName.trim(),
        supervisorName = form.supervisorName.trim(),
        customerSignatureStrokes = form.customerSignatureStrokes,
        inverterSerial = form.inverterSerial,
        generationMeterSerial = form.generationMeterSerial,
        panelSerials = form.panelSerials,
        afterInstallationPhotos = form.afterInstallationPhotos,
        updatedAtMillis = System.currentTimeMillis(),
      )

    surveyRepository.saveInstallationReport(report)
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.INSTALLATION_DASHBOARD,
        activeInstallationTab = InstallationTab.HOME,
        installationReports = surveyRepository.installationReports.value,
        installationForm = InstallationFormState(),
        snackbarMessage = "Installation Certificate saved for ${report.consumerName}.",
      )
    }
    return report
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
