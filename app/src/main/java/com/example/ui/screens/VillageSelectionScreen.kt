package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.District
import com.example.model.Taluka
import com.example.model.Village
import com.example.model.VillageStats
import com.example.ui.components.BrandTopBar
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.DividerLight
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class PickerMode {
  DISTRICT,
  TALUKA,
  VILLAGE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageSelectionScreen(
  executiveFirstName: String,
  districts: List<District>,
  selectedDistrict: District?,
  selectedTaluka: Taluka?,
  selectedVillage: Village?,
  villageStats: VillageStats,
  onSelectDistrict: (District) -> Unit,
  onSelectTaluka: (Taluka) -> Unit,
  onSelectVillage: (Village) -> Unit,
  onAddCustomVillage: (String) -> Unit,
  onProceedClick: () -> Unit,
  onBackToDashboard: () -> Unit,
) {
  BackHandler { onBackToDashboard() }

  var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var customVillageInput by remember { mutableStateOf("") }

  Scaffold(
    containerColor = SurfaceCanvas,
    topBar = {
      BrandTopBar(
        title = "नमस्कार, $executiveFirstName",
        subtitle = "सर्व्हे सुरू करण्यासाठी गाव निवडा",
      )
    },
    bottomBar = {
      Surface(color = SurfaceCanvas) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 20.dp, vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Button(
            onClick = onProceedClick,
            enabled = selectedVillage != null,
            modifier =
              Modifier.widthIn(max = 600.dp)
                .fillMaxWidth()
                .height(54.dp)
                .testTag("view_customer_list_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
          ) {
            Text(
              text = "ग्राहक यादी पहा",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        LocationPickerField(
          label = "जिल्हा",
          value = selectedDistrict?.nameMr ?: "जिल्हा निवडा",
          helper = "${districts.size} जिल्हे उपलब्ध",
          onClick = {
            searchQuery = ""
            pickerMode = PickerMode.DISTRICT
          },
          tag = "district_selector",
        )

        LocationPickerField(
          label = "तालुका",
          value = selectedTaluka?.nameMr ?: "तालुका निवडा",
          helper = selectedDistrict?.let { "${it.nameMr} जिल्ह्यातील ${it.talukas.size} तालुके" },
          onClick = {
            searchQuery = ""
            pickerMode = PickerMode.TALUKA
          },
          tag = "taluka_selector",
        )

        LocationPickerField(
          label = "गाव",
          value = selectedVillage?.nameMr ?: "गाव निवडा",
          helper = selectedTaluka?.let { "${it.nameMr} तालुक्यातील गावे (${it.villages.size}+)" },
          onClick = {
            searchQuery = ""
            customVillageInput = ""
            pickerMode = PickerMode.VILLAGE
          },
          tag = "village_selector",
        )

        if (selectedVillage != null) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = BannerIceBlue,
            border = BorderStroke(1.dp, BorderSubtle),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "निवडलेले गाव: ${selectedVillage.nameMr} · ${selectedTaluka?.nameMr ?: ""} · ${selectedDistrict?.nameMr ?: ""}",
                style = MaterialTheme.typography.titleSmall,
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "आजचे सर्व्हे: ${villageStats.surveyedToday}  |  एकूण: ${villageStats.totalCount}  |  तयार: ${villageStats.readyCount}  |  अपूर्ण: ${villageStats.pendingCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }
    }
  }

  val activeMode = pickerMode
  if (activeMode != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { pickerMode = null },
      sheetState = sheetState,
      containerColor = CardWhite,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
      ) {
        Text(
          text =
            when (activeMode) {
              PickerMode.DISTRICT -> "जिल्हा निवडा"
              PickerMode.TALUKA -> "तालुका निवडा (${selectedDistrict?.nameMr ?: ""})"
              PickerMode.VILLAGE -> "गाव निवडा (${selectedTaluka?.nameMr ?: ""})"
            },
          style = MaterialTheme.typography.titleLarge,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (activeMode != PickerMode.DISTRICT) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("location_search_input"),
            placeholder = { Text("नाव शोधा (मराठी किंवा English)...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCanvas,
                unfocusedContainerColor = SurfaceCanvas,
                focusedBorderColor = BrandNavy,
                unfocusedBorderColor = BorderSubtle,
              ),
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
          when (activeMode) {
            PickerMode.DISTRICT -> {
              items(districts, key = { it.id }) { district ->
                PickerOptionRow(
                  primaryText = district.nameMr,
                  secondaryText = "${district.nameEn} · ${district.talukas.size} तालुके",
                  selected = district.id == selectedDistrict?.id,
                  onClick = {
                    onSelectDistrict(district)
                    pickerMode = null
                  },
                )
              }
            }
            PickerMode.TALUKA -> {
              val talukas =
                selectedDistrict?.talukas.orEmpty().filter {
                  searchQuery.isBlank() ||
                    it.nameMr.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.nameEn.contains(searchQuery.trim(), ignoreCase = true)
                }
              items(talukas, key = { it.id }) { taluka ->
                PickerOptionRow(
                  primaryText = taluka.nameMr,
                  secondaryText = "${taluka.nameEn} · ${taluka.villages.size} गावे",
                  selected = taluka.id == selectedTaluka?.id,
                  onClick = {
                    onSelectTaluka(taluka)
                    pickerMode = null
                  },
                )
              }
            }
            PickerMode.VILLAGE -> {
              val villages =
                selectedTaluka?.villages.orEmpty().filter {
                  searchQuery.isBlank() ||
                    it.nameMr.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.nameEn.contains(searchQuery.trim(), ignoreCase = true)
                }
              items(villages, key = { it.id }) { village ->
                PickerOptionRow(
                  primaryText = village.nameMr,
                  secondaryText = village.nameEn,
                  selected = village.id == selectedVillage?.id,
                  onClick = {
                    onSelectVillage(village)
                    pickerMode = null
                  },
                )
              }
            }
          }
        }

        if (activeMode == PickerMode.VILLAGE) {
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = DividerLight)
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "यादीत गाव / वाडी नसल्यास जोडा:",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = customVillageInput,
              onValueChange = { customVillageInput = it },
              modifier = Modifier.weight(1f),
              placeholder = { Text("गावाचे नाव लिहा", color = TextMuted) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
            )
            OutlinedButton(
              onClick = {
                if (customVillageInput.isNotBlank()) {
                  onAddCustomVillage(customVillageInput)
                  pickerMode = null
                }
              },
              modifier = Modifier.height(52.dp),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.AddLocationAlt, contentDescription = "गाव जोडा", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.size(4.dp))
              Text("जोडा")
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun LocationPickerField(
  label: String,
  value: String,
  helper: String?,
  onClick: () -> Unit,
  tag: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      color = TextPrimary,
    )
    Surface(
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag(tag),
      shape = RoundedCornerShape(14.dp),
      color = CardWhite,
      border = BorderStroke(1.dp, BorderSubtle),
      onClick = onClick,
    ) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = value,
          style = MaterialTheme.typography.bodyLarge,
          color = TextPrimary,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = Icons.Filled.KeyboardArrowDown,
          contentDescription = "$label निवडा",
          tint = TextPrimary,
        )
      }
    }
    if (!helper.isNullOrBlank()) {
      Text(
        text = helper,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
      )
    }
  }
}

@Composable
private fun PickerOptionRow(
  primaryText: String,
  secondaryText: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(vertical = 12.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = primaryText,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) BrandNavy else TextPrimary,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      )
      Text(
        text = secondaryText,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
      )
    }
    if (selected) {
      Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "निवडलेले",
        tint = BrandNavy,
      )
    }
  }
  HorizontalDivider(color = DividerLight)
}
