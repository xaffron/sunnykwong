package com.sunnykwong.HCLW;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.VelocityTracker;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class HCLWService extends WallpaperService  {
	public Bitmap bkgd;
	public Bitmap flare; 
    private final Handler mHandler = new Handler();

	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {
		bkgd = BitmapFactory.decodeResource(this.getResources(), R.drawable.bkgd);
		flare = BitmapFactory.decodeResource(this.getResources(), R.drawable.flare);
	}

	@Override
	public Engine onCreateEngine() {
		// TODO Auto-generated method stub
		return new FlareEngine();
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

    class FlareEngine extends Engine {

    	public final float LDPISCALEX=0.75f;
    	public final float LDPISCALEY=0.75f;
    	public final float MDPISCALEX=1f;
    	public final float MDPISCALEY=1f;
    	public final float HDPISCALEX=1.5f;
    	public final float HDPISCALEY=1.78f;
    	public float SCALEX=1f;
    	public float SCALEY=1f;
    	
    	public final float[] FLAREPATHINITX
    		= {259f,272f,288f,410f,
    		420f,432f,443f,453f,
    		467f,478f,489f,506f};
    	public final float[] FLAREPATHINITY
    		= {234f,234f,234f,248f,
    			248f,248f,248f,248f,
    			248f,248f,248f,248f};
    	public final float[] FLAREPATHINITZ
        	= {0.1f,0.1f,0.1f,0.1f,
    		0.1f,0.1f,0.1f,0.1f,
    		0.1f,0.1f,0.1f,0.1f};
    	
    	public final float[] FLAREPATHMIDX
			= {163,169,181,272,
			313,359,404,448,
			494,540,585,572};
    	public final float[] FLAREPATHMIDY
			= {296,304,315,300,
			300,300,300,300,
			300,300,300,276};
    	public final float[] FLAREPATHMIDZ
	    	= {0.3f,0.3f,0.3f,0.5f,
			0.5f,0.5f,0.5f,0.5f,
			0.5f,0.5f,0.5f,0.5f};
	
    	public final float[] FLAREPATHFINALX
			= {0,0,0,0,
			0,121,270,433,
			590,640,640,640};
    	public final float[] FLAREPATHFINALY
			= {332,348,372,403,
			459,480,480,480,
			480,380,328,300};
		public final float[] FLAREPATHFINALZ
	    	= {1f,1f,1f,1f,
			1f,1f,1f,1f,
			1f,1f,1f,1f};
		public float MINFLARESPEED = 0.02f;
		public float[] FLARESPEEDS
			= {0.01f,0.02f,0.01f,0.02f,
				0.01f,0.02f,0.01f,0.02f,
				0.01f,0.02f,0.01f,0.02f};
		public float[] DISPLACEMENTS
			= {0f,0f,0f,0f,
				0f,0f,0f,0f,
				0f,0f,0f,0f};
	
		public final Matrix TEMPMATRIX = new Matrix(), TEMPMATRIX2=new Matrix();
		
		private final Paint mPaint = new Paint();
        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private long mStartTime;
        private float mCenterX;
        private float mCenterY;

        private long IGNORETOUCHUNTIL;
        
        private final Runnable mDrawFlare = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        
        private boolean mVisible;

        FlareEngine() {
            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            mStartTime = SystemClock.elapsedRealtime();
            
            switch (HCLWService.this.getResources().getDisplayMetrics().densityDpi) {
            	case (DisplayMetrics.DENSITY_HIGH):
            		SCALEX = HDPISCALEX;
            		SCALEY = HDPISCALEY;
            		break;
            	case (DisplayMetrics.DENSITY_MEDIUM):
            		SCALEX = MDPISCALEX;
            		SCALEY = MDPISCALEY;
            		break;
            	case (DisplayMetrics.DENSITY_LOW):
            		SCALEX = LDPISCALEX;
            		SCALEY = LDPISCALEY;
            		break;
            	default:
            		break;
            }
            
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
            mHandler.removeCallbacks(mDrawFlare);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawFlare);
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
            mHandler.removeCallbacks(mDrawFlare);
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
                	drawFlares(c);
                    drawTouchPoint(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawFlare);
            if (mVisible) {
                mHandler.postDelayed(mDrawFlare, 1000 / 25);
            }
        }

        /*
         * Draw a wireframe cube by drawing 12 3 dimensional lines between
         * adjacent corners of the cube
         */
        void drawFlares(Canvas c) {
        	Paint pt = new Paint();
        	pt.setColor(Color.WHITE);
        	c.drawBitmap(bkgd, 0f,0f, pt);
        	for (int i = 0; i < 12; i++) {
        		if (DISPLACEMENTS[i]>1f) {
        			DISPLACEMENTS[i]=0f;
        		} else if (DISPLACEMENTS[i]==0f) {
        			//Only relaunch a flare 1% of the time
        			if (Math.random() < 0.01d) {
        				FLARESPEEDS[i]= (float)Math.random() * 0.05f + MINFLARESPEED;
            			DISPLACEMENTS[i]+=FLARESPEEDS[i];
        			}
        		} else {
        			DISPLACEMENTS[i]+=FLARESPEEDS[i];
        		}
        		
        		TEMPMATRIX.reset();
        		float xPos = floatInterpolate(FLAREPATHINITX[i],FLAREPATHMIDX[i],FLAREPATHFINALX[i],DISPLACEMENTS[i]) * SCALEX;
        		float yPos = floatInterpolate(FLAREPATHINITY[i],FLAREPATHMIDY[i],FLAREPATHFINALY[i],DISPLACEMENTS[i])* SCALEY;
        		float zFactor = ((float)Math.random()*0.5f + 0.5f)  * floatInterpolate(FLAREPATHINITZ[i], FLAREPATHMIDZ[i], FLAREPATHFINALZ[i], DISPLACEMENTS[i]);
        		TEMPMATRIX.postScale(zFactor, zFactor);
        		TEMPMATRIX.postTranslate(xPos-flare.getWidth()/2f*zFactor, yPos-flare.getHeight()/2f*zFactor);
        		
        		c.drawBitmap(flare, TEMPMATRIX, pt);
            }

        
        }

    	public float floatInterpolate (float n1, float n2, float n3, float gradient) {
    		if (gradient > 0.5f) return (n2+ (n3-n2)*(gradient-0.5f) * 2);
    		else return (n1 + (n2-n1) * gradient * 2);
    	}

        /*
         * Draw a circle around the current touch point, if any.
         */
        void drawTouchPoint(Canvas c) {
        	if (System.currentTimeMillis()<IGNORETOUCHUNTIL) return;
            if (mTouchX >=0 && mTouchY >= 0) {
        		TEMPMATRIX2.reset();
            	float zFactor = ((float)Math.random()*0.5f + 0.5f);
        		TEMPMATRIX2.postScale(zFactor, zFactor);
        		TEMPMATRIX2.postTranslate(mTouchX-flare.getWidth()/2f*zFactor, mTouchY-flare.getHeight()/2f*zFactor);
        		c.drawBitmap(flare, TEMPMATRIX2, mPaint);
            }
            IGNORETOUCHUNTIL=System.currentTimeMillis()+50;
        }

    }
}
