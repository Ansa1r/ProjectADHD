package com.ansa1r.projectadhd.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.components.ScreenList
import com.ansa1r.projectadhd.ui.components.SectionCard
import com.ansa1r.projectadhd.ui.mascot.MascotView

@Composable
fun ProfileScreen() {
    ScreenList {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(MascotMood.IDLE, Modifier.size(192.dp))
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.profile_placeholder))
            }
        }
    }
}
