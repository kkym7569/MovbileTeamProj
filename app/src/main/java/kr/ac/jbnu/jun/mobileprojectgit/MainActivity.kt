package kr.ac.jbnu.jun.mobileprojectgit

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var btnInit: Button
    private lateinit var btnBegin: Button
    private lateinit var btnEnd: Button
    private lateinit var btnReport: Button
    private lateinit var btnShareList: Button
    val TAG = "[AsleepSDK]"
    private var createdUserId: String? = null
    private var createdAsleepConfig: AsleepConfig? = null
    private var createdSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NavController를 NavHostFragment로부터 안전하게 가져오기
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        // Firestore에서 닉네임 있는지 확인
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        Log.d("test", "어디까지 진입")
        if (user == null) {
            // 익명 로그인 먼저 시도
            auth.signInAnonymously().addOnSuccessListener {
                checkNickname(it.user?.uid)
                Log.d("test", "어디까지 진입")
            }
        } else {
            checkNickname(user.uid)
        }
    }

    private fun checkNickname(userId: String?) {
        if (userId == null) {
            // 닉네임 입력 화면으로
            goToNickname()
            return
        }
        val docRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        docRef.get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.getString("nickname") != null) {
                // 닉네임 있으면 메인 UI 보여주기
                setupMainUi()
                // ...여기에 메인 로직 계속 작성
            } else {
                // 닉네임 없으면 입력 화면으로 이동
                goToNickname()
            }
        }.addOnFailureListener {
            goToNickname()
        }
    }
    private fun goToNickname() {
        startActivity(Intent(this, NicknameActivity::class.java))
        finish()
    }
    private fun saveSleepRecordToFirestore(report: Report?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        // Report에서 적절히 값 추출

        val db = FirebaseFirestore.getInstance()
        // 1. 먼저 nickname 가져오기
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val nickname = userDoc.getString("nickname") ?: "익명"

            // 2. sleepData에 nickname까지 추가!
            val startTime = report?.session?.startTime?.toString() ?: ""
            val endTime = report?.session?.endTime?.toString() ?: ""
            val duration = if (startTime.isNotEmpty() && endTime.isNotEmpty()) 0 else 0 // 실제 duration 계산 추가 가능

            val sleepData = hashMapOf(
                "nickname" to nickname,
                "startTime" to startTime,
                "endTime" to endTime,
                "duration" to duration
            )

            db.collection("users")
                .document(userId)
                .collection("sleeps")
                .add(sleepData)
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "수면 기록 저장 성공: ${docRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "수면 기록 저장 실패: ${e.message}")
                }
        }
    }
    private fun setupMainUi() {
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.POST_NOTIFICATIONS),
            0)

        btnInit = findViewById(R.id.btn_init)
        btnBegin = findViewById(R.id.btn_begin)
        btnEnd = findViewById(R.id.btn_end)
        btnReport = findViewById(R.id.btn_report)
        btnShareList = findViewById<Button>(R.id.btnShareList)

        btnInit.setOnClickListener {
            Asleep.initAsleepConfig(
                context = this,
                apiKey = "zhfpK9DcIzv0YQlUHE6swAb1Zs92jaiDanBLoaeX",
                userId = null,
                service = "Test App",
                asleepConfigListener = object: Asleep.AsleepConfigListener {
                    override fun onFail(errorCode: Int, detail: String) {
                        Log.d(TAG, "initAsleepConfig onFail $errorCode $detail")
                    }

                    override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                        Log.d(TAG, "initAsleepConfig onSuccess $userId")
                        createdUserId = userId
                        createdAsleepConfig = asleepConfig
                    }
                })
        }
        btnBegin.setOnClickListener {
            createdAsleepConfig?.let { asleepConfig ->

                Asleep.beginSleepTracking(
                    asleepConfig = asleepConfig,
                    asleepTrackingListener = object : Asleep.AsleepTrackingListener {
                        override fun onFail(errorCode: Int, detail: String) {
                            Log.d(TAG, "beginSleepTracking onFail $errorCode $detail")
                        }

                        override fun onFinish(sessionId: String?) {
                            Log.d(TAG, "beginSleepTracking onFinish $sessionId")
                            if (sessionId != null) {
                                val reports = Asleep.createReports(createdAsleepConfig)
                                reports?.getReport(
                                    sessionId = sessionId,
                                    reportListener = object : Reports.ReportListener {
                                        override fun onFail(errorCode: Int, detail: String) {
                                            Log.d(TAG, "getReport onFail $errorCode $detail")
                                        }
                                        override fun onSuccess(report: Report?) {
                                            Log.d(TAG, "getReport onSuccess $report")
                                            // *** 여기서 파이어스토어에 저장 ***
                                            saveSleepRecordToFirestore(report)
                                        }
                                    })
                            }
                        }

                        override fun onPerform(sequence: Int) {
                            Log.d(TAG, "beginSleepTracking onPerform $sequence")
                        }

                        override fun onStart(sessionId: String) {
                            Log.d(TAG, "beginSleepTracking onStart $sessionId")
                            createdSessionId = sessionId
                        }
                    })
            }
        }
        btnEnd.setOnClickListener {
            Asleep.endSleepTracking()

        }
        btnReport.setOnClickListener {
            createdSessionId?.let { sessionId ->

                val reports = Asleep.createReports(createdAsleepConfig)

                reports?.getReport(
                    sessionId = sessionId,
                    reportListener = object : Reports.ReportListener {
                        override fun onFail(errorCode: Int, detail: String) {
                            Log.d(TAG, "getReport onFail $errorCode $detail")
                        }
                        override fun onSuccess(report: Report?) {
                            Log.d(TAG, "getReport onSuccess $report")
                        }
                    })
            }
        }
        btnShareList.setOnClickListener {
            val intent = Intent(this, SleepShareActivity::class.java)
            startActivity(intent)
        }
    }
}

