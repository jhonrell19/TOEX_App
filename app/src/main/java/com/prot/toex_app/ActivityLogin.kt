package com.prot.toex_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.prot.toex_app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSignIn.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // --- SIMPLE HARDCODED VALIDATION (FOR "NO DATABASE" TESTING) ---
                // Replace "user" and "password123" with what you want to test with
                if (username == "user" && password == "password123") {
                    Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()

                    // Navigate to MapActivity ONLY on successful validation
                    val intent = Intent(this, MapActivity::class.java)
                    // Clear previous tasks and make MapActivity the new root
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Finish LoginActivity so user can't go back to it
                } else {
                    // Invalid credentials
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Fields are empty
                Toast.makeText(this, getString(R.string.enter_username_password), Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewForgotPassword.setOnClickListener {
            // TODO: Implement your forgot password logic
            Toast.makeText(this, getString(R.string.forgot_password_clicked), Toast.LENGTH_SHORT).show()
        }

        // You have two listeners for textViewSignUp. The second one will override the first.
        // Let's keep the one that navigates.
        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Do NOT finish LoginActivity here, so user can go back to it from SignUp screen
        }

        // DO NOT have unconditional navigation to MapActivity here.
        // The navigation should ONLY happen inside the buttonSignIn click listener
        // after successful validation.

    } // End of onCreate
}