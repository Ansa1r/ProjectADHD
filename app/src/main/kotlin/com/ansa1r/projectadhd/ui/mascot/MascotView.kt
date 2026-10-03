package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.theme.BrandColors
import com.ansa1r.projectadhd.ui.mascot.animation.BobAnimationState
import com.ansa1r.projectadhd.ui.mascot.animation.BobMascot

@Composable
fun MascotView(mood: MascotMood, modifier: Modifier = Modifier, isTalking: Boolean = false) {
    BobMascot(modifier = modifier, isTalking = isTalking, state = when (mood) {
        MascotMood.IDLE -> BobAnimationState.IDLE
        MascotMood.BLOCKING -> BobAnimationState.BLOCKING
        MascotMood.PRAISE -> BobAnimationState.HAPPY
    })
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
