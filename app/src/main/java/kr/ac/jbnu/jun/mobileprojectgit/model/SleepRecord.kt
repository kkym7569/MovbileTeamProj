package kr.ac.jbnu.jun.mobileprojectgit.model

data class SleepRecord(
    val nickname: String = "",
    val userId: String = "",
    val gender: Long = 0L,
    val age: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val duration: Double = 0.0,
    val deepRatio: String = ""
)