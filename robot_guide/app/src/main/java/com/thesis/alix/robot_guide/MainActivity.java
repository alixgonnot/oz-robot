package com.thesis.alix.robot_guide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import ipl.darsapi.Robot;
import ipl.darsapi.category.Base;
import ipl.darsapi.category.HardwareCommon;
import ipl.darsapi.category.HardwareT1;
import ipl.darsapi.category.ImageProcessing;
import ipl.darsapi.category.SoundProcessing;
import global.ipl.daris.t1_local.iplcomponent.iplrobotemotion.IPLRobotEmotionView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

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
    private IPLRobotEmotionView mFace;
    private int mode = 0; // 0 = not initialized
                          // 1 = text only
                          // 2 = voice only
                          // 3 = voice + emotions
    private OkHttpClient client;
    TextToSpeech t1;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.robot_face);

        super.onCreate(savedInstanceState);

        // set content layout
        setContentView(R.layout.robot_face);

        // get face instance from layout
        mFace = findViewById(R.id.face_view);

        // init face in App singleton
        // display default face on the robot
        mFace.setAnimation(IPLRobotEmotionView.Emotion.USUAL_A);
        mFace.loop(true);
        mFace.playAnimation();

        initRobot();

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.FRANCE);
                }
            }
        });

        client = new OkHttpClient();
        start();
    }

    @Override
    public void onDestroy(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onDestroy();
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

    public Context getActivity() {
        return this;
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
            JSONObject msg = new JSONObject();
            try {
                msg = new JSONObject(text);
                Log.i("SERVER INFO",text);

            } catch (JSONException e) {
                Log.e("JSON", "Could not parse malformed JSON: \"" + text + "\"");
            }

            try {

                // traitement du message envoyé par la télécommande
                if (msg.getString("sender").equals("web_client")){

                    String cat = msg.getString("cat");

                    // demande de changement de modemsg.getString("val")
                    if (cat.equals("mode"))
                        changeMode(msg.getString("val"));

                    // demande d'execution de commande
                    else if(cat.equals("command")){

                        performCommand(msg.getString("val"));
                        Log.i("COMMAND", msg.getString("val"));
                    }

                    // autres messages provenant de la télécommande
                    else
                        Log.i("WEBCLIENT INFO",text);
                }
                // traitement des autres messages
                else{
                    Log.i("OTHER INFO",text);
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

        //Mode 1 : Text Only
        if (mode == 1){

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("Command mode 1", "TESTTEST");

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setMessage("Vous devez lire les fiches descriptives de chacun des téléphones puis donner une note pour chacun des critères.")
                            .setTitle("Etape 3");

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }
            });
        }
        else if(mode == 2){
            Log.i("Command mode 2","TESTTEST");
            t1.speak("Etape 3,Vous devez lire les fiches descriptives de chacun des téléphones puis donner une note pour chacun des critères !", TextToSpeech.QUEUE_FLUSH, null);
        }
        //Mode 3 : Voice and emotion
        else if(mode == 3){
            Log.i("Command mode 3","TESTTEST");
            mFace.setAnimation(IPLRobotEmotionView.Emotion.DELIGHT);
            mFace.loop(false);
            mFace.playAnimation();
            mp = MediaPlayer.create(this, R.raw.transcript);
            mp.start();
        }

    }

    private void start() {

        // hot spot
        //Request request = new Request.Builder().url("ws://192.168.43.100:8080").build();

        //wifi local
        Request request = new Request.Builder().url("ws://10.42.0.1:8080").build();

        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //output.setText(output.getText().toString() + "\n\n" + txt);
                Log.i("OUTPUT",txt);
            }
        });
    }
}
