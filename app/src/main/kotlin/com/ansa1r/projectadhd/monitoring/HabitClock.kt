package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

data class HabitClockSample(val wall: Long, val elapsed: Long, val boot: Int)
fun interface HabitClock { fun sample(): HabitClockSample }
class AndroidHabitClock(private val context: Context) : HabitClock {
    override fun sample() = HabitClockSample(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1))
}
