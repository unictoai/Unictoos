package com.unictoai.unictoos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.unictoai.unictoos.R
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale

private data class OnboardingPage(val image: Int, val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        R.drawable.onboarding_stream_anywhere,
        "Stream anywhere",
        "Broadcast from your phone to YouTube, Twitch, Kick or your own RTMP server.",
    ),
    OnboardingPage(
        R.drawable.onboarding_scenes,
        "Scenes that switch fast",
        "Build layouts with camera, screen, text and overlays — switch live with one tap.",
    ),
    OnboardingPage(
        R.drawable.onboarding_reliable_capture,
        "Reliable capture",
        "Adaptive bitrate, reconnect handling and health monitoring keep you on air.",
    ),
    OnboardingPage(
        R.drawable.onboarding_secure_control,
        "Your keys stay yours",
        "Stream keys are encrypted on-device and never leave your phone.",
    ),
)

@Composable
internal fun OnboardingScreen(onFinished: () -> Unit) {
    val c = StudioColorsScheme
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = onboardingPages[pageIndex]
    val isLastPage = pageIndex == onboardingPages.lastIndex

    Box(Modifier.fillMaxSize().background(c.background)) {
        AnimatedContent(
            targetState = page.image,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding image",
        ) { image ->
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        c.background.copy(alpha = 0.82f),
                        Color.Transparent,
                        c.background.copy(alpha = 0.96f),
                    ),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(5.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_unictoos),
                        contentDescription = "Unictoos logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("UNICTOOS", style = StudioTypeScale.eyebrowLarge, color = c.textPrimary)
                    Text("Mobile broadcast studio", style = StudioTypeScale.caption, color = c.textSecondary)
                }
                TextButton(onClick = onFinished, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Skip", style = StudioTypeScale.bodyStrong, color = c.textSecondary)
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding text",
            ) { index ->
                val p = onboardingPages[index]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "STEP ${index + 1} OF ${onboardingPages.size}",
                        style = StudioTypeScale.eyebrowLarge,
                        color = c.cyan,
                    )
                    Text(p.title, style = StudioTypeScale.display, color = c.textPrimary)
                    Text(p.body, style = StudioTypeScale.body, color = c.textSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                onboardingPages.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == pageIndex) 22.dp else 7.dp, 7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == pageIndex) c.cyan else c.textTertiary.copy(alpha = 0.45f)),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pageIndex > 0) {
                    IconButton(onClick = { pageIndex -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = c.textPrimary)
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                StudioButton(
                    text = if (isLastPage) "Get started" else "Next",
                    onClick = { if (isLastPage) onFinished() else pageIndex += 1 },
                    style = StudioButtonStyle.Primary,
                    icon = if (isLastPage) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        }
    }
}
