package kr.ac.jbnu.jun.mobileprojectgit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepRecord

class ComparisonStatAdapter(
    private val items: List<SleepRecord>
) : RecyclerView.Adapter<ComparisonStatAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAge      = itemView.findViewById<TextView>(R.id.tvAge)
        private val tvGender   = itemView.findViewById<TextView>(R.id.tvGender)
        private val tvDuration = itemView.findViewById<TextView>(R.id.tvDuration)

        fun bind(record: SleepRecord) {
            tvAge.text      = record.age.toString()
            tvGender.text = when (record.gender) {
                0L -> itemView.context.getString(R.string.gender_male)
                1L -> itemView.context.getString(R.string.gender_female)
                else -> itemView.context.getString(R.string.gender_unknown)
            }
            tvDuration.text = record.duration.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comparison_stat, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }
}
