package com.shens.lpggasleakagedetector.designs

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.shens.lpggasleakagedetector.R
import java.util.*

class SplashScreen : AppCompatActivity() {
    //objects variables and constants
    private lateinit var splashCircularProgressbar : CircularProgressIndicator
    private var progress: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen);
        supportActionBar?.hide()

        splashCircularProgressbar = findViewById(R.id.loading_progress_bar)
        activateCircularProgressBar()
        //load the home screen activity
        Handler().postDelayed({
            val intent = Intent(this@SplashScreen, HomeScreen::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.splash_slide_in_right, R.anim.spalsh_slide_out_left)
        },2800)
    }

    private fun activateCircularProgressBar(){
        val timer = Timer()

        val task = object : TimerTask() {
            override fun run() {
                if(progress > 100){
                    timer.cancel()
                }
                splashCircularProgressbar.setProgressCompat(progress, true)
                progress += 2
            }
        }
        // Start the timer to tick every second
        timer.scheduleAtFixedRate(task, 0, 50)
    }
}