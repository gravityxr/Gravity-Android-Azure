package com.gravityxr.gravity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    enum class LaunchMode {
        Admin,
        User
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.admin -> {
                //Launching the admin mode to create / edit anchors
                val intent = Intent(this@MainActivity, GravityExperienceActivity::class.java);
                intent.putExtra(
                    GravityExperienceActivity.Companion.KEY_LAUNCH_MODE,
                    LaunchMode.Admin
                );
                startActivity(intent);
            }
            R.id.user -> {
                //launching the user mode to view anchors.
                val intent = Intent(this@MainActivity, GravityExperienceActivity::class.java);
                intent.putExtra(
                    GravityExperienceActivity.Companion.KEY_LAUNCH_MODE,
                    LaunchMode.User
                );
                startActivity(intent);
            }
        }
    }
}
