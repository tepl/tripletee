package com.teplovoz.tripletee;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

public class Animation implements Parcelable {

    private Bitmap bitmap;		// the animation sequence
    private Rect sourceRect;	// the rectangle to be drawn from the animation bitmap
    private int frameNr;		// number of frames in animation
    private int currentFrame;	// the current frame
    private long frameTicker;	// the time of the last frame update
    private int framePeriod;	// milliseconds between each frame (1000/fps)

    private int spriteWidth;	// the width of the sprite to calculate the cut out rectangle
    private int spriteHeight;	// the height of the sprite

    private int x;				// the X coordinate of the object (top left of the image)
    private int y;				// the Y coordinate of the object (top left of the image)

    private int destw;			// the width of destination
    private int desth;			// the height of destination

    private boolean repeating;     // replays animation
    private boolean running;    // is animation running or not

    public Animation(Bitmap bitmap, int x, int y, int destw, int desth, int fps, int frameCount, boolean repeating) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.destw = destw;
        this.desth = desth;
        this.repeating = repeating;
        currentFrame = 0;
        frameNr = frameCount;
        spriteWidth = bitmap.getWidth() / frameCount;
        spriteHeight = bitmap.getHeight();
        sourceRect = new Rect(0, 0, spriteWidth, spriteHeight);
        framePeriod = 1000 / fps;
        frameTicker = 0l;
        running = true;
    }

    public Animation(Parcel in){
        bitmap = Bitmap.CREATOR.createFromParcel(in);
        x = in.readInt();
        y = in.readInt();
        currentFrame = in.readInt();
        frameNr = in.readInt();
        spriteWidth = in.readInt();
        spriteHeight = in.readInt();
        sourceRect = new Rect(0, 0, spriteWidth, spriteHeight);
        framePeriod = in.readInt();
        frameTicker = in.readLong();
        repeating = (Boolean) in.readValue(null);
        running = (Boolean) in.readValue(null);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags){
        bitmap.writeToParcel(out,0);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(currentFrame);
        out.writeInt(frameNr);
        out.writeInt(spriteWidth);
        out.writeInt(spriteHeight);
        out.writeInt(framePeriod);
        out.writeLong(frameTicker);
        out.writeValue(repeating);
        out.writeValue(running);
    }

    public static final Parcelable.Creator<Animation> CREATOR = new Parcelable.Creator<Animation>() {
        public Animation createFromParcel(Parcel in) {
            return new Animation(in);
        }

        public Animation[] newArray(int size) {
            return new Animation[size];
        }
    };

    public Bitmap getBitmap() {
        return bitmap;
    }
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    public Rect getSourceRect() {
        return sourceRect;
    }
    public void setSourceRect(Rect sourceRect) {
        this.sourceRect = sourceRect;
    }
    public int getFrameNr() {
        return frameNr;
    }
    public void setFrameNr(int frameNr) {
        this.frameNr = frameNr;
    }
    public int getCurrentFrame() {
        return currentFrame;
    }
    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }
    public int getFramePeriod() {
        return framePeriod;
    }
    public void setFramePeriod(int framePeriod) {
        this.framePeriod = framePeriod;
    }
    public int getSpriteWidth() {
        return spriteWidth;
    }
    public void setSpriteWidth(int spriteWidth) {
        this.spriteWidth = spriteWidth;
    }
    public int getSpriteHeight() {
        return spriteHeight;
    }
    public void setSpriteHeight(int spriteHeight) {
        this.spriteHeight = spriteHeight;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }

    // the update method for Elaine
    public void update(long gameTime) {
        if(!running)return;
        if (gameTime > frameTicker + framePeriod) {
            frameTicker = gameTime;
            // increment the frame
            if (currentFrame >= frameNr-1) {
                if(repeating)currentFrame = 0;
                else running = false;
            }
            else currentFrame++;
        }
        // define the rectangle to cut out sprite
        this.sourceRect.left = currentFrame * spriteWidth;
        this.sourceRect.right = this.sourceRect.left + spriteWidth;
    }

    // the draw method which draws the corresponding frame
    public void draw(Canvas canvas, int bx, int by) {
        // where to draw the sprite
        Rect destRect = new Rect(bx + getX(), by + getY(), bx + getX() + destw, by + getY() + desth);
        canvas.drawBitmap(bitmap, sourceRect, destRect, null);
    }
}
