package com.unictoai.unictoos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unictoai.unictoos.R
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette

private data class OnboardingPage(
    val image: Int,
    val eyebrow: String,
    val title: String,
    val body: String,
)

private val onboardingPages = listOf(
    OnboardingPage(
        image = R.drawable.onboarding_stream_anywhere,
        eyebrow = "STREAM ANYWHERE",
        title = "Your broadcast desk, in your pocket.",
        body = "Go live from your phone with a focused workspace built for serious creators.",
    ),
    OnboardingPage(
        image = R.drawable.onboarding_scenes,
        eyebrow = "PROFESSIONAL SCENES",
        title = "Build a look people remember.",
        body = "Create clean portrait or landscape scenes for camera, screen capture, titles, and more.",
    ),
    OnboardingPage(
        image = R.drawable.onboarding_reliable_capture,
        eyebrow = "RELIABLE CAPTURE",
        title = "Stay in control when it matters.",
        body = "Preview, stream, record, mute, and mark moments from one calm mobile control surface.",
    ),
    OnboardingPage(
        image = R.drawable.onboarding_secure_control,
        eyebrow = "SECURE CREATOR CONTROL",
        title = "Your channels. Your keys. Your control.",
        body = "Connect YouTube, Twitch, Kick, or a custom RTMP destination while credentials stay protected on your device.",
    ),
)

@Composable
internal fun OnboardingScreen(onFinished: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = onboardingPages[pageIndex]
    val isLastPage = pageIndex == onboardingPages.lastIndex

    Box(Modifier.fillMaxSize().background(V02Palette.Neutral950)) {
        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        V02Palette.Neutral950.copy(alpha = 0.78f),
                        Color.Transparent,
                        V02Palette.Neutral950.copy(alpha = 0.98f),
                    ),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.xl)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White, shape = RoundedCornerShape(13.dp), modifier = Modifier.size(42.dp)) {
                    Image(
                        painter = painterResource(R.drawable.logo_unictoos),
                        contentDescription = "Unictoos logo",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("UNICTOOS", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Mobile broadcast studio", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onFinished, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Skip", color = Color.White.copy(alpha = 0.80f))
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                label = "onboardingPage",
            ) { index ->
                val animatedPage = onboardingPages[index]
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(animatedPage.eyebrow, color = V02Palette.AccentBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(animatedPage.title, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(animatedPage.body, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            LinearProgressIndicator(
                progress = { (pageIndex + 1) / onboardingPages.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                color = V02Palette.AccentBlue,
                trackColor = Color.White.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    onboardingPages.indices.forEach { index ->
                        Box(Modifier.size(if (index == pageIndex) 22.dp else 7.dp, 7.dp).clip(RoundedCornerShape(50)).background(if (index == pageIndex) V02Palette.AccentBlue else Color.White.copy(alpha = 0.30f)))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pageIndex > 0) {
                        IconButton(onClick = { pageIndex -= 1 }) { Icon(Icons.Default.ArrowBack, contentDescription = "Previous", tint = Color.White) }
                    }
                    Button(
                        onClick = { if (isLastPage) onFinished() else pageIndex += 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = V02Palette.AccentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (isLastPage) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text("Get started", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Next", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(Spacing.sm))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
