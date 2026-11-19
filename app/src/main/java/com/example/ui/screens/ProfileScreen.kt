package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExecutiveUser
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
  executive: ExecutiveUser,
  villageBreadcrumb: String,
  onChangeVillageClick: () -> Unit,
  onLogoutClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = CardWhite,
          border = BorderStroke(1.dp, BorderSubtle),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier.size(56.dp).clip(CircleShape).background(BannerIceBlue),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "प्रोफाइल",
                tint = BrandNavy,
                modifier = Modifier.size(30.dp),
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = executive.nameMr,
                style = MaterialTheme.typography.titleLarge,
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = executive.roleMr,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
              Text(
                text = "${executive.email} · ${executive.mobile}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
              )
            }
          }
        }
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = BannerIceBlue,
          border = BorderStroke(1.dp, BorderSubtle),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.LocationOn,
              contentDescription = "सध्याचे गाव",
              tint = BrandNavy,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "सध्या निवडलेले गाव",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
              )
              Text(
                text = villageBreadcrumb,
                style = MaterialTheme.typography.titleSmall,
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = onChangeVillageClick,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("change_village_button"),
            ) {
              Text("गाव बदला", color = BrandNavy, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      item {
        OutlinedButton(
          onClick = onLogoutClick,
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("logout_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, BrandCrimson),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandCrimson),
        ) {
          Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "लॉगआउट करा",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
