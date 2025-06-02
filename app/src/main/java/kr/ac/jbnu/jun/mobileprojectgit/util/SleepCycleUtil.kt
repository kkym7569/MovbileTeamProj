package kr.ac.jbnu.jun.mobileprojectgit.util

import java.time.LocalTime

object SleepCycleUtil {
    private const val CYCLE_MINUTES = 90
    private const val FALL_ASLEEP_MINUTES = 14

    /**
     * 수면 주기 기반 추천 기상 시간 5개 반환 (2~6 사이클)
     * 예: wakeUpHour=7, wakeUpMinute=0 이면
     *   5:16, 3:46, 2:16, 0:46, 23:16(전날) 식으로 리스트 반환
     */
    fun getRecommendedSleepTimes(wakeUpHour: Int, wakeUpMinute: Int): List<LocalTime> {
        // 잠드는 시간 보정
        val baseTime = LocalTime.of(wakeUpHour, wakeUpMinute)
            .minusMinutes(FALL_ASLEEP_MINUTES.toLong())

        // 2~6 사이클(90분씩) 뒤소리면서 5개 추천
        return (2..6).map { cycle ->
            baseTime.minusMinutes((cycle * CYCLE_MINUTES).toLong())
        }
    }
}
