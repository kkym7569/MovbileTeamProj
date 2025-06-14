package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
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
            if (!isMyNicknameLoaded || myNickname.isNullOrBlank()) {

                onSendRequest(user, false)
                return@SearchUserAdapter
            }
            sendFriendRequest(user) { isSuccess ->
                onSendRequest(user, isSuccess)
            }
            dismiss()
        }

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
    private fun sendFriendRequest(targetUser: User, callback: (Boolean) -> Unit) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (myNickname.isNullOrBlank()) {
            callback(false)
            return
        }
        val request = hashMapOf(
            "sender" to myUid,
            "receiver" to targetUser.uid,
            "nickname" to myNickname
        )
        FirebaseFirestore.getInstance()
            .collection("requests")
            .add(request)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}
