package kr.ac.jbnu.jun.mobileprojectgit.model

data class AsleepReport(
    val stat: Stat = Stat(),
    val session: Session = Session(),
    val stageList: List<SleepStage> = emptyList()
)

data class Stat(
    val sleepEfficiency: Float = 0f,
    val duration: Float = 0f,
    val sleepIndex: Float = 0f,
    val sleepLatency: Float = 0f,
    val deepSleep: Float = 0f,
    val lightSleep: Float = 0f,
    val remSleep: Float = 0f,
    val wakeTime: Float = 0f,
    val wasoCount: Int = 0,
    val wasoLongest: Float = 0f,
    val sleepCycles: Int = 0
)

data class Session(
    val startTime: String = "",
    val endTime: String = ""
)

data class SleepStage(
    val stage: String = "", // "Wake", "REM", "Light", "Deep"
    val time: String = ""   // 시간(또는 경과분)
)