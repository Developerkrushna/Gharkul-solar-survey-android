package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CustomerSurvey
import com.example.model.SurveyCategory
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.SyncErrorRetryBanner
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CustomerStatusFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
  searchQuery: String,
  selectedCategory: SurveyCategory?,
  selectedStatus: CustomerStatusFilter,
  surveys: List<CustomerSurvey>,
  isCloudConfigured: Boolean = false,
  isSyncing: Boolean = false,
  syncErrorMessage: String? = null,
  onRefresh: () -> Unit = {},
  onSearchChange: (String) -> Unit,
  onCategorySelect: (SurveyCategory?) -> Unit,
  onStatusSelect: (CustomerStatusFilter) -> Unit,
  onCustomerClick: (CustomerSurvey) -> Unit,
  onDeleteClick: (CustomerSurvey) -> Unit,
  modifier: Modifier = Modifier,
) {
  PullToRefreshBox(
    isRefreshing = isSyncing,
    onRefresh = { if (isCloudConfigured) onRefresh() },
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SyncErrorRetryBanner(
          errorMessage = syncErrorMessage,
          isSyncing = isSyncing,
          onRetry = onRefresh,
        )
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.fillMaxWidth().testTag("list_search_input"),
          placeholder = { Text("नाव, मोबाईल, घरकुल आयडी किंवा मीटर क्र. शोधा", color = TextMuted) },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "शोध पुसा", tint = TextMuted)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardWhite,
              unfocusedContainerColor = CardWhite,
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )

        // Status filter chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CustomerStatusFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedStatus == filter,
              onClick = { onStatusSelect(filter) },
              label = { Text(filter.labelMr) },
              modifier = Modifier.testTag("status_filter_${filter.name}"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandNavy,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }

        // Category 1-4 filter chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelect(null) },
            label = { Text("सर्व श्रेणी") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandCrimson,
                selectedLabelColor = Color.White,
              ),
          )
          SurveyCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { onCategorySelect(cat) },
              label = { Text("श्रेणी ${cat.number}") },
              modifier = Modifier.testTag("cat_filter_${cat.number}"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandCrimson,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
      }

      if (surveys.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
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
                text = "कोणतेही ग्राहक आढळले नाहीत",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "कृपया शोध किंवा श्रेणी फिल्टर बदलून पहा.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(surveys, key = { it.id }) { customer ->
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("list_customer_${customer.id}"),
              shape = RoundedCornerShape(14.dp),
              color = CardWhite,
              border = BorderStroke(1.dp, BorderSubtle),
              onClick = { onCustomerClick(customer) },
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = customer.beneficiaryName,
                      style = MaterialTheme.typography.titleMedium,
                      color = TextPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "${customer.mobile} · ${customer.casteCategory.labelMr} · ${customer.capacity.labelMr}",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                    )
                  }
                  StatusPillBadge(status = customer.overallStatus)
                  IconButton(
                    onClick = { onDeleteClick(customer) },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("delete_customer_card_${customer.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "${customer.beneficiaryName} हटवा",
                      tint = BrandCrimson,
                    )
                  }
                }

                Text(
                  text = customer.finalCategory.shortTagMr,
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandNavy,
                  fontWeight = FontWeight.SemiBold,
                )

                if (customer.pendingActions.isNotEmpty()) {
                  Text(
                    text = "बाकी: ${customer.pendingActions.joinToString(", ") { it.titleMr }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandCrimson,
                  )
                }
              }
            }
          }
          item { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    }
  }
}
