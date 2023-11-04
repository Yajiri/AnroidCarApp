package com.example.smartcarapp;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.math.BigDecimal;

//This class has simple functions like truncation and stuff
public class UsefulFunctions {
    //takes the string, converts it into a double and then truncates it
    public static int convertToInt(MqttMessage result){
        double value = Double.parseDouble(result.toString());
        value = value/100;
        int temp = (int) value;
        return temp;
    }


}
