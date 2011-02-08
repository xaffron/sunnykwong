package com.sunnykwong.HCLW;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class HCLWService  extends WallpaperService  {
	// First, a few flags for the service.
	static boolean RUNNING=false;	// Am I (already) running?
	static boolean STOPNOW4x4=false;		// Should I stop now? from 4x4 widgets
	static boolean STOPNOW4x2=false;		// Should I stop now? from 4x2 widgets
	static boolean STOPNOW4x1=false;		// Should I stop now? from 4x1 widgets
	static boolean STOPNOW3x3=false;		// Should I stop now? from 3x3 widgets
	static boolean STOPNOW3x1=false;		// Should I stop now? from 3x1 widgets
	static boolean STOPNOW2x2=false;		// Should I stop now? from 2x2 widgets
	static boolean STOPNOW2x1=false;		// Should I stop now? from 2x1 widgets
	static boolean STOPNOW1x3=false;		// Should I stop now? from 1x3 widgets
    static Object[] mStartForegroundArgs = new Object[2];
    static Object[] mStopForegroundArgs = new Object[1];

    private final Handler mHandler = new Handler();

	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {

	
	}

	@Override
	public Engine onCreateEngine() {
		// TODO Auto-generated method stub
		return new CubeEngine();
	}

	@Override
    public void onDestroy() {
		// Make sure our notification is gone.
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {

    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		//	Tell the widgets to refresh themselves.

		// We want intents redelivered and onStartCommand re-executed if the service is killed.
		return 1;  // Service.START_STICKY ; have to use literal because Donut is unaware of the constant
	}

    class CubeEngine extends Engine {

        private final Paint mPaint = new Paint();
        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private long mStartTime;
        private float mCenterX;
        private float mCenterY;

        private final Runnable mDrawCube = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;

        CubeEngine() {
            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            mStartTime = SystemClock.elapsedRealtime();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawCube);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // store the center of the surface, so we can draw the cube in the right spot
            mCenterX = width/2.0f;
            mCenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } else {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }

        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                    drawCube(c);
                    drawTouchPoint(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawCube);
            if (mVisible) {
                mHandler.postDelayed(mDrawCube, 1000 / 25);
            }
        }

        /*
         * Draw a wireframe cube by drawing 12 3 dimensional lines between
         * adjacent corners of the cube
         */
        void drawCube(Canvas c) {
            c.save();
            c.translate(mCenterX, mCenterY);
            c.drawColor(0xff000000);
            drawLine(c, -400, -400, -400,  400, -400, -400);
            drawLine(c,  400, -400, -400,  400,  400, -400);
            drawLine(c,  400,  400, -400, -400,  400, -400);
            drawLine(c, -400,  400, -400, -400, -400, -400);

            drawLine(c, -400, -400,  400,  400, -400,  400);
            drawLine(c,  400, -400,  400,  400,  400,  400);
            drawLine(c,  400,  400,  400, -400,  400,  400);
            drawLine(c, -400,  400,  400, -400, -400,  400);

            drawLine(c, -400, -400,  400, -400, -400, -400);
            drawLine(c,  400, -400,  400,  400, -400, -400);
            drawLine(c,  400,  400,  400,  400,  400, -400);
            drawLine(c, -400,  400,  400, -400,  400, -400);
            c.restore();
        }

        /*
         * Draw a 3 dimensional line on to the screen
         */
        void drawLine(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
            long now = SystemClock.elapsedRealtime();
            float xrot = ((float)(now - mStartTime)) / 1000;
            float yrot = (0.5f - mOffset) * 2.0f;
            float zrot = 0;

            // 3D transformations

            // rotation around X-axis
            float newy1 = (float)(Math.sin(xrot) * z1 + Math.cos(xrot) * y1);
            float newy2 = (float)(Math.sin(xrot) * z2 + Math.cos(xrot) * y2);
            float newz1 = (float)(Math.cos(xrot) * z1 - Math.sin(xrot) * y1);
            float newz2 = (float)(Math.cos(xrot) * z2 - Math.sin(xrot) * y2);

            // rotation around Y-axis
            float newx1 = (float)(Math.sin(yrot) * newz1 + Math.cos(yrot) * x1);
            float newx2 = (float)(Math.sin(yrot) * newz2 + Math.cos(yrot) * x2);
            newz1 = (float)(Math.cos(yrot) * newz1 - Math.sin(yrot) * x1);
            newz2 = (float)(Math.cos(yrot) * newz2 - Math.sin(yrot) * x2);

            // 3D-to-2D projection
            float startX = newx1 / (4 - newz1 / 400);
            float startY = newy1 / (4 - newz1 / 400);
            float stopX =  newx2 / (4 - newz2 / 400);
            float stopY =  newy2 / (4 - newz2 / 400);

            c.drawLine(startX, startY, stopX, stopY, mPaint);
        }

        /*
         * Draw a circle around the current touch point, if any.
         */
        void drawTouchPoint(Canvas c) {
            if (mTouchX >=0 && mTouchY >= 0) {
                c.drawCircle(mTouchX, mTouchY, 80, mPaint);
            }
        }

    }
}
