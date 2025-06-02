package kr.ac.jbnu.jun.mobileprojectgit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kr.ac.jbnu.jun.mobileprojectgit.adapter.SleepRecordAdapter
import kr.ac.jbnu.jun.mobileprojectgit.model.SleepRecord

class SleepShareActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SleepRecordAdapter
    private val recordList = mutableListOf<SleepRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep_share)
        recyclerView = findViewById(R.id.rvSleepRecords)

        // 어댑터 연결
        adapter = SleepRecordAdapter(recordList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Firestore에서 데이터 불러오기
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { userSnapshots ->
                recordList.clear()
                for (userDoc in userSnapshots.documents) {
                    val userId = userDoc.id
                    val nickname = userDoc.getString("nickname") ?: ""
                    db.collection("users").document(userId).collection("sleeps")
                        .get()
                        .addOnSuccessListener { sleepSnapshots ->
                            for (sleepDoc in sleepSnapshots.documents) {
                                val record = sleepDoc.toObject(SleepRecord::class.java)
                                if (record != null) {
                                    // nickname을 SleepRecord에 넣어서 추가
                                    recordList.add(record.copy(nickname = nickname))
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                }
            }

    }
}