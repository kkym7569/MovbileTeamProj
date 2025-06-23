package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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
                            name = doc.getString("nickname") ?: "",
                            sleepStart = "",
                            sleepEnd = "",
                            condition = "",
                            hasSleepData = false  // ✅ 추가
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
                if (friendDocs.isEmpty()) {
                    adapter = FriendSleepAdapter(emptyList())
                    rvSleepFriends.adapter = adapter
                    return@addOnSuccessListener
                }

                val friendSleepList = friendDocs.map { doc ->
                    FriendSleepData(
                        name = doc.getString("nickname") ?: "",
                        sleepStart = "",
                        sleepEnd = "",
                        condition = "수면 정보 없음",
                        hasSleepData = false
                    )
                }.toMutableList()

                var loadedCount = 0
                for ((index, doc) in friendDocs.withIndex()) {
                    val friendUid = doc.id
                    val friendName = doc.getString("nickname") ?: ""

                    val startOfDay = date.atStartOfDay().toString()
                    val startOfNextDay = date.plusDays(1).atStartOfDay().toString()

                    db.collection("users").document(friendUid)
                        .collection("sleeps")
                        .whereGreaterThanOrEqualTo("startTime", startOfDay)
                        .whereLessThan("startTime", startOfNextDay)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            Log.d("SleepDebug", "Friend: $friendName, snapshot size: ${snapshot.size()}")

                            if (!snapshot.isEmpty) {
                                val record = snapshot.documents[0]
                                val rawStart = record.getString("startTime")
                                val rawEnd = record.getString("endTime")
                                val effValue = record.getDouble("efficiency")

                                if (rawStart != null && rawEnd != null) {
                                    val start = rawStart.replace("T", " ").substringBefore("+").substring(0, 16)
                                    val end = rawEnd.replace("T", " ").substringBefore("+").substring(0, 16)
                                    val eff = if (effValue != null && effValue != 0.0) "%.1f%%".format(effValue * 100) else "0%"

                                    val friend = friendSleepList[index]
                                    friend.sleepStart = start
                                    friend.sleepEnd = end
                                    friend.condition = eff
                                    friend.hasSleepData = true
                                }
                            }

                            loadedCount++
                            if (loadedCount == friendDocs.size) {
                                val sortedList = friendSleepList.sortedBy { it.name }
                                adapter = FriendSleepAdapter(sortedList)
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