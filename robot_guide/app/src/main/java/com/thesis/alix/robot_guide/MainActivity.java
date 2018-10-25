package com.thesis.alix.robot_guide;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import ipl.darsapi.Robot;
import ipl.darsapi.category.Base;
import ipl.darsapi.category.HardwareCommon;
import ipl.darsapi.category.HardwareT1;
import ipl.darsapi.category.ImageProcessing;
import ipl.darsapi.category.SoundProcessing;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
