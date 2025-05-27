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
                // TODO: Implement your actual login authentication logic here
                // For example, call an API, check against a local database, etc.
                Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()


            } else {
                Toast.makeText(this, getString(R.string.enter_username_password), Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewForgotPassword.setOnClickListener {
            // TODO: Implement your forgot password logic
            // For example, navigate to a Forgot Password screen or show a dialog
            Toast.makeText(this, getString(R.string.forgot_password_clicked), Toast.LENGTH_SHORT).show()
        }

        binding.textViewSignUp.setOnClickListener {
            // TODO: Implement your sign-up logic
            // For example, navigate to a Sign Up screen
            Toast.makeText(this, getString(R.string.sign_up_clicked), Toast.LENGTH_SHORT).show()
        }
        binding.textViewSignUp.setOnClickListener {
            // Toast.makeText(this, getString(R.string.sign_up_clicked), Toast.LENGTH_SHORT).show() // Remove or keep for debugging
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Do NOT finish LoginActivity here, so user can go back to it if they change their mind on SignUp screen
        }
        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }
}