package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
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
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("requests")
            .whereEqualTo("receiver", myUid)
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
        val db = FirebaseFirestore.getInstance()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 내 닉네임 가져오는 코드 필요
        val myNickname = "내닉네임" // 실제로 내 닉네임 fetch해서 써야 함

        val batch = db.batch()
        // 내 친구목록에 추가
        val myRef = db.collection("friends").document(myUid).collection("list").document(user.uid)
        batch.set(myRef, mapOf("nickname" to user.nickname))
        // 상대방 친구목록에 나 추가
        val theirRef = db.collection("friends").document(user.uid).collection("list").document(myUid)
        batch.set(theirRef, mapOf("nickname" to myNickname))

        // 요청문서 찾아서 삭제
        db.collection("requests")
            .whereEqualTo("receiver", myUid)
            .whereEqualTo("sender", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(requireContext(), "친구로 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    loadRequests() // 리스트 갱신
                }
            }
    }
}
