package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    private lateinit var tvRecentDate: TextView
    private lateinit var tvRecentDuration: TextView

    private lateinit var tvComparisonRemark: TextView
    private lateinit var barChart: BarChart


    private val recordList = mutableListOf<SleepRecord>()
    private var populationAvgDuration: Double = 0.0

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

        tvRecentDate     = view.findViewById(R.id.tvRecentDate)
        tvRecentDuration = view.findViewById(R.id.tvRecentDuration)

        compAdapter = ComparisonStatAdapter(recordList)
        rvComparison.layoutManager = LinearLayoutManager(requireContext())

        tvComparisonRemark = view.findViewById(R.id.tvComparisonRemark)

        rvComparison.adapter = compAdapter

        barChart = view.findViewById(R.id.barChart)
        setupChart()

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
                // 1) 인원 수
                val count = docs.size()
                tvCount.text = "유사한 $count 개의 데이터를 찾았습니다. "

                // 2) 수면시간 합계·평균
                var sum = 0.0
                for (doc in docs) {
                    doc.toObject(SleepRecord::class.java)?.let {
                        sum += it.duration
                    }
                }
                val avg = if (count > 0) sum / count else 0.0
                populationAvgDuration = avg

                // 3) 동적 헤더 만들기
                val decadeLabel = "${lowerAge / 10}0대"
                val genderLabel = if (gender == 0L) "남성" else "여성"
                val header = "$decadeLabel ${genderLabel}의\n"

                // 4) 텍스트 설정
                tvAvgDuration.text = buildString {
                    append(header)
                    append("평균 수면시간: %.2f시간".format(avg))
                }

                // 5) 리스트 숨기기
                rvComparison.visibility = View.GONE
                tvNoData.visibility     = View.GONE
                loadMyRecentSleep()
            }
            .addOnFailureListener { e ->
                Log.e("StatsFragment", "filtered query failed", e)
                Toast.makeText(requireContext(),
                    "데이터를 불러오는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT).show()
            }
    }
    private fun loadMyRecentSleep() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("sleeps")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                Log.d("StatsFragment", "▶ sleeps 문서 개수 = ${docs.size()}")
                if (docs.isEmpty) {
                    // 1) 빈 데이터이므로 0.0 vs 평균으로 차트 그리기
                    drawComparisonChart(
                        myValue  = 0.0,
                        popValue = populationAvgDuration
                    )
                    // 2) 안내 텍스트
                    tvComparisonRemark.text = "최근 수면 기록이 없어요"
                } else {
                    val doc = docs.documents.first()
                    val startTimeStr = doc.getString("startTime") ?: "--"
                    val duration = docs.documents.first().getDouble("duration") ?: 0.0

                    val dateOnly = if (startTimeStr.length >= 10) startTimeStr.substring(0, 10) else "--"

                    drawComparisonChart(duration, populationAvgDuration)


                    tvRecentDate.text = "나의 최근 수면 일자: $dateOnly"
                    tvRecentDuration.text = "나의 최근 수면 시간: %.2f시간".format(duration)
                    // 기존 Remark 세팅
                    val diff = duration - populationAvgDuration
                    tvComparisonRemark.text = when {
                        diff < 0  -> "평균보다 %.2f시간 못 잤습니다".format(-diff)
                        diff > 0  -> "평균보다 %.2f시간 더 잤습니다".format(diff)
                        else      -> "평균과 같습니다"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StatsFragment", "최근 수면 조회 실패", e)
            }
    }
    private fun setupChart() {

        val white = ContextCompat.getColor(requireContext(), R.color.white)
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false

            // 왼쪽 Y축 설정
            axisLeft.apply {
                isEnabled = true
                setDrawLabels(true)
                setDrawGridLines(true)
                granularity = 1f
                labelCount = 6
                axisMinimum = 0f

                textColor = white
                gridColor = white

            }

            // 오른쪽 Y축은 끌 거면
            axisRight.isEnabled = false

            // X축 설정
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawLabels(true)
                setDrawGridLines(false)
                granularity = 1f

                textColor = white
                valueFormatter = IndexAxisValueFormatter(listOf("내 최근 수면", "평균 수면"))
            }

            animateY(600)
        }
    }

    /** 그래프에 데이터 바인딩 */
    private fun drawComparisonChart(myValue: Double, popValue: Double) {
        val entries = listOf(
            BarEntry(0f, myValue.toFloat()),
            BarEntry(1f, popValue.toFloat())
        )
        val set = BarDataSet(entries, "수면시간 비교").apply {
            setColors(
                resources.getColor(R.color.teal_700, null),
                resources.getColor(R.color.red_700, null)
            )
            valueTextSize = 12f
        }
        barChart.data = BarData(set).apply { barWidth = 0.5f }
        barChart.xAxis.valueFormatter =
            IndexAxisValueFormatter(listOf("내 최근 수면", "평균 수면"))
        barChart.invalidate()
    }
}


