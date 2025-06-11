package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
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

class SearchUserDialog(private val onSendRequest: (User) -> Unit) : DialogFragment() {
    private val db = FirebaseFirestore.getInstance()
    private val resultList = mutableListOf<User>()
    private lateinit var adapter: SearchUserAdapter


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_search_user, null)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvUsers = view.findViewById<RecyclerView>(R.id.rvUserSearch)
        val tvNoResult = view.findViewById<TextView>(R.id.tvNoResult)
// 어댑터에서 친구 요청 시
        adapter = SearchUserAdapter(resultList) { user ->
            sendFriendRequest(user)
            dismiss()
        }
        adapter = SearchUserAdapter(resultList) { user ->
            onSendRequest(user)
            dismiss()
        }

        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = adapter

        etSearch.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString()
            searchUser(query, tvNoResult)
            true
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
                    val user = doc.toObject(User::class.java)
                    resultList.add(user)
                }
                adapter.notifyDataSetChanged()
                tvNoResult.visibility = if (resultList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
    // 친구 요청 보내기 함수 추가
    private fun sendFriendRequest(targetUser: User) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val myNickname = "내닉네임" // 실제 내 닉네임 데이터로 대체

        // 요청 중복/자기자신 방지 필요시 체크 추가 가능

        val request = hashMapOf(
            "sender" to myUid,           // 요청 보낸 사람
            "receiver" to targetUser.uid, // 요청 받는 사람
            "nickname" to myNickname
        )

        FirebaseFirestore.getInstance()
            .collection("requests")
            .add(request)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "요청이 전송되었습니다.", Toast.LENGTH_SHORT).show()
            }
    }


}
