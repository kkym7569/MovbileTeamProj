package kr.ac.jbnu.jun.mobileprojectgit

import ai.asleep.asleepsdk.data.AsleepConfig
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NicknameActivity : AppCompatActivity() {
    private lateinit var editTextNickname: EditText
    private lateinit var buttonConfirm: Button
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var spinnerAge: Spinner

    val TAG = "[AsleepSDK]"
    private var createdUserId: String? = null
    private var createdAsleepConfig: AsleepConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        editTextNickname = findViewById(R.id.et_nickname)
        buttonConfirm = findViewById(R.id.btn_confirm)

        radioGroupGender = findViewById(R.id.rg_gender)
        spinnerAge = findViewById(R.id.spinner_age)


        val ageList = (1..100).map { "$it 세" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ageList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAge.adapter = adapter

        buttonConfirm.setOnClickListener {
            val nickname = editTextNickname.text.toString().trim()

            // 성별 선택값
            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val genderText = if (selectedGenderId != -1) {
                findViewById<RadioButton>(selectedGenderId).text.toString()
            } else null

            val gender: Long? = when (genderText) {
                "남" -> 0L
                "여" -> 1L
                else -> null
            }

            val age = spinnerAge.selectedItem?.toString()?.split(" ")?.get(0)?.toLongOrNull()

            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else if (gender == null) {
                Toast.makeText(this, "성별을 선택하세요.", Toast.LENGTH_SHORT).show()
            } else if (age == null) {
                Toast.makeText(this, "나이를 선택하세요.", Toast.LENGTH_SHORT).show()
            } else {
                saveNicknameToFirestore(nickname, gender, age)
            }
        }
    }


    private fun saveNicknameToFirestore(nickname: String, gender: Long, age: Long) {
        // 익명 로그인 or 이미 로그인된 상태면 재로그인 X
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { authResult ->
                    saveUserData(authResult.user?.uid, nickname, gender, age)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveUserData(auth.currentUser?.uid, nickname, gender, age)
        }
    }

    private fun saveUserData(userId: String?, nickname: String, gender: Long, age: Long) {
        if (userId == null) {
            Toast.makeText(this, "유저 정보 없음", Toast.LENGTH_SHORT).show()
            Log.d("test", "유저 정보 없음! userId=null")
            return
        }

        val userMap = hashMapOf(
            "nickname" to nickname,
            "gender" to gender,
            "age" to age)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(userMap)  // ⭐ 저장(set) 먼저!
            .addOnSuccessListener {
                // set 성공시 최초 init으로 userId 생성
                /*
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
                    */


                // set 성공시 바로 MainActivity로 이동
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Log.d("test", "닉네임 저장 실패: ${it.message}")
                Toast.makeText(this, "닉네임 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}