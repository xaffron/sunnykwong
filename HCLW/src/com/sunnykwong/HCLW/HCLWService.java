package com.sunnykwong.HCLW;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.VelocityTracker;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class HCLWService extends WallpaperService  {

	public Handler mHandler;

	
	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {
		mHandler = new Handler();
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

        public final Runnable mDrawFlare = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
    
        // FLARE ENGINE. THIS IS WHERE THE WALLPAPER RENDERING OCCURS.
        //
        FlareEngine() {

        	HCLW.LightningFactor = 0f;
        	HCLW.StartTime = SystemClock.elapsedRealtime();

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
        	HCLW.Visible = visible;
            if (visible) {
                drawFrame();
            } else {
            	mHandler.removeCallbacks(mDrawFlare);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // If no colors are enabled, enable all of them!
            boolean bAllColorsDisabled=true;
            for (int i =0; i<5; i++) {
            	if (HCLW.PREFS.getBoolean("showcolor"+i, false)) {
            		bAllColorsDisabled = false;
            		break;
            	}
            }
            if (bAllColorsDisabled) {
            	HCLW.PREFS.edit()
        		.putBoolean("showcolor0", true)
        		.putBoolean("showcolor1", true)
        		.putBoolean("showcolor2", true)
        		.putBoolean("showcolor3", true)
        		.putBoolean("showcolor4", true)
        		.commit();
            }
            
            // store the center of the surface
            HCLW.CenterX = width/2.0f;
            HCLW.CenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            HCLW.Visible = false;
            mHandler.removeCallbacks(mDrawFlare);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            HCLW.Offset = xPixels;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                HCLW.TouchX = event.getX();
                HCLW.TouchY = event.getY();
            } else {
                HCLW.TouchX = -1;
                HCLW.TouchY = -1;
                HCLW.LightningFactor=1f;
            }
            super.onTouchEvent(event);
        }

       
        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
//                	drawFlares(HCLW.BUFFERCANVAS);
//                    drawTouchPoint(HCLW.BUFFERCANVAS);
//                    c.drawBitmap(HCLW.BUFFER, 0f,0f, HCLW.PaintMid);
                	drawFlares(c);
                  drawTouchPoint(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawFlare);
            if (HCLW.Visible) {
            	mHandler.postDelayed(mDrawFlare, 1000 / 25);
            }
        }

         void drawFlares(Canvas c) {
        	 
        	 float fTempOffset = HCLW.Offset;
        	 
        	// Draw the "Channels" on the bottom.
        	// Default to channel bkgd (white for Sparks).
    		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
    			c.drawColor(Color.parseColor("#888C8C8C"));
    		} else {
//    			c.drawColor(Color.parseColor("#ff1b1939"));
    			c.drawColor(Color.parseColor("#111b1939"));
    		}
        	
        	// if Flares are to be above surface, draw the "Surface" now (and skip the "middle" mask).
        	if (HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
        		c.drawBitmap(HCLW.FG, fTempOffset,0f, HCLW.PaintFg);
        	}
        	
        	// We're tracking each flare.
        	for (int i = 0; i < HCLW.DISPLACEMENTS.length; i++) {
        		// We want each flare to go completely offscreen (plus ample margin)
        		// before resetting... this is to account for trails.
        		if (HCLW.DISPLACEMENTS[i]>2f) {
        			HCLW.DISPLACEMENTS[i]=0f;
        		} else if (HCLW.DISPLACEMENTS[i]==0f) {
        			//Only relaunch a flare 1% of the time by default (can be customized)
        			if (Math.random() < 0.01d * Double.parseDouble(HCLW.PREFS.getString("FlareFrequency", "1"))) {
    	        		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
    	        			HCLW.FLARESPEEDS[i]= (float)(HCLW.MINFLARESPEEDS[i]*(1+Math.random()));
    	        		} else {
    	        			HCLW.FLARESPEEDS[i]= HCLW.MINFLARESPEEDS[i]*2;
    	        		}
    	        		HCLW.DISPLACEMENTS[i]+=HCLW.FLARESPEEDS[i];
    	        		// Pick a color for each flare.
            			do {
            				HCLW.COLORS[i]=(int)(Math.random()*5.);
            			} while (!HCLW.PREFS.getBoolean("showcolor"+HCLW.COLORS[i], true));
        			}
        		} else {
        			HCLW.DISPLACEMENTS[i]+=HCLW.FLARESPEEDS[i];
        		}

        		//Flares
        		
        		//If spark effects, no trails; otherwise, trails
        		int iTrailLength=Integer.parseInt(HCLW.PREFS.getString("TrailLength", "10"));

        		//Render trail for each flare
        		for (int j=0; j>-iTrailLength; j--) {
        			// If Spark effect, always only render flare head
//        			if (j<0) break;
        			if (HCLW.PREFS.getBoolean("SparkEffect", false) && j<0) break;

        			// If the flare head/tail will be offscreen, skip drawing that part
        			if (HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j<0) continue;
        			if (HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j>1) continue;
        			
        			
        			if (!HCLW.PREFS.getBoolean("SparkEffect", false) && HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j==0) continue;
        			HCLW.TEMPMATRIX.reset();
	        		float xPos = floatInterpolate(HCLW.FLAREPATHINITX[i],HCLW.FLAREPATHMIDX[i],HCLW.FLAREPATHFINALX[i],HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j) * HCLW.SCALEX;
	        		float yPos = floatInterpolate(HCLW.FLAREPATHINITY[i],HCLW.FLAREPATHMIDY[i],HCLW.FLAREPATHFINALY[i],HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j) * HCLW.SCALEY;
	        		float zFactor;
	        		//Sparks
	        		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
	        			zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i], HCLW.FLAREPATHMIDZ[i], 
	        					HCLW.FLAREPATHFINALZ[i], HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j) 
	        					* (1f + (float)(.5d*Math.random()));
	        		} else {
	        			zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i], HCLW.FLAREPATHMIDZ[i], 
	        					HCLW.FLAREPATHFINALZ[i], HCLW.DISPLACEMENTS[i]+HCLW.FLARESPEEDS[i]*j);
	        		}
	        		HCLW.TEMPMATRIX.postScale(zFactor, zFactor);
	        		HCLW.TEMPMATRIX.postTranslate(xPos-HCLW.FLARE[HCLW.COLORS[i]].getWidth()/2f*zFactor+fTempOffset, yPos-HCLW.FLARE[HCLW.COLORS[i]].getHeight()/2f*zFactor);

	        		HCLW.PaintFlare.setAlpha((int)(255*(1f+(float)j/iTrailLength)));
	        		c.drawBitmap(HCLW.FLARE[HCLW.COLORS[i]], HCLW.TEMPMATRIX, HCLW.PaintFlare);
        		}
        	}
        	
        	if (HCLW.PREFS.getBoolean("LightningEffect", false)) {
        		if (Math.random()<0.05d) {
        			HCLW.LightningFactor=1f;
        		} else if (HCLW.LightningFactor<=0f) {
        			HCLW.LightningFactor=0f;
        		} else {
        			HCLW.LightningFactor-=0.05f;
        		}
    			HCLW.PaintFg.setAlpha((int)(255f*HCLW.LightningFactor));
        	} else {
        		HCLW.PaintFg.setAlpha(255);
        	}

           	// Draw the  "Middle" mask, then the "Surface".
        	if (!HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
        		if (HCLW.PaintFg.getAlpha()<255) c.drawBitmap(HCLW.MIDDLE, fTempOffset, 0f, HCLW.PaintMid);
        		if (HCLW.LightningFactor>0f) c.drawBitmap(HCLW.FG, fTempOffset,0f, HCLW.PaintFg);
        	}

        }

    	public float floatInterpolate (float n1, float n2, float n3, float gradient) {
    		if (gradient > 0.5f) return (n2+ (n3-n2)*(gradient-0.5f) * 2);
    		else return (n1 + (n2-n1) * gradient * 2);
    	}

        /*
         * Draw a flare around the current touch point.
         */
        void drawTouchPoint(Canvas c) {
//        	if (System.currentTimeMillis()<HCLW.IGNORETOUCHUNTIL) return;
            if (HCLW.TouchX >=0 && HCLW.TouchY >= 0) {
            	HCLW.TEMPMATRIX2.reset();
            	float zFactor = ((float)Math.random()*0.5f + 0.5f);
            	HCLW.TEMPMATRIX2.postScale(zFactor, zFactor);
            	HCLW.TEMPMATRIX2.postTranslate(HCLW.TouchX-HCLW.FLARE[0].getWidth()/2f*zFactor, 
            			HCLW.TouchY-HCLW.FLARE[0].getHeight()/2f*zFactor);
        		c.drawBitmap(HCLW.FLARE[0], HCLW.TEMPMATRIX2, HCLW.PaintFlare);
            }
            HCLW.IGNORETOUCHUNTIL=System.currentTimeMillis()+50;
        }

    }
}