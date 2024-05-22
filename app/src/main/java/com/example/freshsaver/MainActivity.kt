package com.example.freshsaver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonNext: Button = findViewById(R.id.button_next)

        buttonNext.setOnClickListener{
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {

                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, user.displayName ?: user.email, Toast.LENGTH_LONG).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                // User already is signed in

                DB.getInstance().getCategories()
                    .addOnSuccessListener { categories ->
                        categories.forEach { category ->
                            Log.d("category", category.id)
                            Log.d("category", category.title)
                            DB.getInstance().getProductTypesByCategory(category.id)
                                .addOnSuccessListener { pts ->
                                    pts.forEach {
                                        Log.d(category.title, it.title)
                                    }
                            }
                        }
                    }
            }
        }
    }
}