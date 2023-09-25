#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#include <ShiftRegister74HC595.h>
#include<ArduinoJson.h>

//Ports and Pin Config
#define ANALOG_PORT 17
#define MULTIPLEXER_S0 16
#define MULTIPLEXER_S1 5
#define MULTIPLEXER_S2 4
#define GAS_ALARM 0
#define TEMP_ALARM 2
#define SERIAL_DATA 12
#define SERIAL_CLOCK 13
#define SERIAL_LATCH 15

//WiFi Credintials
#define WIFI_SSID "SSID"
#define WIFI_PASSWORD "PASSWORD"

//Firebase Authentications
#define FIREBASE_HOST "YOUR_FIREBASE_HOST"
#define FIREBASE_AUTH "YOUR_FIREBASE_AUTH"

//Shift registor Library
ShiftRegister74HC595<1> sr(D6, D7, D8);

//Gloable Variables and constants
//Device Data and the status
String deviceName = "GasDetector";
bool deviceOn = true;

//Gas Values
int currentGasValue = 0;
int minGasValue = 0;
int gasPotentioPercent = 0;

//Temp Values
int currentTempValue = 0;
int minTempValue = 0;
int tempPotentioPercent = 0;
float pad = 8000;   

//LED indicator
uint8_t  ledStatus[8] = { 0, 0, 0, 0, 0, 0, 0, 0};
uint8_t shiftRegistorArray[] = { 0 };

void setup() {
  Serial.begin(9600);

  //set the pin modes and configurations
  pinMode(MULTIPLEXER_S0, OUTPUT);
  pinMode(MULTIPLEXER_S1, OUTPUT);
  pinMode(MULTIPLEXER_S2, OUTPUT);
  pinMode(GAS_ALARM, OUTPUT);
  pinMode(TEMP_ALARM, OUTPUT);
  digitalWrite(GAS_ALARM, HIGH);
  digitalWrite(TEMP_ALARM, HIGH);
  
  //LED Indicator -- Power LED
  ledStatus[0] = HIGH;
  showLedStatus();
  
  //Connect to the WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    //LED Indicator -- WiFi && Firebase LED    
    ledStatus[1] = HIGH;
    showLedStatus(); 
    
    Serial.print(".");
    delay(250);
    
    //LED indicator  
    ledStatus[1] = LOW;
    showLedStatus(); 
    delay(250);
  }
  
  //LED Indicator -- Power LED
  ledStatus[1] = HIGH; 
  showLedStatus();
  
  Serial.println();
  Serial.println("Connected.");
  Serial.println(WiFi.localIP());

  //Generate the device name
  String deviceMac = WiFi.macAddress();
  deviceName += deviceMac[10] + deviceMac[11] + deviceMac[13] + deviceMac[14] + deviceMac[16] + deviceMac[17];
  Serial.println(deviceName);
    
  //Connect to the Firebase
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  checkFirebaseConnection();
  
  //Testing firebase connection
  Firebase.remove("testdb");
  Firebase.setString("testdb", "testdb");
  if(Firebase.getString("testdb").equals("testdb")){
    Serial.println();
    Serial.println("Firebase Authentication Sucessfully..!");
    Serial.println();
  }

  //Device Status
  //ON off status of the database   
  deviceOn = Firebase.getBool("/" + deviceName + "/STATUS/DEVICE_ON");
  if(deviceOn == NULL){
    deviceOn = true;
    Firebase.setBool("/" + deviceName + "/STATUS/DEVICE_ON", deviceOn);
  }
  
  //Calibration part when the device is turn on
  //Get the minmum value of the gas sensor 
  //Take the mean of the 50 values in 2.5 second
  selectMultiplexerDevice(0, 0, 0);
  unsigned tempGasValue = 0;
  for (int i = 0; i < 50; i++) {      
      tempGasValue += analogRead(ANALOG_PORT);
      delay(50);    
  }
  minGasValue = tempGasValue / 50;
  minGasValue = convertMQ2Value(minGasValue);  
  Serial.print("Minimum Gas Value : ");  
  Serial.println(minGasValue);
  currentGasValue = minGasValue;    
  
  //Get the minmum value of the themistor sensor 
  //Take the mean of the 50 values in 2.5 second
  selectMultiplexerDevice(1, 0, 0);
  unsigned tempTempValue = 0;
  for (int i = 0; i < 50; i++) {
      tempTempValue += analogRead(ANALOG_PORT);
      delay(50);    
  }
  minTempValue = tempTempValue / 50;
  minTempValue = convertThermistorValue(minTempValue);
  Serial.print("Minimum Temp Value : ");  
  Serial.println(minTempValue);
  currentTempValue = minTempValue;    
}

