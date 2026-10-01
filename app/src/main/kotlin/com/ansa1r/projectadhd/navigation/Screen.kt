package com.ansa1r.projectadhd.navigation

import com.ansa1r.projectadhd.R

enum class Screen(val route: String, val title: Int, val icon: Int) {
    HOME("home", R.string.home, R.drawable.ic_nav_home),
    HABITS("habits", R.string.habits, R.drawable.ic_nav_habits),
    APPS("apps", R.string.apps, R.drawable.ic_nav_apps),
    STATS("stats", R.string.stats, R.drawable.ic_nav_stats),
    MASCOT("mascot", R.string.mascot, R.drawable.ic_nav_mascot)
}

object SettingsRoutes {
    const val ROOT = "settings"
    const val PERMISSIONS = "settings/permissions"
    const val BLOCKING = "settings/blocking"
    const val THEME = "settings/theme"
    const val PRIVACY = "settings/privacy"
    const val DEVELOPER = "settings/developer"
    const val DEBUG = "debug"
    fun isSettings(route: String) = route == ROOT || route.startsWith("$ROOT/")
}

object AppsRoutes {
    const val ROOT = "apps_setup"
    const val SELECTION = "apps"
    const val LIMITS = "apps/limits"
}
object ProfileRoutes {
    const val ROOT = "profile"
    const val EDIT = "profile/edit"
    const val PROGRESS = "profile/progress"
    const val ACHIEVEMENTS = "profile/achievements"
}

object HabitRoutes {
    const val CREATE = "habits/create"
    const val EDIT = "habits/edit/{id}"
}
object MascotRoutes {
    const val CUSTOMIZE = "mascot/customize"
    const val HISTORY = "mascot/history"
}
