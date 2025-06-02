package kr.ac.jbnu.jun.mobileprojectgit.ui

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kr.ac.jbnu.jun.mobileprojectgit.R

class AlarmRingActivity : AppCompatActivity() {
    private lateinit var player: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 잠금 화면 위에 표시
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        setContentView(R.layout.activity_alarm_ring)

        // 화면 깨우기
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SleepWell::AlarmWakeLock"
        ).acquire(5000)

        // Notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chan = "alarm_chan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(chan, "Alarm", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        nm.notify(1001,
            NotificationCompat.Builder(this, chan)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("⏰ 알람")
                .setContentText("지금 일어날 시간입니다!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
        )

        // 진동
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(1000)

        // 무한 반복 사운드
        player = MediaPlayer.create(this, R.raw.alarm_sound)
        player.isLooping = true
        player.start()

        findViewById<Button>(R.id.btn_stop_alarm).setOnClickListener {
            player.stop()
            player.release()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized && player.isPlaying) {
            player.stop()
            player.release()
        }
    }
}
