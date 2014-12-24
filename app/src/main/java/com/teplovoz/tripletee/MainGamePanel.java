package com.teplovoz.tripletee;

import com.teplovoz.tripletee.model.ElaineAnimated;
import com.teplovoz.tripletee.model.Droid;
import com.teplovoz.tripletee.model.components.Speed;
import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainGamePanel extends SurfaceView implements
        SurfaceHolder.Callback {

    private MainThread thread;
    private ElaineAnimated elaine;
    private Droid droid;
    private enum GameState { INIT, MENU, START, PLAY, FINISH, EXIT}
    private GameState state;
    private float loading;
    private Paint paintButton, paintText;
    private RectF buttonStart, buttonExit, buttonMenu;

    // the fps to be displayed
    private String avgFps;
    public void setAvgFps(String avgFps) {
        this.avgFps = avgFps;
    }

    public MainGamePanel(Context context) {
        super(context);
        Log.d("MYLOG", "MainGamePanel.Initialization");
        // adding the callback (this) to the surface holder to intercept events
        getHolder().addCallback(this);

        // create Elaine and load bitmap
        elaine = new ElaineAnimated(
                BitmapFactory.decodeResource(getResources(), R.drawable.walk_elaine)
                , 10, 50	// initial position
                , 30, 47	// width and height of sprite
                , 5, 5);	// FPS and number of frames in the animation

        // create droid and load bitmap
        droid = new Droid(BitmapFactory.decodeResource(getResources(), R.drawable.droid_1), 50, 50);

        // Graphic elements
        paintButton = new Paint();
        paintButton.setColor(Color.GREEN);
        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(60);
        paintText.setStrokeWidth(3);

        // create the game loop thread
        state = GameState.INIT;
        loading = 0;
        thread = new MainThread(getHolder(), this);

        // make the GamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d("MYLOG", "MainGamePanel.surfaceChange");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("MYLOG", "MainGamePanel.surfaceCreated");
        buttonStart = new RectF(getWidth()*0.2f,getHeight()*0.2f,getWidth()*0.8f,getHeight()*0.4f);
        buttonExit  = new RectF(getWidth()*0.2f,getHeight()*0.6f,getWidth()*0.8f,getHeight()*0.8f);
        buttonMenu  = new RectF(0,getHeight()*0.8f,getWidth(),getHeight());
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("MYLOG", "MainGamePanel.surfaceDestoyed");
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int mousex = (int)event.getX();
        int mousey = (int)event.getY();
        switch(state){
            case MENU:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (buttonStart.left < mousex && mousex < buttonStart.right &&
                            buttonStart.top < mousey && mousey < buttonStart.bottom) {
                        state = GameState.PLAY;
                    }
                    if (buttonExit.left < mousex && mousex < buttonExit.right &&
                            buttonExit.top < mousey && mousey < buttonExit.bottom) {
                        thread.setRunning(false);
                        ((Activity)getContext()).finish();
                    }
                }
                break;
            case PLAY:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    droid.handleActionDown(mousex, mousey);
                    if (event.getY() > buttonMenu.top) {
                        state = GameState.MENU;
                    }
                } if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (droid.isTouched()) {
                    droid.setX(mousex);
                    droid.setY(mousey);
                }
            } if (event.getAction() == MotionEvent.ACTION_UP) {
                if (droid.isTouched())droid.setTouched(false);
            }
                break;
        }
        return true;
    }

    public void render(Canvas canvas) {
        switch(state){
            case INIT:
                canvas.drawColor(Color.rgb(0,0,Math.round(loading*255)));
                break;
            case MENU:
                canvas.drawColor(Color.RED);
                canvas.drawRect(buttonStart, paintButton);
                canvas.drawText("Start", buttonStart.left, buttonStart.bottom, paintText);
                canvas.drawRect(buttonExit, paintButton);
                canvas.drawText("Exit", buttonExit.left, buttonExit.bottom, paintText);
                break;
            case START:
                canvas.drawColor(Color.rgb(Math.round(loading*255),255,0));
                break;
            case PLAY:
                canvas.drawColor(Color.WHITE);
                elaine.draw(canvas);
                droid.draw(canvas);
                float w = getWidth();
                float o = (buttonMenu.top-w)/2;
                canvas.drawLine(w*1/3,o,w*1/3,o+w,paintText);
                canvas.drawLine(w*2/3,o,w*2/3,o+w,paintText);
                canvas.drawLine(0,w*1/3+o,w,w*1/3+o,paintText);
                canvas.drawLine(0,w*2/3+o,w,w*2/3+o,paintText);
                canvas.drawRect(buttonMenu, paintButton);
                canvas.drawText("Menu", buttonMenu.left, buttonMenu.bottom, paintText);
                break;
            case FINISH: case EXIT:
                canvas.drawColor(Color.BLACK);
                break;
        }
        displayFps(canvas, avgFps);
    }

    public void update() {
        switch (state) {
            case INIT: case START:
                loading += 0.01;
                if(loading >= 1) {
                    loading = 0;
                    state = (state==GameState.INIT) ? GameState.MENU : GameState.PLAY;
                }
                break;
            case MENU:
                break;
            case PLAY:
                elaine.update(System.currentTimeMillis());
                if (droid.getSpeed().getxDirection() == Speed.DIRECTION_RIGHT
                        && droid.getX() + droid.getBitmap().getWidth() / 2 >= getWidth()) {
                    droid.getSpeed().toggleXDirection();
                }
                if (droid.getSpeed().getxDirection() == Speed.DIRECTION_LEFT
                        && droid.getX() - droid.getBitmap().getWidth() / 2 <= 0) {
                    droid.getSpeed().toggleXDirection();
                }
                if (droid.getSpeed().getyDirection() == Speed.DIRECTION_DOWN
                        && droid.getY() + droid.getBitmap().getHeight() / 2 >= getHeight()) {
                    droid.getSpeed().toggleYDirection();
                }
                if (droid.getSpeed().getyDirection() == Speed.DIRECTION_UP
                        && droid.getY() - droid.getBitmap().getHeight() / 2 <= 0) {
                    droid.getSpeed().toggleYDirection();
                }
                droid.update();
                break;
        }
    }

    private void displayFps(Canvas canvas, String fps) {
        if (canvas != null && fps != null) {
            Paint paint = new Paint();
            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(fps, this.getWidth() - 50, 20, paint);
        }
    }

}