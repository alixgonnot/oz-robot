package com.thesis.alix.robot_guide;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import ipl.darsapi.Robot;
import ipl.darsapi.category.Base;
import ipl.darsapi.category.HardwareCommon;
import ipl.darsapi.category.HardwareT1;
import ipl.darsapi.category.ImageProcessing;
import ipl.darsapi.category.SoundProcessing;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private Robot robot = null;
    private int contentID = 1212547;
    private int appID = 1212547;

    private static String TAG = "IJINI-LOG";

    Base categoryBase;
    HardwareT1 hdw;
    HardwareCommon hardwareCommon;
    SoundProcessing soundProc;
    ImageProcessing imageProc;
    private int mode = 0; // 0 = not initialized
                          // 1 = text only
                          // 2 = voice only
                          // 3 = voice + emotions

    private Button start;
    private TextView output;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initRobot();

        client = new OkHttpClient();
        start();
    }

/**********************ROBOT RELATED METHODS*******************************************************/

    private void initRobot(){

        robot = Robot.getInstance(this);

        try{
            robot.initialize(contentID, appID);

            robot.connect(new Robot.OnConnectionEventListener() {
                @Override
                public int onRobotConnected() {
                    Log.d(TAG,"onRobotConnected");
                    initServices();
                    return 0;
                }

                @Override
                public int onRobotDisconnected(int reason) {
                    Log.d(TAG,"onRobotDisconnected reason : "+reason);
                    robot.uninitialize();

                    finishAffinity();

                    System.exit(0);

                    return 0;
                }

                @Override
                public int onRemoteConnected(String targetID) {
                    Log.d(TAG,"onRemoteConnected targetID : "+targetID);
                    return 0;
                }

                @Override
                public int onRemoteDisconnected(String targetID) {
                    Log.d(TAG,"onRemoteDisconnected targetID : "+targetID);
                    return 0;
                }
            });

        }catch(Exception e){
            Log.d(TAG, "Exception "+e);
        }

    }

    private void initServices() {
        categoryBase = (Base) robot.getCategory(Robot.CategoryBase);
        hdw = (HardwareT1) robot.getCategory(Robot.CategoryHardwareT1);
        hardwareCommon = (HardwareCommon) robot.getCategory(Robot.CategoryHardwareCommon);
        soundProc = (SoundProcessing) robot.getCategory(Robot.CategorySoundProcessing);
        imageProc = (ImageProcessing) robot.getCategory(Robot.CategoryImageProcessing);

       /* if(hardwareCommon != null){
            hardwareCommon.startTouch(new Robot.OnResponseListener() {
                @Override
                public int onResponse(int seq, String result) {
                    if(result.equals(Robot.ResultFail))
                        Toast.makeText(getActivity(),"startTouch result : "+result,Toast.LENGTH_SHORT).show();
                    return 0;
                }
            }, onTouchEventListener);
        }*/

       /* hardwareCommon.setCTRLPowerOn(HardwareCommon.PowerAll, new Robot.OnResponseListener() {
            @Override
            public int onResponse(int seq, String result) {
                // face reco
                startFaceDetection();
                return 0;
            }
        });*/

    }

/******************WEBSOCKET RELATED METHODS*******************************************************/

    private final class EchoWebSocketListener extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {


            JSONObject json = new JSONObject();
            try {
                json.put("sender", "robot");
                json.put("cat", "info");
                json.put("val", "1"); // 1 for "connected"

                webSocket.send(json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.i("SERVER_INFO","test");
            JSONObject msg = new JSONObject();
            try {

                msg = new JSONObject(text);
                Log.i("SERVER INFO",text);

            } catch (JSONException e) {
                Log.e("JSON", "Could not parse malformed JSON: \"" + text + "\"");
            }

            try {

                if (msg.getString("sender").equals("web_client")){

                    String cat = msg.getString("cat");

                    if (cat.equals("mode"))
                        changeMode(msg.getString("val"));
                    else if(cat.equals("command"))
                        performCommand(msg.getString("val"));
                    else
                        Log.i("SERVER INFO",text);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }

    private void changeMode(String val) {
        mode = Integer.parseInt(val);
        Log.i("ROBOT INFO", "Mode is now " + mode);
    }

    private void performCommand(String val) {
    }

    private void start() {
        Request request = new Request.Builder().url("ws://192.168.0.13:1337").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                output.setText(output.getText().toString() + "\n\n" + txt);
            }
        });
    }
}
