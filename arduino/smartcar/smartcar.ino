#include <vector>
#include <string>
#include <Smartcar.h>
#include <MQTT.h>
#include <WiFi.h>
#ifdef __SMCE__
#include <OV767X.h>
#endif

WiFiClient net;

MQTTClient mqtt;

char ssid[] = " ";
char pass[] = " ";

const auto oneSecond = 1000UL;
const int TRIGGER_PIN           = 6; // D6
const int ECHO_PIN              = 7; // D7
const unsigned int MAX_DISTANCE = 300; //set the distance to 300
int fSpeed = 70;  // 70% of the full speed forward
int bSpeed   = -70; // 70% of the full speed backward
const int lDegrees = -75; // degrees to turn left
const int rDegrees = 75;  // degrees to turn right
bool flag = false;

ArduinoRuntime arduinoRuntime;
BrushedMotor leftMotor(arduinoRuntime, smartcarlib::pins::v2::leftMotorPins);
BrushedMotor rightMotor(arduinoRuntime, smartcarlib::pins::v2::rightMotorPins);
DifferentialControl control(leftMotor, rightMotor);
unsigned int pulsesPerMeter = 600;
 
DirectionlessOdometer leftOdometer{ arduinoRuntime,
                                    smartcarlib::pins::v2::leftOdometerPin,
                                    []() { leftOdometer.update(); },
                                    pulsesPerMeter };
DirectionlessOdometer rightOdometer{ arduinoRuntime,
                                     smartcarlib::pins::v2::rightOdometerPin,
                                     []() { rightOdometer.update(); },
                                     pulsesPerMeter };
 
DistanceCar car(arduinoRuntime,control, leftOdometer, rightOdometer);
SR04 front(arduinoRuntime, TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);
std::vector<char> frameBuffer;

void autoStop(String message){
  const auto distance = front.getDistance();
  if(distance>0 and distance<110 and !flag){
    car.setSpeed(0);
   } 
  if(message == "l"){
    flag == false;
    car.setSpeed(fSpeed);
    car.setAngle(lDegrees);  
  }else if(message == "r"){
    flag = false;
    car.setSpeed(fSpeed);
    car.setAngle(rDegrees);
  }else if(message == "f"){
    flag = false;
    car.setSpeed(fSpeed);
    car.setAngle(0);
  } else if(message == "b"){
    flag = true;
    car.setSpeed(bSpeed);
    car.setAngle(0);
  }else if(message == "s"){
    flag = false;
    car.setSpeed(0);
    car.setAngle(0);
  }
}
void setCarSpeed(){
  
}
const auto counter = 0;
const auto mqttBrokerUrl = "127.0.0.1";

void setup()
{
    Serial.begin(9600);
   
  #ifdef __SMCE__
  Camera.begin(QVGA, RGB888, 30);
  frameBuffer.resize(Camera.width() * Camera.height() * Camera.bytesPerPixel());
  #endif
  
  WiFi.begin(ssid, pass); 
  mqtt.begin(mqttBrokerUrl, 1883, net);
   

Serial.println("Connecting to MQTT broker");
  while (!mqtt.connect("arduino", "public", "public")) {
    Serial.print(".");
    delay(1000);
  }
  mqtt.subscribe("myfirst/test", 1);
  mqtt.subscribe("smartcar/fspeed", 1);
  mqtt.onMessage([](String topic, String message){
    
    if(topic=="myfirst/test"){
      autoStop(message);
    }else if(topic=="smartcar/fspeed"){
       fSpeed = message.toInt();
       if(car.getSpeed()>0){
       car.setSpeed(fSpeed);
       }
    }
  });
}
  

void loop()
{
    if (mqtt.connected()) {
    mqtt.loop();
    autoStop("");
    const auto currentTime = millis();
#ifdef __SMCE__
    static auto previousFrame = 0UL;
    if (currentTime - previousFrame >= 33) {
      previousFrame = currentTime;
      Camera.readFrame(frameBuffer.data());
      mqtt.publish("/smartcar/camera", frameBuffer.data(), frameBuffer.size(),
                   false, 0);
      const auto avgOdometerSpeed = String(car.getSpeed());             
      const auto defaultSpeed = String(fSpeed);
      const auto travelledDistance = String(car.getDistance());
      mqtt.publish("/smartcar/speedometer",  avgOdometerSpeed);
      mqtt.publish("/smartcar/defaultSpeed", defaultSpeed);
      mqtt.publish( "/smartcar/travelledDistance", travelledDistance);             
    }
#endif
    static auto previousTransmission = 0UL;
    if (currentTime - previousTransmission >= oneSecond) {
      previousTransmission = currentTime;
      const auto distance = String(front.getDistance());
      mqtt.publish("/smartcar/ultrasound/front", distance);
    }
   }
    Serial.println(front.getDistance());
    delay(100);
}
