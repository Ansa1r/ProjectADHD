package com.ansa1r.projectadhd.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.theme.BrandColors
import kotlinx.coroutines.delay

/** One in-Activity sequence: 420 + 300 + 300 + 420 + 400 = 1840 ms. */
@Composable
fun StartupHost(entryId: Long, onVisibilityChanged: (Boolean) -> Unit = {}, content: @Composable () -> Unit) {
    var visible by remember(entryId) { mutableStateOf(entryId > 0) }
    val showing = visible && entryId > 0
    var showTitle by remember(entryId) { mutableStateOf(false) }
    val rotation = remember(entryId) { Animatable(0f) }
    val coverAlpha = remember(entryId) { Animatable(if (entryId > 0) 1f else 0f) }
    val gradient = Brush.verticalGradient(0f to colorResource(R.color.startup_top),
        0.48f to colorResource(R.color.startup_green), 1f to colorResource(R.color.startup_blue))
    val visibilityChanged by rememberUpdatedState(onVisibilityChanged)
    LaunchedEffect(showing, entryId) {
        visibilityChanged(showing)
        if (!showing) return@LaunchedEffect
        delay(420)
        rotation.animateTo(90f, tween(300, easing = FastOutSlowInEasing))
        showTitle = true
        rotation.snapTo(-90f)
        rotation.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        delay(420)
        coverAlpha.animateTo(0f, tween(400))
        visible = false
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (showing) 1f - coverAlpha.value else 1f }
            .then(if (showing) Modifier.clearAndSetSemantics { } else Modifier)) { content() }
        if (showing) Box(Modifier.fillMaxSize().testTag("startup_cover").graphicsLayer { alpha = coverAlpha.value }
            .background(gradient).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(24.dp).graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }, contentAlignment = Alignment.Center) {
                if (showTitle) Text(stringResource(R.string.app_name), color = BrandColors.PurpleDeep,
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.testTag("startup_title"))
                else Image(painterResource(R.drawable.mascot_startup), stringResource(R.string.startup_mascot_description),
                    modifier = Modifier.size(256.dp).testTag("startup_mascot"), contentScale = ContentScale.Fit)
            }
        }
    }
}