void loop() { 
  //Check the device is On or Off  
  if(deviceOn){
    //Set the multiplexer to the A0 line 
    selectMultiplexerDevice(0, 0, 0);
    delay(100);
    //Get the value of the gas sensor value for 0.05 second
    currentGasValue = analogRead(ANALOG_PORT);
    
    //Get the temp value
    //Set the multiplexer to the A1 line
    selectMultiplexerDevice(1, 0, 0);
    delay(100);
    //Get the value of the gas sensor value for 0.05 second
    currentTempValue = analogRead(ANALOG_PORT);

    //Get the gas potentiometer value
    //Set the multiplexer to the A2 line
    selectMultiplexerDevice(0, 1, 0);  
    delay(100);
    gasPotentioPercent = map(analogRead(ANALOG_PORT), 0, 1024, 0, 100);

    //Get the Temp potentiometer values
    //Set the multiplexer to the A3 line
    selectMultiplexerDevice(1, 1, 0);
    delay(100);
    tempPotentioPercent = map(analogRead(ANALOG_PORT), 0, 1024, 0, 100);

    //Convert to the celcius
    currentGasValue = convertMQ2Value(currentGasValue);
    currentTempValue = convertThermistorValue(currentTempValue);
    
    //Get the percentage of increased of the sensors    
    float gasIncreasedPercentage = (float)currentGasValue / minGasValue;
    float tempIncreasedPercentgae = (float)currentTempValue / minTempValue;
    gasIncreasedPercentage = (gasIncreasedPercentage * 100) -100;
    tempIncreasedPercentgae = (tempIncreasedPercentgae * 100) - 100;
      
    ledStatus[2] = HIGH;
    ledStatus[5] = HIGH; 
    showLedStatus();
    
    //Trigger alarams when increasing the gas
    if(gasIncreasedPercentage > (gasPotentioPercent * 0.5)){
      Serial.print("Gas Dangerous ");
      Serial.println(gasIncreasedPercentage);
        
      ledStatus[3] = HIGH;    
    }else{
      ledStatus[3] = LOW;             
    }
    
    if(gasIncreasedPercentage > gasPotentioPercent){
      Serial.print("Gas Alarm Fired ");
      Serial.println(gasIncreasedPercentage);
      digitalWrite(GAS_ALARM, LOW);
          
      ledStatus[4] = HIGH;  
    }else{
      digitalWrite(GAS_ALARM, HIGH); 
      ledStatus[4] = LOW;
    }
    showLedStatus();      
    
    //Trigger alarams when increasing the temp
    if(tempIncreasedPercentgae > (tempPotentioPercent * 0.5)){
      Serial.print("Temp Dangerous ");
      Serial.println(tempIncreasedPercentgae);
        
      ledStatus[6] = HIGH;   
    }else{
      ledStatus[6] = LOW;      
    }
    
    if(tempIncreasedPercentgae > tempPotentioPercent){
      Serial.print("Temp Alarm Fired ");
      Serial.println(tempIncreasedPercentgae);
      digitalWrite(TEMP_ALARM, LOW);
      
      ledStatus[7] = HIGH; 
    }else{
      digitalWrite(TEMP_ALARM, HIGH);
      ledStatus[7] = LOW; 
    }
    showLedStatus();
    
    //Set All data to the Firebase
    //Gas Related Values
    StaticJsonBuffer<512> jsonBuffer;
    JsonObject& root = jsonBuffer.createObject();
    //Gas Related Values  
    root["CURRENT_GAS_VALUE"] = currentGasValue;
    root["MINIMUM_GAS_VALUE"] = minGasValue;
    root["SENSING_GAS_PERCENT"] = gasPotentioPercent;
    root["GAS_INCREASED_PERCENT"] = gasIncreasedPercentage;
    
    //Tempurature Related Values
    root["CURRENT_TEMP_VALUE"] = currentTempValue; 
    root["MINIMUM_TEMP_VALUE"] = minTempValue;
    root["SENSING_TEMP_PERCENT"] = tempPotentioPercent;
    root["TEMP_INCREASED_PERCENT"] = tempIncreasedPercentgae;
    
    //Set the data to the firebase
    Firebase.set(deviceName + "/DATA",root);
    //Check the power on off condition
    deviceOn = Firebase.getBool("/" + deviceName + "/STATUS/DEVICE_ON");
    
  } else {
      //LED Indicator -- When Device is Power Off by mobile App
      ledStatus[2] = HIGH;
      ledStatus[3] = HIGH;
      ledStatus[4] = HIGH;
      ledStatus[5] = HIGH;
      ledStatus[6] = HIGH;
      ledStatus[7] = HIGH;
      showLedStatus();
      delay(500);
      ledStatus[2] = LOW;
      ledStatus[3] = LOW;
      ledStatus[4] = LOW;
      ledStatus[5] = LOW;
      ledStatus[6] = LOW;
      ledStatus[7] = LOW;
      showLedStatus();
      delay(500);         
  }
       
  deviceOn = Firebase.getBool("/" + deviceName + "/STATUS/DEVICE_ON");
  
  //Check the WiFi is working or not  
  if(WiFi.status() != WL_CONNECTED){
    checkWiFiConnection();
    checkFirebaseConnection(); 
  }
  delay(200);
}

