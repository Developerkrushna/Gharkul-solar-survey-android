package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerSurvey
import com.example.model.SurveyCategory
import com.example.model.VillageStats
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeDashboardScreen(
  stats: VillageStats,
  searchQuery: String,
  recentSurveys: List<CustomerSurvey>,
  onSearchChange: (String) -> Unit,
  onCustomerClick: (CustomerSurvey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          DashboardStatCard(
            count = stats.surveyedToday,
            label = "आजचे सर्व्हे",
            modifier = Modifier.weight(1f).testTag("stat_today"),
          )
          DashboardStatCard(
            count = stats.readyCount,
            label = "तयार",
            modifier = Modifier.weight(1f).testTag("stat_ready"),
          )
          DashboardStatCard(
            count = stats.pendingCount,
            label = "अपूर्ण",
            modifier = Modifier.weight(1f).testTag("stat_pending"),
          )
        }
      }

      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.fillMaxWidth().testTag("home_search_input"),
          placeholder = { Text("नाव किंवा मोबाईल शोधा", color = TextMuted) },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "शोध पुसा", tint = TextMuted)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardWhite,
              unfocusedContainerColor = CardWhite,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = BorderSubtle,
            ),
        )
      }

      item {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "अलीकडील ग्राहक",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
        )
      }

      if (recentSurveys.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = CardWhite,
            border = BorderStroke(1.dp, BorderSubtle),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "या गावात अद्याप ग्राहक नोंद नाही.",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "नवीन ग्राहक जोडण्यासाठी खालील '+' नवीन बटणावर टॅप करा.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
              )
            }
          }
        }
      } else {
        items(recentSurveys, key = { it.id }) { customer ->
          CustomerSummaryCard(
            customer = customer,
            onClick = { onCustomerClick(customer) },
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardStatCard(
  count: Int,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun CustomerSummaryCard(
  customer: CustomerSurvey,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val subtitleText =
    when (customer.finalCategory) {
      SurveyCategory.CATEGORY_1 -> "श्रेणी 1 · ${customer.capacity.labelMr}"
      SurveyCategory.CATEGORY_2 -> "श्रेणी 2 · नाव जुळत नाही"
      SurveyCategory.CATEGORY_3 -> "श्रेणी 3 · मीटर दुसऱ्याच्या नावावर"
      SurveyCategory.CATEGORY_4 -> "श्रेणी 4 · नवीन कनेक्शन"
    }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("customer_card_${customer.id}"),
    shape = RoundedCornerShape(14.dp),
    color = CardWhite,
    border = BorderStroke(1.dp, BorderSubtle),
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = customer.beneficiaryName,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.widthIn(min = 10.dp))
      StatusPillBadge(status = customer.overallStatus)
    }
  }
}
