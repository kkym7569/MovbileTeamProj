package kr.ac.jbnu.jun.mobileprojectgit.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kr.ac.jbnu.jun.mobileprojectgit.ui.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ✅ 절전모드에서도 깨우기
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SleepWell::AlarmReceiverWakeLock"
        )
        wl.acquire(3000)

        // ✅ 알람 울리는 액티비티 띄우기
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(ringIntent)
    }
}
