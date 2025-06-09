package kr.ac.jbnu.jun.mobileprojectgit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.jbnu.jun.mobileprojectgit.NicknameActivity // ✅ 이 줄 추가로 오류 해결

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NavController 연결
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        // 닉네임 확인 및 로그인
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            auth.signInAnonymously().addOnSuccessListener {
                checkNickname(it.user?.uid)
            }
        } else {
            checkNickname(user.uid)
        }
    }

    //닉네임 체크
    private fun checkNickname(userId: String?) {
        if (userId == null) {
            goToNickname()
            return
        }

        val docRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        docRef.get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.getString("nickname") != null) {
                Log.d("MainActivity", "닉네임 존재: ${doc.getString("nickname")}")
                // 닉네임이 이미 있으면 현재 Fragment UI가 자동 표시되므로 추가 작업 없음
            } else {
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
}
