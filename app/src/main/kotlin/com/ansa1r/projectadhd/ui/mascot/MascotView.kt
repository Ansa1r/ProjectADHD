package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun MascotView(mood: MascotMood, modifier: Modifier = Modifier) {
    val asset = when (mood) {
        MascotMood.IDLE -> R.drawable.mascot_idle
        MascotMood.BLOCKING -> R.drawable.mascot_blocking
        MascotMood.PRAISE -> R.drawable.mascot_praise
    }
    val description = when (mood) {
        MascotMood.IDLE -> R.string.mascot_idle_description
        MascotMood.BLOCKING -> R.string.mascot_blocking_description
        MascotMood.PRAISE -> R.string.mascot_praise_description
    }
    Image(painterResource(asset), stringResource(description),
        modifier, contentScale = ContentScale.Fit)
}

@Composable
fun MascotBackdrop(modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(BrandColors.Background)) {
        if (enabled) {
            Image(painterResource(R.drawable.background_main), contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(BrandColors.Background.copy(alpha = 0.3f)))
        }
        content()
    }
}
