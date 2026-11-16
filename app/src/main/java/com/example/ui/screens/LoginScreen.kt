package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun LoginScreen(
  email: String,
  password: String,
  errorMessage: String?,
  onEmailChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
) {
  var passwordVisible by remember { mutableStateOf(false) }
  var showForgotDialog by remember { mutableStateOf(false) }

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
          .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Image(
        painter = painterResource(id = R.drawable.brand_logo),
        contentDescription = "घरकुल सोलर सर्व्हे लोगो",
        modifier = Modifier.size(120.dp),
        contentScale = ContentScale.Fit,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "घरकुल सोलर सर्व्हे",
        style = MaterialTheme.typography.headlineMedium,
        color = BrandNavy,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "एक्झिक्युटिव्ह लॉगिन",
        style = MaterialTheme.typography.bodyLarge,
        color = TextMuted,
      )

      Spacer(modifier = Modifier.height(32.dp))

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "ईमेल",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
        OutlinedTextField(
          value = email,
          onValueChange = onEmailChange,
          modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
          placeholder = { Text("नाव@ईमेल.com", color = TextMuted) },
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

      Spacer(modifier = Modifier.height(18.dp))

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "पासवर्ड",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
        )
        OutlinedTextField(
          value = password,
          onValueChange = onPasswordChange,
          modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
          placeholder = { Text("पासवर्ड टाका", color = TextMuted) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (passwordVisible) "पासवर्ड लपवा" else "पासवर्ड पहा",
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

      Spacer(modifier = Modifier.height(10.dp))

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
          text = "पासवर्ड विसरलात?",
          style = MaterialTheme.typography.titleSmall,
          color = BrandNavy,
          modifier =
            Modifier.clickable { showForgotDialog = true }
              .padding(vertical = 8.dp, horizontal = 4.dp)
              .testTag("forgot_password_button"),
        )
      }

      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = errorMessage,
          color = BrandCrimson,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      Button(
        onClick = onLoginClick,
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("login_submit_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = Color.White),
      ) {
        Text(
          text = "लॉगिन करा",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }

  if (showForgotDialog) {
    AlertDialog(
      onDismissRequest = { showForgotDialog = false },
      title = { Text("पासवर्ड मदतीसाठी", color = BrandNavy, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "आपला नवीन पासवर्ड किंवा लॉगिन तपशील मिळवण्यासाठी कृपया मुख्य कार्यालय (अ‍ॅडमिन) यांच्याशी संपर्क साधा. डेमो लॉगिनसाठी पूर्वनिर्धारित ईमेल व पासवर्ड वापरू शकता.",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showForgotDialog = false }) {
          Text("ठीक आहे", color = BrandNavy, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}
