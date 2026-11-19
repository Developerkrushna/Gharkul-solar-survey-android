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
import com.example.model.InstallationPhotoItem
import com.example.model.InstallationReport
import java.io.File
import java.io.FileOutputStream

object InstallationPdfGenerator {

  private const val PAGE_WIDTH = 595f
  private const val PAGE_HEIGHT = 842f
  private const val MARGIN = 24f
  private const val TABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2f)

  private val navyColor = Color.rgb(24, 39, 75)
  private val crimsonColor = Color.rgb(229, 25, 75)
  private val slateColor = Color.rgb(71, 85, 105)
  private val textPrimaryColor = Color.rgb(15, 23, 42)
  private val tableHeaderBgColor = Color.rgb(214, 224, 240)
  private val subRowBgColor = Color.rgb(248, 250, 252)
  private val borderGrayColor = Color.rgb(203, 213, 225)
  private val surfaceTintColor = Color.rgb(241, 245, 249)

  fun generateCertificatePdf(
    context: Context,
    report: InstallationReport,
    includeImages: Boolean = true,
  ): Uri? {
    var logoBitmap: Bitmap? = null
    return runCatching {
      logoBitmap =
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo) }.getOrNull()

      val pdf = PdfDocument()

      // ================= PAGE 1: INSTALLATION / COMPLETION CERTIFICATE =================
      val page1 =
        pdf.startPage(
          PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        )
      drawCertificatePage1(page1.canvas, report, logoBitmap)
      pdf.finishPage(page1)

      // ================= PAGE 2: SERIAL NUMBERS =================
      val page2 =
        pdf.startPage(
          PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 2).create()
        )
      drawSerialNumbersPage2(page2.canvas, report, logoBitmap)
      pdf.finishPage(page2)

      // ================= PAGE 3+: AFTER-INSTALLATION NOTECAM GEO IMAGES (OPTIONAL) =================
      val attachedPhotos =
        if (includeImages) report.afterInstallationPhotos.filter { !it.photoUri.isNullOrBlank() }
        else emptyList()

      if (attachedPhotos.isNotEmpty()) {
        val cardHeight = 344f
        val cardGap = 14f
        var pageNum = 3
        attachedPhotos.chunked(2).forEachIndexed { chunkIdx, pair ->
          val photoPage =
            pdf.startPage(
              PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNum).create()
            )
          val c = photoPage.canvas
          var y = drawPhotoPageHeader(
            canvas = c,
            title =
              if (chunkIdx == 0) "After Installation NoteCam Site Photos"
              else "After Installation Site Photos (Continued)",
            subtitle = "Consumer: ${report.consumerName.ifBlank { "-" }}  |  Consumer No.: ${report.consumerNo.ifBlank { "-" }}",
            logoBitmap = logoBitmap,
          )
          pair.forEach { item ->
            val bitmap = decodeUriBitmap(context, item.photoUri, maxDim = 1200)
            drawInstallationPhotoCard(
              canvas = c,
              item = item,
              bitmap = bitmap,
              top = y,
              height = cardHeight,
            )
            bitmap?.recycle()
            y += cardHeight + cardGap
          }
          val footerPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = slateColor
              textSize = 8.5f
              textAlign = Paint.Align.CENTER
            }
          c.drawText(
            "REFLECTION MEDIA · Page $pageNum · Installation Site Proof",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 16f,
            footerPaint,
          )
          pdf.finishPage(photoPage)
          pageNum++
        }
      }

      val pdfDir = File(context.cacheDir, "survey_pdfs").apply { mkdirs() }
      val safeName =
        report.consumerName.replace(Regex("""[^\p{L}\p{N}_-]"""), "_").take(24).ifBlank { "installation" }
      val modeTag = if (includeImages) "with_images" else "certificate"
      val pdfFile = File(pdfDir, "installation_${modeTag}_${safeName}_${System.currentTimeMillis()}.pdf")
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

  private fun drawPhotoPageHeader(
    canvas: Canvas,
    title: String,
    subtitle: String,
    logoBitmap: Bitmap?,
  ): Float {
    var y = MARGIN + 4f
    val bannerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 48f)
    canvas.drawRoundRect(bannerRect, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navyColor })
    var textX = MARGIN + 12f
    if (logoBitmap != null) {
      val logoBox = RectF(MARGIN + 10f, y + 8f, MARGIN + 42f, y + 40f)
      canvas.drawRoundRect(logoBox, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
      drawBitmapFitCenter(canvas, logoBitmap, RectF(logoBox.left + 4f, logoBox.top + 4f, logoBox.right - 4f, logoBox.bottom - 4f))
      textX = logoBox.right + 10f
    }
    canvas.drawText(
      title,
      textX,
      y + 21f,
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
      },
    )
    canvas.drawText(
      subtitle,
      textX,
      y + 37f,
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(219, 234, 254)
        textSize = 9f
      },
    )
    return y + 62f
  }

  private fun drawInstallationPhotoCard(
    canvas: Canvas,
    item: InstallationPhotoItem,
    bitmap: Bitmap?,
    top: Float,
    height: Float,
  ) {
    val rect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, top + height)
    canvas.drawRoundRect(rect, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    canvas.drawRoundRect(
      rect,
      8f,
      8f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderGrayColor
        style = Paint.Style.STROKE
        strokeWidth = 1f
      },
    )

    val textWidth = (TABLE_WIDTH - 24f).toInt().coerceAtLeast(100)
    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 11.5f
        typeface = Typeface.DEFAULT_BOLD
      }
    val subPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 9f
      }
    val caption = item.caption.ifBlank { "Solar Installation Site Photo" }
    val geoMeta =
      item.geoInfo.replace("\n", " · ").ifBlank {
        if (item.isCameraGeoStamped) "Live GPS NoteCam Photo"
        else "Gallery Photo Attached"
      }
    val sizePart = item.photoSizeKb?.let { "  |  Size: $it KB" } ?: ""
    val subtitleText = "$geoMeta$sizePart"

    val titleLayout = StaticLayout.Builder.obtain(caption, 0, caption.length, titlePaint, textWidth).build()
    val subLayout = StaticLayout.Builder.obtain(subtitleText, 0, subtitleText.length, subPaint, textWidth).build()

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
    ty += subLayout.height + 6f

    val imageBox = RectF(MARGIN + 12f, ty, PAGE_WIDTH - MARGIN - 12f, top + height - 10f)
    canvas.drawRoundRect(imageBox, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceTintColor })

    if (bitmap != null) {
      drawBitmapFitCenter(
        canvas,
        bitmap,
        RectF(imageBox.left + 4f, imageBox.top + 4f, imageBox.right - 4f, imageBox.bottom - 4f),
      )
    } else {
      val emptyPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = slateColor
          textSize = 10f
          textAlign = Paint.Align.CENTER
        }
      canvas.drawText("Photo preview unavailable", imageBox.centerX(), imageBox.centerY() + 3f, emptyPaint)
    }
  }

  private fun decodeUriBitmap(context: Context, uriStr: String?, maxDim: Int): Bitmap? {
    if (uriStr.isNullOrBlank()) return null
    return runCatching {
      val uri = Uri.parse(uriStr)
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
      if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
      var sample = 1
      val maxSide = maxOf(opts.outWidth, opts.outHeight)
      while (maxSide / sample > maxDim) {
        sample *= 2
      }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
  }

  private fun drawCertificatePage1(
    canvas: Canvas,
    report: InstallationReport,
    logoBitmap: Bitmap?,
  ) {
    val frameTop = MARGIN
    val frameLeft = MARGIN
    val frameRight = PAGE_WIDTH - MARGIN

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val thickBorderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
      }
    val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tableHeaderBgColor }
    val subRowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = subRowBgColor }

    var y = frameTop

    // 1. Top Brand Logo + REFLECTION MEDIA Block (54f)
    val brandBoxH = 54f
    canvas.drawRect(frameLeft, y, frameRight, y + brandBoxH, borderPaint)
    if (logoBitmap != null) {
      drawBitmapFitCenter(
        canvas,
        logoBitmap,
        RectF((PAGE_WIDTH / 2f) - 18f, y + 4f, (PAGE_WIDTH / 2f) + 18f, y + 26f),
      )
    }
    val brandTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 11.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    val phonePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = crimsonColor
        textSize = 8.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("REFLECTION MEDIA", PAGE_WIDTH / 2f, y + 38f, brandTitlePaint)
    canvas.drawText("91300028122", PAGE_WIDTH / 2f, y + 49f, phonePaint)
    y += brandBoxH

    // 2. Address Strip (20f)
    val addrH = 20f
    canvas.drawRect(frameLeft, y, frameRight, y + addrH, borderPaint)
    val addrPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "REFLECTION MEDIA , NEWASA , A'NAGAR , PIN - 414603.",
      PAGE_WIDTH / 2f,
      y + 13.5f,
      addrPaint,
    )
    y += addrH

    // 3. Certificate Title Strip (19f)
    val titleH = 19f
    canvas.drawRect(frameLeft, y, frameRight, y + titleH, borderPaint)
    val certTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "INSTALLATION / COMPLETION CERTIFICATE FOR SOLAR SYSTEM",
      PAGE_WIDTH / 2f,
      y + 13f,
      certTitlePaint,
    )
    y += titleH

    // 4. Consumer & System Metadata Rows (4 rows)
    val col1W = TABLE_WIDTH * 0.27f
    val col2W = TABLE_WIDTH * 0.25f
    val col3W = TABLE_WIDTH * 0.20f
    val col4W = TABLE_WIDTH - col1W - col2W - col3W

    val metaLabelPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 7.8f
        typeface = Typeface.DEFAULT_BOLD
      }
    val metaValuePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 8.5f
        typeface = Typeface.DEFAULT_BOLD
      }

    // Meta Row 1: CONSUMER NO. (LIGHT BILL NO) | val | INSTALLATION DATE | val
    val metaRow1H = 25f
    drawCellRect(canvas, frameLeft, y, col1W, metaRow1H, borderPaint)
    drawCenteredLabel(canvas, "CONSUMER NO.\n(LIGHT BILL NO)", frameLeft, y, col1W, metaRow1H, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W, y, col2W, metaRow1H, borderPaint)
    drawCenteredLabel(canvas, report.consumerNo.ifBlank { "-" }, frameLeft + col1W, y, col2W, metaRow1H, metaValuePaint)
    drawCellRect(canvas, frameLeft + col1W + col2W, y, col3W, metaRow1H, borderPaint)
    drawCenteredLabel(canvas, "INSTALLATION\nDATE", frameLeft + col1W + col2W, y, col3W, metaRow1H, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W + col2W + col3W, y, col4W, metaRow1H, borderPaint)
    drawCenteredLabel(canvas, report.installationDate.ifBlank { "-" }, frameLeft + col1W + col2W + col3W, y, col4W, metaRow1H, metaValuePaint)
    y += metaRow1H

    // Meta Row 2: CONSUMER NAME
    val metaRowH = 19f
    drawCellRect(canvas, frameLeft, y, col1W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, "CONSUMER NAME", frameLeft, y, col1W, metaRowH, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W, y, TABLE_WIDTH - col1W, metaRowH, borderPaint)
    drawLeftLabel(canvas, report.consumerName.ifBlank { "-" }, frameLeft + col1W + 8f, y, TABLE_WIDTH - col1W - 12f, metaRowH, metaValuePaint)
    y += metaRowH

    // Meta Row 3: MOBILE NO.
    drawCellRect(canvas, frameLeft, y, col1W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, "MOBILE NO.", frameLeft, y, col1W, metaRowH, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W, y, TABLE_WIDTH - col1W, metaRowH, borderPaint)
    drawLeftLabel(canvas, report.mobileNo.ifBlank { "-" }, frameLeft + col1W + 8f, y, TABLE_WIDTH - col1W - 12f, metaRowH, metaValuePaint)
    y += metaRowH

    // Meta Row 4: SYSTEM CAPACITY | 1 KW | SYSTEM TYPE | ONGRID
    drawCellRect(canvas, frameLeft, y, col1W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, "SYSTEM CAPACITY", frameLeft, y, col1W, metaRowH, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W, y, col2W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, report.systemCapacity.ifBlank { "1 KW" }, frameLeft + col1W, y, col2W, metaRowH, metaValuePaint)
    drawCellRect(canvas, frameLeft + col1W + col2W, y, col3W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, "SYSTEM TYPE", frameLeft + col1W + col2W, y, col3W, metaRowH, metaLabelPaint)
    drawCellRect(canvas, frameLeft + col1W + col2W + col3W, y, col4W, metaRowH, borderPaint)
    drawCenteredLabel(canvas, report.systemType.ifBlank { "ONGRID" }, frameLeft + col1W + col2W + col3W, y, col4W, metaRowH, metaValuePaint)
    y += metaRowH

    // 5. Product Table Header + 23 Material Rows
    val wSr = TABLE_WIDTH * 0.075f
    val wModel = TABLE_WIDTH * 0.255f
    val wCompany = TABLE_WIDTH * 0.235f
    val wSize = TABLE_WIDTH * 0.165f
    val wCap = TABLE_WIDTH * 0.155f
    val wQty = TABLE_WIDTH - wSr - wModel - wCompany - wSize - wCap

    val tblHeaderH = 19f
    val xPositions =
      floatArrayOf(
        frameLeft,
        frameLeft + wSr,
        frameLeft + wSr + wModel,
        frameLeft + wSr + wModel + wCompany,
        frameLeft + wSr + wModel + wCompany + wSize,
        frameLeft + wSr + wModel + wCompany + wSize + wCap,
      )
    val colWidths = floatArrayOf(wSr, wModel, wCompany, wSize, wCap, wQty)
    val headers = arrayOf("Sr. No.", "Product Model", "Company", "SIZE", "Capacity", "QTY")

    val tblHeaderPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
        typeface = Typeface.DEFAULT_BOLD
      }
    for (i in headers.indices) {
      canvas.drawRect(xPositions[i], y, xPositions[i] + colWidths[i], y + tblHeaderH, headerFillPaint)
      canvas.drawRect(xPositions[i], y, xPositions[i] + colWidths[i], y + tblHeaderH, borderPaint)
      drawCenteredLabel(canvas, headers[i], xPositions[i], y, colWidths[i], tblHeaderH, tblHeaderPaint)
    }
    y += tblHeaderH

    val rowH = 17.5f
    val srPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 7.8f
        typeface = Typeface.DEFAULT_BOLD
      }
    val modelPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 7.6f
        typeface = Typeface.DEFAULT_BOLD
      }
    val cellValPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimaryColor
        textSize = 7.5f
      }

    report.products.forEach { item ->
      if (item.isSubRow) {
        canvas.drawRect(frameLeft, y, frameRight, y + rowH, subRowFillPaint)
      }
      for (i in colWidths.indices) {
        canvas.drawRect(xPositions[i], y, xPositions[i] + colWidths[i], y + rowH, borderPaint)
      }
      drawCenteredLabel(canvas, item.srNoDisplay, xPositions[0], y, colWidths[0], rowH, srPaint)
      drawLeftLabel(
        canvas = canvas,
        text = item.productModel,
        x = xPositions[1] + if (item.isSubRow) 8f else 5f,
        y = y,
        width = colWidths[1] - 10f,
        height = rowH,
        paint = modelPaint,
      )
      drawCenteredLabel(canvas, item.company, xPositions[2], y, colWidths[2], rowH, cellValPaint)
      drawCenteredLabel(canvas, item.size, xPositions[3], y, colWidths[3], rowH, cellValPaint)
      drawCenteredLabel(canvas, item.capacity, xPositions[4], y, colWidths[4], rowH, cellValPaint)
      drawCenteredLabel(canvas, item.qty, xPositions[5], y, colWidths[5], rowH, cellValPaint)
      y += rowH
    }

    // 6. Sign-Off Section
    val signBlockH = 98f
    val leftSignW = TABLE_WIDTH * 0.33f
    val midSignW = TABLE_WIDTH * 0.42f
    val rightSignW = TABLE_WIDTH - leftSignW - midSignW

    val xLeft = frameLeft
    val xMid = frameLeft + leftSignW
    val xRight = frameLeft + leftSignW + midSignW

    drawCellRect(canvas, xLeft, y, leftSignW, 20f, borderPaint)
    drawLeftLabel(
      canvas = canvas,
      text = "INSTALLER: ${report.installerName.ifBlank { "-" }}",
      x = xLeft + 5f,
      y = y,
      width = leftSignW - 8f,
      height = 20f,
      paint = metaLabelPaint,
    )
    drawCellRect(canvas, xLeft, y + 20f, leftSignW, signBlockH - 20f, borderPaint)
    drawCenteredLabel(canvas, "SIGNATURE", xLeft, y + 22f, leftSignW, 14f, metaLabelPaint)
    drawSignatureStrokes(
      canvas = canvas,
      rect = RectF(xLeft + 8f, y + 36f, xLeft + leftSignW - 8f, y + signBlockH - 6f),
      strokes = report.installerSignatureStrokes,
    )

    val verifyBoxW1 = midSignW * 0.56f
    val verifyBoxW2 = midSignW - verifyBoxW1
    drawCellRect(canvas, xMid, y, verifyBoxW1, signBlockH, borderPaint)
    drawCellRect(canvas, xMid + verifyBoxW1, y, verifyBoxW2, signBlockH, borderPaint)
    val verifyTitlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
        typeface = Typeface.DEFAULT_BOLD
      }
    val verifyValPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 8f
        typeface = Typeface.DEFAULT_BOLD
      }
    canvas.drawText("1)  Accountant  -", xMid + 6f, y + 24f, verifyTitlePaint)
    canvas.drawText(report.accountantName.ifBlank { "-" }, xMid + verifyBoxW1 + 6f, y + 24f, verifyValPaint)

    canvas.drawText("2)  Manager      -", xMid + 6f, y + 52f, verifyTitlePaint)
    canvas.drawText(report.managerName.ifBlank { "-" }, xMid + verifyBoxW1 + 6f, y + 52f, verifyValPaint)

    canvas.drawText("3)  Supervisor   -", xMid + 6f, y + 80f, verifyTitlePaint)
    canvas.drawText(report.supervisorName.ifBlank { "-" }, xMid + verifyBoxW1 + 6f, y + 80f, verifyValPaint)

    drawCellRect(canvas, xRight, y, rightSignW, signBlockH, borderPaint)
    drawSignatureStrokes(
      canvas = canvas,
      rect = RectF(xRight + 8f, y + 8f, xRight + rightSignW - 8f, y + signBlockH - 22f),
      strokes = report.customerSignatureStrokes,
    )
    drawCenteredLabel(
      canvas = canvas,
      text = "CUSTOMER SIGN AND\nSTAMP",
      x = xRight,
      y = y + signBlockH - 22f,
      width = rightSignW,
      height = 20f,
      paint = metaLabelPaint.apply { textSize = 7f },
    )
    y += signBlockH

    // 7. Bottom Corporate Footer
    val footRow1H = 19f
    val footRow2H = 19f
    drawCellRect(canvas, frameLeft, y, TABLE_WIDTH, footRow1H, borderPaint)
    val foot1Paint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 7.6f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "CUSTOMER CARE – 7558723283 / 7283838383   For Service please send mail at : support@reflectionmedias.in",
      PAGE_WIDTH / 2f,
      y + 12.5f,
      foot1Paint,
    )
    y += footRow1H

    drawCellRect(canvas, frameLeft, y, TABLE_WIDTH, footRow2H, borderPaint)
    val foot2Paint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 7.3f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "CORPORATE OFFICE – REFLECTION MEDIA , NEAR BUS STAND, NEWASA , DIST – AHILYANAGAR PIN - 414603",
      PAGE_WIDTH / 2f,
      y + 12.5f,
      foot2Paint,
    )
    y += footRow2H

    canvas.drawRect(frameLeft, frameTop, frameRight, y, thickBorderPaint)
  }

  private fun drawSerialNumbersPage2(
    canvas: Canvas,
    report: InstallationReport,
    logoBitmap: Bitmap?,
  ) {
    val left = MARGIN + 8f
    val right = PAGE_WIDTH - MARGIN - 8f
    val tableW = right - left
    val col1W = tableW * 0.45f
    val col2W = tableW - col1W

    var y = MARGIN + 12f
    if (logoBitmap != null) {
      drawBitmapFitCenter(
        canvas,
        logoBitmap,
        RectF(left, y, left + 28f, y + 28f),
      )
    }

    val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText("Serial Numbers", PAGE_WIDTH / 2f, y + 20f, titlePaint)

    val subPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 9.5f
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "Consumer: ${report.consumerName.ifBlank { "-" }}  |  Consumer No.: ${report.consumerNo.ifBlank { "-" }}  |  Date: ${report.installationDate}",
      PAGE_WIDTH / 2f,
      y + 38f,
      subPaint,
    )
    y += 54f

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }
    val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tableHeaderBgColor }
    val labelPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
      }
    val valPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        textSize = 10.5f
        typeface = Typeface.DEFAULT_BOLD
      }

    val hdrH = 24f
    canvas.drawRect(left, y, left + col1W, y + hdrH, headerFillPaint)
    canvas.drawRect(left + col1W, y, right, y + hdrH, headerFillPaint)
    canvas.drawRect(left, y, left + col1W, y + hdrH, borderPaint)
    canvas.drawRect(left + col1W, y, right, y + hdrH, borderPaint)
    drawLeftLabel(canvas, "Component / Item", left + 8f, y, col1W - 14f, hdrH, labelPaint)
    drawLeftLabel(canvas, "Verified Serial Number", left + col1W + 8f, y, col2W - 14f, hdrH, labelPaint)
    y += hdrH

    fun drawSerialRow(label: String, serial: String, rowHeight: Float = 26f) {
      drawCellRect(canvas, left, y, col1W, rowHeight, borderPaint)
      drawCellRect(canvas, left + col1W, y, col2W, rowHeight, borderPaint)
      drawLeftLabel(canvas, label, left + 8f, y, col1W - 14f, rowHeight, labelPaint)
      drawLeftLabel(canvas, serial.ifBlank { "-" }, left + col1W + 8f, y, col2W - 14f, rowHeight, valPaint)
      y += rowHeight
    }

    drawSerialRow("Invertor Serial Number", report.inverterSerial.trimmedPrimary)
    drawSerialRow("Generation Meter Serial Number", report.generationMeterSerial.trimmedPrimary)

    report.panelSerials.forEachIndexed { idx, entry ->
      val label = if (idx == 0) "Panel Serial Number (Panel 1)" else "Panel ${idx + 1} Serial Number"
      drawSerialRow(label, entry.serial.trimmedPrimary)
    }

    val footerPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = slateColor
        textSize = 8.5f
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(
      "REFLECTION MEDIA · Page 2 · All serial numbers double-verified at site.",
      PAGE_WIDTH / 2f,
      PAGE_HEIGHT - 18f,
      footerPaint,
    )
  }

  private fun drawCellRect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, paint: Paint) {
    canvas.drawRect(x, y, x + w, y + h, paint)
  }

  private fun drawCenteredLabel(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: TextPaint,
  ) {
    val lines = text.split("\n")
    val lineH = paint.textSize + 2f
    val totalH = lines.size * lineH
    var lineY = y + ((height - totalH) / 2f) + paint.textSize * 0.85f
    val centeredPaint = TextPaint(paint).apply { textAlign = Paint.Align.CENTER }
    lines.forEach { line ->
      canvas.drawText(line, x + (width / 2f), lineY, centeredPaint)
      lineY += lineH
    }
  }

  private fun drawLeftLabel(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: TextPaint,
  ) {
    val safe = text.ifBlank { "" }
    if (safe.isEmpty()) return
    val layout =
      StaticLayout.Builder.obtain(safe, 0, safe.length, paint, width.toInt().coerceAtLeast(24))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(2)
        .build()
    val drawY = y + ((height - layout.height) / 2f).coerceAtLeast(1f)
    canvas.save()
    canvas.translate(x, drawY)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun drawSignatureStrokes(
    canvas: Canvas,
    rect: RectF,
    strokes: List<List<Pair<Float, Float>>>,
  ) {
    val allPts = strokes.flatten()
    if (allPts.isEmpty()) return
    val minX = allPts.minOf { it.first }
    val maxX = allPts.maxOf { it.first }
    val minY = allPts.minOf { it.second }
    val maxY = allPts.maxOf { it.second }
    val spanX = (maxX - minX).coerceAtLeast(1f)
    val spanY = (maxY - minY).coerceAtLeast(1f)
    val pad = 4f
    val availW = (rect.width() - pad * 2f).coerceAtLeast(8f)
    val availH = (rect.height() - pad * 2f).coerceAtLeast(8f)
    val scale = minOf(availW / spanX, availH / spanY)
    val offsetX = rect.left + pad + (availW - spanX * scale) / 2f
    val offsetY = rect.top + pad + (availH - spanY * scale) / 2f

    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyColor
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }
    for (pts in strokes) {
      if (pts.size == 1) {
        canvas.drawCircle(
          offsetX + (pts[0].first - minX) * scale,
          offsetY + (pts[0].second - minY) * scale,
          1.4f,
          strokePaint,
        )
      } else if (pts.size > 1) {
        val path = Path()
        path.moveTo(offsetX + (pts[0].first - minX) * scale, offsetY + (pts[0].second - minY) * scale)
        for (i in 1 until pts.size) {
          path.lineTo(offsetX + (pts[i].first - minX) * scale, offsetY + (pts[i].second - minY) * scale)
        }
        canvas.drawPath(path, strokePaint)
      }
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
    canvas.drawBitmap(
      bitmap,
      Rect(0, 0, bitmap.width, bitmap.height),
      RectF(left, top, left + drawnW, top + drawnH),
      Paint(Paint.FILTER_BITMAP_FLAG),
    )
  }
}
