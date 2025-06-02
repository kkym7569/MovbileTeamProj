package kr.ac.jbnu.jun.mobileprojectgit.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.FriendSleepData

class FriendSleepAdapter(private val items: List<FriendSleepData>) :
    RecyclerView.Adapter<FriendSleepAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvSleepTime: TextView = view.findViewById(R.id.tv_sleep_time)
        val tvCondition: TextView = view.findViewById(R.id.tv_condition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_sleep, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvSleepTime.text = "${item.sleepStart} ~ ${item.sleepEnd}"
        holder.tvCondition.text = item.condition
    }

    override fun getItemCount(): Int = items.size
}
