package com.teplovoz.tripletee;

import com.teplovoz.tripletee.model.ElaineAnimated;
import com.teplovoz.tripletee.model.Droid;
import com.teplovoz.tripletee.model.components.Speed;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Arrays;

public class MainGamePanel extends SurfaceView implements
        SurfaceHolder.Callback {

    private MainThread thread;
    private ElaineAnimated elaine;
    private Droid droid;
    private boolean isReady;
    private enum GameState { INIT, MENU, START, PLAY, FINISH }
    private GameState state;
    private int[][] board = new int[3][3];
    private float loading;
    private Paint paintButton, paintText, paintShape;
    private RectF buttonStart, buttonExit, buttonMenu, boardRect,labelRect;
    private float sw,sh;        // screen width and height
    private float bw,bx,by,bs;  // board width, offsets and grid step
    private int player;         // player numeber, 1 or 2
    private int textd;         // distance from the baseline to the center

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
        paintText.setTextAlign(Paint.Align.CENTER);
        textd = -(int)((paintText.descent() + paintText.ascent()) / 2);
        paintShape = new Paint();
        paintShape.setColor(Color.BLACK);
        paintShape.setStyle(Paint.Style.STROKE);
        paintShape.setStrokeWidth(3);

        // create the game loop thread
        state = GameState.INIT;
        loading = 0;

        // make the GamePanel focusable so it can handle events
        setFocusable(true);
        isReady = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d("MYLOG", "MainGamePanel.surfaceChange");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("MYLOG", "MainGamePanel.surfaceCreated");
        sw = getWidth();
        sh = getHeight();
        buttonStart = new RectF(sw*0.2f,sh*0.2f,sw*0.8f,sh*0.4f);
        buttonExit  = new RectF(sw*0.2f,sh*0.6f,sw*0.8f,sh*0.8f);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT || sw == sh) {
            bw = sw;
            bx = 0;
            by = (sh - bw) / 2;
            labelRect =  new RectF(0, 0, sw, by);
            buttonMenu = new RectF(0, by+bw, sw, sh);
        }
        else{
            bw = sh;
            bx = sw - bw;
            by = 0;
            labelRect =  new RectF(0, 0, bx, sh / 2);
            buttonMenu = new RectF(0, sh / 2, bx, sh);
        }
        bs = bw / 3;
        boardRect = new RectF(bx, by, bx + bw, by + bw);
        isReady = true;
        startPlaying();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("MYLOG", "MainGamePanel.surfaceDestoyed");
        isReady = false;
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

    public void startPlaying(){
        if(!isReady)return;
        thread = new MainThread(getHolder(),this);
        thread.setRunning(true);
        thread.start();
    }

    public void stopPlaying(){
        thread.setRunning(false);
    }

    public void InitGame(){
        for (int[] line : board) Arrays.fill(line, 0);
        player = 1;
    }

    private boolean withinRect(int x, int y, RectF r){
        return (r.left <= x && x <= r.right && r.top <= y && y <= r.bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch(state){
            case MENU:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (withinRect(x,y,buttonStart)) {
                        state = GameState.PLAY;
                        InitGame();
                    }
                    if (withinRect(x,y,buttonExit)) {
                        thread.setRunning(false);
                        ((Activity)getContext()).finish();
                    }
                }
                break;
            case PLAY:
                switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    droid.handleActionDown(x, y);
                    if (withinRect(x,y,buttonMenu)) {
                        state = GameState.MENU;
                    }
                    else if(withinRect(x,y,boardRect)){
                        int j = (int)( (x - bx) / bs);
                        int i = (int)( (y - by) / bs);
                        if(0 <= i && i <= 2 && 0 <= j && j <= 2) {
                            if(board[i][j]==0){
                                board[i][j] = player;
                                if( board[i][(j+1)%3] == player && board[i][(j+2)%3] == player ||
                                    board[(i+1)%3][j] == player && board[(i+2)%3][j] == player ||
                                    i==j   && board[(i+1)%3][(j+1)%3] == player && board[(i+2)%3][(j+2)%3] == player ||
                                    i==2-j && board[(i+2)%3][(j+1)%3] == player && board[(i+1)%3][(j+2)%3] == player )
                                    state = GameState.FINISH;
                                else player = 2 - (player+1) % 2;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (droid.isTouched()) {
                        droid.setX(x);
                        droid.setY(y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (droid.isTouched()) droid.setTouched(false);
                    break;
                }
                break;
            case FINISH:
                if (event.getAction() == MotionEvent.ACTION_DOWN && withinRect(x,y,buttonMenu)) state = GameState.MENU;
                break;
        }
        return true;
    }

    public void render(Canvas canvas) {
        switch(state){
            case INIT:
                canvas.drawColor(Color.rgb((int)(loading*256),0,0));
                break;
            case MENU:
                canvas.drawColor(Color.RED);
                canvas.drawRect(buttonStart, paintButton);
                canvas.drawText("Start", buttonStart.centerX(), buttonStart.centerY()+textd, paintText);
                canvas.drawRect(buttonExit, paintButton);
                canvas.drawText("Exit", buttonExit.centerX(), buttonExit.centerY()+textd, paintText);
                break;
            case START:
                canvas.drawColor(Color.rgb((int)(loading*256),255,0));
                break;
            case PLAY:
            case FINISH:
                canvas.drawColor(Color.WHITE);
                elaine.draw(canvas);
                droid.draw(canvas);
                canvas.drawLine(bx+1*bs, by, bx+bs,   by+bw,   paintShape);
                canvas.drawLine(bx+2*bs, by, bx+2*bs, by+bw,   paintShape);
                canvas.drawLine(bx, by+1*bs, bx+bw,   by+1*bs, paintShape);
                canvas.drawLine(bx, by+2*bs, bx+bw,   by+2*bs, paintShape);
                for (int i=0;i<3;i++) for (int j=0;j<3;j++){
                    if(board[i][j]==1){
                        canvas.drawLine(bx+bs*j,by+bs*i,bx+bs*(j+1),by+bs*(i+1),paintShape);
                        canvas.drawLine(bx+bs*(j+1),by+bs*i,bx+bs*j,by+bs*(i+1),paintShape);
                    }
                    else if(board[i][j]==2){
                        canvas.drawCircle(bx+bs*(j+.5f),by+bs*(i+.5f),bs/2,paintShape);
                    }
                }
                canvas.drawRect(buttonMenu, paintButton);
                canvas.drawText("Menu", buttonMenu.centerX(), buttonMenu.centerY()+textd, paintText);
                canvas.drawText("Player " + Integer.toString(player), labelRect.centerX(), labelRect.centerY()+textd, paintText);
                if(state==GameState.FINISH){
                    canvas.drawText("Player " + Integer.toString(player) + " won!", sw/2, sh/2+textd, paintText);
                }
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
                        && droid.getX() + droid.getBitmap().getWidth() / 2 >= sw) {
                    droid.getSpeed().toggleXDirection();
                }
                if (droid.getSpeed().getxDirection() == Speed.DIRECTION_LEFT
                        && droid.getX() - droid.getBitmap().getWidth() / 2 <= 0) {
                    droid.getSpeed().toggleXDirection();
                }
                if (droid.getSpeed().getyDirection() == Speed.DIRECTION_DOWN
                        && droid.getY() + droid.getBitmap().getHeight() / 2 >= sh) {
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
            paint.setColor(Color.BLACK);
            canvas.drawText(fps, sw - 50, 20, paint);
        }
    }

}