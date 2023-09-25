package com.shens.lpggasleakagedetector.designs

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.shens.lpggasleakagedetector.R
import org.json.JSONObject

class HomeScreen : AppCompatActivity() {

    //Constants variables and objects
    lateinit var  gasIncreasedPercentageTxt : TextView
    lateinit var firebaseInstance : FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)
        supportActionBar?.hide()

        //Find the objects from the resources
        gasIncreasedPercentageTxt = findViewById(R.id.gas_increased_percentage_txt);

        //Initialize the database and add listeners
        firebaseInstance = FirebaseDatabase.getInstance();
        firebaseInstance.getReference("GasDetector266").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        });

    }
}