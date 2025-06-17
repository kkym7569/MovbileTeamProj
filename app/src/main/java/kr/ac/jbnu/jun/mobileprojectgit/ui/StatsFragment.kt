package kr.ac.jbnu.jun.mobileprojectgit.ui

import android.app.DatePickerDialog
import android.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.github.mikephil.charting.charts.HorizontalBarChart
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kr.ac.jbnu.jun.mobileprojectgit.R
import kr.ac.jbnu.jun.mobileprojectgit.adapter.ComparisonStatAdapter
import kr.ac.jbnu.jun.mobileprojectgit.model.AsleepReport
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepRecord
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepStage
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.mapIndexed

class StatsFragment : Fragment() {
    private lateinit var chartHypnogram: LineChart
    private lateinit var chartStageRatio: HorizontalBarChart

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
    private lateinit var tvStageRatioDetail: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnDatePrev: ImageButton
    private lateinit var btnDateNext: ImageButton

    private var selectedDate: LocalDate = LocalDate.now()


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
        tvStageRatioDetail = view.findViewById(R.id.tvStageRatioDetail)
        rvComparison.adapter = compAdapter

        barChart = view.findViewById(R.id.barChart)
        setupChart()

        loadMyInfo()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = FirebaseApp.getInstance()
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)

        tvSelectedDate.setOnClickListener {
            Log.d("StatsFragment", "날짜 텍스트 클릭됨!") // 로그 반드시 확인
            val now = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateText()
                    loadReportByDate(selectedDate)
                },
                now.year, now.monthValue - 1, now.dayOfMonth
            ).show()
        }

        Log.d("FirebaseApp", "Project ID: ${app.options.projectId}")
        chartHypnogram = view.findViewById(R.id.chartHypnogram)
        chartStageRatio = view.findViewById(R.id.chartStageRatio)

        updateDateText()
        loadReportByDate(selectedDate)

        loadRecentReportAndBindCharts()
        // 기타 기존 코드...
        btnDatePrev.setOnClickListener {
            selectedDate = selectedDate.minusDays(1)
            updateDateText()
            loadReportByDate(selectedDate)
        }
        btnDateNext.setOnClickListener {
            selectedDate = selectedDate.plusDays(1)
            updateDateText()
            loadReportByDate(selectedDate)
        }


    }


    private fun updateDateText() {
        tvSelectedDate.text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    private fun loadReportByDate(date: LocalDate) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val startOfDay = date.atStartOfDay().toString() + "+09:00" // "2025-06-17T00:00:00+09:00"
        val endOfDay = date.atTime(23, 59, 59).toString() + "+09:00"
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("sleeps")
            .whereGreaterThanOrEqualTo("startTime", startOfDay)
            .whereLessThanOrEqualTo("startTime", endOfDay)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents.first()
                    Log.d("StatsFragment", "불러온 sleep 데이터: ${doc.data}")
                    val sleepStages = doc.get("sleepStages") as? List<Long> ?: emptyList()
                    val stageNames = listOf("Wake", "REM", "Light", "Deep")
                    val stageList = sleepStages.map { idx ->
                        SleepStage(stage = stageNames.getOrElse(idx.toInt()) { "Wake" })
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                    val startTimeStr = docs.documents.first().getString("startTime") ?: ""
                    val endTimeStr = docs.documents.first().getString("endTime") ?: ""
                    val startTime = ZonedDateTime.parse(startTimeStr, formatter)
                    val endTime = ZonedDateTime.parse(endTimeStr, formatter)


                    val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
                    val rawInterval = if (sleepStages.size > 1) totalMinutes / (sleepStages.size - 1) else 1
                    val intervalMinutes = if (rawInterval < 1) 1 else rawInterval // 최소 1분

                    val timeLabels = sleepStages.indices.map { i ->
                        startTime.plusMinutes(i * intervalMinutes).toLocalTime().toString().substring(0, 5)
                    }
                    bindHypnogram(stageList, timeLabels)

                    // 2. 비율 계산 및 바차트+텍스트
                    showStageRatioAndChart(sleepStages)
                } else {
                    chartHypnogram.clear()
                    chartStageRatio.clear()
                    tvStageRatioDetail.text = ""
                }
            }
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
                Log.d("StatsFragment", "현재 로그인 uid: ${user.uid}")

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

                // 3) “20대 여성의”처럼 동적 헤더 만들기
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
                    // 기존 로직: 실제 duration 값을 받아와 그리기
                    val duration = docs.documents.first().getDouble("duration") ?: 0.0
                    drawComparisonChart(duration, populationAvgDuration)

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
    private fun bindHypnogram(stageList: List<SleepStage>, timeLabels: List<String>) {
        val entries = stageList.mapIndexed { i, s ->
            Entry(i.toFloat(), stageToY(s.stage))
        }

        val ds = LineDataSet(entries, "수면 단계 변화").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 3f
            color = ContextCompat.getColor(requireContext(), R.color.teal_700)
        }
        chartHypnogram.axisLeft.valueFormatter = IndexAxisValueFormatter(
            listOf("Wake","REM","Light","Deep")
        )
        chartHypnogram.axisLeft.granularity = 1f
        chartHypnogram.axisLeft.labelCount = 4
        chartHypnogram.axisLeft.axisMinimum = 0f
        chartHypnogram.axisLeft.axisMaximum = 3f

        chartHypnogram.axisLeft.textColor = Color.WHITE
        chartHypnogram.axisRight.isEnabled = false
        chartHypnogram.xAxis.isEnabled = true
        chartHypnogram.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartHypnogram.xAxis.valueFormatter = IndexAxisValueFormatter(timeLabels)
        chartHypnogram.xAxis.granularity = 1f
        chartHypnogram.xAxis.setLabelCount(timeLabels.size / 6, true)
        chartHypnogram.xAxis.textColor = Color.WHITE
        chartHypnogram.xAxis.labelRotationAngle = -45f // 기울이면 더 잘보임

        chartHypnogram.description.isEnabled = false
        chartHypnogram.legend.isEnabled = false
        chartHypnogram.data = LineData(ds)
        chartHypnogram.invalidate()
    }
    private fun stageToY(stage: String): Float = when(stage) {
        "Wake" -> 0f; "REM" -> 1f; "Light" -> 2f; "Deep" -> 3f
        else -> 0f
    }
    private fun bindStageRatio(r: AsleepReport) {
        val total = r.stat.wakeTime + r.stat.remSleep + r.stat.lightSleep + r.stat.deepSleep
        if (total <= 0) return
        val arr = floatArrayOf(
            r.stat.wakeTime / total * 100f,
            r.stat.remSleep / total * 100f,
            r.stat.lightSleep / total * 100f,
            r.stat.deepSleep / total * 100f
        )

        val ds = BarDataSet(listOf(BarEntry(0f, arr)), "단계별 비율").apply {
            setColors(
                Color.parseColor("#FFA726"),
                Color.parseColor("#64B5F6"),
                Color.parseColor("#9575CD"),
                Color.parseColor("#00897B")
            )
            stackLabels = arrayOf("Wake", "REM", "Light", "Deep")
            setDrawValues(true)
            valueTextSize = 16f

            setValueTextColor(Color.YELLOW)
        }
        val barData = BarData(ds)
        chartStageRatio.data = barData
        chartStageRatio.apply {
            data = BarData(ds).apply { barWidth = 0.95f }
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }

        chartStageRatio.invalidate()
    }

    private fun showStageRatioAndChart(sleepStages: List<Long>) {
        if (sleepStages.isEmpty()) {
            chartStageRatio.clear()
            tvStageRatioDetail.text = ""
            return
        }
        val total = sleepStages.size.toFloat()
        val counts = IntArray(4) // Wake, REM, Light, Deep
        for (idx in sleepStages) {
            if (idx in 0..3) counts[idx.toInt()]++
        }
        val percents = counts.map { it / total * 100f }
        val labels = listOf("Wake", "REM", "Light", "Deep")
        val ratioString = percents.mapIndexed { i, v -> "${labels[i]}: %.1f%%".format(v) }
            .joinToString("  ")
        tvStageRatioDetail.text = ratioString
        tvStageRatioDetail.setSingleLine(true)
        tvStageRatioDetail.ellipsize = TextUtils.TruncateAt.END

        // 스택형 바차트
        val arr = percents.toFloatArray()
        val ds = BarDataSet(listOf(BarEntry(0f, arr)), "단계별 비율").apply {
            setColors(
                Color.parseColor("#FFA726"), // Wake
                Color.parseColor("#64B5F6"), // REM
                Color.parseColor("#9575CD"), // Light
                Color.parseColor("#00897B")  // Deep
            )
            stackLabels = arrayOf("Wake", "REM", "Light", "Deep")
            setDrawValues(true)
            valueTextSize = 18f
            setValueTextColor(Color.YELLOW)
        }
        chartStageRatio.apply {
            data = BarData(ds).apply { barWidth = 0.98f }
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }
    }
    private fun loadRecentReportAndBindCharts() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("sleeps")
            .orderBy("session.startTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                Log.d("StatsFragment", "sleeps 문서 개수: ${docs.size()}")
                if (!docs.isEmpty) {
                    val report = docs.documents.first().toObject(AsleepReport::class.java)
                    Log.d("StatsFragment", "파싱된 report: $report")
                    report?.let {
                        // 1. 시작 시간 파싱
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                        val startTimeStr = docs.documents.first().getString("startTime")
                        val startTime = ZonedDateTime.parse(startTimeStr, formatter)
                        // 2. 시간 라벨 생성 (예: 10분 단위 데이터면 10L)
                        val timeLabels = it.stageList.indices.map { i ->
                            startTime.plusMinutes(i * 10L).toLocalTime().toString().substring(0, 5)
                        }
                        bindHypnogram(it.stageList, timeLabels)
                        bindStageRatio(it)
                        // 추가 바인딩 필요하면 여기에!
                    }
                }
            }
    }
}


