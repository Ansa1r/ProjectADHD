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
        MascotMood.IDLE, MascotMood.PRAISE -> R.drawable.mascot_idle
        MascotMood.BLOCKING -> R.drawable.mascot_blocking
    }
    Image(painterResource(asset), stringResource(if (mood == MascotMood.BLOCKING)
        R.string.mascot_blocking_description else R.string.mascot_idle_description),
        modifier, contentScale = ContentScale.Fit)
}

@Composable
fun MascotBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(BrandColors.Background)) {
        Image(painterResource(R.drawable.background_main), contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        content()
    }
}
