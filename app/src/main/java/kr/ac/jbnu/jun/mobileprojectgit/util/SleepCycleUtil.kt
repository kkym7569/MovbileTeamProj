package kr.ac.jbnu.jun.mobileprojectgit.util

import java.time.LocalTime

object SleepCycleUtil {
    fun getRecommendedSleepTimes(hour: Int, minute: Int): List<LocalTime> {
        val wakeTime = LocalTime.of(hour, minute)
        val fallAsleepTimeBuffer = 15L // 잠드는 시간 고려

        return (1..6).map { cycle ->
            wakeTime.minusMinutes(cycle * 90L + fallAsleepTimeBuffer)
        }.reversed()
    }
}
