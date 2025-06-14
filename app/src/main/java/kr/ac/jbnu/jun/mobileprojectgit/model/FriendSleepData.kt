package kr.ac.jbnu.jun.mobileprojectgit.model

data class FriendSleepData(
    val name: String,
    var sleepStart: String?,   // ← var 로 수정
    var sleepEnd: String?,     // ← var 로 수정
    var condition: String,     // ← var 로 수정
    var hasSleepData: Boolean  // ← var 로 수정
)