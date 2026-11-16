package com.example.domain

import com.example.model.CasteCategory
import com.example.model.ChecklistAnswers
import com.example.model.CustomerSurvey
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.MeterInfo
import com.example.model.NameMatchResult
import com.example.model.OverallStatus
import com.example.model.PendingActionItem
import com.example.model.ResponsibleParty
import com.example.model.SurveyCategory

object SurveyRuleEngine {

  private val honorificRegex =
    Regex("""^(श्री\.?|श्रीमती\.?|सौ\.?|कु\.?|mr\.?|mrs\.?|smt\.?)\s+""", RegexOption.IGNORE_CASE)

  fun matchName(referenceName: String, documentName: String): NameMatchResult {
    val refRaw = referenceName.trim()
    val docRaw = documentName.trim()
    if (docRaw.isEmpty()) return NameMatchResult.NOT_ENTERED
    if (refRaw.isEmpty()) return NameMatchResult.MISMATCH

    val refClean = basicClean(refRaw)
    val docClean = basicClean(docRaw)
    if (refClean == docClean) return NameMatchResult.MATCH

    val refNorm = normalizePhonetic(refClean)
    val docNorm = normalizePhonetic(docClean)
    if (refNorm == docNorm) return NameMatchResult.CLOSE_MATCH

    val refTokens = refNorm.split(" ").filter { it.isNotBlank() }.toSet()
    val docTokens = docNorm.split(" ").filter { it.isNotBlank() }.toSet()
    if (refTokens.isNotEmpty() && refTokens == docTokens) {
      return NameMatchResult.CLOSE_MATCH
    }
    val intersection = refTokens.intersect(docTokens).size
    val union = refTokens.union(docTokens).size
    if (union > 0 && intersection.toFloat() / union.toFloat() >= 0.66f) {
      return NameMatchResult.CLOSE_MATCH
    }
    return NameMatchResult.MISMATCH
  }

  private fun basicClean(input: String): String {
    return input
      .replace(honorificRegex, "")
      .replace(Regex("""\s+"""), " ")
      .trim()
      .lowercase()
  }

  private fun normalizePhonetic(input: String): String {
    return input
      .replace("ी", "ि")
      .replace("ू", "ु")
      .replace("ई", "इ")
      .replace("ऊ", "उ")
      .replace("श", "स")
      .replace("ष", "स")
      .replace("ण", "न")
      .replace("ं", "न")
      .replace("ee", "i")
      .replace("oo", "u")
      .replace("aa", "a")
  }

  /**
   * Returns all required & conditional document types based on PDF checklist, caste (SC/ST), and meter ownership.
   */
  fun requiredDocumentsFor(
    meterInfo: MeterInfo,
    casteCategory: CasteCategory = CasteCategory.OPEN,
  ): List<DocumentType> {
    val list = mutableListOf(
      DocumentType.LIGHT_BILL,
      DocumentType.AADHAAR,
      DocumentType.PAN,
      DocumentType.BANK_PASSBOOK,
      DocumentType.OWNERSHIP_PROOF,
      DocumentType.PASSPORT_PHOTO,
    )
    if (casteCategory == CasteCategory.SC || casteCategory == CasteCategory.ST) {
      list.add(DocumentType.CASTE_CERTIFICATE)
    }
    if (meterInfo.hasMeter && !meterInfo.sameAsBeneficiary) {
      list.add(DocumentType.RATION_CARD)
      if (!meterInfo.isHolderAlive) {
        list.add(DocumentType.DEATH_CERTIFICATE)
      }
    }
    return list
  }

