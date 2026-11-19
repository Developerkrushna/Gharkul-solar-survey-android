package com.example.model

enum class LoginRole {
  SURVEY_EXECUTIVE,
  SOLAR_INSTALLATION,
}

enum class AppScreen {
  LOGIN,
  VILLAGE_SELECT,
  MAIN_APP,
  INSTALLATION_DASHBOARD,
  INSTALLATION_FORM,
}

enum class MainTab {
  HOME,
  CUSTOMERS,
  PROFILE,
}

enum class CasteCategory(val labelMr: String) {
  OPEN("OPEN"),
  SC("SC"),
  ST("ST"),
}

enum class HouseType(val labelMr: String) {
  SINGLE("1 मजली"),
  DUPLEX("2 मजली"),
}

enum class RoofType(val labelMr: String) {
  RCC("RCC (सिमेंट धाबा)"),
  TIN_SHEET("पत्रा"),
  TILED("कौल"),
  CEMENT_SHEET("सिमेंट पत्रा"),
  UNDER_CONSTRUCTION("बांधकाम सुरू आहे"),
}

enum class SolarCapacity(val kw: Int, val labelMr: String) {
  KW_1(1, "1 kW"),
}

enum class DocumentType(
  val labelMr: String,
  val subtitleMr: String,
  val isRequiredCore: Boolean,
  val responsiblePartyOnMismatch: ResponsibleParty,
) {
  LIGHT_BILL(
    labelMr = "लाईट बिल",
    subtitleMr = "घरगुती (Residential) अनिवार्य.",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.COMPANY,
  ),
  AADHAAR(
    labelMr = "आधार कार्ड",
    subtitleMr = "स्पष्ट व वाचण्यायोग्य दोन्ही बाजूंचा फोटो.",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  PAN(
    labelMr = "पॅन कार्ड",
    subtitleMr = "आधार आणि पॅनवरील नाव व जन्मतारीख सारखीच असावी.",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.COMPANY,
  ),
  BANK_PASSBOOK(
    labelMr = "बँक पासबुक किंवा कॅन्सल चेक",
    subtitleMr = "राष्ट्रीयकृत बँक खाते असावे (ADCC बँकेत खाते नसावे). खाते क्र. व IFSC स्पष्ट दिसावा.",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  OWNERSHIP_PROOF(
    labelMr = "मालकी हक्काचा पुरावा",
    subtitleMr = "उदा. ७/१२ उतारा, ८-अ, किंवा घरपट्टी पावती (बँक कर्ज / मीटर ट्रान्सफरसाठी).",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  PASSPORT_PHOTO(
    labelMr = "पासपोर्ट साईज फोटो",
    subtitleMr = "लाभार्थ्याचा स्पष्ट पासपोर्ट आकाराचा फोटो.",
    isRequiredCore = true,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  CASTE_CERTIFICATE(
    labelMr = "जातीचा दाखला (SC / ST)",
    subtitleMr = "SC किंवा ST प्रवर्गासाठी जातीचा दाखला अनिवार्य आहे.",
    isRequiredCore = false,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  RATION_CARD(
    labelMr = "रेशन कार्ड",
    subtitleMr = "मीटर घरातील दुसऱ्या व्यक्तीच्या नावावर असल्यास आवश्यक.",
    isRequiredCore = false,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
  DEATH_CERTIFICATE(
    labelMr = "मृत्यू दाखला",
    subtitleMr = "मीटर धारक व्यक्ती मृत असल्यास अनिवार्य.",
    isRequiredCore = false,
    responsiblePartyOnMismatch = ResponsibleParty.CUSTOMER,
  ),
}

enum class ResponsibleParty(val labelMr: String) {
  CUSTOMER("ग्राहकाने बदल करून देणे"),
  COMPANY("आम्ही बदल करून देऊ"),
}

enum class DocumentLegibility(val labelMr: String) {
  CLEAR("स्पष्ट"),
  UNCLEAR("अस्पष्ट"),
  MISSING("उपलब्ध नाही"),
}

enum class NameMatchResult(val labelMr: String) {
  MATCH("नाव जुळले"),
  CLOSE_MATCH("जवळपास जुळते (तपासा)"),
  MISMATCH("नाव जुळत नाही"),
  NOT_ENTERED("नाव प्रविष्ट नाही"),
}

enum class SurveyCategory(
  val number: Int,
  val titleMr: String,
  val shortTagMr: String,
  val descriptionMr: String,
) {
  CATEGORY_1(
    number = 1,
    titleMr = "श्रेणी 1 · घरकुल व मीटर एकाच व्यक्तीच्या नावावर",
    shortTagMr = "श्रेणी 1 · सर्व नाव समान",
    descriptionMr = "घरकुल व मीटर एकाच व्यक्तीच्या नावावर आहे. बँक पासबुक, आधार कार्ड, पॅन कार्ड, लाईट बिल सर्व नाव समान आहे.",
  ),
  CATEGORY_2(
    number = 2,
    titleMr = "श्रेणी 2 · नावामध्ये बदल (आधार, लाईट बिल, पॅन कार्ड)",
    shortTagMr = "श्रेणी 2 · नावामध्ये बदल",
    descriptionMr = "आधार, लाईट बिल, पॅन कार्ड किंवा बँक पासबुकवरील नावामध्ये बदल (तफावत) आहे.",
  ),
  CATEGORY_3(
    number = 3,
    titleMr = "श्रेणी 3 · मीटर घरातील दुसऱ्या व्यक्तीच्या नावे",
    shortTagMr = "श्रेणी 3 · मीटर दुसऱ्याच्या नावे",
    descriptionMr = "घरकुल नावावर आहे पण लाईट मीटर कनेक्शन घरातील दुसऱ्या व्यक्तीच्या नावे आहे.",
  ),
  CATEGORY_4(
    number = 4,
    titleMr = "श्रेणी 4 · लाईट कनेक्शन नाही",
    shortTagMr = "श्रेणी 4 · लाईट कनेक्शन नाही",
    descriptionMr = "घरकुल नावावर आहे पण लाईट कनेक्शन नाही.",
  ),
}

enum class OverallStatus(val labelMr: String, val chipLabelMr: String) {
  READY("तयार", "तयार"),
  PENDING_CUSTOMER("प्रलंबित · ग्राहक कार्यवाही", "अपूर्ण"),
  PENDING_COMPANY("प्रलंबित · कंपनी कार्यवाही", "अपूर्ण"),
}

data class District(
  val id: String,
  val nameMr: String,
  val nameEn: String,
  val talukas: List<Taluka>,
)

data class Taluka(
  val id: String,
  val nameMr: String,
  val nameEn: String,
  val villages: List<Village>,
)

data class Village(
  val id: String,
  val nameMr: String,
  val nameEn: String,
)

data class DocumentEntry(
  val type: DocumentType,
  val printedName: String = "",
  val photoUri: String? = null,
  val photoSizeKb: Int? = null,
  val isSizeValid: Boolean = true,
  val legibility: DocumentLegibility = DocumentLegibility.MISSING,
  val matchResult: NameMatchResult = NameMatchResult.NOT_ENTERED,
  val queryNote: String = "",
)

data class MeterInfo(
  val hasMeter: Boolean = true,
  val consumerNumber: String = "",
  val meterHolderName: String = "",
  val sameAsBeneficiary: Boolean = true,
  val relationship: String = "",
  val isHolderAlive: Boolean = true,
  val isPoleNearbyForNewConn: Boolean? = null,
  val interestedInNewConnection: Boolean? = null,
)

data class SiteSurveyDetails(
  val roofType: RoofType? = null,
  val hasSolarPanelSpace100SqFt: Boolean? = null,
  val generationMeterLocation: String = "",
  val hasEarthingSpace: Boolean? = null,
  val noteCamRoofBeforePhotoDone: Boolean = false,
  val noteCamRoofBeforePhotoUri: String? = null,
  val noteCamRoofBeforePhotoKb: Int? = null,
  val noteCamRoofGeoInfo: String = "",
  val noteCamEarthingWithOwnerDone: Boolean = false,
  val noteCamEarthingWithOwnerPhotoUri: String? = null,
  val noteCamEarthingWithOwnerPhotoKb: Int? = null,
  val noteCamEarthingGeoInfo: String = "",
)

data class DocumentComplianceChecks(
  val lightBillIsResidential: Boolean = false,
  val panNameAndDobMatchAadhaar: Boolean = false,
  val bankIsNationalizedNotAdcc: Boolean = false,
  val singleMobileLinkedAllDocs: Boolean = false,
  val aadhaarSeededToBank: Boolean = false,
)

data class MaterialAndOfficeDetails(
  val documentsCompleteStatus: Boolean = true, // पूर्ण / अपूर्ण
  val earthWireMeters: String = "20 मीटर",
  val acCableMeters: String = "10 मीटर",
  val pipeMeters: String = "5 पाईप",
  val customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
  val surveyorName: String = "रमेश कुलकर्णी",
  val surveyorSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
)

/**
 * Explicit Yes/No answers covering all 4 Categories (used with 4 > 3 > 2 > 1 priority rule).
 */
data class ChecklistAnswers(
  // Category 1 requirements
  val gharkulAndMeterSamePerson: Boolean = true,
  val allDocsHaveSameName: Boolean = true,
  // Category 2 requirements (name changes)
  val aadhaarNameChangeNeeded: Boolean = false,
  val lightBillNameChangeNeeded: Boolean = false,
  val panNameChangeNeeded: Boolean = false,
  val passbookNameChangeNeeded: Boolean = false,
  // Category 3 requirement
  val meterOnFamilyMemberName: Boolean = false,
  // Category 4 requirement
  val noElectricityConnection: Boolean = false,
)

data class PendingActionItem(
  val titleMr: String,
  val responsibleMr: String,
  val isCompanyAction: Boolean,
)

data class CustomerSurvey(
  val id: String,
  val villageId: String,
  val villageNameMr: String,
  val talukaNameMr: String,
  val districtNameMr: String,
  val surveyDate: String,
  val beneficiaryName: String,
  val mobile: String,
  val emailId: String,
  val addressLandmark: String,
  val gharkulId: String,
  val aadhaarLast4: String,
  val casteCategory: CasteCategory,
  val houseType: HouseType,
  val capacity: SolarCapacity,
  val gpsLocation: String?,
  val meterInfo: MeterInfo,
  val siteSurveyDetails: SiteSurveyDetails,
  val documentComplianceChecks: DocumentComplianceChecks,
  val materialAndOfficeDetails: MaterialAndOfficeDetails,
  val documents: Map<DocumentType, DocumentEntry>,
  val checklistAnswers: ChecklistAnswers,
  val matchedCategories: List<SurveyCategory>,
  val suggestedCategory: SurveyCategory,
  val finalCategory: SurveyCategory,
  val overrideReason: String = "",
  val overallStatus: OverallStatus,
  val pendingActions: List<PendingActionItem>,
  val executiveRemark: String = "",
  val surveyDateLabel: String,
  val isSurveyedToday: Boolean = true,
)

data class VillageStats(
  val surveyedToday: Int,
  val totalCount: Int,
  val readyCount: Int,
  val pendingCount: Int,
)

data class ReportBRow(
  val srNo: String,
  val labelMr: String,
  val total: Int,
  val openCount: Int,
  val scCount: Int,
  val stCount: Int,
)

data class ExecutiveUser(
  val nameMr: String,
  val firstNameMr: String,
  val email: String,
  val mobile: String,
  val roleMr: String,
)
