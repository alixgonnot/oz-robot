package com.thesis.alix.robot_guide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;

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
    private WebSocket socket;
    TextToSpeech t1;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.robot_face);
        initRobot();

        // set content layout
        setContentView(R.layout.robot_face);

        // get face instance from layout
        mFace = findViewById(R.id.face_view);

        // init face in App singleton
        // display default face on the robot
        mFace.setAnimation(IPLRobotEmotionView.Emotion.USUAL_A);
        mFace.loop(true);
        mFace.playAnimation();

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.FRANCE);
                }
            }
        });

        client = new OkHttpClient();start();
        testMove();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Log.i("TOUCH SCREEN", "AAAAAAAAAAAH" );
        if (socket != null){
            JSONObject json = new JSONObject();
            try {
                json.put("sender", "robot");
                json.put("cat", "info");
                json.put("val", "2"); // 1 for "connected"

                socket.send(json.toString());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return true;
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
        soundProc = (SoundProcessing) robot.getCategory(Robot.CategorySoundProcessing);
        imageProc = (ImageProcessing) robot.getCategory(Robot.CategoryImageProcessing);
    }

    public Context getActivity() {
        return this;
    }

    public void testMove(){
        Log.i("flow", "testmove");
        hardwareCommon = (HardwareCommon) robot.getCategory(Robot.CategoryHardwareCommon);
        int seq1 = hardwareCommon.setCTRLPowerOn(HardwareCommon.PowerAll, new Robot.OnResponseListener(){
            @Override
            public int onResponse(int seq, String result){
                return 0;
            }
        });
        int seq = hardwareCommon.setMovement(100,0);

        int seq2 = hardwareCommon.setCTRLPowerOff(HardwareCommon.PowerAll, new Robot.OnResponseListener(){
            @Override
            public int onResponse(int seq, String result){
                return 0;
            }
        });
    }


    /******************WEBSOCKET RELATED METHODS*******************************************************/

    private final class EchoWebSocketListener extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {

            socket = webSocket;

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
                Log.i("MESSAGE_RECEIVED",text);

            } catch (JSONException e) {
                Log.e("JSON", "Could not parse malformed JSON: \"" + text + "\"");
            }

            try {

                if (msg.getString("sender").equals("web_client")){

                    String cat = msg.getString("cat");

                    // demande de changement de mode
                    if (cat.equals("mode"))
                        changeMode(msg.getString("val"));

                    // demande d'execution de commande
                    else if(cat.equals("command") || cat.equals("evaluation") || cat.equals("correction")){

                        performCommand(cat, msg.getString("val"));
                        Log.i("COMMAND", cat + " - " + msg.getString("val"));
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

    private void performCommand(String cat, String val) {

        final String info = selectSentence(cat, val);

        //Mode 1 : Text Only
        if (mode == 1){

           String temp = "";

            if (cat.equals("command"))
                temp = "Consigne";
            else if (cat.equals("evaluation"))
                temp = "Evaluation";
            else if (cat.equals("correction"))
                temp = "Correction";

            final String dialog_title = temp;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("Command mode 1", info);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setMessage(info)
                            .setTitle(dialog_title);

                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    // Hide after some seconds
                    final Handler handler  = new Handler();
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    };

                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            handler.removeCallbacks(runnable);
                        }
                    });

                    handler.postDelayed(runnable, 30000);

                }
            });
        }
        else if(mode == 2){
            Log.i("Command mode 2",info);
            t1.speak(info, TextToSpeech.QUEUE_FLUSH, null);
        }
        //Mode 3 : Voice and emotion
        else if(mode == 3){
            Log.i("Command mode 3",info);
            final IPLRobotEmotionView.Emotion emotion = selectEmotion(cat, val);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFace.setAnimation(emotion);
                    mFace.playAnimation();
                }
            });
            t1.speak(info, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

    private IPLRobotEmotionView.Emotion selectEmotion(String cat, String val) {
        if ((cat.equals("evaluation"))&& (val.equals("1") || val.equals("4"))){
            Log.i("emotion change", "happy");
            return IPLRobotEmotionView.Emotion.SATISFACTION;
        }
        else if ((cat.equals("evaluation"))&& (val.equals("2") || val.equals("3"))){
            Log.i("emotion change", "sad");
            return IPLRobotEmotionView.Emotion.GLOOM;
        }
        else if (cat.equals("evaluation")){
            Log.i("emotion change", "sad");
            return IPLRobotEmotionView.Emotion.GLOOM;
        }
        Log.i("emotion change", "neutral");
        return IPLRobotEmotionView.Emotion.USUAL_A;
    }

    private String selectSentence(String cat, String val) {

        String result = "";

        if (cat.equals("command")){

            switch (val) {
                case "1":  result = getResources().getString(R.string.command_1);
                    break;
                case "2":  result = getResources().getString(R.string.command_2);
                    break;
                case "3":  result = getResources().getString(R.string.command_3);
                    break;
                case "4":  result = getResources().getString(R.string.command_4);
                    break;
                case "5":  result = getResources().getString(R.string.command_5);
                    break;
                case "6":  result = getResources().getString(R.string.command_6);
                    break;
                case "7":  result = getResources().getString(R.string.command_7);
                    break;
                case "8":  result = getResources().getString(R.string.command_8);
                    break;
                default:
                    break;
            }

        }
        else if (cat.equals("evaluation")){

            switch (val) {
                case "1":  result = getResources().getString(R.string.evaluation_1);
                    break;
                case "2":  result = getResources().getString(R.string.evaluation_2);
                    break;
                case "3":  result = getResources().getString(R.string.evaluation_3);
                    break;
                case "4":  result = getResources().getString(R.string.evaluation_4);
                    break;
                default:
                    result = val;
                    break;
            }

        }
        else if (cat.equals("correction")){

            switch (val) {
                case "1":  result = getResources().getString(R.string.correction_1);
                    break;
                case "2":  result = getResources().getString(R.string.correction_2);
                    break;
                default:
                    break;
            }

        }

        return result;
    }

    private void start() {

        // hot spot
        //Request request = new Request.Builder().url("ws://192.168.43.100:8080").build();

        //wifi local
       // Request request = new Request.Builder().url("ws://10.42.0.1:8080").build();

        //maison
        Request request = new Request.Builder().url("ws://192.168.0.13:8080").build();

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
