package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.adapter.ComparisonStatAdapter
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepRecord

class StatsFragment : Fragment() {

    private lateinit var rvComparison: RecyclerView
    private lateinit var compAdapter: ComparisonStatAdapter

    private lateinit var tvCount: TextView
    private lateinit var tvAvgDuration: TextView

    private lateinit var tvNoData: TextView
    private lateinit var tvMyInfo: TextView
    private val recordList = mutableListOf<SleepRecord>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        tvMyInfo = view.findViewById(R.id.tvMyInfo)
        rvComparison = view.findViewById(R.id.rvComparisonStats)

        tvNoData     = view.findViewById(R.id.tvNoData)
        compAdapter = ComparisonStatAdapter(recordList)

        tvCount       = view.findViewById(R.id.tvCount)
        tvAvgDuration = view.findViewById(R.id.tvAvgDuration)


        rvComparison.layoutManager = LinearLayoutManager(requireContext())
        rvComparison.adapter = compAdapter

        loadMyInfo()

        return view
    }

    //현재 내 나이 / 내 성별 출력하는 함수
    private fun loadMyInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            tvMyInfo.text = "내 나이: --    내 성별: -- (로그인 필요)"
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                // Firestore 에 Long 으로 저장된 gender
                val genderLong = doc.getLong("gender") ?: -1L
                val genderStr = when(genderLong) {
                    0L -> "남성"
                    1L -> "여성"
                    else -> "--"
                }
                val age = doc.getLong("age")?.toInt()
                tvMyInfo.text = "내 나이: ${age ?: "--"}    내 성별: $genderStr"

                if (age != null && genderLong in 0..1) {
                    val decade = age / 10
                    val lower  = decade * 10
                    val upper  = (lower + 9).coerceAtMost(100)
                    loadFilteredSleepData(lower, upper, genderLong)
                } else {
                    showEmpty(true)
                }
            }
            .addOnFailureListener {
                tvMyInfo.text = "내 나이: --    내 성별: -- (불러오기 실패)"
                showEmpty(true)
            }
    }

    private fun showEmpty(isEmpty: Boolean) {
        if (isEmpty) {
            rvComparison.visibility = View.GONE
            tvNoData.visibility     = View.VISIBLE
        } else {
            rvComparison.visibility = View.VISIBLE
            tvNoData.visibility     = View.GONE
        }
    }

    // 2) Firestore 에서 성별·나이대 필터링하여 가져오기
    private fun loadFilteredSleepData(lowerAge: Int, upperAge: Int, gender: Long) {
        val db = FirebaseFirestore.getInstance()
        db.collection("public_sleep_data")
            .whereEqualTo("gender", gender)
            .whereGreaterThanOrEqualTo("age", lowerAge)
            .whereLessThanOrEqualTo("age", upperAge)
            .get()
            .addOnSuccessListener { docs ->
                // 1) 대상 인원
                val count = docs.size()
                tvCount.text = "비교 통계 수: $count"

                // 2) duration 합산·평균 계산
                var sum = 0.0
                for (doc in docs) {
                    doc.toObject(SleepRecord::class.java)?.let {
                        sum += it.duration
                    }
                }
                val avg = if (count > 0) sum / count else 0.0
                tvAvgDuration.text = "평균 수면시간: %.2f시간".format(avg)

                // 3) 리스트 대신 통계만 보여줄 거면 RecyclerView 숨기기
                rvComparison.visibility = View.GONE
                tvNoData.visibility     = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("StatsFragment", "filtered query failed", e)
                Toast.makeText(requireContext(),
                    "데이터를 불러오는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT).show()
            }
    }

}

