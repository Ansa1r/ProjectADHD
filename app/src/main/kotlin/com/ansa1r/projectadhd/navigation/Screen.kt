package com.ansa1r.projectadhd.navigation

import com.ansa1r.projectadhd.R

enum class Screen(val route: String, val title: Int, val icon: Int) {
    HOME("home", R.string.home, R.drawable.ic_nav_home),
    HABITS("habits", R.string.habits, R.drawable.ic_nav_habits),
    APPS("apps", R.string.apps, R.drawable.ic_nav_apps),
    STATS("stats", R.string.stats, R.drawable.ic_nav_stats),
    SETTINGS("settings", R.string.settings, R.drawable.ic_nav_settings)
}
