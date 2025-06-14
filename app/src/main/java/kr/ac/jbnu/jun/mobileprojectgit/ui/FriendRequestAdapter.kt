package kr.ac.jbnu.jun.mobileprojectgit.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.User

class FriendRequestAdapter(
    private val requests: List<User>,
    private val onAccept: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRequester: TextView = view.findViewById(R.id.tv_requester)
        val btnAccept: TextView = view.findViewById(R.id.btn_accept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val requester = requests[position]
        holder.tvRequester.text = requester.nickname
        holder.btnAccept.setOnClickListener { onAccept(requester) }
    }

    override fun getItemCount(): Int = requests.size
}
