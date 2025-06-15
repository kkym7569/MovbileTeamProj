package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.User
import kr.ac.jbnu.jun.mobileprojectgit.ui.adapter.SearchUserAdapter

class SearchUserDialog(private val onSendRequest: (User, Boolean) -> Unit) : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private val resultList = mutableListOf<User>()
    private lateinit var adapter: SearchUserAdapter
    private val myFriendUids = mutableSetOf<String>() // 내 친구 uid 목록

    private var myNickname: String? = null
    private var isMyNicknameLoaded = false

    private fun loadMyFriendsAndSearch(nickname: String, tvNoResult: TextView) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("friends").document(myUid).collection("list")
            .get()
            .addOnSuccessListener { snapshot ->
                myFriendUids.clear()
                for (doc in snapshot) {
                    myFriendUids.add(doc.id)
                }
                Log.d("디버깅", "불러온 친구 수: ${myFriendUids.size}")

                // nickname은 함수 매개변수로 전달된 값을 사용
                searchUser(nickname, tvNoResult)
            }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_search_user, null)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvUsers = view.findViewById<RecyclerView>(R.id.rvUserSearch)
        val tvNoResult = view.findViewById<TextView>(R.id.tvNoResult)
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        val tvMyNickname = view.findViewById<TextView>(R.id.tvMyNickname)


        if (myUid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(myUid)
                .get()
                .addOnSuccessListener { doc ->
                    myNickname = doc.getString("nickname") ?: ""
                    tvMyNickname.text = getString(R.string.my_nickname_display, myNickname)
                    isMyNicknameLoaded = true
                }
        }

        etSearch.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString()
            loadMyFriendsAndSearch(query, tvNoResult) // 친구 먼저 불러오고, 그 후 검색
            true
        }
// 어댑터에서 친구 요청 시
        adapter = SearchUserAdapter(resultList) { user: User ->
            sendFriendRequest(user) { isSuccess, msg ->
                val safeContext = this.context
                if (safeContext != null) {
                    Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                }
                // 외부 콜백은 딱 성공 여부만 전달!
                onSendRequest(user, isSuccess)
                // dismiss()도 이 콜백 안에서, 성공시에만 처리하는 게 자연스러움!
                if (isSuccess) dismiss()
            }
        }
            // 수정된 sendFriendRequest를 사용

        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = adapter


        if (myUid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(myUid)
                .get()
                .addOnSuccessListener { doc ->
                    val myNickname = doc.getString("nickname") ?: ""
                    tvMyNickname.text = getString(R.string.my_nickname_display, myNickname)
                }
        }
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("친구 검색")
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun searchUser(nickname: String, tvNoResult: TextView) {

        db.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .addOnSuccessListener { snapshot ->
                resultList.clear()
                for (doc in snapshot) {
                    val user = doc.toObject(User::class.java).copy(uid = doc.id)
                    val myUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (user.uid != myUid && !myFriendUids.contains(user.uid)) {
                        resultList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
                tvNoResult.visibility = if (resultList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
    // 친구 요청 보내기 함수 추가
    private fun sendFriendRequest(targetUser: User, callback: (Boolean, String) -> Unit) {
        Log.d("친구요청", "실행됨: targetUser=${targetUser.uid}, nickname=$myNickname")
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myUid == null || myNickname.isNullOrBlank()) {
            callback(false, "요청 실패(닉네임 문제 등)!")
            return
        }

        val idPair = listOf(myUid, targetUser.uid).sorted()
        val docId = "${idPair[0]}_${idPair[1]}"

        val request = hashMapOf(
            "sender" to myUid,
            "receiver" to targetUser.uid,
            "nickname" to myNickname
        )

        val requestRef = FirebaseFirestore.getInstance().collection("requests").document(docId)
        requestRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(false, "이미 보낸 요청입니다.")
                } else {
                    requestRef.set(request)
                        .addOnSuccessListener { callback(true, "친구 요청을 보냈습니다.") }
                        .addOnFailureListener { callback(false, "요청 실패(네트워크 오류 등)!") }
                }
            }
            .addOnFailureListener { callback(false, "요청 실패(네트워크 오류 등)!") }
    }
}