  /**
   * Derives initial Yes/No answers across all 4 categories from Step 1 & Step 2 inputs.
   */
  fun deriveChecklist(
    beneficiaryName: String,
    meterInfo: MeterInfo,
    documents: Map<DocumentType, DocumentEntry>,
  ): ChecklistAnswers {
    val aadhaarName = documents[DocumentType.AADHAAR]?.printedName.orEmpty().ifBlank { beneficiaryName }
    val gharkulMatchesAadhaar =
      matchName(beneficiaryName, documents[DocumentType.AADHAAR]?.printedName.orEmpty()) == NameMatchResult.MATCH
    val panMatches =
      matchName(aadhaarName, documents[DocumentType.PAN]?.printedName.orEmpty()) == NameMatchResult.MATCH
    val billMatches =
      matchName(aadhaarName, documents[DocumentType.LIGHT_BILL]?.printedName.orEmpty()) == NameMatchResult.MATCH
    val passbookMatches =
      matchName(aadhaarName, documents[DocumentType.BANK_PASSBOOK]?.printedName.orEmpty()) == NameMatchResult.MATCH

    val noMeter = !meterInfo.hasMeter
    val otherMemberMeter = meterInfo.hasMeter && !meterInfo.sameAsBeneficiary
    val samePerson = meterInfo.hasMeter && meterInfo.sameAsBeneficiary

    val aadhaarDiffers = !gharkulMatchesAadhaar && documents[DocumentType.AADHAAR]?.printedName?.isNotBlank() == true
    val panDiffers = !panMatches && documents[DocumentType.PAN]?.printedName?.isNotBlank() == true
    val billDiffers = meterInfo.hasMeter && !billMatches && documents[DocumentType.LIGHT_BILL]?.printedName?.isNotBlank() == true
    val passbookDiffers = !passbookMatches && documents[DocumentType.BANK_PASSBOOK]?.printedName?.isNotBlank() == true

    val allSame =
      samePerson && gharkulMatchesAadhaar && panMatches && billMatches && passbookMatches

    return ChecklistAnswers(
      gharkulAndMeterSamePerson = samePerson,
      allDocsHaveSameName = allSame,
      aadhaarNameChangeNeeded = aadhaarDiffers,
      lightBillNameChangeNeeded = billDiffers,
      panNameChangeNeeded = panDiffers,
      passbookNameChangeNeeded = passbookDiffers,
      meterOnFamilyMemberName = otherMemberMeter,
      noElectricityConnection = noMeter,
    )
  }

  /**
   * Evaluates ALL 4 categories satisfied by the Yes/No answers.
   * Note: When a user answers No to "same person / same name" or Yes to Category 2/3/4 conditions,
   * multiple categories can be satisfied simultaneously.
   */
  fun evaluateMatchedCategories(checklist: ChecklistAnswers): List<SurveyCategory> {
    val matched = mutableListOf<SurveyCategory>()

    val cat2Triggered =
      checklist.aadhaarNameChangeNeeded ||
        checklist.lightBillNameChangeNeeded ||
        checklist.panNameChangeNeeded ||
        checklist.passbookNameChangeNeeded ||
        !checklist.allDocsHaveSameName

    val cat3Triggered = checklist.meterOnFamilyMemberName || !checklist.gharkulAndMeterSamePerson
    val cat4Triggered = checklist.noElectricityConnection

    if (!cat2Triggered && !cat3Triggered && !cat4Triggered &&
      checklist.gharkulAndMeterSamePerson && checklist.allDocsHaveSameName
    ) {
      matched.add(SurveyCategory.CATEGORY_1)
    }
    if (cat2Triggered) {
      matched.add(SurveyCategory.CATEGORY_2)
    }
    if (cat3Triggered && !cat4Triggered) {
      matched.add(SurveyCategory.CATEGORY_3)
    } else if (checklist.meterOnFamilyMemberName) {
      // Explicit Cat 3 answer also marks Cat 3 even if Cat 4 is checked (priority engine still picks 4 > 3)
      if (!matched.contains(SurveyCategory.CATEGORY_3)) {
        matched.add(SurveyCategory.CATEGORY_3)
      }
    }
    if (cat4Triggered) {
      matched.add(SurveyCategory.CATEGORY_4)
    }

    if (matched.isEmpty()) {
      matched.add(SurveyCategory.CATEGORY_2)
    }
    return matched.sortedBy { it.number }
  }

  /**
   * Picks the highest priority category among all matched categories: 4 > 3 > 2 > 1.
   */
  fun suggestCategoryWithPriority(
    checklist: ChecklistAnswers,
  ): Pair<SurveyCategory, List<SurveyCategory>> {
    val matched = evaluateMatchedCategories(checklist)
    val highest = matched.maxByOrNull { it.number } ?: SurveyCategory.CATEGORY_2
    return highest to matched
  }

