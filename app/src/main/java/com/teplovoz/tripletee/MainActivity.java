package com.teplovoz.tripletee;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private MainGamePanel panel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MYLOG","MainActivity.OnCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        panel = new MainGamePanel(this);
        setContentView(panel);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        panel.saveState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        panel.restoreState(savedInstanceState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("MYLOG","MainActivity.OnRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MYLOG","MainActivity.OnStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MYLOG","MainActivity.OnResume");
        panel.startPlaying();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MYLOG","MainActivity.OnPause");
        panel.stopPlaying();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MYLOG","MainActivity.OnStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MYLOG","MainActivity.OnDestroy");
    }

}
