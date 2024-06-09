package com.example.freshsaver

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userEmail: EditText = findViewById(R.id.user_email_auth)
        val userPass: EditText = findViewById(R.id.user_pass_auth)
        val button: Button = findViewById(R.id.button_signin)
        val imageToGoogle: ImageView = findViewById(R.id.image_google_auth)
        val linkToReg: TextView = findViewById(R.id.link_to_reg)

        imageToGoogle.setOnClickListener{
            val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            startActivityForResult(googleSignInClient.signInIntent, 100)
        }

        linkToReg.setOnClickListener{
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if (email == "" || pass == "")
                Toast.makeText(this, "Not all fields are filled", Toast.LENGTH_LONG).show()
            else {
                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            userEmail.text.clear()
                            userPass.text.clear()

                            Toast.makeText(
                                baseContext,
                                "Authentication succeed.",
                                Toast.LENGTH_SHORT,
                            ).show()

                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                        } else {
                            Log.w(ContentValues.TAG, "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            val signInAccountTask: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            if (signInAccountTask.isSuccessful) {
                Toast.makeText(
                    baseContext,
                    "Google sign in successful",
                    Toast.LENGTH_SHORT,
                ).show()
                // Initialize sign in account
                try {
                    val googleSignInAccount = signInAccountTask.getResult(ApiException::class.java)
                    if (googleSignInAccount != null) {
                        val authCredential: AuthCredential = GoogleAuthProvider.getCredential(
                            googleSignInAccount.idToken, null
                        )
                        FirebaseAuth.getInstance().signInWithCredential(authCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    startActivity(
                                        Intent(
                                            this,
                                            HomeActivity::class.java
                                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    Toast.makeText(
                                        baseContext,
                                        "Authentication Failed: " + task.exception?.message,
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    baseContext,
                    "Authorization failed: " + signInAccountTask.exception?.message,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}