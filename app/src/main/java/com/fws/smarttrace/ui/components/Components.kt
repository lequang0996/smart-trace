package com.fws.smarttrace.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.smarttrace.R
import com.fws.smarttrace.model.Severity
import com.fws.smarttrace.model.Verdict
import com.fws.smarttrace.ui.theme.AppTheme
import com.fws.smarttrace.ui.theme.Navy

@Composable
fun Ico(@DrawableRes id: Int, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, size: Dp = 24.dp) {
    Icon(painterResource(id), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

/** Full-screen container that respects system bars (edge-to-edge). */
@Composable
fun ScreenSurface(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
    }
}

@Composable
fun SmartCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    var m = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, AppTheme.colors.outline, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(contentPadding), content = content)
}

@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = if (enabled) Modifier.background(AppTheme.colors.goldBrush) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .then(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Ico(icon, tint = if (enabled) Navy else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text, color = if (enabled) Navy else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val c = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .border(BorderStroke(1.2.dp, AppTheme.colors.gold), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Ico(icon, tint = AppTheme.colors.gold)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = c, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun SelectChip(text: String, selected: Boolean, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) c.gold.copy(alpha = 0.16f) else c.chipBg)
            .border(1.dp, if (selected) c.gold else c.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Ico(icon, tint = if (selected) c.gold else MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Ico(R.drawable.ic_back, tint = MaterialTheme.colorScheme.onBackground)
            }
        } else Spacer(Modifier.width(8.dp))
        Text(
            title, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1,
        )
        actions()
    }
}

@Composable
fun IconAction(@DrawableRes icon: Int, tint: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ico(icon, tint = tint)
    }
}

@Composable
fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.CONSISTENT -> AppTheme.colors.success
    Verdict.CHECK -> AppTheme.colors.warn
    Verdict.SUSPICIOUS -> AppTheme.colors.bad
    Verdict.INSUFFICIENT -> AppTheme.colors.info
}

@Composable
fun severityColor(s: Severity): Color = when (s) {
    Severity.OK -> AppTheme.colors.success
    Severity.INFO -> AppTheme.colors.info
    Severity.WARN -> AppTheme.colors.warn
    Severity.BAD -> AppTheme.colors.bad
}

@DrawableRes
fun severityIcon(s: Severity): Int = when (s) {
    Severity.OK -> R.drawable.ic_check
    Severity.INFO -> R.drawable.ic_info
    Severity.WARN -> R.drawable.ic_alert
    Severity.BAD -> R.drawable.ic_cross
}

@Composable
fun VerdictChip(v: Verdict, label: String) {
    val c = verdictColor(v)
    Text(
        label, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun ScoreRing(score: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 92.dp) {
    val track = AppTheme.colors.outline
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(track, -90f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            drawArc(color, -90f, 360f * score.coerceIn(0, 100) / 100f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
        }
        Text(
            score.toString(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(118.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
