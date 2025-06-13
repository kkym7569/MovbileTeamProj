package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.model.FriendSleepData
import kr.ac.jbnu.jun.mobileprojectgit.ui.adapter.FriendSleepAdapter
import kr.ac.jbnu.jun.mobileprojectgit.ui.dialog.FriendRequestDialog
import kr.ac.jbnu.jun.mobileprojectgit.ui.dialog.SearchUserDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FriendsFragment : Fragment() {
    private lateinit var rvSleepFriends: RecyclerView
    private lateinit var adapter: FriendSleepAdapter
    private lateinit var btnAddFriend: TextView
    private lateinit var btnRequestAlert: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var tvEmptyFriends: TextView

    private var friendListener: ListenerRegistration? = null

    // 기본값: 어제 날짜
    private var selectedDate: LocalDate = LocalDate.now().minusDays(1)
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// ... 기존 코드 ...
        tvEmptyFriends = view.findViewById(R.id.tvEmptyFriends)

        // ... 나머지 뷰 초기화 ...

        setUpFriendListener()
        loadSleepData(selectedDate)
        // 뷰 연결
        rvSleepFriends = view.findViewById(R.id.rv_sleep_friends)
        btnAddFriend = view.findViewById(R.id.btn_add_friend)
        btnRequestAlert = view.findViewById(R.id.btn_request_alert)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)

        rvSleepFriends.layoutManager = LinearLayoutManager(requireContext())

        // 날짜 초기 세팅 (어제)
        btnSelectDate.text = selectedDate.format(dateFormatter)

        // 날짜 선택 버튼 클릭 시
        btnSelectDate.setOnClickListener {
            val now = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    btnSelectDate.text = selectedDate.format(dateFormatter)
                    loadSleepData(selectedDate)
                },
                now.year, now.monthValue - 1, now.dayOfMonth
            ).show()
        }

        // 친구 추가
        btnAddFriend.setOnClickListener {
            SearchUserDialog { selectedUser, isSuccess ->
                requireActivity().runOnUiThread {
                    if (isSuccess) {
                        Toast.makeText(requireContext(), "${selectedUser.nickname}에게 요청 보냄", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "요청 실패(닉네임 문제 등)!", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show(parentFragmentManager, "SearchUserDialog")
        }

        // 친구 요청 알림
        btnRequestAlert.setOnClickListener {
            FriendRequestDialog().show(parentFragmentManager, "FriendRequestDialog")
        }

        // 친구 목록 리스너 (실시간 반영)
        setUpFriendListener()

        // 최초 로딩
        loadSleepData(selectedDate)
    }

    private fun setUpFriendListener() {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        friendListener = db.collection("friends").document(myUid).collection("list")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val friends = snapshot.map { doc ->
                        FriendSleepData(
                            doc.getString("nickname") ?: "",
                            "", "", ""
                        )
                    }
                    adapter = FriendSleepAdapter(friends)
                    rvSleepFriends.adapter = adapter
                    if (friends.isEmpty()) {
                        tvEmptyFriends.visibility = View.VISIBLE
                        rvSleepFriends.visibility = View.GONE
                    } else {
                        tvEmptyFriends.visibility = View.GONE
                        rvSleepFriends.visibility = View.VISIBLE
                    }
                }
            }
    }
    private fun loadSleepData(date: LocalDate) {
        val db = FirebaseFirestore.getInstance()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dateStr = date.format(dateFormatter)

        db.collection("friends").document(myUid).collection("list")
            .get().addOnSuccessListener { friendsSnapshot ->
                val friendDocs = friendsSnapshot.documents
                val sleepDataList = mutableListOf<FriendSleepData>()

                if (friendDocs.isEmpty()) {
                    adapter = FriendSleepAdapter(emptyList())
                    rvSleepFriends.adapter = adapter
                    return@addOnSuccessListener
                }

                var loadedCount = 0
                for (doc in friendDocs) {
                    val friendUid = doc.id
                    val friendName = doc.getString("nickname") ?: ""
                    db.collection("sleepRecords").document(friendUid)
                        .collection("records").document(dateStr)
                        .get()
                        .addOnSuccessListener { recordDoc ->
                            if (recordDoc.exists()) {
                                val start = recordDoc.getString("sleepStart") ?: "-"
                                val end = recordDoc.getString("sleepEnd") ?: "-"
                                val cond = recordDoc.getString("condition") ?: "-"
                                sleepDataList.add(
                                    FriendSleepData(friendName, start, end, cond)
                                )
                            } else {
                                sleepDataList.add(
                                    FriendSleepData(friendName, "수면 정보 없음", "수면 정보 없음", "수면 정보 없음")
                                )
                            }
                            loadedCount++
                            if (loadedCount == friendDocs.size) {
                                val sortedList = sleepDataList.sortedBy { it.name }
                                adapter = FriendSleepAdapter(sortedList)
                                rvSleepFriends.adapter = adapter
                            }
                        }
                        .addOnFailureListener {
                            sleepDataList.add(
                                FriendSleepData(friendName, "수면 정보 없음", "수면 정보 없음", "수면 정보 없음")
                            )
                            loadedCount++
                            if (loadedCount == friendDocs.size) {
                                adapter = FriendSleepAdapter(sleepDataList)
                                rvSleepFriends.adapter = adapter
                            }
                        }
                }
            }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        friendListener?.remove()
    }
}
