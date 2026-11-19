package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.CasteCategory
import com.example.model.HouseType
import com.example.model.MeterInfo
import com.example.model.RoofType
import com.example.model.SiteSurveyDetails
import com.example.ui.components.NullableYesNoPillGroup
import com.example.ui.components.SegmentedChoiceButton
import com.example.ui.components.YesNoPillGroup
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.DividerLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberText
import com.example.viewmodel.AddEditWizardState
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStep1BasicAndMeter(
  wizard: AddEditWizardState,
  villageNameMr: String,
  talukaNameMr: String,
  onUpdateBasic: (
    surveyDate: String?,
    beneficiaryName: String?,
    mobile: String?,
    emailId: String?,
    addressLandmark: String?,
    gharkulId: String?,
    aadhaarLast4: String?,
    casteCategory: CasteCategory?,
    houseType: HouseType?,
    gpsLocation: String?,
  ) -> Unit,
  onCaptureGps: () -> Unit,
  onUpdateMeter: ((MeterInfo) -> MeterInfo) -> Unit,
  onUpdateSiteSurvey: ((SiteSurveyDetails) -> SiteSurveyDetails) -> Unit,
) {
  val context = LocalContext.current

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (wizard.duplicateWarnings.isNotEmpty()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = WarningAmberBg,
        border = BorderStroke(1.dp, WarningAmberText.copy(alpha = 0.35f)),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = "दुबार नोंद चेतावणी",
            tint = WarningAmberText,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "दुबार नोंदीची शक्यता:",
              style = MaterialTheme.typography.titleSmall,
              color = WarningAmberText,
              fontWeight = FontWeight.Bold,
            )
            wizard.duplicateWarnings.forEach { msg ->
              Text(text = "• $msg", style = MaterialTheme.typography.bodySmall, color = WarningAmberText)
            }
          }
        }
      }
    }

    // Mandatory marker legend + Survey Date + Village/Taluka summary
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = BannerIceBlue,
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text =
            buildAnnotatedString {
              withStyle(SpanStyle(color = BrandCrimson, fontWeight = FontWeight.Bold)) { append("* ") }
              withStyle(SpanStyle(color = BrandNavy, fontWeight = FontWeight.SemiBold)) {
                append("असलेली माहिती भरणे अनिवार्य आहे. (गाव: $villageNameMr · तालुका: $talukaNameMr)")
              }
            },
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // * सर्व्हे दिनांक
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      MandatoryLabel("सर्व्हे दिनांक", isMandatory = true)
      Surface(
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("survey_date_picker"),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
        onClick = {
          val cal = Calendar.getInstance()
          DatePickerDialog(
            context,
            { _, year, month, day ->
              val formatted = String.format(Locale.getDefault(), "%02d / %02d / %d", day, month + 1, year)
              onUpdateBasic(formatted, null, null, null, null, null, null, null, null, null)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
          ).show()
        },
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = wizard.surveyDate,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
          )
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "दिनांक निवडा",
            tint = BrandNavy,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    // * प्रवर्ग (OPEN / SC / ST)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      MandatoryLabel("प्रवर्ग", isMandatory = true)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CasteCategory.entries.forEach { caste ->
          SegmentedChoiceButton(
            label = caste.labelMr,
            selected = wizard.casteCategory == caste,
            onClick = { onUpdateBasic(null, null, null, null, null, null, null, caste, null, null) },
            modifier = Modifier.weight(1f),
            testTag = "caste_${caste.name}",
          )
        }
      }
    }

    // * सोलर क्षमता (Focused on 1 kW)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      MandatoryLabel("सोलर क्षमता", isMandatory = true)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BrandNavy,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "1 kW (घरकुल योजना)",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "निवडलेले",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }
    }

    // * घरकुलावरील नाव (मालकाचे नाव)
    LabeledTextField(
      label = "घरकुलावरील नाव (मालकाचे नाव)",
      isMandatory = true,
      value = wizard.beneficiaryName,
      placeholder = "उदा. सुनीता शिंदे",
      onValueChange = { onUpdateBasic(null, it, null, null, null, null, null, null, null, null) },
      testTag = "wizard_name_input",
    )

    // * मोबाईल नं. (आधार व बँकेशी लिंक असलेला) — 10 digit limit
    LabeledTextField(
      label = "मोबाईल नं. (आधार व बँकेशी लिंक असलेला)",
      isMandatory = true,
      value = wizard.mobile,
      placeholder = "१० अंकी चालू मोबाईल नंबर",
      supportingText = "${wizard.mobile.length} / 10 अंक",
      onValueChange = { input ->
        val digits = input.filter { it.isDigit() }.take(10)
        onUpdateBasic(null, null, digits, null, null, null, null, null, null, null)
      },
      keyboardType = KeyboardType.Phone,
      testTag = "wizard_mobile_input",
    )

    // * ई-मेल आयडी (E-mail ID)
    LabeledTextField(
      label = "ई-मेल आयडी (E-mail ID)",
      isMandatory = true,
      value = wizard.emailId,
      placeholder = "name@gmail.com",
      onValueChange = { onUpdateBasic(null, null, null, it.replace(" ", ""), null, null, null, null, null, null) },
      keyboardType = KeyboardType.Email,
      testTag = "wizard_email_input",
    )

    LabeledTextField(
      label = "पत्ता / जवळची खूण",
      isMandatory = false,
      value = wizard.addressLandmark,
      placeholder = "प्रभाग, गल्ली किंवा जवळची खूण",
      onValueChange = { onUpdateBasic(null, null, null, null, it, null, null, null, null, null) },
      testTag = "wizard_address_input",
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      LabeledTextField(
        label = "घरकुल आयडी / मंजुरी क्र.",
        isMandatory = false,
        value = wizard.gharkulId,
        placeholder = "GH-1042",
        onValueChange = { onUpdateBasic(null, null, null, null, null, it, null, null, null, null) },
        modifier = Modifier.weight(1f),
        testTag = "wizard_gharkul_id_input",
      )
      LabeledTextField(
        label = "आधार शेवटचे ४ अंक",
        isMandatory = false,
        value = wizard.aadhaarLast4,
        placeholder = "४ अंक",
        onValueChange = { input ->
          onUpdateBasic(null, null, null, null, null, null, input.filter { it.isDigit() }.take(4), null, null, null)
        },
        keyboardType = KeyboardType.Number,
        modifier = Modifier.weight(1f),
        testTag = "wizard_aadhaar_last4_input",
      )
    }

    // GPS Location row
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.MyLocation,
          contentDescription = "GPS लोकेशन",
          tint = BrandNavy,
          modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "घराचे GPS लोकेशन (पर्यायी)",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
          )
          Text(
            text = wizard.gpsLocation ?: "अद्याप नोंदवलेले नाही",
            style = MaterialTheme.typography.bodySmall,
            color = if (wizard.gpsLocation != null) BrandNavy else TextMuted,
          )
        }
        OutlinedButton(
          onClick = onCaptureGps,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("capture_gps_button"),
        ) {
          Text(text = if (wizard.gpsLocation == null) "नोंदवा" else "अपडेट")
        }
      }
    }

    // Electricity Meter & New Connection Section
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "वीज मीटर व नवीन कनेक्शन माहिती",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        MandatoryYesNoRow(
          label = "लाईट मीटर उपलब्ध आहे का?",
          isMandatory = true,
          value = wizard.meterInfo.hasMeter,
          onValueChange = { has ->
            onUpdateMeter {
              it.copy(
                hasMeter = has,
                sameAsBeneficiary = if (has) it.sameAsBeneficiary else false,
              )
            }
          },
          testTagPrefix = "meter_available",
        )

        MandatoryNullableYesNoRow(
          label = "नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?",
          isMandatory = true,
          value = wizard.meterInfo.isPoleNearbyForNewConn,
          onValueChange = { near -> onUpdateMeter { it.copy(isPoleNearbyForNewConn = near) } },
          testTagPrefix = "pole_nearby",
        )

        MandatoryNullableYesNoRow(
          label = "नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?",
          isMandatory = true,
          value = wizard.meterInfo.interestedInNewConnection,
          onValueChange = { interested ->
            onUpdateMeter { it.copy(interestedInNewConnection = interested) }
          },
          testTagPrefix = "new_conn_interested",
        )

        AnimatedVisibility(visible = wizard.meterInfo.hasMeter) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(color = DividerLight)

            LabeledTextField(
              label = "ग्राहक क्रमांक (Consumer No.)",
              isMandatory = false,
              value = wizard.meterInfo.consumerNumber,
              placeholder = "१२ अंकी ग्राहक क्रमांक",
              supportingText =
                if (wizard.meterInfo.consumerNumber.isNotEmpty())
                  "${wizard.meterInfo.consumerNumber.length} / 12 अंक"
                else "भरल्यास १२ अंकी ग्राहक क्रमांक आवश्यक",
              onValueChange = { num ->
                onUpdateMeter { it.copy(consumerNumber = num.filter { c -> c.isDigit() }.take(12)) }
              },
              keyboardType = KeyboardType.Number,
              testTag = "meter_consumer_no_input",
            )

            MandatoryYesNoRow(
              label = "मीटर लाभार्थ्याच्या नावावर आहे का?",
              isMandatory = true,
              value = wizard.meterInfo.sameAsBeneficiary,
              onValueChange = { same ->
                onUpdateMeter {
                  it.copy(
                    sameAsBeneficiary = same,
                    meterHolderName = if (same) wizard.beneficiaryName else it.meterHolderName,
                  )
                }
              },
              testTagPrefix = "meter_same_person",
            )

            if (wizard.meterInfo.sameAsBeneficiary) {
              LabeledTextField(
                label = "लाईट बिलावरील नाव",
                isMandatory = true,
                value = wizard.meterInfo.meterHolderName.ifEmpty { wizard.beneficiaryName },
                placeholder = "लाईट बिलावरील नाव",
                onValueChange = { name -> onUpdateMeter { it.copy(meterHolderName = name) } },
                testTag = "meter_holder_name_input",
              )
            } else {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = BannerIceBlue,
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "श्रेणी 3 · मीटर घरातील दुसऱ्या व्यक्तीच्या नावावर आहे",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                  )
                  LabeledTextField(
                    label = "लाईट बिलावरील नाव (मीटर धारक)",
                    isMandatory = true,
                    value = wizard.meterInfo.meterHolderName,
                    placeholder = "ज्यांच्या नावे मीटर आहे त्यांचे नाव",
                    onValueChange = { name -> onUpdateMeter { it.copy(meterHolderName = name) } },
                    testTag = "meter_other_holder_name_input",
                  )

                  Text(
                    text = "लाभार्थ्याशी नाते",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    listOf("पती", "पत्नी", "वडील", "आई", "मुलगा", "भाऊ").forEach { rel ->
                      FilterChip(
                        selected = wizard.meterInfo.relationship == rel,
                        onClick = { onUpdateMeter { it.copy(relationship = rel) } },
                        label = { Text(rel) },
                        colors =
                          FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandNavy,
                            selectedLabelColor = CardWhite,
                          ),
                      )
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "मीटर धारक हयात आहेत का?",
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextPrimary,
                      modifier = Modifier.weight(1f),
                    )
                    YesNoPillGroup(
                      value = wizard.meterInfo.isHolderAlive,
                      onValueChange = { alive -> onUpdateMeter { it.copy(isHolderAlive = alive) } },
                      yesLabel = "हयात",
                      noLabel = "मृत",
                      testTagPrefix = "meter_holder_alive",
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // १. साईट सर्व्हे माहिती (Site Survey Details) from PDF Page 1
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = "१. साईट सर्व्हे माहिती (Site Survey Details)",
          style = MaterialTheme.typography.titleMedium,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          MandatoryLabel("छताचा प्रकार", isMandatory = true)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            RoofType.entries.forEach { roof ->
              FilterChip(
                selected = wizard.siteSurveyDetails.roofType == roof,
                onClick = { onUpdateSiteSurvey { it.copy(roofType = roof) } },
                label = { Text(roof.labelMr) },
                modifier = Modifier.testTag("roof_${roof.name}"),
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandNavy,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
        }

        MandatoryNullableYesNoRow(
          label = "सोलर पॅनेलसाठी जागा उपलब्ध आहे का? (किमान १०० चौ.फूट)",
          isMandatory = true,
          value = wizard.siteSurveyDetails.hasSolarPanelSpace100SqFt,
          onValueChange = { has -> onUpdateSiteSurvey { it.copy(hasSolarPanelSpace100SqFt = has) } },
          testTagPrefix = "solar_space_100sqft",
        )

        LabeledTextField(
          label = "जनरेशन मीटर बसवण्याची जागा",
          isMandatory = true,
          value = wizard.siteSurveyDetails.generationMeterLocation,
          placeholder = "उदा. मुख्य दरवाजाजवळ भिंतीवर / पोर्च",
          onValueChange = { loc -> onUpdateSiteSurvey { it.copy(generationMeterLocation = loc) } },
          testTag = "generation_meter_loc_input",
        )

        MandatoryNullableYesNoRow(
          label = "अर्थिंगसाठी (Earthing) जागा उपलब्ध आहे का?",
          isMandatory = true,
          value = wizard.siteSurveyDetails.hasEarthingSpace,
          onValueChange = { has -> onUpdateSiteSurvey { it.copy(hasEarthingSpace = has) } },
          testTagPrefix = "earthing_space",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MandatoryLabel(
            text = "छत किंवा घर एकच मजली आहे का?",
            isMandatory = true,
            modifier = Modifier.weight(1f).padding(end = 10.dp),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HouseType.entries.forEach { house ->
              val selected = wizard.houseType == house
              Surface(
                modifier = Modifier.height(48.dp).testTag("house_${house.name}"),
                shape = RoundedCornerShape(10.dp),
                color = if (selected) BrandNavy else CardWhite,
                border = BorderStroke(1.dp, if (selected) BrandNavy else BorderSubtle),
                onClick = { onUpdateBasic(null, null, null, null, null, null, null, null, house, null) },
              ) {
                Box(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = house.labelMr,
                    color = if (selected) Color.White else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
  }
}

@Composable
fun MandatoryLabel(
  text: String,
  isMandatory: Boolean,
  modifier: Modifier = Modifier,
) {
  Text(
    text =
      buildAnnotatedString {
        if (isMandatory) {
          withStyle(SpanStyle(color = BrandCrimson, fontWeight = FontWeight.Bold)) { append("* ") }
        }
        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) { append(text) }
      },
    style = MaterialTheme.typography.titleSmall,
    modifier = modifier,
  )
}

@Composable
fun LabeledTextField(
  label: String,
  value: String,
  placeholder: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  isMandatory: Boolean = false,
  supportingText: String? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
  testTag: String = "",
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    MandatoryLabel(text = label, isMandatory = isMandatory)
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
        Modifier.fillMaxWidth()
          .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
      placeholder = { Text(placeholder, color = TextMuted) },
      supportingText =
        supportingText?.let { msg ->
          { Text(text = msg, style = MaterialTheme.typography.labelSmall, color = TextMuted) }
        },
      singleLine = singleLine,
      shape = RoundedCornerShape(14.dp),
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = CardWhite,
          unfocusedContainerColor = CardWhite,
          focusedBorderColor = BrandNavy,
          unfocusedBorderColor = BorderSubtle,
        ),
    )
  }
}

@Composable
private fun MandatoryYesNoRow(
  label: String,
  isMandatory: Boolean,
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  testTagPrefix: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MandatoryLabel(
      text = label,
      isMandatory = isMandatory,
      modifier = Modifier.weight(1f).padding(end = 10.dp),
    )
    YesNoPillGroup(
      value = value,
      onValueChange = onValueChange,
      testTagPrefix = testTagPrefix,
    )
  }
}

@Composable
private fun MandatoryNullableYesNoRow(
  label: String,
  isMandatory: Boolean,
  value: Boolean?,
  onValueChange: (Boolean) -> Unit,
  testTagPrefix: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MandatoryLabel(
      text = label,
      isMandatory = isMandatory,
      modifier = Modifier.weight(1f).padding(end = 10.dp),
    )
    NullableYesNoPillGroup(
      value = value,
      onValueChange = onValueChange,
      testTagPrefix = testTagPrefix,
    )
  }
}
