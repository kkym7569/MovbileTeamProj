package kr.ac.jbnu.jun.mobileprojectgit.model

import com.google.gson.annotations.SerializedName

data class SleepEfficiencyStat(
    @SerializedName("sleep_efficiency")
    val sleepEfficiency: Float? // ← nullable로 바꿔줌
)