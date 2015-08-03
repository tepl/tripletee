package com.teplovoz.tripletee;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Arrays;

public class MainGamePanel extends SurfaceView implements
        SurfaceHolder.Callback {

    private MainThread thread;
    private ArrayList<Animation> animations = new ArrayList<Animation>();
    private boolean isReady;

    private enum GameState {INIT, MENU, PLAY, FINISH}

    private GameState state;
    private int[][] board = new int[3][3];
    private float loading;
    private Paint paintText, paintGrid, paintFinish, paintAuthor, paintFPS;
    private Rect screenRect, boardRect, startRect, exitRect, menuRect, labelRect, titleRect;
    private Bitmap bitmapCross, bitmapNought, bitmapSplash, bitmapTitle;
    private GradientDrawable gradientScreen, gradientBoard;
    private NinePatchDrawable button;
    private Animation splash;
    private float fontFactor;
    private int sw, sh, bw, bx, by, bs;     // screen, board, offsets and grid step
    private int player;                     // player number, 1 or 2
    private int textd;                      // distance from the baseline to the center
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
        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintGrid = new Paint();
        paintGrid.setColor(Color.BLACK);
        paintFinish = new Paint();
        paintFinish.setColor(Color.CYAN);
        paintAuthor = new Paint();
        paintAuthor.setColor(Color.BLACK);
        paintAuthor.setTextAlign(Paint.Align.CENTER);
        paintAuthor.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC));
        paintFPS = new Paint();
        paintFPS.setColor(Color.BLACK);
        gradientScreen = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{Color.WHITE, Color.CYAN});
        gradientScreen.setShape(GradientDrawable.RECTANGLE);
        gradientScreen.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gradientBoard = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{Color.WHITE, Color.CYAN});
        gradientBoard.setShape(GradientDrawable.RECTANGLE);
        gradientBoard.setGradientType(GradientDrawable.RADIAL_GRADIENT);

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
        startRect = new Rect((int) (sw * .2f), (int) (sh * .25f), (int) (sw * .8f), (int) (sh * .45f));
        exitRect = new Rect((int) (sw * .2f), (int) (sh * .55f), (int) (sw * .8f), (int) (sh * .75f));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT || sw == sh) {
            bw = sw;
            bx = 0;
            by = (sh - bw) / 2;
            labelRect = new Rect(0, 0, sw, by);
            menuRect = new Rect(sw / 50, by + bw + sh / 50, sw - sw / 50, sh - sh / 50);
            titleRect = new Rect(0, (int) (sh * .25f - sw / 6) / 2, sw, (int) (sh * .25f + sw / 6) / 2);
        } else {
            bw = sh;
            bx = sw - bw;
            by = 0;
            labelRect = new Rect(0, 0, bx, sh / 2);
            menuRect = new Rect(sw / 50, sh / 2 + sh / 50, bx - sw / 50, sh - sh / 50);
            titleRect = new Rect((int) (sw - sh * .25f * 6) / 2, 0, (int) (sw + sh * .25f * 6) / 2, (int) (sh * .25f));
        }
        bs = bw / 3;

        if (bs <= 160) {
            bitmapCross = BitmapFactory.decodeResource(getResources(), R.drawable.cross160);
            bitmapNought = BitmapFactory.decodeResource(getResources(), R.drawable.nought160);
            bitmapSplash = BitmapFactory.decodeResource(getResources(), R.drawable.splash160);
            bitmapTitle = BitmapFactory.decodeResource(getResources(), R.drawable.title160);
            button = (NinePatchDrawable) getResources().getDrawable(R.drawable.button160);
        } else {
            bitmapCross = BitmapFactory.decodeResource(getResources(), R.drawable.cross320);
            bitmapNought = BitmapFactory.decodeResource(getResources(), R.drawable.nought320);
            bitmapSplash = BitmapFactory.decodeResource(getResources(), R.drawable.splash320);
            bitmapTitle = BitmapFactory.decodeResource(getResources(), R.drawable.title320);
            button = (NinePatchDrawable) getResources().getDrawable(R.drawable.button320);
        }
        splash = new Animation(bitmapSplash, (sw - bs) / 2, (sh - bs) / 2, bs, bs, 30, 30, false);

        boardRect = new Rect(bx, by, bx + bw, by + bw);
        screenRect = new Rect(0, 0, sw, sh);
        paintGrid.setStrokeWidth(bw / 50);
        fontFactor = Math.min(sw, sh) / 480f;
        paintText.setTextSize(60 * fontFactor);
        paintAuthor.setTextSize(20 * fontFactor);
        paintFPS.setTextSize(16 * fontFactor);
        textd = -(int) ((paintText.descent() + paintText.ascent()) / 2);
        gradientScreen.setGradientRadius(bw);
        gradientScreen.setBounds(screenRect);
        gradientBoard.setGradientRadius(bw);
        gradientBoard.setBounds(screenRect);
        gradientBoard.setGradientCenter((bx + bw / 2f) / sw, (by + bw / 2f) / sh);
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
    static final String SAVE_ANIMATIONS = "animations";

    public void saveState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(SAVE_STATE, state);
        savedInstanceState.putInt(SAVE_PLAYER, player);
        savedInstanceState.putBoolean(SAVE_TIE, tie);
        savedInstanceState.putIntArray(SAVE_ROW0, board[0]);
        savedInstanceState.putIntArray(SAVE_ROW1, board[1]);
        savedInstanceState.putIntArray(SAVE_ROW2, board[2]);
        savedInstanceState.putParcelableArrayList(SAVE_ANIMATIONS, animations);
    }

    public void restoreState(Bundle savedInstanceState) {
        state = (GameState) savedInstanceState.getSerializable(SAVE_STATE);
        player = savedInstanceState.getInt(SAVE_PLAYER);
        tie = savedInstanceState.getBoolean(SAVE_TIE);
        board[0] = savedInstanceState.getIntArray(SAVE_ROW0);
        board[1] = savedInstanceState.getIntArray(SAVE_ROW1);
        board[2] = savedInstanceState.getIntArray(SAVE_ROW2);
        animations = savedInstanceState.getParcelableArrayList(SAVE_ANIMATIONS);
    }

    public void InitGame() {
        for (int[] line : board) Arrays.fill(line, 0);
        animations.clear();
        player = 1;
        tie = false;
    }

    private boolean withinRect(int x, int y, Rect r) {
        return (r.left <= x && x <= r.right && r.top <= y && y <= r.bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (state) {
            case MENU:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (withinRect(x, y, startRect)) {
                        state = GameState.PLAY;
                        InitGame();
                    }
                    if (withinRect(x, y, exitRect)) {
                        thread.setRunning(false);
                        ((Activity) getContext()).finish();
                    }
                }
                break;
            case PLAY:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (withinRect(x, y, menuRect)) {
                        state = GameState.MENU;
                    } else if (withinRect(x, y, boardRect)) {
                        int j = (x - bx) / bs;
                        int i = (y - by) / bs;
                        if (0 <= i && i <= 2 && 0 <= j && j <= 2) {
                            if (board[i][j] == 0) {
                                board[i][j] = player;
                                synchronized (animations) {
                                    animations.add(new Animation(player == 1 ? bitmapCross : bitmapNought, j * bs, i * bs, bs, bs, 30, 15, false));
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
                gradientScreen.draw(canvas);
                splash.draw(canvas, 0, 0);
                break;
            case MENU:
                gradientScreen.draw(canvas);
                canvas.drawBitmap(bitmapTitle, null, titleRect, null);
                button.setBounds(startRect);
                button.draw(canvas);
                canvas.drawText("Start", startRect.centerX(), startRect.centerY() + textd, paintText);
                button.setBounds(exitRect);
                button.draw(canvas);
                canvas.drawText("Exit", exitRect.centerX(), exitRect.centerY() + textd, paintText);
                canvas.drawText("© 2015 Alexander Teplukhin", sw / 2, sh * .85f, paintAuthor);
                canvas.drawText("Version 1.2", sw / 2, sh * .925f, paintAuthor);
                break;
            case PLAY:
            case FINISH:
                gradientBoard.draw(canvas);
                synchronized (animations) {
                    for (Animation animation : animations) {
                        animation.draw(canvas, bx, by);
                    }
                }
                canvas.drawLine(bx + 1 * bs, by, bx + bs, by + bw, paintGrid);
                canvas.drawLine(bx + 2 * bs, by, bx + 2 * bs, by + bw, paintGrid);
                canvas.drawLine(bx, by + 1 * bs, bx + bw, by + 1 * bs, paintGrid);
                canvas.drawLine(bx, by + 2 * bs, bx + bw, by + 2 * bs, paintGrid);
                button.setBounds(menuRect);
                button.draw(canvas);
                canvas.drawText("Menu", menuRect.centerX(), menuRect.centerY() + textd, paintText);
                String label = "Player " + Integer.toString(player);
                if (bx > 0 && paintText.measureText(label) > bx)
                    label = "Plr " + Integer.toString(player);
                canvas.drawText(label, labelRect.centerX(), labelRect.centerY() + textd, paintText);
                if (state == GameState.FINISH) {
                    if (tie) label = "Tie!";
                    else label = "Player " + Integer.toString(player) + " won!";
                    int tw = (int) paintText.measureText(label);
                    Rect finishRect = new Rect((int) ((sw - tw) / 2 - .1f * sw), (int) (sh * .4f), (int) ((sw + tw) / 2 + .1f * sw), (int) (sh * .6f));
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
            splash.update(System.currentTimeMillis());
            loading += 0.01;
            if (loading >= 1) {
                loading = 0;
                state = GameState.MENU;
            }
        }
    }

}