package com.example.smartcarapp;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "SmartCarApp";
    private static final String LOCALHOST = "10.0.2.2";
    private static final String MQTT_SERVER = "tcp://" + LOCALHOST + ":1883";
    private static final int QOS = 1;
    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 240;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static MqttClient mMqttClient;
    private boolean isConnected = false;
    public static double meters;

    public ImageView mic;
    private TextView mSpeedometer;
    private ImageView mCameraView;
    private TextView displayInteger;
    private Button plus;
    private int minteger;
    private Button minus;
    private Button forward;
    private Button backward;
    private Button turnLeft;
    private Button turnRight;
    private Button stopCar;
    private TextView travelledDistance;
    private int counter = 0;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mMqttClient = new MqttClient(getContext(), MQTT_SERVER, TAG);


    }
    @Override
    public void onResume() {
        super.onResume();

        connectToMqttBroker();
    }

    @Override
    public void onPause() {
        super.onPause();

        mMqttClient.disconnect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.i(TAG, "Disconnected from broker");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            }
        });
    }

    private void connectToMqttBroker() {
        if (!isConnected) {
            mMqttClient.connect(TAG, "", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = true;
                    final String successfulConnection = "Connected to MQTT broker";
                    Log.i(TAG, successfulConnection);
                    //Toast.makeText(getContext(), successfulConnection, Toast.LENGTH_SHORT).show();
                    mMqttClient.subscribe("/smartcar/ultrasound/front", QOS, null);
                    mMqttClient.subscribe("/smartcar/camera", QOS, null);
                    mMqttClient.subscribe("/smartcar/speedometer", QOS,null);
                    mMqttClient.subscribe("/smartcar/travelledDistance",QOS,null);
                    mMqttClient.subscribe("/smartcar/defaultSpeed",QOS,null);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    final String failedConnection = "Failed to connect to MQTT broker";
                    Log.e(TAG, failedConnection);
                    Toast.makeText(getContext(), failedConnection, Toast.LENGTH_SHORT).show();
                }
            }, new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    isConnected = false;

                    System.out.println("Connection to MQTT broker lost");
                    final String connectionLost = "Connection to MQTT broker lost";
                    Log.w(TAG, connectionLost);
                    //Toast.makeText(getContext(), connectionLost, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (topic.equals("/smartcar/camera")) {
                        final Bitmap bm = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                        final byte[] payload = message.getPayload();
                        final int[] colors = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
                        for (int ci = 0; ci < colors.length; ++ci) {
                            final int r = payload[3 * ci] & 0xFF;
                            final int g = payload[3 * ci + 1] & 0xFF;
                            final int b = payload[3 * ci + 2] & 0xFF;
                            colors[ci] = Color.rgb(r, g, b);
                        }
                        bm.setPixels(colors, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
                        mCameraView.setImageBitmap(bm);
                    }else if(topic.equals("/smartcar/speedometer")){
                        double value = Double.parseDouble(message.toString());
                        value = value * 100.0;
                        int temp = (int) value;
                        value = temp / 100.0;
                        mSpeedometer.setText(value + "\nm/s");

                    }else if(topic.equals("/smartcar/travelledDistance")) {
                        int value = UsefulFunctions.convertToInt(message);
                        System.out.println(value);
                        setMeters(value);
                        travelledDistance.setText("Travelled distance: "+ value + " m");
                    }else if(topic.equals("/smartcar/defaultSpeed" ) && counter==0) {
                        minteger = Integer.parseInt(message.toString());
                        display(minteger);
                        counter=1;

                    }else{
                        Log.i(TAG, "[MQTT] Topic: " + topic + " | Message: " + message.toString());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Message delivered");
                }
            });
        }
    }

    void drive(String topic,String message,String actionDescription){
        if (!isConnected) {
            final String notConnected = "Not connected (yet)";
            Log.e(TAG, notConnected);
            Toast.makeText(getContext(), notConnected, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, actionDescription);
        mMqttClient.publish(topic,message,QOS,null);
    }


    public void reverse(View view) {
        drive("myfirst/test","b","Moving backward");
    }

    public void turnRight(View view) {
        drive("myfirst/test","r","Turning right");
    }

    public void turnLeft(View view) {
        drive("myfirst/test","l","Turning left");
    }


    public void stopCar(View view) {
        drive("myfirst/test","s","Stopping");
    }

    public void plus(View view) {
        minteger = minteger + 10;
        if (minteger >= 100){
            minteger = 100;
        }
        display(minteger);

    }public void minus(View view) {
        minteger = minteger - 10;
        if(minteger <= 0){
            minteger = 0;
        }
        display(minteger);
    }

    private void display(int number) {
        //displayInteger.setText("" + number);
        mMqttClient.publish("smartcar/fspeed",Integer.toString(number),1,null);
        displayInteger.setText("" + number);
    }



    public void playAudio(int AudioFile){
        final MediaPlayer mp3 = MediaPlayer.create(getActivity(),AudioFile);
        CountDownTimer cntr_aCounter = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long l) {
                mp3.start();
            }

            @Override
            public void onFinish() {
                mp3.stop();
            }

        };cntr_aCounter.start();
    }

    private String message;
    public static int audioPath;

    public  void speechCommands(int requestCode, int resultCode, Intent data){
        if (requestCode==10 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String stringResult = result.get(0);
            message = SpeechToText.speechCommands(stringResult);


            CountDownTimer cntr_aCounter = new CountDownTimer(150, 1000) {

                @Override
                public void onTick(long l) {
                    //200 IQ if statement
                    if(audioPath!=5){
                        playAudio(audioPath);
                    }
                }

                @Override
                public void onFinish() {
                    if(!message.equals("")){
                        mMqttClient.publish("myfirst/test", message, 1, null);
                        int targetMeters = SpeechToText.metersProcessing(stringResult);
                        System.out.println(targetMeters);
                        if (targetMeters > 0) {
                            double currentMeters = meters;
                            double currentMeters2 = meters;
                        }
                    }
                }

            };cntr_aCounter.start();

        }

    }


    public void setMeters(double value){
        meters = value;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        speechCommands(requestCode,resultCode,data);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home,container,false);
        connectToMqttBroker();
        mCameraView = v.findViewById(R.id.imageView);
        mSpeedometer= v.findViewById(R.id.textView);
        travelledDistance = v.findViewById(R.id.travelledDistance);
        displayInteger =  v.findViewById(R.id.integer_number);
        plus = v.findViewById(R.id.plus);
        minus = v.findViewById(R.id.minus);
        forward = v.findViewById(R.id.forward);
        backward = v.findViewById(R.id.reverse);
        turnLeft = v.findViewById(R.id.turnLeft);
        turnRight = v.findViewById(R.id.turnRight);
        stopCar = v.findViewById(R.id.stopCar);
        mic = v.findViewById(R.id.speechMic);
        mic.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivityForResult(intent, 10);
            }
        });
        plus.setOnClickListener(this::plus);
        minus.setOnClickListener(this::minus);
        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        drive("myfirst/test","f","Moving forward");
                        return true;
                    case MotionEvent.ACTION_UP:

                        drive("myfirst/test","s","Stopping the car");
                        return true;
                }
                return false;
            }
        });
        backward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        drive("myfirst/test","b","Moving forward");
                        return true;
                    case MotionEvent.ACTION_UP:

                        drive("myfirst/test","s","Stopping the car");
                        return true;
                }
                return false;
            }
        });
        turnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        drive("myfirst/test","l","Moving forward");
                        return true;
                    case MotionEvent.ACTION_UP:

                        drive("myfirst/test","s","Stopping the car");
                        return true;
                }
                return false;
            }
        });
        turnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        drive("myfirst/test","r","Moving forward");
                        return true;
                    case MotionEvent.ACTION_UP:

                        drive("myfirst/test","s","Stopping the car");
                        return true;
                }
                return false;
            }
        });
        stopCar.setOnClickListener(this::stopCar);
        return v;
    }

}