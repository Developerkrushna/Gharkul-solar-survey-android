package com.example.data.remote

import com.example.model.CasteCategory
import com.example.model.ChecklistAnswers
import com.example.model.CustomerSurvey
import com.example.model.DocumentComplianceChecks
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.ExecutiveUser
import com.example.model.HouseType
import com.example.model.InstallationPhotoItem
import com.example.model.InstallationProductRow
import com.example.model.InstallationReport
import com.example.model.MaterialAndOfficeDetails
import com.example.model.MeterInfo
import com.example.model.NameMatchResult
import com.example.model.OverallStatus
import com.example.model.PanelSerialEntry
import com.example.model.PendingActionItem
import com.example.model.RoofType
import com.example.model.SerialVerificationField
import com.example.model.SiteSurveyDetails
import com.example.model.SolarCapacity
import com.example.model.SurveyCategory
import com.example.model.defaultInstallationProductRows
import com.example.model.defaultPanelSerialEntries
import org.json.JSONArray
import org.json.JSONObject

data class CustomVillageRecord(
  val id: String,
  val districtId: String,
  val districtNameMr: String,
  val talukaId: String,
  val talukaNameMr: String,
  val villageNameMr: String,
  val villageNameEn: String,
)

object SupabaseMappers {

  // ---- Signatures JSONB: List<List<Pair<Float, Float>>> <-> [[[x, y], ...], ...] ----

  fun strokesToJsonArray(strokes: List<List<Pair<Float, Float>>>): JSONArray {
    val arr = JSONArray()
    for (stroke in strokes) {
      val sArr = JSONArray()
      for (pt in stroke) {
        val pair = JSONArray()
        pair.put(pt.first.toDouble())
        pair.put(pt.second.toDouble())
        sArr.put(pair)
      }
      arr.put(sArr)
    }
    return arr
  }

  fun jsonArrayToStrokes(arr: JSONArray?): List<List<Pair<Float, Float>>> {
    if (arr == null) return emptyList()
    val result = mutableListOf<List<Pair<Float, Float>>>()
    for (i in 0 until arr.length()) {
      val sArr = arr.optJSONArray(i) ?: continue
      val stroke = mutableListOf<Pair<Float, Float>>()
      for (j in 0 until sArr.length()) {
        val pt = sArr.optJSONArray(j)
        if (pt != null && pt.length() >= 2) {
          stroke.add(pt.optDouble(0, 0.0).toFloat() to pt.optDouble(1, 0.0).toFloat())
        } else {
          val obj = sArr.optJSONObject(j)
          if (obj != null) {
            stroke.add(obj.optDouble("x", 0.0).toFloat() to obj.optDouble("y", 0.0).toFloat())
          }
        }
      }
      if (stroke.isNotEmpty()) result.add(stroke)
    }
    return result
  }

  // ---- Executive ----

  fun executiveFromJson(obj: JSONObject): ExecutiveUser {
    val fullName =
      obj.optString("name_mr").ifBlank { obj.optString("full_name") }
    val firstName =
      obj.optString("first_name_mr")
        .ifBlank { obj.optString("first_name") }
        .ifBlank { fullName.substringBefore(" ") }
    val role = obj.optString("role", "SURVEY_EXECUTIVE")
    val defaultRoleMr =
      when (role) {
        "SOLAR_INSTALLATION" -> "सोलर इन्स्टॉलेशन एक्झिक्युटिव्ह"
        "ADMIN" -> "ऑफिस अ‍ॅडमिन"
        else -> "फील्ड एक्झिक्युटिव्ह"
      }
    val roleMr =
      obj.optString("role_mr")
        .ifBlank { obj.optString("role_label_mr") }
        .ifBlank { defaultRoleMr }
    return ExecutiveUser(
      id = obj.optString("id"),
      authUserId = obj.optString("auth_user_id"),
      email = obj.optString("email"),
      nameMr = fullName,
      firstNameMr = firstName,
      mobile = obj.optString("mobile"),
      role = role,
      roleMr = roleMr,
      assignedDistrict = obj.optString("assigned_district"),
      assignedTaluka = obj.optString("assigned_taluka"),
      assignedVillage = obj.optString("assigned_village"),
      isActive = obj.optBoolean("is_active", true),
    )
  }

