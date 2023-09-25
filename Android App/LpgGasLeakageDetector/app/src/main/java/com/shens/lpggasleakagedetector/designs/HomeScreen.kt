package com.shens.lpggasleakagedetector.designs

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shens.lpggasleakagedetector.R
import org.json.JSONObject

class HomeScreen : AppCompatActivity() {

    //Variable objects and constants
    lateinit var firebaseInstance : FirebaseDatabase;
    val channel_id = "firedetector";

    var isGasWarningNotificationShown  = false;
    var isTempWarningNotificationShown  = false;
    var isGasAlertNotificationShown  = false;
    var isTempAlertNotificationShown  = false;

    //Components
    lateinit var deviceNameDropDown : TextInputLayout
    lateinit var deviceNameDropDownData : AutoCompleteTextView
    lateinit var currentGasLevelText : TextView
    lateinit var minGasLevelText : TextView
    lateinit var alarmActivatedGasPercentText : TextView
    lateinit var currentTempLevelText : TextView
    lateinit var minTempLevelText : TextView
    lateinit var  tempLevelAlarmActivatedText : TextView
    lateinit var gasIncreasedPercentage : TextView
    lateinit var tempIncreasedPercentage : TextView
    lateinit var gasIncreasedProgressbar : CircularProgressIndicator
    lateinit var tempIncreasedProgressbar : CircularProgressIndicator
    lateinit var deviceOnOffSwitch : MaterialSwitch
    lateinit var gasAlarmBtn : MaterialButton
    lateinit var tempAlarmBtn : MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        //Layout initializing
        deviceNameDropDownData = findViewById(R.id.device_name_dropdown_data);
        deviceNameDropDown = findViewById(R.id.device_name_dropdown);
        currentGasLevelText = findViewById(R.id.gas_level_txt);
        minGasLevelText = findViewById(R.id.min_gas_level_txt);
        alarmActivatedGasPercentText = findViewById(R.id.alarm_activate_gas_percent_txt);
        currentTempLevelText = findViewById(R.id.temp_level_txt);
        minTempLevelText = findViewById(R.id.min_temp_level_txt);
        tempLevelAlarmActivatedText = findViewById(R.id.alarm_activate_temp_percent_txt);
        gasIncreasedPercentage = findViewById(R.id.gas_increased_percentage_txt);
        tempIncreasedPercentage = findViewById(R.id.temp_increased_percentage_txt);
        gasIncreasedProgressbar = findViewById(R.id.gas_increased_progressbar);
        tempIncreasedProgressbar = findViewById(R.id.temp_increased_progressbar);
        deviceOnOffSwitch = findViewById(R.id.device_on_off_switch);
        gasAlarmBtn = findViewById(R.id.gas_alarm_btn);
        tempAlarmBtn = findViewById(R.id.heat_alarm_btn);

        //Set the default device selector text
        deviceNameDropDown.editText?.setText("Device Name");

        //Notification Channel Registration
        createNotificationChannel();

