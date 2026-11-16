package com.example.data

import com.example.domain.SurveyRuleEngine
import com.example.model.CasteCategory
import com.example.model.CustomerSurvey
import com.example.model.DocumentComplianceChecks
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.ExecutiveUser
import com.example.model.HouseType
import com.example.model.MaterialAndOfficeDetails
import com.example.model.MeterInfo
import com.example.model.NameMatchResult
import com.example.model.RoofType
import com.example.model.SiteSurveyDetails
import com.example.model.SolarCapacity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SurveyRepository {
  val surveys: StateFlow<List<CustomerSurvey>>
  fun saveSurvey(survey: CustomerSurvey)
  fun deleteSurvey(surveyId: String): Boolean
  fun getExecutive(): ExecutiveUser
}

class InMemorySurveyRepository : SurveyRepository {

  private val _surveys = MutableStateFlow(seedSurveys())
  override val surveys: StateFlow<List<CustomerSurvey>> = _surveys.asStateFlow()

  override fun getExecutive(): ExecutiveUser =
    ExecutiveUser(
      nameMr = "रमेश कुलकर्णी",
      firstNameMr = "रमेश",
      email = "ramesh.k@gharkulsolar.in",
      mobile = "9823011223",
      roleMr = "फील्ड एक्झिक्युटिव्ह",
    )

  override fun saveSurvey(survey: CustomerSurvey) {
    _surveys.update { current ->
      val idx = current.indexOfFirst { it.id == survey.id }
      if (idx >= 0) {
        current.toMutableList().apply { set(idx, survey) }
      } else {
        listOf(survey) + current
      }
    }
  }

  override fun deleteSurvey(surveyId: String): Boolean {
    var removed = false
    _surveys.update { current ->
      val next = current.filterNot { it.id == surveyId }
      removed = next.size < current.size
      next
    }
    return removed
  }

