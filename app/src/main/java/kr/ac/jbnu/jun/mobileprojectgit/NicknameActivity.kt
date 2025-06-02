package kr.ac.jbnu.jun.mobileprojectgit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NicknameActivity : AppCompatActivity() {
    private lateinit var editTextNickname: EditText
    private lateinit var buttonConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        editTextNickname = findViewById(R.id.et_nickname)
        buttonConfirm = findViewById(R.id.btn_confirm)

        buttonConfirm.setOnClickListener {
            val nickname = editTextNickname.text.toString().trim()
            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                saveNicknameToFirestore(nickname)
            }
        }
    }

    private fun saveNicknameToFirestore(nickname: String) {
        // 익명 로그인 or 이미 로그인된 상태면 재로그인 X
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { authResult ->
                    saveUserData(authResult.user?.uid, nickname)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveUserData(auth.currentUser?.uid, nickname)
        }
    }

    private fun saveUserData(userId: String?, nickname: String) {
        if (userId == null) {
            Toast.makeText(this, "유저 정보 없음", Toast.LENGTH_SHORT).show()
            Log.d("test", "유저 정보 없음! userId=null")
            return
        }

        val userMap = hashMapOf("nickname" to nickname)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(userMap)  // ⭐ 저장(set) 먼저!
            .addOnSuccessListener {
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