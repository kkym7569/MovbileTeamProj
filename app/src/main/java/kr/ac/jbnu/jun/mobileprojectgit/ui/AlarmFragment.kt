package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.alarm.AlarmReceiver
import kr.ac.jbnu.jun.mobileprojectgit.util.SleepCycleUtil
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AlarmFragment : Fragment() {
    private lateinit var btnSuggested: Button
    private lateinit var btnSetWake: Button
    private lateinit var tvSleepStartRecommend: TextView

    // 추천 시간과 수면 시작 시간 포맷 (HH:mm)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_alarm, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnSuggested = view.findViewById(R.id.btn_suggested_wake_time)
        btnSetWake   = view.findViewById(R.id.btn_set_wake_time)
        tvSleepStartRecommend = view.findViewById(R.id.tv_sleep_start_recommend)

        // 초기 텍스트
        btnSuggested.text = "--:--"
        btnSetWake.text = "--:--"
        tvSleepStartRecommend.text = ""

        // 1) “지금 자면 🌙” 버튼 클릭 → 추천 기상시간 다이얼로그
        btnSuggested.setOnClickListener {
            val now = Calendar.getInstance()
            val recTimes = SleepCycleUtil.getRecommendedSleepTimes(
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)
            )
            val items = recTimes.map { it.format(timeFmt) }.toTypedArray()

            // 기본 AlertDialog로 팝업 (배경색은 기본값)
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("추천 기상 시간")
                .setItems(items) { _, idx ->
                    val selected: LocalTime = recTimes[idx]
                    btnSuggested.text = selected.format(timeFmt)
                    // 선택한 추천 시각을 기준으로 알람 설정
                    scheduleAlarm(selected.hour, selected.minute)
                }
                .create()
            dialog.show()
        }

        // 2) “언제 일어나야 할까요?” 버튼 클릭 → TimePickerDialog
        btnSetWake.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(),
                { _, h, m ->
                    // 1) 버튼 텍스트 변경
                    val strTime = String.format("%02d:%02d", h, m)
                    btnSetWake.text = strTime

                    // 2) 알람 예약
                    scheduleAlarm(h, m)

                    // 3) 지금 시간과 설정한 시간의 차이를 기준으로
                    //    90분 주기 계산 → 추천 수면 시작 시간 리스트
                    val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val nowMin  = Calendar.getInstance().get(Calendar.MINUTE)
                    // 현재 시각 LocalTime 객체
                    val nowLT = LocalTime.of(nowHour, nowMin)
                    // 알람 시각 LocalTime 객체
                    val wakeLT = LocalTime.of(h, m)
                    // SleepCycleUtil에 있는 getRecommendedSleepTimes(wakeHour, wakeMin) 메서드 활용
                    // → 알람 시각 기준, 2~6 사이클 뒤로 계산된 5개 수면 시작 시간 반환
                    val recStarts = SleepCycleUtil.getRecommendedSleepTimes(h, m)
                    // 예를 들어, recStarts = [start2, start3, start4, start5, start6]
                    // 문자열로 연결
                    val recStrings = recStarts.joinToString(separator = "  |  ") {
                        it.format(timeFmt)
                    }
                    tvSleepStartRecommend.text = "추천 수면 시작: $recStrings"
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun scheduleAlarm(hour: Int, minute: Int) {
        // AlarmReceiver를 트리거할 PendingIntent 생성
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            requireContext(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // AlarmManager 인스턴스 가져오기
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Calendar 객체에 alarm 시각 설정
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Doze 모드에서도 정확히 울리도록 설정
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
        // 토스트로 알람 등록 완료 알림
        Toast.makeText(
            context,
            "알람이 ${String.format("%02d:%02d", hour, minute)}에 설정되었습니다.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
