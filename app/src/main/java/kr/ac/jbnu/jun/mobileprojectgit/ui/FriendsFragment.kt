package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.ui.adapter.FriendSleepAdapter
import kr.ac.jbnu.jun.mobileprojectgit.model.FriendSleepData

class FriendsFragment : Fragment() {

    private lateinit var rvSleepFriends: RecyclerView
    private lateinit var adapter: FriendSleepAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvSleepFriends = view.findViewById(R.id.rv_sleep_friends)
        rvSleepFriends.layoutManager = LinearLayoutManager(requireContext())

        // 샘플 데이터
        val friendList = listOf(
            FriendSleepData("민수", "23:30", "07:00", "좋음"),
            FriendSleepData("지훈", "01:20", "06:10", "부족"),
            FriendSleepData("유진", "22:50", "07:30", "충분")
        )

        adapter = FriendSleepAdapter(friendList)
        rvSleepFriends.adapter = adapter
    }
}
