package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstallationProductRow(
  val rowId: String,
  val srNoDisplay: String,
  val productModel: String,
  val isSubRow: Boolean = false,
  val isQtyRequired: Boolean = false,
  val company: String = "",
  val size: String = "",
  val capacity: String = "",
  val qty: String = "",
)

data class SerialVerificationField(
  val primaryValue: String = "",
  val confirmValue: String = "",
) {
  val trimmedPrimary: String get() = primaryValue.trim()
  val trimmedConfirm: String get() = confirmValue.trim()
  val isFilled: Boolean get() = trimmedPrimary.isNotEmpty() && trimmedConfirm.isNotEmpty()
  val isMatch: Boolean get() = isFilled && trimmedPrimary == trimmedConfirm
  val hasMismatch: Boolean get() = trimmedConfirm.isNotEmpty() && trimmedPrimary != trimmedConfirm
}

data class PanelSerialEntry(
  val id: String,
  val serial: SerialVerificationField = SerialVerificationField(),
)

data class InstallationPhotoItem(
  val id: String,
  val caption: String = "Solar Site Photo (After Installation)",
  val photoUri: String? = null,
  val photoSizeKb: Int? = null,
  val geoInfo: String = "",
  val isCameraGeoStamped: Boolean = false,
)

enum class InstallationTab {
  HOME,
  PROFILE,
}

data class InstallationReport(
  val id: String,
  val linkedSurveyId: String? = null,
  val villageName: String = "",
  val consumerNo: String = "",
  val installationDate: String = defaultInstallationDate(),
  val consumerName: String = "",
  val mobileNo: String = "",
  val systemCapacity: String = "1 KW",
  val systemType: String = "ONGRID",
  val products: List<InstallationProductRow> = defaultInstallationProductRows(),
  val installerName: String = "",
  val installerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
  val accountantName: String = "",
  val managerName: String = "",
  val supervisorName: String = "",
  val customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList(),
  val inverterSerial: SerialVerificationField = SerialVerificationField(),
  val generationMeterSerial: SerialVerificationField = SerialVerificationField(),
  val panelSerials: List<PanelSerialEntry> = defaultPanelSerialEntries(),
  val afterInstallationPhotos: List<InstallationPhotoItem> = defaultInstallationPhotos(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class InstallationQueueFilter(val labelEn: String) {
  ALL("All"),
  PENDING("Pending"),
  COMPLETED("Completed"),
}

fun defaultInstallationDate(): String =
  SimpleDateFormat("dd / MM / yyyy", Locale.ENGLISH).format(Date())

fun defaultPanelSerialEntries(): List<PanelSerialEntry> =
  listOf(
    PanelSerialEntry(id = "panel_1"),
    PanelSerialEntry(id = "panel_2"),
  )

fun defaultInstallationPhotos(): List<InstallationPhotoItem> =
  listOf(
    InstallationPhotoItem(
      id = "site_photo_1",
      caption = "Solar Site Photo (After Installation)",
    )
  )

fun defaultInstallationProductRows(): List<InstallationProductRow> =
  listOf(
    InstallationProductRow(
      rowId = "1",
      srNoDisplay = "1.",
      productModel = "PANEL'S",
      isQtyRequired = true,
      qty = "02",
    ),
    InstallationProductRow(
      rowId = "2",
      srNoDisplay = "2.",
      productModel = "INVERTER",
      isQtyRequired = true,
      qty = "01",
    ),
    InstallationProductRow(
      rowId = "2A",
      srNoDisplay = "",
      productModel = "A) LA Cable 16 sq mm",
      isSubRow = true,
      isQtyRequired = true,
      qty = "20 mtr",
    ),
    InstallationProductRow(
      rowId = "2B",
      srNoDisplay = "",
      productModel = "B) AC Cable 1.5 sq mm",
      isSubRow = true,
      isQtyRequired = true,
      qty = "10 mtr",
    ),
    InstallationProductRow(
      rowId = "2C",
      srNoDisplay = "",
      productModel = "C) DC Cable",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "2D",
      srNoDisplay = "",
      productModel = "D) Earthing Cable",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "3",
      srNoDisplay = "3.",
      productModel = "ACDB",
      isQtyRequired = true,
      qty = "01",
    ),
    InstallationProductRow(
      rowId = "4",
      srNoDisplay = "4.",
      productModel = "EARTHING RODE",
      isQtyRequired = true,
      qty = "02",
    ),
    InstallationProductRow(
      rowId = "5",
      srNoDisplay = "5.",
      productModel = "L.A.",
      isQtyRequired = true,
      qty = "01",
    ),
    InstallationProductRow(
      rowId = "6",
      srNoDisplay = "6.",
      productModel = "CHEMICAL BAG",
      isQtyRequired = true,
      qty = "01",
    ),
    InstallationProductRow(
      rowId = "7A",
      srNoDisplay = "7.",
      productModel = "A) PVC PIPE",
      isSubRow = true,
      isQtyRequired = true,
      qty = "08",
    ),
    InstallationProductRow(
      rowId = "7B",
      srNoDisplay = "",
      productModel = "B) PVC CONDUIT",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "7C",
      srNoDisplay = "",
      productModel = "C) PVC ELBOW",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "7D",
      srNoDisplay = "",
      productModel = "D) PVC T",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "7E",
      srNoDisplay = "",
      productModel = "E) PVC CLAMP",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "7F",
      srNoDisplay = "",
      productModel = "F) PVC LONG BEND",
      isSubRow = true,
    ),
    InstallationProductRow(
      rowId = "8",
      srNoDisplay = "8.",
      productModel = "MC 4 CONNECTOR",
    ),
    InstallationProductRow(
      rowId = "9",
      srNoDisplay = "9.",
      productModel = "LUG FOR STRUCTURE",
    ),
    InstallationProductRow(
      rowId = "10",
      srNoDisplay = "10.",
      productModel = "LUG FOR ACDB",
    ),
    InstallationProductRow(
      rowId = "11",
      srNoDisplay = "11.",
      productModel = "LUG FOR L.A.",
      isQtyRequired = true,
      qty = "02",
    ),
    InstallationProductRow(
      rowId = "12",
      srNoDisplay = "12.",
      productModel = "EARTH COVER PIT",
      isQtyRequired = true,
      qty = "02",
    ),
    InstallationProductRow(
      rowId = "13",
      srNoDisplay = "13.",
      productModel = "GENERATION METER",
      isQtyRequired = true,
      qty = "01",
    ),
    InstallationProductRow(
      rowId = "14",
      srNoDisplay = "14.",
      productModel = "EXTRA WIRING",
    ),
  )
