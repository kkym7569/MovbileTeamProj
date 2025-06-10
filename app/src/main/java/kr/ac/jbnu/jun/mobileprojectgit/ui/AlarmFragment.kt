package kr.ac.jbnu.jun.mobileprojectgit.ui

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.alarm.AlarmReceiver
import kr.ac.jbnu.jun.mobileprojectgit.util.SleepCycleUtil
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AlarmFragment : Fragment() {

    private lateinit var btnSuggested: Button
    private lateinit var btnSetWake: Button
    private lateinit var tvSleepStartRecommend: TextView

    private lateinit var btnInit: Button
    private lateinit var btnBegin: Button
    private lateinit var btnEnd: Button
    private lateinit var btnShareList: Button

    private var createdUserId: String? = null
    private var createdAsleepConfig: AsleepConfig? = null
    private var createdSessionId: String? = null

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_alarm, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnSuggested = view.findViewById(R.id.btn_suggested_wake_time)
        btnSetWake = view.findViewById(R.id.btn_set_wake_time)
        tvSleepStartRecommend = view.findViewById(R.id.tv_sleep_start_recommend)

        btnInit = view.findViewById(R.id.btn_init)
        btnBegin = view.findViewById(R.id.btn_begin)
        btnEnd = view.findViewById(R.id.btn_end)
        btnShareList = view.findViewById(R.id.btnShareList)

        btnSuggested.text = "--:--"
        btnSetWake.text = "--:--"
        tvSleepStartRecommend.text = ""

        btnSuggested.setOnClickListener {
            val now = Calendar.getInstance()
            val recTimes = SleepCycleUtil.getRecommendedSleepTimes(
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)
            )
            val items = recTimes.map { it.format(timeFmt) }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("추천 기상 시간")
                .setItems(items) { _, idx ->
                    val selected = recTimes[idx]
                    btnSuggested.text = selected.format(timeFmt)
                    scheduleAlarm(selected.hour, selected.minute)

                    //알람 설정 구간 기존 비긴 함수 기능 넣음
                    beginFun()
                }
                .setOnCancelListener {
                    showToast("추천 시간 선택이 취소되었습니다")
                }.show()
        }

        btnSetWake.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                val strTime = String.format("%02d:%02d", h, m)
                btnSetWake.text = strTime
                scheduleAlarm(h, m)

                val recStarts = SleepCycleUtil.getRecommendedSleepTimes(h, m)
                val recStrings = recStarts.joinToString("  |  ") { it.format(timeFmt) }
                tvSleepStartRecommend.text = "추천 수면 시작: $recStrings"

                // 알람 설정 구간 기존 비긴 기능 넣음
                beginFun()
            },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnInit.setOnClickListener {
            Asleep.initAsleepConfig(
                context = requireContext(),
                apiKey = "zhfpK9DcIzv0YQlUHE6swAb1Zs92jaiDanBLoaeX",
                userId = null,
                service = "Test App",
                asleepConfigListener = object : Asleep.AsleepConfigListener {
                    override fun onFail(errorCode: Int, detail: String) {
                        showToast("init 실패: $detail")
                    }

                    override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                        showToast("init 성공")
                        createdUserId = userId
                        createdAsleepConfig = asleepConfig
                    }
                })
        }


        /*비긴 + 엔드 기존 함수
        btnBegin.setOnClickListener {
            createdAsleepConfig?.let { config ->
                Asleep.beginSleepTracking(
                    asleepConfig = config,
                    asleepTrackingListener = object : Asleep.AsleepTrackingListener {
                        override fun onFail(errorCode: Int, detail: String) {
                            showToast("begin 실패: $detail")
                        }

                        override fun onStart(sessionId: String) {
                            createdSessionId = sessionId
                            showToast("tracking 시작됨")
                        }

                        override fun onFinish(sessionId: String?) {
                            sessionId?.let {
                                createdSessionId = it
                                fetchReport(it)
                            }
                        }

                        override fun onPerform(sequence: Int) {}
                    })
            } ?: showToast("먼저 init 해주세요")
        }
 */
        btnEnd.setOnClickListener {
            EndFun()
        }


        btnShareList.setOnClickListener {
            startActivity(Intent(requireContext(), kr.ac.jbnu.jun.mobileprojectgit.SleepShareActivity::class.java))
        }
    }

    //기존 비긴 버튼 눌렀을 때 기능 함수화
    fun beginFun()
    {
        createdAsleepConfig?.let { config ->
            Asleep.beginSleepTracking(
                asleepConfig = config,
                asleepTrackingListener = object : Asleep.AsleepTrackingListener {
                    override fun onFail(errorCode: Int, detail: String) {
                        showToast("begin 실패: $detail")
                    }

                    override fun onStart(sessionId: String) {
                        createdSessionId = sessionId
                        showToast("tracking 시작됨")
                    }

                    override fun onFinish(sessionId: String?) {
                        sessionId?.let {
                            createdSessionId = it
                            fetchReport(it)
                        }
                    }

                    override fun onPerform(sequence: Int) {}
                })
        } ?: showToast("먼저 init 해주세요")
    }

    //기존 엔드 버튼 눌렀을 때 기능 함수화
    fun EndFun()
    {
        Asleep.endSleepTracking()
        showToast("tracking 종료")
    }

    private fun fetchReport(sessionId: String) {
        Asleep.createReports(createdAsleepConfig)?.getReport(
            sessionId = sessionId,
            reportListener = object : Reports.ReportListener {
                override fun onFail(errorCode: Int, detail: String) {
                    showToast("report 실패: $detail")
                }

                override fun onSuccess(report: Report?) {
                    showToast("report 성공")
                    saveSleepRecordToFirestore(report)
                }
            }
        )
    }

//파이어 스토어에 저장하는 함수
    private fun saveSleepRecordToFirestore(report: Report?) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val nickname = doc.getString("nickname") ?: "익명"
            val sleepData = hashMapOf(
                "nickname" to nickname,
                "startTime" to (report?.session?.startTime?.toString() ?: ""),
                "endTime" to (report?.session?.endTime?.toString() ?: ""),
                "duration" to 0
            )
            db.collection("users").document(uid).collection("sleeps")
                .add(sleepData)
                .addOnSuccessListener { showToast("기록 저장됨") }
                .addOnFailureListener { showToast("기록 실패") }
        }
    }

    private fun scheduleAlarm(hour: Int, minute: Int) {
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
            showToast("정확한 알람 권한을 허용해주세요.")
            return
        }

        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            requireContext(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)

        showToast("알람이 ${String.format("%02d:%02d", hour, minute)}에 설정되었습니다.")
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

}
