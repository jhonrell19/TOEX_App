package com.prot.toex_app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.prot.toex_app.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonPerformSignUp.setOnClickListener {
            performSignUp()
        }

        binding.textViewGoToSignIn.setOnClickListener {
            // Go back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP // Clears back stack to Login
            startActivity(intent)
            finish() // Finish SignUpActivity
        }
    }

    private fun performSignUp() {
        val username = binding.editTextSignUpUsername.text.toString().trim()
        val email = binding.editTextSignUpEmail.text.toString().trim()
        val password = binding.editTextSignUpPassword.text.toString() // No trim for password
        val confirmPassword = binding.editTextSignUpConfirmPassword.text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fields_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email_format), Toast.LENGTH_SHORT).show()
            binding.editTextSignUpEmail.error = getString(R.string.invalid_email_format)
            binding.editTextSignUpEmail.requestFocus()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            binding.editTextSignUpConfirmPassword.error = getString(R.string.passwords_do_not_match)
            binding.editTextSignUpConfirmPassword.requestFocus()
            return
        }

        // --- TODO: Implement actual account creation logic here (e.g., API call, Firebase Auth) ---
        // For now, just show a success message and simulate going to login
        Toast.makeText(this, getString(R.string.signup_successful), Toast.LENGTH_LONG).show()

        // Example: After successful signup, navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        // You might want to pass the email/username back to pre-fill login fields
        // intent.putExtra("EMAIL", email)
        startActivity(intent)
        finish() // Finish SignUpActivity
    }
}