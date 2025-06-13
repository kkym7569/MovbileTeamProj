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
    private val myFriendUids = mutableSetOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view =
            requireActivity().layoutInflater.inflate(R.layout.dialog_friend_request, null) // 재사용
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
    private fun loadFilteredFriendRequests() {
        val db = FirebaseFirestore.getInstance()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. 내 친구 UID 리스트 가져오기
        db.collection("friends").document(myUid).collection("list")
            .get().addOnSuccessListener { friendsSnapshot ->
                val myFriendUids = friendsSnapshot.documents.map { it.id }

                // 2. 친구 요청 목록에서 이미 친구인 애들은 제외하고 불러오기
                db.collection("requests")
                    .whereEqualTo("receiver", myUid)
                    .get().addOnSuccessListener { requestSnapshot ->
                        val filteredRequests = requestSnapshot.documents.filter { doc ->
                            val senderUid = doc.getString("sender")
                            senderUid != null && !myFriendUids.contains(senderUid)
                        }
                        // filteredRequests 리스트 → 어댑터에 넘겨주기
                        // 어댑터에 데이터 갱신 예시: adapter.submitList(filteredRequests.map { ... })
                    }
            }
    }

    private fun acceptFriendRequest(user: User) {
        val db = FirebaseFirestore.getInstance()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(myUid).get()
            .addOnSuccessListener { doc ->
                val myNickname = doc.getString("nickname") ?: ""
                val batch = db.batch()

                val myRef =
                    db.collection("friends").document(myUid).collection("list").document(user.uid)
                batch.set(myRef, mapOf("nickname" to user.nickname))  // 내 리스트에 상대 닉네임

                val theirRef =
                    db.collection("friends").document(user.uid).collection("list").document(myUid)
                batch.set(theirRef, mapOf("nickname" to myNickname))  // 상대 리스트에 내 닉네임

                // 요청 삭제 코드 그대로
                db.collection("requests")
                    .whereEqualTo("receiver", myUid)
                    .whereEqualTo("sender", user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().addOnSuccessListener {
                            Toast.makeText(requireContext(), "${user.nickname}와 친구가 되었습니다.", Toast.LENGTH_SHORT).show()
                            // 2. UI 갱신: 리스트에서 해당 요청 제거
                            requestList.remove(user)
                            adapter.notifyDataSetChanged()
                            // 처리 후 UI 업데이트 등
                        }
                    }
            }
    }
}