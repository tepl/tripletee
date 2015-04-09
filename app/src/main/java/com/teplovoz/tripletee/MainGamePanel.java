package com.teplovoz.tripletee;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MainGamePanel extends SurfaceView implements
        SurfaceHolder.Callback {

    private MainThread thread;
    private ArrayList<Animation> animations = new ArrayList<Animation>();
    private boolean isReady;

    private enum GameState {INIT, MENU, PLAY, FINISH}

    private GameState state;
    private int[][] board = new int[3][3];
    private float loading;
    private Paint paintButton, paintText, paintGrid, paintCross, paintNought, paintFinish, paintTitle, paintAuthor, paintFPS;
    private RectF buttonStart, buttonExit, buttonMenu, boardRect, labelRect;
    private float sw, sh, fontFactor;  // screen width, height and fontFactor
    private float bw, bx, by, bs;       // board width, offsets and grid step
    private int player;              // player number, 1 or 2
    private int textd, textt;         // distance from the baseline to the center
    private boolean tie;

    // the fps to be displayed
    private String avgFps;

    public void setAvgFps(String avgFps) {
        this.avgFps = avgFps;
    }

    public MainGamePanel(Context context) {
        super(context);
        getHolder().addCallback(this);

        // Graphic elements
        paintButton = new Paint();
        paintButton.setColor(Color.GREEN);
        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintGrid = new Paint();
        paintGrid.setColor(Color.BLACK);
        paintCross = new Paint();
        paintCross.setColor(Color.RED);
        paintNought = new Paint();
        paintNought.setColor(Color.BLUE);
        paintNought.setStyle(Paint.Style.STROKE);
        paintFinish = new Paint();
        paintFinish.setColor(Color.CYAN);
        paintTitle = new Paint();
        paintTitle.setColor(Color.BLACK);
        paintTitle.setTextAlign(Paint.Align.CENTER);
        paintTitle.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paintAuthor = new Paint();
        paintAuthor.setColor(Color.BLACK);
        paintAuthor.setTextAlign(Paint.Align.CENTER);
        paintAuthor.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC));
        paintFPS = new Paint();
        paintFPS.setColor(Color.BLACK);

        state = GameState.INIT;
        loading = 0;
        setFocusable(true);
        isReady = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        sw = getWidth();
        sh = getHeight();
        buttonStart = new RectF(sw * .2f, sh * .25f, sw * .8f, sh * .45f);
        buttonExit = new RectF(sw * .2f, sh * .55f, sw * .8f, sh * .75f);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT || sw == sh) {
            bw = sw;
            bx = 0;
            by = (sh - bw) / 2;
            labelRect = new RectF(0, 0, sw, by);
            buttonMenu = new RectF(0, by + bw, sw, sh);
        } else {
            bw = sh;
            bx = sw - bw;
            by = 0;
            labelRect = new RectF(0, 0, bx, sh / 2);
            buttonMenu = new RectF(0, sh / 2, bx, sh);
        }
        bs = bw / 3;
        boardRect = new RectF(bx, by, bx + bw, by + bw);
        paintGrid.setStrokeWidth(bw / 50);
        paintCross.setStrokeWidth(bw / 20);
        paintNought.setStrokeWidth(bw / 20);
        fontFactor = Math.min(sw, sh) / 480;
        paintText.setTextSize(60 * fontFactor);
        paintTitle.setTextSize(80 * fontFactor);
        paintAuthor.setTextSize(20 * fontFactor);
        paintFPS.setTextSize(16 * fontFactor);
        textd = -(int) ((paintText.descent() + paintText.ascent()) / 2);
        textt = -(int) ((paintTitle.descent() + paintTitle.ascent()) / 2);
        isReady = true;
        startPlaying();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isReady = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public void startPlaying() {
        if (!isReady) return;
        thread = new MainThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    public void stopPlaying() {
        thread.setRunning(false);
    }

    static final String SAVE_STATE = "state";
    static final String SAVE_PLAYER = "player";
    static final String SAVE_TIE = "tie";
    static final String SAVE_ROW0 = "row0";
    static final String SAVE_ROW1 = "row1";
    static final String SAVE_ROW2 = "row2";

    public void saveState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(SAVE_STATE, state);
        savedInstanceState.putInt(SAVE_PLAYER, player);
        savedInstanceState.putBoolean(SAVE_TIE, tie);
        savedInstanceState.putIntArray(SAVE_ROW0, board[0]);
        savedInstanceState.putIntArray(SAVE_ROW1, board[1]);
        savedInstanceState.putIntArray(SAVE_ROW2, board[2]);
    }

    public void restoreState(Bundle savedInstanceState) {
        state = (GameState) savedInstanceState.getSerializable(SAVE_STATE);
        player = savedInstanceState.getInt(SAVE_PLAYER);
        tie = savedInstanceState.getBoolean(SAVE_TIE);
        board[0] = savedInstanceState.getIntArray(SAVE_ROW0);
        board[1] = savedInstanceState.getIntArray(SAVE_ROW1);
        board[2] = savedInstanceState.getIntArray(SAVE_ROW2);
    }

    public void InitGame() {
        for (int[] line : board) Arrays.fill(line, 0);
        animations.clear();
        player = 1;
        tie = false;
    }

    private boolean withinRect(int x, int y, RectF r) {
        return (r.left <= x && x <= r.right && r.top <= y && y <= r.bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (state) {
            case MENU:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (withinRect(x, y, buttonStart)) {
                        state = GameState.PLAY;
                        InitGame();
                    }
                    if (withinRect(x, y, buttonExit)) {
                        thread.setRunning(false);
                        ((Activity) getContext()).finish();
                    }
                }
                break;
            case PLAY:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (withinRect(x, y, buttonMenu)) {
                        state = GameState.MENU;
                    } else if (withinRect(x, y, boardRect)) {
                        int j = (int) ((x - bx) / bs);
                        int i = (int) ((y - by) / bs);
                        if (0 <= i && i <= 2 && 0 <= j && j <= 2) {
                            if (board[i][j] == 0) {
                                board[i][j] = player;
                                synchronized (animations) {
                                    animations.add(new Animation(BitmapFactory.decodeResource(getResources(), player == 1 ? R.drawable.cross : R.drawable.nought), (int) (bx + j * bs), (int) (by + i * bs), 5, 5, false));
                                }
                                if (board[i][(j + 1) % 3] == player && board[i][(j + 2) % 3] == player ||
                                        board[(i + 1) % 3][j] == player && board[(i + 2) % 3][j] == player ||
                                        i == j && board[(i + 1) % 3][(j + 1) % 3] == player && board[(i + 2) % 3][(j + 2) % 3] == player ||
                                        i == 2 - j && board[(i + 2) % 3][(j + 1) % 3] == player && board[(i + 1) % 3][(j + 2) % 3] == player)
                                    state = GameState.FINISH;
                                else {
                                    for (i = 0; i < 9; i++) if (board[i / 3][i % 3] == 0) break;
                                    if (i == 9) {
                                        tie = true;
                                        state = GameState.FINISH;
                                    } else player = 2 - (player + 1) % 2;
                                }
                            }
                        }
                    }
                }
                break;
            case FINISH:
                if (event.getAction() == MotionEvent.ACTION_DOWN) state = GameState.MENU;
                break;
        }
        return true;
    }

    public void render(Canvas canvas) {
        switch (state) {
            case INIT:
                canvas.drawColor(Color.rgb((int) (loading * 256), 0, 0));
                break;
            case MENU:
                canvas.drawColor(Color.RED);
                canvas.drawText("Triple Tee", sw / 2, sh * .125f + textt, paintTitle);
                canvas.drawRect(buttonStart, paintButton);
                canvas.drawText("Start", buttonStart.centerX(), buttonStart.centerY() + textd, paintText);
                canvas.drawRect(buttonExit, paintButton);
                canvas.drawText("Exit", buttonExit.centerX(), buttonExit.centerY() + textd, paintText);
                canvas.drawText("© 2015 Alexander Teplukhin", sw / 2, sh * .85f, paintAuthor);
                canvas.drawText("Version 1.2", sw / 2, sh * .925f, paintAuthor);
                break;
            case PLAY:
            case FINISH:
                canvas.drawColor(Color.WHITE);
                synchronized (animations) {
                    for (Animation animation : animations) {
                        animation.draw(canvas);
                    }
                }
                canvas.drawLine(bx + 1 * bs, by, bx + bs, by + bw, paintGrid);
                canvas.drawLine(bx + 2 * bs, by, bx + 2 * bs, by + bw, paintGrid);
                canvas.drawLine(bx, by + 1 * bs, bx + bw, by + 1 * bs, paintGrid);
                canvas.drawLine(bx, by + 2 * bs, bx + bw, by + 2 * bs, paintGrid);
                canvas.drawRect(buttonMenu, paintButton);
                canvas.drawText("Menu", buttonMenu.centerX(), buttonMenu.centerY() + textd, paintText);
                String label = "Player " + Integer.toString(player);
                if (bx > 0 && paintText.measureText(label) > bx)
                    label = "Plr " + Integer.toString(player);
                canvas.drawText(label, labelRect.centerX(), labelRect.centerY() + textd, paintText);
                if (state == GameState.FINISH) {
                    if (tie) label = "Tie!";
                    else label = "Player " + Integer.toString(player) + " won!";
                    float tw = paintText.measureText(label);
                    RectF finishRect = new RectF((sw - tw) / 2 - .1f * sw, sh * .4f, (sw + tw) / 2 + .1f * sw, sh * .6f);
                    canvas.drawRect(finishRect, paintFinish);
                    canvas.drawText(label, sw / 2, sh / 2 + textd, paintText);
                }
                break;
        }
        if (avgFps != null)
            canvas.drawText(avgFps, sw - 60 * fontFactor, 20 * fontFactor, paintFPS);
    }

    public void update() {
        synchronized (animations) {
            for (Animation animation : animations) {
                animation.update(System.currentTimeMillis());
            }
        }
        if (state == GameState.INIT) {
            loading += 0.01;
            if (loading >= 1) {
                loading = 0;
                state = GameState.MENU;
            }
        }
    }

}