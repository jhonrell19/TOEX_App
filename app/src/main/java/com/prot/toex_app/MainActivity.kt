package com.prot.toex_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prot.toex_app.ui.theme.TOEX_AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isLoggedIn = checkIfUserIsLoggedIn() // Implement this function

        if (isLoggedIn) {
            startActivity(Intent(this, MapActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish() // Finish MainActivity so user can't go back to it
        setContent {
            TOEX_AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    private fun checkIfUserIsLoggedIn(): Boolean {
        // TODO: Implement your actual login status check here.
        // Example using SharedPreferences:
        // val prefs = getSharedPreferences("YourAppPrefsName", MODE_PRIVATE)
        // return prefs.getBoolean("USER_IS_LOGGED_IN", false)
        return false // Default to false to show LoginActivity first
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TOEX_AppTheme {
        Greeting("Android")
    }
}