package com.fws.smarttrace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.smarttrace.R
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.ui.components.GoldButton
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.theme.Gold
import com.fws.smarttrace.ui.theme.GoldDeep
import com.fws.smarttrace.ui.theme.GoldLight
import com.fws.smarttrace.ui.theme.Ivory
import com.fws.smarttrace.ui.theme.Navy
import com.fws.smarttrace.ui.theme.NavyDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FLAGS = mapOf(
    "en" to "🇬🇧", "zh-CN" to "🇨🇳", "es" to "🇪🇸", "pt" to "🇧🇷",
    "ru" to "🇷🇺", "ja" to "🇯🇵", "ko" to "🇰🇷", "vi" to "🇻🇳",
)
private val ENGLISH_NAMES = mapOf(
    "en" to "English", "zh-CN" to "Chinese · Simplified", "es" to "Spanish", "pt" to "Portuguese",
    "ru" to "Russian", "ja" to "Japanese", "ko" to "Korean", "vi" to "Vietnamese",
)

/**
 * Shown once, before the user guide. Always dark navy + gold (independent of the theme) so the first impression is
 * the same everywhere. Picking a language applies it immediately, so the texts on this screen switch live.
 */
@Composable
fun LanguageWelcomeScreen(onPick: (String) -> Unit, onContinue: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(Languages.currentTag()) }
    // The language change re-creates the activity; remember that the intro already played so it does not replay.
    var introDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1100); introDone = true }

    val inf = rememberInfiniteTransition(label = "welcome")
    val pulse by inf.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Navy, Color(0xFF14325E))))
            .drawBehind {
                val gold = Offset(size.width * 0.88f, size.height * 0.07f)
                drawCircle(
                    Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f * pulse), Color.Transparent), center = gold, radius = size.width * 0.9f),
                    radius = size.width * 0.9f, center = gold,
                )
                val blue = Offset(size.width * 0.02f, size.height * 0.96f)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF3C6FC1).copy(alpha = 0.24f), Color.Transparent), center = blue, radius = size.width),
                    radius = size.width, center = blue,
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(30.dp))
                Emblem(pulse)
                Spacer(Modifier.height(18.dp))
                Text3(
                    "SmartTrace",
                    TextStyle(
                        brush = Brush.horizontalGradient(listOf(GoldLight, Gold, GoldLight)),
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = 1.2.sp,
                    ),
                )
                Text3(
                    stringResource(R.string.tagline),
                    TextStyle(color = Ivory.copy(alpha = 0.72f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp),
                    Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(26.dp))
                Ornament()
                Spacer(Modifier.height(22.dp))
                Text3(
                    stringResource(R.string.lang_welcome_title),
                    TextStyle(color = Ivory, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, textAlign = TextAlign.Center),
                )
                Text3(
                    stringResource(R.string.lang_welcome_sub),
                    TextStyle(color = Ivory.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp),
                    Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(22.dp))

                Languages.supported.chunked(2).forEachIndexed { row, tags ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        tags.forEachIndexed { col, tag ->
                            LanguageCard(
                                tag = tag, selected = selected == tag, playIntro = !introDone, index = row * 2 + col,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (selected != tag) {
                                    selected = tag
                                    onPick(tag)
                                }
                            }
                        }
                        if (tags.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
            Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 20.dp)) {
                GoldButton(stringResource(R.string.lang_continue), Modifier.fillMaxWidth(), onClick = onContinue)
            }
        }
    }
}

@Composable
private fun Text3(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text, modifier = modifier, style = style)
}

/** Logo badge: soft pulsing glow, slowly rotating gold ring, scan icon in the middle. */
@Composable
private fun Emblem(pulse: Float) {
    val inf = rememberInfiniteTransition(label = "emblem")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(10_000, easing = LinearEasing)), label = "spin")
    Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(124.dp).drawBehind {
                drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.38f * pulse), Color.Transparent), radius = size.minDimension / 2f))
            }
        )
        Canvas(Modifier.size(104.dp).graphicsLayer { rotationZ = spin }) {
            drawCircle(
                Brush.sweepGradient(listOf(Gold, Color.Transparent, GoldLight, Color.Transparent, Gold)),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        Box(
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1F4378), Navy)))
                .border(1.dp, Gold.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Ico(R.drawable.ic_scan_code, tint = Gold, size = 44.dp) }
    }
}

/** ——— ◆ ——— */
@Composable
private fun Ornament() {
    Row(Modifier.fillMaxWidth(0.7f), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.7f)))))
        Text3("◆", TextStyle(color = Gold, fontSize = 10.sp), Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.7f), Color.Transparent))))
    }
}

@Composable
private fun LanguageCard(
    tag: String, selected: Boolean, playIntro: Boolean, index: Int, modifier: Modifier, onClick: () -> Unit,
) {
    val alpha = remember { Animatable(if (playIntro) 0f else 1f) }
    val rise = remember { Animatable(if (playIntro) 28f else 0f) }
    LaunchedEffect(Unit) {
        if (playIntro) {
            delay(250L + index * 70L)
            launch { alpha.animateTo(1f, tween(450)) }
            rise.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        }
    }
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scale")
    val shape = RoundedCornerShape(22.dp)
    val fill = if (selected) {
        Brush.linearGradient(listOf(Gold.copy(alpha = 0.24f), Gold.copy(alpha = 0.07f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f)))
    }
    val edge: Brush = if (selected) Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep)) else SolidColor(Color.White.copy(alpha = 0.14f))

    Box(
        modifier
            .graphicsLayer { this.alpha = alpha.value; translationY = rise.value; scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(fill)
            .border(if (selected) 1.5.dp else 1.dp, edge, shape)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            Text3(FLAGS[tag].orEmpty(), TextStyle(fontSize = 30.sp))
            Spacer(Modifier.height(8.dp))
            Text3(
                Languages.nativeNames[tag].orEmpty(),
                TextStyle(color = if (selected) GoldLight else Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            )
            Text3(ENGLISH_NAMES[tag].orEmpty(), TextStyle(color = Ivory.copy(alpha = 0.5f), fontSize = 11.sp), Modifier.padding(top = 2.dp))
        }
        AnimatedVisibility(
            selected,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Brush.linearGradient(listOf(GoldLight, Gold))),
                contentAlignment = Alignment.Center,
            ) { Ico(R.drawable.ic_check, tint = Navy, size = 15.dp) }
        }
    }
}
