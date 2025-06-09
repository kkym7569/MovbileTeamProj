package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.User
import kr.ac.jbnu.jun.mobileprojectgit.ui.adapter.FriendRequestAdapter

class FriendRequestDialog : DialogFragment() {
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private val requestList = mutableListOf<User>()
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_search_user, null) // 재사용
        val rvRequest = view.findViewById<RecyclerView>(R.id.rvUserSearch)

        adapter = FriendRequestAdapter(requestList) { user ->
            acceptFriendRequest(user)
        }

        rvRequest.layoutManager = LinearLayoutManager(context)
        rvRequest.adapter = adapter

        loadRequests()

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("친구 요청 목록")
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun loadRequests() {
        db.collection("requests")
            .whereEqualTo("receiver", currentUid)
            .get()
            .addOnSuccessListener { snapshot ->
                requestList.clear()
                for (doc in snapshot) {
                    val user = User(
                        uid = doc.getString("sender") ?: "",
                        nickname = doc.getString("nickname") ?: ""
                    )
                    requestList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun acceptFriendRequest(user: User) {
        // 친구 목록에 추가하고 요청 삭제
        val batch = db.batch()
        val myUid = currentUid ?: return

        val myRef = db.collection("friends").document(myUid)
        val theirRef = db.collection("friends").document(user.uid)

        batch.set(myRef.collection("list").document(user.uid), mapOf("nickname" to user.nickname))
        batch.set(theirRef.collection("list").document(myUid), mapOf("nickname" to "나")) // 실제 닉네임 필요

        db.collection("requests")
            .whereEqualTo("receiver", myUid)
            .whereEqualTo("sender", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
    }
}
