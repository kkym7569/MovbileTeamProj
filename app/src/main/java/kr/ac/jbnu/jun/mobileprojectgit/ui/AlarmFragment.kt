package kr.ac.jbnu.jun.mobileprojectgit.ui

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

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_alarm, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnSuggested = view.findViewById(R.id.btn_suggested_wake_time)
        btnSetWake = view.findViewById(R.id.btn_set_wake_time)
        tvSleepStartRecommend = view.findViewById(R.id.tv_sleep_start_recommend)

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

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("추천 기상 시간")
                .setItems(items) { _, idx ->
                    val selected: LocalTime = recTimes[idx]
                    btnSuggested.text = selected.format(timeFmt)
                    scheduleAlarm(selected.hour, selected.minute)
                }
                .create()
            dialog.show()
        }

        btnSetWake.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(),
                { _, h, m ->
                    val strTime = String.format("%02d:%02d", h, m)
                    btnSetWake.text = strTime
                    scheduleAlarm(h, m)

                    val recStarts = SleepCycleUtil.getRecommendedSleepTimes(h, m)
                    val recStrings = recStarts.joinToString("  |  ") { it.format(timeFmt) }
                    tvSleepStartRecommend.text = "추천 수면 시작: $recStrings"
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun scheduleAlarm(hour: Int, minute: Int) {
        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ✅ Android 12 이상 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
            Toast.makeText(context, "정확한 알람 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
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

        Toast.makeText(
            context,
            "알람이 ${String.format("%02d:%02d", hour, minute)}에 설정되었습니다.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
