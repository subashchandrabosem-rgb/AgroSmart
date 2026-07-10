package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.AgriAppMain
import com.example.viewmodel.AgriViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  private val viewModel: AgriViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    intent?.let { handleAuthLink(it) }

    setContent {
      AgriAppMain(viewModel)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAuthLink(intent)
  }

  private fun handleAuthLink(intent: Intent) {
    val data = intent.data ?: return
    val emailLink = data.toString()
    try {
      if (FirebaseAuth.getInstance().isSignInWithEmailLink(emailLink)) {
        var email = data.getQueryParameter("email") ?: ""
        if (email.isEmpty()) {
          val prefs = getSharedPreferences("agrosmart_prefs", MODE_PRIVATE)
          email = prefs.getString("auth_email", "") ?: ""
        }
        if (email.isNotEmpty()) {
          FirebaseAuth.getInstance().signInWithEmailLink(email, emailLink)
            .addOnCompleteListener { task ->
              if (task.isSuccessful) {
                Toast.makeText(this, "Email Link Verified successfully!", Toast.LENGTH_LONG).show()
                viewModel.handleFirebaseSignInSuccess(email)
              } else {
                val errorMsg = task.exception?.message ?: "Verification failed"
                Toast.makeText(this, "Verification error: $errorMsg", Toast.LENGTH_LONG).show()
              }
            }
        } else {
          Toast.makeText(this, "Failed to resolve email from sign-in link. Please enter it manually or try again.", Toast.LENGTH_LONG).show()
        }
      }
    } catch (e: Throwable) {
      e.printStackTrace()
      Toast.makeText(this, "Authentication Link Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }
}
