package com.ansa1r.projectadhd.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun InstalledAppIcon(packageName: String, available: Boolean, revision: Int, loader: AppIconLoader) {
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(null, packageName, available, revision, loader, sizePx) {
        value = null
        if (available) value = loader.load(packageName, sizePx, revision)?.asImageBitmap()
    }
    Surface(Modifier.size(52.dp), shape = RoundedCornerShape(14.dp), color = BrandColors.PurpleDeep.copy(alpha = 0.85f), border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.PurpleOutline)) {
        bitmap?.let {
            Image(it, contentDescription = null, modifier = Modifier.padding(4.dp), contentScale = ContentScale.Fit)
        } ?: Icon(painterResource(R.drawable.ic_nav_apps), contentDescription = null,
            modifier = Modifier.padding(12.dp), tint = BrandColors.Muted)
    }
}
