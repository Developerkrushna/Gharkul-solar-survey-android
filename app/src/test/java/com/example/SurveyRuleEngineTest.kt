package com.example

import com.example.domain.SurveyRuleEngine
import com.example.model.CasteCategory
import com.example.model.ChecklistAnswers
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.MeterInfo
import com.example.model.NameMatchResult
import com.example.model.OverallStatus
import com.example.model.SurveyCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SurveyRuleEngineTest {

  @Test
  fun `name matching detects exact, close, and mismatch in Marathi`() {
    assertEquals(
      NameMatchResult.MATCH,
      SurveyRuleEngine.matchName("सुनीता शिंदे", "सुनीता शिंदे"),
    )
    assertEquals(
      NameMatchResult.CLOSE_MATCH,
      SurveyRuleEngine.matchName("सुनीता शिंदे", "सुनिता शिंदे"),
    )
    assertEquals(
      NameMatchResult.CLOSE_MATCH,
      SurveyRuleEngine.matchName("सुनीता शिंदे", "सुनीता बबन शिंदे"),
    )
    assertEquals(
      NameMatchResult.MISMATCH,
      SurveyRuleEngine.matchName("सुनीता शिंदे", "सुनीता जाधव"),
    )
  }

  @Test
  fun `category 4 suggested when no electricity connection exists`() {
    val meter = MeterInfo(hasMeter = false, interestedInNewConnection = true)
    val checklist = ChecklistAnswers(
      gharkulAndMeterSamePerson = false,
      allDocsHaveSameName = false,
      noElectricityConnection = true,
    )
    val category = SurveyRuleEngine.suggestCategory(meter, checklist, emptyMap())
    assertEquals(SurveyCategory.CATEGORY_4, category)
  }

  @Test
  fun `category priority selects 4 over 3 over 2 over 1 when multiple categories match`() {
    // Cat 2 + Cat 3 -> picks Category 3
    val cat2And3Checklist = ChecklistAnswers(
      gharkulAndMeterSamePerson = false,
      allDocsHaveSameName = false,
      aadhaarNameChangeNeeded = true,
      meterOnFamilyMemberName = true,
      noElectricityConnection = false,
    )
    val (highest23, matched23) = SurveyRuleEngine.suggestCategoryWithPriority(cat2And3Checklist)
    assertEquals(SurveyCategory.CATEGORY_3, highest23)
    assertTrue(matched23.contains(SurveyCategory.CATEGORY_2))
    assertTrue(matched23.contains(SurveyCategory.CATEGORY_3))

    // Cat 2 + Cat 3 + Cat 4 -> picks Category 4
    val cat234Checklist = cat2And3Checklist.copy(noElectricityConnection = true)
    val (highest234, matched234) = SurveyRuleEngine.suggestCategoryWithPriority(cat234Checklist)
    assertEquals(SurveyCategory.CATEGORY_4, highest234)
    assertTrue(matched234.containsAll(listOf(SurveyCategory.CATEGORY_2, SurveyCategory.CATEGORY_3, SurveyCategory.CATEGORY_4)))
  }

  @Test
  fun `requiredDocumentsFor includes core ownership, photo, SC ST caste cert, and Cat 3 ration or death cert`() {
    val openMeter = MeterInfo(hasMeter = true, sameAsBeneficiary = true)
    val openDocs = SurveyRuleEngine.requiredDocumentsFor(openMeter, CasteCategory.OPEN)
    assertTrue(openDocs.contains(DocumentType.OWNERSHIP_PROOF))
    assertTrue(openDocs.contains(DocumentType.PASSPORT_PHOTO))
    assertFalse(openDocs.contains(DocumentType.CASTE_CERTIFICATE))

    val scDocs = SurveyRuleEngine.requiredDocumentsFor(openMeter, CasteCategory.SC)
    assertTrue(scDocs.contains(DocumentType.CASTE_CERTIFICATE))

    val stDocs = SurveyRuleEngine.requiredDocumentsFor(openMeter, CasteCategory.ST)
    assertTrue(stDocs.contains(DocumentType.CASTE_CERTIFICATE))

    val aliveMeter = MeterInfo(hasMeter = true, sameAsBeneficiary = false, isHolderAlive = true)
    val aliveDocs = SurveyRuleEngine.requiredDocumentsFor(aliveMeter, CasteCategory.OPEN)
    assertTrue(aliveDocs.contains(DocumentType.RATION_CARD))
    assertFalse(aliveDocs.contains(DocumentType.DEATH_CERTIFICATE))

    val deceasedMeter = MeterInfo(hasMeter = true, sameAsBeneficiary = false, isHolderAlive = false)
    val deceasedDocs = SurveyRuleEngine.requiredDocumentsFor(deceasedMeter, CasteCategory.OPEN)
    assertTrue(deceasedDocs.contains(DocumentType.DEATH_CERTIFICATE))
  }

  @Test
  fun `category 1 ready only when all core documents are clear, size-valid, and match`() {
    val meter = MeterInfo(hasMeter = true, sameAsBeneficiary = true)
    val docs =
      mapOf(
        DocumentType.AADHAAR to DocumentEntry(DocumentType.AADHAAR, "सुनीता शिंदे", "uri://a", 300, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        DocumentType.PAN to DocumentEntry(DocumentType.PAN, "सुनीता शिंदे", "uri://p", 310, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        DocumentType.LIGHT_BILL to DocumentEntry(DocumentType.LIGHT_BILL, "सुनीता शिंदे", "uri://l", 280, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        DocumentType.BANK_PASSBOOK to DocumentEntry(DocumentType.BANK_PASSBOOK, "सुनीता शिंदे", "uri://b", 260, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        DocumentType.OWNERSHIP_PROOF to DocumentEntry(DocumentType.OWNERSHIP_PROOF, "सुनीता शिंदे", "uri://o", 350, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
        DocumentType.PASSPORT_PHOTO to DocumentEntry(DocumentType.PASSPORT_PHOTO, "सुनीता शिंदे", "uri://ph", 240, true, DocumentLegibility.CLEAR, NameMatchResult.MATCH),
      )
    val checklist = SurveyRuleEngine.deriveChecklist("सुनीता शिंदे", meter, docs)
    val category = SurveyRuleEngine.suggestCategory(meter, checklist, docs)
    val pending = SurveyRuleEngine.computePendingActions(category, meter, CasteCategory.OPEN, checklist, docs)
    val status = SurveyRuleEngine.computeOverallStatus(category, pending)

    assertEquals(SurveyCategory.CATEGORY_1, category)
    assertTrue(pending.isEmpty())
    assertEquals(OverallStatus.READY, status)
  }
}
