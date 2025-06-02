package kr.ac.jbnu.jun.mobileprojectgit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepRecord

class SleepRecordAdapter(private val recordList: List<SleepRecord>)
    : RecyclerView.Adapter<SleepRecordAdapter.ViewHolder>() {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val tvNickname: TextView = view.findViewById(R.id.tvNickname)
        val tvSleepTime: TextView = view.findViewById(R.id.tvSleepTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sleep_record, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = recordList[position]
        holder.tvNickname.text = record.nickname
        holder.tvSleepTime.text = "${record.startTime} ~ ${record.endTime}"
    }

    override fun getItemCount() = recordList.size
}