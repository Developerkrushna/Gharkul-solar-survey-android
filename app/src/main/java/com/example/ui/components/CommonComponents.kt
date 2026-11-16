package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MainTab
import com.example.model.OverallStatus
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.PendingRoseBg
import com.example.ui.theme.PendingRoseText
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun BrandTopBar(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  showBackButton: Boolean = false,
  onBackClick: (() -> Unit)? = null,
  stepProgress: Int? = null, // 1, 2, or 3
  trailingAction: (@Composable () -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().background(CardWhite).statusBarsPadding()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showBackButton && onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("top_bar_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "मागे जा",
            tint = BrandNavy,
          )
        }
      }
      Image(
        painter = painterResource(id = R.drawable.brand_logo),
        contentDescription = "घरकुल सोलर सर्व्हे लोगो",
        modifier = Modifier.size(44.dp),
        contentScale = ContentScale.Fit,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = BrandNavy,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = TextMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (trailingAction != null) {
        Spacer(modifier = Modifier.width(8.dp))
        trailingAction()
      }
    }

    if (stepProgress != null) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (step in 1..3) {
          val active = step <= stepProgress
          Box(
            modifier =
              Modifier.weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(if (active) BrandNavy else Color(0xFFDCE3F0))
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
    }

    HorizontalDivider(thickness = 2.dp, color = BrandCrimson)
  }
}

@Composable
fun GharkulBottomBar(
  activeTab: MainTab,
  onSelectTab: (MainTab) -> Unit,
  onAddNewClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = CardWhite,
    shadowElevation = 8.dp,
  ) {
    Column {
      HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomNavItem(
          label = "होम",
          selected = activeTab == MainTab.HOME,
          icon = {
            Icon(
              imageVector = if (activeTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
              contentDescription = "होम",
              tint = if (activeTab == MainTab.HOME) BrandNavy else TextMuted,
              modifier = Modifier.size(24.dp),
            )
          },
          onClick = { onSelectTab(MainTab.HOME) },
          tag = "nav_home",
        )

        BottomNavItem(
          label = "ग्राहक",
          selected = activeTab == MainTab.CUSTOMERS,
          icon = {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ListAlt,
              contentDescription = "ग्राहक",
              tint = if (activeTab == MainTab.CUSTOMERS) BrandNavy else TextMuted,
              modifier = Modifier.size(24.dp),
            )
          },
          onClick = { onSelectTab(MainTab.CUSTOMERS) },
          tag = "nav_customers",
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onAddNewClick)
              .minimumInteractiveComponentSize()
              .padding(horizontal = 10.dp, vertical = 2.dp)
              .testTag("nav_add_new"),
        ) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandCrimson),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "नवीन ग्राहक",
              tint = Color.White,
              modifier = Modifier.size(26.dp),
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "नवीन",
            style = MaterialTheme.typography.labelSmall,
            color = BrandCrimson,
            fontWeight = FontWeight.Bold,
          )
        }

        BottomNavItem(
          label = "प्रोफाइल",
          selected = activeTab == MainTab.PROFILE,
          icon = {
            Icon(
              imageVector = if (activeTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
              contentDescription = "प्रोफाइल",
              tint = if (activeTab == MainTab.PROFILE) BrandNavy else TextMuted,
              modifier = Modifier.size(24.dp),
            )
          },
          onClick = { onSelectTab(MainTab.PROFILE) },
          tag = "nav_profile",
        )
      }
    }
  }
}

@Composable
private fun BottomNavItem(
  label: String,
  selected: Boolean,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  tag: String,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier.clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(horizontal = 14.dp, vertical = 6.dp)
        .testTag(tag),
  ) {
    icon()
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) BrandNavy else TextMuted,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
  }
}

@Composable
fun StatusPillBadge(
  status: OverallStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg, label) =
    when (status) {
      OverallStatus.READY -> Triple(BrandNavy, Color.White, "तयार")
      OverallStatus.PENDING_CUSTOMER -> Triple(PendingRoseBg, PendingRoseText, "अपूर्ण")
      OverallStatus.PENDING_COMPANY -> Triple(PendingRoseBg, PendingRoseText, "अपूर्ण")
    }
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(bg)
        .padding(horizontal = 14.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun SegmentedChoiceButton(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selectedColor: Color = BrandNavy,
  enabled: Boolean = true,
  testTag: String = "",
) {
  val bg =
    when {
      !enabled -> Color(0xFFF3F4F6)
      selected -> selectedColor
      else -> CardWhite
    }
  val textColor =
    when {
      !enabled -> Color(0xFF9CA3AF)
      selected -> Color.White
      else -> Color(0xFF1F2937)
    }
  val borderColor = if (selected) selectedColor else BorderSubtle

  Surface(
    modifier =
      modifier
        .height(48.dp)
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = BorderStroke(1.dp, borderColor),
    onClick = onClick,
    enabled = enabled,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
      Text(
        text = label,
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun YesNoPillGroup(
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  yesLabel: String = "होय",
  noLabel: String = "नाही",
  testTagPrefix: String = "yn",
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    YesNoSmallPill(
      text = yesLabel,
      selected = value,
      activeColor = BrandNavy,
      onClick = { onValueChange(true) },
      modifier = Modifier.testTag("${testTagPrefix}_yes"),
    )
    YesNoSmallPill(
      text = noLabel,
      selected = !value,
      activeColor = BrandCrimson,
      onClick = { onValueChange(false) },
      modifier = Modifier.testTag("${testTagPrefix}_no"),
    )
  }
}

@Composable
private fun YesNoSmallPill(
  text: String,
  selected: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(44.dp).width(68.dp),
    shape = RoundedCornerShape(10.dp),
    color = if (selected) activeColor else CardWhite,
    border = BorderStroke(1.dp, if (selected) activeColor else BorderSubtle),
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text,
        color = if (selected) Color.White else TextSecondary,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun StatusTagChip(
  text: String,
  bgColor: Color = SuccessGreenBg,
  textColor: Color = SuccessGreenText,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(8.dp))
        .background(bgColor)
        .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      color = textColor,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
fun DeleteCustomerConfirmationDialog(
  beneficiaryName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardWhite,
    icon = {
      Icon(
        imageVector = Icons.Outlined.DeleteOutline,
        contentDescription = null,
        tint = BrandCrimson,
        modifier = Modifier.size(32.dp),
      )
    },
    title = {
      Text(
        text = "ग्राहक कायमचा हटवायचा का?",
        style = MaterialTheme.typography.titleLarge,
        color = BrandNavy,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = "$beneficiaryName यांची संपूर्ण सर्व्हे माहिती, सह्या आणि जोडलेले सर्व कागदपत्रांचे फोटो कायमचे हटवले जातील. ही कृती पूर्ववत करता येणार नाही.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson, contentColor = Color.White),
        modifier = Modifier.height(48.dp).testTag("confirm_delete_customer_button"),
      ) {
        Text(text = "होय, कायमचे हटवा", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp).testTag("cancel_delete_customer_button"),
      ) {
        Text(text = "रद्द करा", color = BrandNavy, fontWeight = FontWeight.SemiBold)
      }
    },
  )
}