        //set the devices names
        firebaseInstance = FirebaseDatabase.getInstance();
        firebaseInstance.getReference("devices_name").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Take the devices names from the firebase
                var jsonDeviceNames = JSONObject(snapshot.value.toString());
                var deviceNamesList : MutableList<String> = mutableListOf();
                val nameKeys: Iterator<String> = jsonDeviceNames.keys();
                while(nameKeys.hasNext()){
                    val name: String = nameKeys.next();
                    deviceNamesList.add(name);
                }
                var deviceNamesAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, deviceNamesList);
                deviceNameDropDownData.setAdapter(deviceNamesAdapter);
            }
            override fun onCancelled(error: DatabaseError) {

            }
        });

        //set the device function based on the selected device
        deviceNameDropDownData.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                onDeviceDataChanged(deviceNameDropDownData.adapter.getItem(p2).toString());
                onDeviceStatusChanged(deviceNameDropDownData.adapter.getItem(p2).toString());
                deviceOnOff(deviceNameDropDownData.adapter.getItem(p2).toString());
            }
        };
    }

    //Register notification channel function
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channel_id, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    //Device on off function
    private fun deviceOnOff(deviceName: String) {
        deviceOnOffSwitch.setOnCheckedChangeListener { compoundButton, checked ->
            if(checked){
                firebaseInstance.getReference(deviceName).child("STATUS").child("DEVICE_ON").setValue(true);
            }else{
                firebaseInstance.getReference(deviceName).child("STATUS").child("DEVICE_ON").setValue(false);
            }
        }
    }

    //On device status chnaged
    private fun onDeviceStatusChanged(deviceName: String) {
        //set the device is online and offline function
        firebaseInstance.getReference(deviceName).child("STATUS").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val deviceStatus = JSONObject(snapshot.value.toString());
                deviceOnOffSwitch.isChecked = deviceStatus.getBoolean("DEVICE_ON");
            }

            override fun onCancelled(error: DatabaseError) {

            }

        });
    }

    //On device data changed
    private fun onDeviceDataChanged(deviceName: String) {
        firebaseInstance.getReference(deviceName).child("DATA").addValueEventListener(object :
            ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {

                val deviceJsonData = JSONObject(snapshot.value.toString());
                //Taking the values from the device
                val currentGasLevel = deviceJsonData.getInt("CURRENT_GAS_VALUE");
                val minGasLevel = deviceJsonData.getInt("MINIMUM_GAS_VALUE");
                val alarmActivatedGasPercentage = deviceJsonData.getInt("SENSING_GAS_PERCENT");
                val currentTempLevel = deviceJsonData.getInt("CURRENT_TEMP_VALUE");
                val minTempLevel = deviceJsonData.getInt("MINIMUM_TEMP_VALUE");
                val alarmActivatedTempPercentage = deviceJsonData.getInt("SENSING_TEMP_PERCENT");
                val gasIncreasedPercent = deviceJsonData.getDouble("GAS_INCREASED_PERCENT");
                val tempIncreasedPercent = deviceJsonData.getDouble("TEMP_INCREASED_PERCENT");

                //Set the card data
                gasIncreasedProgressbar.setProgressCompat(gasIncreasedPercent.toInt(),true);
                tempIncreasedProgressbar.setProgressCompat(tempIncreasedPercent.toInt(),true);
                gasIncreasedPercentage.text = "$gasIncreasedPercent %";
                tempIncreasedPercentage.text = "$tempIncreasedPercent %";

                //Set the table data
                currentGasLevelText.text = currentGasLevel.toString();
                minGasLevelText.text = minGasLevel.toString();
                alarmActivatedGasPercentText.text = "$alarmActivatedGasPercentage %";
                currentTempLevelText.text = "$currentTempLevel°C";
                minTempLevelText.text = "$minTempLevel°C";
                tempLevelAlarmActivatedText.text = "$alarmActivatedTempPercentage %";

                //Calculation and trigger alarms
                //When gas level reaching the limit
                if(gasIncreasedPercent > alarmActivatedGasPercentage){
                    gasAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.dark_red));
                    gasAlarmBtn.icon = ContextCompat.getDrawable(applicationContext, R.drawable.alarm__on_icon);

                    //Notification
                    if(!isGasAlertNotificationShown){
                        var builder = NotificationCompat.Builder(applicationContext, channel_id)
                            .setSmallIcon(R.drawable.alarm__on_icon)
                            .setContentTitle("Gas Alarm Activated.!")
                            .setContentText("Attention..! Gas Level is $currentGasLevel. Gas level is reached the limit. Gas Alarm Activated.!")
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText("Attention..! Gas Level is $currentGasLevel. Gas level is reached the limit. Gas Alarm Activated.!"))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                        notificationManager.notify(1, builder.build());
                        isGasAlertNotificationShown = true;
                    }


                }else if(gasIncreasedPercent > (alarmActivatedGasPercentage * 0.5)){
                    gasAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.orange));

                    //Notification
                    if(!isGasWarningNotificationShown){
                        var builder = NotificationCompat.Builder(applicationContext, channel_id)
                            .setSmallIcon(R.drawable.alarm__on_icon)
                            .setContentTitle("Gas Warning.!")
                            .setContentText("Attention..! Gas Level is $currentGasLevel reached the half of limit.")
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText("Attention..! Gas Level is $currentGasLevel reached the half of limit."))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                        notificationManager.notify(2, builder.build());
                        isGasWarningNotificationShown = true;
                    }
                }else{
                    gasAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_green));
                    isGasWarningNotificationShown = false;
                    isGasAlertNotificationShown = false;
                }

                //When temp reaching the limit
                if(tempIncreasedPercent > alarmActivatedTempPercentage){
                    tempAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.dark_red));
                    tempAlarmBtn.icon = ContextCompat.getDrawable(applicationContext, R.drawable.alarm__on_icon);

                    //Notification
                    if(!isTempAlertNotificationShown){
                        var builder = NotificationCompat.Builder(applicationContext, channel_id)
                            .setSmallIcon(R.drawable.alarm__on_icon)
                            .setContentTitle("Heat Alarm Activated.!")
                            .setContentText("Attention..! Heat Level is $currentTempLevel. Heat level is reached the limit. Heat Alarm Activated.!")
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText("Attention..! Heat Level is $currentTempLevel. Heat level is reached the limit. Heat Alarm Activated.!"))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                        notificationManager.notify(3, builder.build());
                        isTempAlertNotificationShown = true;
                    }
                }else if(tempIncreasedPercent > (alarmActivatedTempPercentage * 0.5)){
                    tempAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.orange));

                    //Notification
                    if(!isTempWarningNotificationShown){
                        var builder = NotificationCompat.Builder(applicationContext, channel_id)
                            .setSmallIcon(R.drawable.alarm__on_icon)
                            .setContentTitle("Heat Warning.!")
                            .setContentText("Attention..! Heat Level is $currentTempLevel reached the half of limit.")
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText("Attention..! Heat Level is $currentTempLevel reached the half of limit."))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                        notificationManager.notify(2, builder.build());
                        isTempWarningNotificationShown = true;
                    }
                }else{
                    tempAlarmBtn.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_green));
                    isTempWarningNotificationShown = false;
                    isTempAlertNotificationShown = false;
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        });
    }
}