  // ---- Custom Villages ----

  fun customVillageFromJson(obj: JSONObject): CustomVillageRecord =
    CustomVillageRecord(
      id = obj.getString("id"),
      districtId = obj.getString("district_id"),
      districtNameMr = obj.optString("district_name_mr"),
      talukaId = obj.getString("taluka_id"),
      talukaNameMr = obj.optString("taluka_name_mr"),
      villageNameMr = obj.getString("village_name_mr"),
      villageNameEn = obj.optString("village_name_en").ifBlank { obj.getString("village_name_mr") },
    )

  fun customVillageToJson(record: CustomVillageRecord, executiveId: String?): JSONObject =
    JSONObject()
      .put("id", record.id)
      .put("district_id", record.districtId)
      .put("district_name_mr", record.districtNameMr)
      .put("taluka_id", record.talukaId)
      .put("taluka_name_mr", record.talukaNameMr)
      .put("village_name_mr", record.villageNameMr)
      .put("village_name_en", record.villageNameEn)
      .put("created_by", executiveId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)

  // ---- Customer Survey ----

  fun customerSurveyToJson(survey: CustomerSurvey): JSONObject {
    val chk = survey.checklistAnswers
    val checklistObj =
      JSONObject()
        .put("gharkulAndMeterSamePerson", chk.gharkulAndMeterSamePerson)
        .put("allDocsHaveSameName", chk.allDocsHaveSameName)
        .put("aadhaarNameChangeNeeded", chk.aadhaarNameChangeNeeded)
        .put("lightBillNameChangeNeeded", chk.lightBillNameChangeNeeded)
        .put("panNameChangeNeeded", chk.panNameChangeNeeded)
        .put("passbookNameChangeNeeded", chk.passbookNameChangeNeeded)
        .put("meterOnFamilyMemberName", chk.meterOnFamilyMemberName)
        .put("noElectricityConnection", chk.noElectricityConnection)

    val matchedArr = JSONArray(survey.matchedCategories.map { it.name })

    val pendingArr = JSONArray()
    survey.pendingActions.forEach { item ->
      pendingArr.put(
        JSONObject()
          .put("titleMr", item.titleMr)
          .put("responsibleMr", item.responsibleMr)
          .put("isCompanyAction", item.isCompanyAction)
      )
    }

    val signaturesObj =
      JSONObject()
        .put("customer", strokesToJsonArray(survey.materialAndOfficeDetails.customerSignatureStrokes))
        .put("surveyor", strokesToJsonArray(survey.materialAndOfficeDetails.surveyorSignatureStrokes))

    val meter = survey.meterInfo
    val site = survey.siteSurveyDetails
    val comp = survey.documentComplianceChecks
    val mat = survey.materialAndOfficeDetails

    return JSONObject()
      .put("id", survey.id)
      .put("executive_id", survey.executiveId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("surveyor_name", mat.surveyorName)
      .put("village_id", survey.villageId)
      .put("village_name_mr", survey.villageNameMr)
      .put("taluka_name_mr", survey.talukaNameMr)
      .put("district_name_mr", survey.districtNameMr)
      .put("survey_date", survey.surveyDate)
      .put("beneficiary_name", survey.beneficiaryName)
      .put("mobile", survey.mobile)
      .put("email_id", survey.emailId)
      .put("address_landmark", survey.addressLandmark)
      .put("gharkul_id", survey.gharkulId)
      .put("aadhaar_last4", survey.aadhaarLast4)
      .put("caste_category", survey.casteCategory.name)
      .put("house_type", survey.houseType.name)
      .put("capacity_kw", survey.capacity.kw)
      .put("gps_location", survey.gpsLocation?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("has_meter", meter.hasMeter)
      .put("consumer_number", meter.consumerNumber)
      .put("meter_holder_name", meter.meterHolderName)
      .put("same_as_beneficiary", meter.sameAsBeneficiary)
      .put("meter_relationship", meter.relationship)
      .put("is_holder_alive", meter.isHolderAlive)
      .put("is_pole_nearby_for_new_conn", meter.isPoleNearbyForNewConn ?: JSONObject.NULL)
      .put("interested_in_new_connection", meter.interestedInNewConnection ?: JSONObject.NULL)
      .put("roof_type", site.roofType?.name ?: JSONObject.NULL)
      .put("has_solar_panel_space_100_sqft", site.hasSolarPanelSpace100SqFt ?: JSONObject.NULL)
      .put("generation_meter_location", site.generationMeterLocation)
      .put("has_earthing_space", site.hasEarthingSpace ?: JSONObject.NULL)
      .put("notecam_roof_done", site.noteCamRoofBeforePhotoDone)
      .put("notecam_roof_photo_url", site.noteCamRoofBeforePhotoUri?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("notecam_roof_r2_key", site.noteCamRoofR2Key?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("notecam_roof_photo_kb", site.noteCamRoofBeforePhotoKb ?: JSONObject.NULL)
      .put("notecam_roof_geo_info", site.noteCamRoofGeoInfo)
      .put("notecam_earthing_done", site.noteCamEarthingWithOwnerDone)
      .put("notecam_earthing_photo_url", site.noteCamEarthingWithOwnerPhotoUri?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("notecam_earthing_r2_key", site.noteCamEarthingR2Key?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("notecam_earthing_photo_kb", site.noteCamEarthingWithOwnerPhotoKb ?: JSONObject.NULL)
      .put("notecam_earthing_geo_info", site.noteCamEarthingGeoInfo)
      .put("light_bill_is_residential", comp.lightBillIsResidential)
      .put("pan_name_dob_match_aadhaar", comp.panNameAndDobMatchAadhaar)
      .put("bank_is_nationalized_not_adcc", comp.bankIsNationalizedNotAdcc)
      .put("single_mobile_linked_all_docs", comp.singleMobileLinkedAllDocs)
      .put("aadhaar_seeded_to_bank", comp.aadhaarSeededToBank)
      .put("documents_complete_status", mat.documentsCompleteStatus)
      .put("earth_wire_meters", mat.earthWireMeters)
      .put("ac_cable_meters", mat.acCableMeters)
      .put("pipe_meters", mat.pipeMeters)
      .put("checklist_answers", checklistObj)
      .put("matched_categories", matchedArr)
      .put("suggested_category", survey.suggestedCategory.name)
      .put("final_category", survey.finalCategory.name)
      .put("override_reason", survey.overrideReason)
      .put("overall_status", survey.overallStatus.name)
      .put("pending_actions", pendingArr)
      .put("signatures", signaturesObj)
      .put("executive_remark", survey.executiveRemark)
      .put("gov_form_completed", survey.govFormCompleted)
      .put("gov_form_completed_at", survey.govFormCompletedAt?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("survey_date_label", survey.surveyDateLabel)
      .put("is_surveyed_today", survey.isSurveyedToday)
  }

  fun surveyDocumentsToJsonArray(surveyId: String, documents: Map<DocumentType, DocumentEntry>): JSONArray {
    val arr = JSONArray()
    documents.values.forEach { entry ->
      arr.put(
        JSONObject()
          .put("id", "${surveyId}_${entry.type.name}")
          .put("survey_id", surveyId)
          .put("doc_type", entry.type.name)
          .put("printed_name", entry.printedName)
          .put("photo_url", entry.photoUri?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
          .put("r2_object_key", entry.r2ObjectKey?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
          .put("photo_size_kb", entry.photoSizeKb ?: JSONObject.NULL)
          .put("is_size_valid", entry.isSizeValid)
          .put("legibility", entry.legibility.name)
          .put("match_result", entry.matchResult.name)
          .put("query_note", entry.queryNote)
      )
    }
    return arr
  }

  fun customerSurveyFromJson(obj: JSONObject): CustomerSurvey {
    val docsArray = obj.optJSONArray("survey_documents")
    val docMap = mutableMapOf<DocumentType, DocumentEntry>()
    if (docsArray != null) {
      for (i in 0 until docsArray.length()) {
        val dObj = docsArray.optJSONObject(i) ?: continue
        val type = runCatching { DocumentType.valueOf(dObj.getString("doc_type")) }.getOrNull() ?: continue
        docMap[type] =
          DocumentEntry(
            type = type,
            printedName = dObj.optString("printed_name"),
            photoUri = optNullableString(dObj, "photo_url"),
            r2ObjectKey = optNullableString(dObj, "r2_object_key"),
            photoSizeKb = optNullableInt(dObj, "photo_size_kb"),
            isSizeValid = dObj.optBoolean("is_size_valid", true),
            legibility = enumOrDefault(dObj.optString("legibility"), DocumentLegibility.MISSING),
            matchResult = enumOrDefault(dObj.optString("match_result"), NameMatchResult.NOT_ENTERED),
            queryNote = dObj.optString("query_note"),
          )
      }
    }

    val chkObj = obj.optJSONObject("checklist_answers") ?: JSONObject()
    val checklist =
      ChecklistAnswers(
        gharkulAndMeterSamePerson = chkObj.optBoolean("gharkulAndMeterSamePerson", true),
        allDocsHaveSameName = chkObj.optBoolean("allDocsHaveSameName", true),
        aadhaarNameChangeNeeded = chkObj.optBoolean("aadhaarNameChangeNeeded", false),
        lightBillNameChangeNeeded = chkObj.optBoolean("lightBillNameChangeNeeded", false),
        panNameChangeNeeded = chkObj.optBoolean("panNameChangeNeeded", false),
        passbookNameChangeNeeded = chkObj.optBoolean("passbookNameChangeNeeded", false),
        meterOnFamilyMemberName = chkObj.optBoolean("meterOnFamilyMemberName", false),
        noElectricityConnection = chkObj.optBoolean("noElectricityConnection", false),
      )

    val matchedCategories = mutableListOf<SurveyCategory>()
    val matchedArr = obj.optJSONArray("matched_categories")
    if (matchedArr != null) {
      for (i in 0 until matchedArr.length()) {
        runCatching { SurveyCategory.valueOf(matchedArr.getString(i)) }
          .getOrNull()
          ?.let { matchedCategories.add(it) }
      }
    }

    val pendingActions = mutableListOf<PendingActionItem>()
    val pendingArr = obj.optJSONArray("pending_actions")
    if (pendingArr != null) {
      for (i in 0 until pendingArr.length()) {
        val pObj = pendingArr.optJSONObject(i) ?: continue
        pendingActions.add(
          PendingActionItem(
            titleMr = pObj.optString("titleMr"),
            responsibleMr = pObj.optString("responsibleMr"),
            isCompanyAction = pObj.optBoolean("isCompanyAction", false),
          )
        )
      }
    }

    val sigObj = obj.optJSONObject("signatures") ?: JSONObject()
    val suggested = enumOrDefault(obj.optString("suggested_category"), SurveyCategory.CATEGORY_1)
    val finalCat = enumOrDefault(obj.optString("final_category"), suggested)

    return CustomerSurvey(
      id = obj.getString("id"),
      executiveId = optNullableString(obj, "executive_id"),
      villageId = obj.optString("village_id"),
      villageNameMr = obj.optString("village_name_mr"),
      talukaNameMr = obj.optString("taluka_name_mr"),
      districtNameMr = obj.optString("district_name_mr"),
      surveyDate = obj.optString("survey_date"),
      beneficiaryName = obj.optString("beneficiary_name"),
      mobile = obj.optString("mobile"),
      emailId = obj.optString("email_id"),
      addressLandmark = obj.optString("address_landmark"),
      gharkulId = obj.optString("gharkul_id"),
      aadhaarLast4 = obj.optString("aadhaar_last4"),
      casteCategory = enumOrDefault(obj.optString("caste_category"), CasteCategory.OPEN),
      houseType = enumOrDefault(obj.optString("house_type"), HouseType.SINGLE),
      capacity = SolarCapacity.KW_1,
      gpsLocation = optNullableString(obj, "gps_location"),
      meterInfo =
        MeterInfo(
          hasMeter = obj.optBoolean("has_meter", true),
          consumerNumber = obj.optString("consumer_number"),
          meterHolderName = obj.optString("meter_holder_name"),
          sameAsBeneficiary = obj.optBoolean("same_as_beneficiary", true),
          relationship = obj.optString("meter_relationship"),
          isHolderAlive = obj.optBoolean("is_holder_alive", true),
          isPoleNearbyForNewConn = optNullableBool(obj, "is_pole_nearby_for_new_conn"),
          interestedInNewConnection = optNullableBool(obj, "interested_in_new_connection"),
        ),
      siteSurveyDetails =
        SiteSurveyDetails(
          roofType = optNullableString(obj, "roof_type")?.let { runCatching { RoofType.valueOf(it) }.getOrNull() },
          hasSolarPanelSpace100SqFt = optNullableBool(obj, "has_solar_panel_space_100_sqft"),
          generationMeterLocation = obj.optString("generation_meter_location"),
          hasEarthingSpace = optNullableBool(obj, "has_earthing_space"),
          noteCamRoofBeforePhotoDone = obj.optBoolean("notecam_roof_done", false),
          noteCamRoofBeforePhotoUri = optNullableString(obj, "notecam_roof_photo_url"),
          noteCamRoofR2Key = optNullableString(obj, "notecam_roof_r2_key"),
          noteCamRoofBeforePhotoKb = optNullableInt(obj, "notecam_roof_photo_kb"),
          noteCamRoofGeoInfo = obj.optString("notecam_roof_geo_info"),
          noteCamEarthingWithOwnerDone = obj.optBoolean("notecam_earthing_done", false),
          noteCamEarthingWithOwnerPhotoUri = optNullableString(obj, "notecam_earthing_photo_url"),
          noteCamEarthingR2Key = optNullableString(obj, "notecam_earthing_r2_key"),
          noteCamEarthingWithOwnerPhotoKb = optNullableInt(obj, "notecam_earthing_photo_kb"),
          noteCamEarthingGeoInfo = obj.optString("notecam_earthing_geo_info"),
        ),
      documentComplianceChecks =
        DocumentComplianceChecks(
          lightBillIsResidential = obj.optBoolean("light_bill_is_residential", false),
          panNameAndDobMatchAadhaar = obj.optBoolean("pan_name_dob_match_aadhaar", false),
          bankIsNationalizedNotAdcc = obj.optBoolean("bank_is_nationalized_not_adcc", false),
          singleMobileLinkedAllDocs = obj.optBoolean("single_mobile_linked_all_docs", false),
          aadhaarSeededToBank = obj.optBoolean("aadhaar_seeded_to_bank", false),
        ),
      materialAndOfficeDetails =
        MaterialAndOfficeDetails(
          documentsCompleteStatus = obj.optBoolean("documents_complete_status", true),
          earthWireMeters = obj.optString("earth_wire_meters").ifBlank { "20 मीटर" },
          acCableMeters = obj.optString("ac_cable_meters").ifBlank { "10 मीटर" },
          pipeMeters = obj.optString("pipe_meters").ifBlank { "5 पाईप" },
          customerSignatureStrokes = jsonArrayToStrokes(sigObj.optJSONArray("customer")),
          surveyorName = obj.optString("surveyor_name"),
          surveyorSignatureStrokes = jsonArrayToStrokes(sigObj.optJSONArray("surveyor")),
        ),
      documents = docMap,
      checklistAnswers = checklist,
      matchedCategories = matchedCategories.ifEmpty { listOf(finalCat) },
      suggestedCategory = suggested,
      finalCategory = finalCat,
      overrideReason = obj.optString("override_reason"),
      overallStatus = enumOrDefault(obj.optString("overall_status"), OverallStatus.READY),
      pendingActions = pendingActions,
      executiveRemark = obj.optString("executive_remark"),
      govFormCompleted = obj.optBoolean("gov_form_completed", false),
      govFormCompletedAt = optNullableString(obj, "gov_form_completed_at"),
      surveyDateLabel = obj.optString("survey_date_label").ifBlank { obj.optString("survey_date") },
      isSurveyedToday = obj.optBoolean("is_surveyed_today", false),
    )
  }

  // ---- Installation Report & Photos ----

  fun installationReportToJson(report: InstallationReport): JSONObject {
    val productsArr = JSONArray()
    report.products.forEach { row ->
      productsArr.put(
        JSONObject()
          .put("rowId", row.rowId)
          .put("srNoDisplay", row.srNoDisplay)
          .put("productModel", row.productModel)
          .put("isSubRow", row.isSubRow)
          .put("isQtyRequired", row.isQtyRequired)
          .put("company", row.company)
          .put("size", row.size)
          .put("capacity", row.capacity)
          .put("qty", row.qty)
      )
    }

    val panelArr = JSONArray()
    report.panelSerials.forEach { panel ->
      panelArr.put(
        JSONObject()
          .put("id", panel.id)
          .put("primaryValue", panel.serial.primaryValue)
          .put("confirmValue", panel.serial.confirmValue)
      )
    }

    val signaturesObj =
      JSONObject()
        .put("installer", strokesToJsonArray(report.installerSignatureStrokes))
        .put("customer", strokesToJsonArray(report.customerSignatureStrokes))

    return JSONObject()
      .put("id", report.id)
      .put("linked_survey_id", report.linkedSurveyId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("installer_executive_id", report.installerExecutiveId?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
      .put("village_name", report.villageName)
      .put("consumer_no", report.consumerNo)
      .put("installation_date", report.installationDate)
      .put("consumer_name", report.consumerName)
      .put("mobile_no", report.mobileNo)
      .put("system_capacity", report.systemCapacity)
      .put("system_type", report.systemType)
      .put("products", productsArr)
      .put("installer_name", report.installerName)
      .put("supervisor_name", report.supervisorName)
      .put("manager_name", report.managerName)
      .put("signatures", signaturesObj)
      .put("inverter_serial_primary", report.inverterSerial.primaryValue)
      .put("inverter_serial_confirm", report.inverterSerial.confirmValue)
      .put("generation_meter_serial_primary", report.generationMeterSerial.primaryValue)
      .put("generation_meter_serial_confirm", report.generationMeterSerial.confirmValue)
      .put("panel_serial_numbers", panelArr)
      .put("updated_at_millis", report.updatedAtMillis)
  }

  fun installationPhotosToJsonArray(reportId: String, photos: List<InstallationPhotoItem>): JSONArray {
    val arr = JSONArray()
    photos.forEachIndexed { idx, item ->
      arr.put(
        JSONObject()
          .put("id", item.id)
          .put("report_id", reportId)
          .put("slot_index", idx)
          .put("caption", item.caption)
          .put("photo_url", item.photoUri?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
          .put("r2_object_key", item.r2ObjectKey?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
          .put("photo_size_kb", item.photoSizeKb ?: JSONObject.NULL)
          .put("geo_info", item.geoInfo)
          .put("is_camera_geo_stamped", item.isCameraGeoStamped)
      )
    }
    return arr
  }

  fun installationReportFromJson(obj: JSONObject): InstallationReport {
    val productsArr = obj.optJSONArray("products")
    val products =
      if (productsArr != null && productsArr.length() > 0) {
        buildList {
          for (i in 0 until productsArr.length()) {
            val pObj = productsArr.optJSONObject(i) ?: continue
            add(
              InstallationProductRow(
                rowId = pObj.optString("rowId"),
                srNoDisplay = pObj.optString("srNoDisplay"),
                productModel = pObj.optString("productModel"),
                isSubRow = pObj.optBoolean("isSubRow", false),
                isQtyRequired = pObj.optBoolean("isQtyRequired", false),
                company = pObj.optString("company"),
                size = pObj.optString("size"),
                capacity = pObj.optString("capacity"),
                qty = pObj.optString("qty"),
              )
            )
          }
        }
      } else {
        defaultInstallationProductRows()
      }

    val panelArr = obj.optJSONArray("panel_serial_numbers")
    val panelSerials =
      if (panelArr != null && panelArr.length() > 0) {
        buildList {
          for (i in 0 until panelArr.length()) {
            val sObj = panelArr.optJSONObject(i) ?: continue
            add(
              PanelSerialEntry(
                id = sObj.optString("id", "panel_${i + 1}"),
                serial =
                  SerialVerificationField(
                    primaryValue = sObj.optString("primaryValue"),
                    confirmValue = sObj.optString("confirmValue"),
                  ),
              )
            )
          }
        }
      } else {
        defaultPanelSerialEntries()
      }

    val photosArr = obj.optJSONArray("installation_photos")
    val photos = mutableListOf<Pair<Int, InstallationPhotoItem>>()
    if (photosArr != null) {
      for (i in 0 until photosArr.length()) {
        val ph = photosArr.optJSONObject(i) ?: continue
        photos.add(
          ph.optInt("slot_index", i) to
            InstallationPhotoItem(
              id = ph.optString("id", "site_photo_${i + 1}"),
              caption = ph.optString("caption").ifBlank { "Solar Site Photo (After Installation)" },
              photoUri = optNullableString(ph, "photo_url"),
              r2ObjectKey = optNullableString(ph, "r2_object_key"),
              photoSizeKb = optNullableInt(ph, "photo_size_kb"),
              geoInfo = ph.optString("geo_info"),
              isCameraGeoStamped = ph.optBoolean("is_camera_geo_stamped", false),
            )
        )
      }
    }

    val sigObj = obj.optJSONObject("signatures") ?: JSONObject()

    return InstallationReport(
      id = obj.getString("id"),
      linkedSurveyId = optNullableString(obj, "linked_survey_id"),
      installerExecutiveId = optNullableString(obj, "installer_executive_id"),
      villageName = obj.optString("village_name"),
      consumerNo = obj.optString("consumer_no"),
      installationDate = obj.optString("installation_date"),
      consumerName = obj.optString("consumer_name"),
      mobileNo = obj.optString("mobile_no"),
      systemCapacity = obj.optString("system_capacity").ifBlank { "1 KW" },
      systemType = obj.optString("system_type").ifBlank { "ONGRID" },
      products = products,
      installerName = obj.optString("installer_name"),
      installerSignatureStrokes = jsonArrayToStrokes(sigObj.optJSONArray("installer")),
      supervisorName = obj.optString("supervisor_name"),
      managerName = obj.optString("manager_name"),
      customerSignatureStrokes = jsonArrayToStrokes(sigObj.optJSONArray("customer")),
      inverterSerial =
        SerialVerificationField(
          primaryValue = obj.optString("inverter_serial_primary"),
          confirmValue = obj.optString("inverter_serial_confirm"),
        ),
      generationMeterSerial =
        SerialVerificationField(
          primaryValue = obj.optString("generation_meter_serial_primary"),
          confirmValue = obj.optString("generation_meter_serial_confirm"),
        ),
      panelSerials = panelSerials,
      afterInstallationPhotos = photos.sortedBy { it.first }.map { it.second },
      updatedAtMillis = obj.optLong("updated_at_millis", System.currentTimeMillis()),
    )
  }

  private fun optNullableString(obj: JSONObject, key: String): String? {
    if (!obj.has(key) || obj.isNull(key)) return null
    val s = obj.optString(key)
    return s.takeIf { it.isNotBlank() && it != "null" }
  }

  private fun optNullableInt(obj: JSONObject, key: String): Int? =
    if (!obj.has(key) || obj.isNull(key)) null else obj.optInt(key)

  private fun optNullableBool(obj: JSONObject, key: String): Boolean? =
    if (!obj.has(key) || obj.isNull(key)) null else obj.optBoolean(key)

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, fallback: T): T =
    runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
}
