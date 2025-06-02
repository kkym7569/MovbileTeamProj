package kr.ac.jbnu.jun.mobileprojectgit.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.jbnu.jun.mobileprojectgit.ui.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val intentActivity = Intent(context, AlarmRingActivity::class.java)
        intentActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intentActivity)
    }
}
