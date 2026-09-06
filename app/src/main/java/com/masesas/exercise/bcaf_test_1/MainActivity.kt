package com.masesas.exercise.bcaf_test_1

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.masesas.exercise.bcaf_test_1.databinding.ActivityMainBinding
import com.masesas.exercise.bcaf_test_1.presentation.compose.HomeActivityCompose
import com.masesas.exercise.bcaf_test_1.presentation.legacy.HomeActivityLegacy

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val LOGIN_KEY = "LOGIN_DATA"
        const val ADDITIONAL_LOGIN_DATA_KEY = "ADDITIONAL_LOGIN_DATA" // 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            btnHomeLegacy.setOnClickListener {
                startActivity(Intent(this@MainActivity, HomeActivityLegacy::class.java))
            }

            btnHomeCompose.setOnClickListener {
                startActivity(Intent(this@MainActivity, HomeActivityCompose::class.java))
            }


            btnLogin.setOnClickListener {
                /* val intent = Intent(
                     this@MainActivity,
                     SecondActivity::class.java
                 ).apply {
                     putExtra(
                         LOGIN_KEY, LoginRequest(
                             email = etEmail.text.toString(),
                             password = etPassword.text.toString()
                         )
                     )
                     putExtra(
                         ADDITIONAL_LOGIN_DATA_KEY,
                         1
                     )
                 }
                 startActivity(intent)*/

                /*val intent = Intent(this@MainActivity, MainActivityCompose::class.java).apply {
                    putExtra(
                        LOGIN_KEY, LoginRequest(
                            email = etEmail.text.toString(),
                            password = etPassword.text.toString()
                        )
                    )
                }
                startActivity(intent)*/
            }
        }
    }
}