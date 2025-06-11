package kr.ac.jbnu.jun.mobileprojectgit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        adapter = SearchUserAdapter(resultList) { user ->
            onSendRequest(user)
            dismiss()
        }

        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = adapter

        etSearch.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString()
            searchUser(query)
            true
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("친구 검색")
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun searchUser(nickname: String) {
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
            }
    }
}
