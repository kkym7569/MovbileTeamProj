package kr.ac.jbnu.jun.mobileprojectgit.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.User

class SearchUserAdapter(
    private val users: List<User>,
    private val onAddFriendClicked: (User) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNickname: TextView = view.findViewById(R.id.tv_nickname)
        val btnAdd: ImageView = view.findViewById(R.id.btn_add_friend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvNickname.text = user.nickname
        holder.btnAdd.setOnClickListener { onAddFriendClicked(user) }
    }

    override fun getItemCount(): Int = users.size
}
