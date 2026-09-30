package com.ansa1r.projectadhd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ansa1r.projectadhd.navigation.AppNavigation
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ProjectADHDApplication).container
        setContent { ProjectADHDTheme { AppNavigation(container) } }
    }
}
