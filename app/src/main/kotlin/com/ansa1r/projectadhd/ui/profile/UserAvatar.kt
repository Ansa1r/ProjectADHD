package com.ansa1r.projectadhd.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.theme.BrandColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UserAvatar(path: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { runCatching { path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }.getOrNull() }
    }
    Surface(modifier, shape = CircleShape, border = BorderStroke(2.dp, BrandColors.PurpleOutline),
        color = BrandColors.PurpleSurface.copy(alpha = com.ansa1r.projectadhd.ui.theme.BrandOpacity.Ordinary)) {
        bitmap?.let { Image(it, contentDescription = "Аватар пользователя", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: Icon(painterResource(R.drawable.ic_nav_profile), contentDescription = "Аватар пользователя", tint = BrandColors.Text, modifier = Modifier.padding(12.dp))
    }
}