//Convert gas Value
float convertMQ2Value(int value){
  return value;
}

//Calculate the Tempurature
float convertThermistorValue(int value) {
  long resistance;
  float temp; 

  resistance = pad * ((1024.0 / value) - 1);
  temp = log(resistance); 
  temp = 1 / (0.001129148 + (0.000234125 * temp) + (0.0000000876741 * temp * temp * temp));
  temp = temp - 273.15; 
  return temp;                                      
}

//Aditional Function
void selectMultiplexerDevice(int S0, int S1, int S2){
  digitalWrite(MULTIPLEXER_S0, S0);
  digitalWrite(MULTIPLEXER_S1, S1);
  digitalWrite(MULTIPLEXER_S2, S2);    
}

//Check the WiFi connection
// If there is no WiFi
//Then Try to Reconnect
void checkWiFiConnection(){
  if(WiFi.status() != WL_CONNECTED){
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("ReConnecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
      //LED indicator    
      ledStatus[1] = HIGH; 
      showLedStatus();
      
      Serial.print(".");
      delay(250);
      
      //LED indicator  
      ledStatus[1] = LOW;
      showLedStatus();
      delay(250);
    }
    ledStatus[1] = LOW;
    showLedStatus();
  }
}

//Check the Firebase connection
// If there is no Firebase
//Then Try to Reconnect
void checkFirebaseConnection(){
  if(Firebase.success()){
    ledStatus[1] = HIGH; 
    showLedStatus();
  }else {
    while (!Firebase.success() || Firebase.failed()) {
      ////LED indicator
      ledStatus[1] = HIGH; 
      showLedStatus();
      
      Serial.println(Firebase.error());
      delay(1000);
      
      //LED indicator    
      ledStatus[1] = LOW; 
      showLedStatus();
      
      Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
      delay(1000);
    }
    ledStatus[1] = HIGH; 
    showLedStatus();
  }
}

//Power on LED by its index
//This method dosent delete the previous leds also
void showLedStatus(){
  int ledBinaryValue = 0;
  for (int i = 0; i < 8; i++) {
    ledBinaryValue += ledStatus[i] * pow(2,i);
  }
  shiftRegistorArray[0] = ledBinaryValue;
  sr.setAll(shiftRegistorArray);  
}