  fun suggestCategory(
    meterInfo: MeterInfo,
    checklist: ChecklistAnswers,
    documents: Map<DocumentType, DocumentEntry>,
  ): SurveyCategory {
    return suggestCategoryWithPriority(checklist).first
  }

  fun computePendingActions(
    category: SurveyCategory,
    meterInfo: MeterInfo,
    casteCategory: CasteCategory,
    checklist: ChecklistAnswers,
    documents: Map<DocumentType, DocumentEntry>,
  ): List<PendingActionItem> {
    val items = mutableListOf<PendingActionItem>()

    when (category) {
      SurveyCategory.CATEGORY_1 -> {
        requiredDocumentsFor(meterInfo, casteCategory).forEach { docType ->
          val entry = documents[docType]
          if (entry == null || entry.legibility != DocumentLegibility.CLEAR) {
            val statusLabel =
              if (entry?.legibility == DocumentLegibility.UNCLEAR) "फोटो अस्पष्ट (पुन्हा घ्या)"
              else "कागदपत्र बाकी"
            items.add(
              PendingActionItem(
                titleMr = "${docType.labelMr} – $statusLabel",
                responsibleMr = docType.responsiblePartyOnMismatch.labelMr,
                isCompanyAction = docType.responsiblePartyOnMismatch == ResponsibleParty.COMPANY,
              )
            )
          } else if (!entry.isSizeValid) {
            items.add(
              PendingActionItem(
                titleMr = "${docType.labelMr} – आकार 200-500 KB मर्यादेत नाही",
                responsibleMr = ResponsibleParty.CUSTOMER.labelMr,
                isCompanyAction = false,
              )
            )
          }
        }
      }
      SurveyCategory.CATEGORY_2 -> {
        if (checklist.aadhaarNameChangeNeeded || documents[DocumentType.AADHAAR]?.legibility != DocumentLegibility.CLEAR) {
          items.add(
            PendingActionItem(
              titleMr = "आधार कार्ड – ग्राहकाने बदल करून देणे",
              responsibleMr = ResponsibleParty.CUSTOMER.labelMr,
              isCompanyAction = false,
            )
          )
        }
        if (checklist.panNameChangeNeeded || documents[DocumentType.PAN]?.legibility != DocumentLegibility.CLEAR) {
          items.add(
            PendingActionItem(
              titleMr = "पॅन कार्ड – आम्ही बदल करून देऊ",
              responsibleMr = ResponsibleParty.COMPANY.labelMr,
              isCompanyAction = true,
            )
          )
        }
        if (checklist.lightBillNameChangeNeeded || documents[DocumentType.LIGHT_BILL]?.legibility != DocumentLegibility.CLEAR) {
          items.add(
            PendingActionItem(
              titleMr = "लाईट बिल – आम्ही बदल करून देऊ",
              responsibleMr = ResponsibleParty.COMPANY.labelMr,
              isCompanyAction = true,
            )
          )
        }
        if (checklist.passbookNameChangeNeeded || documents[DocumentType.BANK_PASSBOOK]?.legibility != DocumentLegibility.CLEAR) {
          items.add(
            PendingActionItem(
              titleMr = "बँक पासबुक – ग्राहकाने बदल करून देणे",
              responsibleMr = ResponsibleParty.CUSTOMER.labelMr,
              isCompanyAction = false,
            )
          )
        }
        if (items.isEmpty() && !checklist.allDocsHaveSameName) {
          items.add(
            PendingActionItem(
              titleMr = "कागदपत्रांवरील नाव दुरुस्ती बाकी",
              responsibleMr = ResponsibleParty.CUSTOMER.labelMr,
              isCompanyAction = false,
            )
          )
        }
      }
      SurveyCategory.CATEGORY_3 -> {
        items.add(
          PendingActionItem(
            titleMr = if (meterInfo.isHolderAlive) "मीटर ट्रान्सफर (व्यक्ती हयात)" else "मीटर ट्रान्सफर (व्यक्ती मृत)",
            responsibleMr = "नावावर मीटर ट्रान्सफर प्रक्रिया",
            isCompanyAction = true,
          )
        )
        requiredDocumentsFor(meterInfo, casteCategory).forEach { docType ->
          val entry = documents[docType]
          if (entry == null || entry.legibility != DocumentLegibility.CLEAR) {
            items.add(
              PendingActionItem(
                titleMr = "${docType.labelMr} – अपूर्ण / बाकी",
                responsibleMr = ResponsibleParty.CUSTOMER.labelMr,
                isCompanyAction = false,
              )
            )
          }
        }
      }
      SurveyCategory.CATEGORY_4 -> {
        val interested = meterInfo.interestedInNewConnection != false
        items.add(
          PendingActionItem(
            titleMr =
              if (interested) "नवीन वीज कनेक्शन – अर्ज व कागदपत्रे बाकी"
              else "नवीन कनेक्शनसाठी इच्छुक नाही",
            responsibleMr = if (interested) "ग्राहक व कंपनी समन्वय" else "बंद",
            isCompanyAction = false,
          )
        )
      }
    }
    return items
  }

