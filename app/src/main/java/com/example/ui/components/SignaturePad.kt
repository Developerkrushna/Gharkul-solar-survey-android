package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.CardWhite
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SignaturePadCard(
  title: String,
  strokes: List<List<Pair<Float, Float>>>,
  onStrokesChange: (List<List<Pair<Float, Float>>>) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "signature_pad",
  clearLabel: String = "पुसा",
  placeholderText: String = "येथे बोटाने सही करा",
) {
  val currentStroke = remember { mutableStateListOf<Pair<Float, Float>>() }
  var dragVersion by remember { mutableStateOf(0) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      if (strokes.isNotEmpty() || currentStroke.isNotEmpty()) {
        TextButton(
          onClick = {
            currentStroke.clear()
            onStrokesChange(emptyList())
          },
          modifier = Modifier.testTag("${testTag}_clear"),
        ) {
          Text(clearLabel, color = BrandCrimson, fontWeight = FontWeight.Bold)
        }
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth().height(120.dp).testTag(testTag),
      shape = RoundedCornerShape(12.dp),
      color = SurfaceCanvas,
      border = BorderStroke(1.dp, BorderSubtle),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (strokes.isEmpty() && currentStroke.isEmpty()) {
          Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.align(Alignment.Center),
          )
        }
        Canvas(
          modifier =
            Modifier.fillMaxSize()
              .clip(RoundedCornerShape(12.dp))
              .background(CardWhite.copy(alpha = 0.4f))
              .pointerInput(strokes.size) {
                detectDragGestures(
                  onDragStart = { offset ->
                    currentStroke.clear()
                    currentStroke.add(offset.x to offset.y)
                    dragVersion++
                  },
                  onDrag = { change, _ ->
                    change.consume()
                    currentStroke.add(change.position.x to change.position.y)
                    dragVersion++
                  },
                  onDragEnd = {
                    if (currentStroke.isNotEmpty()) {
                      onStrokesChange(strokes + listOf(currentStroke.toList()))
                      currentStroke.clear()
                    }
                  },
                  onDragCancel = {
                    currentStroke.clear()
                  },
                )
              }
        ) {
          // Read dragVersion so active stroke redraws smoothly
          @Suppress("UNUSED_VARIABLE")
          val tick = dragVersion
          val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke.toList()) else strokes
          allStrokes.forEach { pts ->
            if (pts.size == 1) {
              drawCircle(
                color = BrandNavy,
                radius = 3f,
                center = Offset(pts[0].first, pts[0].second),
              )
            } else if (pts.size > 1) {
              val path =
                Path().apply {
                  moveTo(pts[0].first, pts[0].second)
                  for (i in 1 until pts.size) {
                    lineTo(pts[i].first, pts[i].second)
                  }
                }
              drawPath(
                path = path,
                color = BrandNavy,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
            }
          }
        }
      }
    }
  }
}
