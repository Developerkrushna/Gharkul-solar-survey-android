package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SolarPower
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LoginRole
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.PendingRoseBg
import com.example.ui.theme.PendingRoseText
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun LoginScreen(
  selectedRole: LoginRole = LoginRole.SURVEY_EXECUTIVE,
  email: String,
  password: String,
  errorMessage: String?,
  isAuthenticating: Boolean = false,
  isCloudConfigured: Boolean = false,
  onSelectRole: (LoginRole) -> Unit = {},
  onEmailChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
) {
  var passwordVisible by remember { mutableStateOf(false) }
  val isInstallation = selectedRole == LoginRole.SOLAR_INSTALLATION

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(SurfaceCanvas)
        .systemBarsPadding()
        .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      Image(
        painter = painterResource(id = R.drawable.brand_logo),
        contentDescription = if (isInstallation) "Reflection Media Solar Logo" else "घरकुल सोलर सर्व्हे लोगो",
        modifier = Modifier.size(104.dp),
        contentScale = ContentScale.Fit,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (isInstallation) "Reflection Media Solar" else "घरकुल सोलर सर्व्हे",
        style = MaterialTheme.typography.headlineMedium,
        color = BrandNavy,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = if (isInstallation) "Solar Installation Login" else "एक्झिक्युटिव्ह लॉगिन",
        style = MaterialTheme.typography.bodyLarge,
        color = TextMuted,
      )

      Spacer(modifier = Modifier.height(18.dp))

      // 2-Login Role Switcher Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, BorderSubtle),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LoginRoleButton(
            title = "1. Executive Login",
            subtitle = "घरकुल सर्व्हे",
            icon = Icons.Outlined.Assignment,
            selected = selectedRole == LoginRole.SURVEY_EXECUTIVE,
            onClick = { onSelectRole(LoginRole.SURVEY_EXECUTIVE) },
            modifier = Modifier.weight(1f).testTag("role_executive_button"),
          )
          LoginRoleButton(
            title = "2. Solar Installation",
            subtitle = "Completion Form",
            icon = Icons.Outlined.SolarPower,
            selected = selectedRole == LoginRole.SOLAR_INSTALLATION,
            onClick = { onSelectRole(LoginRole.SOLAR_INSTALLATION) },
            modifier = Modifier.weight(1f).testTag("role_installation_button"),
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = if (isInstallation) "Email Address" else "ईमेल",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
        OutlinedTextField(
          value = email,
          onValueChange = onEmailChange,
          modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
          placeholder = {
            Text(
              text = if (isInstallation) "installer@reflectionmedias.in" else "नाव@ईमेल.com",
              color = TextMuted,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardWhite,
              unfocusedContainerColor = CardWhite,
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = if (isInstallation) "Password" else "पासवर्ड",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
        OutlinedTextField(
          value = password,
          onValueChange = onPasswordChange,
          modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
          placeholder = {
            Text(if (isInstallation) "Enter password" else "पासवर्ड टाका", color = TextMuted)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription =
                  if (isInstallation) {
                    if (passwordVisible) "Hide password" else "Show password"
                  } else {
                    if (passwordVisible) "पासवर्ड लपवा" else "पासवर्ड पहा"
                  },
                tint = TextMuted,
              )
            }
          },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardWhite,
              unfocusedContainerColor = CardWhite,
              focusedBorderColor = BrandNavy,
              unfocusedBorderColor = BorderSubtle,
            ),
        )
      }

      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("login_error_banner"),
          shape = RoundedCornerShape(12.dp),
          color = PendingRoseBg,
          border = BorderStroke(1.dp, BrandCrimson.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = PendingRoseText,
              modifier = Modifier.size(20.dp).padding(top = 1.dp),
            )
            Text(
              text = errorMessage,
              color = PendingRoseText,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = onLoginClick,
        enabled = !isAuthenticating,
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("login_submit_button"),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = if (isInstallation) BrandCrimson else BrandNavy,
            contentColor = Color.White,
          ),
      ) {
        if (isAuthenticating) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
          )
        } else {
          Text(
            text = if (isInstallation) "Sign In as Solar Installer" else "लॉगिन करा",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (isInstallation) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = BannerIceBlue,
          border = BorderStroke(1.dp, BorderSubtle),
        ) {
          Text(
            text = "For approved Category 1 solar installations (1 KW ONGRID) & Completion Certificate reports.",
            style = MaterialTheme.typography.bodySmall,
            color = BrandNavy,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text =
          if (isCloudConfigured) "Supabase Auth + PostgreSQL + Cloudflare R2 Connected"
          else "Offline Demo Mode (Add SUPABASE_URL & SUPABASE_ANON_KEY in Secrets to sync)",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun LoginRoleButton(
  title: String,
  subtitle: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(64.dp),
    shape = RoundedCornerShape(12.dp),
    color = if (selected) BrandNavy else BannerIceBlue.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, if (selected) BrandNavy else Color.Transparent),
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (selected) Color.White else BrandNavy,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) Color.White else BrandNavy,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) Color.White.copy(alpha = 0.85f) else TextMuted,
        maxLines = 1,
      )
    }
  }
}