  private fun seedSurveys(): List<CustomerSurvey> {
    return listOf(
      buildSeededSurvey(
        id = "cust_1",
        villageId = "ng_19",
        villageNameMr = "हिवरे बाजार",
        talukaNameMr = "नगर",
        districtNameMr = "अहिल्यानगर",
        surveyDate = "15 / 11 / 2026",
        beneficiaryName = "सुनीता शिंदे",
        mobile = "9822456710",
        emailId = "sunita.shinde@gmail.com",
        address = "मारुती मंदिराजवळ, प्रभाग २",
        gharkulId = "GH-AHN-1042",
        aadhaarLast4 = "4821",
        caste = CasteCategory.OPEN,
        houseType = HouseType.SINGLE,
        gps = "19.0481° N, 74.7392° E",
        meterInfo = MeterInfo(
          hasMeter = true,
          consumerNumber = "18203948120",
          meterHolderName = "सुनीता शिंदे",
          sameAsBeneficiary = true,
          isPoleNearbyForNewConn = true,
          interestedInNewConnection = false,
        ),
        site = SiteSurveyDetails(
          roofType = RoofType.RCC,
          hasSolarPanelSpace100SqFt = true,
          generationMeterLocation = "मुख्य दरवाजाजवळ भिंतीवर",
          hasEarthingSpace = true,
          noteCamRoofBeforePhotoDone = true,
          noteCamEarthingWithOwnerDone = true,
        ),
        docs = mapOf(
          DocumentType.LIGHT_BILL to DocumentEntry(DocumentType.LIGHT_BILL, "सुनीता शिंदे", null, 345, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "सुनीता शिंदे", null, 312, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PAN to DocumentEntry(DocumentType.PAN, "सुनीता शिंदे", null, 284, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "सुनीता शिंदे", null, 298, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "सुनीता शिंदे", null, 330, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "सुनीता शिंदे", null, 240, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        ),
        remark = "सर्व कागदपत्रे स्पष्ट आणि नावे समान आहेत.",
        dateLabel = "आज",
        isToday = true,
      ),
      buildSeededSurvey(
        id = "cust_2",
        villageId = "ng_19",
        villageNameMr = "हिवरे बाजार",
        talukaNameMr = "नगर",
        districtNameMr = "अहिल्यानगर",
        surveyDate = "15 / 11 / 2026",
        beneficiaryName = "रामदास पवार",
        mobile = "9765120394",
        emailId = "ramdas.pawar@gmail.com",
        address = "ग्रामपंचायत रोड, हिवरे बाजार",
        gharkulId = "GH-AHN-1043",
        aadhaarLast4 = "7319",
        caste = CasteCategory.SC,
        houseType = HouseType.DUPLEX,
        gps = "19.0493° N, 74.7410° E",
        meterInfo = MeterInfo(
          hasMeter = true,
          consumerNumber = "18203948195",
          meterHolderName = "रामदास तुकाराम पवार",
          sameAsBeneficiary = true,
          isPoleNearbyForNewConn = true,
          interestedInNewConnection = false,
        ),
        site = SiteSurveyDetails(
          roofType = RoofType.TIN_SHEET,
          hasSolarPanelSpace100SqFt = true,
          generationMeterLocation = "वरच्या मजल्याची गॅलरी",
          hasEarthingSpace = true,
          noteCamRoofBeforePhotoDone = true,
          noteCamEarthingWithOwnerDone = true,
        ),
        docs = mapOf(
          DocumentType.LIGHT_BILL to DocumentEntry(DocumentType.LIGHT_BILL, "रामदास तुकाराम पवार", null, 360, true, DocumentLegibility.CLEAR, NameMatchResult.MISMATCH),
          DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "रामदास पवार", null, 320, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PAN to DocumentEntry(DocumentType.PAN, "रामदास तु. पवार", null, 290, true, DocumentLegibility.CLEAR, NameMatchResult.MISMATCH),
          DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "रामदास पवार", null, 310, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "रामदास पवार", null, 350, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "रामदास पवार", null, 225, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.CASTE_CERTIFICATE to DocumentEntry(DocumentType.CASTE_CERTIFICATE, "रामदास पवार", null, 280, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        ),
        remark = "पॅन कार्ड आणि लाईट बिलावर मधले नाव बदलणे आवश्यक.",
        dateLabel = "आज",
        isToday = true,
      ),
      buildSeededSurvey(
        id = "cust_3",
        villageId = "ng_19",
        villageNameMr = "हिवरे बाजार",
        talukaNameMr = "नगर",
        districtNameMr = "अहिल्यानगर",
        surveyDate = "15 / 11 / 2026",
        beneficiaryName = "लक्ष्मी जाधव",
        mobile = "9850118822",
        emailId = "laxmi.jadhav@gmail.com",
        address = "जि.प. शाळेसमोर",
        gharkulId = "GH-AHN-1044",
        aadhaarLast4 = "9054",
        caste = CasteCategory.OPEN,
        houseType = HouseType.SINGLE,
        gps = "19.0472° N, 74.7365° E",
        meterInfo = MeterInfo(
          hasMeter = true,
          consumerNumber = "18203948231",
          meterHolderName = "बबन जाधव",
          sameAsBeneficiary = false,
          relationship = "पती",
          isHolderAlive = true,
          isPoleNearbyForNewConn = true,
          interestedInNewConnection = false,
        ),
        site = SiteSurveyDetails(
          roofType = RoofType.RCC,
          hasSolarPanelSpace100SqFt = true,
          generationMeterLocation = "पोर्चच्या बाजूला",
          hasEarthingSpace = true,
          noteCamRoofBeforePhotoDone = true,
          noteCamEarthingWithOwnerDone = true,
        ),
        docs = mapOf(
          DocumentType.LIGHT_BILL to DocumentEntry(DocumentType.LIGHT_BILL, "बबन जाधव", null, 340, true, DocumentLegibility.CLEAR, NameMatchResult.MISMATCH),
          DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "लक्ष्मी जाधव", null, 305, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PAN to DocumentEntry(DocumentType.PAN, "लक्ष्मी जाधव", null, 275, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "लक्ष्मी जाधव", null, 315, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "", null, null, true, DocumentLegibility.MISSING, NameMatchResult.NOT_ENTERED),
          DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "लक्ष्मी जाधव", null, 240, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.RATION_CARD to DocumentEntry(DocumentType.RATION_CARD, "लक्ष्मी बबन जाधव", null, 355, true, DocumentLegibility.CLEAR, NameMatchResult.CLOSE_MATCH),
        ),
        remark = "पतीच्या नावावर मीटर; ७/१२ उतारा किंवा NOC बाकी.",
        dateLabel = "आज",
        isToday = true,
      ),
      buildSeededSurvey(
        id = "cust_4",
        villageId = "ng_19",
        villageNameMr = "हिवरे बाजार",
        talukaNameMr = "नगर",
        districtNameMr = "अहिल्यानगर",
        surveyDate = "14 / 11 / 2026",
        beneficiaryName = "गणेश मोरे",
        mobile = "9422883344",
        emailId = "ganesh.more@gmail.com",
        address = "नवीन वस्ती, हिवरे बाजार",
        gharkulId = "GH-AHN-1045",
        aadhaarLast4 = "2187",
        caste = CasteCategory.ST,
        houseType = HouseType.SINGLE,
        gps = null,
        meterInfo = MeterInfo(
          hasMeter = false,
          sameAsBeneficiary = false,
          isPoleNearbyForNewConn = true,
          interestedInNewConnection = true,
        ),
        site = SiteSurveyDetails(
          roofType = RoofType.CEMENT_SHEET,
          hasSolarPanelSpace100SqFt = true,
          generationMeterLocation = "बाहेरील व्हरांडा",
          hasEarthingSpace = true,
          noteCamRoofBeforePhotoDone = true,
          noteCamEarthingWithOwnerDone = false,
        ),
        docs = mapOf(
          DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "गणेश मोरे", null, 290, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PAN to DocumentEntry(DocumentType.PAN, "गणेश मोरे", null, 260, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "गणेश मोरे", null, 280, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "गणेश मोरे", null, 310, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "गणेश मोरे", null, 220, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.CASTE_CERTIFICATE to DocumentEntry(DocumentType.CASTE_CERTIFICATE, "गणेश मोरे", null, 265, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        ),
        remark = "नवीन वीज कनेक्शनसाठी इच्छुक; जवळ पोल उपलब्ध आहे.",
        dateLabel = "काल",
        isToday = false,
      ),
      buildSeededSurvey(
        id = "cust_5",
        villageId = "cs_6",
        villageNameMr = "करमाड",
        talukaNameMr = "छत्रपती संभाजीनगर",
        districtNameMr = "छत्रपती संभाजीनगर",
        surveyDate = "15 / 11 / 2026",
        beneficiaryName = "कविता देशमुख",
        mobile = "9823554411",
        emailId = "kavita.deshmukh@gmail.com",
        address = "बस स्थानकाजवळ, करमाड",
        gharkulId = "GH-CSN-2011",
        aadhaarLast4 = "6630",
        caste = CasteCategory.OPEN,
        houseType = HouseType.DUPLEX,
        gps = "19.8902° N, 75.5018° E",
        meterInfo = MeterInfo(
          hasMeter = true,
          consumerNumber = "22401199882",
          meterHolderName = "कविता देशमुख",
          sameAsBeneficiary = true,
          isPoleNearbyForNewConn = true,
          interestedInNewConnection = false,
        ),
        site = SiteSurveyDetails(
          roofType = RoofType.RCC,
          hasSolarPanelSpace100SqFt = true,
          generationMeterLocation = "तळमजला मुख्य मीटर बॉक्सशेजारी",
          hasEarthingSpace = true,
          noteCamRoofBeforePhotoDone = true,
          noteCamEarthingWithOwnerDone = true,
        ),
        docs = mapOf(
          DocumentType.LIGHT_BILL to DocumentEntry(DocumentType.LIGHT_BILL, "कविता देशमुख", null, 355, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "कविता देशमुख", null, 340, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PAN to DocumentEntry(DocumentType.PAN, "कविता देशमुख", null, 310, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "कविता देशमुख", null, 325, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "कविता देशमुख", null, 380, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
          DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "कविता देशमुख", null, 240, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        ),
        remark = "सर्व कागदपत्रे व साईट सर्व्हे पूर्ण.",
        dateLabel = "आज",
        isToday = true,
      ),
    )
  }

  private fun buildSeededSurvey(
    id: String,
    villageId: String,
    villageNameMr: String,
    talukaNameMr: String,
    districtNameMr: String,
    surveyDate: String,
    beneficiaryName: String,
    mobile: String,
    emailId: String,
    address: String,
    gharkulId: String,
    aadhaarLast4: String,
    caste: CasteCategory,
    houseType: HouseType,
    gps: String?,
    meterInfo: MeterInfo,
    site: SiteSurveyDetails,
    docs: Map<DocumentType, DocumentEntry>,
    remark: String,
    dateLabel: String,
    isToday: Boolean,
  ): CustomerSurvey {
    val checklist = SurveyRuleEngine.deriveChecklist(beneficiaryName, meterInfo, docs)
    val (category, matched) = SurveyRuleEngine.suggestCategoryWithPriority(checklist)
    val pending = SurveyRuleEngine.computePendingActions(category, meterInfo, caste, checklist, docs)
    val status = SurveyRuleEngine.computeOverallStatus(category, pending)
    return CustomerSurvey(
      id = id,
      villageId = villageId,
      villageNameMr = villageNameMr,
      talukaNameMr = talukaNameMr,
      districtNameMr = districtNameMr,
      surveyDate = surveyDate,
      beneficiaryName = beneficiaryName,
      mobile = mobile,
      emailId = emailId,
      addressLandmark = address,
      gharkulId = gharkulId,
      aadhaarLast4 = aadhaarLast4,
      casteCategory = caste,
      houseType = houseType,
      capacity = SolarCapacity.KW_1,
      gpsLocation = gps,
      meterInfo = meterInfo,
      siteSurveyDetails = site,
      documentComplianceChecks = DocumentComplianceChecks(),
      materialAndOfficeDetails = MaterialAndOfficeDetails(documentsCompleteStatus = pending.isEmpty()),
      documents = docs,
      checklistAnswers = checklist,
      matchedCategories = matched,
      suggestedCategory = category,
      finalCategory = category,
      overallStatus = status,
      pendingActions = pending,
      executiveRemark = remark,
      surveyDateLabel = dateLabel,
      isSurveyedToday = isToday,
    )
  }
}
