package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.domain.SurveyRuleEngine
import com.example.model.CustomerSurvey
import com.example.model.DocumentLegibility
import java.io.File
import java.io.FileOutputStream

private data class PdfPhotoCardData(
  val title: String,
  val subtitle: String,
  val noteText: String?,
  val photoUri: String?,
)

object SurveyPdfGenerator {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 32f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f)
  private const val MAX_Y = PAGE_HEIGHT - 38f

  private val navyColor = Color.rgb(24, 39, 75)
  private val crimsonColor = Color.rgb(229, 25, 75)
  private val slateColor = Color.rgb(71, 85, 105)
  private val textPrimaryColor = Color.rgb(15, 23, 42)
  private val borderGrayColor = Color.rgb(203, 213, 225)
  private val surfaceTintColor = Color.rgb(241, 245, 249)
  private val iceBlueColor = Color.rgb(235, 242, 255)

  fun formatPipeWithMeters(pipeMeters: String): String {
    val digits = pipeMeters.filter { it.isDigit() }.toIntOrNull()
    return if (digits != null && digits > 0) {
      "$digits पाईप (${digits * 30} मीटर)"
    } else {
      pipeMeters.ifBlank { "5 पाईप (150 मीटर)" }
    }
  }

  fun generateCompleteSurveyPdf(context: Context, customer: CustomerSurvey): Uri? {
    var logoBitmap: Bitmap? = null
    return runCatching {
      logoBitmap =
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo) }.getOrNull()

      val pdf = PdfDocument()
      var pageNumber = 1
      var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
      var canvas = page.canvas
      var y = MARGIN

      fun startNextPage() {
        drawFooter(canvas, pageNumber, logoBitmap)
        pdf.finishPage(page)
        pageNumber++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
      }

      fun ensureSpace(neededHeight: Float) {
        if (y + neededHeight > MAX_Y) {
          startNextPage()
        }
      }

      // Header Banner with 'm' Brand Logo
      val headerHeight = 76f
      val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navyColor }
      canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight), 10f, 10f, headerPaint)

      var textLeft = MARGIN + 14f
      val logo = logoBitmap
      if (logo != null) {
        val logoBox = RectF(MARGIN + 12f, y + 13f, MARGIN + 62f, y + 63f)
        val logoBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(logoBox, 10f, 10f, logoBg)
        drawBitmapFitCenter(canvas, logo, RectF(logoBox.left + 6f, logoBox.top + 6f, logoBox.right - 6f, logoBox.bottom - 6f))
        textLeft = logoBox.right + 12f
      }

      val titlePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 14f
          typeface = Typeface.DEFAULT_BOLD
        }
      val subPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(219, 234, 254)
          textSize = 9.5f
        }
      val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = crimsonColor }
      val badgeTextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 10f
          typeface = Typeface.DEFAULT_BOLD
        }

      canvas.drawText("१ kW घरकुल सोलर सर्व्हे – संपूर्ण फॉर्म अहवाल", textLeft, y + 27f, titlePaint)
      canvas.drawText(
        "गाव: ${customer.villageNameMr} · तालुका: ${customer.talukaNameMr} · जिल्हा: ${customer.districtNameMr}",
        textLeft,
        y + 45f,
        subPaint,
      )
      canvas.drawText(
        "अर्ज क्र.: ${customer.id}  |  सर्व्हे दिनांक: ${customer.surveyDate.ifBlank { customer.surveyDateLabel }}",
        textLeft,
        y + 60f,
        subPaint,
      )

      val badgeLabel = "श्रेणी ${customer.finalCategory.number}"
      val badgeWidth = badgeTextPaint.measureText(badgeLabel) + 18f
      val badgeRect = RectF(PAGE_WIDTH - MARGIN - badgeWidth - 10f, y + 12f, PAGE_WIDTH - MARGIN - 10f, y + 33f)
      canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)
      canvas.drawText(badgeLabel, badgeRect.left + 9f, badgeRect.top + 14.5f, badgeTextPaint)

      y += headerHeight + 14f

      // Section 1: लाभार्थी व प्राथमिक माहिती
      y = drawSectionHeader(canvas, "१. लाभार्थी व प्राथमिक माहिती", y)
      val basicRows = listOf(
        "लाभार्थ्याचे नाव" to customer.beneficiaryName,
        "मोबाईल नंबर" to customer.mobile,
        "ई-मेल आयडी" to customer.emailId.ifBlank { "-" },
        "पत्ता / पिनकोड" to customer.addressLandmark.ifBlank { "-" },
        "घरकुल आयडी" to customer.gharkulId.ifBlank { "-" },
        "आधार शेवटचे ४ अंक" to if (customer.aadhaarLast4.isNotBlank()) "XXXX-XXXX-${customer.aadhaarLast4}" else "-",
        "प्रवर्ग / घर प्रकार / क्षमता" to "${customer.casteCategory.labelMr}  |  ${customer.houseType.labelMr}  |  ${customer.capacity.labelMr}",
        "GPS लोकेशन" to (customer.gpsLocation ?: "-"),
      )
      for ((k, v) in basicRows) {
        ensureSpace(22f)
        y = drawKeyValueRow(canvas, k, v, y)
      }
      y += 8f

      // Section 2: लाईट मीटर व साईट सर्व्हे तपशील
      ensureSpace(40f)
      y = drawSectionHeader(canvas, "२. लाईट मीटर व साईट सर्व्हे तपशील", y)
      val meter = customer.meterInfo
      val site = customer.siteSurveyDetails
      val siteRows = buildList {
        add("लाईट मीटर उपलब्ध आहे का?" to if (meter.hasMeter) "होय" else "नाही")
        if (meter.hasMeter) {
          add("ग्राहक क्रमांक" to meter.consumerNumber.ifBlank { "-" })
          add(
            "मीटर धारक" to
              if (meter.sameAsBeneficiary) "लाभार्थी स्वतः"
              else "${meter.meterHolderName} (${meter.relationship}, ${if (meter.isHolderAlive) "हयात" else "मृत"})",
          )
        }
        add("पोल जवळ आहे का?" to yesNoUnset(meter.isPoleNearbyForNewConn))
        add("नवीन कनेक्शन इच्छुक?" to yesNoUnset(meter.interestedInNewConnection))
        add("छताचा प्रकार" to (site.roofType?.labelMr ?: "-"))
        add("सोलर पॅनेलसाठी १०० चौ.फूट जागा" to yesNoUnset(site.hasSolarPanelSpace100SqFt))
        add("जनरेशन मीटर बसवण्याची जागा" to site.generationMeterLocation.ifBlank { "-" })
        add("अर्थिंगसाठी मोकळी जागा" to yesNoUnset(site.hasEarthingSpace))
      }
      for ((k, v) in siteRows) {
        ensureSpace(22f)
        y = drawKeyValueRow(canvas, k, v, y)
      }
      y += 8f

      // Section 3: महत्त्वाच्या सूचना व श्रेणी पडताळणी
      ensureSpace(40f)
      y = drawSectionHeader(canvas, "३. महत्त्वाच्या सूचना व सर्व श्रेणी पडताळणी", y)
      val comp = customer.documentComplianceChecks
      val checkLines = listOf(
        "• लाईट बिल घरगुती (Residential) आहे: ${yn(comp.lightBillIsResidential)}",
        "• पॅन कार्डवरील नाव व जन्मतारीख आधारसारखी आहे: ${yn(comp.panNameAndDobMatchAadhaar)}",
        "• बँक खाते राष्ट्रीयकृत बँकेत आहे (ADCC नाही): ${yn(comp.bankIsNationalizedNotAdcc)}",
        "• सर्व कागदपत्रांशी एकच मोबाईल लिंक आहे: ${yn(comp.singleMobileLinkedAllDocs)}",
        "• आधार कार्ड बँक खात्याशी सीडेड (DBT) आहे: ${yn(comp.aadhaarSeededToBank)}",
      )
      for (line in checkLines) {
        ensureSpace(20f)
        y = drawWrappedText(canvas, line, MARGIN + 6f, y, CONTENT_WIDTH - 12f, 9.5f, textPrimaryColor)
      }
      y += 4f

      val chk = customer.checklistAnswers
      val qaLines = listOf(
        "१. घरकुल व मीटर एकाच व्यक्तीच्या नावावर: ${yn(chk.gharkulAndMeterSamePerson)}  |  सर्व कागदपत्रांवर नाव समान: ${yn(chk.allDocsHaveSameName)}",
        "२. नाव बदल (आधार/बिल/पॅन/पासबुक): आधार=${yn(chk.aadhaarNameChangeNeeded)}, बिल=${yn(chk.lightBillNameChangeNeeded)}, पॅन=${yn(chk.panNameChangeNeeded)}, पासबुक=${yn(chk.passbookNameChangeNeeded)}",
        "३. मीटर घरातील दुसऱ्या व्यक्तीच्या नावे: ${yn(chk.meterOnFamilyMemberName)}  |  ४. लाईट कनेक्शन नाही: ${yn(chk.noElectricityConnection)}",
        "अंतिम श्रेणी निकाल: ${customer.finalCategory.titleMr}",
      )
      for (line in qaLines) {
        ensureSpace(22f)
        y = drawWrappedText(canvas, line, MARGIN + 6f, y, CONTENT_WIDTH - 12f, 9.5f, navyColor, bold = line.startsWith("अंतिम"))
      }
      if (customer.pendingActions.isNotEmpty()) {
        val pendingText = "बाकी कार्यवाही: " + customer.pendingActions.joinToString("; ") { it.titleMr }
        ensureSpace(28f)
        y = drawWrappedText(canvas, pendingText, MARGIN + 6f, y, CONTENT_WIDTH - 12f, 9.5f, crimsonColor, bold = true)
      }
      y += 8f

      // Section 4: साहित्य तपशील व स्वाक्षऱ्या
      ensureSpace(160f)
      y = drawSectionHeader(canvas, "४. साहित्य तपशील व स्वाक्षऱ्या", y)
      val office = customer.materialAndOfficeDetails
      val materialRows = listOf(
        "कागदपत्रांची स्थिती" to if (office.documentsCompleteStatus) "पूर्ण" else "अपूर्ण",
        "१६ Sq.mm. Aluminium (Earth Wire)" to office.earthWireMeters.ifBlank { "20 मीटर" },
        "AC 1.5 sq.mm 3 Core केबल" to office.acCableMeters.ifBlank { "10 मीटर" },
        "पाईप (१ पाईप = ३० मीटर)" to formatPipeWithMeters(office.pipeMeters),
      )
      for ((k, v) in materialRows) {
        ensureSpace(22f)
        y = drawKeyValueRow(canvas, k, v, y)
      }
      y += 8f

      // Draw Signature Boxes Side-by-Side
      ensureSpace(95f)
      val boxGap = 16f
      val boxWidth = (CONTENT_WIDTH - boxGap) / 2f
      val boxHeight = 68f
      drawSignatureBox(
        canvas = canvas,
        left = MARGIN,
        top = y,
        width = boxWidth,
        height = boxHeight,
        title = "ग्राहक / अर्जदाराची सही (${customer.beneficiaryName})",
        strokes = office.customerSignatureStrokes,
      )
      drawSignatureBox(
        canvas = canvas,
        left = MARGIN + boxWidth + boxGap,
        top = y,
        width = boxWidth,
        height = boxHeight,
        title = "सर्व्हे अधिकारी सही (${office.surveyorName.ifBlank { "-" }})",
        strokes = office.surveyorSignatureStrokes,
      )

      // Section 5: Dedicated Document & Note Cam Image Pages (Large Readable Cards: 2 per page)
      val requiredDocs = SurveyRuleEngine.requiredDocumentsFor(customer.meterInfo, customer.casteCategory)
      val extraUploadedDocs = customer.documents.keys.filter { it !in requiredDocs && !customer.documents[it]?.photoUri.isNullOrBlank() }
      val allDocsToRender = requiredDocs + extraUploadedDocs

      val photoCards = buildList {
        allDocsToRender.forEach { docType ->
          val entry = customer.documents[docType]
          val statusLabel =
            when {
              entry == null || entry.legibility == DocumentLegibility.MISSING -> "उपलब्ध नाही"
              entry.printedName.isBlank() -> "फोटो जोडलेला"
              else -> entry.matchResult.labelMr
            }
          val subtitle = buildString {
            append("नाव: ${entry?.printedName?.ifBlank { "-" } ?: "-"}")
            append("  |  स्थिती: $statusLabel")
            entry?.photoSizeKb?.let { append("  |  आकार: $it KB") }
          }
          val noteText = entry?.queryNote?.takeIf { it.isNotBlank() }?.let { "टीप (Note): $it" }
          add(
            PdfPhotoCardData(
              title = docType.labelMr,
              subtitle = subtitle,
              noteText = noteText,
              photoUri = entry?.photoUri,
            )
          )
        }

        // Note Cam Photos
        add(
          PdfPhotoCardData(
            title = "Note Cam · सोलर बसवण्याच्या आधीच्या छताचा फोटो",
            subtitle =
              site.noteCamRoofGeoInfo.replace("\n", " · ").ifBlank {
                if (site.noteCamRoofBeforePhotoDone) "गॅलरी Note Cam फोटो संलग्न" else "फोटो संलग्न नाही"
              },
            noteText = null,
            photoUri = site.noteCamRoofBeforePhotoUri,
          )
        )
        add(
          PdfPhotoCardData(
            title = "Note Cam · Earthing Point चा फोटो (मालकासोबत)",
            subtitle =
              site.noteCamEarthingGeoInfo.replace("\n", " · ").ifBlank {
                if (site.noteCamEarthingWithOwnerDone) "गॅलरी Note Cam फोटो संलग्न" else "फोटो संलग्न नाही"
              },
            noteText = null,
            photoUri = site.noteCamEarthingWithOwnerPhotoUri,
          )
        )
      }

      // Render 2 large readable cards per A4 page
      val cardHeight = 344f
      val cardGap = 14f
      photoCards.chunked(2).forEachIndexed { chunkIndex, pair ->
        startNextPage()
        val headerTitle =
          if (chunkIndex == 0) "५. अपलोड केलेली कागदपत्रे व Note Cam फोटो"
          else "५. कागदपत्रे व Note Cam फोटो (सुरू...)"
        y = drawSectionHeader(canvas, headerTitle, y)

        pair.forEach { card ->
          val bitmap = decodeUriBitmap(context, card.photoUri, maxDim = 1200)
          drawLargeDocumentCard(
            canvas = canvas,
            card = card,
            bitmap = bitmap,
            top = y,
            height = cardHeight,
          )
          bitmap?.recycle()
          y += cardHeight + cardGap
        }
      }

      drawFooter(canvas, pageNumber, logoBitmap)
      pdf.finishPage(page)

      val pdfDir = File(context.cacheDir, "survey_pdfs").apply { mkdirs() }
      val safeName = customer.beneficiaryName.replace(Regex("""[^\p{L}\p{N}_-]"""), "_").take(24).ifBlank { "survey" }
      val pdfFile = File(pdfDir, "gharkul_${safeName}_${System.currentTimeMillis()}.pdf")
      try {
        FileOutputStream(pdfFile).use { out -> pdf.writeTo(out) }
      } finally {
        runCatching { pdf.close() }
      }

      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    }.also {
      logoBitmap?.recycle()
    }.getOrNull()
  }

  private fun yn(value: Boolean): String = if (value) "होय" else "नाही"

  private fun yesNoUnset(value: Boolean?): String =
    when (value) {
      true -> "होय"
      false -> "नाही"
      null -> "-"
    }

  private fun drawSectionHeader(canvas: Canvas, title: String, y: Float): Float {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = iceBlueColor }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navyColor }
    canvas.drawRoundRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f), 5f, 5f, bgPaint)
    canvas.drawRect(MARGIN, y, MARGIN + 4f, y + 22f, accentPaint)
    val textPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
      }
    canvas.drawText(title, MARGIN + 10f, y + 15f, textPaint)
    return y + 28f
  }

  private fun drawKeyValueRow(canvas: Canvas, key: String, value: String, y: Float): Float {
    val keyPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 9.5f
      }
    val valPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimaryColor
        textSize = 9.5f
        typeface = Typeface.DEFAULT_BOLD
      }
    val keyWidth = CONTENT_WIDTH * 0.42f
    val valWidth = (CONTENT_WIDTH - keyWidth - 10f).toInt().coerceAtLeast(100)
    val safeVal = value.ifBlank { "-" }
    val layout =
      StaticLayout.Builder.obtain(safeVal, 0, safeVal.length, valPaint, valWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .build()

    canvas.drawText(key, MARGIN + 6f, y + 11f, keyPaint)
    canvas.save()
    canvas.translate(MARGIN + keyWidth + 6f, y)
    layout.draw(canvas)
    canvas.restore()

    val rowHeight = maxOf(16f, layout.height.toFloat() + 3f)
    val dividerPaint = Paint().apply {
      color = Color.rgb(226, 232, 240)
      strokeWidth = 0.6f
    }
    canvas.drawLine(MARGIN + 4f, y + rowHeight, PAGE_WIDTH - MARGIN - 4f, y + rowHeight, dividerPaint)
    return y + rowHeight + 4f
  }

  private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    size: Float,
    color: Int,
    bold: Boolean = false,
  ): Float {
    val paint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        if (bold) typeface = Typeface.DEFAULT_BOLD
      }
    val layout =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(80))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height + 4f
  }

  private fun drawSignatureBox(
    canvas: Canvas,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    title: String,
    strokes: List<List<Pair<Float, Float>>>,
  ) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceTintColor }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderGrayColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val rect = RectF(left, top, left + width, top + height)
    canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
    canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

    val allPts = strokes.flatten()
    if (allPts.isNotEmpty()) {
      val minX = allPts.minOf { it.first }
      val maxX = allPts.maxOf { it.first }
      val minY = allPts.minOf { it.second }
      val maxY = allPts.maxOf { it.second }
      val spanX = (maxX - minX).coerceAtLeast(1f)
      val spanY = (maxY - minY).coerceAtLeast(1f)
      val pad = 10f
      val availW = (width - pad * 2).coerceAtLeast(10f)
      val availH = (height - pad * 2).coerceAtLeast(10f)
      val scale = minOf(availW / spanX, availH / spanY)
      val offsetX = left + pad + (availW - spanX * scale) / 2f
      val offsetY = top + pad + (availH - spanY * scale) / 2f

      val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = navyColor
          style = Paint.Style.STROKE
          strokeWidth = 1.8f
          strokeCap = Paint.Cap.ROUND
          strokeJoin = Paint.Join.ROUND
        }
      for (pts in strokes) {
        if (pts.size == 1) {
          val cx = offsetX + (pts[0].first - minX) * scale
          val cy = offsetY + (pts[0].second - minY) * scale
          canvas.drawCircle(cx, cy, 1.5f, strokePaint)
        } else if (pts.size > 1) {
          val path = Path()
          path.moveTo(offsetX + (pts[0].first - minX) * scale, offsetY + (pts[0].second - minY) * scale)
          for (i in 1 until pts.size) {
            path.lineTo(offsetX + (pts[i].first - minX) * scale, offsetY + (pts[i].second - minY) * scale)
          }
          canvas.drawPath(path, strokePaint)
        }
      }
    } else {
      val emptyPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = slateColor
          textSize = 9f
        }
      canvas.drawText("सही उपलब्ध नाही", left + 14f, top + height / 2f + 3f, emptyPaint)
    }

    val labelPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 8.5f
        typeface = Typeface.DEFAULT_BOLD
      }
    canvas.drawText(title, left + 2f, top + height + 12f, labelPaint)
  }

  private fun drawLargeDocumentCard(
    canvas: Canvas,
    card: PdfPhotoCardData,
    bitmap: Bitmap?,
    top: Float,
    height: Float,
  ) {
    val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val cardBorder =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderGrayColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val rect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, top + height)
    canvas.drawRoundRect(rect, 8f, 8f, cardBg)
    canvas.drawRoundRect(rect, 8f, 8f, cardBorder)

    val textWidth = (CONTENT_WIDTH - 24f).toInt().coerceAtLeast(100)
    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 11.5f
        typeface = Typeface.DEFAULT_BOLD
      }
    val subPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 9.2f
      }
    val notePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 83, 9)
        textSize = 9.2f
        typeface = Typeface.DEFAULT_BOLD
      }

    val titleLayout = StaticLayout.Builder.obtain(card.title, 0, card.title.length, titlePaint, textWidth).build()
    val subLayout = StaticLayout.Builder.obtain(card.subtitle, 0, card.subtitle.length, subPaint, textWidth).build()
    val noteLayout =
      card.noteText?.let { StaticLayout.Builder.obtain(it, 0, it.length, notePaint, textWidth).build() }

    var ty = top + 10f
    canvas.save()
    canvas.translate(MARGIN + 12f, ty)
    titleLayout.draw(canvas)
    canvas.restore()
    ty += titleLayout.height + 2f

    canvas.save()
    canvas.translate(MARGIN + 12f, ty)
    subLayout.draw(canvas)
    canvas.restore()
    ty += subLayout.height + 3f

    if (noteLayout != null) {
      canvas.save()
      canvas.translate(MARGIN + 12f, ty)
      noteLayout.draw(canvas)
      canvas.restore()
      ty += noteLayout.height + 4f
    }

    val imageBox = RectF(MARGIN + 12f, ty + 4f, PAGE_WIDTH - MARGIN - 12f, top + height - 10f)
    val imagePlaceholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceTintColor }
    canvas.drawRoundRect(imageBox, 6f, 6f, imagePlaceholderBg)

    if (bitmap != null) {
      val innerPad = 4f
      val targetRect =
        RectF(
          imageBox.left + innerPad,
          imageBox.top + innerPad,
          imageBox.right - innerPad,
          imageBox.bottom - innerPad,
        )
      drawBitmapFitCenter(canvas, bitmap, targetRect)
    } else {
      val placeholderText =
        if (!card.photoUri.isNullOrBlank() && card.photoUri.startsWith("captured://")) {
          "नमुना फोटो संलग्न (${card.photoUri.removePrefix("captured://")})"
        } else {
          "फोटो अपलोड केलेला नाही / उपलब्ध नाही"
        }
      val emptyPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = slateColor
          textSize = 10f
        }
      val textW = emptyPaint.measureText(placeholderText)
      canvas.drawText(
        placeholderText,
        imageBox.centerX() - (textW / 2f),
        imageBox.centerY() + 3f,
        emptyPaint,
      )
    }
  }

  private fun drawBitmapFitCenter(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    val availW = target.width().coerceAtLeast(1f)
    val availH = target.height().coerceAtLeast(1f)
    val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
    val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
    val scale = minOf(availW / bmpW, availH / bmpH)
    val drawnW = bmpW * scale
    val drawnH = bmpH * scale
    val left = target.left + (availW - drawnW) / 2f
    val top = target.top + (availH - drawnH) / 2f
    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
    val dstRect = RectF(left, top, left + drawnW, top + drawnH)
    canvas.drawBitmap(bitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
  }

  private fun decodeUriBitmap(context: Context, uriStr: String?, maxDim: Int): Bitmap? {
    if (uriStr.isNullOrBlank() || uriStr.startsWith("captured://")) return null
    return runCatching {
      val bytes =
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
          val conn = (java.net.URL(uriStr).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
          }
          try {
            if (conn.responseCode in 200..299) {
              conn.inputStream.use { it.readBytes() }
            } else {
              null
            }
          } finally {
            conn.disconnect()
          }
        } else {
          val uri = Uri.parse(uriStr)
          context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null

      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
      if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
      var sample = 1
      val maxSide = maxOf(opts.outWidth, opts.outHeight)
      while (maxSide / sample > maxDim) {
        sample *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }.getOrNull()
  }

  private fun drawFooter(canvas: Canvas, pageNumber: Int, logoBitmap: Bitmap?) {
    var textX = MARGIN
    if (logoBitmap != null) {
      drawBitmapFitCenter(
        canvas = canvas,
        bitmap = logoBitmap,
        target = RectF(MARGIN, PAGE_HEIGHT - 25f, MARGIN + 14f, PAGE_HEIGHT - 11f),
      )
      textX = MARGIN + 18f
    }
    val footerPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 8.5f
      }
    canvas.drawText(
      "घरकुल सोलर सर्व्हे · पान $pageNumber",
      textX,
      PAGE_HEIGHT - 15f,
      footerPaint,
    )
  }
}
