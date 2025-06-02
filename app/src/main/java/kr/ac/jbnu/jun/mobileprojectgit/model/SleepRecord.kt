package kr.ac.jbnu.jun.mobileprojectgit.model

data class SleepRecord(
    val nickname: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val duration: Long = 0L,
    val deepRatio: String = ""
)