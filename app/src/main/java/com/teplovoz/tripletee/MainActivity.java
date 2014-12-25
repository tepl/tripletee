package com.teplovoz.tripletee;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private MainGamePanel panel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onResume() {
        super.onResume();
        panel.startPlaying();
    }

    @Override
    protected void onPause() {
        super.onPause();
        panel.stopPlaying();
    }

}
