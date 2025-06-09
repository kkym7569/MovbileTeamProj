package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.ui.adapter.FriendSleepAdapter
import kr.ac.jbnu.jun.mobileprojectgit.model.FriendSleepData
import kr.ac.jbnu.jun.mobileprojectgit.ui.dialog.SearchUserDialog
import kr.ac.jbnu.jun.mobileprojectgit.ui.dialog.FriendRequestDialog

class FriendsFragment : Fragment() {

    private lateinit var rvSleepFriends: RecyclerView
    private lateinit var adapter: FriendSleepAdapter
    private lateinit var btnAddFriend: TextView      // 친구 추가 ➕ 버튼
    private lateinit var btnRequestAlert: TextView   // 알림 🔔 버튼

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화
        rvSleepFriends = view.findViewById(R.id.rv_sleep_friends)
        btnAddFriend = view.findViewById(R.id.btn_add_friend)
        btnRequestAlert = view.findViewById(R.id.btn_request_alert)

        rvSleepFriends.layoutManager = LinearLayoutManager(requireContext())

        // 친구 수면 데이터 샘플
        val friendList = listOf(
            FriendSleepData("민수", "23:30", "07:00", "좋음"),
            FriendSleepData("지훈", "01:20", "06:10", "부족"),
            FriendSleepData("유진", "22:50", "07:30", "충분")
        )

        adapter = FriendSleepAdapter(friendList)
        rvSleepFriends.adapter = adapter

        // ➕ 버튼 클릭 시 친구 검색 다이얼로그
        btnAddFriend.setOnClickListener {
            SearchUserDialog { selectedUser ->
                Toast.makeText(requireContext(), "${selectedUser.nickname}에게 요청 보냄", Toast.LENGTH_SHORT).show()
            }.show(parentFragmentManager, "SearchUserDialog")
        }

        // 🔔 버튼 클릭 시 친구 요청 수락 다이얼로그
        btnRequestAlert.setOnClickListener {
            FriendRequestDialog().show(parentFragmentManager, "FriendRequestDialog")
        }
    }
}
