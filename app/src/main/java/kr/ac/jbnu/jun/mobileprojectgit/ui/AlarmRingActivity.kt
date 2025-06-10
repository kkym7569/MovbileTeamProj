package kr.ac.jbnu.jun.mobileprojectgit.ui

import ai.asleep.asleepsdk.Asleep
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kr.ac.jbnu.jun.mobileprojectgit.R

class AlarmRingActivity : AppCompatActivity() {
    private lateinit var player: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 화면 깨우기 및 잠금 해제
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm_ring)

        // WakeLock 확보
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SleepWell::AlarmWakeLock"
        ).acquire(5000)

        // 🔔 알림 생성
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chan = "alarm_chan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(chan, "Alarm", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        nm.notify(
            1001,
            NotificationCompat.Builder(this, chan)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("⏰ 알람")
                .setContentText("지금 일어날 시간입니다!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
        )

        // 🔊 사운드 및 진동
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(1000)

        player = MediaPlayer.create(this, R.raw.alarm_sound)
        player.isLooping = true
        player.start()

        findViewById<Button>(R.id.btn_stop_alarm).setOnClickListener {
            try {
                if (player.isPlaying) {
                    player.stop()
                    player.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            finish()
        }
    }

    private fun stopAlarmSound() {
        try {
            if (::player.isInitialized && player.isPlaying) {
                player.stop()
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun finish() {
        Asleep.endSleepTracking()
        Toast.makeText(this, "트래킹이 종료되었습니다", Toast.LENGTH_SHORT).show()
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::player.isInitialized && player.isPlaying) {
                player.stop()
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