  fun computeOverallStatus(
    category: SurveyCategory,
    pendingActions: List<PendingActionItem>,
  ): OverallStatus {
    if (category == SurveyCategory.CATEGORY_1 && pendingActions.isEmpty()) {
      return OverallStatus.READY
    }
    val hasCustomerPending = pendingActions.any { !it.isCompanyAction }
    return if (hasCustomerPending || pendingActions.isEmpty()) {
      OverallStatus.PENDING_CUSTOMER
    } else {
      OverallStatus.PENDING_COMPANY
    }
  }

  fun findDuplicates(
    existingSurveys: List<CustomerSurvey>,
    editingId: String?,
    aadhaarLast4: String,
    gharkulId: String,
    consumerNo: String,
  ): List<String> {
    val warnings = mutableListOf<String>()
    val cleanAadhaar = aadhaarLast4.trim()
    val cleanGharkul = gharkulId.trim()
    val cleanConsumer = consumerNo.trim()

    existingSurveys.filter { it.id != editingId }.forEach { other ->
      if (cleanAadhaar.length == 4 && other.aadhaarLast4 == cleanAadhaar) {
        warnings.add("आधार शेवटचे ४ अंक ($cleanAadhaar) '${other.beneficiaryName}' कडे आधीच नोंदलेले आहेत.")
      }
      if (cleanGharkul.isNotBlank() && other.gharkulId.equals(cleanGharkul, ignoreCase = true)) {
        warnings.add("घरकुल आयडी '$cleanGharkul' आधीच नोंदलेला आहे (${other.beneficiaryName}).")
      }
      if (cleanConsumer.isNotBlank() && other.meterInfo.consumerNumber.equals(cleanConsumer, ignoreCase = true)) {
        warnings.add("ग्राहक क्रमांक '$cleanConsumer' आधीच नोंदलेला आहे (${other.beneficiaryName}).")
      }
    }
    return warnings.distinct()
  }

  fun buildWhatsAppPendingMessage(
    beneficiaryName: String,
    villageName: String,
    category: SurveyCategory,
    pendingActions: List<PendingActionItem>,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("नमस्कार $beneficiaryName,")
    sb.appendLine("आपला स्मार्ट घरकुल सोलर सर्व्हे (गाव: $villageName) नोंदवला गेला आहे.")
    sb.appendLine("स्थिती: ${category.shortTagMr}")
    if (pendingActions.isEmpty()) {
      sb.appendLine("सर्व कागदपत्रे व नावे स्पष्ट आहेत. आपला अर्ज फॉर्म भरण्यासाठी तयार आहे.")
    } else {
      sb.appendLine("पुढील कार्यवाही बाकी आहे:")
      pendingActions.forEachIndexed { index, item ->
        sb.appendLine("${index + 1}. ${item.titleMr}")
      }
      sb.appendLine("कृपया आवश्यक कागदपत्रे (200–500 KB स्पष्ट फोटो) लवकरात लवकर उपलब्ध करून द्यावीत.")
    }
    return sb.toString().trim()
  }
}
